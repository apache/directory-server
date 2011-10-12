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
package org.apache.directory.server.core.partition.index;


import static junit.framework.Assert.assertFalse;

import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the {@link EmptyIndexCursor} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EmptyIndexCursorTest
{

    private EmptyIndexCursor<String, Entry, Long> indexCursor;


    @Before
    public void setUp()
    {
        indexCursor = new EmptyIndexCursor<String, Entry, Long>();
    }


    @Test
    public void testConstructor()
    {
        new EmptyIndexCursor<String, Entry, Long>();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testGet() throws Exception
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
    public void testBeforeFirst() throws Exception
    {
        indexCursor.beforeFirst();
    }


    @Test
    public void testAfterLast() throws Exception
    {
        indexCursor.afterLast();
    }


    @Test
    public void testFirst() throws Exception
    {
        assertFalse( indexCursor.first() );
    }


    @Test
    public void testLast() throws Exception
    {
        assertFalse( indexCursor.last() );
    }


    @Test
    public void testAvailable() throws Exception
    {
        assertFalse( indexCursor.available() );
    }


    @Test
    public void testNext() throws Exception
    {
        // not explicitly positioned, implicit before first 
        assertFalse( indexCursor.next() );

        // position before first
        indexCursor.beforeFirst();
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
        assertFalse( indexCursor.previous() );
    }


    @Test
    public void testBefore() throws Exception
    {
        indexCursor.before( null );
    }


    @Test
    public void testBeforeValue() throws Exception
    {
        indexCursor.beforeValue( 1L, "test" );
    }


    @Test
    public void testAfter() throws Exception
    {
        indexCursor.after( null );
    }


    @Test
    public void testAfterValue() throws Exception
    {
        indexCursor.afterValue( 1L, "test" );
    }


    @Test
    public void testClose() throws Exception
    {
        indexCursor.close();
    }

}
