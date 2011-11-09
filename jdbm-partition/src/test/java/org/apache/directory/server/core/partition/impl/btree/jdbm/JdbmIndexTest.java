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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the JdbmIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmIndexTest
{
    private static File dbFileDir;
    Index<String> idx;
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = JdbmIndexTest.class.getResource( "" ).getPath();
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
    public void setup() throws IOException
    {

        File tmpIndexFile = File.createTempFile( JdbmIndexTest.class.getSimpleName(), "db" );
        tmpIndexFile.deleteOnExit();
        dbFileDir = new File( tmpIndexFile.getParentFile(), JdbmIndexTest.class.getSimpleName() );

        dbFileDir.mkdirs();
    }


    @After
    public void teardown() throws Exception
    {
        destroyIndex();

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

            // created by this test
            File dbFile = new File( idx.getWkDirPath().getPath(), idx.getAttribute().getOid() + ".db" );
            assertTrue( dbFile.delete() );

            // created by TransactionManager, if transactions are not disabled
            File logFile = new File( idx.getWkDirPath().getPath(), idx.getAttribute().getOid() + ".lg" );
            
            if ( logFile.exists() )
            {
                assertTrue( logFile.delete() );
            }
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        JdbmIndex<String> index = new JdbmIndex<String>();
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
    }


    void initIndex( JdbmIndex<String> jdbmIdx ) throws Exception
    {
        if ( jdbmIdx == null )
        {
            jdbmIdx = new JdbmIndex<String>();
        }
        
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );

        jdbmIdx.init( schemaManager, attributeType );
        this.idx = jdbmIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testAttributeId() throws Exception
    {
        // uninitialized index
        JdbmIndex<Object> jdbmIndex1 = new JdbmIndex<Object>();
        jdbmIndex1.setAttributeId( "foo" );
        assertEquals( "foo", jdbmIndex1.getAttributeId() );

        JdbmIndex<Object> jdbmIndex2 = new JdbmIndex<Object>( "bar" );
        assertEquals( "bar", jdbmIndex2.getAttributeId() );

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
        JdbmIndex<String> index = new JdbmIndex<String>( "foo" );
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
        assertEquals( "foo", idx.getAttributeId() );
    }


    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        JdbmIndex<Object> jdbmIndex = new JdbmIndex<Object>();
        jdbmIndex.setCacheSize( 337 );
        assertEquals( 337, jdbmIndex.getCacheSize() );

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
        JdbmIndex<String> jdbmIndex = new JdbmIndex<String>();
        jdbmIndex.setWkDirPath( wkdir.toURI() );
        assertEquals( "foo", new File( jdbmIndex.getWkDirPath() ).getName() );

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
        jdbmIndex = new JdbmIndex<String>();
        wkdir.mkdirs();
        jdbmIndex.setWkDirPath( wkdir.toURI() );
        initIndex( jdbmIndex );
        assertEquals( wkdir.toURI(), idx.getWkDirPath() );
    }


    @Test
    public void testNumDupLimit() throws Exception
    {
        // uninitialized index
        JdbmIndex<Object> jdbmIndex = new JdbmIndex<Object>();
        jdbmIndex.setNumDupLimit( 337 );
        assertEquals( 337, jdbmIndex.getNumDupLimit() );

        // initialized index
        initIndex();
        try
        {
            ( ( JdbmIndex<String> ) idx ).setNumDupLimit( 30 );
            fail( "Should not be able to set numDupLimit after initialization." );
        }
        catch ( Exception e )
        {
        }
        assertEquals( JdbmIndex.DEFAULT_DUPLICATE_LIMIT, ( ( JdbmIndex<String> ) idx ).getNumDupLimit() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        JdbmIndex<Object> jdbmIndex = new JdbmIndex<Object>();
        assertNull( jdbmIndex.getAttribute() );

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

        idx.add( "foo", getUUIDString( 1234 ) );
        assertEquals( 1, idx.count() );

        idx.add( "foo", getUUIDString( 333 ) );
        assertEquals( 2, idx.count() );

        idx.add( "bar", getUUIDString( 555 ) );
        assertEquals( 3, idx.count() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "bar", getUUIDString( 1234 ) );
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "foo", getUUIDString( 1234 ) );
        assertEquals( 1, idx.count( "foo" ) );

        idx.add( "foo", getUUIDString( 333 ) );
        assertEquals( 2, idx.count( "foo" ) );
    }


    @Test
    public void testGreaterThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.greaterThanCount( "a" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( String.valueOf( ch ), getUUIDString( ( int ) ch ) );
        }
        assertEquals( 26, idx.greaterThanCount( "a" ) );
    }


    @Test
    public void testLessThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.lessThanCount( "z" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( String.valueOf( ch ), getUUIDString( ( int ) ch ) );
        }
        assertEquals( 26, idx.lessThanCount( "z" ) );
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
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( -24 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( 24 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( 24 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( -24 ) ) );

        idx.add( "foo", getUUIDString( 1 ) );
        assertEquals( getUUIDString( 1 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );
        assertTrue( idx.forward( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 2 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( 0 ) ) );

        idx.add( "foo", getUUIDString( 2 ) );
        assertEquals( getUUIDString( 1 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 2 ) ) );
        assertTrue( idx.forward( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forward( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( 3 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 3 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( 0 ) ) );

        idx.add( "bar", getUUIDString( 1 ) );
        assertEquals( getUUIDString( 1 ), idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( getUUIDString( 1 ) ) ); // reverse lookup returns first val
        assertTrue( idx.forward( "bar", getUUIDString( 1 ) ) );
        assertTrue( idx.forward( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forward( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "bar", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", getUUIDString( 3 ) ) );
        assertFalse( idx.forwardGreaterOrEq( "bar", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardLessOrEq( "bar", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 1 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 2 ) ) );
        assertTrue( idx.forwardLessOrEq( "foo", getUUIDString( 3 ) ) );
        assertFalse( idx.forwardLessOrEq( "foo", getUUIDString( 0 ) ) );
        assertFalse( idx.forwardLessOrEq( "bar", getUUIDString( 0 ) ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
        assertNull( idx.reverseLookup( getUUIDString( 1 ) ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 0 ) ) );

        idx.drop( getUUIDString( 0 ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );

        // test add/drop with duplicates in bulk
        idx.add( "foo", getUUIDString( 0 ) );
        idx.add( "foo", getUUIDString( 1 ) );
        idx.add( "bar", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "foo" ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( getUUIDString( 0 ) ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );

        idx.drop( getUUIDString( 0 ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );
        assertFalse( idx.forward( "bar", getUUIDString( 0 ) ) );
        assertFalse( idx.forward( "foo", getUUIDString( 0 ) ) );

        idx.drop( getUUIDString( 1 ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
        assertNull( idx.reverseLookup( getUUIDString( 1 ) ) );
        assertEquals( 0, idx.count() );
    }


    @Test
    public void testAddDropOneByOne() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
        assertNull( idx.reverseLookup( getUUIDString( 1 ) ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 0 ) ) );

        idx.drop( "foo", getUUIDString( 0 ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );

        // test add/drop with duplicates but one at a time
        idx.add( "foo", getUUIDString( 0 ) );
        idx.add( "foo", getUUIDString( 1 ) );
        idx.add( "bar", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "foo" ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( getUUIDString( 0 ) ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );

        idx.drop( "bar", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 0 ) ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );
        assertFalse( idx.forward( "bar", getUUIDString( 0 ) ) );

        idx.drop( "foo", getUUIDString( 0 ) );
        assertEquals( getUUIDString( 1 ), idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( getUUIDString( 1 ) ) );
        assertFalse( idx.forward( "foo", getUUIDString( 0 ) ) );

        idx.drop( "foo", getUUIDString( 1 ) );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
        assertNull( idx.reverseLookup( getUUIDString( 1 ) ) );
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

        idx.add( "foo", getUUIDString( 1234 ) );
        assertEquals( 1, idx.count() );

        idx.add( "foo", getUUIDString( 333 ) );
        assertEquals( 2, idx.count() );

        idx.add( "bar", getUUIDString( 555 ) );
        assertEquals( 3, idx.count() );

        // use forward index's cursor
        Cursor<IndexEntry<String>> cursor = idx.forwardCursor();
        cursor.beforeFirst();

        cursor.next();
        IndexEntry<String> e1 = cursor.get();
        assertEquals( getUUIDString( 555 ), e1.getId() );
        assertEquals( "bar", e1.getValue() );

        cursor.next();
        IndexEntry<String> e2 = cursor.get();
        assertEquals( getUUIDString( 333 ), e2.getId() );
        assertEquals( "foo", e2.getValue() );

        cursor.next();
        IndexEntry<String> e3 = cursor.get();
        assertEquals( getUUIDString( 1234 ), e3.getId() );
        assertEquals( "foo", e3.getValue() );

        // use reverse index's cursor
        cursor = idx.reverseCursor();
        cursor.beforeFirst();

        cursor.next();
        e1 = cursor.get();
        assertEquals( getUUIDString( 333 ), e1.getId() );
        assertEquals( "foo", e1.getValue() );

        cursor.next();
        e2 = cursor.get();
        assertEquals( getUUIDString( 555 ), e2.getId() );
        assertEquals( "bar", e2.getValue() );

        cursor.next();
        e3 = cursor.get();
        assertEquals( getUUIDString( 1234 ), e3.getId() );
        assertEquals( "foo", e3.getValue() );
    }


    @Test
    public void testNoEqualityMatching() throws Exception
    {
        JdbmIndex<Object> jdbmIndex = new JdbmIndex<Object>();

        try
        {
            AttributeType noEqMatchAttribute = new AttributeType( "1.1" );
            jdbmIndex.setWkDirPath( dbFileDir.toURI() );
            jdbmIndex.init( schemaManager, noEqMatchAttribute );
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
        JdbmIndex<Object> jdbmIndex = new JdbmIndex<Object>();
        jdbmIndex.setWkDirPath( dbFileDir.toURI() );
        jdbmIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT ) );
        jdbmIndex.close();
    }
    
    public static UUID getUUIDString( int idx )
    {
        /** UUID string */
        UUID baseUUID = UUID.fromString( "00000000-0000-0000-0000-000000000000" );
        
        long low = baseUUID.getLeastSignificantBits();
        long high = baseUUID.getMostSignificantBits();
        low = low + idx;
        
        return new UUID( high, low );
    }
}
