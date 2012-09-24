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
package org.apache.directory.server.xdbm;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the {@link SingletonIndexCursor} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingletonIndexCursorTest
{

    private IndexEntry<String, String> indexEntry;
    private SingletonIndexCursor<String> indexCursor;


    @Before
    public void setUp()
    {
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Partition.DEFAULT_ID );
        indexEntry.setEntry( new DefaultEntry() );
        indexEntry.setKey( "test" );
        indexCursor = new SingletonIndexCursor<String>( indexEntry );
    }


    @After
    public void cleanup() throws Exception
    {
        indexCursor.close();
    }


    @Test
    public void testConstructor() throws Exception
    {
        Cursor cursor = new SingletonIndexCursor<String>( indexEntry );

        cursor.close();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testGetNotPositioned() throws Exception
    {
        indexCursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testGetBeforeFirst() throws Exception
    {
        indexCursor.beforeFirst();
        indexCursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testGetAfterLast() throws Exception
    {
        indexCursor.afterLast();
        indexCursor.get();
    }


    @Test
    public void testGet() throws Exception
    {
        // not positioned
        indexCursor.next();
        assertNotNull( indexCursor.get() );

        indexCursor.first();
        assertNotNull( indexCursor.get() );

        indexCursor.last();
        assertNotNull( indexCursor.get() );

        indexCursor.afterLast();
        assertTrue( indexCursor.previous() );
        assertNotNull( indexCursor.get() );

        indexCursor.beforeFirst();
        assertTrue( indexCursor.next() );
        assertNotNull( indexCursor.get() );
    }


    @Test
    public void testBeforeFirst() throws Exception
    {
        // not explicitly positioned, implicit before first 
        assertTrue( indexCursor.isBeforeFirst() );

        indexCursor.first();
        assertFalse( indexCursor.isBeforeFirst() );

        indexCursor.beforeFirst();
        assertTrue( indexCursor.isBeforeFirst() );
    }


    @Test
    public void testAfterLast() throws Exception
    {
        assertFalse( indexCursor.isAfterLast() );

        indexCursor.afterLast();
        assertTrue( indexCursor.isAfterLast() );
    }


    @Test
    public void testFirst() throws Exception
    {
        assertFalse( indexCursor.isFirst() );

        assertTrue( indexCursor.first() );
        assertTrue( indexCursor.isFirst() );
        assertNotNull( indexCursor.get() );
    }


    @Test
    public void testLast() throws Exception
    {
        assertFalse( indexCursor.isLast() );

        assertTrue( indexCursor.last() );
        assertTrue( indexCursor.isLast() );
        assertNotNull( indexCursor.get() );
    }


    @Test
    public void testAvailable() throws Exception
    {
        assertFalse( indexCursor.available() );

        indexCursor.first();
        assertTrue( indexCursor.available() );

        indexCursor.last();
        assertTrue( indexCursor.available() );

        indexCursor.afterLast();
        assertFalse( indexCursor.available() );

        indexCursor.beforeFirst();
        assertFalse( indexCursor.available() );
    }


    @Test
    public void testNext() throws Exception
    {
        // not explicitly positioned, implicit before first 
        assertTrue( indexCursor.next() );
        assertFalse( indexCursor.next() );

        // position before first
        indexCursor.beforeFirst();
        assertTrue( indexCursor.next() );
        assertFalse( indexCursor.next() );

        // position first
        indexCursor.first();
        assertFalse( indexCursor.next() );

        // position last
        indexCursor.last();
        assertFalse( indexCursor.next() );

        // position after first
        indexCursor.afterLast();
        assertFalse( indexCursor.next() );
    }


    @Test
    public void testPrevious() throws Exception
    {
        // not positioned
        assertFalse( indexCursor.previous() );

        // position before first
        indexCursor.beforeFirst();
        assertFalse( indexCursor.previous() );

        // position first
        indexCursor.first();
        assertFalse( indexCursor.previous() );

        // position last
        indexCursor.last();
        assertFalse( indexCursor.previous() );

        // position after first
        indexCursor.afterLast();
        assertTrue( indexCursor.previous() );
        assertFalse( indexCursor.previous() );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testBefore() throws Exception
    {
        indexCursor.before( null );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testAfter() throws Exception
    {
        indexCursor.after( null );
    }
}
