/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * 
 */
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import jdbm.btree.BTree;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.core.avltree.ArrayTreeCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over a BTree which manages duplicate keys.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class DupsCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    private static final Logger LOG = LoggerFactory.getLogger( DupsCursor.class );

    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /**
     * The JDBM backed table this Cursor traverses over.
     */
    private final JdbmTable<K, V> table;

    /**
     * An wrappedCursor Cursor which returns Tuples whose values are
     * DupsContainer objects representing either AvlTrees or BTreeRedirect
     * objects used to store the values of duplicate keys.  It does not return
     * different values for the same key.
     */
    private final DupsContainerCursor<K, V> containerCursor;

    /**
     * The current Tuple returned from the wrappedCursor DupsContainerCursor.
     */
    private final Tuple<K, DupsContainer<V>> containerTuple = new Tuple<K, DupsContainer<V>>();

    /**
     * A Cursor over a set of value objects for the current key held in the
     * containerTuple.  A new Cursor will be set for each new key as we
     * traverse.  The Cursor traverses over either a AvlTree object full
     * of values in a multi-valued key or it traverses over a BTree which
     * contains the values in the key field of it's Tuples.
     */
    private Cursor<V> dupsCursor;

    /**
     * The Tuple that is used to return values via the get() method. This
     * same Tuple instance will be returned every time.  At different
     * positions it may return different values for the same key.
     */
    private final Tuple<K, V> returnedTuple = new Tuple<K, V>();

    /**
     * Whether or not a value is available when get() is called.
     */
    private boolean valueAvailable;


    public DupsCursor( JdbmTable<K, V> table )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating DupsCursor {}", this );
        }

        this.table = table;
        this.containerCursor = new DupsContainerCursor<K, V>( table );
        LOG.debug( "Created on table {}", table );
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void beforeKey( K key ) throws Exception
    {
        beforeValue( key, null );
    }


    public void beforeValue( K key, V value ) throws LdapException, CursorException
    {
        checkNotClosed( "beforeValue()" );
        containerCursor.before( new Tuple<K, DupsContainer<V>>( key, null ) );

        if ( containerCursor.next() )
        {
            containerTuple.setBoth( containerCursor.get() );
            DupsContainer<V> values = containerTuple.getValue();

            if ( values.isArrayTree() )
            {
                ArrayTree<V> set = values.getArrayTree();
                dupsCursor = new ArrayTreeCursor<V>( set );
            }
            else
            {
                try
                {
                    BTree tree = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyBTreeCursor<V>( tree, table.getValueComparator() );
                }
                catch ( IOException e )
                {
                    throw new CursorException( e );
                }
            }

            if ( value == null )
            {
                return;
            }

            // advance the dupsCursor only if we're on same key
            if ( table.getKeyComparator().compare( containerTuple.getKey(), key ) == 0 )
            {
                dupsCursor.before( value );
            }

            return;
        }

        clearValue();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
    }


    public void afterKey( K key ) throws Exception
    {
        afterValue( key, null );
    }


    public void afterValue( K key, V value ) throws LdapException, CursorException
    {
        checkNotClosed( "afterValue()" );
        /*
         * There is a subtle difference between after and before handling
         * with duplicate key values.  Say we have the following tuples:
         *
         * (0, 0)
         * (1, 1)
         * (1, 2)
         * (1, 3)
         * (2, 2)
         *
         * If we request an after cursor on (1, 2).  We must make sure that
         * the container cursor does not advance after the entry with key 1
         * since this would result in us skip returning (1. 3) on the call to
         * next which will incorrectly return (2, 2) instead.
         *
         * So if the value is null in the element then we don't care about
         * this obviously since we just want to advance past the duplicate key
         * values all together.  But when it is not null, then we want to
         * go right before this key instead of after it.
         */

        if ( value == null )
        {
            containerCursor.after( new Tuple<K, DupsContainer<V>>( key, null ) );
        }
        else
        {
            containerCursor.before( new Tuple<K, DupsContainer<V>>( key, null ) );
        }

        if ( containerCursor.next() )
        {
            containerTuple.setBoth( containerCursor.get() );
            DupsContainer<V> values = containerTuple.getValue();

            if ( values.isArrayTree() )
            {
                ArrayTree<V> set = values.getArrayTree();
                dupsCursor = new ArrayTreeCursor<V>( set );
            }
            else
            {
                try
                {
                    BTree tree = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyBTreeCursor<V>( tree, table.getValueComparator() );
                }
                catch ( IOException e )
                {
                    throw new CursorException( e );
                }
            }

            if ( value == null )
            {
                return;
            }

            // only advance the dupsCursor if we're on same key
            if ( table.getKeyComparator().compare( containerTuple.getKey(), key ) == 0 )
            {
                dupsCursor.after( value );
            }

            return;
        }

        clearValue();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
    }


    /**
     * {@inheritDoc}
     */
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        beforeValue( element.getKey(), element.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        afterValue( element.getKey(), element.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed( "beforeFirst()" );
        clearValue();
        containerCursor.beforeFirst();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
        dupsCursor = null;
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed( "afterLast()" );
        clearValue();
        containerCursor.afterLast();
        containerTuple.setKey( null );
        containerTuple.setValue( null );
        dupsCursor = null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed( "first()" );
        clearValue();
        dupsCursor = null;

        if ( containerCursor.first() )
        {
            containerTuple.setBoth( containerCursor.get() );
            DupsContainer<V> values = containerTuple.getValue();

            if ( containerTuple.getValue().isArrayTree() )
            {
                dupsCursor = new ArrayTreeCursor<V>( values.getArrayTree() );
            }
            else
            {
                try
                {
                    BTree bt = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyBTreeCursor<V>( bt, table.getValueComparator() );
                }
                catch ( IOException e )
                {
                    throw new CursorException( e );
                }
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.first();
            valueAvailable = true;
            returnedTuple.setKey( containerTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );

            return true;
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed( "last()" );
        clearValue();
        dupsCursor = null;

        if ( containerCursor.last() )
        {
            containerTuple.setBoth( containerCursor.get() );
            DupsContainer<V> values = containerTuple.getValue();

            if ( values.isArrayTree() )
            {
                ArrayTree<V> set = values.getArrayTree();
                dupsCursor = new ArrayTreeCursor<V>( set );
            }
            else
            {
                try
                {
                    BTree tree = table.getBTree( values.getBTreeRedirect() );
                    dupsCursor = new KeyBTreeCursor<V>( tree, table.getValueComparator() );
                }
                catch ( IOException e )
                {
                    throw new CursorException( e );
                }
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.last();
            valueAvailable = true;
            returnedTuple.setKey( containerTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );

            return true;
        }

        return false;
    }


    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed( "previous()" );
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        if ( null == dupsCursor || !dupsCursor.previous() )
        {
            if ( dupsCursor != null )
            {
                dupsCursor.close();
            }

            /*
             * If the wrappedCursor cursor has more elements we get the previous
             * key/AvlTree Tuple to work with and get a cursor over it's
             * values.
             */
            if ( containerCursor.previous() )
            {
                containerTuple.setBoth( containerCursor.get() );
                DupsContainer<V> values = containerTuple.getValue();

                if ( values.isArrayTree() )
                {
                    ArrayTree<V> set = values.getArrayTree();
                    dupsCursor = new ArrayTreeCursor<V>( set );
                }
                else
                {
                    try
                    {
                        BTree tree = table.getBTree( values.getBTreeRedirect() );
                        dupsCursor = new KeyBTreeCursor<V>( tree, table.getValueComparator() );
                    }
                    catch ( IOException e )
                    {
                        throw new CursorException( e );
                    }
                }

                /*
                 * Since only tables with duplicate keys enabled use this
                 * cursor, entries must have at least one value, and therefore
                 * call to previous() after bringing the cursor to afterLast()
                 * will always return true.
                 */
                dupsCursor.afterLast();
                dupsCursor.previous();
            }
            else
            {
                dupsCursor = null;
                return false;
            }
        }

        returnedTuple.setKey( containerTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );

        valueAvailable = true;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed( "next()" );
        /*
         * If the iterator over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        if ( ( null == dupsCursor ) || !dupsCursor.next() )
        {
            if ( dupsCursor != null )
            {
                dupsCursor.close();
            }

            /*
             * If the wrappedCursor cursor has more elements we get the next
             * key/AvlTree Tuple to work with and get a cursor over it.
             */
            if ( containerCursor.next() )
            {
                containerTuple.setBoth( containerCursor.get() );
                DupsContainer<V> values = containerTuple.getValue();

                if ( values.isArrayTree() )
                {
                    ArrayTree<V> set = values.getArrayTree();
                    dupsCursor = new ArrayTreeCursor<V>( set );
                }
                else
                {
                    try
                    {
                        BTree tree = table.getBTree( values.getBTreeRedirect() );
                        dupsCursor = new KeyBTreeCursor<V>( tree, table.getValueComparator() );
                    }
                    catch ( IOException e )
                    {
                        throw new CursorException( e );
                    }
                }

                /*
                 * Since only tables with duplicate keys enabled use this
                 * cursor, entries must have at least one value, and therefore
                 * call to next() after bringing the cursor to beforeFirst()
                 * will always return true.
                 */
                dupsCursor.beforeFirst();
                dupsCursor.next();
            }
            else
            {
                dupsCursor = null;
                return false;
            }
        }

        /*
         * If we get to this point then cursor has more elements and
         * containerTuple holds the Tuple containing the key and the btree or
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        returnedTuple.setKey( containerTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );

        valueAvailable = true;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws CursorException
    {
        checkNotClosed( "get()" );

        if ( !valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return returnedTuple;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing DupsCursor {}", this );
        }

        super.close();
        containerCursor.close();

        if ( dupsCursor != null )
        {
            dupsCursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing DupsCursor {}", this );
        }

        super.close( cause );
        containerCursor.close( cause );

        if ( dupsCursor != null )
        {
            dupsCursor.close( cause );
        }
    }
}
