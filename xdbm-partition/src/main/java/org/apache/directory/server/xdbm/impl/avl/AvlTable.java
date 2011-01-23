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

import org.apache.directory.server.core.avltree.AvlSingletonOrOrderedSetCursor;
import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.AvlTreeMap;
import org.apache.directory.server.core.avltree.AvlTreeMapImpl;
import org.apache.directory.server.core.avltree.AvlTreeMapNoDupsWrapperCursor;
import org.apache.directory.server.core.avltree.KeyTupleAvlCursor;
import org.apache.directory.server.core.avltree.LinkedAvlMapNode;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.apache.directory.server.xdbm.Table;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.cursor.*;
import org.apache.directory.shared.ldap.model.cursor.Tuple;


/**
 * A Table implementation backed by in memory AVL tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTable<K, V> implements Table<K, V>
{
    private final AvlTreeMap<K, V> avl;
    private final String name;
    private final Comparator<K> keyComparator;
    private final Comparator<V> valComparator;
    private final Comparator<Tuple<K,V>> keyOnlytupleComparator;
    private int count;
    
    
    public AvlTable( String name, final Comparator<K> keyComparator, final Comparator<V> valComparator, boolean dupsEnabled )
    {
        this.name = name;
        this.keyComparator = keyComparator;
        this.valComparator = valComparator;
        this.avl = new AvlTreeMapImpl<K, V>( keyComparator, valComparator, dupsEnabled );
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
    public void close() throws Exception
    {
        ( ( AvlTreeMapImpl ) avl ).removeAll();
    }

    
    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return count;
    }

    
    /**
     * {@inheritDoc}
     */
    public int count( K key ) throws Exception
    {
        if ( key == null )
        {
            return 0;
        }
        
        LinkedAvlMapNode<K, V> node = avl.find( key );
        if ( node == null )
        {
            return 0;
        }
        
        SingletonOrOrderedSet<V> val = node.getValue();
        if ( val.isOrderedSet() )
        {
            return val.getOrderedSet().getSize();
        }
        
        return 1;
    }

   
    /**
     * {@inheritDoc}
     */
    public V get( K key ) throws Exception
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
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }

    
    /**
     * {@inheritDoc}
     */
    public Comparator<V> getValueComparator()
    {
        return valComparator;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }
    

    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( K key ) throws Exception
    {
        return avl.getSize();
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean has( K key ) throws Exception
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
    public boolean has( K key, V value ) throws Exception
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
    public boolean hasGreaterOrEqual( K key ) throws Exception
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
    public boolean hasGreaterOrEqual( K key, V val ) throws Exception
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
        
        return valComparator.compare( node.getValue().getSingleton(), val ) >= 0;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean hasLessOrEqual( K key ) throws Exception
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
    public boolean hasLessOrEqual( K key, V val ) throws Exception
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
        
        return valComparator.compare( node.getValue().getSingleton(), val ) <= 0;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isCountExact()
    {
        return false;
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return avl.isDupsAllowed();
    }
    

    /**
     * {@inheritDoc}
     */
    public int lessThanCount( K key ) throws Exception
    {
        return count;
    }
    

    /**
     * {@inheritDoc}
     */
    public void put( K key, V value ) throws Exception
    {
        if ( key == null || value == null )
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
    public void remove( K key ) throws Exception
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
            count --;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void remove( K key, V value ) throws Exception
    {
        if ( avl.remove( key, value ) != null )
        {
            count--;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<K, V>> cursor() throws Exception
    {
        if ( ! avl.isDupsAllowed() )
        {
            return new AvlTreeMapNoDupsWrapperCursor<K, V>( new AvlSingletonOrOrderedSetCursor<K,V>( avl ) );
        }

        return new AvlTableDupsCursor<K, V>( this );
    }

    
    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<K, V>> cursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<Tuple<K,V>>();
        }
        
        LinkedAvlMapNode<K, V> node = avl.find( key );
        if ( node == null )
        {
            return new EmptyCursor<Tuple<K,V>>();
        }
        
        if ( node.getValue().isOrderedSet() )
        {
            return new KeyTupleAvlCursor<K,V>( node.getValue().getOrderedSet(), key );
        }
        
        return new SingletonCursor<Tuple<K,V>>( new Tuple<K,V>( key, node.getValue().getSingleton() ), 
                keyOnlytupleComparator );
    }

    
    /**
     * {@inheritDoc}
     */
    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( key == null )
        {
            return new EmptyCursor<V>();
        }
        
        LinkedAvlMapNode<K, V> node = avl.find( key );
        if ( node == null )
        {
            return new EmptyCursor<V>();
        }
        
        if ( node.getValue().isOrderedSet() )
        {
            return new AvlTreeCursor<V>( node.getValue().getOrderedSet() );
        }
        
        return new SingletonCursor<V>( node.getValue().getSingleton(), valComparator );
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
