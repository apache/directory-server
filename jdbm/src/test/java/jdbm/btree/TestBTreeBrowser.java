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
package jdbm.btree;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.SnapshotRecordManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 *  Tests proper function of {@link TupleBrowser} and {@link BPage.Browser}
 *  when structural changes happen on the BTree.
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TestBTreeBrowser
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private RecordManager recordManager;
    private SnapshotRecordManager snapshotRecman;
    private BTree<String, String> tree;
    private Tuple<String, String> tuple;


    private String getTemporaryFile( String name ) throws IOException
    {
        String file = folder.newFile( name ).getAbsolutePath();
        return file;
    }


    @Before
    public void setup() throws IOException
    {
        tuple = new Tuple<String, String>();
        recordManager = RecordManagerFactory.createRecordManager( getTemporaryFile( "testBrowser" ) );
        snapshotRecman = new SnapshotRecordManager( recordManager, 1 << 12 );
        tree = new BTree<String, String>( snapshotRecman, new StringComparator() );
        tree.setPageSize( 4 );

        // insert different objects and retrieve them
        tree.insert( "test15", "value1", false );
        tree.insert( "test25", "value2", false );
        tree.insert( "test35", "value3", false );
        tree.insert( "test45", "value4", false );
        tree.insert( "test55", "value5", false );
        tree.insert( "test65", "value6", false );
    }


    @After
    public void teardown() throws IOException
    {
        recordManager.close();
    }


    /**
     *  Test the browser.
     */
    @Test
    public void testBrowse() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        int count = 0;
        while ( browser.getNext( tuple ) )
        {
            count++;
        }
        assertEquals( 6, count );

        count = 0;
        while ( browser.getPrevious( tuple ) )
        {
            count++;
        }
        assertEquals( 6, count );
    }


    @Test
    public void testBrowseWithRemoveFirstBeforeStart() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        tree.remove( "test15" );
        tree.remove( "test55" );

        // removed tuples should still be visible
        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );
        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        tree.remove( "test65" );
        tree.remove( "test35" );

        // removed tuples should still be visible
        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithInsertFirstBeforeStart() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        tree.insert( "test11", "value1", false );
        tree.insert( "test59", "value5", false );

        // inserted tuples should not be visible
        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );
        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        tree.insert( "test29", "value2", false );
        tree.insert( "test69", "value6", false );

        // inserted tuples should not be visible
        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithRemoveFirstWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.remove( "test15" );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );

        tree.remove( "test65" );

        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithInsertFirstWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.insert( "test11", "value1", false );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );

        tree.insert( "test69", "value6", false );

        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithRemovePreviousWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.remove( "test35" );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );

        tree.remove( "test55" );

        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithInsertPreviousWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.insert( "test29", "value2", false );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );

        tree.insert( "test59", "value5", false );

        assertHasPrevious( browser, "test45", "value4" );
        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithRemoveNextWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.remove( "test45" );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );

        tree.remove( "test35" );

        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithInsertNextWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.insert( "test39", "value3", false );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );

        tree.insert( "test41", "value4", false );

        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithRemoveLastWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.remove( "test65" );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );

        tree.remove( "test15" );

        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    @Test
    public void testBrowseWithInsertLastWhileBrowsing() throws IOException
    {
        TupleBrowser<String, String> browser = tree.browse();

        assertHasNext( browser, "test15", "value1" );
        assertHasNext( browser, "test25", "value2" );
        assertHasNext( browser, "test35", "value3" );

        tree.insert( "test69", "value6", false );

        assertHasNext( browser, "test45", "value4" );
        assertHasNext( browser, "test55", "value5" );
        assertHasNext( browser, "test65", "value6" );
        assertFalse( browser.getNext( tuple ) );

        assertHasPrevious( browser, "test65", "value6" );
        assertHasPrevious( browser, "test55", "value5" );
        assertHasPrevious( browser, "test45", "value4" );

        tree.insert( "test11", "value1", false );

        assertHasPrevious( browser, "test35", "value3" );
        assertHasPrevious( browser, "test25", "value2" );
        assertHasPrevious( browser, "test15", "value1" );
        assertFalse( browser.getPrevious( tuple ) );
    }


    private void assertHasNext( TupleBrowser<String, String> browser, String key, String value ) throws IOException
    {
        assertTrue( browser.getNext( tuple ) );
        assertEquals( key, tuple.getKey() );
        assertEquals( value, tuple.getValue() );
    }


    private void assertHasPrevious( TupleBrowser<String, String> browser, String key, String value ) throws IOException
    {
        assertTrue( browser.getPrevious( tuple ) );
        assertEquals( key, tuple.getKey() );
        assertEquals( value, tuple.getValue() );
    }

}
