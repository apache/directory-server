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
import java.util.Arrays;
import java.util.Comparator;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.Serializer;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractTable;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeTable<K, V> extends AbstractTable<K, V>
{
    private Database db;
    private Serializer<K> keySerializer;
    private Serializer<V> valueSerializer;
    private final DatabaseEntry discaredVE = new DatabaseEntry();


    public BdbJeTable( Database db, SchemaManager schemaManager, Comparator<K> keyComparator,
        Serializer<K> keySerializer, Serializer<V> valueSerializer )
    {
        super( schemaManager, db.getDatabaseName(), keyComparator, null );
        UuidComparator.INSTANCE.setSchemaManager( schemaManager );
        this.db = db;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;

        this.allowsDuplicates = false;
    }


    public BdbJeTable( Database db, SchemaManager schemaManager,
        Comparator<K> keyComparator, Comparator<V> valueComparator,
        Serializer<K> keySerializer, Serializer<V> valueSerializer )
    {
        super( schemaManager, db.getDatabaseName(), keyComparator, valueComparator );
        UuidComparator.INSTANCE.setSchemaManager( schemaManager );
        this.db = db;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;

        this.allowsDuplicates = true;
    }


    @Override
    public boolean has( PartitionTxn transaction, K key ) throws LdapException
    {
        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            Transaction tx = getTxn( transaction );
            OperationStatus status = db.get( tx, de, discaredVE, LockMode.READ_COMMITTED );
            return ( status == SUCCESS );
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    @Override
    public boolean has( PartitionTxn transaction, K key, V value ) throws LdapException
    {
        if ( key == null || value == null )
        {
            return false;
        }

        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry( valueSerializer.serialize( value ) );
            Transaction tx = getTxn( transaction );
            OperationStatus status = db.getSearchBoth( tx, de, ve, LockMode.READ_COMMITTED );
            return ( status == SUCCESS );
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    @Override
    public boolean hasGreaterOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        try
        {
            byte[] keyBytes = keySerializer.serialize( key );
            DatabaseEntry de = new DatabaseEntry( keyBytes );
            Transaction tx = getTxn( transaction );
            CursorConfig cc = new CursorConfig();
            cc.setReadCommitted( true );
            com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
            OperationStatus status = cursor.getSearchKey( de, discaredVE, LockMode.DEFAULT );
            if ( status != SUCCESS )
            {
                status = cursor.getNext( de, discaredVE, LockMode.DEFAULT );
            }
            cursor.close();

            if ( status != SUCCESS )
            {
                return false;
            }

            if ( Arrays.equals( keyBytes, de.getData() ) )
            {
                return true;
            }

            K fetched = keySerializer.deserialize( de.getData() );
            int cmpVal = keyComparator.compare( fetched, key );
            return ( cmpVal > 0 );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
    }


    @Override
    public boolean hasLessOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        try
        {
            byte[] keyBytes = keySerializer.serialize( key );
            DatabaseEntry de = new DatabaseEntry( keyBytes );
            Transaction tx = getTxn( transaction );
            CursorConfig cc = new CursorConfig();
            cc.setReadCommitted( true );
            com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
            OperationStatus status = cursor.getSearchKey( de, discaredVE, LockMode.DEFAULT );
            if ( status != SUCCESS )
            {
                status = cursor.getPrev( de, discaredVE, LockMode.DEFAULT );
            }
            cursor.close();

            if ( status != SUCCESS )
            {
                return false;
            }

            if ( Arrays.equals( keyBytes, de.getData() ) )
            {
                return true;
            }

            K fetched = keySerializer.deserialize( de.getData() );
            int cmpVal = keyComparator.compare( fetched, key );
            return ( cmpVal == -1 );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
    }


    @Override
    public boolean hasGreaterOrEqual( PartitionTxn transaction, K key, V val ) throws LdapException
    {

        if ( key == null || val == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry();
            Transaction tx = getTxn( transaction );
            CursorConfig cc = new CursorConfig();
            cc.setReadCommitted( true );
            com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
            OperationStatus status = cursor.getSearchKey( de, ve, LockMode.DEFAULT );
            if ( status != SUCCESS )
            {
                cursor.close();
                return false;
            }

            boolean found = false;
            do
            {
                V fetched = valueSerializer.deserialize( ve.getData() );
                int cmpVal = valueComparator.compare( fetched, val );
                if ( cmpVal >= 0 )
                {
                    found = true;
                    break;
                }
            }
            while ( cursor.getNextDup( de, ve, LockMode.DEFAULT ) == SUCCESS );

            cursor.close();

            return found;
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
    }


    @Override
    public boolean hasLessOrEqual( PartitionTxn transaction, K key, V val ) throws LdapException
    {
        if ( key == null || val == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry();
            Transaction tx = getTxn( transaction );
            CursorConfig cc = new CursorConfig();
            cc.setReadCommitted( true );
            com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
            OperationStatus status = cursor.getSearchKey( de, ve, LockMode.DEFAULT );
            if ( status != SUCCESS )
            {
                cursor.close();
                return false;
            }

            boolean found = false;
            do
            {
                V fetched = valueSerializer.deserialize( ve.getData() );
                int cmpVal = valueComparator.compare( fetched, val );
                if ( cmpVal <= 0 )
                {
                    found = true;
                    break;
                }
            }
            while ( cursor.getNextDup( de, ve, LockMode.DEFAULT ) == SUCCESS );

            cursor.close();

            return found;
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
    }


    @Override
    public V get( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return null;
        }

        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry();
            Transaction tx = getTxn( transaction );
            OperationStatus status = db.get( tx, de, ve, LockMode.READ_COMMITTED );
            if ( status == SUCCESS )
            {
                return valueSerializer.deserialize( ve.getData() );
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }

        return null;
    }


    @Override
    public void put( PartitionTxn writeTransaction, K key, V value ) throws LdapException
    {
        if ( ( value == null ) || ( key == null ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_594 ) );
        }
        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry( valueSerializer.serialize( value ) );
            Transaction tx = getTxn( writeTransaction );
            OperationStatus status;
            if ( allowsDuplicates )
            {
                status = db.putNoDupData( tx, de, ve );
            }
            else
            {
                status = db.put( tx, de, ve );
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    @Override
    public void remove( PartitionTxn writeTransaction, K key ) throws LdapException
    {
        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            Transaction tx = getTxn( writeTransaction );
            db.delete( tx, de );
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    @Override
    public void remove( PartitionTxn writeTransaction, K key, V value )
        throws LdapException
    {
        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            Transaction tx = getTxn( writeTransaction );
            if ( allowsDuplicates )
            {
                DatabaseEntry ve = new DatabaseEntry( valueSerializer.serialize( value ) );
                CursorConfig cc = new CursorConfig();
                cc.setReadCommitted( true );
                com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
                OperationStatus status = cursor.getSearchBoth( de, ve, LockMode.DEFAULT );
                if ( status == SUCCESS )
                {
                    cursor.delete();
                }
                cursor.close();
            }
            else
            {
                db.delete( tx, de );
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    @Override
    public Cursor<Tuple<K, V>> cursor()
    {
        throw new UnsupportedOperationException( "cursor cannot be obtained without a transaction" );
    }


    public Cursor<Tuple<K, V>> cursor( PartitionTxn partitionTxn ) throws LdapException
    {
        Transaction tx = getTxn( partitionTxn );
        CursorConfig cc = new CursorConfig();
        cc.setReadCommitted( true );
        com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
        return new BdbJeTableCursor<>( cursor, keySerializer, valueSerializer, allowsDuplicates );
    }


    @Override
    public Cursor<Tuple<K, V>> cursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        Transaction tx = getTxn( partitionTxn );
        CursorConfig cc = new CursorConfig();
        cc.setReadCommitted( true );
        com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
        return new BdbJeTableCursor<>( cursor, keySerializer, valueSerializer, allowsDuplicates );
    }


    @Override
    public Cursor<V> valueCursor( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return new EmptyCursor<>();
        }

        try
        {
            Transaction tx = getTxn( transaction );
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            DatabaseEntry ve = new DatabaseEntry();
            if ( !allowsDuplicates )
            {
                OperationStatus status = db.get( tx, de, ve, LockMode.READ_COMMITTED );
                if ( status == OperationStatus.SUCCESS )
                {
                    V value = valueSerializer.deserialize( ve.getData() );
                    return new SingletonCursor<>( value );
                }
            }
            else
            {
                CursorConfig cc = new CursorConfig();
                cc.setReadCommitted( true );
                com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
                return new BdbJeTableDupKeyCursor<>( cursor, keySerializer, valueSerializer, key );
            }
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }

        return new EmptyCursor<>();
    }


    @Override
    public long count( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return 0;
        }

        Transaction tx = getTxn( transaction );
        try
        {
            DatabaseEntry de = new DatabaseEntry( keySerializer.serialize( key ) );
            OperationStatus status = db.get( tx, de, discaredVE, LockMode.READ_COMMITTED );
            if ( status == SUCCESS )
            {
                if ( allowsDuplicates )
                {
                    CursorConfig cc = new CursorConfig();
                    cc.setReadCommitted( true );
                    com.sleepycat.je.Cursor cursor = db.openCursor( tx, cc );
                    cursor.getSearchKey( de, discaredVE, LockMode.DEFAULT );
                    int c = cursor.count();
                    cursor.close();
                    return c;
                }
                else
                {
                    return 1;
                }
            }
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }

        return 0;
    }


    @Override
    public void close( PartitionTxn transaction ) throws LdapException
    {
        db.close();
    }


    @Override
    public long count( PartitionTxn transaction ) throws LdapException
    {
        return db.count();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long greaterThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        // take a best guess
        return Math.min( db.count(), 10L );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long lessThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        // take a best guess
        return Math.min( db.count(), 10L );
    }


    private Transaction getTxn( PartitionTxn transaction )
    {
        return ( ( JeTransaction ) transaction ).getTxn();
    }


    void printDb()
    {
        com.sleepycat.je.Cursor cursor = null;
        try
        {
            CursorConfig cc = new CursorConfig();
            cc.setReadCommitted( true );
            cursor = db.openCursor( null, cc );
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            while ( cursor.getNext( key, value, LockMode.DEFAULT ) == OperationStatus.SUCCESS )
            {
                Object k = keySerializer.deserialize( key.getData() );
                Object v = valueSerializer.deserialize( value.getData() );

                System.out.println( k + " = " + v );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }
}
