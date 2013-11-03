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
package org.apache.directory.server.xdbm.impl.avl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests the AvlRdnIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlRdnIndexTest
{
    private static File dbFileDir;
    Index<ParentIdAndRdn, String> idx;
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AvlRdnIndexTest.class.getResource( "" ).getPath();
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

        File tmpIndexFile = File.createTempFile( AvlRdnIndexTest.class.getSimpleName(), "db" );
        tmpIndexFile.deleteOnExit();
        dbFileDir = new File( tmpIndexFile.getParentFile(), AvlRdnIndexTest.class.getSimpleName() );

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
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        initIndex( new AvlRdnIndex() );
    }


    void initIndex( AvlRdnIndex avlIdx ) throws Exception
    {
        if ( avlIdx == null )
        {
            avlIdx = new AvlRdnIndex();
        }

        avlIdx.init( schemaManager, schemaManager
            .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        this.idx = avlIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test(expected = UnsupportedOperationException.class)
    @Ignore
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        AvlRdnIndex AvlRdnIndex = new AvlRdnIndex();
        AvlRdnIndex.setCacheSize( 337 );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testWkDirPath() throws Exception
    {
        // uninitialized index
        AvlRdnIndex AvlRdnIndex = new AvlRdnIndex();
        AvlRdnIndex.setWkDirPath( new File( dbFileDir, "foo" ).toURI() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        AvlRdnIndex rdnIndex = new AvlRdnIndex();
        assertNull( rdnIndex.getAttribute() );

        initIndex();
        assertEquals( schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT ), idx
            .getAttribute() );
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

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertNull( idx.forwardLookup( key ) );

        idx.add( key, Strings.getUUID( 0L ) );
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
        assertEquals( Strings.getUUID( 2L ), e3.getKey().getParentId() );

        cursor.close();
    }

    //    @Test
    //    public void testStoreRdnWithTwoATAVs() throws Exception
    //    {
    //        initIndex();
    //        
    //        Dn dn = new Dn( "dc=example,dc=com" );
    //        dn.normalize( schemaManager.getNormalizerMapping() );
    //        
    //        Rdn rdn = new Rdn( dn.getName() );
    //        rdn._setParentId( 1 );
    //        idx.add( rdn, 0l );
    //        
    //        Rdn rdn2 = idx.reverseLookup( 0l );
    //        System.out.println( rdn2 );
    //        InternalRdnComparator rdnCom = new InternalRdnComparator( "" );
    //        assertEquals( 0, rdnCom.compare( rdn, rdn2 ) );
    //    }
}
