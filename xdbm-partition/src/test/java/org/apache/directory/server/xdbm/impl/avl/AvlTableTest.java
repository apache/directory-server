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
package org.apache.directory.server.xdbm.impl.avl;


import static org.apache.directory.server.xdbm.impl.avl.TableData.injectDupsData;
import static org.apache.directory.server.xdbm.impl.avl.TableData.injectNoDupsData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A set of test cases for the AvlTable class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class AvlTableTest
{
    private AvlTable<Integer, Integer> dups;
    private AvlTable<Integer, Integer> nodups;
    private final Comparator<Integer> comparator = new IntComparator();


    @Before
    public void setUp()
    {
        dups = new AvlTable<Integer, Integer>( "dups", comparator, comparator, true );
        nodups = new AvlTable<Integer, Integer>( "nodups", comparator, comparator, false );
    }


    @Test
    public void testGetName()
    {
        assertEquals( "dups", dups.getName() );
        assertEquals( "nodups", nodups.getName() );
    }


    @Test
    public void testCursorWithKey() throws Exception
    {
        injectNoDupsData( nodups );
        Cursor<Tuple<Integer, Integer>> cursor = nodups.cursor( 2 );

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        Tuple<Integer, Integer> tuple = cursor.get();
        assertEquals( 2, tuple.getKey().intValue() );
        assertEquals( 1, tuple.getValue().intValue() );

        assertFalse( cursor.next() );

        // ---- on duplicates ----

        injectDupsData( dups );
        cursor = dups.cursor( 3 );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 0, tuple.getValue().intValue() );

        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 8, tuple.getValue().intValue() );

        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 9, tuple.getValue().intValue() );

        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 10, tuple.getValue().intValue() );

        assertFalse( cursor.next() );
        cursor.close();
    }


    @Test
    public void testCursor() throws Exception
    {
        injectNoDupsData( nodups );
        Cursor<Tuple<Integer, Integer>> cursor = nodups.cursor();

        // position at first element (0,3)
        assertTrue( cursor.first() );
        Tuple<Integer, Integer> tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 0, tuple.getKey().intValue() );
        assertEquals( 3, tuple.getValue().intValue() );

        // move to next element (1,2)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 1, tuple.getKey().intValue() );
        assertEquals( 2, tuple.getValue().intValue() );

        // move to next element (2,1)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 2, tuple.getKey().intValue() );
        assertEquals( 1, tuple.getValue().intValue() );

        // move to next element (3,0)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 0, tuple.getValue().intValue() );

        // move to next element (23,8934)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 23, tuple.getKey().intValue() );
        assertEquals( 8934, tuple.getValue().intValue() );

        assertFalse( cursor.next() );
        cursor.close();

        // work with duplicates now

        injectDupsData( dups );
        cursor = dups.cursor();

        // position at first element (0,3)
        assertTrue( cursor.first() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 0, tuple.getKey().intValue() );
        assertEquals( 3, tuple.getValue().intValue() );

        // move to next element (1,2)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 1, tuple.getKey().intValue() );
        assertEquals( 2, tuple.getValue().intValue() );

        // move to next element (1,4)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 1, tuple.getKey().intValue() );
        assertEquals( 4, tuple.getValue().intValue() );

        // move to next element (1,6)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 1, tuple.getKey().intValue() );
        assertEquals( 6, tuple.getValue().intValue() );

        // move to next element (2,1)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 2, tuple.getKey().intValue() );
        assertEquals( 1, tuple.getValue().intValue() );

        // move to next element (3,0)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 0, tuple.getValue().intValue() );

        // move to next element (3,8)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 8, tuple.getValue().intValue() );

        // move to next element (3,9)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 9, tuple.getValue().intValue() );

        // move to next element (3,10)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 10, tuple.getValue().intValue() );

        // move to next element (23,8934)
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 23, tuple.getKey().intValue() );
        assertEquals( 8934, tuple.getValue().intValue() );

        assertFalse( cursor.next() );

        // test beforeFirst

        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 0, tuple.getKey().intValue() );
        assertEquals( 3, tuple.getValue().intValue() );

        // test afterLast

        cursor.afterLast();
        assertFalse( cursor.available() );
        assertFalse( cursor.next() );
        assertTrue( cursor.previous() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 23, tuple.getKey().intValue() );
        assertEquals( 8934, tuple.getValue().intValue() );
        cursor.close();
    }


    /**
     * Checks that cursor.after() behavior with duplicates enabled obeys 
     * the required semantics.
     */
    @Test
    public void testCursorAfterWithDups() throws Exception
    {
        injectDupsData( dups );
        Cursor<Tuple<Integer, Integer>> cursor;
        Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>();

        cursor = dups.cursor();
        cursor.after( tuple.setKey( 1 ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 2, tuple.getKey().intValue() );
        assertEquals( 1, tuple.getValue().intValue() );

        tuple = new Tuple<Integer, Integer>();
        cursor.after( tuple.setKey( 2 ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 3, tuple.getKey().intValue() );
        assertEquals( 0, tuple.getValue().intValue() );

        tuple = new Tuple<Integer, Integer>();
        cursor.after( tuple.setKey( 3 ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        tuple = cursor.get();
        assertNotNull( tuple );
        assertEquals( 23, tuple.getKey().intValue() );
        assertEquals( 8934, tuple.getValue().intValue() );
        cursor.close();
    }


    /**
     * Tests the put() and get() methods on an AvlTable.
     */
    @Test
    public void testPutGetCount() throws Exception
    {
        // ---------------------------------------------------------
        // normal operation 
        // ---------------------------------------------------------

        injectNoDupsData( nodups );

        assertEquals( 5, nodups.count() );

        assertEquals( 3, nodups.get( 0 ).intValue() );
        assertEquals( 2, nodups.get( 1 ).intValue() );
        assertEquals( 1, nodups.get( 2 ).intValue() );
        assertEquals( 0, nodups.get( 3 ).intValue() );
        assertEquals( 8934, nodups.get( 23 ).intValue() );

        // ---------------------------------------------------------
        // try adding duplicates when not supported
        // ---------------------------------------------------------

        nodups.put( 23, 34 );
        assertEquals( 34, nodups.get( 23 ).intValue() );
        assertEquals( 5, nodups.count() );

        // ---------------------------------------------------------
        // now with duplicates
        // ---------------------------------------------------------

        assertEquals( 0, dups.count() );

        injectDupsData( dups );

        // [3,0] was put twice so only 10 of 11 should have been put in
        assertEquals( 10, dups.count() );

        assertEquals( 3, dups.get( 0 ).intValue() );
        assertEquals( 2, dups.get( 1 ).intValue() );
        assertEquals( 1, dups.get( 2 ).intValue() );
        assertEquals( 0, dups.get( 3 ).intValue() );
    }

    class IntComparator implements Comparator<Integer>
    {
        public int compare( Integer i1, Integer i2 )
        {
            return i1.compareTo( i2 );
        }
    }
}
