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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.server.core.partition.impl.btree.Table;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.schema.SerializableComparator;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;

import jdbm.RecordManager;
import jdbm.helper.IntegerSerializer;
import jdbm.recman.BaseRecordManager;


/**
 * Tests the Cursor functionality of a JdbmTable when duplicate keys are 
 * supported.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JdbmDupsCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmDupsCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final int SIZE = 15;

    transient Table<Integer,Integer> table;
    transient File dbFile;
    transient RecordManager recman;


    @Before
    public void createTable() throws Exception
    {
        File tmpDir = null;
        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        // gosh this is a terrible use of a global static variable
        SerializableComparator.setRegistry( new MockComparatorRegistry() );

        table = new JdbmTable<Integer,Integer>( "test", SIZE, recman,
                new SerializableComparator<Integer>( "" ),
                new SerializableComparator<Integer>( "" ),
                null, new IntegerSerializer() );
        LOG.debug( "Created new table and populated it with data" );
    }


    @After
    public void destryTable() throws Exception
    {
        table.close();
        table = null;
        recman.close();
        recman = null;
        dbFile.deleteOnExit();
        dbFile = null;
    }


    @Test
    public void testNextNoDups() throws Exception
    {
        // first try without duplicates at all
        for ( int ii = 0; ii < SIZE-1; ii++ )
        {
            table.put( ii, ii );
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();

        int ii = 0;
        while ( cursor.next() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii++;
        }
    }


    @Test
    public void testPreviousNoDups() throws Exception
    {
        for ( int ii = 0; ii < SIZE-1; ii++ )
        {
            table.put( ii, ii );
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();

        int ii = SIZE-2;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii--;
        }
    }


    @Test
    public void testNextDups() throws Exception
    {
        for ( int ii = 0; ii < SIZE*3; ii++ )
        {
            if ( ii > 12 && ii < 17 + SIZE )
            {
                table.put( 13, ii );
            }
            else
            {
                table.put( ii, ii );
            }
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();

        int ii = 0;
        while ( cursor.next() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 + SIZE )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii++;
        }
    }


    @Test
    public void testPreviousDups() throws Exception
    {
        for ( int ii = 0; ii < SIZE*3; ii++ )
        {
            if ( ii > 12 && ii < 17 + SIZE )
            {
                table.put( 13, ii );
            }
            else
            {
                table.put( ii, ii );
            }
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        cursor.afterLast();

        int ii = SIZE*3 - 1;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 + SIZE )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii--;
        }
    }


    @Test
    public void testFirstLastUnderDupLimit() throws Exception
    {
        for ( int ii = 0; ii < SIZE*2 - 1; ii++ )
        {
            if ( ii > 12 && ii < 17 )
            {
                table.put( 13, ii );
            }
            else
            {
                table.put( ii, ii );
            }
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();

        int ii = 0;
        while ( cursor.next() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii++;
        }

        cursor.first();
        ii = 0;
        do
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii++;
        }
        while ( cursor.next() );

        // now go backwards
        ii = SIZE*2-2;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii--;
        }

        // now advance to last and go backwards again
        cursor.last();
        ii = SIZE*2-2;
        do
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        ii = SIZE*2-2;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            if ( ii > 12 && ii < 17 )
            {
                assertEquals( 13, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            else
            {
                assertEquals( ii, ( int ) tuple.getKey() );
                assertEquals( ii, ( int ) tuple.getValue() );
            }
            ii--;
        }
    }


    @Test
    public void testFirstLastOverDupLimit() throws Exception
    {
        for ( int ii = 0; ii < SIZE*3-1; ii++ )
        {
            table.put( ii, ii );
        }
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();

        int ii = 0;
        while ( cursor.next() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii++;
        }

        // now go back to first and traverse all over again
        cursor.first();
        ii = 0;
        do
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii++;
        }
        while ( cursor.next() );

        // now go backwards
        ii = SIZE*3-2;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii--;
        }

        // now advance to last and go backwards again
        cursor.last();
        ii = SIZE*3-2;
        do
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        ii = SIZE*3-2;
        while ( cursor.previous() )
        {
            Tuple<Integer,Integer> tuple = cursor.get();
            assertEquals( ii, ( int ) tuple.getKey() );
            assertEquals( ii, ( int ) tuple.getValue() );
            ii--;
        }
    }


    @Test
    public void testOnEmptyTable() throws Exception
    {
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 1, 2 ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testOverDupLimit() throws Exception
    {
        table.put( 5, 5 );
        table.put( 6, 6 );
        for ( int ii = 0; ii < 20; ii++ )
        {
            table.put( 7, ii );
        }
        table.put( 8, 8 );
        table.put( 9, 9 );
        
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 7, 2 ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testUnderDupLimit() throws Exception
    {
        table.put( 5, 5 );
        table.put( 6, 6 );
        for ( int ii = 0; ii < 10; ii++ )
        {
            table.put( 7, ii );
        }
        table.put( 8, 8 );
        table.put( 9, 9 );
        
        Cursor<Tuple<Integer,Integer>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<Integer, Integer>( 7, 2 ) );
        assertFalse( cursor.available() );
    }
}
