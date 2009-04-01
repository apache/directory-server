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

import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.xdbm.Tuple;

import junit.framework.TestCase;


/**
 * TODO AvlUsageTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlUsageTest extends TestCase
{
    public void testWithTuple()
    {
        //AvlTree<Tuple<K,V>> avlTree = new AvlTree<Tuple<K, V>>();
    }
    
    
    class AvlTupleComparator<K,V> implements Comparator<Tuple<K,V>>
    {
        public int compare( Tuple<K,V> arg0, Tuple<K,V> arg1 )
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}
