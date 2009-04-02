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
package org.apache.directory.server.core.partition.avl;


import java.util.Comparator;

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.AvlTreeMap;
import org.apache.directory.server.core.avltree.AvlTreeMapNoDupsCursor;
import org.apache.directory.server.core.avltree.KeyTupleAvlCursor;
import org.apache.directory.server.core.avltree.LinkedAvlMapNode;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.EmptyCursor;
import org.apache.directory.server.core.cursor.SingletonCursor;
import org.apache.directory.server.xdbm.Table;
import org.apache.directory.server.xdbm.Tuple;


/**
 * A Table implementation backed by in memory AVL tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTable<K, V> implements Table<K, V>
{
    private final AvlTreeMap<K, V> avl;
    private final String name;
    private final Comparator<K> keyComparator;
    private final Comparator<V> valComparator;
    private int count;
    
    
    public AvlTable( String name, Comparator<K> keyComparator, Comparator<V> valComparator, boolean dupsEnabled )
    {
        this.name = name;
        this.keyComparator = keyComparator;
        this.valComparator = valComparator;
        this.avl = new AvlTreeMap<K, V>( keyComparator, valComparator, dupsEnabled );
    }
    

    /**
     * Does nothing.
     * 
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
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
    @SuppressWarnings("unchecked")
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
        
        V val = node.getValue();
        if ( val instanceof AvlTree )
        {
            return ( ( AvlTree ) val ).getSize();
        }
        
        return 1;
    }

   
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
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
        
        V val = node.getValue();
        if ( val instanceof AvlTree )
        {
            return ( ( AvlTree<V> ) val ).getFirst().getKey();
        }
        
        return val;
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
    @SuppressWarnings("unchecked")
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
        
        if ( node.getValue() instanceof AvlTree )
        {
            AvlTree<V> values = ( AvlTree<V> ) node.getValue();
            return values.findGreaterOrEqual( val ) != null;
        }
        
        return valComparator.compare( node.getValue(), val ) >= 0;
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
    @SuppressWarnings("unchecked")
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
        
        if ( node.getValue() instanceof AvlTree )
        {
            AvlTree<V> values = ( AvlTree<V> ) node.getValue();
            return values.findLessOrEqual( val ) != null;
        }
        
        return valComparator.compare( node.getValue(), val ) <= 0;
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
    @SuppressWarnings("unchecked")
    public void remove( K key ) throws Exception
    {
        if ( key == null )
        {
            return;
        }
        
        if ( ! avl.isDupsAllowed() )
        {
            if ( avl.remove( key, null ) != null )
            {
                count--;
            }
            return;
        }
        
        LinkedAvlMapNode<K, V> node = avl.find( key );
        if ( node == null )
        {
            return;
        }
        
        V value = node.getValue();
        
        if ( value instanceof AvlTree )
        {
            count -= ( ( AvlTree ) value ).getSize();
            avl.remove( key, null );
        }
        else
        {
            avl.remove( key, null );
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
            return new AvlTreeMapNoDupsCursor<K,V>( avl );
        }

        return new AvlTableDupsCursor<K, V>( this );
    }

    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
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
        
        V value = node.getValue();
        if ( value instanceof AvlTree )
        {
            return new KeyTupleAvlCursor<K,V>( ( AvlTree<V> ) value, key );
        }
        
        return new SingletonCursor<Tuple<K,V>>( new Tuple<K,V>( key, value ) );
    }

    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
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
        
        V value = node.getValue();
        if ( value instanceof AvlTree )
        {
            return new AvlTreeCursor<V>( ( AvlTree<V> ) value );
        }
        
        return new SingletonCursor<V>( value );
    }


    /**
     * Returns the internal AvlTreeMap so other classes like Cursors
     * in the same package can access it.
     *
     * @return AvlTreeMap used to store Tuples
     */
    AvlTreeMap<K,V> getAvlTreeMap()
    {
        return avl;
    }
}
