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
package org.apache.directory.server.core.partition.ldif;


import java.util.Comparator;

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.LinkedAvlNode;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.EmptyCursor;
import org.apache.directory.server.core.cursor.SingletonCursor;
import org.apache.directory.server.xdbm.Table;
import org.apache.directory.server.xdbm.Tuple;


/**
 * A Table implementation that does not allow duplicate keys backed by in 
 * memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NoDupsAvlTable<K,V> implements Table<K, V>
{
    private final AvlTree<Tuple<K,V>> avlTree;
    private final Comparator<K> keyComparator;
    private final Comparator<V> valueComparator;
    private String name;
    
    
    public NoDupsAvlTable( NoDupsAvlTupleComparator<K,V> comparator )
    {
        this.avlTree = new AvlTree<Tuple<K,V>>( comparator );
        this.keyComparator = comparator.getKeyComparator();
        this.valueComparator = comparator.getValueComparator();
    }
    

    public void close() throws Exception
    {
    }

    
    public int count() throws Exception
    {
        return avlTree.getSize();
    }
    

    public int count( K key ) throws Exception
    {
        if ( avlTree.find( new Tuple<K,V>( key, null ) ) != null )
        {
            return 1;
        }
        
        return 0;
    }

    
    public Cursor<Tuple<K, V>> cursor() throws Exception
    {
        return new AvlTreeCursor<Tuple<K,V>>( avlTree );
    }

    
    public Cursor<Tuple<K, V>> cursor( K key ) throws Exception
    {
        LinkedAvlNode<Tuple<K,V>> node = avlTree.find( new Tuple<K,V>( key, null ) );
        
        if ( node == null )
        {
            return new EmptyCursor<Tuple<K,V>>();
        }

        return new SingletonCursor<Tuple<K,V>>( node.getKey() );
    }

    
    public V get( K key ) throws Exception
    {
        LinkedAvlNode<Tuple<K,V>> node = avlTree.find( new Tuple<K,V>( key, null ) );
        
        if ( node != null )
        {
            return node.getKey().getValue();
        }
        
        return null;
    }

    
    public String getName()
    {
        return name;
    }

    
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }

    
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }
    

    public int greaterThanCount( K key ) throws Exception
    {
        return avlTree.getSize();
    }

    
    public boolean has( K key ) throws Exception
    {
        return avlTree.find( new Tuple<K,V>( key, null ) ) != null;
    }

    
    public boolean has( K key, V value ) throws Exception
    {
        LinkedAvlNode<Tuple<K,V>> node = avlTree.find( new Tuple<K,V>( key, value ) );
        
        if ( valueComparator.compare( value, node.getKey().getValue() ) == 0 )
        {
            return true;
        }

        return false;
    }

    
    public final boolean hasGreaterOrEqual( K key ) throws Exception
    {
        return avlTree.findGreaterOrEqual( new Tuple<K,V>( key, null ) ) != null;
    }
    

    public final boolean hasGreaterOrEqual( K key, V val ) throws Exception
    {
        return avlTree.findGreaterOrEqual( new Tuple<K,V>( key, val ) ) != null;
    }
    

    public final boolean hasLessOrEqual( K key ) throws Exception
    {
        return avlTree.findLessOrEqual( new Tuple<K,V>( key, null ) ) != null;
    }
    

    public final boolean hasLessOrEqual( K key, V val ) throws Exception
    {
        return avlTree.findGreaterOrEqual( new Tuple<K,V>( key, val ) ) != null;
    }

    
    public final boolean isCountExact()
    {
        return false;
    }

    
    public final boolean isDupsEnabled()
    {
        return false;
    }

    
    public final int lessThanCount( K key ) throws Exception
    {
        return avlTree.getSize();
    }

    
    public final void put( K key, V value ) throws Exception
    {
        avlTree.insert( new Tuple<K,V>( key, value) );
    }

    
    public final void remove( K key ) throws Exception
    {
        avlTree.remove( new Tuple<K,V>( key, null) );
    }
    

    public final void remove( K key, V value ) throws Exception
    {
        avlTree.remove( new Tuple<K,V>( key, value) );
    }

    
    public final Cursor<V> valueCursor( K key ) throws Exception
    {
        V value = get( key );
        
        if ( value == null )
        {
            return new EmptyCursor<V>();
        }

        return new SingletonCursor<V>( value );
    }
}
