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
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.cursor.LessEqCursor;
import org.apache.directory.server.xdbm.search.evaluator.LessEqEvaluator;
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
    Store<Entry> store;
    static SchemaManager schemaManager = null;


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
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
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

        // initialize the store
        store = new AvlPartition( schemaManager );
        ( ( Partition ) store ).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.POSTALCODE_AT_OID ) );
        ( ( Partition ) store ).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ( ( Partition ) store ).initialize();

        ( ( Partition ) store ).initialize();

        StoreUtils.loadExampleData( store, schemaManager );
        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ( ( Partition ) store ).destroy();
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
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        LessEqCursor<String> cursor = new LessEqCursor<String>( store, evaluator );
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
        assertEquals( Strings.getUUID( 1L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 2L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 3L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 1L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 2L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 3L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 3L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 2L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 1L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        cursor = new LessEqCursor( store, evaluator );

        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 3L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 2L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 1L ), cursor.get().getId() );
        assertEquals( "1", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "2" );

        assertFalse( cursor.available() );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "7" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "3" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );
        cursor.close();

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "1" );

        assertFalse( cursor.available() );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertEquals( "2", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "7" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );
        cursor.close();

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "3" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );
        cursor.close();
    }


    @Test
    public void testCursorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );
        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        LessEqCursor<String> cursor = new LessEqCursor<String>( store, evaluator );
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
            set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );
        }

        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 1L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 2L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 3L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUID( 4L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUID( 5L ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test beforeFirst() ----------
        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );
        }

        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 1L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 2L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 3L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUID( 4L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUID( 5L ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------
        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );
        }

        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 1L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 2L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 3L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUID( 4L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUID( 5L ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        set.clear();
        cursor = new LessEqCursor( store, evaluator );
        cursor.last();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, UUID>( cursor.get().getKey(), cursor.get().getId() ) );
        }

        assertEquals( 5, set.size() );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 1L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 2L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "1", Strings.getUUID( 3L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "2", Strings.getUUID( 4L ) ) ) );
        assertTrue( set.contains( new Tuple<String, UUID>( "3", Strings.getUUID( 5L ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new LessEqCursor( store, evaluator );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "2" );

        try
        {
            cursor.before( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
            cursor.close();
            assertTrue( cursor.isClosed() );
        }

        // ---------- test after() ----------

        cursor = new LessEqCursor( store, evaluator );
        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setKey( "2" );
        try
        {
            cursor.after( indexEntry );
            fail( "Should never get here." );
        }
        catch ( UnsupportedOperationException e )
        {
            cursor.close();
            assertTrue( cursor.isClosed() );
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

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUID( 1L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 4L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 7L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 8L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 9L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 10L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.STREET_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
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
        attrs.add( "entryUUID", Strings.getUUID( 12L ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, attrs );
        ( ( Partition ) store ).add( addContext );

        indexEntry.setId( Strings.getUUID( 12L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithoutDescendants() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.C_POSTALCODE_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "2" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.C_POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUID( 1L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        LessEqNode node = new LessEqNode( at, new StringValue( at, "3" ) );

        LessEqEvaluator evaluator = new LessEqEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String, UUID> indexEntry = new ForwardIndexEntry<String, UUID>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUID( 1L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 4L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 7L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 8L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 9L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<String, UUID>();
        indexEntry.setId( Strings.getUUID( 10L ) );
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

            new LessEqEvaluator( node, store, schemaManager );
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
        new LessEqEvaluator( node, store, schemaManager );
        schemaManager.delete( at );
    }
}
