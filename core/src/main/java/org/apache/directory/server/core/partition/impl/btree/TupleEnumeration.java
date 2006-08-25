/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.util.Iterator;

import javax.naming.NamingEnumeration;


/**
 * A NamingEnumeration that returns underlying Iterator values for a single key
 * as Tuples.
 * 
 * <p>
 * WARNING: The tuple returned is reused every time for efficiency and populated
 * a over and over again with the new value.  The key never changes.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TupleEnumeration implements NamingEnumeration
{
    private final Object key;
    private final Iterator iterator;
    private final Tuple tuple = new Tuple();


    /**
     * Creates a cursor over an Iterator of single key's values
     * 
     * @param key the keys whose duplicate values are to be returned
     * @param iterator the underlying iterator this cursor uses
     */
    public TupleEnumeration(Object key, Iterator iterator)
    {
        this.key = key;
        tuple.setKey( key );
        this.iterator = iterator;
    }


    /**
     * Gets the next value as a Tuple.
     *
     * @see javax.naming.NamingEnumeration#next()
     */
    public Object next()
    {
        tuple.setKey( key );
        tuple.setValue( iterator.next() );
        return tuple;
    }


    /**
     * Gets the next value as a Tuple.
     *
     * @see javax.naming.NamingEnumeration#nextElement()
     */
    public Object nextElement()
    {
        tuple.setKey( key );
        tuple.setValue( iterator.next() );
        return tuple;
    }


    /**
     * Checks if another value is available.
     *
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return iterator.hasNext();
    }


    /**
     * Checks if another value is available.
     *
     * @see javax.naming.NamingEnumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return iterator.hasNext();
    }


    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close()
    {
    }
}
