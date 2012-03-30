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


import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;


/**
 * A cursor that converts SingletonOrOrderedSet objects in the value from a
 * AvlTreeMap into Tuples with just K and V presuming that all the keys have
 * no duplicates. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeMapNoDupsWrapperCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    private final AvlSingletonOrOrderedSetCursor<K, V> wrapped;
    private final Tuple<K, V> returnedTuple = new Tuple<K, V>();


    public AvlTreeMapNoDupsWrapperCursor( AvlSingletonOrOrderedSetCursor<K, V> wrapped )
    {
        this.wrapped = wrapped;
    }


    public void afterKey( K key ) throws Exception
    {
        wrapped.afterKey( key );
    }


    public void afterValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }


    public void beforeKey( K key ) throws Exception
    {
        wrapped.beforeKey( key );
    }


    public void beforeValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }


    public void after( Tuple<K, V> element ) throws Exception
    {
        wrapped.afterKey( element.getKey() );
    }


    public void afterLast() throws Exception
    {
        wrapped.afterLast();
    }


    public boolean available()
    {
        return wrapped.available();
    }


    public void before( Tuple<K, V> element ) throws Exception
    {
        wrapped.beforeKey( element.getKey() );
    }


    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
    }


    public boolean first() throws Exception
    {
        return wrapped.first();
    }


    public Tuple<K, V> get() throws Exception
    {
        if ( wrapped.available() )
        {
            Tuple<K, SingletonOrOrderedSet<V>> tuple = wrapped.get();

            if ( tuple.getValue().isOrderedSet() )
            {
                System.out.println( "tuple key = " + tuple.getKey() );
                tuple.getValue().getOrderedSet().printTree();
            }

            returnedTuple.setBoth( tuple.getKey(), tuple.getValue().getSingleton() );
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    public boolean last() throws Exception
    {
        return wrapped.last();
    }


    public boolean next() throws Exception
    {
        return wrapped.next();
    }


    public boolean previous() throws Exception
    {
        return wrapped.previous();
    }
}
