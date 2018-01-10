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


import java.util.Comparator;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.avltree.AvlSingletonOrOrderedSetCursor;
import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.AvlTreeMap;
import org.apache.directory.server.core.avltree.AvlTreeMapImpl;
import org.apache.directory.server.core.avltree.AvlTreeMapNoDupsWrapperCursor;
import org.apache.directory.server.core.avltree.KeyTupleAvlCursor;
import org.apache.directory.server.core.avltree.LinkedAvlMapNode;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.apache.directory.server.xdbm.AbstractTable;


/**
 * A Table implementation backed by in memory AVL tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTable<K, V> extends AbstractTable<K, V>
{
    private final AvlTreeMap<K, V> avl;
    private final Comparator<Tuple<K, V>> keyOnlytupleComparator;


    public AvlTable( String name, final Comparator<K> keyComparator, final Comparator<V> valueComparator,
        boolean dupsEnabled )
    {
        super( null, name, keyComparator, valueComparator );
        this.avl = new AvlTreeMapImpl<>( keyComparator, valueComparator, dupsEnabled );
        allowsDuplicates = this.avl.isDupsAllowed();
        this.keyOnlytupleComparator = new Comparator<Tuple<K, V>>()
        {
            public int compare( Tuple<K, V> t0, Tuple<K, V> t1 )
            {
                return keyComparator.compare( t0.getKey(), t1.getKey() );
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( PartitionTxn transaction ) throws LdapException
    {
        ( ( AvlTreeMapImpl<K, V> ) avl ).removeAll();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long count( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return 0L;
        }

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return 0L;
        }

        SingletonOrOrderedSet<V> val = node.getValue();

        if ( val.isOrderedSet() )
        {
            return val.getOrderedSet().getSize();
        }

        return 1L;
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

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return null;
        }

        SingletonOrOrderedSet<V> val = node.getValue();

        if ( val.isOrderedSet() )
        {
            return val.getOrderedSet().getFirst().getKey();
        }

        return val.getSingleton();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long greaterThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        return avl.getSize();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        return avl.find( key ) != null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has( PartitionTxn transaction, K key, V value ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        return avl.find( key, value ) != null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasGreaterOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        return avl.findGreaterOrEqual( key ) != null;
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

        LinkedAvlMapNode<K, V> node = avl.findGreaterOrEqual( key );

        if ( node == null )
        {
            return false;
        }

        if ( node.getValue().isOrderedSet() )
        {
            AvlTree<V> values = node.getValue().getOrderedSet();
            return values.findGreaterOrEqual( val ) != null;
        }

        return valueComparator.compare( node.getValue().getSingleton(), val ) >= 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasLessOrEqual( PartitionTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return false;
        }

        return avl.findLessOrEqual( key ) != null;
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

        LinkedAvlMapNode<K, V> node = avl.findLessOrEqual( key );

        if ( node == null )
        {
            return false;
        }

        if ( node.getValue().isOrderedSet() )
        {
            AvlTree<V> values = node.getValue().getOrderedSet();
            return values.findLessOrEqual( val ) != null;
        }

        return valueComparator.compare( node.getValue().getSingleton(), val ) <= 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long lessThanCount( PartitionTxn transaction, K key ) throws LdapException
    {
        return count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void put( PartitionWriteTxn transaction, K key, V value ) throws LdapException
    {
        if ( ( key == null ) || ( value == null ) )
        {
            return;
        }

        if ( avl.insert( key, value ) == null )
        {
            count++;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( PartitionWriteTxn transaction, K key ) throws LdapException
    {
        if ( key == null )
        {
            return;
        }

        SingletonOrOrderedSet<V> value = avl.remove( key );

        if ( value == null )
        {
            return;
        }

        if ( value.isOrderedSet() )
        {
            count -= value.getOrderedSet().getSize();
        }
        else
        {
            count--;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void remove( PartitionWriteTxn transaction, K key, V value ) throws LdapException
    {
        if ( avl.remove( key, value ) != null )
        {
            count--;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Cursor<Tuple<K, V>> cursor( PartitionTxn transaction ) throws LdapException
    {
        if ( !allowsDuplicates )
        {
            return new AvlTreeMapNoDupsWrapperCursor<>(
                new AvlSingletonOrOrderedSetCursor<K, V>( avl ) );
        }

        return new AvlTableDupsCursor<>( this );
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

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return new EmptyCursor<>();
        }

        if ( node.getValue().isOrderedSet() )
        {
            return new KeyTupleAvlCursor<>( node.getValue().getOrderedSet(), key );
        }

        return new SingletonCursor<>( new Tuple<K, V>( key, node.getValue().getSingleton() ),
            keyOnlytupleComparator );
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

        LinkedAvlMapNode<K, V> node = avl.find( key );

        if ( node == null )
        {
            return new EmptyCursor<>();
        }

        if ( node.getValue().isOrderedSet() )
        {
            return new AvlTreeCursor<>( node.getValue().getOrderedSet() );
        }

        return new SingletonCursor<>( node.getValue().getSingleton(), valueComparator );
    }


    /**
     * Returns the internal AvlTreeMap so other classes like Cursors
     * in the same package can access it.
     *
     * @return AvlTreeMap used to store Tuples
     */
    AvlTreeMap<K, V> getAvlTreeMap()
    {
        return avl;
    }
}
