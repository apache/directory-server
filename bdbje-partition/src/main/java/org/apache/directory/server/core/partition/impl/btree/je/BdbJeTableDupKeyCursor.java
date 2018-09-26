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

package org.apache.directory.server.core.partition.impl.btree.je;


import static com.sleepycat.je.OperationStatus.SUCCESS;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.Serializer;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeTableDupKeyCursor<K, V> extends AbstractCursor<V>
{
    private DatabaseEntry ke = new DatabaseEntry();
    private DatabaseEntry ve = new DatabaseEntry();

    private com.sleepycat.je.Cursor wrapped;
    private Serializer<V> valueSerializer;

    private boolean bf;

    /**
     * Whether or not a value is available when get() is called.
     */
    private OperationStatus status;


    public BdbJeTableDupKeyCursor( com.sleepycat.je.Cursor wrapped, Serializer<K> keySerializer,
        Serializer<V> valueSerializer, K key ) throws IOException
    {
        this.wrapped = wrapped;
        this.valueSerializer = valueSerializer;
        ke.setData( keySerializer.serialize( key ) );
    }


    @Override
    public boolean available()
    {
        return ( status == SUCCESS );
    }


    @Override
    public void before( V element ) throws LdapException, CursorException
    {
        try
        {
            ve.setData( valueSerializer.serialize( element ) );
            status = wrapped.getSearchBoth( ke, ve, LockMode.DEFAULT );
            if ( status == SUCCESS )
            {
                status = wrapped.getPrevDup( ke, ve, LockMode.DEFAULT );
            }
        }
        catch ( Exception e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public void after( V element ) throws LdapException, CursorException
    {
        try
        {
            ve.setData( valueSerializer.serialize( element ) );
            status = wrapped.getSearchBoth( ke, ve, LockMode.DEFAULT );
            if ( status == SUCCESS )
            {
                status = wrapped.getNextDup( ke, ve, LockMode.DEFAULT );
            }
        }
        catch ( Exception e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public void beforeFirst() throws LdapException, CursorException
    {
        status = wrapped.getSearchKey( ke, ve, LockMode.DEFAULT );
        bf = true;
    }


    @Override
    public void afterLast() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( "afterlast is not supported on a duplicate key cursor" );
    }


    @Override
    public boolean first() throws LdapException, CursorException
    {
        if ( bf )
        {
            bf = false;
            return bf;
        }
        status = wrapped.getSearchKey( ke, ve, LockMode.DEFAULT );
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public boolean last() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( "navigating to last is not supported" );
    }


    @Override
    public boolean previous() throws LdapException, CursorException
    {
        status = wrapped.getPrevDup( ke, ve, LockMode.DEFAULT );
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public boolean next() throws LdapException, CursorException
    {
        status = wrapped.getNextDup( ke, ve, LockMode.DEFAULT );
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public V get() throws CursorException
    {
        if ( status != SUCCESS )
        {
            throw new InvalidCursorPositionException();
        }

        try
        {
            V value = valueSerializer.deserialize( ve.getData() );
            return value;
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public void close( Exception cause ) throws IOException
    {
        wrapped.close();
        super.close( cause );
    }


    @Override
    public void close() throws IOException
    {
        wrapped.close();
        super.close();
    }
}
