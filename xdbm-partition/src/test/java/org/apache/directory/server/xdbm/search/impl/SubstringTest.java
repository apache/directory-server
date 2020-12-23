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
package org.apache.directory.server.xdbm.search.impl;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.cursor.SubstringCursor;
import org.apache.directory.server.xdbm.search.evaluator.SubstringEvaluator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the SubstringCursor and the SubstringEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SubstringTest
{
    private static final Logger LOG = LoggerFactory.getLogger( SubstringTest.class );

    File wkdir;
    Store store;
    static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;


    @BeforeAll
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SubstringTest.class.getResource( "" ).getPath();
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

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        dnFactory = new DefaultDnFactory( schemaManager, 100 );
    }


    @BeforeEach
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        StoreUtils.createdExtraAttributes( schemaManager );
        
        // initialize the store
        store = new AvlPartition( schemaManager, dnFactory );
        ( ( Partition ) store ).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex<String>( SchemaConstants.CN_AT_OID ) );

        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        ( ( Partition ) store ).setSuffixDn( suffixDn );

        ( ( Partition ) store ).initialize();

        StoreUtils.loadExampleData( store, schemaManager );

        LOG.debug( "Created new store" );
    }


    @AfterEach
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ( ( Partition ) store ).destroy( null );
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testIndexedCnStartsWithJ() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        SubstringCursor cursor = new SubstringCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( " jack  daniels ", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( " jack  daniels ", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( " jack  daniels ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test last ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( " johnny  walker ", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertEquals( " jack  daniels ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testIndexedCnStartsWithJim() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        SubstringCursor cursor = new SubstringCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test last ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testIndexedCnEndsWithBean() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), null, "bean" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        SubstringCursor cursor = new SubstringCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test last ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( " jim  bean ", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testNonIndexedSnStartsWithB() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        SubstringCursor cursor = new SubstringCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "BEAN", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "BEAN", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "BEAN", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test last ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertEquals( "BEAN", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testIndexedSnEndsWithEr() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), null, "er" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        SubstringCursor cursor = new SubstringCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( "WAlkeR", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( "WAlkeR", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertEquals( "WAlkeR", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test last ----------

        cursor = new SubstringCursor( txn, store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "WAlkeR", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testNonIndexedAttributes() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "walk", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );
        
        indexEntry.setId( Strings.getUUID( 3L ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );
        
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "wa", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 5L ) ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "searchGuide" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "st" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "s", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );
        indexEntry.setId( Strings.getUUID( 3L ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertTrue( evaluator.evaluate( txn, indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "s", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        indexEntry.setEntry( store.fetch( txn, Strings.getUUID( 6L ) ) );
        assertFalse( evaluator.evaluate( txn, indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
    }


    @Test
    public void testInvalidCursorPositionException() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
            SubstringCursor cursor = null;
    
            try
            {
                SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
                SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
                cursor = new SubstringCursor( txn, store, evaluator );
                cursor.get();
            }
            finally
            {
                cursor.close();
            }
        } );
    }


    @Test
    public void testInvalidCursorPositionException2() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
            SubstringCursor cursor = null;
    
            try
            {
                SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
                SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
                cursor = new SubstringCursor( txn, store, evaluator );
                cursor.get();
            }
            finally
            {
                cursor.close();
            }
        } );
    }


    @Test
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () ->
        {
            PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
            SubstringCursor cursor = null;
    
            try
            {
                SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
                SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
                cursor = new SubstringCursor( txn, store, evaluator );
    
                // test before()
                IndexEntry<String, String> entry = new IndexEntry<String, String>();
                entry.setKey( SchemaConstants.SN_AT_OID );
                cursor.before( entry );
            }
            finally
            {
                cursor.close();
            }
        } );
    }


    @Test
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        assertThrows( UnsupportedOperationException.class, () ->
        {
            PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
            SubstringCursor cursor = null;
    
            try
            {
                SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
                SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager );
                cursor = new SubstringCursor( txn, store, evaluator );
    
                // test before()
                IndexEntry<String, String> entry = new IndexEntry<String, String>();
                entry.setKey( SchemaConstants.SN_AT_OID );
                cursor.after( entry );
            }
            finally
            {
                cursor.close();
            }
        } );
    }
}
