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

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.MutableAttributeType;
import org.apache.directory.api.ldap.model.schema.MutableMatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.StringComparator;
import org.apache.directory.api.ldap.model.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.cursor.GreaterEqCursor;
import org.apache.directory.server.xdbm.search.evaluator.GreaterEqEvaluator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the GreaterEqEvaluator and GreaterEqCursor classes for correct operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GreaterEqTest
{
    public static final Logger LOG = LoggerFactory.getLogger( GreaterEqTest.class );

    File wkdir;
    Store store;
    static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = GreaterEqTest.class.getResource( "" ).getPath();
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

        CacheService cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache" ) );
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
        store = new AvlPartition( schemaManager, dnFactory );
        ( ( Partition ) store ).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.POSTALCODE_AT_OID ) );
        ( ( Partition ) store ).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
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
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        GreaterEqCursor<String> cursor = new GreaterEqCursor<String>( store, evaluator );
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
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6L ), cursor.get().getId() );
        assertEquals( "4", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6L ), cursor.get().getId() );
        assertEquals( "4", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6L ), cursor.get().getId() );
        assertEquals( "4", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        cursor = new GreaterEqCursor( store, evaluator );

        cursor.last();

        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6L ), cursor.get().getId() );
        assertEquals( "4", cursor.get().getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        indexEntry.setKey( "5" );

        assertFalse( cursor.available() );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setKey( "7" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );
        cursor.close();

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setKey( "3" );
        cursor.before( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 5L ), cursor.get().getId() );
        assertEquals( "3", cursor.get().getKey() );
        cursor.close();

        // ---------- test after() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setKey( "4" );

        assertFalse( cursor.available() );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7L ), cursor.get().getId() );
        assertEquals( "5", cursor.get().getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
        indexEntry.setKey( "7" );
        cursor.after( indexEntry );
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertEquals( Strings.getUUID( 8L ), cursor.get().getId() );
        assertEquals( "6", cursor.get().getKey() );
        cursor.close();

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
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
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        GreaterEqCursor<String> cursor = new GreaterEqCursor<String>( store, evaluator );
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

        Set<Tuple<String, String>> set = new HashSet<Tuple<String, String>>();
        cursor.beforeFirst();
        assertFalse( cursor.available() );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );
        }
        assertEquals( 4, set.size() );
        assertTrue( set.contains( new Tuple<String, String>( "3", Strings.getUUID( 5L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "4", Strings.getUUID( 6L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "5", Strings.getUUID( 7L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "6", Strings.getUUID( 8L ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first() ----------

        set.clear();
        cursor = new GreaterEqCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );
        }
        assertEquals( 4, set.size() );
        assertTrue( set.contains( new Tuple<String, String>( "3", Strings.getUUID( 5L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "4", Strings.getUUID( 6L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "5", Strings.getUUID( 7L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "6", Strings.getUUID( 8L ) ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test afterLast() ----------

        set.clear();
        cursor = new GreaterEqCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );
        }
        assertEquals( 4, set.size() );
        assertTrue( set.contains( new Tuple<String, String>( "3", Strings.getUUID( 5L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "4", Strings.getUUID( 6L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "5", Strings.getUUID( 7L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "6", Strings.getUUID( 8L ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test last() ----------

        set.clear();
        cursor = new GreaterEqCursor( store, evaluator );
        cursor.last();

        assertTrue( cursor.available() );
        set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            set.add( new Tuple<String, String>( cursor.get().getKey(), cursor.get().getId() ) );
        }
        assertEquals( 4, set.size() );
        assertTrue( set.contains( new Tuple<String, String>( "3", Strings.getUUID( 5L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "4", Strings.getUUID( 6L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "5", Strings.getUUID( 7L ) ) ) );
        assertTrue( set.contains( new Tuple<String, String>( "6", Strings.getUUID( 8L ) ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test before() ----------

        cursor = new GreaterEqCursor( store, evaluator );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
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

        cursor = new GreaterEqCursor( store, evaluator );
        indexEntry = new IndexEntry<String, String>();
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
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Partition.DEFAULT_ID );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 4L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 7L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 8L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 9L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 10L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDescendantValue() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.STREET_AT_OID );
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "2" ) );
        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.STREET_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        Dn dn = new Dn( schemaManager, "cn=jane doe,o=good times co." );
        Entry attrs = new DefaultEntry( schemaManager, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "c-street", "3" );
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
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "2" ) );

        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.C_POSTALCODE_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Partition.DEFAULT_ID );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        AttributeType at = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.POSTOFFICEBOX_AT_OID );
        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );

        GreaterEqEvaluator evaluator = new GreaterEqEvaluator( node, store, schemaManager );
        IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();
        assertEquals( node, evaluator.getExpression() );
        assertEquals( SchemaConstants.POSTOFFICEBOX_AT_OID, evaluator.getAttributeType().getOid() );
        assertNotNull( evaluator.getNormalizer() );
        assertNotNull( evaluator.getComparator() );

        indexEntry.setId( Strings.getUUID( 1L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 4L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 7L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 8L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
        indexEntry.setId( Strings.getUUID( 9L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new IndexEntry<String, String>();
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

        schemaManager.add( syntax );
        schemaManager.add( at );

        try
        {
            GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );
            new GreaterEqEvaluator( node, store, schemaManager );
        }
        finally
        {
            schemaManager.delete( at );
            schemaManager.delete( syntax );
        }
    }


    @Test
    public void testEvaluatorAttributeOrderingMatchingRule() throws Exception
    {
        LdapSyntax syntax = new BogusSyntax( 1 );
        MutableMatchingRule mr = new MutableMatchingRule( "1.1" );
        mr.setSyntax( syntax );
        mr.setLdapComparator( new StringComparator( "1.1" ) );

        MutableAttributeType at = new MutableAttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT_OID + ".5000" );
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
        //schemaManager.register( at.getSyntax().getSyntaxChecker() );

        GreaterEqNode node = new GreaterEqNode( at, new StringValue( at, "3" ) );
        new GreaterEqEvaluator( node, store, schemaManager );
        schemaManager.delete( at );
    }
}