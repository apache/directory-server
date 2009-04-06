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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Set;
import java.util.HashSet;


/**
 * Tests the SubstringCursor and the SubstringEvaluator.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringTest
{
    private static final Logger LOG = LoggerFactory.getLogger( SubstringTest.class.getSimpleName() );

    File wkdir;
    JdbmStore<ServerEntry> store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;


    public SubstringTest() throws Exception
    {
        // setup the standard registries
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );

        // load essential bootstrap schemas
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        bootstrapSchemas.add( new CollectiveSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        attributeRegistry = registries.getAttributeTypeRegistry();
    }


    @Before
    public void createStore() throws Exception
    {
        destryStore();

        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        // initialize the store
        store = new JdbmStore<ServerEntry>();
        store.setName( "example" );
        store.setCacheSize( 10 );
        store.setWorkingDirectory( wkdir );
        store.setSyncOnWrite( false );

        store.addIndex( new JdbmIndex<String,ServerEntry>( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex<String,ServerEntry>( SchemaConstants.CN_AT_OID ) );
        StoreUtils.loadExampleData( store, registries );
        LOG.debug( "Created new store" );
    }


    @After
    public void destryStore() throws Exception
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
    public void testIndexedCnStartsWithJ() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.CN_AT_OID, "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );


        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 11, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "johnny walker", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertEquals( "jack daniels", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnStartsWithJim() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.CN_AT_OID, "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );


        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedCnEndsWithBean() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.CN_AT_OID, null, "bean" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );


        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertEquals( 10, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "jim bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedSnStartsWithB() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, "b", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );


        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertEquals( "bean", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testIndexedSnEndsWithEr() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, null, "er" );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // ---------- test first ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.first();

        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );


        // ---------- test afterLast ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ---------- test last ----------

        cursor = new SubstringCursor( store, evaluator );
        cursor.last();
        assertTrue( cursor.available() );

        assertTrue( cursor.available() );
        assertEquals( 5L, ( long ) cursor.get().getId() );
        assertEquals( "walker", cursor.get().getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedAttributes() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, "walk", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 3L );
        indexEntry.setObject( null );
        assertFalse( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 6L );
        indexEntry.setObject( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.SN_AT_OID, "wa", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 5L );
        indexEntry.setObject( store.lookup( 5L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.SEARCHGUIDE_AT_OID, "j", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.ST_AT_OID, "j", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.NAME_AT_OID, "j", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.NAME_AT_OID, "s", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.CN_AT_OID, "jim", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );
        indexEntry.setId( 3L );
        indexEntry.setObject( null );
        assertFalse( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.CN_AT_OID, "j", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        node = new SubstringNode( SchemaConstants.CN_AT_OID, "s", null );
        evaluator = new SubstringEvaluator( node, store, registries );
        indexEntry = new ForwardIndexEntry<String,ServerEntry>();
        indexEntry.setId( 6L );
        indexEntry.setObject( store.lookup( 6L ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
    }


    @Test ( expected = InvalidCursorPositionException.class )
    public void testInvalidCursorPositionException() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, "b", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );
        cursor.get();
    }


    @Test ( expected = InvalidCursorPositionException.class )
    public void testInvalidCursorPositionException2() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.CN_AT_OID, "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );
        cursor.get();
    }


    @Test ( expected = UnsupportedOperationException.class )
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<String,ServerEntry> entry = new ForwardIndexEntry<String,ServerEntry>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.before( entry );
    }


    @Test ( expected = UnsupportedOperationException.class )
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        SubstringNode node = new SubstringNode( SchemaConstants.SN_AT_OID, "j", null );
        SubstringEvaluator evaluator = new SubstringEvaluator( node, store, registries );
        SubstringCursor cursor = new SubstringCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<String,ServerEntry> entry = new ForwardIndexEntry<String,ServerEntry>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.after( entry );
    }
}
