/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.apache.mavibot.btree.BTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cursor over a set of values for the same key which are store in an in
 * memory ArrayTree.  This Cursor is limited to the same key and it's tuples
 * will always return the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyTupleArrayCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private final org.apache.mavibot.btree.Cursor<V,V> wrapped;
    private final K key;

    private Tuple<K, V> returnedTuple = new Tuple<K, V>();
    private boolean valueAvailable;


    /**
     * Creates a Cursor over the tuples of an ArrayTree.
     *
     * @param arrayTree the ArrayTree to build a Tuple returning Cursor over
     * @param key the constant key for which values are returned
     */
    public KeyTupleArrayCursor( BTree<V,V> arrayTree, K key )
    {
        this.key = key;
        
        try
        {
            this.wrapped = arrayTree.browse();
        }
        catch( IOException e )
        {
            throw new RuntimeException( e );
        }
        
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating KeyTupleArrayCursor {}", this );
        }
    }


    private void clearValue()
    {
        returnedTuple.setKey( key );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void beforeKey( K key ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    public void afterKey( K key ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    public void beforeValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    public void afterValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    /**
     * Positions this Cursor over the same keys before the value of the
     * supplied element Tuple.  The supplied element Tuple's key is not
     * considered at all.
     *
     * @param element the valueTuple who's value is used to position this Cursor
     * @throws Exception if there are failures to position the Cursor
     */
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed( "previous()" );
        
        try
        {
        	if ( wrapped.hasPrev() )
        	{
        		org.apache.mavibot.btree.Tuple<V, V> t = wrapped.prev();
        		returnedTuple.setKey( key );
        		returnedTuple.setValue( t.getKey() );
        		return valueAvailable = true;
        	}
        	else
        	{
        		clearValue();
        		return false;
        	}
        }
        catch( IOException e )
        {
        	throw new CursorException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed( "next()" );
        try
        {
        	if ( wrapped.hasNext() )
        	{
        		org.apache.mavibot.btree.Tuple<V, V> t = wrapped.next();
        		returnedTuple.setKey( key );
        		returnedTuple.setValue( t.getKey() );
        		return valueAvailable = true;
        	}
        	else
        	{
        		clearValue();
        		return false;
        	}
        }
        catch( IOException e )
        {
        	throw new CursorException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws CursorException
    {
        checkNotClosed( "get()" );

        if ( valueAvailable )
        {
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing KeyTupleArrayCursor {}", this );
        }

        if ( wrapped != null )
        {
            wrapped.close();
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception reason )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing KeyTupleArrayCursor {}", this );
        }

        if ( wrapped != null )
        {
            wrapped.close();
        }

        super.close( reason );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "KeyTupleArrayCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#" ).append( key );

        sb.append( " :\n" );

        sb.append( wrapped.toString() );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
