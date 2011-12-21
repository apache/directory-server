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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.directory.server.core.avltree.AvlTreeMapNoDupsWrapperCursor;
import org.apache.directory.server.core.avltree.ConcurrentMapCursor;
import org.apache.directory.server.core.avltree.KeyTupleAvlCursor;
import org.apache.directory.server.core.avltree.OrderedSet;
import org.apache.directory.server.core.avltree.OrderedSetCursor;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.apache.directory.server.core.api.partition.index.AbstractTable;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.model.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;


/**
 * A Table implementation backed by in memory AVL tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTable<K, V> extends AbstractTable<K,V>
{
    private final ConcurrentNavigableMap<K, SingletonOrOrderedSet<V>> map;
    private final Comparator<Tuple<K,V>> keyOnlytupleComparator;
    
    /** Whether dups is enabled */
    private boolean dupsEnabled;
    
    
    public AvlTable( String name, final Comparator<K> keyComparator, final Comparator<V> valueComparator, boolean dupsEnabled )
    {
        super( null, name, keyComparator, valueComparator );
        this.keyOnlytupleComparator = new Comparator<Tuple<K, V>>()
        {
            public int compare( Tuple<K, V> t0, Tuple<K, V> t1 )
            {
                return keyComparator.compare( t0.getKey(), t1.getKey() );
            }
        };
        
        map = new ConcurrentSkipListMap<K, SingletonOrOrderedSet<V>>( keyComparator );
        this.dupsEnabled = dupsEnabled;
    }
    
    
    public ConcurrentNavigableMap<K, SingletonOrOrderedSet<V>> getBackingMap()
    {
        return map;
    }
    

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        map.clear();
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return 0;
        }
        
        if ( set.isOrderedSet() )
        {
            return set.getOrderedSet().getSize();
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return null;
        }
        
        if ( set.isOrderedSet() )
        {
            return set.getOrderedSet().first();
        }
        
        return set.getSingleton();
    }

    
    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( K key ) throws Exception
    {
        return count;
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
        
        return ( map.get( key ) != null );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean has( K key, V value ) throws Exception
    {
        if ( key == null || value == null )
        {
            return false;
        }
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return false;
        }
        
        if ( set.isOrderedSet() )
        {
            return set.getOrderedSet().contains( value );
        }
        
        V singletonValue = set.getSingleton();
        
        if ( valueComparator.compare( singletonValue, value ) == 0 )
        {
            return true;
        }
        
        return false;
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
        
        return ( map.ceilingKey( key ) != null );
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return false;
        }
        
        if ( set.isOrderedSet() )
        {
            return set.getOrderedSet().hasGreaterOrEqual( val );
        }

                
        return valueComparator.compare( set.getSingleton(), val ) >= 0;
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
        
        return ( map.floorEntry( key ) != null );
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return false;
        }
        
        if ( set.isOrderedSet() )
        {
            return set.getOrderedSet().hasLessOrEqual( val );
        }

                
        return valueComparator.compare( set.getSingleton(), val ) <= 0;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return dupsEnabled;
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
    public synchronized void put( K key, V value ) throws Exception
    {
        if ( key == null || value == null )
        {
            return;
        }
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
           
            if ( dupsEnabled )
            {
                OrderedSet<V> orderedSet = new OrderedSet<V>( valueComparator );
                orderedSet.insert( value );
                set = new SingletonOrOrderedSet<V>( orderedSet ); 
            }
            else
            {
                set = new SingletonOrOrderedSet<V>( value );
            }
            
            map.put( key, set );
            count++;
                
            return;
        }
        
        if ( set.isOrderedSet() )
        {
            if ( set.getOrderedSet().insert( value ) == true )
            {
                count++;
            }
            
            return;
        }
        
        // Replace existing value
        set.setSingleton( value );
        
        return;
    }

    
    /**
     * {@inheritDoc}
     */
    public synchronized void remove( K key ) throws Exception
    {
        if ( key == null )
        {
            return;
        }
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return;
        }
        
        map.remove( key );
        
        if ( set.isOrderedSet() )
        {
            count -= set.getOrderedSet().getSize();
        }
        else
        {
            count --;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public synchronized void remove( K key, V value ) throws Exception
    {
        if ( key == null || value == null )
        {
            return;
        }
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return;
        }
        
        if ( set.isOrderedSet() )
        {
            if ( set.getOrderedSet().remove( value ) == true  )
            {
                count --;
                
                if ( set.getOrderedSet().getSize() == 0 )
                {
                    map.remove( key );
                }
            }
            
            return;
        }
        
        // Remove singleton value
        map.remove( key );
        count--;    
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<K, V>> cursor() throws Exception
    {
        if ( ! dupsEnabled )
        {
            return new AvlTreeMapNoDupsWrapperCursor<K, V>( new ConcurrentMapCursor<K,SingletonOrOrderedSet<V>>( map ) );
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return new EmptyCursor<Tuple<K,V>>();
        }
        
        if ( set.isOrderedSet() )
        {
            return new KeyTupleAvlCursor<K,V>( set.getOrderedSet(), key );
        }
        
        return new SingletonCursor<Tuple<K,V>>( new Tuple<K,V>( key, set.getSingleton() ), 
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
        
        SingletonOrOrderedSet<V> set = map.get( key );
        
        if ( set == null )
        {
            return new EmptyCursor<V>();
        }
        
        if ( set.isOrderedSet() )
        {
            return new OrderedSetCursor<V>( set.getOrderedSet() );
        }
        
        return new SingletonCursor<V>( set.getSingleton(), valueComparator );
    }

}
