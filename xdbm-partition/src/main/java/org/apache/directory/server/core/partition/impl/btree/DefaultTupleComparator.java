/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.partition.impl.btree;


import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;


/**
 * The default implementation of a pair of comparators which compares both
 * keys and values of a Tuple.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultTupleComparator<K, V> implements TupleComparator<K, V>
{
    private static final long serialVersionUID = -6639792479317762334L;

    SerializableComparator<K> keyComparator;
    SerializableComparator<V> valueComparator;


    public DefaultTupleComparator( SerializableComparator<K> keyComparator, SerializableComparator<V> valueComparator )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
    }


    public SerializableComparator<K> getKeyComparator()
    {
        return keyComparator;
    }


    public SerializableComparator<V> getValueComparator()
    {
        return valueComparator;
    }


    public int compareKey( K key1, K key2 )
    {
        return keyComparator.compare( key1, key2 );
    }


    public int compareValue( V value1, V value2 )
    {
        return valueComparator.compare( value1, value2 );
    }
}
