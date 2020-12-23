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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.server.core.api.partition.Partition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests the {@link SingletonIndexCursor} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SingletonIndexCursorTest
{

    private IndexEntry<String, String> indexEntry;
    private SingletonIndexCursor<String> indexCursor;


    @BeforeEach
    public void setUp()
    {
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Partition.DEFAULT_ID );
        indexEntry.setEntry( new DefaultEntry() );
        indexEntry.setKey( "test" );
        indexCursor = new SingletonIndexCursor<String>( new MockPartitionReadTxn(), indexEntry );
    }


    @AfterEach
    public void cleanup() throws Exception
    {
        indexCursor.close();
    }


    @Test
    public void testConstructor() throws Exception
    {
        Cursor<IndexEntry<String, String>> cursor = new SingletonIndexCursor<String>( new MockPartitionReadTxn(), indexEntry );

        cursor.close();
    }


    @Test
    public void testGetNotPositioned() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            indexCursor.get();
        } );
    }


    @Test
    public void testGetBeforeFirst() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            indexCursor.beforeFirst();
            indexCursor.get();
        } );
    }


    @Test
    public void testGetAfterLast() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            indexCursor.afterLast();
            indexCursor.get();
        } );
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


    @Test
    public void testBefore() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () ->
        {
            indexCursor.before( null );
        } );
    }


    @Test
    public void testAfter() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () ->
        {
            indexCursor.after( null );
        } );
    }
}
