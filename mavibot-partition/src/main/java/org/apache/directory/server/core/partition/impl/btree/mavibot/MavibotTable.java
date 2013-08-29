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

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.BTree;
import org.apache.directory.mavibot.btree.DuplicateKeyVal;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.exception.BTreeAlreadyManagedException;
import org.apache.directory.mavibot.btree.exception.KeyNotFoundException;
import org.apache.directory.mavibot.btree.serializer.ElementSerializer;
import org.apache.directory.server.core.avltree.ArrayMarshaller;
import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotTable<K, V> extends AbstractTable<K, V>
{

    private BTree<K, V> bt;

    private ArrayMarshaller<V> arrayMarshaller;

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotTable.class );

    protected RecordManager recordMan;


    public MavibotTable( RecordManager recordMan, SchemaManager schemaManager, String name,
        ElementSerializer<K> keySerializer, ElementSerializer<V> valueSerializer, boolean allowDuplicates )
        throws IOException
    {
        super( schemaManager, name, keySerializer.getComparator(), valueSerializer.getComparator() );
        this.recordMan = recordMan;

        bt = recordMan.getManagedTree( name );

        if ( bt == null )
        {
            bt = new BTree<K, V>( name, keySerializer, valueSerializer, allowDuplicates );

            try
            {
                recordMan.manage( bt );
            }
            catch ( BTreeAlreadyManagedException e )
            {
                // should never happen
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
        arrayMarshaller = new ArrayMarshaller<V>( valueComparator );

        // Initialize the count
        count = bt.getNbElems();
    }


    @Override
    public boolean isDupsEnabled()
    {
        return allowsDuplicates;
    }


    @Override
    public boolean has( K key ) throws IOException
    {
        return bt.hasKey( key );
    }


    @Override
    public boolean has( K key, V value ) throws LdapException
    {
        try
        {
            return bt.contains( key, value );
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }


    @Override
    public boolean hasGreaterOrEqual( K key ) throws Exception
    {
        org.apache.directory.mavibot.btree.Cursor<K, V> cursor = null;
        try
        {
            cursor = bt.browseFrom( key );

            return cursor.hasNext();
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    @Override
    public boolean hasLessOrEqual( K key ) throws Exception
    {
        org.apache.directory.mavibot.btree.Cursor<K, V> cursor = null;

        try
        {
            cursor = bt.browseFrom( key );

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
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    @Override
    public boolean hasGreaterOrEqual( K key, V val ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        org.apache.directory.mavibot.btree.Cursor<V, V> cursor = null;

        try
        {
            if ( !bt.hasKey( key ) )
            {
                return false;
            }

            DuplicateKeyVal<V> dupHolder = bt.getValues( key );
            if( dupHolder.isSingleValue() )
            {
                int equal = bt.getValueSerializer().compare( val, dupHolder.getSingleValue() );
                return ( equal == 0 );
            }
            else
            {
                BTree<V, V> dups = dupHolder.getSubTree();
                
                cursor = dups.browseFrom( val );
                
                return cursor.hasNext();
            }
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    @Override
    public boolean hasLessOrEqual( K key, V val ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }

        if ( !allowsDuplicates )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_593 ) );
        }

        if ( !bt.hasKey( key ) )
        {
            return false;
        }

        DuplicateKeyVal<V> dupHolder = bt.getValues( key );

        if( dupHolder.isSingleValue() )
        {
            return true;
        }
            
        BTree<V, V> dups = dupHolder.getSubTree();

        org.apache.directory.mavibot.btree.Cursor<V, V> cursor = null;

        try
        {
            cursor = dups.browseFrom( val );

            org.apache.directory.mavibot.btree.Tuple<V, V> tuple = null;

            if ( cursor.hasNext() )
            {
                tuple = cursor.next();
            }

            // Test for equality first since it satisfies both greater/less than
            if ( null != tuple && keyComparator.compare( ( K ) tuple.getKey(), key ) == 0 )
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
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    @Override
    public V get( K key ) throws LdapException
    {
        if ( key == null )
        {
            return null;
        }

        try
        {
            return bt.get( key );
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


    @Override
    public void put( K key, V value ) throws Exception
    {
        try
        {
            if ( ( value == null ) || ( key == null ) )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_594 ) );
            }

            V replaced = bt.insert( key, value );

            if ( replaced == null )
            {
                count++;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_131, key, name ), e );
            throw e;
        }
    }


    @Override
    public void remove( K key ) throws Exception
    {
        try
        {
            if ( key == null )
            {
                return;
            }

            org.apache.directory.mavibot.btree.Tuple<K, V> returned = bt.delete( key );

            if ( null == returned )
            {
                return;
            }

            this.count--;
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_133, key, name ), e );

            if ( e instanceof IOException )
            {
                throw ( IOException ) e;
            }
        }
    }


    @Override
    public void remove( K key, V value ) throws Exception
    {
        try
        {
            if ( key == null )
            {
                return;
            }

            org.apache.directory.mavibot.btree.Tuple<K, V> t = bt.delete( key, value );

            if ( t != null )
            {
                count--;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_132, key, value, name ), e );
        }
    }


    @Override
    public Cursor<Tuple<K, V>> cursor() throws LdapException
    {
        return new MavibotCursor<K, V>( this );
    }


    @Override
    public Cursor<Tuple<K, V>> cursor( K key ) throws LdapException
    {
        if ( key == null )
        {
            return new EmptyCursor<Tuple<K, V>>();
        }

        try
        {
            if ( !allowsDuplicates )
            {
                V val = bt.get( key );

                return new SingletonCursor<Tuple<K, V>>(
                    new Tuple<K, V>( key, val ) );
            }
            else
            {
                DuplicateKeyVal<V> dupHolder = bt.getValues( key );
                if( dupHolder.isSingleValue() )
                {
                    return new SingletonCursor<Tuple<K,V>>( new Tuple<K, V>( key, dupHolder.getSingleValue() ) );
                }
                
                BTree<V, V> dups = dupHolder.getSubTree();

                return new KeyTupleArrayCursor<K, V>( dups, key );
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            return new EmptyCursor<Tuple<K, V>>();
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    @Override
    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<V>();
        }

        try
        {
            if ( !allowsDuplicates )
            {
                V val = bt.get( key );

                return new SingletonCursor<V>( val );
            }
            else
            {
                DuplicateKeyVal<V> dupHolder = bt.getValues( key );
                if( dupHolder.isSingleValue() )
                {
                    return new SingletonCursor<V>( dupHolder.getSingleValue() );
                }
                else
                {
                    return new ValueTreeCursor<V>( dupHolder.getSubTree() );
                }
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            return new EmptyCursor<V>();
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    @Override
    public long count( K key ) throws Exception
    {
        if ( key == null )
        {
            return 0;
        }

        try
        {
            if ( bt.isAllowDuplicates() )
            {
                DuplicateKeyVal<V> dupHolder = bt.getValues( key );
                if( dupHolder.isSingleValue() )
                {
                    return 1;
                }
                
                BTree<V, V> values = dupHolder.getSubTree();

                return values.getNbElems();
            }
            else
            {
                bt.get( key );

                return 1;
            }
        }
        catch ( KeyNotFoundException knfe )
        {
            return 0L;
        }

        /*
        if ( bt.hasKey( key ) )
        {
            if ( !allowsDuplicates )
            {
                return 1L;
            }
            else
            {
                BTree<V, V> values = bt.getValues( key );
                return values.getNbElems();
            }
        }

        return 0;
        */
    }


    @Override
    public long greaterThanCount( K key ) throws Exception
    {
        // take a best guess
        return Math.min( count, 10L );
    }


    @Override
    public long lessThanCount( K key ) throws Exception
    {
        // take a best guess
        return Math.min( count, 10L );
    }


    @Override
    public void close() throws Exception
    {
        // do nothing here, the RecordManager will be closed in MavibotMasterTable.close()
    }


    public ArrayTree<V> getDupsContainer( byte[] serialized ) throws IOException
    {
        if ( serialized == null )
        {
            return new ArrayTree<V>( valueComparator );
        }

        return arrayMarshaller.deserialize( serialized );
    }


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
}
