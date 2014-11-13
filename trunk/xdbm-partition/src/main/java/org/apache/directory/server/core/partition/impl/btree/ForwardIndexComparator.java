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


import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;


/**
 * TupleComparator for index records.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ForwardIndexComparator<K> implements TupleComparator<K, Long>
{
    private static final long serialVersionUID = 3257283621751633459L;

    /** The key comparison to use */
    private final SerializableComparator<K> keyComparator;


    /**
     * Creates an IndexComparator.
     *
     * @param keyComparator the table comparator to use for keys
     */
    public ForwardIndexComparator( SerializableComparator<K> keyComparator )
    {
        this.keyComparator = keyComparator;
    }


    /**
     * Gets the comparator used to compare keys.
     *
     * @return the comparator for comparing keys.
     */
    public SerializableComparator<K> getKeyComparator()
    {
        return keyComparator;
    }


    /**
     * Gets the binary comparator used to compare values which are the indices
     * into the master table.
     *
     * @return the binary comparator for comparing values.
     */
    public SerializableComparator<Long> getValueComparator()
    {
        return LongComparator.INSTANCE;
    }


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param key1 the first key to compare
     * @param key2 the other key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second byte array.
     */
    public int compareKey( K key1, K key2 )
    {
        return getKeyComparator().compare( key1, key2 );
    }


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param l1 the first Long value to compare
     * @param l2 the other Long value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second Object.
     */
    public int compareValue( Long l1, Long l2 )
    {
        return ( l1 < l2 ? -1 : ( l1.equals( l2 ) ? 0 : 1 ) );
    }
}