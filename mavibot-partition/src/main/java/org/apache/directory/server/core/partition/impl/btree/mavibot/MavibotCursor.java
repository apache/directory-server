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

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cursor over the Tuples of a Mavibot BTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class MavibotCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private final MavibotTable<K, V> table;

    private Tuple<K, V> returnedTuple = new Tuple<K, V>();
    private boolean valueAvailable;

    private org.apache.mavibot.btree.Cursor<K, V> browser;

    /**
     * Creates a Cursor over the tuples of a Mavibot table.
     *
     * @param table the JDBM Table to build a Cursor over
     * @throws IOException of there are problems accessing the BTree
     */
    public MavibotCursor( MavibotTable<K, V> table )
    {
        LOG_CURSOR.debug( "Creating MavibotCursor {}", this );
        this.table = table;
    }


    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void beforeKey( K key ) throws LdapException, CursorException, IOException
    {
        checkNotClosed( "beforeKey()" );
        closeBrowser( browser );
        browser = table.getBTree().browseFrom( key );

        clearValue();
    }


    @SuppressWarnings("unchecked")
    public void afterKey( K key ) throws LdapException, CursorException, IOException
    {
        checkNotClosed( "afterKey()" );
        closeBrowser( browser );
        browser = table.getBTree().browseFrom( key );
        
        if( table.isDupsEnabled() )
        {
            browser.moveToNextNonDuplicateKey();
        }
        else
        {
            if( browser.hasNext() )
            {
                browser.next();
            }
            else
            {
                browser.afterLast();
            }
        }
        
        clearValue();
    }


    public void beforeValue( K key, V value ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_596 ) );
    }


    public void afterValue( K key, V value ) throws LdapException, CursorException, IOException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_596 ) );
    }


    /**
     * Positions this Cursor before the key of the supplied tuple.
     *
     * @param element the tuple who's key is used to position this Cursor
     * @throws IOException if there are failures to position the Cursor
     */
    public void before( Tuple<K, V> element )throws LdapException, CursorException, IOException
    {
        beforeKey( element.getKey() );
    }


    public void after( Tuple<K, V> element )throws LdapException, CursorException, IOException
    {
        afterKey( element.getKey() );
    }


    public void beforeFirst() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "beforeFirst()" );
        
        if( browser == null )
        {
            browser = table.getBTree().browse();
        }
        
        browser.beforeFirst();
        clearValue();
    }


    public void afterLast() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "afterLast()" );
        
        if( browser == null )
        {
            browser = table.getBTree().browse();
        }
        
        browser.afterLast();
        clearValue();
    }


    public boolean first() throws LdapException, CursorException, IOException
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws LdapException, CursorException, IOException
    {
        afterLast();
        return previous();
    }


    @SuppressWarnings("unchecked")
    public boolean previous() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "previous()" );
        if ( browser == null )
        {
            afterLast();
        }

        if ( browser.hasPrev() )
        {
        	org.apache.mavibot.btree.Tuple<K, V> tuple = browser.prev();

            returnedTuple.setKey( tuple.getKey() );
            returnedTuple.setValue( ( V ) tuple.getValue() );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    public boolean next() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "previous()" );

        if ( browser == null )
        {
            beforeFirst();
        }

        if ( browser.hasNext() )
        {
        	org.apache.mavibot.btree.Tuple<K, V> tuple = browser.next();
        	
            returnedTuple.setKey( tuple.getKey() );
            returnedTuple.setValue( tuple.getValue() );
            return valueAvailable = true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    public Tuple<K, V> get() throws CursorException, IOException
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
    @Override
    public void close()
    {
        LOG_CURSOR.debug( "Closing MavibotCursor {}", this );
        super.close();
        closeBrowser( browser );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause )
    {
        LOG_CURSOR.debug( "Closing MavibotCursor {}", this );
        super.close( cause );
        closeBrowser( browser );
    }


    private void closeBrowser( org.apache.mavibot.btree.Cursor<K, V> browser )
    {
        if ( browser != null )
        {
            browser.close();
        }
    }
}
