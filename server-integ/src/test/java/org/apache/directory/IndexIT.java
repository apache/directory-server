/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jdbm.recman.BaseRecordManager;
import jdbm.recman.TransactionManager;


public class IndexIT
{
    private static SchemaManager schemaManager;
    private Normalizer normalizer;

    private JdbmIndex<String> jdbmIndex;
    private AvlIndex<String> avlIndex;

    
    /** The recordManager used */
    private BaseRecordManager recMan;
    
    /** The temporary directory the files will be created in */
    private static Path tempDir;
    
    /** The temporary index file */  
    private File tmpIndexFile;
    
    /** A temporary file */
    private Path tempFile;

    /** The partition transaction */
    private PartitionTxn partitionTxn;

    @BeforeAll
    public static void init() throws Exception
    {
        tempDir = Files.createTempDirectory( IndexIT.class.getSimpleName() );

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
    public void setup() throws Exception
    {
        tempFile = Files.createTempFile( tempDir, "data", null );

        tmpIndexFile = tempFile.toFile();
        partitionTxn = new MockPartitionReadTxn();

        recMan = new BaseRecordManager( tmpIndexFile.getPath() );
        TransactionManager transactionManager = recMan.getTransactionManager();
        transactionManager.setMaximumTransactionsInLog( 2000 );

        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OU_AT );
        normalizer = attributeType.getEquality().getNormalizer();

        jdbmIndex = new JdbmIndex<String>( attributeType.getName(), false );
        jdbmIndex.init( recMan, schemaManager, attributeType );

        avlIndex = new AvlIndex<String>();
        avlIndex.init( schemaManager, attributeType );
    }


    @Test
    public void testAvlIndex() throws Exception
    {
        doTest( avlIndex );
    }


    @Test
    @Disabled("Does not work with JDBM2")
    public void testJdbmIndex() throws Exception
    {
        doTest( jdbmIndex );
    }


    private void doTest( Index<String, String> idx ) throws Exception
    {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";

        for ( int i = 0; i < 26; i++ )
        {
            String val = alphabet.substring( i, i + 1 );
            idx.add( partitionTxn, normalizer.normalize( val ), Strings.getUUID( i + 1 ) );
        }

        assertEquals( 26, idx.count( partitionTxn ) );

        Cursor<IndexEntry<String, String>> cursor1 = idx.forwardCursor( partitionTxn );
        cursor1.beforeFirst();

        assertHasNext( cursor1, Strings.getUUID( 1L ) );
        assertHasNext( cursor1, Strings.getUUID( 2L ) );

        idx.drop( partitionTxn, normalizer.normalize( "c" ), Strings.getUUID( 3L ) );

        for ( long i = 4L; i < 27L; i++ )
        {
            assertHasNext( cursor1, Strings.getUUID( i ) );
        }

        assertFalse( cursor1.next() );

        cursor1.close();
    }


    private void assertHasNext( Cursor<IndexEntry<String, String>> cursor1, String expectedId ) throws Exception
    {
        assertTrue( cursor1.next() );
        assertEquals( expectedId, cursor1.get().getId() );
    }
}
