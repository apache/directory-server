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
package org.apache.directory.server.core.api.partition.index;

import java.util.Comparator;

public class ReverseIndexComparator<V, ID> implements IndexComparator<V,ID>
{
    Comparator<V> keyComparator;
    Comparator<ID> valueComparator;
    
    public ReverseIndexComparator( Comparator<V> keyComparator, Comparator<ID> valueComparator )
    {
        this.keyComparator = keyComparator;
        this.valueComparator = valueComparator;
    }
    
    public int compare( IndexEntry<V, ID> entry1, IndexEntry<V, ID> entry2 )
    {
        V value1 = entry1.getValue();
        V value2 = entry2.getValue();
        ID id1 = entry1.getId();
        ID id2 = entry2.getId();
        
        int result = valueComparator.compare( id1, id2 );
        
        if ( result == 0 )
        {
            if ( value1 == value2 )
            {
                result = 0;
            }
            else if ( value1 == null )
            {
                result = -1;
            }
            else if ( value2 == null )
            {
                result = 1;
            }
            else
            {
                result = keyComparator.compare( value1, value2 );
            }
        }
        
        return result;
    }
    
    public Comparator<V> getValueComparator()
    {
        return keyComparator;
    }
    
    
    public Comparator<ID> getIDComparator()
    {
        return valueComparator;
    }
}
