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
 * A Table backed by in memory AVL Trees which allows for duplicates.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DupsAvlTable<K,V> implements Table<K, AvlTree<V>>
{
    private final AvlTree<Tuple<K,AvlTree<V>>> avlTree;
    private final Comparator<K> keyComparator;
    private final Comparator<V> valueComparator;
    private final String name;
    private int count;
    
    
    public DupsAvlTable( String name, DupsAvlTupleComparator<K,V> comparator )
    {
        this.name = name;
        this.avlTree = new AvlTree<Tuple<K,AvlTree<V>>>( comparator );
        this.keyComparator = comparator.getKeyComparator();
        this.valueComparator = comparator.getValueComparator();
    }
    


    public void close() throws Exception
    {
    }

    
    public int count() throws Exception
    {
        return count;
    }

    
    public int count( K key ) throws Exception
    {
        LinkedAvlNode<Tuple<K,AvlTree<V>>> node = avlTree.find( new Tuple<K,AvlTree<V>>( key, null ) );
        
        if ( node != null )
        {
            return node.getKey().getValue().getSize();
        }
        
        return 0;
    }

    
    public Cursor<Tuple<K, AvlTree<V>>> cursor() throws Exception
    {
        return new AvlTreeCursor<Tuple<K,AvlTree<V>>>( avlTree );
    }

    
    public Cursor<Tuple<K, AvlTree<V>>> cursor( K key ) throws Exception
    {
        LinkedAvlNode<Tuple<K,AvlTree<V>>> node = avlTree.find( new Tuple<K,AvlTree<V>>( key, null ) );
        
        if ( node == null )
        {
            return new EmptyCursor<Tuple<K,AvlTree<V>>>();
        }
        
        return new SingletonCursor<Tuple<K,AvlTree<V>>>( node.getKey() );
    }

    
    public AvlTree<V> get( K key ) throws Exception
    {
        if ( key == null )
        {
            return null;
        }
        
        LinkedAvlNode<Tuple<K,AvlTree<V>>> node = avlTree.find( new Tuple<K,AvlTree<V>>( key, null ) );
        
        if ( node == null )
        {
            return null;
        }
        
        return node.getKey().getValue();
    }

    
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }

    
    @SuppressWarnings("unchecked")
    public Comparator<AvlTree<V>> getValueComparator()
    {
        return ( Comparator<AvlTree<V>> ) valueComparator;
    }


    public String getName()
    {
        return name;
    }

    
    public int greaterThanCount( K key ) throws Exception
    {
        if ( key == null )
        {
            return 0;
        }
        
        return count;
    }

    
    public boolean has( K key ) throws Exception
    {
        if ( key == null )
        {
            return false;
        }
        
        return avlTree.find( new Tuple<K,AvlTree<V>>( key, null ) ) != null;
    }

    
    public boolean has( K key, AvlTree<V> value ) throws Exception
    {
        return has( key );
    }

    
    public boolean hasGreaterOrEqual( K key ) throws Exception
    {
        return avlTree.findGreaterOrEqual( new Tuple<K,AvlTree<V>>( key, null ) ) != null;
    }

    
    public boolean hasGreaterOrEqual( K key, AvlTree<V> val ) throws Exception
    {
        return avlTree.findGreaterOrEqual( new Tuple<K,AvlTree<V>>( key, null ) ) != null;
    }

    
    public boolean hasLessOrEqual( K key ) throws Exception
    {
        return avlTree.findLessOrEqual( new Tuple<K,AvlTree<V>>( key, null ) ) != null;
    }

    
    public boolean hasLessOrEqual( K key, AvlTree<V> val ) throws Exception
    {
        return avlTree.findLessOrEqual( new Tuple<K,AvlTree<V>>( key, null ) ) != null;
    }

    
    public boolean isCountExact()
    {
        return false;
    }

    
    public boolean isDupsEnabled()
    {
        return true;
    }

    
    public int lessThanCount( K key ) throws Exception
    {
        return count;
    }

    
    public void put( K key, AvlTree<V> value ) throws Exception
    {
        Tuple<K,AvlTree<V>> replaced = avlTree.insert( new Tuple<K, AvlTree<V>>( key, value ) );
        
        if ( replaced != null )
        {
            count -= replaced.getValue().getSize();
        }
        
        count += value.getSize();
    }

    
    public void remove( K key ) throws Exception
    {
        Tuple<K,AvlTree<V>> removed = avlTree.remove( new Tuple<K, AvlTree<V>>( key, null ) );
        
        if ( removed != null )
        {
            count -= removed.getValue().getSize();
        }
    }

    
    public void remove( K key, AvlTree<V> value ) throws Exception
    {
        LinkedAvlNode<Tuple<K,AvlTree<V>>> node = avlTree.find( new Tuple<K, AvlTree<V>>( key, null ) );
        
        if ( node == null )
        {
            return;
        }
        
        AvlTree<V> existing = node.getKey().getValue();
        AvlTreeCursor<V> cursor = new AvlTreeCursor<V>( value );
        
        while ( cursor.next() )
        {
            V remove = cursor.get();
            if ( existing.remove( remove ) != null )
            {
                count --;
            }
        }
    }

    
    public Cursor<AvlTree<V>> valueCursor( K key ) throws Exception
    {
        LinkedAvlNode<Tuple<K,AvlTree<V>>> node = avlTree.find( new Tuple<K, AvlTree<V>>( key, null ) );

        if ( node == null )
        {
            return new EmptyCursor<AvlTree<V>>();
        }
        
        return new SingletonCursor<AvlTree<V>>( node.getKey().getValue() );
    }
    
    
    /**
     * Exposes access so some Cursor implementations can access the AvlTree.
     *
     * @return avlTree
     */
    AvlTree<Tuple<K,AvlTree<V>>> getAvlTree()
    {
        return avlTree;
    }
}
