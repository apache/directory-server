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
import static org.junit.Assert.fail;

import java.io.File;

import jdbm.RecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.server.core.api.partition.index.Table;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the Cursor functionality of a JdbmTable when duplicate keys are 
 * supported.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DupsCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( DupsCursorTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final int SIZE = 15;

    Table<String,String> table;
    File dbFile;
    RecordManager recman;
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = DupsContainerCursorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }

    
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

        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
                comparator, comparator, null, new DefaultSerializer() );
        LOG.debug( "Created new table and populated it with data" );
    }


    @After
    public void destroyTable() throws Exception
    {
        table.close();
        table = null;
        recman.close();
        recman = null;
        dbFile.deleteOnExit();
        // Remove temporary files
        String fileToDelete = dbFile.getAbsolutePath();
        new File( fileToDelete ).delete();
        new File( fileToDelete + ".db" ).delete();
        new File( fileToDelete + ".lg" ).delete();
        
        dbFile = null;
    }


    @Test
    public void testEmptyTableOperations() throws Exception
    {
        Cursor<Tuple<String,String>> cursor = table.cursor();
        assertFalse( cursor.next() );
        
        cursor.afterLast();
        assertFalse( cursor.previous() );

        cursor.beforeFirst();
        assertFalse( cursor.next() );

        assertFalse( cursor.first() );
        assertFalse( cursor.last() );
    }


    @Test
    public void testNextNoDups() throws Exception
    {
        // first try without duplicates at all
        for ( int i = 0; i < SIZE-1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }

        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            assertEquals( i, Integer.parseInt( tuple.getKey() ) );
            assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            i++;
        }
    }


    @Test
    public void testPreviousNoDups() throws Exception
    {
        for ( int i = 0; i < SIZE-1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }

        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = SIZE-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            assertEquals( i, Integer.parseInt( tuple.getKey() ) );
            assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            i--;
        }
    }


    @Test
    public void testNextDups() throws Exception
    {
        for ( int i = 0; i < SIZE*3; i++ )
        {
            String istr = Integer.toString( i );
            
            if ( i > 12 && i < 17 + SIZE )
            {
                table.put( "13", istr );
            }
            else
            {
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
    }


    @Test
    public void testPreviousDups() throws Exception
    {
        for ( int i = 0; i < SIZE*3; i++ )
        {
            String istr = Integer.toString( i );
            
            if ( i > 12 && i < 17 + SIZE )
            {
                table.put( "13", Integer.toString( i ) );
            }
            else
            {
                
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();
        cursor.afterLast();

        int i = SIZE*3 - 1;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
    }


    @Test
    public void testFirstLastUnderDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*2 - 1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i > 12 && i < 17 )
            {
                table.put( "13", istr );
            }
            else
            {
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        cursor.first();
        i = 0;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
        while ( cursor.next() );

        // now go backwards
        cursor.afterLast();
        i = SIZE*2-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }

        // now advance to last and go backwards again
        cursor.last();
        i = SIZE*2-2;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        i = SIZE*2-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
    }


    @Test
    public void testFirstLastOverDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*3-1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i < 2 + SIZE ) // keys with multiple values
            {
                table.put( "0", istr );
            }
            else // keys with single values
            {
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // now go back to first and traverse all over again
        cursor.first();
        i = 0;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
        while ( cursor.next() );

        // now go backwards
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();

            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }

        // now advance to last and go backwards again
        cursor.last();
        i = SIZE*3-2;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
    }


    @Test
    public void testFirstOverDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*3-1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i < 2 + SIZE ) // keys with multiple values
            {
                table.put( "0", istr );
            }
            else // keys with single values
            {
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // now go back to first and traverse all over again
        cursor.first();
        i = 0;
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
        while ( cursor.next() );

        // now go backwards
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();

            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            i--;
        }

        // now advance to last and go backwards again
        cursor.last();
        i = SIZE*3-2;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i < 2 + SIZE )
            {
                assertEquals( 0, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
    }


    @Test
    public void testLastOverDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*3-1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i > 2 + SIZE ) // keys with multiple values
            {
                table.put( Integer.toString( 3 + SIZE ), istr );
            }
            else // keys with single values
            {
                table.put( istr, istr );
            }
        }
        
        Cursor<Tuple<String,String>> cursor = table.cursor();

        int i = 0;
        
        while ( cursor.next() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 2 + SIZE )
            {
                assertEquals( 3 + SIZE, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // now go back to first and traverse all over again
        cursor.first();
        i = 0;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 2 + SIZE )
            {
                assertEquals( 3 + SIZE, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
        while ( cursor.next() );

        // now go backwards
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();

            if ( i > 2 + SIZE )
            {
                assertEquals( 3 + SIZE, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }

        // now advance to last and go backwards again
        cursor.last();
        i = SIZE*3-2;
        
        do
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 2 + SIZE )
            {
                assertEquals( 3 + SIZE, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
        while ( cursor.previous() );

        // advance to first then last and go backwards again
        cursor.beforeFirst();
        cursor.afterLast();
        i = SIZE*3-2;
        
        while ( cursor.previous() )
        {
            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 2 + SIZE )
            {
                assertEquals( 3 + SIZE, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i--;
        }
    }


    @Test
    public void testOnEmptyTable() throws Exception
    {
        Cursor<Tuple<String,String>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<String, String>( "1", "2" ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testOverDupLimit() throws Exception
    {
        table.put( "5", "5" );
        table.put( "6", "6" );
        
        for ( int i = 0; i < 20; i++ )
        {
            table.put( "7", Integer.toString( i ) );
        }
        
        table.put( "8", "8" );
        table.put( "9", "9" );
        
        Cursor<Tuple<String,String>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<String, String>( "7", "2" ) );
        assertFalse( cursor.available() );
    }

    
    @Test
    public void testUnderDupLimit() throws Exception
    {
        table.put( "5", "5" );
        table.put( "6", "6" );
        
        for ( int i = 0; i < 10; i++ )
        {
            table.put( "7", Integer.toString( i ) );
        }
        
        table.put( "8", "8" );
        table.put( "9", "9" );
        
        Cursor<Tuple<String,String>> cursor = table.cursor();
        assertNotNull( cursor );
        assertFalse( cursor.isClosed() );
        
        cursor.before( new Tuple<String, String>( "7", "2" ) );
        assertFalse( cursor.available() );
    }


    @Test
    public void testBeforeAfterBelowDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*2 - 1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i > 12 && i < 17 ) // keys with multiple values
            {
                table.put( "13", Integer.toString( i ) );
            }
            else if ( i > 17 && i < 21 ) // adds hole with no keys for i
            {
            }
            else // keys with single values
            {
                table.put( istr, istr );
            }
        }

        // test before to advance just before a key with a single value
        int i = 5;
        Cursor<Tuple<String,String>> cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "5", "5" ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test before to advance just before a key with a single value but
        // with a null tuple value which should not advance the dupsCursor
        i = 5;
        cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "5", null ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            i++;
        }

        // test before to advance just before a key value pair where the key
        // does not exist - using value so we hit check for key equality
        i = 21;
        cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "18", "18" ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just after the end
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "111", null ) );
        assertFalse( cursor.next() );

        // test after to advance just before a key with a single value
        i = 6;
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "5", null ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test before to advance just before a key & value with multiple
        // values for the key - we should advance just before the value
        cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "13", "14" ) );

        cursor.next();
        Tuple<String,String> tuple = cursor.get();
        assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 14, Integer.parseInt( tuple.getValue() ) );
        i = 15;

        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            i++;
        }

        // test after to advance just before a key & value with multiple
        // values for the key - we should advance just before the value
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "13", "14" ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 15, Integer.parseInt( tuple.getValue() ) );
        i=16;

        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just before a key that does not exist
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "18", null ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 21, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 21, Integer.parseInt( tuple.getValue() ) );
        i=22;

        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just before a key and value where the key
        // does not exist - used to force key comparison in after()
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "18", "18" ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 21, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 21, Integer.parseInt( tuple.getValue() ) );
        i=22;

        while ( cursor.next() )
        {
            if ( i > 17 && i < 21 )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
    }


    @Test
    public void testBeforeAfterOverDupLimit() throws Exception
    {
        for ( int i = 0; i < SIZE*3 - 1; i++ )
        {
            String istr = Integer.toString( i );

            if ( i > 12 && i < 17 + SIZE ) // keys with multiple values
            {
                table.put( "13", Integer.toString( i ) );
            }
            else if ( i > 17 + SIZE  && i < 21 + SIZE ) // adds hole with no keys for i
            {
            }
            else // keys with single values
            {
                table.put( istr, istr );
            }
        }

        // test before to advance just before a key with a single value
        int i = 5;
        Cursor<Tuple<String,String>> cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "5", "5" ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test before to advance just before a key with a single value but
        // with a null tuple value which should not advance the dupsCursor
        i = 5;
        cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "5", null ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            i++;
        }

        // test before to advance just before a key value pair where the key
        // does not exist - using value so we hit check for key equality
        i = 21 + SIZE;
        cursor = table.cursor();
        String istr = Integer.toString( 18 + SIZE );
        
        cursor.before( new Tuple<String,String>( istr, istr ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just after the end
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "111", null ) );
        assertFalse( cursor.next() );

        // test after to advance just before a key with a single value
        i = 6;
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "5", null ) );
        
        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            Tuple<String,String> tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test before to advance just before a key & value with multiple
        // values for the key - we should advance just before the value
        cursor = table.cursor();
        cursor.before( new Tuple<String,String>( "13", "14" ) );

        cursor.next();
        Tuple<String,String> tuple = cursor.get();
        assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 14, Integer.parseInt( tuple.getValue() ) );
        i = 15;

        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just before a key & value with multiple
        // values for the key - we should advance just before the value
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( "13", "14" ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 15, Integer.parseInt( tuple.getValue() ) );
        i=16;

        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString(  i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }

        // test after to advance just before a key that does not exist
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( Integer.toString( 18 + SIZE ), null ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 21 + SIZE, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 21 + SIZE, Integer.parseInt( tuple.getValue() ) );
        i=22 + SIZE;

        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            tuple = cursor.get();
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            i++;
        }

        // test after to advance just before a key and value where the key
        // does not exist - used to force key comparison in after()
        cursor = table.cursor();
        cursor.after( new Tuple<String,String>( istr, istr ) );

        cursor.next();
        tuple = cursor.get();
        assertEquals( 21 + SIZE, Integer.parseInt( tuple.getKey() ) );
        assertEquals( 21 + SIZE, Integer.parseInt( tuple.getValue() ) );
        i=22+ SIZE;

        while ( cursor.next() )
        {
            if ( i > 17 + SIZE && i < 21 + SIZE )
            {
                assertFalse( table.has( Integer.toString( i ) ) );
                continue;
            }

            tuple = cursor.get();
            
            if ( i > 12 && i < 17 + SIZE )
            {
                assertEquals( 13, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            else
            {
                assertEquals( i, Integer.parseInt( tuple.getKey() ) );
                assertEquals( i, Integer.parseInt( tuple.getValue() ) );
            }
            
            i++;
        }
    }


    @Test
    public void testMiscellaneous() throws Exception
    {
        Cursor<Tuple<String,String>> cursor = table.cursor();
        assertNotNull( cursor );

        try
        {
            cursor.get();
            fail( "Should never get here due to invalid cursor position exception." );
        }
        catch( InvalidCursorPositionException e )
        {
        }
    }
}
