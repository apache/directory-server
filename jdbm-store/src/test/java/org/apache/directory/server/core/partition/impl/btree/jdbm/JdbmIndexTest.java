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

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the JdbmIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmIndexTest
{
    private static File dbFileDir;
    Index<String, ServerEntry, Long> idx;
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
            fail( "Schema load failed : " + ExceptionUtils.printErrors( schemaManager.getErrors() ) );
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
            File dbFile = new File( idx.getWkDirPath(), idx.getAttribute().getName() + ".db" );
            assert dbFile.delete();

            // created by TransactionManager
            File logFile = new File( idx.getWkDirPath(), idx.getAttribute().getName() + ".lg" );
            assert logFile.delete();
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        initIndex( new JdbmIndex<String, ServerEntry>() );
    }


    void initIndex( JdbmIndex<String, ServerEntry> jdbmIdx ) throws Exception
    {
        if ( jdbmIdx == null )
        {
            jdbmIdx = new JdbmIndex<String, ServerEntry>();
        }

        jdbmIdx.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT ), dbFileDir );
        this.idx = jdbmIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testAttributeId() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex1 = new JdbmIndex();
        jdbmIndex1.setAttributeId( "foo" );
        assertEquals( "foo", jdbmIndex1.getAttributeId() );

        JdbmIndex jdbmIndex2 = new JdbmIndex( "bar" );
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
        initIndex( new JdbmIndex<String, ServerEntry>( "foo" ) );
        assertEquals( "foo", idx.getAttributeId() );
    }


    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex = new JdbmIndex();
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
        // uninitialized index
        JdbmIndex<String, ServerEntry> jdbmIndex = new JdbmIndex<String, ServerEntry>();
        jdbmIndex.setWkDirPath( new File( dbFileDir, "foo" ) );
        assertEquals( "foo", jdbmIndex.getWkDirPath().getName() );

        // initialized index
        initIndex();
        try
        {
            idx.setWkDirPath( new File( dbFileDir, "foo" ) );
            fail( "Should not be able to set wkDirPath after initialization." );
        }
        catch ( Exception e )
        {
        }
        assertEquals( dbFileDir, idx.getWkDirPath() );

        destroyIndex();
        jdbmIndex = new JdbmIndex<String, ServerEntry>();
        File wkdir = new File( dbFileDir, "foo" );
        wkdir.mkdirs();
        jdbmIndex.setWkDirPath( wkdir );
        initIndex( jdbmIndex );
        assertEquals( wkdir, idx.getWkDirPath() );
    }


    @Test
    public void testNumDupLimit() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex = new JdbmIndex();
        jdbmIndex.setNumDupLimit( 337 );
        assertEquals( 337, jdbmIndex.getNumDupLimit() );

        // initialized index
        initIndex();
        try
        {
            ( ( JdbmIndex ) idx ).setNumDupLimit( 30 );
            fail( "Should not be able to set numDupLimit after initialization." );
        }
        catch ( Exception e )
        {
        }
        assertEquals( JdbmIndex.DEFAULT_DUPLICATE_LIMIT, ( ( JdbmIndex ) idx ).getNumDupLimit() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex = new JdbmIndex();
        assertNull( jdbmIndex.getAttribute() );

        initIndex();
        assertEquals( schemaManager.lookupAttributeTypeRegistry( "ou" ), idx.getAttribute() );
    }


    @Test
    public void testIsCountExact() throws Exception
    {
        assertFalse( new JdbmIndex().isCountExact() );
    }


    // -----------------------------------------------------------------------
    // Count Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count() );

        idx.add( "foo", 1234L );
        assertEquals( 1, idx.count() );

        idx.add( "foo", 333L );
        assertEquals( 2, idx.count() );

        idx.add( "bar", 555L );
        assertEquals( 3, idx.count() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "bar", 1234L );
        assertEquals( 0, idx.count( "foo" ) );

        idx.add( "foo", 1234L );
        assertEquals( 1, idx.count( "foo" ) );

        idx.add( "foo", 333L );
        assertEquals( 2, idx.count( "foo" ) );
    }


    @Test
    public void testGreaterThanCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.greaterThanCount( "a" ) );

        for ( char ch = 'a'; ch <= 'z'; ch++ )
        {
            idx.add( String.valueOf( ch ), ( long ) ch );
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
            idx.add( String.valueOf( ch ), ( long ) ch );
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
        assertNull( idx.reverseLookup( 0L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", 0L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", -24L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", 24L ) );
        assertFalse( idx.forwardLessOrEq( "foo", 0L ) );
        assertFalse( idx.forwardLessOrEq( "foo", 24L ) );
        assertFalse( idx.forwardLessOrEq( "foo", -24L ) );

        idx.add( "foo", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );
        assertTrue( idx.forward( "foo", 0L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", 0L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", -1L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", 1L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 0L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 1L ) );
        assertFalse( idx.forwardLessOrEq( "foo", -1L ) );

        idx.add( "foo", 1L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertTrue( idx.forward( "foo", 0L ) );
        assertTrue( idx.forward( "foo", 1L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", 0L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", 1L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", -1L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", 2L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 0L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 1L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 2L ) );
        assertFalse( idx.forwardLessOrEq( "foo", -1L ) );

        idx.add( "bar", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( 0L ) ); // reverse lookup returns first val
        assertTrue( idx.forward( "bar", 0L ) );
        assertTrue( idx.forward( "foo", 0L ) );
        assertTrue( idx.forward( "foo", 1L ) );
        assertTrue( idx.forwardGreaterOrEq( "bar", 0L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", 0L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", 1L ) );
        assertTrue( idx.forwardGreaterOrEq( "foo", -1L ) );
        assertFalse( idx.forwardGreaterOrEq( "foo", 2L ) );
        assertFalse( idx.forwardGreaterOrEq( "bar", 1L ) );
        assertTrue( idx.forwardLessOrEq( "bar", 0L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 0L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 1L ) );
        assertTrue( idx.forwardLessOrEq( "foo", 2L ) );
        assertFalse( idx.forwardLessOrEq( "foo", -1L ) );
        assertFalse( idx.forwardLessOrEq( "bar", -1L ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( 0L ) );
        assertNull( idx.reverseLookup( 1L ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );

        idx.drop( 0L );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.reverseLookup( 0L ) );

        // test add/drop with duplicates in bulk
        idx.add( "foo", 0L );
        idx.add( "foo", 1L );
        idx.add( "bar", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( 0L, ( long ) idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( 0L ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );

        idx.drop( 0L );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertFalse( idx.forward( "bar", 0L ) );
        assertFalse( idx.forward( "foo", 0L ) );

        idx.drop( 1L );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( 0L ) );
        assertNull( idx.reverseLookup( 1L ) );
        assertEquals( 0, idx.count() );
    }


    @Test
    public void testAddDropOneByOne() throws Exception
    {
        initIndex();
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( 0L ) );
        assertNull( idx.reverseLookup( 1L ) );

        // test add/drop without adding any duplicates
        idx.add( "foo", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );

        idx.drop( "foo", 0L );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.reverseLookup( 0L ) );

        // test add/drop with duplicates but one at a time
        idx.add( "foo", 0L );
        idx.add( "foo", 1L );
        idx.add( "bar", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( 0L, ( long ) idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( 0L ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );

        idx.drop( "bar", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertFalse( idx.forward( "bar", 0L ) );

        idx.drop( "foo", 0L );
        assertEquals( 1L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertFalse( idx.forward( "foo", 0L ) );

        idx.drop( "foo", 1L );
        assertNull( idx.forwardLookup( "foo" ) );
        assertNull( idx.forwardLookup( "bar" ) );
        assertNull( idx.reverseLookup( 0L ) );
        assertNull( idx.reverseLookup( 1L ) );
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

        idx.add( "foo", 1234L );
        assertEquals( 1, idx.count() );

        idx.add( "foo", 333L );
        assertEquals( 2, idx.count() );

        idx.add( "bar", 555L );
        assertEquals( 3, idx.count() );

        // use forward index's cursor
        Cursor<IndexEntry<String, ServerEntry, Long>> cursor = idx.forwardCursor();
        cursor.beforeFirst();

        cursor.next();
        IndexEntry<String, ServerEntry, Long> e1 = cursor.get();
        assertEquals( 555L, ( long ) e1.getId() );
        assertEquals( "bar", e1.getValue() );

        cursor.next();
        IndexEntry<String, ServerEntry, Long> e2 = cursor.get();
        assertEquals( 333L, ( long ) e2.getId() );
        assertEquals( "foo", e2.getValue() );

        cursor.next();
        IndexEntry<String, ServerEntry, Long> e3 = cursor.get();
        assertEquals( 1234L, ( long ) e3.getId() );
        assertEquals( "foo", e3.getValue() );

        // use reverse index's cursor
        cursor = idx.reverseCursor();
        cursor.beforeFirst();

        cursor.next();
        e1 = cursor.get();
        assertEquals( 333L, ( long ) e1.getId() );
        assertEquals( "foo", e1.getValue() );

        cursor.next();
        e2 = cursor.get();
        assertEquals( 555L, ( long ) e2.getId() );
        assertEquals( "bar", e2.getValue() );

        cursor.next();
        e3 = cursor.get();
        assertEquals( 1234L, ( long ) e3.getId() );
        assertEquals( "foo", e3.getValue() );
    }


    @Test
    public void testNoEqualityMatching() throws Exception
    {
        JdbmIndex jdbmIndex = new JdbmIndex();

        try
        {
            AttributeType noEqMatchAttribute = new AttributeType( "1.1" );

            jdbmIndex.init( schemaManager, noEqMatchAttribute, dbFileDir );
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
        JdbmIndex jdbmIndex = new JdbmIndex();
        jdbmIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT ),
            dbFileDir );
        jdbmIndex.close();
    }
}
