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

import java.util.Iterator;
import java.util.concurrent.ConcurrentNavigableMap;

import org.apache.directory.shared.ldap.model.cursor.AbstractTupleCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;

public class ConcurrentMapCursor<K,V> extends AbstractTupleCursor<K,V>
{
    /** Backing map */
    private ConcurrentNavigableMap<K,V> map;
    
    /** Keeps track of whether the cursor is positioned */
    private boolean positioned;
    
    /** Keeps track of whether moving next */
    private boolean movingNext;
    
    /** Currently available value */
    private Tuple<K, V> returnedTuple = new Tuple();
    
    /** true if value is available */
    private K availableKey;
    
    /** Iterator over the keys */
    Iterator<K> it;
    
    public ConcurrentMapCursor( ConcurrentNavigableMap<K,V> map )
    {
        this.map = map;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return ( availableKey != null );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Tuple<K,V> get() throws Exception
    {
        checkNotClosed( "get" );
        
        if ( availableKey != null )
        {
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K,V> element ) throws Exception
    {
        afterKey( element.getKey() );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void before( Tuple<K,V> element ) throws Exception
    {   
        beforeKey( element.getKey() );   
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterKey( K key ) throws Exception
    {
        checkNotClosed( "afterKey" );
        
        availableKey = null;
        positioned = true;
        movingNext = true;

        if ( key == null )
        {
            afterLast();
            return;
        }

        
        // Simply position the iterator past the given element.
        it = map.tailMap( key, false ).keySet().iterator();      
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void beforeKey( K key ) throws Exception
    {
        checkNotClosed( "beforeKey" );
        
        availableKey = null;
        positioned = true;
        movingNext = true;
        
        if ( key == null )
        {
            beforeFirst();
            return;
        }
        
        // Simply position the iterator past the given element.
        it = map.tailMap( key, true ).keySet().iterator();      
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void beforeValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst" );
        
        positioned = true;
        availableKey = null;
        movingNext = true;
        
        it = map.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast" );
        
        positioned = true;
        availableKey = null;
        movingNext = false;

        it = map.keySet().descendingIterator();
        
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        afterLast();

        return previous();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous" );
        
        if ( positioned == false )
        {
            afterLast();
        }

        // If currently moving in the next() direction, then get a descending iterator using the last availableValue
        if ( movingNext == true )
        {
            if ( availableKey == null )
            {
                if ( it.hasNext() )
                {
                    availableKey = it.next();
                }
            }

            if ( availableKey == null )
            {
                it = map.keySet().descendingIterator();
            }
            else
            {
                it = map.headMap( availableKey, false ).keySet().descendingIterator();
            }

            availableKey = null;
            movingNext = false;
        }

        
        V value;
        availableKey = null;
        
        while ( it.hasNext() )
        {
            availableKey = it.next();
            value = map.get( availableKey );
            
            if ( value != null )
            {
                returnedTuple.setBoth( availableKey, value );
                break;
            }
            
            availableKey = null;
        }
       
        return ( availableKey != null );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next" );
        
        if ( positioned == false )
        {
            beforeFirst();
        }

        // If currently moving in the previous() direction, then get a increasing iterator using the last availableValue
        if ( movingNext == false )
        {
            if ( availableKey == null )
            {
                if ( it.hasNext() )
                {
                    availableKey = it.next();
                }
            }

            if ( availableKey == null )
            {
                it = map.keySet().iterator();
            }
            else
            {
                it = map.tailMap( availableKey, false ).keySet().iterator();
            }

            availableKey = null;
            movingNext = true;
        }


        availableKey = null;
        V value;
        
        while ( it.hasNext() )
        {
            availableKey = it.next();
            value = map.get( availableKey );
            
            if ( value != null )
            {
                returnedTuple.setBoth( availableKey, value );
                break;
            }
            
            availableKey = null;
        }
       
        return ( availableKey != null );
    }
}
