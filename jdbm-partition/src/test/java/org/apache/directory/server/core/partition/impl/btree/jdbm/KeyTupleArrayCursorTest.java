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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.Comparator;

import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.xdbm.KeyTupleArrayCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * 
 * Test case for KeyTupleAvlCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class KeyTupleArrayCursorTest
{

    ArrayTree<Integer> tree;
    KeyTupleArrayCursor<Integer, Integer> cursor;

    private static final Integer KEY = Integer.valueOf( 1 );


    @Before
    public void createTree()
    {
        Comparator<Integer> comparator = new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }

        };

        tree = new ArrayTree<Integer>( comparator );

        cursor = new KeyTupleArrayCursor<Integer, Integer>( tree, KEY );
    }
    
    
    @After
    public void cleanup() throws Exception
    {
        cursor.close();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testEmptyCursor() throws Exception
    {
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );

        assertFalse( cursor.first() );
        assertFalse( cursor.last() );

        cursor.get(); // should throw InvalidCursorPositionException
    }


    @Test
    public void testNonEmptyCursor() throws Exception
    {
        tree.insert( 3 );
        tree.insert( 5 );
        tree.insert( 7 );
        tree.insert( 12 );
        tree.insert( 0 );
        tree.insert( 30 );
        tree.insert( 25 );

        cursor.before( new Tuple<Integer, Integer>( null, 3 ) );
        assertTrue( cursor.next() );
        assertEquals( 3, ( int ) cursor.get().getValue() );

        cursor.after( new Tuple<Integer, Integer>( null, 34 ) );
        assertFalse( cursor.next() );

        cursor.after( new Tuple<Integer, Integer>( null, 13 ) );
        assertTrue( cursor.next() );
        assertEquals( 25, ( int ) cursor.get().getValue() );

        cursor.beforeFirst();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( 0, ( int ) cursor.get().getValue() );

        cursor.afterLast();
        assertFalse( cursor.next() );

        assertTrue( cursor.first() );
        assertTrue( cursor.available() );
        assertEquals( 0, ( int ) cursor.get().getValue() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        assertEquals( 30, ( int ) cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertEquals( 25, ( int ) cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertEquals( 30, ( int ) cursor.get().getValue() );

    }
}
