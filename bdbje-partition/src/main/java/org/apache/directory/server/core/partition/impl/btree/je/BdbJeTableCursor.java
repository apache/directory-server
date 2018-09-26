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


import static com.sleepycat.je.OperationStatus.NOTFOUND;
import static com.sleepycat.je.OperationStatus.SUCCESS;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.Serializer;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeTableCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    /**
     * Whether or not a value is available when get() is called.
     */
    private OperationStatus status;

    private DatabaseEntry ke = new DatabaseEntry();
    private DatabaseEntry ve = new DatabaseEntry();

    private com.sleepycat.je.Cursor wrapped;
    private Serializer<K> keySerializer;
    private Serializer<V> valueSerializer;
    private boolean allowsDuplicates;

    private boolean bf;
    private boolean al;


    public BdbJeTableCursor( com.sleepycat.je.Cursor wrapped, Serializer<K> keySerializer,
        Serializer<V> valueSerializer, boolean allowsDuplicates )
    {
        this.wrapped = wrapped;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.allowsDuplicates = allowsDuplicates;
    }


    @Override
    public boolean available()
    {
        return ( status == SUCCESS );
    }


    @Override
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        try
        {
            ke.setData( keySerializer.serialize( element.getKey() ) );
            if ( allowsDuplicates )
            {
                ve.setData( valueSerializer.serialize( element.getValue() ) );
                wrapped.getSearchBoth( ke, ve, LockMode.DEFAULT );
                wrapped.getPrevDup( ke, ve, LockMode.DEFAULT );
            }
            else
            {
                wrapped.getSearchKey( ke, ve, LockMode.DEFAULT );
                wrapped.getPrev( ke, ve, LockMode.DEFAULT );
            }
        }
        catch ( Exception e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        try
        {
            ke.setData( keySerializer.serialize( element.getKey() ) );
            if ( allowsDuplicates )
            {
                ve.setData( valueSerializer.serialize( element.getValue() ) );
                wrapped.getSearchBoth( ke, ve, LockMode.DEFAULT );
                wrapped.getNextDup( ke, ve, LockMode.DEFAULT );
            }
            else
            {
                wrapped.getSearchKey( ke, ve, LockMode.DEFAULT );
                wrapped.getNext( ke, ve, LockMode.DEFAULT );
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
        al = false;
        status = wrapped.getFirst( ke, ve, LockMode.DEFAULT );
        bf = true;
    }


    @Override
    public void afterLast() throws LdapException, CursorException
    {
        bf = false;
        status = wrapped.getLast( ke, ve, LockMode.DEFAULT );
        al = true;
    }


    @Override
    public boolean first() throws LdapException, CursorException
    {
        if ( bf )
        {
            bf = false;
            al = false;
            return bf;
        }
        status = wrapped.getFirst( ke, ve, LockMode.DEFAULT );
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public boolean last() throws LdapException, CursorException
    {
        if ( al )
        {
            al = false;
            bf = false;
            return al;
        }
        status = wrapped.getLast( ke, ve, LockMode.DEFAULT );
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public boolean previous() throws LdapException, CursorException
    {
        if ( al )
        {
            al = false;
            return ( status == OperationStatus.SUCCESS );
        }
        else if ( bf )
        {
            return false;
        }

        if ( allowsDuplicates )
        {
            status = wrapped.getPrevDup( ke, ve, LockMode.DEFAULT );
            if ( status == NOTFOUND )
            {
                status = wrapped.getPrev( ke, ve, LockMode.DEFAULT );
            }
        }
        else
        {
            status = wrapped.getPrev( ke, ve, LockMode.DEFAULT );
        }
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public boolean next() throws LdapException, CursorException
    {
        if ( bf )
        {
            bf = false;
            return ( status == OperationStatus.SUCCESS );
        }
        else if ( al )
        {
            return false;
        }

        if ( allowsDuplicates )
        {
            status = wrapped.getNextDup( ke, ve, LockMode.DEFAULT );
            if ( status == NOTFOUND )
            {
                status = wrapped.getNext( ke, ve, LockMode.DEFAULT );
            }
        }
        else
        {
            status = wrapped.getNext( ke, ve, LockMode.DEFAULT );
        }
        return ( status == OperationStatus.SUCCESS );
    }


    @Override
    public Tuple<K, V> get() throws CursorException
    {
        if ( status != SUCCESS )
        {
            throw new InvalidCursorPositionException();
        }

        try
        {
            K key = keySerializer.deserialize( ke.getData() );
            V value = valueSerializer.deserialize( ve.getData() );
            Tuple<K, V> tuple = new Tuple<>( key, value );
            return tuple;
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
