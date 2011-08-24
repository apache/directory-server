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
package org.apache.directory.server.xdbm.search.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.junit.Test;


/**
 * Tests {@link RdnIndexTreeCursor}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnIndexTreeCursorTest extends TestBase
{

    // This is how the test data looks like
    //   ForwardIndexEntry[ ParentIdAndRdn<0, 'o=Good Times Co.'>, 1 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<1, 'ou=Board of Directors'>, 3 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<1, 'ou=Engineering'>, 4 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<1, 'ou=Sales'>, 2 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<2, 'cn=JIM BEAN'>, 6 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<2, 'cn=JOhnny WAlkeR'>, 5 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<3, 'ou=Apache'>, 7 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<3, 'commonName=Jim Bean'>, 10 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<4, 'cn=Jack Daniels'>, 8 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<4, '2.5.4.3=Johnny Walker'>, 11 ]
    //   ForwardIndexEntry[ ParentIdAndRdn<7, 'commonName=Jim Bean'>, 9 ]

    @Test
    public void testOneLevelFromContextEntry() throws Exception
    {
        IndexCursor<Long, Entry, Long> cursor = store.getRdnIndexHelper().getOneLevelScopeCursor( 1L );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertHasNext( cursor, 1L, 3L );

        assertHasNext( cursor, 1L, 4L );

        assertHasNext( cursor, 1L, 2L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        assertHasFirst( cursor, 1L, 3L );

        assertHasNext( cursor, 1L, 4L );

        assertHasNext( cursor, 1L, 2L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertHasPrevious( cursor, 1L, 2L );

        assertHasPrevious( cursor, 1L, 4L );

        assertHasPrevious( cursor, 1L, 3L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        assertHasLast( cursor, 1L, 2L );

        assertHasPrevious( cursor, 1L, 4L );

        assertHasPrevious( cursor, 1L, 3L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testOneLevelFromApache() throws Exception
    {
        IndexCursor<Long, Entry, Long> cursor = store.getRdnIndexHelper().getOneLevelScopeCursor( 7L );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertHasNext( cursor, 7L, 9L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        assertHasFirst( cursor, 7L, 9L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertHasPrevious( cursor, 7L, 9L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        assertHasLast( cursor, 7L, 9L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testSubLevelFromContextEntry() throws Exception
    {
        IndexCursor<Long, Entry, Long> cursor = store.getRdnIndexHelper().getSubLevelScopeCursor( 1L );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertHasNext( cursor, 1L, 1L );
        assertHasNext( cursor, 1L, 3L );
        assertHasNext( cursor, 1L, 7L );
        assertHasNext( cursor, 1L, 9L );
        assertHasNext( cursor, 1L, 10L );
        assertHasNext( cursor, 1L, 4L );
        assertHasNext( cursor, 1L, 8L );
        assertHasNext( cursor, 1L, 11L );
        assertHasNext( cursor, 1L, 2L );
        assertHasNext( cursor, 1L, 6L );
        assertHasNext( cursor, 1L, 5L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        assertHasFirst( cursor, 1L, 1L );

        // --------- Test afterLast() ---------

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertHasPrevious( cursor, 1L, 5L );
        assertHasPrevious( cursor, 1L, 6L );
        assertHasPrevious( cursor, 1L, 2L );
        assertHasPrevious( cursor, 1L, 11L );
        assertHasPrevious( cursor, 1L, 8L );
        assertHasPrevious( cursor, 1L, 4L );
        assertHasPrevious( cursor, 1L, 10L );
        assertHasPrevious( cursor, 1L, 9L );
        assertHasPrevious( cursor, 1L, 7L );
        assertHasPrevious( cursor, 1L, 3L );
        assertHasPrevious( cursor, 1L, 1L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        assertHasLast( cursor, 1L, 5L );
    }


    @Test
    public void testSubLevelFromApache() throws Exception
    {
        IndexCursor<Long, Entry, Long> cursor = store.getRdnIndexHelper().getSubLevelScopeCursor( 7L );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertHasNext( cursor, 7L, 7L );
        assertHasNext( cursor, 7L, 9L );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        assertHasFirst( cursor, 7L, 7L );

        // --------- Test afterLast() ---------

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertHasPrevious( cursor, 7L, 9L );
        assertHasPrevious( cursor, 7L, 7L );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        assertHasLast( cursor, 7L, 9L );

    }


    private void assertHasNext( IndexCursor<Long, Entry, Long> cursor, Long value, Long id ) throws Exception
    {
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( value, indexEntry.getValue() );
        assertEquals( id, indexEntry.getId() );
    }


    private void assertHasPrevious( IndexCursor<Long, Entry, Long> cursor, Long value, Long id ) throws Exception
    {
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( value, indexEntry.getValue() );
        assertEquals( id, indexEntry.getId() );
    }


    private void assertHasFirst( IndexCursor<Long, Entry, Long> cursor, Long value, Long id ) throws Exception
    {
        assertTrue( cursor.first() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( value, indexEntry.getValue() );
        assertEquals( id, indexEntry.getId() );
    }


    private void assertHasLast( IndexCursor<Long, Entry, Long> cursor, Long value, Long id ) throws Exception
    {
        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( value, indexEntry.getValue() );
        assertEquals( id, indexEntry.getId() );
    }

}