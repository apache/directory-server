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

import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;

public class OrderedSetCursor<V> extends AbstractCursor<V>
{
    /** Backing set */
    private OrderedSet<V> set;
    
    /** Cursor for the map backing OrderedSet */
    private ConcurrentMapCursor<V, String> wrappedCursor;
    
    public OrderedSetCursor( OrderedSet<V> set )
    {
        this.set = set;
        wrappedCursor = new ConcurrentMapCursor<V, String>( set.getBackingMap() );
    }
    
    
    public Comparator<V> getValueComparator()
    {
        return set.getValueComparator();
    }
    
    
    public void after( V value ) throws Exception
    {
       wrappedCursor.afterKey( value );
    }


    public void afterLast() throws Exception
    {
       wrappedCursor.afterLast();
    }


    public boolean available()
    {
        return wrappedCursor.available();
    }


    public void before( V value ) throws Exception
    {
        wrappedCursor.beforeKey( value );
    }


    public void beforeFirst() throws Exception
    {
        wrappedCursor.beforeFirst();
    }


    public boolean first() throws Exception
    {
        return wrappedCursor.first();
    }


    public V get() throws Exception
    {
        V value;
        
        if ( wrappedCursor.available() )
        {
            value = wrappedCursor.get().getKey();
            
            return value;
        }
        
        throw new InvalidCursorPositionException();
    }


    public boolean last() throws Exception
    {
        return wrappedCursor.last();
    }


    public boolean next() throws Exception
    {
        return wrappedCursor.next();
    }


    public boolean previous() throws Exception
    {
        return wrappedCursor.previous();
    }
}
