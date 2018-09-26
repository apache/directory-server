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

package org.apache.directory.server.core.partition.impl.btree.je;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.StringSerializer;
import org.apache.directory.server.xdbm.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Database;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeTableNoDuplicatesTest
{
    private static final Logger LOG = LoggerFactory.getLogger( BdbJeTableNoDuplicatesTest.class );
    private static final String TEST_OUTPUT_PATH = "test.output.path";

    Table<String, String> table;
    private static SchemaManager schemaManager;
    private PartitionTxn partitionTxn;
    static BdbJePartitionEnviroment env;

    static File dbDir;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = BdbJeTableNoDuplicatesTest.class.getResource( "" ).getPath();
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

        dbDir = new File( workingDirectory + "/" + BdbJeTableNoDuplicatesTest.class.getSimpleName() );
        if ( dbDir.exists() )
        {
            FileUtils.deleteDirectory( dbDir );
        }

        dbDir.mkdirs();
        env = new BdbJePartitionEnviroment( schemaManager, dbDir );
    }


    @Before
    public void createTable() throws Exception
    {
        destroyTable();

        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        StringSerializer serializer = new StringSerializer();
        comparator.setSchemaManager( schemaManager );
        Database db = env.createDb( "test", false );
        table = new BdbJeTable<String, String>( db, schemaManager, comparator, serializer, serializer );
        LOG.debug( "Created new table and populated it with data" );

        renewTxn();
    }


    private void renewTxn()
    {
        if ( partitionTxn != null )
        {
            try
            {
                partitionTxn.commit();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        partitionTxn = new JeTransaction( env.createTxn() );
    }


    @After
    public void destroyTable() throws Exception
    {
        if ( table != null )
        {
            table.close( partitionTxn );
            env.deleteDb( table.getName() );
            table = null;
        }
    }


    @Test
    public void testCloseReopen() throws Exception
    {
        table.put( partitionTxn, "1", "2" );
        table.close( partitionTxn );
        SerializableComparator<String> comparator = new SerializableComparator<String>(
            SchemaConstants.INTEGER_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );
        Database db = env.createDb( "test", false );
        StringSerializer serializer = new StringSerializer();
        table = new BdbJeTable<String, String>( db, schemaManager, comparator, serializer, serializer );
        assertEquals( "2", table.get( partitionTxn, "1" ) );
    }


    @Test
    public void testConfigMethods() throws Exception
    {
        assertFalse( table.isDupsEnabled() );
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

        // Test remove methods
        table.remove( partitionTxn, "1" );
        assertNull( table.get( partitionTxn, "1" ) );

        // Test has operations
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1" ) );
        assertFalse( table.has( partitionTxn, "1", "0" ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, "1" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "1" ) );

        try
        {
            assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", "0" ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertFalse( table.hasLessOrEqual( partitionTxn, "1", "0" ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }
    }


    @Test
    public void testLoadData() throws Exception
    {
        // add some data to it
        for ( int i = 0; i < 10; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, istr, istr );
        }

        assertEquals( 10, table.count( partitionTxn ) );
        assertEquals( 1, table.count( partitionTxn, "0" ) );

        /*
         * If counts are exact then we can test for exact values.  Again this 
         * is not a critical function but one used for optimization so worst 
         * case guesses are allowed.
         */

        assertEquals( 10, table.lessThanCount( partitionTxn, "5" ) );
        assertEquals( 10, table.greaterThanCount( partitionTxn, "5" ) );
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

        // Let's add the key with a valid value and remove just the value
        assertEquals( 0, table.count( partitionTxn, "1" ) );
        table.remove( partitionTxn, "1" );
        assertEquals( 0, table.count( partitionTxn, "1" ) );
        table.put( partitionTxn, "1", "1" );
        assertEquals( 1, table.count( partitionTxn, "1" ) );
        table.remove( partitionTxn, "1", "1" );
        assertEquals( 0, table.count( partitionTxn, "1" ) );
        assertNull( table.get( partitionTxn, "1" ) );
        assertFalse( table.has( partitionTxn, "1" ) );
    }


    @Test
    public void testRemove() throws Exception
    {
        table.put( partitionTxn, "1", "1" );
        table.remove( partitionTxn, "1" );
        assertNull( table.get( partitionTxn, "1" ) );

        table.put( partitionTxn, "10", "10" );

        table.remove( partitionTxn, "10", "11" );
        assertFalse( table.has( partitionTxn, "10", "11" ) );

        //        assertNull( table.remove( null ) );
        //        assertNull( table.remove( null, null ) );
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
    }


    @Test
    public void testHas() throws Exception
    {
        assertFalse( table.has( partitionTxn, "1" ) );
        final int SIZE = 15;

        for ( int i = 0; i < SIZE; i++ )
        {
            String istr = Integer.toString( i );
            table.put( partitionTxn, istr, istr );
        }

        assertEquals( SIZE, table.count( partitionTxn ) );

        assertFalse( table.has( partitionTxn, "-1" ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "-1" ) );
        assertFalse( table.hasLessOrEqual( partitionTxn, "-1" ) );

        assertTrue( table.has( partitionTxn, "0" ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, "0" ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, "0" ) );

        assertTrue( table.has( partitionTxn, Integer.toString( SIZE - 1 ) ) );
        assertTrue( table.hasGreaterOrEqual( partitionTxn, Integer.toString( SIZE - 1 ) ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, Integer.toString( SIZE - 1 ) ) );

        assertFalse( table.has( partitionTxn, Integer.toString( SIZE ) ) );
        assertFalse( table.hasGreaterOrEqual( partitionTxn, Integer.toString( SIZE ) ) );
        assertTrue( table.hasLessOrEqual( partitionTxn, Integer.toString( SIZE ) ) );
        table.remove( partitionTxn, "10" );
        table.remove( partitionTxn, "11" );
        assertTrue( table.hasLessOrEqual( partitionTxn, "11" ) );

        try
        {
            assertFalse( table.hasGreaterOrEqual( partitionTxn, "1", "1" ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertFalse( table.hasLessOrEqual( partitionTxn, "1", "1" ) );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        try
        {
            assertTrue( table.hasLessOrEqual( partitionTxn, "1", "2" ) );
            fail( "Should never get here since no dups tables " +
                "freak when they cannot find a value comparator" );
        }
        catch ( UnsupportedOperationException e )
        {
            assertNotNull( e );
        }
    }
}
