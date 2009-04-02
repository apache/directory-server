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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Comparator;

import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.xdbm.Tuple;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 * AvlTreeMapNoDupsCursorTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlTreeMapNoDupsCursorTest
{
    AvlTreeMapImpl<Integer, Integer> tree;

    AvlTreeMapNoDupsCursor<Integer, Integer> cursor;


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

        tree = new AvlTreeMapImpl<Integer, Integer>( comparator, comparator, true );

        cursor = new AvlTreeMapNoDupsCursor<Integer, Integer>( tree );
    }


    @Test
    public void testEmptyCursor() throws Exception
    {
        assertFalse( cursor.isClosed() );
        assertFalse( cursor.available() );
        assertTrue( cursor.isElementReused() );

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

        cursor.before( new Tuple( 3, null ) );
        assertFalse( cursor.available() );

        cursor.after( new Tuple( 3, null ) );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testCursor() throws Exception
    {
        tree.insert( 7, 7 );
        assertFalse( cursor.isClosed() );
        assertFalse( cursor.available() );
        assertTrue( cursor.isElementReused() );

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
        assertEquals( 7, ( int ) cursor.get().getValue() );

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

        cursor.before( new Tuple( 3, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );

        cursor.after( new Tuple( 3, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );

        cursor.before( new Tuple( 7, null ) );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );
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
        assertTrue( cursor.isElementReused() );
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
        assertEquals( 3, ( int ) cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue() );
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( int ) cursor.get().getKey() );
        assertEquals( 10, ( int ) cursor.get().getValue() );
        
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( int ) cursor.get().getKey() );
        assertEquals( 3, ( int ) cursor.get().getValue() );
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

        cursor.after( new Tuple( 5, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( int ) cursor.get().getKey() );
        assertEquals( 7, ( int ) cursor.get().getValue() );
        
        cursor.before( new Tuple( 11, null ) );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( int ) cursor.get().getKey() );
        assertEquals( 11, ( int ) cursor.get().getValue() );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testAfterValue() throws Exception
    {
        cursor.afterValue( 0, 0 );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testBeforeValue() throws Exception
    {
        cursor.beforeValue( 0, 0 );
    }

    
    @Test
    public void testCursorWithDupValues() throws Exception
    {
        tree.insert( 3, 3 );
        tree.insert( 3, 7 );
        tree.insert( 3, 10 );
        tree.insert( 11, 11 );

        cursor.next();
        Tuple t = cursor.get();
     
        assertEquals( 3, t.getKey() );
        
        assertEquals( AvlTreeImpl.class, t.getValue().getClass() );
        
        AvlTree dupsTree = ( AvlTree ) t.getValue();
        assertEquals( 3, dupsTree.getSize() );
        
        AvlTreeCursor valCursor = new AvlTreeCursor<Integer>( dupsTree );
        
        assertTrue( valCursor.next() );
        assertEquals( 3, valCursor.get() );
        
        assertTrue( valCursor.next() );
        assertEquals( 7, valCursor.get() );
        
        assertTrue( valCursor.next() );
        assertEquals( 10, valCursor.get() );

        assertFalse( valCursor.next() );
    }
}
