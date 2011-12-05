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
package org.apache.directory.server.core.avltree;

import java.util.Comparator;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class OrderedSet<V>
{
    /** string used for all values */
    private final static String EMPTY_STRING = "";
    
    /** Backing store */
    private ConcurrentNavigableMap<V, String> orderedMap;
    
    /** Size of the map */
    private int count = 0;
    
    /** Value comparator */
    Comparator<V> valueComparator;
    
    
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }
    
    public OrderedSet( Comparator<V> comparator )
    {
        orderedMap = new ConcurrentSkipListMap<V, String>( comparator );
        valueComparator = comparator;
    }
    
    
    public ConcurrentNavigableMap<V, String> getBackingMap()
    {
        return orderedMap;
    }
    
    public synchronized boolean insert( V value )
    {
        if ( value == null )
        {
            throw new IllegalArgumentException( "Cannot insert null value into ordered set" );
        }
        
        String existingString = orderedMap.put( value, EMPTY_STRING );
        
        if ( existingString == null )
        {
            count++;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    public synchronized boolean remove( V value )
    {
        if ( value == null )
        {
            throw new IllegalArgumentException( "Cannot remove null value from ordered set" );
        }
        
        
        if ( orderedMap.remove( value ) != null )
        {
            count--;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    public boolean contains( V value )
    {
        String contained = orderedMap.get( value );
        
        return ( contained != null );
    }
    
    public V first()
    {
        return orderedMap.firstKey();
    }
    
    public V last()
    {
        return orderedMap.lastKey();
    }
    
    
    public boolean hasGreaterOrEqual( V value )
    {
        if ( value == null )
        {
            return false;
        }
        
        return ( orderedMap.ceilingKey( value ) != null );
    }
    
    
    public boolean hasLessOrEqual( V value )
    {
        if ( value == null )
        {
            return false;
        }
        
        return ( orderedMap.floorKey( value ) != null );
    }
    
    
    public int getSize()
    {        
        return count; 
    }

}
