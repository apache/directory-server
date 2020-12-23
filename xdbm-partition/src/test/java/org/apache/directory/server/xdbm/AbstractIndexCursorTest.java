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

import java.util.Iterator;

import org.apache.directory.api.ldap.model.cursor.CursorClosedException;
import org.apache.directory.api.ldap.model.cursor.DefaultClosureMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests the {@link AbstractIndexCursor} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class AbstractIndexCursorTest
{

    private AbstractIndexCursor<String> indexCursor;


    @BeforeEach
    public void setUp()
    {
        indexCursor = new EmptyIndexCursor<String>( new MockPartitionReadTxn() );
    }


    @AfterEach
    public void cleanup() throws Exception
    {
        if ( !indexCursor.isClosed() )
        {
            indexCursor.close();
        }
    }


    @Test
    public void testSetClosureMonitorNull()
    {
        assertThrows( IllegalArgumentException.class, () ->
        {
            indexCursor.setClosureMonitor( null );
        } );
    }


    @Test
    public void testSetClosureMonitor()
    {
        indexCursor.setClosureMonitor( new DefaultClosureMonitor() );
    }


    @Test
    public void testCheckNotClosedIfNotClosed() throws Exception
    {
        indexCursor.checkNotClosed();
    }


    @Test
    public void testCheckNotClosedIfClosed() throws Exception
    {
        assertThrows( CursorClosedException.class, () ->
        {
            indexCursor.close();
            indexCursor.checkNotClosed();
        } );
    }


    @Test
    public void testCheckNotClosedIfClosedWithCustomException() throws Exception
    {
        assertThrows( CursorClosedException.class, () ->
        {
            indexCursor.close( new IllegalArgumentException() );
            indexCursor.checkNotClosed();
        } );
    }


    @Test
    public void testIsClosed() throws Exception
    {
        assertFalse( indexCursor.isClosed() );
        indexCursor.close();
        assertTrue( indexCursor.isClosed() );
    }


    @Test
    public void testClose() throws Exception
    {
        indexCursor.close();
    }


    @Test
    public void testCloseException() throws Exception
    {
        indexCursor.close( new IllegalArgumentException() );
    }


    @Test
    public void testIterator()
    {
        Iterator<IndexEntry<String, String>> iterator = indexCursor.iterator();
        assertNotNull( iterator );
    }

}
