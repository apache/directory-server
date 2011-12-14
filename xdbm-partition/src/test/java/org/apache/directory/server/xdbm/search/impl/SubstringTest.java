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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.SubstringNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the SubstringCursor and the SubstringEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringTest
{
    private static final Logger LOG = LoggerFactory.getLogger( SubstringTest.class.getSimpleName() );

    File wkdir;
    Partition store;
    static SchemaManager schemaManager = null;
    
    /** txn and operation execution manager factories */
    private static TxnManagerFactory txnManagerFactory;
    private static OperationExecutionManagerFactory executionManagerFactory;


    @BeforeClass
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
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }


    @Before
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        File logDir = new File( wkdir.getPath() + File.separatorChar + "txnlog" + File.separatorChar );
        logDir.mkdirs();
        txnManagerFactory = new TxnManagerFactory( logDir.getPath(), 1 << 13, 1 << 14 );
        executionManagerFactory = new OperationExecutionManagerFactory( txnManagerFactory );
        
        // initialize the store
        store = new AvlPartition( schemaManager, txnManagerFactory, executionManagerFactory );
        store.setId( "example" );
        ( (Store )store ).setCacheSize( 10 );
        ( (Store )store ).setPartitionPath( wkdir.toURI() );
        ( (Store )store ).setSyncOnWrite( false );

        ( (Store )store ).addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        ( (Store )store ).addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        
        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        store.setSuffixDn( suffixDn );

        store.initialize();

        XdbmStoreUtils.loadExampleData( store, schemaManager, executionManagerFactory.instance() );
        
        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ((Partition)store).destroy();
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
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUIDString( 11 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 8 ), cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnStartsWithJim() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnEndsWithBean() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), null, "bean" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );;
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( Strings.getUUIDString( 10 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 9 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedSnStartsWithB() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 6 ), cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedSnEndsWithEr() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), null, "er" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedAttributes() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "walk", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( Strings.getUUIDString( 3 ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "wa", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 5 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 5 ) ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "searchGuide" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "st" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "name" ), "s", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( Strings.getUUIDString( 3 ) );
        indexEntry.setEntry( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "s", null );
        evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        indexEntry.setEntry( store.getMasterTable().get( Strings.getUUIDString( 6 ) ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "b", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException2() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "cn" ), "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        // test before()
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( schemaManager.getAttributeType( "sn" ), "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        SubstringCursor cursor = new SubstringCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        // test before()
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.after( entry );
    }
}
