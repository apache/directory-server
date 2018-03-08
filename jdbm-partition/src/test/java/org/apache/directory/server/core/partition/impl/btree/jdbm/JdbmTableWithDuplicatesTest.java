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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.MockPartitionReadTxn;
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
    private static final Logger LOG = LoggerFactory.getLogger( JdbmTableWithDuplicatesTest.class );
    private static final String TEST_OUTPUT_PATH = "test.output.path";
    private static final int SIZE = 15;
    private static final int SIZE2 = 30;

    private static final String SIZE_MINUS_ONE_STR = "14";
    private static final String SIZE_STR = "15";
    private static final String SIZE_PLUS_ONE_STR = "16";

    private static final String SIZE2_MINUS_ONE_STR = "29";
    private static final String SIZE2_STR = "30";
    private static final String SIZE2_PLUS_ONE_STR = "31";

    JdbmTable<String, String> table;
    File dbFile;
    RecordManager recman;
    private static SchemaManager schemaManager;
    private PartitionTxn partitionTxn;


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
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    @Before
    public void createTable() throws Exception
    {
        destroyTable();
        File tmpDir = null;

        if ( System.getProperty( TEST_OUTPUT_PATH, null ) != null )
        {
            tmpDir = new File( System.getProperty( TEST_OUTPUT_PATH ) );
        }

        dbFile = File.createTempFile( getClass().getSimpleName(), "db", tmpDir );
        recman = new BaseRecordManager( dbFile.getAbsolutePath() );

        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String, String>( schemaManager, "test", SIZE, recman,
            comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );
        LOG.debug( "Created new table and populated it with data" );
        
        partitionTxn = new MockPartitionReadTxn();
    }


    @After
    public void destroyTable() throws Exception
    {
        if ( table != null )
        {
            table.close( partitionTxn );
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
    public void testCountOneArg() throws Exception
    {
        assertEquals( 0, table.count( partitionTxn, "3" ) );
        assertEquals( 0, table.count( partitionTxn, null ) );
    }


    @Test(expected = IllegalArgumentException.class)
    public void testNullKeyComparator() throws Exception
    {
        assertNotNull( table.getKeyComparator() );

        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        new JdbmTable<String, String>( schemaManager, "test", SIZE, recman,
            null, comparator, null, new IntegerSerializer() );
    }


    @Test(expected = IllegalArgumentException.class)
    public void testNullValueComparator() throws Exception
    {
        assertNotNull( table.getValueComparator() );

        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        new JdbmTable<String, String>( schemaManager, "test", SIZE, recman,
            comparator, null, null, new IntegerSerializer() );
    }


    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( partitionTxn, "1", "2" );
        assertEquals( "2", table.get( partitionTxn, "1" ) );
        table.close( partitionTxn );
        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String, String>( schemaManager, "test", SIZE, recman,
            comparator, comparator, new DefaultSerializer(), new DefaultSerializer() );
        assertEquals( "2", table.get( partitionTxn, "1" ) );
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
        assertEquals( 0, table.count( partitionTxn ) );
        assertEquals( 0, table.count( partitionTxn, "1" ) );

        // Test get method
        assertNull( table.get( partitionTxn, "0" ) );
        assertNull( table.get( partitionTxn, null ) );

        // Test remove methods
        table.remove( partitionTxn, "1" );
        assertFalse( table.has( partitionTxn, "1" ) );

        // Test has operations
        assertFalse( table.has( partitionTxn, "1" ) );
        assertFalse( table.has( partitionTxn, "1", "0" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", "0" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1", "0" ) );
    }


    @Test
    public void testPut() throws Exception
    {
        final int SIZE = 15;

        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, istr, istr );
        }

        assertEquals( SIZE, table.count( partitionTxn ) );
        table.put( partitionTxn, "0", "0" );
        assertTrue( table.has( partitionTxn, "0", "0" ) );

        // add some duplicates
        for ( int i = 0; i < SIZE * 2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, SIZE2_STR, istr );
        }

        assertEquals( SIZE * 3, table.count( partitionTxn ) );

        table.put( partitionTxn, "0", "0" );
        assertTrue( table.has( partitionTxn, "0", "0" ) );

        table.put( partitionTxn, SIZE2_STR, "0" );
        assertTrue( table.has( partitionTxn, SIZE2_STR, "0" ) );
    }


    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( partitionTxn, "1" ) );

        for ( int i = 0; i < SIZE * 2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, "1", istr );
        }

        assertEquals( SIZE2, table.count( partitionTxn ) );

        assertTrue( table.has( partitionTxn, "1" ) );
        assertTrue( table.has( partitionTxn, "1", "0" ) );
        assertFalse( table.has( partitionTxn, "1", SIZE2_STR ) );

        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", "0" ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", "0" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1", "-1" ) );

        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertFalse( table.has( partitionTxn, "1", SIZE2_STR ) );

        // let's go over the this limit now and ask the same questions
        table.put( partitionTxn, "1", SIZE2_STR );

        assertTrue( table.has( partitionTxn, "1" ) );
        assertTrue( table.has( partitionTxn, "1", "0" ) );
        assertTrue( table.has( partitionTxn, "1", SIZE2_STR ) );
        assertFalse( table.has( partitionTxn, null, null ) );

        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", "0" ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", "0" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1", "-1" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, null, null ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, null, null ) );

        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "1", SIZE2_STR ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", SIZE2_PLUS_ONE_STR ) );
        assertFalse( table.has( partitionTxn, "1", SIZE2_PLUS_ONE_STR ) );

        // now do not add duplicates and check has( partitionTxn, key, boolean )
        for ( int i = 0; i < SIZE; i++ )
        {
            // note we are not adding duplicates not put( partitionTxn, 1, i )
            String istr = Integer.toString( i );
            table.put( partitionTxn, istr, istr );
        }

        assertFalse( table.has( partitionTxn, "-1" ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "-1" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "-1" ) );

        assertTrue( table.has( partitionTxn, "0" ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "0" ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "0" ) );

        assertTrue( table.has( partitionTxn, SIZE_MINUS_ONE_STR ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, SIZE_MINUS_ONE_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, SIZE_MINUS_ONE_STR ) );

        assertFalse( table.has( partitionTxn, SIZE_STR ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, SIZE_STR ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, SIZE_STR ) );

        for ( int i = 0; i < SIZE; i++ )
        {
            if ( i == 1 ) // don't delete the node which had multiple values
            {
                continue;
            }

            String istr = Integer.toString( i );
            table.remove( partitionTxn, istr, istr );
        }

        // delete all values of the duplicate key one by one
        for ( int i = 0; i < SIZE * 2 + 1; i++ )
        {
            String istr = Integer.toString( i );
            table.remove( partitionTxn, "1", istr );
        }

        Cursor<Tuple<String, String>> cursor = table.cursor();

        cursor.beforeFirst();

        while ( cursor.next() )
        {
            //System.out.println( cursor.get() );
        }

        cursor.close();

        assertFalse( table.hasLessOrEqual( partitionTxn, "1" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1", "10" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", "0" ) );

        table.put( partitionTxn, "1", "0" );

    }


    @Test
    public void testRemove() throws Exception
    {
        assertEquals( 0, table.count( partitionTxn ) );

        table.put( partitionTxn, "1", "1" );
        table.put( partitionTxn, "1", "2" );
        assertEquals( 2, table.count( partitionTxn ) );
        table.remove( partitionTxn, "1" );
        assertFalse( table.has( partitionTxn, "1" ) );
        assertEquals( 0, table.count( partitionTxn ) );

        table.put( partitionTxn, "10", "10" );
        assertEquals( 1, table.count( partitionTxn ) );
        table.remove( partitionTxn, "10", "11" );
        assertFalse( table.has( partitionTxn, "10", "11" ) );
        assertEquals( 1, table.count( partitionTxn ) );
        table.remove( partitionTxn, "10", "10" );
        assertFalse( table.has( partitionTxn, "10", "10" ) );
        assertEquals( 0, table.count( partitionTxn ) );

        // add duplicates
        for ( int i = 0; i < SIZE * 2; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, "0", istr );
        }

        assertEquals( SIZE * 2, table.count( partitionTxn ) );
        table.remove( partitionTxn, "0", "100" );
        assertFalse( table.has( partitionTxn, "0", "100" ) );
        assertEquals( SIZE * 2, table.count( partitionTxn ) );

        table.remove( partitionTxn, "0" );
        assertNull( table.get( partitionTxn, "0" ) );
    }


    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, istr, istr );
        }

        assertEquals( 15, table.count( partitionTxn ) );
        assertEquals( 1, table.count( partitionTxn, "0" ) );

        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */

        assertEquals( 10, table.lessThanCount( partitionTxn, "5" ) );
        assertEquals( 10, table.greaterThanCount( partitionTxn, "5" ) );
    }


    @Test
    public void testDuplicateLimit() throws Exception
    {
        assertFalse( table.isKeyUsingBTree( "1" ) );

        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, "1", istr );
        }
        assertEquals( SIZE, table.count( partitionTxn ) );
        assertEquals( SIZE, table.count( partitionTxn, "1" ) );
        assertFalse( table.isKeyUsingBTree( "1" ) );

        // this switches to B+Trees from AvlTree
        table.put( partitionTxn, "1", SIZE_STR );
        assertEquals( SIZE + 1, table.count( partitionTxn ) );
        assertEquals( SIZE + 1, table.count( partitionTxn, "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        // go one more over still a B+Tree
        table.put( partitionTxn, "1", SIZE_PLUS_ONE_STR );
        assertEquals( SIZE + 2, table.count( partitionTxn ) );
        assertEquals( SIZE + 2, table.count( partitionTxn, "1" ) );
        assertEquals( "0", table.get( partitionTxn, "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        // now start removing and see what happens 
        table.remove( partitionTxn, "1", SIZE_PLUS_ONE_STR );
        assertFalse( table.has( partitionTxn, "1", SIZE_PLUS_ONE_STR ) );
        assertTrue( table.has( partitionTxn, "1", SIZE_STR ) );
        assertEquals( SIZE + 1, table.count( partitionTxn ) );
        assertEquals( SIZE + 1, table.count( partitionTxn, "1" ) );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        // this switches to AvlTree from B+Trees
        table.remove( partitionTxn, "1", SIZE_STR );
        assertFalse( table.has( partitionTxn, "1", SIZE_STR ) );
        assertEquals( SIZE, table.count( partitionTxn ) );
        assertEquals( SIZE, table.count( partitionTxn, "1" ) );
        assertEquals( "0", table.get( partitionTxn, "1" ) );
        assertFalse( table.isKeyUsingBTree( "1" ) );

        for ( int i = SIZE - 1; i >= 0; i-- )
        {
            String istr = Integer.toString( i );
            table.remove( partitionTxn, "1", istr );
            assertFalse( table.isKeyUsingBTree( "1" ) );
        }

        assertEquals( 0, table.count( partitionTxn ) );

        for ( int i = 0; i < SIZE - 1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, "1", istr );
            assertFalse( table.isKeyUsingBTree( "1" ) );
        }

        // this switches back to using B+Trees from AvlTree
        table.put( partitionTxn, "1", SIZE_STR );
        table.put( partitionTxn, "1", SIZE_PLUS_ONE_STR );
        assertTrue( table.isKeyUsingBTree( "1" ) );

        assertEquals( SIZE + 1, table.count( partitionTxn ) );
        table.remove( partitionTxn, "1" );
        assertEquals( 0, table.count( partitionTxn ) );
    }


    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValueAfterDuplicateLimit() throws Exception
    {
        testDuplicateLimit();
        assertEquals( 0, table.count( partitionTxn ) );

        try
        {
            table.put( partitionTxn, "1", null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        try
        {
            table.put( partitionTxn, null, "1" );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        assertEquals( 0, table.count( partitionTxn ) );
        assertEquals( null, table.get( partitionTxn, "1" ) );

        // Let's add the key with two valid values and remove all values
        table.remove( partitionTxn, "1" );
        table.put( partitionTxn, "1", "1" );
        table.put( partitionTxn, "1", "2" );
        assertEquals( 2, table.count( partitionTxn, "1" ) );
        table.remove( partitionTxn, "1", "1" );
        assertEquals( 1, table.count( partitionTxn, "1" ) );
        assertEquals( "2", table.get( partitionTxn, "1" ) );

        table.remove( partitionTxn, "1", "2" );
        assertNull( table.get( partitionTxn, "1" ) );
        assertEquals( 0, table.count( partitionTxn, "1" ) );
        assertFalse( table.has( partitionTxn, "1" ) );
    }


    @Test
    public void testMiscellaneous() throws Exception
    {
        assertNotNull( table.getMarshaller() );
        table.close( partitionTxn );

        // test value btree creation without serializer
        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );

        table = new JdbmTable<String, String>( schemaManager, "test", SIZE, recman,
            comparator, comparator, new DefaultSerializer(), null );

        for ( int i = 0; i < SIZE + 1; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, "0", istr );
        }

        table.remove( partitionTxn, "0" );
        assertFalse( table.has( partitionTxn, "0" ) );
    }


    /**
     * Let's test keys with a null or lack of any values.
     * @throws Exception on error
     */
    @Test
    public void testNullOrEmptyKeyValue() throws Exception
    {
        assertEquals( 0, table.count( partitionTxn ) );

        try
        {
            table.put( partitionTxn, "1", null );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        try
        {
            table.put( partitionTxn, null, "2" );
            fail( "should never get here due to IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }

        assertEquals( 0, table.count( partitionTxn ) );
        assertEquals( null, table.get( partitionTxn, "1" ) );

        // Let's add the key with two valid values and remove all values
        table.remove( partitionTxn, "1" );
        table.put( partitionTxn, "1", "1" );
        table.put( partitionTxn, "1", "2" );
        assertEquals( 2, table.count( partitionTxn, "1" ) );
        table.remove( partitionTxn, "1", "1" );
        assertEquals( 1, table.count( partitionTxn, "1" ) );
        assertEquals( "2", table.get( partitionTxn, "1" ) );

        table.remove( partitionTxn, "1", "2" );
        assertNull( table.get( partitionTxn, "1" ) );
        assertEquals( 0, table.count( partitionTxn, "1" ) );
        assertFalse( table.has( partitionTxn, "1" ) );
    }
}
