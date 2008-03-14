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


import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.Ignore;
import static org.junit.Assert.*;

import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;

import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.IOException;


/**
 * TODO doc me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmIndexTest
{
    AttributeTypeRegistry registry;
    File dbFileDir;
    Index<String> idx;


    @Before
    public void setup() throws Exception
    {
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        Registries registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        // load essential bootstrap schemas
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        this.registry = registries.getAttributeTypeRegistry();

        if ( dbFileDir != null )
        {
            dbFileDir.delete();
        }

        dbFileDir = new File( File.createTempFile(
            JdbmIndexTest.class.getSimpleName(), "db" ).getParentFile(),
            JdbmIndexTest.class.getSimpleName() );

        dbFileDir.mkdirs();
    }


    @After
    public void teardown() throws Exception
    {
        registry = null;
        destroyIndex();
    }


    void destroyIndex() throws Exception
    {
        if ( idx != null )
        {
            idx.sync();
            idx.close();
            File file = new File( idx.getWkDirPath(), idx.getAttribute().getName() + ".db" );
            file.delete();
        }
        idx = null;
    }


    void initIndex() throws Exception
    {
        initIndex( new JdbmIndex() );
    }


    void initIndex( JdbmIndex jdbmIdx ) throws Exception
    {
        if ( jdbmIdx == null )
        {
            jdbmIdx = new JdbmIndex();
        }

        jdbmIdx.init( registry.lookup( SchemaConstants.OU_AT ), dbFileDir );
        this.idx = jdbmIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------


    @Test
    public void testAttributeId() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex = new JdbmIndex();
        jdbmIndex.setAttributeId( "foo" );
        assertEquals( "foo", jdbmIndex.getAttributeId() );

        jdbmIndex = new JdbmIndex( "bar" );
        assertEquals( "bar", jdbmIndex.getAttributeId() );

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
        initIndex( new JdbmIndex( "foo" ) );
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
        JdbmIndex jdbmIndex = new JdbmIndex();
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
        jdbmIndex = new JdbmIndex();
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
            ( ( JdbmIndex ) idx).setNumDupLimit( 30 );
            fail( "Should not be able to set numDupLimit after initialization." );
        }
        catch ( Exception e )
        {
        }
        assertEquals( JdbmIndex.DEFAULT_DUPLICATE_LIMIT, ( ( JdbmIndex ) idx).getNumDupLimit() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        JdbmIndex jdbmIndex = new JdbmIndex();
        assertNull( jdbmIndex.getAttribute() );

        initIndex();
        assertEquals( registry.lookup( "ou" ), idx.getAttribute() );
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

        idx.add( "foo", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );

        idx.add( "foo", 1L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 0L ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );

        idx.add( "bar", 0L );
        assertEquals( 0L, ( long ) idx.forwardLookup( "bar" ) );
        assertEquals( "bar", idx.reverseLookup( 0L ) );  // reverse lookup returns first val
    }


    @Test
    @Ignore( "Will not work until duplicates cursor is finished." )
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
        assertEquals( 0L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertFalse( idx.hasValue( "bar", 0L ) );
        assertFalse( idx.hasValue( "foo", 0L ) );

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
        assertFalse( idx.hasValue( "bar", 0L ) );

        idx.drop( "foo", 0L );
        assertEquals( 1L, ( long ) idx.forwardLookup( "foo" ) );
        assertEquals( "foo", idx.reverseLookup( 1L ) );
        assertFalse( idx.hasValue( "foo", 0L ) );

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
    public void testNoEqualityMatching() throws Exception
    {
        JdbmIndex jdbmIndex = new JdbmIndex();

        try
        {
            jdbmIndex.init( new NoEqMatchAttribute(), dbFileDir );
            fail( "should not get here" );
        }
        catch( IOException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Failing Tests
    // -----------------------------------------------------------------------


    @Test
    @Ignore ( "not working now" )
    public void testSingleValuedAttribute() throws Exception
    {
        JdbmIndex jdbmIndex = new JdbmIndex();
        jdbmIndex.init( registry.lookup( SchemaConstants.CREATORS_NAME_AT ), dbFileDir );
    }
}
