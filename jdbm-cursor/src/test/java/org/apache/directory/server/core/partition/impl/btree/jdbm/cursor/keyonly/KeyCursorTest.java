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
package org.apache.directory.server.core.partition.impl.btree.jdbm.cursor.keyonly;


import org.junit.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNull;

import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.cursor.keyonly.KeyCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.RecordManager;
import jdbm.helper.StringComparator;
import jdbm.helper.TupleBrowser;
import jdbm.helper.Tuple;
import jdbm.btree.BTree;
import jdbm.recman.BaseRecordManager;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;



/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( KeyCursorTest.class.getSimpleName() );
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    File dbFile;
    RecordManager recman;
    BTree bt;
    Comparator comparator;

    KeyCursor<String> cursor;


    @Before
    public void createCursor() throws Exception
    {
        File tmpDir = null;
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( KeyCursorTest.class.getName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );
        comparator = new StringComparator();
        bt = BTree.createInstance( recman, comparator );

        // add some data to it
        bt.insert( "0", EMPTY_BYTES, true );
        bt.insert( "1", EMPTY_BYTES, true );
        bt.insert( "2", EMPTY_BYTES, true );
        bt.insert( "3", EMPTY_BYTES, true );
        bt.insert( "4", EMPTY_BYTES, true );
        bt.insert( "5", EMPTY_BYTES, true );
        bt.insert( "6", EMPTY_BYTES, true );
        bt.insert( "7", EMPTY_BYTES, true );
        bt.insert( "8", EMPTY_BYTES, true );
        bt.insert( "9", EMPTY_BYTES, true );

        cursor = new KeyCursor<String>( bt );
    }


    @After
    public void destryCursor() throws Exception
    {
        recman.close();
        recman = null;
        dbFile.deleteOnExit();
        dbFile = null;
    }


    @Test
    public void testJdbmBehaviorBrowse() throws Exception
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
        TupleBrowser browser = bt.browse( "5" );
        assertNotNull( browser );
        Tuple tuple = new Tuple();
        browser.getNext( tuple );
        assertEquals( "8", tuple.getKey() );

        // browse will position us right after "1" and getNext() will return 2
        // since "2" exists.
        browser = bt.browse( "2" );
        assertNotNull( browser );
        tuple = new Tuple();
        browser.getNext( tuple );
        assertEquals( "2", tuple.getKey() );

        // browse will position us right after "8" and getNext() will null
        // since nothing else exists past 8.  We've come to the end.
        browser = bt.browse( "9" );
        assertNotNull( browser );
        tuple = new Tuple();
        browser.getNext( tuple );
        assertNull( tuple.getKey() );

        // browse will position us right before "1" and getPrevious() will
        // null since nothing else exists before 1.  We've come to the end.
        // getNext() will however return "1".
        browser = bt.browse( "0" );
        assertNotNull( browser );
        tuple = new Tuple();
        browser.getPrevious( tuple );
        assertNull( tuple.getKey() );
        browser.getNext( tuple );
        assertEquals( "1", tuple.getKey() );
    }


    private void assertInvalid( Cursor<String> cursor )
    {
        try
        {
            cursor.get();
            fail( "Invalid Cursor should not return valid value from get()" );
        }
        catch ( IOException e )
        {
            assertNotNull( e );
        }
    }
}