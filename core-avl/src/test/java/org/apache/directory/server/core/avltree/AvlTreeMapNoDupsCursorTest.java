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
package org.apache.directory.server.core.avltree;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Comparator;

import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * 
 * AvlTreeMapNoDupsCursorTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.CONCURRENT)
public class AvlTreeMapNoDupsCursorTest
{
    AvlTreeMap<Integer, Integer> tree;

    AvlSingletonOrOrderedSetCursor<Integer, Integer> cursor;


    @BeforeEach
    public void createTree()
    {
        Comparator<Integer> comparator = new Comparator<Integer>()
        {

            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }

        };

        tree = new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, true );

        cursor = new AvlSingletonOrOrderedSetCursor<Integer, Integer>( tree );
    }
    
    
    @AfterEach
    public void cleanup() throws Exception
    {
        cursor.close();
    }


    @Test
    public void testEmptyCursor() throws Exception
    {
        assertFalse( cursor.isClosed() );
        assertFalse( cursor.available() );

        try
        {
            cursor.get();
            fail( "Should not get here" );
        }
        catch ( InvalidCursorPositionException e )
        {
            assertNotNull( e );
        }

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertFalse( cursor.first() );
        assertFalse( cursor.available() );

        assertFalse( cursor.last() );
        assertFalse( cursor.available() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursor() throws Exception
    {
        tree.insert( 7, 7 );
        assertFalse( cursor.isClosed() );
        assertFalse( cursor.available() );

        try
        {
            cursor.get();
            fail( "Should not get here" );
        }
        catch ( InvalidCursorPositionException e )
        {
            assertNotNull( e );
        }

        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.afterLast();
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertTrue( cursor.first() );
        assertTrue( cursor.available() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 7, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 7, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 7, null ) );
        assertFalse( cursor.available() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 7, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 9, null ) );
        assertFalse( cursor.available() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 9, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );
    }


    @Test
    public void testManyEntriesCursor() throws Exception
    {
        tree.insert( 3, 3 );
        tree.insert( 7, 7 );
        tree.insert( 10, 10 );
        tree.insert( 11, 11 );

        assertFalse( cursor.isClosed() );
        assertFalse( cursor.available() );
        assertEquals( 4, tree.getSize() );

        try
        {
            cursor.get();
            fail( "Should not get here" );
        }
        catch ( InvalidCursorPositionException e )
        {
            assertNotNull( e );
        }

        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue().getSingleton() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue().getSingleton() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        assertTrue( cursor.first() );
        assertTrue( cursor.available() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );

        // position before first object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 2, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 2, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // position on first object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        // position after first object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 5, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 5, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        // position before last object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 10, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 10, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue().getSingleton() );

        // position on last object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 11, null ) );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 11, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        // position after last object
        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 20, null ) );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.after( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 20, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        // position after last object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 20, null ) );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 20, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        // position on last object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 11, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 11, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue().getSingleton() );

        // position before last object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 10, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 10, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        // position after first object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 5, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 5, null ) );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        // position on first object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 3, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // position before first object
        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 2, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue().getSingleton() );

        cursor.before( new Tuple<Integer, SingletonOrOrderedSet<Integer>>( 2, null ) );
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testAfterValue() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () -> cursor.afterValue( 0, null ) );
    }


    @Test
    public void testBeforeValue() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () -> cursor.beforeValue( 0, null ) );
    }


    @Test
    public void testCursorWithDupValues() throws Exception
    {
        tree.insert( 3, 3 );
        tree.insert( 3, 7 );
        tree.insert( 3, 10 );
        tree.insert( 11, 11 );

        cursor.next();
        Tuple<Integer, SingletonOrOrderedSet<Integer>> t = cursor.get();

        assertEquals( 3, t.getKey().intValue() );

        assertEquals( AvlTreeImpl.class, t.getValue().getOrderedSet().getClass() );

        AvlTree<Integer> dupsTree = t.getValue().getOrderedSet();
        assertEquals( 3, dupsTree.getSize() );

        AvlTreeCursor<Integer> valCursor = new AvlTreeCursor<Integer>( dupsTree );

        assertTrue( valCursor.next() );
        assertEquals( 3, valCursor.get().intValue() );

        assertTrue( valCursor.next() );
        assertEquals( 7, valCursor.get().intValue() );

        assertTrue( valCursor.next() );
        assertEquals( 10, valCursor.get().intValue() );

        assertFalse( valCursor.next() );
        valCursor.close();
    }
}
