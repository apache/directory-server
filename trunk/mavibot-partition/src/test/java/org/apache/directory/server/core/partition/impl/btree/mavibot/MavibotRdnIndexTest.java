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

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.exception.DuplicateValueNotAllowedException;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Tests the MavibotRdnIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotRdnIndexTest
{
    private Index<ParentIdAndRdn, String> idx;

    private static SchemaManager schemaManager;

    private RecordManager recordMan;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = MavibotRdnIndexTest.class.getResource( "" ).getPath();
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
        recordMan = new RecordManager( tmpDir.getRoot().getAbsolutePath() );
    }


    @After
    public void teardown() throws Exception
    {
        destroyIndex();
        recordMan.close();
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
        MavibotRdnIndex index = new MavibotRdnIndex();
        index.setWkDirPath( tmpDir.getRoot().toURI() );
        initIndex( index );
    }


    void initIndex( MavibotRdnIndex mavibotIdx ) throws Exception
    {
        if ( mavibotIdx == null )
        {
            mavibotIdx = new MavibotRdnIndex();
        }

        mavibotIdx.setRecordManager( recordMan );
        mavibotIdx.init( schemaManager,
            schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        this.idx = mavibotIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        MavibotRdnIndex MavibotRdnIndex = new MavibotRdnIndex();
        MavibotRdnIndex.setCacheSize( 337 );
        assertEquals( 337, MavibotRdnIndex.getCacheSize() );

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

        destroyIndex();
        initIndex();

        assertEquals( Index.DEFAULT_INDEX_CACHE_SIZE, idx.getCacheSize() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        MavibotRdnIndex rdnIndex = new MavibotRdnIndex();
        assertNull( rdnIndex.getAttribute() );

        initIndex();
        assertEquals( schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT ),
            idx.getAttribute() );
    }


    // -----------------------------------------------------------------------
    // Count Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count() );

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        idx.add( key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count() );

        // setting a different parentId should make this key a different key
        key = new ParentIdAndRdn( Strings.getUUID( 1L ), new Rdn( "cn=key" ) );

        idx.add( key, Strings.getUUID( 1L ) );
        assertEquals( 2, idx.count() );

        //count shouldn't get affected cause of inserting the same key
        // the value will be replaced instead
        idx.add( key, Strings.getUUID( 2L ) );
        assertEquals( 2, idx.count() );

        key = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( "cn=key" ) );
        idx.add( key, Strings.getUUID( 3L ) );
        assertEquals( 3, idx.count() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertEquals( 0, idx.count( key ) );

        idx.add( key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count( key ) );
    }


    // -----------------------------------------------------------------------
    // Add, Drop and Lookup Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testLookups() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( schemaManager, "cn=key" ) );

        assertNull( idx.forwardLookup( key ) );

        idx.add( key, Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( key ) );
        assertEquals( key, idx.reverseLookup( Strings.getUUID( 0L ) ) );

        // check with the different case in UP name, this ensures that the custom
        // key comparator is used
        key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( schemaManager, "cn=KEY" ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( key ) );
        assertEquals( key, idx.reverseLookup( Strings.getUUID( 0L ) ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertNull( idx.forwardLookup( key ) );

        // test add/drop without adding any duplicates
        idx.add( key, Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( key ) );

        idx.drop( key, Strings.getUUID( 0L ) );
        assertNull( idx.forwardLookup( key ) );
        assertNull( idx.reverseLookup( Strings.getUUID( 0L ) ) );
    }


    // -----------------------------------------------------------------------
    // Miscellaneous Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCursors() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertEquals( 0, idx.count() );

        idx.add( key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count() );

        for ( long i = 1; i < 5; i++ )
        {
            key = new ParentIdAndRdn( Strings.getUUID( i ), new Rdn( "cn=key" + i ) );

            idx.add( key, Strings.getUUID( i ) );
        }

        assertEquals( 5, idx.count() );

        // use forward index's cursor
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = idx.forwardCursor();
        cursor.beforeFirst();

        cursor.next();
        IndexEntry<ParentIdAndRdn, String> e1 = cursor.get();
        assertEquals( Strings.getUUID( 0L ), e1.getId() );
        assertEquals( "cn=key", e1.getKey().getRdns()[0].getName() );
        assertEquals( Strings.getUUID( 0L ), e1.getKey().getParentId() );

        cursor.next();
        IndexEntry<ParentIdAndRdn, String> e2 = cursor.get();
        assertEquals( Strings.getUUID( 1L ), e2.getId() );
        assertEquals( "cn=key1", e2.getKey().getRdns()[0].getName() );
        assertEquals( Strings.getUUID( 1L ), e2.getKey().getParentId() );

        cursor.next();
        IndexEntry<ParentIdAndRdn, String> e3 = cursor.get();
        assertEquals( Strings.getUUID( 2L ), e3.getId() );
        assertEquals( "cn=key2", e3.getKey().getRdns()[0].getName() );
        assertEquals( Strings.getUUID( 2 ), e3.getKey().getParentId() );

        // test before(Tuple<>)
        cursor.before( e3 );
        // e3
        assertTrue( cursor.next() );
        assertEquals( e3.getId(), cursor.get().getId() );

        // e4
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 3 ), cursor.get().getId() );

        // e5
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 4 ), cursor.get().getId() );

        assertFalse( cursor.next() );

        cursor.close();
    }

}
