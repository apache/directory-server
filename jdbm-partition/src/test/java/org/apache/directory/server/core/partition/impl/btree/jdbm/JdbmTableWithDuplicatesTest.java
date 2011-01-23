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

import jdbm.RecordManager;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.IntegerSerializer;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
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
 * Tests JdbmTable operations with duplicates.  Does not test Cursor capabilities.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmTableWithDuplicatesTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmTableWithDuplicatesTest.class.getSimpleName() );
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final int SIZE = 15;
    private static final int SIZE2 = 30;

    private static final String SIZE_MINUS_ONE_STR = "14";
    private static final String SIZE_STR = "15";
    private static final String SIZE_PLUS_ONE_STR = "16";

    private static final String SIZE2_MINUS_ONE_STR = "29";
    private static final String SIZE2_STR = "30";
    private static final String SIZE2_PLUS_ONE_STR = "31";
    
    transient JdbmTable<String,String> table;
    transient File dbFile;
    transient RecordManager recman;
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
        destryTable();
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
                comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );
        LOG.debug( "Created new table and populated it with data" );
    }


    @After
    public void destryTable() throws Exception
    {
        if ( table != null )
        {
            table.close();
        }

        table = null;

        if ( recman != null )
        {
            recman.close();
        }

        recman = null;

        if ( dbFile != null )
        {
            String fileToDelete = dbFile.getAbsolutePath();
            new File( fileToDelete + ".db" ).delete();
            new File( fileToDelete + ".lg" ).delete();

            dbFile.delete();
        }

        dbFile = null;
    }


    @Test
    public void testSerializers() throws Exception
    {
        assertNotNull( table.getKeySerializer() );
        assertNotNull( ( ( JdbmTable<?,?> ) table ).getValueSerializer() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        assertEquals( 0, table.count( "3" ) );
        assertEquals( 0, table.count( null ) );
    }


    @Test( expected = IllegalArgumentException.class )
    public void testNullKeyComparator() throws Exception
    {
        assertNotNull( table.getKeyComparator() );

        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
            null, comparator, null, new IntegerSerializer() );
    }


    @Test( expected = IllegalArgumentException.class )
    public void testNullValueComparator() throws Exception
    {
        assertNotNull( table.getValueComparator() );

        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
            comparator, null, null, new IntegerSerializer() );
    }


    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( "1", "2" );
        assertEquals( "2", table.get( "1" ) );
        table.close();
        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
                comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );
        assertEquals( "2", table.get( "1" ) );
    }

    
    @Test 
    public void testConfigMethods() throws Exception
    {
        assertTrue( table.isDupsEnabled() );
        assertEquals( "test", table.getName() );
        assertNotNull( table.getKeyComparator() );
    }

    
    @Test
    public void testWhenEmpty() throws Exception
    {
        // Test the count methods
        assertEquals( 0, table.count() );
        assertEquals( 0, table.count( "1" ) );

        // Test get method
        assertNull( table.get( "0" ) );
        assertNull( table.get( null ) );

        // Test remove methods
        table.remove( "1" );
        assertFalse( table.has( "1" ) );
        
        // Test has operations
        assertFalse( table.has( "1" ) );
        assertFalse( table.has( "1", "0" ) );
        assertFalse( table.hasGreaterOrEqual( "1" ) );
        assertFalse( table.hasLessOrEqual( "1" ) );
        assertFalse( table.hasGreaterOrEqual( "1", "0" ) );
        assertFalse( table.hasLessOrEqual( "1", "0" ) );
    }


    @Test
    public void testPut() throws Exception
    {
        final int SIZE = 15;

        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }
        
        assertEquals( SIZE, table.count() );
        table.put( "0", "0" );
        assertTrue( table.has( "0", "0" ) );

        // add some duplicates
        for ( int i = 0; i < SIZE*2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( SIZE2_STR, istr );
        }
        
        assertEquals( SIZE*3, table.count() );
        
        table.put( "0", "0" );
        assertTrue( table.has( "0", "0" ) );
        
        table.put( SIZE2_STR, "0" );
        assertTrue( table.has( SIZE2_STR, "0" ) );
    }


    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( "1" ) );
        
        for ( int i = 0; i < SIZE*2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( "1", istr );
        }
        
        assertEquals( SIZE2, table.count() );

        assertTrue( table.has( "1" ) );
        assertTrue( table.has( "1", "0" ) );
        assertFalse( table.has( "1", SIZE2_STR ) );

        assertTrue( table.hasGreaterOrEqual( "1", "0" ) );
        assertTrue( table.hasLessOrEqual( "1", "0" ) );
        assertFalse( table.hasLessOrEqual( "1", "-1" ) );

        assertTrue( table.hasGreaterOrEqual( "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasGreaterOrEqual( "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( "1", SIZE2_STR ) );
        assertFalse( table.hasGreaterOrEqual( "1", SIZE2_STR ) );
        assertFalse( table.has( "1", SIZE2_STR ) );

        // let's go over the this limit now and ask the same questions
        table.put( "1", SIZE2_STR );

        assertTrue( table.has( "1" ) );
        assertTrue( table.has( "1", "0" ) );
        assertTrue( table.has( "1", SIZE2_STR ) );
        assertFalse( table.has( null, null ) );

        assertTrue( table.hasGreaterOrEqual( "1", "0" ) );
        assertTrue( table.hasLessOrEqual( "1", "0" ) );
        assertFalse( table.hasLessOrEqual( "1", "-1" ) );
        assertFalse( table.hasGreaterOrEqual( null, null ) );
        assertFalse( table.hasLessOrEqual( null, null ) );

        assertTrue( table.hasGreaterOrEqual( "1", SIZE2_STR ) );
        assertTrue( table.hasLessOrEqual( "1", SIZE2_STR ) );
        assertTrue( table.hasGreaterOrEqual( "1", SIZE2_STR ) );
        assertTrue( table.hasLessOrEqual( "1", SIZE2_STR ) );
        assertFalse( table.hasGreaterOrEqual( "1", SIZE2_PLUS_ONE_STR ) );
        assertFalse( table.has( "1", SIZE2_PLUS_ONE_STR ) );
        
        // now do not add duplicates and check has( key, boolean )
        for ( int i = 0; i < SIZE; i++ )
        {
            // note we are not adding duplicates not put( 1, i )
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }
        
        assertFalse( table.has( "-1" ) );
        assertTrue( table.hasGreaterOrEqual( "-1" ) );
        assertFalse( table.hasLessOrEqual( "-1" ) );
        
        assertTrue( table.has( "0" ) );
        assertTrue( table.hasGreaterOrEqual( "0" ) );
        assertTrue( table.hasLessOrEqual( "0" ) );
        
        assertTrue( table.has( SIZE_MINUS_ONE_STR ) );
        assertTrue( table.hasGreaterOrEqual( SIZE_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( SIZE_MINUS_ONE_STR ) );
        
        assertFalse( table.has( SIZE_STR ) );
        assertFalse( table.hasGreaterOrEqual( SIZE_STR ) );
        assertTrue( table.hasLessOrEqual( SIZE_STR ) );

        for ( int i = 0; i < SIZE; i++ )
        {
            if ( i == 1 ) // don't delete the node which had multiple values
            {
                continue;
            }
            
            String istr = Integer.toString( i );
            table.remove( istr, istr );
        }
        
        // delete all values of the duplicate key one by one
        for ( int i = 0; i < SIZE * 2 + 1; i++ )
        {
            String istr = Integer.toString( i );
            table.remove( "1", istr );
        }

        Cursor<Tuple<String, String>> cursor = table.cursor();

        cursor.beforeFirst();
        
        while ( cursor.next() )
        {
            //System.out.println( cursor.get() );
        }

        assertFalse( table.hasLessOrEqual( "1" ) );
        assertFalse( table.hasLessOrEqual( "1", "10" ) );
        assertFalse( table.hasGreaterOrEqual( "1" ) );
        assertFalse( table.hasGreaterOrEqual( "1", "0" ) );

        table.put( "1", "0" );

    }

    
    @Test
    public void testRemove() throws Exception
    {
        assertEquals( 0, table.count() );

        table.put( "1", "1" );
        table.put( "1", "2" );
        assertEquals( 2, table.count() );
        table.remove( "1" );
        assertFalse( table.has( "1" ) );
        assertEquals( 0, table.count() );

        table.put( "10", "10" );
        assertEquals( 1, table.count() );
        table.remove( "10", "11" );
        assertFalse( table.has( "10", "11" ) );
        assertEquals( 1, table.count() );
        table.remove( "10", "10" );
        assertFalse( table.has( "10", "10" ) );
        assertEquals( 0, table.count() );

        // add duplicates
        for ( int i = 0; i < SIZE*2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( "0", istr );
        }

        assertEquals( SIZE*2, table.count() );
        table.remove( "0", "100" );
        assertFalse( table.has( "0", "100" ) );
        assertEquals( SIZE*2, table.count() );
        
        table.remove( "0" );
        assertNull( table.get( "0" ) );
    }
    
    
    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( istr, istr );
        }
        
        assertEquals( 15, table.count() );
        assertEquals( 1, table.count( "0" ) );
        
        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */
        
        if ( table.isCountExact() )
        {
            assertEquals( 5, table.lessThanCount( "5" ) );
            assertEquals( 9, table.greaterThanCount( "5" ) );
        }
        else
        {
            assertEquals( SIZE, table.lessThanCount( "5" ) );
            assertEquals( SIZE, table.greaterThanCount( "5" ) );
        }
    }
    

    @Test
    public void testDuplicateLimit() throws Exception
    {
        assertFalse( table.isKeyUsingBTree( "1" ) );
        
        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( "1", istr );
        }
        assertEquals( SIZE, table.count() );
        assertEquals( SIZE, table.count( "1" ) );
        assertFalse( table.isKeyUsingBTree( "1" ) );
        
        // this switches to B+Trees from AvlTree
        table.put( "1", SIZE_STR );
        assertEquals( SIZE + 1, table.count() );
        assertEquals( SIZE + 1, table.count( "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        // go one more over still a B+Tree
        table.put( "1", SIZE_PLUS_ONE_STR );
        assertEquals( SIZE + 2, table.count() );
        assertEquals( SIZE + 2, table.count( "1" ) );
        assertEquals( "0", table.get( "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );
        
        // now start removing and see what happens 
        table.remove( "1", SIZE_PLUS_ONE_STR );
        assertFalse( table.has( "1", SIZE_PLUS_ONE_STR ) );
        assertTrue( table.has( "1", SIZE_STR ) );
        assertEquals( SIZE + 1, table.count() );
        assertEquals( SIZE + 1, table.count( "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        // this switches to AvlTree from B+Trees
        table.remove( "1", SIZE_STR );
        assertFalse( table.has( "1", SIZE_STR ) );
        assertEquals( SIZE, table.count() );
        assertEquals( SIZE, table.count( "1" ) );
        assertEquals( "0", table.get( "1" ) );
        assertFalse( table.isKeyUsingBTree( "1" ) );
    
        for ( int i = SIZE - 1; i >= 0; i-- )
        {
            String istr = Integer.toString( i );
            table.remove( "1", istr );
            assertFalse( table.isKeyUsingBTree( "1" ) );
        }
        
        assertEquals( 0, table.count() );

        for ( int i = 0; i < SIZE - 1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( "1", istr );
            assertFalse( table.isKeyUsingBTree( "1" ) );
        }
        
        // this switches back to using B+Trees from AvlTree
        table.put( "1", SIZE_STR ) ;
        table.put( "1", SIZE_PLUS_ONE_STR );
        assertTrue( table.isKeyUsingBTree( "1" ) );
        
        assertEquals( SIZE + 1, table.count() );
        table.remove( "1" );
        assertEquals( 0, table.count() );
    }
    
    
    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValueAfterDuplicateLimit() throws Exception
    {
        testDuplicateLimit();
        assertEquals( 0, table.count() );
        
        try
        {
            table.put( "1", null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        try
        {
            table.put( null, "1" );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        assertEquals( 0, table.count() );
        assertEquals( null, table.get( "1" ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( "1" );
        table.put( "1", "1" );
        table.put( "1", "2" );
        assertEquals( 2, table.count( "1" ) );
        table.remove( "1", "1" );
        assertEquals( 1, table.count( "1" ) );
        assertEquals( "2", table.get( "1" ) );

        table.remove( "1", "2" );
        assertNull( table.get( "1" ) );
        assertEquals( 0, table.count( "1" ) );
        assertFalse( table.has( "1" ) );
    }


    @Test
    public void testMiscellaneous() throws Exception
    {
        assertNotNull( table.getMarshaller() );
        table.close();

        // test value btree creation without serializer
        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String,String>( schemaManager, "test", SIZE, recman,
                comparator, comparator, new DefaultSerializer(), null );
        assertNull( table.getValueSerializer() );
        
        for ( int i = 0; i < SIZE + 1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( "0", istr );
        }
        
        table.remove( "0" );
        assertFalse( table.has( "0" ) );
    }

    
    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValue() throws Exception
    {
        assertEquals( 0, table.count() );
        
        try
        {
            table.put( "1", null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        try
        {
            table.put( null, "2" );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
        
        assertEquals( 0, table.count() );
        assertEquals( null, table.get( "1" ) );
        
        // Let's add the key with two valid values and remove all values
        table.remove( "1" );
        table.put( "1", "1" );
        table.put( "1", "2" );
        assertEquals( 2, table.count( "1" ) );
        table.remove( "1", "1" );
        assertEquals( 1, table.count( "1" ) );
        assertEquals( "2", table.get( "1" ) );

        table.remove( "1", "2" );
        assertNull( table.get( "1" ) );
        assertEquals( 0, table.count( "1" ) );
        assertFalse( table.has( "1" ) );
    }
}
