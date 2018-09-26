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


import java.io.IOException;
import java.util.Iterator;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.Serializer;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;


/**
 * An IndexCursor for BdbJeIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ForwardBdbJeIndexCursor<K> extends AbstractIndexCursor<K>
{

    private static final Logger LOG = LoggerFactory.getLogger( ForwardBdbJeIndexCursor.class );

    /** the underlying cursor */
    private Cursor wrapped;

    /** search key */
    private K searchKey;

    /** key serializer */
    private Serializer<K> keySerializer;

    /** value serializer */
    private Serializer<String> valueSerializer;

    /** container to store the key */
    private DatabaseEntry key;

    /** container to store the value */
    private DatabaseEntry value;

    private OperationStatus status;

    private IndexEntry<K, String> entry;

    private IndexEntry<K, String> nextEntry, prevEntry;

    private boolean closed;

    //private boolean forward;

    /**
     * this is the flag used to avoid java.lang.IllegalStateException: Cursor not initialized.
     * when the given searchKey is not found and we call previous() after next()
     */
    private boolean keyDoesNotExist;


    public ForwardBdbJeIndexCursor( com.sleepycat.je.Cursor wrapped, Serializer<K> keySerializer,
        Serializer<String> valueSerializer, boolean forward ) throws LdapException
    {
        this( wrapped, keySerializer, valueSerializer, forward, null );
    }


    public ForwardBdbJeIndexCursor( com.sleepycat.je.Cursor wrapped, Serializer<K> keySerializer,
        Serializer<String> valueSerializer, boolean forward, K searchKey ) throws LdapException
    {
        this.wrapped = wrapped;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.searchKey = searchKey;
        //this.forward = forward;

        key = new DatabaseEntry();
        value = new DatabaseEntry();

        if ( searchKey != null )
        {
            try
            {
                key.setData( keySerializer.serialize( searchKey ) );
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
        }
    }


    public boolean next() throws LdapException
    {
        if ( nextEntry != null )
        {
            entry = nextEntry;
            nextEntry = null;

            return true;
        }

        OperationStatus tmpStatus = status;

        if ( searchKey != null )
        {
            if ( tmpStatus == null ) //first search
            {
                tmpStatus = wrapped.getSearchKey( key, value, LockMode.DEFAULT );
            }
            else
            {
                tmpStatus = wrapped.getNextDup( key, value, LockMode.DEFAULT );
            }
        }
        else
        {
            tmpStatus = wrapped.getNext( key, value, LockMode.DEFAULT );
        }

        if ( ( status == null ) && ( tmpStatus == OperationStatus.NOTFOUND ) )
        {
            //to reproduce this IllegalStateException comment the below line
            keyDoesNotExist = true;
        }

        status = tmpStatus;

        boolean next = false;

        if ( status == OperationStatus.SUCCESS )
        {
            next = true;

            readData();
        }
        else
        {
            prevEntry = entry;
            entry = null;
        }

        return next;
    }


    public boolean previous() throws LdapException
    {
        // if previous is the first call return false
        if ( status == null || keyDoesNotExist )
        {
            return false;
        }

        if ( prevEntry != null )
        {
            entry = prevEntry;
            prevEntry = null;
            return true;
        }

        OperationStatus tmpStatus;

        if ( searchKey != null )
        {
            tmpStatus = wrapped.getPrevDup( key, value, LockMode.DEFAULT );
        }
        else
        {
            tmpStatus = wrapped.getPrev( key, value, LockMode.DEFAULT );
        }

        if ( tmpStatus == OperationStatus.SUCCESS )
        {
            status = tmpStatus;
            readData();
            return true;
        }
        else
        {
            nextEntry = entry;
            entry = null;
        }

        return false;
    }


    private void readData() throws LdapException
    {
        try
        {
            K k = keySerializer.deserialize( key.getData() );
            String val = valueSerializer.deserialize( value.getData() );
            entry = new IndexEntry<K, String>();
            // need to set in reverse way for forward
            entry.setId( val );
            entry.setKey( k );
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    public IndexEntry<K, String> get()
    {
        return entry;
    }


    public boolean available()
    {
        return ( entry != null );
    }


    public void close()
    {
        closed = true;
        wrapped.close();
    }


    public void close( Exception arg0 )
    {
        close();
    }


    public boolean isElementReused()
    {
        return false;
    }


    public void beforeFirst() throws LdapException
    {
        // just ignore this call, cause the cursor will be first anyway before using
        // one more reason is at many places beforeFirst() is called
        // on the Cursor interface because of some issues in the way JDBM backed
        // cursors position themselves after opening
    }


    public boolean first() throws LdapException
    {
        throw new UnsupportedOperationException();
    }


    public boolean isAfterLast()
    {
        throw new UnsupportedOperationException();
    }


    public boolean isBeforeFirst()
    {
        throw new UnsupportedOperationException();
    }


    public boolean isFirst()
    {
        throw new UnsupportedOperationException();
    }


    public boolean isLast()
    {
        throw new UnsupportedOperationException();
    }


    public boolean last()
    {
        throw new UnsupportedOperationException();
    }


    public Iterator<IndexEntry<K, String>> iterator()
    {
        throw new UnsupportedOperationException();
    }


    public void beforeValue( K id, String indexValue ) throws Exception
    {
        //throw new NotImplementedException( "beforeValue( ID, V ) not yet supported" );
    }


    public void afterValue( K id, String indexValue ) throws Exception
    {
        throw new NotImplementedException( "afterValue( ID, V ) not yet supported" );
    }


    public void after( IndexEntry<K, String> arg0 ) throws LdapException
    {
        throw new NotImplementedException( "after( IndexEntry<V, ID> ) not yet supported" );
    }


    public void afterLast() throws LdapException
    {
        throw new NotImplementedException( "afterLast() not yet supported" );
    }


    public void before( IndexEntry<K, String> arg0 ) throws LdapException
    {
        throw new NotImplementedException( "before( IndexEntry<V, ID> ) not yet supported" );
    }


    @Override
    protected String getUnsupportedMessage()
    {
        return "";
    }
}
