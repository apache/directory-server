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
public class ReverseIndexComparator<V> implements TupleComparator<Long, V>
{
    private static final long serialVersionUID = 3257283621751633459L;

    /** The value comparison to use - keys are Longs */
    private final SerializableComparator<V> valueComparator;


    /**
     * Creates an IndexComparator.
     *
     * @param valueComparator the table comparator to use for values
     */
    public ReverseIndexComparator( SerializableComparator<V> valueComparator )
    {
        this.valueComparator = valueComparator;
    }


    /**
     * Gets the comparator used to compare keys.
     *
     * @return the comparator for comparing keys.
     */
    public SerializableComparator<Long> getKeyComparator()
    {
        return LongComparator.INSTANCE;
    }


    /**
     * Gets the binary comparator used to compare values which are the values
     * of attributes.
     *
     * @return the binary comparator for comparing values.
     */
    public SerializableComparator<V> getValueComparator()
    {
        return valueComparator;
    }


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, &lt; or &gt; than 0.
     *
     * @param l1 the first long key to compare
     * @param l2 the other long key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a positive value if the first is greater than
     * the second byte array.
     */
    public int compareKey( Long l1, Long l2 )
    {
        return ( l1 < l2 ? -1 : ( l1.equals( l2 ) ? 0 : 1 ) );
    }


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, &lt; or &gt; than 0.
     *
     * @param v1 the first value to compare
     * @param v2 the other value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a positive value if the first is greater than
     * the second Object.
     */
    public int compareValue( V v1, V v2 )
    {
        return valueComparator.compare( v1, v2 );
    }
}