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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.MockPartitionReadTxn;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import jdbm.recman.BaseRecordManager;
import jdbm.recman.TransactionManager;


/**
 * Tests the JdbmRdnIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class JdbmRdnIndexTest
{
    private static File dbFileDir;
    Index<ParentIdAndRdn, String> idx;
    private static SchemaManager schemaManager;
    private PartitionTxn partitionTxn;
    
    /** The temporary directory the files will be created in */
    private static Path tempDir;
    
    /** The temporary index file */  
    private File tmpIndexFile;

    /** A temporary file */
    private Path tempFile;

    /** The RecordManager */
    private BaseRecordManager recMan;


    @BeforeAll
    public static void init() throws Exception
    {
        tempDir = Files.createTempDirectory( JdbmIndexTest.class.getSimpleName() );

        File schemaRepository = new File( tempDir.toFile(), "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( tempDir.toFile() );
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
        tempFile = Files.createTempFile( tempDir, "data", null );

        tmpIndexFile = tempFile.toFile();
        tmpIndexFile.deleteOnExit();
        
        recMan = new BaseRecordManager( tmpIndexFile.getPath() );
        TransactionManager transactionManager = recMan.getTransactionManager();
        transactionManager.setMaximumTransactionsInLog( 2000 );
    }


    @AfterEach
    public void teardown() throws Exception
    {
        recMan.close();
        destroyIndex();
    }

    
    
    @AfterAll
    public static void cleanup() throws Exception
    {
        FileUtils.deleteDirectory( tempDir.toFile() );
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
        JdbmRdnIndex index = new JdbmRdnIndex();
        index.setWkDirPath( tmpIndexFile.toURI() );
        initIndex( index );
        partitionTxn = new MockPartitionReadTxn();
    }


    void initIndex( JdbmRdnIndex jdbmIdx ) throws Exception
    {
        if ( jdbmIdx == null )
        {
            jdbmIdx = new JdbmRdnIndex();
        }

        jdbmIdx.init( recMan, schemaManager,
            schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        this.idx = jdbmIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        JdbmRdnIndex JdbmRdnIndex = new JdbmRdnIndex();
        JdbmRdnIndex.setCacheSize( 337 );
        assertEquals( 337, JdbmRdnIndex.getCacheSize() );

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
    public void testWkDirPath() throws Exception
    {
        File wkdir = new File( dbFileDir, "foo" );

        // uninitialized index
        JdbmRdnIndex jdbmRdnIndex = new JdbmRdnIndex();
        jdbmRdnIndex.setWkDirPath( wkdir.toURI() );
        assertEquals( "foo", new File( jdbmRdnIndex.getWkDirPath() ).getName() );

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

        assertEquals( tmpIndexFile.toURI(), idx.getWkDirPath() );

        destroyIndex();

        jdbmRdnIndex = new JdbmRdnIndex();
        wkdir.mkdirs();
        jdbmRdnIndex.setWkDirPath( wkdir.toURI() );
        initIndex( jdbmRdnIndex );
        assertEquals( wkdir.toURI(), idx.getWkDirPath() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        JdbmRdnIndex rdnIndex = new JdbmRdnIndex();
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
        assertEquals( 0, idx.count( partitionTxn ) );

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        idx.add( partitionTxn, key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count( partitionTxn ) );

        // setting a different parentId should make this key a different key
        key = new ParentIdAndRdn( Strings.getUUID( 1L ), new Rdn( "cn=key" ) );

        idx.add( partitionTxn, key, Strings.getUUID( 1L ) );
        assertEquals( 2, idx.count( partitionTxn ) );

        //count shouldn't get affected cause of inserting the same key
        idx.add( partitionTxn, key, Strings.getUUID( 2L ) );
        assertEquals( 2, idx.count( partitionTxn ) );

        key = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( "cn=key" ) );
        idx.add( partitionTxn, key, Strings.getUUID( 3L ) );
        assertEquals( 3, idx.count( partitionTxn ) );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertEquals( 0, idx.count( partitionTxn, key ) );

        idx.add( partitionTxn, key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count( partitionTxn, key ) );
    }


    // -----------------------------------------------------------------------
    // Add, Drop and Lookup Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testLookups() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( schemaManager, "cn=key" ) );

        assertNull( idx.forwardLookup( partitionTxn, key ) );

        idx.add( partitionTxn, key, Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( partitionTxn, key ) );
        assertEquals( key, idx.reverseLookup( partitionTxn, Strings.getUUID( 0L ) ) );

        // check with the different case in UP name, this ensures that the custom
        // key comparator is used
        key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( schemaManager, "cn=KEY" ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup( partitionTxn, key ) );
        assertEquals( key, idx.reverseLookup( partitionTxn, Strings.getUUID( 0L ) ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertNull( idx.forwardLookup( partitionTxn, key ) );

        // test add/drop without adding any duplicates
        idx.add( partitionTxn, key, Strings.getUUID( 0L ) );
        assertEquals( Strings.getUUID( 0L ), idx.forwardLookup(partitionTxn,  key ) );

        idx.drop( partitionTxn, key, Strings.getUUID( 0L ) );
        assertNull( idx.forwardLookup( partitionTxn, key ) );
        assertNull( idx.reverseLookup( partitionTxn, Strings.getUUID( 0L ) ) );
    }


    // -----------------------------------------------------------------------
    // Miscellaneous Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCursors() throws Exception
    {
        initIndex();

        ParentIdAndRdn key = new ParentIdAndRdn( Strings.getUUID( 0L ), new Rdn( "cn=key" ) );

        assertEquals( 0, idx.count( partitionTxn ) );

        idx.add( partitionTxn, key, Strings.getUUID( 0L ) );
        assertEquals( 1, idx.count( partitionTxn ) );

        for ( long i = 1; i < 5; i++ )
        {
            key = new ParentIdAndRdn( Strings.getUUID( i ), new Rdn( "cn=key" + i ) );

            idx.add( partitionTxn, key, Strings.getUUID( i ) );
        }

        assertEquals( 5, idx.count( partitionTxn ) );

        // use forward index's cursor
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = idx.forwardCursor( partitionTxn );
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
