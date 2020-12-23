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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.IntegerComparator;
import jdbm.helper.IntegerSerializer;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.BaseRecordManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test case to confirm the JDBM browser issue.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class JdbmBrowserBugTest
{
    Comparator<Integer> comparator;
    BTree<Integer, Integer> bt;
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final Logger LOG = LoggerFactory.getLogger( JdbmBrowserBugTest.class );
    private File dbFile = null;
    private RecordManager recman = null;


    @BeforeEach
    public void createTree() throws Exception
    {
        comparator = new Comparator<Integer>()
        {
            public int compare( Integer i1, Integer i2 )
            {
                return i1.compareTo( i2 );
            }
        };

        File tmpDir = null;

        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );
        bt = new BTree<Integer, Integer>( recman, new IntegerComparator(), new IntegerSerializer(),
            new IntegerSerializer() );
        LOG.debug( "created new BTree" );
    }


    @AfterEach
    public void cleanup() throws IOException
    {
        recman.close();
        recman = null;
        bt = null;

        if ( dbFile.exists() )
        {
            String fileToDelete = dbFile.getAbsolutePath();
            new File( fileToDelete ).delete();
            new File( fileToDelete + ".db" ).delete();
            new File( fileToDelete + ".lg" ).delete();

            dbFile.delete();
        }

        dbFile = null;
    }


    @Test
    public void testDirectionChange() throws Exception
    {
        bt.insert( 3, 3, true );
        bt.insert( 5, 3, true );
        bt.insert( 7, 3, true );
        bt.insert( 12, 3, true );
        bt.insert( 0, 3, true );
        bt.insert( 30, 3, true );
        bt.insert( 25, 3, true );

        Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>();
        TupleBrowser<Integer, Integer> browser = bt.browse( null );
        assertTrue( browser.getPrevious( tuple ) );
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals( Integer.valueOf( 30 ), tuple.getKey() );

        assertTrue( browser.getPrevious( tuple ) );
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals( Integer.valueOf( 25 ), tuple.getKey() );

        assertTrue( browser.getNext( tuple ) );
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals( tuple.getKey(),Integer.valueOf( 25 ), 
            "If this works the jdbm bug is gone: will start to return " +
                "30 instead as expected for correct operation");
    }
}
