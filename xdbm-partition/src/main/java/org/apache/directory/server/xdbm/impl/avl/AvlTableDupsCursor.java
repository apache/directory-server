/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm.impl.avl;


import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.ConcurrentMapCursor;
import org.apache.directory.server.core.avltree.OrderedSet;
import org.apache.directory.server.core.avltree.OrderedSetCursor;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.apache.directory.shared.ldap.model.cursor.AbstractTupleCursor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which walks and advance over AvlTables that may contain duplicate
 * keys with values stored in an AvlTree.  All duplicate keys are traversed 
 * returning the key and the value in a Tuple.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTableDupsCursor<K,V> extends AbstractTupleCursor<K, V>
{
    private static final Logger LOG = LoggerFactory.getLogger( AvlTableDupsCursor.class.getSimpleName() );
    
    /** The AVL backed table this Cursor traverses over. */
    private final AvlTable<K,V> table;
    
    /**
     * The underlying wrapped cursor which returns Tuples whose values are
     * either V objects or AvlTree objects.
     */
    private final ConcurrentMapCursor<K, SingletonOrOrderedSet<V>> wrappedCursor;
    
    /**
     * A Cursor over a set of value objects for the current key held in the
     * containerTuple.  A new Cursor will be set for each new key as we
     * traverse.  The Cursor traverses over either a AvlTree object full
     * of values in a multi-valued key or it traverses over a BTree which
     * contains the values in the key field of it's Tuples.
     */
    private Cursor<V> dupsCursor;

    /** The current Tuple returned from the wrapped cursor. */
    private final Tuple<K,SingletonOrOrderedSet<V>> wrappedTuple = new Tuple<K, SingletonOrOrderedSet<V>>();

    /**
     * The Tuple that is used to return values via the get() method. This
     * same Tuple instance will be returned every time.  At different
     * positions it may return different values for the same key.
     */
    private final Tuple<K,V> returnedTuple = new Tuple<K,V>();

    /** Whether or not a value is available when get() is called. */
    private boolean valueAvailable;

    
    /**
     * Creates a new instance of AvlTableDupsCursor.
     *
     * @param table the AvlTable to build a Cursor on.
     */
    public AvlTableDupsCursor( AvlTable<K,V> table )
    {
        this.table = table;
        this.wrappedCursor = new ConcurrentMapCursor<K, SingletonOrOrderedSet<V>>( table.getBackingMap() );
        LOG.debug( "Created on table {}", table.getName() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeKey( K key ) throws Exception
    {
        beforeValue( key, null );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( K key, V value ) throws Exception
    {
        checkNotClosed( "beforeValue()" );
        wrappedCursor.beforeKey( key );
        
        if ( wrappedCursor.next() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            
            if ( wrappedTuple.getValue().isOrderedSet() )
            {
                OrderedSet<V> orderedSet = wrappedTuple.getValue().getOrderedSet();
                dupsCursor = new OrderedSetCursor<V>( orderedSet );
            }
            else
            {
                dupsCursor = new SingletonCursor<V>( 
                    wrappedTuple.getValue().getSingleton(), table.getValueComparator() );
            }
            
            if ( value == null )
            {
                clearValue();
                return;
            }
    
            /* 
             * The cursor over the values is only advanced if we're on the 
             * same key as the primary cursor.  This is because we want this
             * method to really position us within a set of duplicate key 
             * entries at the proper value.
             */
            if ( table.getKeyComparator().compare( wrappedTuple.getKey(), key ) == 0 )
            {
                dupsCursor.before( value );
            }
            
            clearValue();
            return;
        }
        
        clearValue();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
    }
    

    /**
     * {@inheritDoc}
     */
    public void afterKey( K key ) throws Exception
    {
        afterValue( key, null );
    }

    
    /**
     * {@inheritDoc}
     */
    public void afterValue( K key, V value ) throws Exception
    {
        checkNotClosed( "afterValue()" );
        /*
         * There is a subtle difference between after and before handling
         * with dupicate key values.  Say we have the following tuples:
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
            wrappedCursor.afterKey( key );
        }
        else
        {
            wrappedCursor.beforeKey( key );
        }

        if ( wrappedCursor.next() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                OrderedSet<V> set = values.getOrderedSet();
                dupsCursor = new OrderedSetCursor<V>( set );
            }
            else
            {
                dupsCursor = new SingletonCursor<V>( values.getSingleton(), table.getValueComparator() );
            }

            if ( value == null )
            {
                clearValue();
                return;
            }

            // only advance the dupsCursor if we're on same key
            if ( table.getKeyComparator().compare( wrappedTuple.getKey(), key ) == 0 )
            {
                dupsCursor.after( value );
            }

            clearValue();
            return;
        }

        clearValue();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
    }

    
    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws Exception
    {
        afterValue( element.getKey(), element.getValue() );
    }

    
    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        clearValue();
        wrappedCursor.afterLast();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
        dupsCursor = null;
    }


    /**
     * {@inheritDoc}
     */
    public void before( Tuple<K, V> element ) throws Exception
    {
        beforeValue( element.getKey(), element.getValue() );
    }

    
    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        clearValue();
        wrappedCursor.beforeFirst();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
        dupsCursor = null;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        checkNotClosed( "first()" );
        clearValue();
        dupsCursor = null;

        if ( wrappedCursor.first() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                dupsCursor = new OrderedSetCursor<V>( values.getOrderedSet() );
            }
            else
            {
                dupsCursor = new SingletonCursor<V>( values.getSingleton(), table.getValueComparator() );
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.first();
            valueAvailable =  true;
            returnedTuple.setKey( wrappedTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );
            return true;
        }

        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( ! valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return returnedTuple;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        checkNotClosed( "last()" );
        clearValue();
        dupsCursor = null;

        if ( wrappedCursor.last() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                dupsCursor = new OrderedSetCursor<V>( values.getOrderedSet() );
            }
            else
            {
                dupsCursor = new SingletonCursor<V>( values.getSingleton(), table.getValueComparator() );
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.last();
            valueAvailable = true;
            returnedTuple.setKey( wrappedTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );
            return true;
        }

        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        /*
         * If the cursor over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        if ( null == dupsCursor || ! dupsCursor.next() )
        {
            /*
             * If the wrappedCursor cursor has more elements we get the next
             * key/AvlTree Tuple to work with and get a cursor over it.
             */
            if ( wrappedCursor.next() )
            {
                wrappedTuple.setBoth( wrappedCursor.get() );
                SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

                if ( values.isOrderedSet())
                {
                    dupsCursor = new OrderedSetCursor<V>( values.getOrderedSet() );
                }
                else
                {
                    dupsCursor = new SingletonCursor<V>( values.getSingleton(), table.getValueComparator() );
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
         * wrappedTuple holds the Tuple containing the key and the 
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        returnedTuple.setKey( wrappedTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );
        return valueAvailable = true;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        /*
         * If the cursor over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        if ( null == dupsCursor || ! dupsCursor.previous() )
        {
            /*
             * If the wrappedCursor cursor has more elements we get the previous
             * key/AvlTree Tuple to work with and get a cursor over it's
             * values.
             */
            if ( wrappedCursor.previous() )
            {
                wrappedTuple.setBoth( wrappedCursor.get() );
                SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

                if ( values.isOrderedSet() )
                {
                    dupsCursor = new OrderedSetCursor<V>( values.getOrderedSet() );
                }
                else
                {
                    dupsCursor = new SingletonCursor<V>( values.getSingleton(), table.getValueComparator() );
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

        returnedTuple.setKey( wrappedTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );
        return valueAvailable = true;
    }


    /**
     * Clears the returned Tuple and makes sure valueAvailable returns false.
     */
    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }
}
