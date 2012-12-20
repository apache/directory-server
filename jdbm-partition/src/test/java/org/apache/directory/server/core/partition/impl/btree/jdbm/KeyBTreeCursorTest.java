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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Comparator;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.api.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyBTreeCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( KeyBTreeCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    File dbFile;
    RecordManager recman;
    BTree<String, byte[]> bt;
    Comparator<String> comparator;

    KeyBTreeCursor<String> cursor;


    @Before
    public void createCursor() throws Exception
    {
        File tmpDir = null;

        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( KeyBTreeCursorTest.class.getName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );
        comparator = new StringComparator();
        bt = new BTree<String, byte[]>( recman, comparator );

        // add some data to it
        bt.insert( "0", Strings.EMPTY_BYTES, true );
        bt.insert( "1", Strings.EMPTY_BYTES, true );
        bt.insert( "2", Strings.EMPTY_BYTES, true );
        bt.insert( "3", Strings.EMPTY_BYTES, true );
        bt.insert( "4", Strings.EMPTY_BYTES, true );
        bt.insert( "5", Strings.EMPTY_BYTES, true );
        bt.insert( "6", Strings.EMPTY_BYTES, true );
        bt.insert( "7", Strings.EMPTY_BYTES, true );
        bt.insert( "8", Strings.EMPTY_BYTES, true );
        bt.insert( "9", Strings.EMPTY_BYTES, true );

        cursor = new KeyBTreeCursor<String>( bt, comparator );
        LOG.debug( "Created new KeyBTreeCursor and populated it's btree" );
    }


    @After
    public void destroyCursor() throws Exception
    {
        recman.close();
        recman = null;
        dbFile.deleteOnExit();

        String fileToDelete = dbFile.getAbsolutePath();
        new File( fileToDelete + ".db" ).delete();
        new File( fileToDelete + ".lg" ).delete();

        dbFile = null;

        cursor.close();
    }


    @Test
    public void testPreviousBeforePositioning() throws Exception
    {
        // test initial setup, advances after, and before inside elements
        assertInvalidCursor();

        assertTrue( cursor.previous() );
        assertEquals( "9", cursor.get() );
    }


    @Test
    public void testNextBeforePositioning() throws Exception
    {
        // test initial setup, advances after, and before inside elements
        assertInvalidCursor();

        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get() );
    }


    @Test
    public void testOperations() throws Exception
    {
        // test initial setup, advances after, and before inside elements
        assertInvalidCursor();

        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get() );

        cursor.after( "5" );
        assertInvalidCursor();
        assertTrue( cursor.next() );
        assertEquals( "6", cursor.get() );

        cursor.before( "2" );
        assertInvalidCursor();
        assertTrue( cursor.next() );
        assertEquals( "2", cursor.get() );

        // test advances up to and past the tail end
        cursor.after( "9" );
        assertInvalidCursor();
        assertFalse( cursor.next() );
        assertTrue( cursor.previous() );
        assertEquals( "9", cursor.get() );

        cursor.after( "a" );
        assertInvalidCursor();
        assertFalse( cursor.next() );
        assertTrue( cursor.previous() );
        assertEquals( "9", cursor.get() );

        cursor.before( "a" );
        assertInvalidCursor();
        assertFalse( cursor.next() );
        assertTrue( cursor.previous() );
        assertEquals( "9", cursor.get() );

        // test advances up to and past the head
        cursor.before( "0" );
        assertInvalidCursor();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get() );

        cursor.after( "*" );
        assertInvalidCursor();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get() );

        cursor.before( "*" );
        assertInvalidCursor();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( "0", cursor.get() );

        bt.remove( "0" );
        bt.remove( "1" );
        bt.remove( "2" );
        bt.remove( "3" );
        bt.remove( "4" );
        bt.remove( "6" );
        bt.remove( "7" );
        bt.remove( "8" );
        bt.remove( "9" );

        // now test with only one element: "5" remains now with others deleted
        cursor.before( "5" );
        assertInvalidCursor();
        assertFalse( cursor.previous() );
        assertTrue( cursor.next() );
        assertEquals( "5", cursor.get() );
        assertFalse( cursor.next() );

        cursor.after( "5" );
        assertInvalidCursor();
        assertFalse( cursor.next() );
        assertTrue( cursor.previous() );
        assertEquals( "5", cursor.get() );
        assertFalse( cursor.previous() );
    }


    @Test
    public void testJdbmBrowse() throws Exception
    {
        bt.remove( "0" );
        bt.remove( "5" );
        bt.remove( "6" );
        bt.remove( "7" );
        bt.remove( "9" );

        assertNull( bt.find( "0" ) );
        assertNotNull( bt.find( "1" ) );
        assertNotNull( bt.find( "2" ) );
        assertNotNull( bt.find( "3" ) );
        assertNotNull( bt.find( "4" ) );
        assertNull( bt.find( "5" ) );
        assertNull( bt.find( "6" ) );
        assertNull( bt.find( "7" ) );
        assertNotNull( bt.find( "8" ) );
        assertNull( bt.find( "9" ) );

        // browse will position us right after "4" and getNext() will return 8
        // since "5", "6", and "7" do not exist
        TupleBrowser<String, byte[]> browser = bt.browse( "5" );
        assertNotNull( browser );
        Tuple<String, byte[]> tuple = new Tuple<String, byte[]>();
        browser.getNext( tuple );
        assertEquals( "8", tuple.getKey() );

        // browse will position us right after "1" and getNext() will return 2
        // since "2" exists.
        browser = bt.browse( "2" );
        assertNotNull( browser );
        tuple = new Tuple<String, byte[]>();
        browser.getNext( tuple );
        assertEquals( "2", tuple.getKey() );

        // browse will position us right after "8" and getNext() will null
        // since nothing else exists past 8.  We've come to the end.
        browser = bt.browse( "9" );
        assertNotNull( browser );
        tuple = new Tuple<String, byte[]>();
        browser.getNext( tuple );
        assertNull( tuple.getKey() );

        // browse will position us right before "1" and getPrevious() will
        // null since nothing else exists before 1.  We've come to the end.
        // getNext() will however return "1".
        browser = bt.browse( "0" );
        assertNotNull( browser );
        tuple = new Tuple<String, byte[]>();
        browser.getPrevious( tuple );
        assertNull( tuple.getKey() );
        browser.getNext( tuple );
        assertEquals( "1", tuple.getKey() );
    }


    @Test
    public void testMiscelleneous() throws Exception
    {
        // Test available()

        assertFalse( cursor.available() );
        cursor.beforeFirst();
        assertFalse( cursor.available() );
        cursor.afterLast();
        assertFalse( cursor.available() );
        cursor.first();
        assertTrue( cursor.available() );
        cursor.last();
        assertTrue( cursor.available() );
    }


    private void assertInvalidCursor()
    {
        try
        {
            cursor.get();
            fail( "Invalid Cursor should not return valid value from get()" );
        }
        catch ( Exception e )
        {
            assertNotNull( e );
        }
    }
}
