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
import java.util.UUID;

public class ReverseIndexComparator<V> implements IndexComparator<V>
{
    /** Key comparator */
    private Comparator<V> keyComparator;


    public ReverseIndexComparator( Comparator<V> keyComparator )
    {
        this.keyComparator = keyComparator;
    }


    public int compare( IndexEntry<V> entry1, IndexEntry<V> entry2 )
    {
        V value1 = entry1.getValue();
        V value2 = entry2.getValue();
        UUID id1 = entry1.getId();
        UUID id2 = entry2.getId();

        int result = UUIDComparator.INSTANCE.compare( id1, id2 );

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
}
