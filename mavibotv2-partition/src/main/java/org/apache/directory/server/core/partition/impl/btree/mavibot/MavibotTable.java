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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.IOException;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.BTree;
import org.apache.directory.mavibot.btree.BTreeFactory;
import org.apache.directory.mavibot.btree.InsertResult;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.Transaction;
import org.apache.directory.mavibot.btree.TupleCursor;
import org.apache.directory.mavibot.btree.ValueCursor;
import org.apache.directory.mavibot.btree.WriteTransaction;
import org.apache.directory.mavibot.btree.exception.BTreeAlreadyManagedException;
import org.apache.directory.mavibot.btree.exception.KeyNotFoundException;
import org.apache.directory.mavibot.btree.serializer.ElementSerializer;
import org.apache.directory.mavibot.btree.serializer.LongSerializer;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.avltree.ArrayMarshaller;
import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.helper.Serializer;


/**
 * A Mavibot Table. It extends the default Apache DS Table, when Mavibot is the
 * underlying database.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotTable<K, V> extends AbstractTable<K, V>
{
    /** the underlying B-tree */
    private BTree<K, V> bt;

    /** The marshaller that will be used to read the values when we have more than one */
    private ArrayMarshaller<V> arrayMarshaller;

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotTable.class );

    /** The used recordManager */
    protected RecordManager recordMan;

    /** the limit at which we start using btree redirection for duplicates */
    private int numDupLimit = MavibotIndex.DEFAULT_DUPLICATE_LIMIT;

    /** a cache of duplicate BTrees */
    private final Map<Long, BTree<K, V>> duplicateBtrees;

    /** A value serializer */
    private final ElementSerializer valueSerializer;


    /**
     * Creates a new instance of MavibotTable.
     *
     * @param recordMan The associated RecordManager
     * @param schemaManager The SchemaManager
     * @param name The Table name
     * @param keySerializer The Key serializer
     * @param valueSerializer The Value serializer
     * @param allowDuplicates If the table allows duplicate values
     * @throws IOException If the instance creation failed
     */
    public MavibotTable( RecordManager recordMan, SchemaManager schemaManager, String name,
        ElementSerializer<K> keySerializer, ElementSerializer<V> valueSerializer, boolean allowDuplicates )
        throws IOException
    {
        this( recordMan, schemaManager, name, keySerializer, valueSerializer, allowDuplicates,
            AbstractBTreePartition.DEFAULT_CACHE_SIZE );
    }


    /**
     * Creates a new instance of MavibotTable.
     *
     * @param recordMan The associated RecordManager
     * @param schemaManager The SchemaManager
     * @param name The Table name
     * @param keySerializer The Key serializer
     * @param valueSerializer The Value serializer
     * @param allowDuplicates If the table allows duplicate values
     * @param cacheSize The cache size to use
     * @throws IOException If the instance creation failed
     */
    public MavibotTable( RecordManager recordMan, SchemaManager schemaManager, String name,
        ElementSerializer<K> keySerializer, ElementSerializer<V> valueSerializer, boolean allowDuplicates, int cacheSize )
        throws IOException
    {
        super( schemaManager, name, keySerializer.getComparator(), valueSerializer.getComparator() );
        this.recordMan = recordMan;
        this.valueSerializer = valueSerializer;
        duplicateBtrees = null;

        try ( Transaction transaction = recordMan.beginReadTransaction() )
        {
            bt = transaction.getBTree( name );
        }

        if ( bt == null )
        {
            // Create a new BTree
            WriteTransaction writeTransaction = recordMan.beginWriteTransaction();
                
            try
            {
                bt = recordMan.addBTree( writeTransaction, name, keySerializer, valueSerializer );
                writeTransaction.commit();
            }
            catch ( Exception e )
            {
                writeTransaction.abort();
                throw new RuntimeException( e );
            }
        }
        else
        {
            // it is important to set the serializers cause serializers will contain default
            // comparators when loaded from disk and we need schema aware comparators in certain indices
            bt.setKeySerializer( keySerializer );
            bt.setValueSerializer( valueSerializer );
        }

        this.allowsDuplicates = allowDuplicates;
        arrayMarshaller = new ArrayMarshaller<>( valueComparator );

        // Initialize the count
        count = bt.getNbElems();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has( PartitionTxn transaction, K key ) throws LdapException
    {
        try
        {
            return bt.hasKey( ( ( MavibotTxn ) transaction ).getTransaction(), key );
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has( PartitionTxn transaction, K key, V value ) throws LdapException
    {
        try
        {
            return bt.contains( ( ( MavibotTxn ) transaction ).getTransaction(), key, value );
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasGreaterOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        TupleCursor<K, V> cursor = null;

        try
        {
            cursor = bt.browseFrom( ( ( MavibotTxn ) transaction ).getTransaction(), key );

            return cursor.hasNext();
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLessOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        TupleCursor<K, V> cursor = null;

        try
        {
            cursor = bt.browseFrom( ( ( MavibotTxn ) transaction ).getTransaction(), key );

            org.apache.directory.mavibot.btree.Tuple<K, V> tuple = null;

            if ( cursor.hasNext() )
            {
                tuple = cursor.next();
            }

            // Test for equality first since it satisfies both greater/less than
            if ( null != tuple && keyComparator.compare( tuple.getKey(), key ) == 0 )
            {
                return true;
            }

            if ( null == tuple )
            {
                return count > 0;
            }
            else
            {
                if ( cursor.hasPrev() )
                {
                    return true;
                }
            }

            return false;
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasGreaterOrEqual( PartitionTxn transaction, K key, V val ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        ValueCursor<V> valueCursor = null;

        try
        {
            if ( !bt.hasKey( ( ( MavibotTxn ) transaction ).getTransaction(), key ) )
            {
                return false;
            }

            valueCursor = bt.getValues( ( ( MavibotTxn ) transaction ).getTransaction(), key );

            int equal = bt.getValueSerializer().compare( val, valueCursor.next() );

            return ( equal >= 0 );
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
        finally
        {
            if ( valueCursor != null )
            {
                valueCursor.close();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLessOrEqual( PartitionTxn transaction, K key, V val ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        if ( !bt.hasKey( ( ( MavibotTxn ) transaction ).getTransaction(), key ) )
        {
            return false;
        }

        ValueCursor<V> dupHolder = bt.getValues( key );

        return dupHolder.hasNext();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public V get( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return null;
        }

        try
        {
            return bt.get( ( ( MavibotTxn ) transaction ).getTransaction(), key );
        }
        catch ( KeyNotFoundException knfe )
        {
            return null;
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void put( PartitionWriteTxn transaction, K key, V value ) throws LdapException
    {
        try
        {
            if ( ( value == null ) || ( key == null ) )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_594 ) );
            }

            InsertResult<K, V> existingVal = bt.insert( ( ( MavibotWriteTxn ) transaction ).getWriteTransaction(), key, value );

            if ( existingVal == null )
            {
                count++;
            }
        }
        catch ( IOException ioe )
        {
            LOG.error( I18n.err( I18n.ERR_131, key, name ), ioe );
            throw new LdapOtherException( ioe.getMessage(), ioe);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( PartitionWriteTxn transaction, K key ) throws LdapException
    {
        try
        {
            if ( key == null )
            {
                return;
            }

            // Get the associated valueHolder
            if ( allowsDuplicates )
            {
                ValueCursor<V> valueCursor = bt.getValues( ( ( MavibotWriteTxn ) transaction ).getWriteTransaction(), key );
                int size = valueCursor.size();
                valueCursor.close();
                org.apache.directory.mavibot.btree.Tuple<K, V> returned = 
                    bt.delete( ( ( MavibotWriteTxn ) transaction ).getWriteTransaction(), key );

                if ( null == returned )
                {
                    return;
                }

                count -= size;
            }
            else
            {
                org.apache.directory.mavibot.btree.Tuple<K, V> returned = 
                    bt.delete( ( ( MavibotWriteTxn ) transaction ).getWriteTransaction(), key );

                if ( null == returned )
                {
                    return;
                }

                count--;
            }
        }
        catch ( IOException ioe )
        {
            LOG.error( I18n.err( I18n.ERR_133, key, name ), ioe );

            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( PartitionWriteTxn transaction, K key, V value ) throws LdapException
    {
        try
        {
            if ( key == null )
            {
                return;
            }

            org.apache.directory.mavibot.btree.Tuple<K, V> tuple = 
                bt.delete( ( ( MavibotWriteTxn ) transaction ).getWriteTransaction(), key, value );

            // We decrement the counter only when the key was found
            if ( tuple != null )
            {
                count--;
            }
        }
        catch ( IOException ioe )
        {
            LOG.error( I18n.err( I18n.ERR_132, key, value, name ), ioe );

            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<Tuple<K, V>> cursor( PartitionTxn transaction ) throws LdapException
    {
        return new MavibotCursor<>( this );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<Tuple<K, V>> cursor( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return new EmptyCursor<>();
        }

        try
        {
            if ( !allowsDuplicates )
            {
                V val = bt.get( key );

                return new SingletonCursor<>( new Tuple<K, V>( key, val ) );
            }
            else
            {
                ValueCursor<V> dupHolder = bt.getValues( key );

                return new KeyTupleValueCursor<>( dupHolder, key );
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            return new EmptyCursor<>();
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<V> valueCursor( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return new EmptyCursor<>();
        }

        try
        {
            if ( !allowsDuplicates )
            {
                V val = bt.get( ( ( MavibotTxn ) transaction ).getTransaction(), key );

                return new SingletonCursor<>( val );
            }
            else
            {
                ValueCursor<V> dupCursor = bt.getValues( ( ( MavibotTxn ) transaction ).getTransaction(), key );

                return new ValueTreeCursor<>( dupCursor );
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            return new EmptyCursor<>();
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long count( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return 0;
        }

        try
        {
            if ( allowsDuplicates )
            {
                try
                {
                    ValueCursor dupHolder = (ValueCursor) bt.get( ( ( MavibotTxn ) transaction ).getTransaction(), key );
                    int size = dupHolder.size();
                    dupHolder.close();
    
                    return size;
                }
                catch ( KeyNotFoundException knfe )
                {
                    // No key
                    return 0;
                }
            }
            else
            {
                if ( bt.hasKey( ( ( MavibotTxn ) transaction ).getTransaction(), key ) )
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public ArrayTree<V> getDupsContainer( byte[] serialized ) throws IOException
    {
        if ( serialized == null )
        {
            return new ArrayTree<V>( valueComparator );
        }

        return arrayMarshaller.deserialize( serialized );
    }


    /**
     * @return the underlying B-tree
     */
    protected BTree<K, V> getBTree()
    {
        return bt;
    }


    /**
     * Synchronizes the buffers with disk.
     *
     * @throws IOException if errors are encountered on the flush
     */
    public synchronized void sync() throws IOException
    {
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Mavibot table :\n" ).append( super.toString() );

        return sb.toString();
    }
}
