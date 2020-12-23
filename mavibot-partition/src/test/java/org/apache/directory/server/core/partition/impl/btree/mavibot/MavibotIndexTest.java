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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.MockPartitionReadTxn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;


/**
 * Tests the MavibotIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class MavibotIndexTest
{
    private static File dbFileDir;
    Index<String, String> idx;
    private static SchemaManager schemaManager;

    private RecordManager recordMan;

    private static final String UUID_0 = Strings.getUUID( 0L );
    private static final String UUID_1 = Strings.getUUID( 1L );
    private static final String UUID_1234 = Strings.getUUID( 1234L );
    private static final String UUID_333 = Strings.getUUID( 333L );
    private static final String UUID_555 = Strings.getUUID( 555L );
    
    private PartitionTxn partitionTxn;

    @TempDir
    public Path tempFolder;


    @BeforeAll
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = MavibotIndexTest.class.getResource( "" ).getPath();
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


    @BeforeEach
    public void setup() throws IOException
    {
        dbFileDir = Files.createDirectory( tempFolder.resolve( MavibotIndexTest.class.getSimpleName() ) ).toFile();

        recordMan = new RecordManager( dbFileDir.getAbsolutePath() );
        
        partitionTxn = new MockPartitionReadTxn();
    }


    @AfterEach
    public void teardown() throws Exception
    {
        destroyIndex();

        recordMan.close();

        if ( ( dbFileDir != null ) && dbFileDir.exists() )
        {
            FileUtils.deleteDirectory( dbFileDir );
        }
    }


    void destroyIndex() throws Exception
    {
        if ( idx != null )
        {
            idx.close( partitionTxn );
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );
        MavibotIndex<String> index = new MavibotIndex<String>( attributeType.getName(), false );
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
    }


    void initIndex( MavibotIndex<String> mavibotIdx ) throws Exception
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );

        if ( mavibotIdx == null )
        {
            mavibotIdx = new MavibotIndex<String>( attributeType.getName(), false );
        }

        mavibotIdx.setRecordManager( recordMan );
        mavibotIdx.init( schemaManager, attributeType );
        this.idx = mavibotIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testAttributeId() throws Exception
    {
        // uninitialized index
        MavibotIndex<Object> MavibotIndex1 = new MavibotIndex<Object>( "foo", false );
        assertEquals( "foo", MavibotIndex1.getAttributeId() );

        MavibotIndex<Object> MavibotIndex2 = new MavibotIndex<Object>( "bar", false );
        assertEquals( "bar", MavibotIndex2.getAttributeId() );

        // initialized index
        initIndex();

        try
        {
            idx.setAttributeId( "foo" );
            fail( "Should not be able to set attributeId after initialization." );
        }
        catch ( Exception e )
        {
        }

        assertEquals( "ou", idx.getAttributeId() );

        destroyIndex();
        MavibotIndex<String> index = new MavibotIndex<String>( "foo", false );
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
        assertEquals( "foo", idx.getAttributeId() );
    }


    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        MavibotIndex<Object> MavibotIndex = new MavibotIndex<Object>( "ou", false );
        MavibotIndex.setCacheSize( 337 );
        assertEquals( 337, MavibotIndex.getCacheSize() );

        // initialized index
        initIndex();

        try
        {
            idx.setCacheSize( 30 );
            fail( "Should not be able to set cacheSize after initialization." );
        }
        catch ( Exception e )
        {
        }
        assertEquals( Index.DEFAULT_INDEX_CACHE_SIZE, idx.getCacheSize() );
    }


    @Test
    public void testWkDirPath() throws Exception
    {
        File wkdir = new File( dbFileDir, "foo" );

        // uninitialized index
        MavibotIndex<String> MavibotIndex = new MavibotIndex<String>( "foo", false );
        MavibotIndex.setWkDirPath( wkdir.toURI() );
        assertEquals( "foo", new File( MavibotIndex.getWkDirPath() ).getName() );

        // initialized index
        initIndex();

        try
        {
            idx.setWkDirPath( wkdir.toURI() );
            fail( "Should not be able to set wkDirPath after initialization." );
        }
        catch ( Exception e )
        {
        }

        assertEquals( dbFileDir.toURI(), idx.getWkDirPath() );

        destroyIndex();
        MavibotIndex = new MavibotIndex<String>( "ou", false );
        wkdir.mkdirs();
        MavibotIndex.setWkDirPath( wkdir.toURI() );
        initIndex( MavibotIndex );
        assertEquals( wkdir.toURI(), idx.getWkDirPath() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        MavibotIndex<Object> MavibotIndex = new MavibotIndex<Object>( "ou", false );
        assertNull( MavibotIndex.getAttribute() );

        initIndex();
        assertEquals( schemaManager.lookupAttributeTypeRegistry( "ou" ), idx.getAttribute() );
    }


    // -----------------------------------------------------------------------
    // Count Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "foo", UUID_1234 );
        assertEquals( 1, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "foo", UUID_333 );
        assertEquals( 2, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "bar", UUID_555 );
        assertEquals( 3, idx.count( partitionTxn ) );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( partitionTxn, "foo" ) );

        idx.add( partitionTxn, "bar", UUID_1234 );
        assertEquals( 0, idx.count( partitionTxn, "foo" ) );

        idx.add( partitionTxn, "foo", UUID_1234 );
        assertEquals( 1, idx.count( partitionTxn, "foo" ) );

        idx.add( partitionTxn, "foo", UUID_333 );
        assertEquals( 2, idx.count( partitionTxn, "foo" ) );
    }


    @Test
    public void testGreaterThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.greaterThanCount( partitionTxn, "a" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( partitionTxn, String.valueOf( ch ), Strings.getUUID( ch ) );
        }

        // We should not go above the magic limit of 10
        assertEquals( 10, idx.greaterThanCount(partitionTxn,  "a" ) );
    }


    @Test
    public void testLessThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.lessThanCount( partitionTxn, "z" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( partitionTxn, String.valueOf( ch ), Strings.getUUID( ch ) );
        }

        // We should not go above the magic limit of 10
        assertEquals( 10, idx.lessThanCount( partitionTxn, "z" ) );
    }


    // -----------------------------------------------------------------------
    // Add, Drop and Lookup Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testLookups() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );
        assertNull( idx.forwardLookup( partitionTxn, "bar" ) );

        idx.add( partitionTxn, "foo", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );
        assertTrue( idx.forward( partitionTxn, "foo", UUID_0 ) );

        idx.add( partitionTxn, "foo", UUID_1 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );
        assertTrue( idx.forward( partitionTxn, "foo", UUID_0 ) );
        assertTrue( idx.forward( partitionTxn, "foo", UUID_1 ) );

        idx.add( partitionTxn, "bar", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "bar" ) );
        assertTrue( idx.forward( partitionTxn, "bar", UUID_0 ) );
        assertTrue( idx.forward( partitionTxn, "foo", UUID_0 ) );
        assertTrue( idx.forward( partitionTxn, "foo", UUID_1 ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );
        assertNull( idx.forwardLookup( partitionTxn, "bar" ) );

        // test add/drop without adding any duplicates
        idx.add( partitionTxn, "foo", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );

        idx.drop( partitionTxn, "foo", UUID_0 );
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );

        // test add/drop with duplicates in bulk
        idx.add( partitionTxn, "foo", UUID_0 );
        idx.add( partitionTxn, "foo", UUID_1 );
        idx.add( partitionTxn, "bar", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "bar" ) );

        idx.drop( partitionTxn, "foo", UUID_0 );
        idx.drop( partitionTxn, "bar", UUID_0 );
        assertFalse( idx.forward( partitionTxn, "bar", UUID_0 ) );
        assertFalse( idx.forward( partitionTxn, "foo", UUID_0 ) );

        idx.drop( partitionTxn, "bar", UUID_1 );
        idx.drop( partitionTxn, "foo", UUID_1 );
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );
        assertNull( idx.forwardLookup( partitionTxn, "bar" ) );
        assertEquals( 0, idx.count( partitionTxn ) );
    }


    @Test
    public void testAddDropOneByOne() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );
        assertNull( idx.forwardLookup( partitionTxn, "bar" ) );

        // test add/drop without adding any duplicates
        idx.add( partitionTxn, "foo", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );

        idx.drop( partitionTxn, "foo", UUID_0 );
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );

        // test add/drop with duplicates but one at a time
        idx.add( partitionTxn, "foo", UUID_0 );
        idx.add( partitionTxn, "foo", UUID_1 );
        idx.add( partitionTxn, "bar", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "bar" ) );

        idx.drop( partitionTxn, "bar", UUID_0 );
        assertEquals( UUID_0, idx.forwardLookup( partitionTxn, "foo" ) );
        assertFalse( idx.forward( partitionTxn, "bar", UUID_0 ) );

        idx.drop( partitionTxn, "foo", UUID_0 );
        assertEquals( UUID_1, idx.forwardLookup( partitionTxn, "foo" ) );
        assertFalse( idx.forward( partitionTxn, "foo", UUID_0 ) );

        idx.drop( partitionTxn, "foo", UUID_1 );
        assertNull( idx.forwardLookup( partitionTxn, "foo" ) );
        assertNull( idx.forwardLookup( partitionTxn, "bar" ) );
        assertEquals( 0, idx.count( partitionTxn ) );
    }


    // -----------------------------------------------------------------------
    // Miscellaneous Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCursors() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "foo", UUID_1234 );
        assertEquals( 1, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "foo", UUID_333 );
        assertEquals( 2, idx.count( partitionTxn ) );

        idx.add( partitionTxn, "bar", UUID_555 );
        assertEquals( 3, idx.count( partitionTxn ) );

        // use forward index's cursor
        Cursor<IndexEntry<String, String>> cursor = idx.forwardCursor( partitionTxn );
        cursor.beforeFirst();

        assertEquals( 3, idx.count( partitionTxn ) );

        cursor.next();
        IndexEntry<String, String> e1 = cursor.get();
        assertEquals( UUID_555, e1.getId() );
        assertEquals( "bar", e1.getKey() );

        cursor.next();
        IndexEntry<String, String> e2 = cursor.get();
        assertEquals( UUID_333, e2.getId() );
        //assertEquals( UUID_1234, e3.getId() );
        assertEquals( "foo", e2.getKey() );

        cursor.next();
        IndexEntry<String, String> e3 = cursor.get();
        assertEquals( UUID_1234, e3.getId() );
        assertEquals( "foo", e3.getKey() );

        cursor.close();
    }


    @Test
    public void testNoEqualityMatching() throws Exception
    {
        MavibotIndex<Object> MavibotIndex = new MavibotIndex<Object>( "1.1", false );

        try
        {
            AttributeType noEqMatchAttribute = new AttributeType( "1.1" );
            MavibotIndex.setWkDirPath( dbFileDir.toURI() );
            MavibotIndex.setRecordManager( recordMan );
            MavibotIndex.init( schemaManager, noEqMatchAttribute );
            fail( "should not get here" );
        }
        catch ( IOException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Failing Tests
    // -----------------------------------------------------------------------

    @Test
    public void testSingleValuedAttribute() throws Exception
    {
        MavibotIndex<Object> MavibotIndex = new MavibotIndex<Object>( SchemaConstants.CREATORS_NAME_AT,
            false );
        MavibotIndex.setWkDirPath( dbFileDir.toURI() );
        MavibotIndex.setRecordManager( recordMan );
        MavibotIndex
            .init( schemaManager, schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT ) );
        MavibotIndex.close( partitionTxn );
    }
}
