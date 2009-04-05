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
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.xdbm.Table;
import org.apache.directory.server.xdbm.Tuple;


/**
 * A Table implementation backed by an in memory AVL Tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTable<K,V> implements Table<K, V>
{
    public final DupsAvlTable<K,V> dupsTable;
    public final NoDupsAvlTable<K, V> noDupsTable;
    public final boolean dupsEnabled;
    
    
    public AvlTable( String name, Comparator<K> keyComparator, Comparator<V> valueComparator,
        boolean dupsEnabled )
    {
        this.dupsEnabled = dupsEnabled;
        
        if ( dupsEnabled )
        {
            dupsTable = new DupsAvlTable<K, V>( name, 
                new DupsAvlTupleComparator<K, V>( keyComparator, valueComparator ) );
            noDupsTable = null;
        }
        else
        {
            dupsTable = null;
            noDupsTable = new NoDupsAvlTable<K, V>( 
                new NoDupsAvlTupleComparator<K, V>( keyComparator, valueComparator ) );
        }
    }

    
    public void close() throws Exception
    {
    }

    
    public int count() throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.count();
        }
        
        return noDupsTable.count();
    }
    

    public int count( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.count( key );
        }
        
        return noDupsTable.count( key );
    }

    
    public Cursor<Tuple<K, V>> cursor() throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.cursor();
        }
        
        return noDupsTable.cursor();
    }
    

    public Cursor<Tuple<K, V>> cursor( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.cursor( key );
        }
        
        return noDupsTable.cursor( key );
    }

    
    public V get( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.get( key ).getFirst().getKey();
        }
        
        return noDupsTable.get( key );
    }

    
    public Comparator<K> getKeyComparator()
    {
        if ( dupsEnabled )
        {
            return dupsTable.getKeyComparator();
        }
        
        return noDupsTable.getKeyComparator();
    }
    

    public String getName()
    {
        if ( dupsEnabled )
        {
            return dupsTable.getName();
        }
        
        return noDupsTable.getName();
    }
    

    public Comparator<V> getValueComparator()
    {
        if ( dupsEnabled )
        {
            return dupsTable.getValueComparator();
        }
        
        return noDupsTable.getValueComparator();
    }

    
    public int greaterThanCount( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.greaterThanCount( key );
        }
        
        return noDupsTable.greaterThanCount( key );
    }

    
    public boolean has( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.has( key );
        }
        
        return noDupsTable.has( key );
    }

    
    public boolean has( K key, V value ) throws Exception
    {
        if ( dupsEnabled )
        {
            AvlTree<V> secondary = dupsTable.get( key );
            if ( secondary == null )
            {
                return false;
            }
            return secondary.find( value ) != null;
        }
        
        return noDupsTable.has( key, value );
    }

    
    public boolean hasGreaterOrEqual( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.hasGreaterOrEqual( key );
        }
        
        return noDupsTable.hasGreaterOrEqual( key );
    }

    
    public boolean hasGreaterOrEqual( K key, V val ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.hasGreaterOrEqual( key, val );
        }
        
        return noDupsTable.hasGreaterOrEqual( key, val );
    }

    
    public boolean hasLessOrEqual( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.hasLessOrEqual( key );
        }
        
        return noDupsTable.hasLessOrEqual( key );
    }
    

    public boolean hasLessOrEqual( K key, V val ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.hasLessOrEqual( key, val );
        }
        
        return noDupsTable.hasLessOrEqual( key, val );
    }
    

    public boolean isCountExact()
    {
        return false;
    }

    
    public boolean isDupsEnabled()
    {
        return dupsEnabled;
    }

    
    public int lessThanCount( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.lessThanCount( key );
        }
        
        return noDupsTable.lessThanCount( key );
    }

    
    public void put( K key, V value ) throws Exception
    {
        if ( dupsEnabled )
        {
            dupsTable.put( key, value );
        }
        else
        {
            noDupsTable.put( key, value );
        }
    }

    
    public void remove( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            dupsTable.remove( key );
        }
        else
        {
            noDupsTable.remove( key );
        }
    }

    
    public void remove( K key, V value ) throws Exception
    {
        if ( dupsEnabled )
        {
            dupsTable.remove( key, value );
        }
        else
        {
            return noDupsTable.remove( key, value );
        }
    }

    
    public Cursor<V> valueCursor( K key ) throws Exception
    {
        if ( dupsEnabled )
        {
            return dupsTable.valueCursor( key );
        }
        
        return noDupsTable.valueCursor( key );
    }
}
