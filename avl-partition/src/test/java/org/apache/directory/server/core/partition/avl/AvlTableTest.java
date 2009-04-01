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
package org.apache.directory.server.core.partition.avl;


import java.util.Comparator;

import org.junit.Ignore;

import junit.framework.TestCase;


/**
 * A set of test cases for the AvlTable class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTableTest extends TestCase
{
    /**
     * Tests the put() and get() methods on an AvlTable with duplicate keys 
     * disabled.
     */
    @Ignore ( "Test is failing due to failure of AvlTreeMap.insert to replace a tuple." )
    public void testPutNoDups() throws Exception
    {
        AvlTable<Integer,Integer> table = new AvlTable<Integer,Integer>( 
            "test", new IntComparator(), new IntComparator(), false );

        // ---------------------------------------------------------
        // normal operation 
        // ---------------------------------------------------------
        
        table.put( 0, 3 );
        table.put( 1, 2 );
        table.put( 2, 1 );
        table.put( 3, 0 );
        table.put( 23, 8934 );
        
        assertEquals( 3, table.get( 0 ).intValue() );
        assertEquals( 2, table.get( 1 ).intValue() );
        assertEquals( 1, table.get( 2 ).intValue() );
        assertEquals( 0, table.get( 3 ).intValue() );
        assertEquals( 8934, table.get( 23 ).intValue() );

        // ---------------------------------------------------------
        // try adding duplicates when not supported
        // ---------------------------------------------------------
        
        table.put( 23, 34 );
        assertEquals( 34, table.get( 23 ).intValue() );
    }
    
    
    class IntComparator implements Comparator<Integer>
    {
        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }
    }
}
