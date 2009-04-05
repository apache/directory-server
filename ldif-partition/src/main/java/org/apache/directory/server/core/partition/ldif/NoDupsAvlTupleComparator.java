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

import org.apache.directory.server.xdbm.Tuple;


/**
 * A pair of comparators used to order Tuples in a AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NoDupsAvlTupleComparator<K,V> implements Comparator<Tuple<K,V>>
{
    private final Comparator<K> keyComparator;
    private final Comparator<V> valueComparator;
  
    
    public NoDupsAvlTupleComparator( Comparator<K> keyComparator, Comparator<V> valueComparator )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
    }
  
    
    public Comparator<K> getKeyComparator()
    {
        return keyComparator;
    }
    
    
    public Comparator<V> getValueComparator()
    {
        return valueComparator;
    }
    

    public int compare( Tuple<K, V> t1, Tuple<K, V> t2 )
    {
        int keyComp = keyComparator.compare( t1.getKey(), t2.getKey() );
        
        if ( keyComp == 0 )
        {
            return valueComparator.compare( t1.getValue(), t2.getValue() );
        }
        
        return keyComp;
    }
}
