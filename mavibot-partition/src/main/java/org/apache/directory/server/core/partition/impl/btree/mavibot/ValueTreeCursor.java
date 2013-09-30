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

package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.mavibot.btree.managed.BTree;
import org.apache.directory.server.i18n.I18n;


/**
 * TODO ValueTreeCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ValueTreeCursor<V> extends AbstractCursor<V>
{

    private org.apache.directory.mavibot.btree.Cursor<V, V> wrapped;

    private V available;

    // marker to detect the availability (cause Mavibot supports null values also)
    private V NOT_AVAILABLE = ( V ) new Object();


    public ValueTreeCursor( BTree<V, V> dupsTree )
    {
        try
        {
            this.wrapped = dupsTree.browse();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


    @Override
    public boolean available()
    {
        return ( available != NOT_AVAILABLE );
    }


    @Override
    public void before( V element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    @Override
    public void after( V element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    @Override
    public void beforeFirst() throws LdapException, CursorException
    {
    }


    @Override
    public void afterLast() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    @Override
    public boolean first() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    @Override
    public boolean last() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    @Override
    public boolean previous() throws LdapException, CursorException
    {
        try
        {
            if ( wrapped.hasPrev() )
            {
                available = wrapped.prev().getKey();
                return true;
            }
            else
            {
                available = NOT_AVAILABLE;
                return false;
            }
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public boolean next() throws LdapException, CursorException
    {
        try
        {
            if ( wrapped.hasNext() )
            {
                available = wrapped.next().getKey();
                return true;
            }
            else
            {
                available = NOT_AVAILABLE;
                return false;
            }
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public V get() throws CursorException
    {
        return available;
    }

}
