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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.mavibot.btree.RecordManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Tests the MavibotIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotIndexTest
{
    private static File dbFileDir;
    Index<String, Entry, String> idx;
    private static SchemaManager schemaManager;

    private RecordManager recordMan;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @BeforeClass
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


    @Before
    public void setup() throws IOException
    {
        dbFileDir = tempFolder.newFolder( MavibotIndexTest.class.getSimpleName() );

        recordMan = new RecordManager( dbFileDir.getAbsolutePath() );
    }


    @After
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
            idx.sync();
            idx.close();
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );
        MavibotIndex<String, Entry> index = new MavibotIndex<String, Entry>( attributeType.getName(), false );
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
    }


    void initIndex( MavibotIndex<String, Entry> mavibotIdx ) throws Exception
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );

        if ( mavibotIdx == null )
        {
            mavibotIdx = new MavibotIndex<String, Entry>( attributeType.getName(), false );
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
        MavibotIndex<Object, Object> MavibotIndex1 = new MavibotIndex<Object, Object>( "foo", false );
        assertEquals( "foo", MavibotIndex1.getAttributeId() );

        MavibotIndex<Object, Object> MavibotIndex2 = new MavibotIndex<Object, Object>( "bar", false );
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
        MavibotIndex<String, Entry> index = new MavibotIndex<String, Entry>( "foo", false );
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
        assertEquals( "foo", idx.getAttributeId() );
    }


    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        MavibotIndex<Object, Object> MavibotIndex = new MavibotIndex<Object, Object>( "ou", false );
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
        MavibotIndex<String, Entry> MavibotIndex = new MavibotIndex<String, Entry>( "foo", false );
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
        MavibotIndex = new MavibotIndex<String, Entry>( "ou", false );
        wkdir.mkdirs();
        MavibotIndex.setWkDirPath( wkdir.toURI() );
        initIndex( MavibotIndex );
        assertEquals( wkdir.toURI(), idx.getWkDirPath() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        MavibotIndex<Object, Object> MavibotIndex = new MavibotIndex<Object, Object>( "ou", false );
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
        assertEquals( 0, idx.count() );

        idx.add( "foo", Strings.getUUID( 1234L ) );
        assertEquals( 1, idx.count() );

        idx.add( "foo", Strings.getUUID( 333L ) );
        assertEquals( 2, idx.count() );

        idx.add( "bar", Strings.getUUID( 555L ) );
        assertEquals( 3, idx.count() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "bar", Strings.getUUID( 1234L ) );
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "foo", Strings.getUUID( 1234L ) );
        assertEquals( 1, idx.count( "foo" ) );

        idx.add( "foo", Strings.getUUID( 333L ) );
        assertEquals( 2, idx.count( "foo" ) );
    }


    @Test
    public void testGreaterThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.greaterThanCount( "a" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( String.valueOf( ch ), Strings.getUUID( ch ) );
        }

        // We should not go above the magic limit of 10
        assertEquals( 10, idx.greaterThanCount( "a" ) );
    }


    @Test
    public void testLessThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.lessThanCount( "z" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( String.valueOf( ch ), Strings.getUUID( ch ) );
        }

        // We should not go above the magic limit of 10
        assertEquals( 10, idx.lessThanCount( "z" ) );
    }


    // -----------------------------------------------------------------------
    // Add, Drop and Lookup Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testLookups() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( -24L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 24L ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", Strings.getUUID( 24L ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", Strings.getUUID( -24L ) ) );

        idx.add( "foo", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );
        assertTrue( idx.forward( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( -1L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( -1L ) ) );

        idx.add( "foo", Strings.getUUID( 1L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );
        assertTrue( idx.forward( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forward( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( -1L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 2L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 2L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( -1L ) ) );

        idx.add( "bar", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "bar" ) );
        assertTrue( idx.forward( "bar", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forward( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forward( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "bar", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( -1L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", Strings.getUUID( 2L ) ) );
        assertFalse( idx.forwardGreaterOrEq( "bar", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardLessOrEq( "bar", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 0L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 1L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( 2L ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", Strings.getUUID( -1L ) ) );
        assertTrue( idx.forwardLessOrEq( "bar", Strings.getUUID( -1L ) ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );

        idx.drop( "foo", Strings.getUUID( 0L ) );
        assertNull( idx.forwardLookup( "foo" ) );

        // test add/drop with duplicates in bulk
        idx.add( "foo", Strings.getUUID( 0L ) );
        idx.add( "foo", Strings.getUUID( 1L ) );
        idx.add( "bar", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "bar" ) );

        idx.drop( "foo", Strings.getUUID( 0L ) );
        idx.drop( "bar", Strings.getUUID( 0L ) );
        assertFalse( idx.forward( "bar", Strings.getUUID( 0L ) ) );
        assertFalse( idx.forward( "foo", Strings.getUUID( 0L ) ) );

        idx.drop( "bar", Strings.getUUID( 1L ) );
        idx.drop( "foo", Strings.getUUID( 1L ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertEquals( 0, idx.count() );
    }


    @Test
    public void testAddDropOneByOne() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );

        idx.drop( "foo", Strings.getUUID( 0L ) );
        assertNull( idx.forwardLookup( "foo" ) );

        // test add/drop with duplicates but one at a time
        idx.add( "foo", Strings.getUUID( 0L ) );
        idx.add( "foo", Strings.getUUID( 1L ) );
        idx.add( "bar", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "bar" ) );

        idx.drop( "bar", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( "foo" ) );
        assertFalse( idx.forward( "bar", Strings.getUUID( 0L ) ) );

        idx.drop( "foo", Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 1L ), idx.forwardLookup( "foo" ) );
        assertFalse( idx.forward( "foo", Strings.getUUID( 0L ) ) );

        idx.drop( "foo", Strings.getUUID( 1L ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertEquals( 0, idx.count() );
    }


    // -----------------------------------------------------------------------
    // Miscellaneous Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCursors() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count() );

        idx.add( "foo", Strings.getUUID( 1234L ) );
        assertEquals( 1, idx.count() );

        idx.add( "foo", Strings.getUUID( 333L ) );
        assertEquals( 2, idx.count() );

        idx.add( "bar", Strings.getUUID( 555L ) );
        assertEquals( 3, idx.count() );

        // use forward index's cursor
        Cursor<IndexEntry<String, String>> cursor = idx.forwardCursor();
        cursor.beforeFirst();

        assertEquals( 3, idx.count() );

        cursor.next();
        IndexEntry<String, String> e1 = cursor.get();
        assertEquals( Strings.getUUID( 555L ), e1.getId() );
        assertEquals( "bar", e1.getKey() );

        cursor.next();
        IndexEntry<String, String> e2 = cursor.get();
        assertEquals( Strings.getUUID( 333L ), e2.getId() );
        assertEquals( "foo", e2.getKey() );

        cursor.next();
        IndexEntry<String, String> e3 = cursor.get();
        assertEquals( Strings.getUUID( 1234L ), e3.getId() );
        assertEquals( "foo", e3.getKey() );

        cursor.close();
    }


    @Test
    public void testNoEqualityMatching() throws Exception
    {
        MavibotIndex<Object, Object> MavibotIndex = new MavibotIndex<Object, Object>( "1.1", false );

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
        MavibotIndex<Object, Object> MavibotIndex = new MavibotIndex<Object, Object>( SchemaConstants.CREATORS_NAME_AT, false );
        MavibotIndex.setWkDirPath( dbFileDir.toURI() );
        MavibotIndex.setRecordManager( recordMan );
        MavibotIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT ) );
        MavibotIndex.close();
    }
}
