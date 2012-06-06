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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.filter.LessEqNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.LdapSyntax;
import org.apache.directory.shared.ldap.model.schema.MutableAttributeType;
import org.apache.directory.shared.ldap.model.schema.MutableMatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.StringComparator;
import org.apache.directory.shared.ldap.model.schema.parsers.SyntaxCheckerDescription;
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
 * Tests the LessEqEvaluator and LessEqCursor classes for correct operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LessEqTest
{
    public static final Logger LOG = LoggerFactory.getLogger( LessEqTest.class );

    File wkdir;
    Partition store;
    static SchemaManager schemaManager = null;
    
    /** txn and operation execution manager factories */
    private static TxnManagerFactory txnManagerFactory;
    private static OperationExecutionManagerFactory executionManagerFactory;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AndCursorTest.class.getResource( "" ).getPath();
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
        ( ( Store ) store ).setCacheSize( 10 );
        ( ( Store ) store ).setPartitionPath( wkdir.toURI() );
        ( ( Store ) store ).setSyncOnWrite( false );

        ( ( Store ) store ).addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        ( ( Store ) store ).addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        ( ( Store ) store ).addIndex( new AvlIndex( SchemaConstants.POSTALCODE_AT_OID ) );
        store.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        store.initialize();

        store.initialize();

        XdbmStoreUtils.loadExampleData( store, schemaManager, executionManagerFactory.instance() );
        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            store.destroy();
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testCursorIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        LessEqCursor<String> cursor = new LessEqCursor<String>( store, evaluator, txnManagerFactory, executionManagerFactory );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try
        {
            cursor.get();
            fail();
        }
        catch ( InvalidCursorPositionException e )
        {
        }

        // ---------- test beforeFirst() ----------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 1 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 3 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 1 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 3 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 3 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 1 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );

        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 3 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 2 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 1 ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "2" );

        assertFalse( cursor.available() );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "7" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory);
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "3" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "1" );

        assertFalse( cursor.available() );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 4 ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "7" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "3" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUIDString( 5 ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getValue() );
        cursor.close();
    }


    @Test
    public void testCursorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        LessEqCursor<String> cursor = new LessEqCursor<String>( store, evaluator, txnManagerFactory, executionManagerFactory );
        assertNotNull( cursor );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );

        // ---------- test bad get() ----------

        try
        {
            cursor.get();
            fail();
        }
        catch ( InvalidCursorPositionException e )
        {
        }

        // ---------- test beforeFirst() ----------

        Set<Tuple<String, UUID>> set = new HashSet<Tuple<String, UUID>>();
        cursor.beforeFirst();
        assertFalse( cursor.available() );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 1 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 2 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 3 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUIDString( 4 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUIDString( 5 ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test beforeFirst() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.first();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 1 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 2 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 3 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUIDString( 4 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUIDString( 5 ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 1 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 2 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 3 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUIDString( 4 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUIDString( 5 ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        cursor.last();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getValue(), cursor.get().getId() ) );
        }
        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 1 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 2 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUIDString( 3 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUIDString( 4 ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUIDString( 5 ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "2" );
        try
        {
            cursor.before( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator, txnManagerFactory, executionManagerFactory );
        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setValue( "2" );
        try
        {
            cursor.after( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
        }
    }


    // -----------------------------------------------------------------------
    // Evaluator Test Cases
    // -----------------------------------------------------------------------

    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUIDString( 1 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 4 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 7 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 8 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 9 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 10 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.STREET_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.STREET_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        Dn dn = new Dn( schemaManager, "cn=jane doe,o=good times co." );
        Entry attrs = new DefaultEntry( schemaManager, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "c-street", "1" );
        attrs.add( "cn", "jane doe" );
        attrs.add( "sn", "doe" );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );
        
        AddOperationContext addContext = new AddOperationContext( schemaManager, attrs );
        executionManagerFactory.instance().add( store, addContext );

        indexEntry.setId( Strings.getUUIDString( 12 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithoutDescendants() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.C_POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.C_POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUIDString( 1 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        ForwardIndexEntry<String> indexEntry = new ForwardIndexEntry<String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUIDString( 1 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 4 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 7 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 8 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 9 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String>();
        indexEntry.setId( Strings.getUUIDString( 10 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test(expected = IllegalStateException.class)
    public void testEvaluatorAttributeNoMatchingRule() throws Exception
    {
        LdapSyntax syntax = new BogusSyntax( 1 );
        MutableAttributeType at = new MutableAttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".2000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( syntax );

        assertTrue( schemaManager.add( syntax ) );
        assertTrue( schemaManager.add( at ) );

        try
        {
            LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

            new LessEqEvaluator( node, store, schemaManager , txnManagerFactory, executionManagerFactory );
        }
        finally
        {
            assertTrue( schemaManager.delete( at ) );
            assertTrue( schemaManager.delete( syntax ) );
        }
    }


    @Test
    public void testEvaluatorAttributeOrderingMatchingRule() throws Exception
    {
        LdapSyntax syntax = new BogusSyntax( 2 );

        MutableMatchingRule mr = new MutableMatchingRule( "1.1" );
        mr.setSyntax( syntax );
        mr.setLdapComparator( new StringComparator( "1.1" ) );

        MutableAttributeType at = new MutableAttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".3000" );
        at.addName( "bogus" );
        at.setSchemaName( "other" );
        at.setSyntax( syntax );
        at.setOrdering( mr );

        assertTrue( schemaManager.add( syntax ) );
        assertTrue( schemaManager.add( mr ) );
        assertTrue( schemaManager.add( at ) );

        SyntaxCheckerDescription desc = new SyntaxCheckerDescription( at.getSyntax().getOid() );
        desc.setDescription( "bogus" );
        desc.setFqcn( BogusSyntax.class.getName() );
        List<String> names = new ArrayList<String>();
        names.add( "bogus" );
        desc.setNames( names );
        desc.setObsolete( false );

        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        new LessEqEvaluator( node, store, schemaManager, txnManagerFactory, executionManagerFactory );
        schemaManager.delete( at );
    }
}
