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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests PresenceCursor and PresenceEvaluator functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class PresenceTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PresenceTest.class.getSimpleName() );

    File wkdir;
    Store<ServerEntry, Long> store;
    static SchemaManager schemaManager = null;


    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = PresenceTest.class.getResource( "" ).getPath();
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
            fail( "Schema load failed : " + ExceptionUtils.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( schemaManager.getErrors() ) );
        }
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

        store.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.CN_AT_OID ) );
        StoreUtils.loadExampleData( store, schemaManager );
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
    public void testIndexedServerEntry() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.CN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 10, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // test first()
        cursor.first();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test last()
        cursor.last();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test beforeFirst()
        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test afterLast()
        cursor.afterLast();
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test before()
        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        cursor.before( entry );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test after()
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        cursor.after( entry );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        node = new PresenceNode( SchemaConstants.OU_AT_OID );
        evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        cursor = new PresenceCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 2, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 3, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 4, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 7, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 9, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 11, ( long ) cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testSystemIndexedServerEntry() throws Exception
    {
        testSystemIndexedServerEntry( SchemaConstants.OBJECT_CLASS_AT_OID );
        testSystemIndexedServerEntry( SchemaConstants.ENTRY_UUID_AT_OID );
        testSystemIndexedServerEntry( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    public void testSystemIndexedServerEntry( String oid ) throws Exception
    {
        PresenceNode node = new PresenceNode( oid );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();

        List<Long> ids = new ArrayList<Long>();
        while ( cursor.next() && cursor.available() )
        {
            ids.add( cursor.get().getId() );
        }
        assertEquals( 11, ids.size() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testNonIndexedServerEntry() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.SN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );

        assertEquals( node, evaluator.getExpression() );
        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 8, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 6, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 5, ( long ) cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // test first()
        cursor.first();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );

        // test last()
        cursor.last();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );

        // test beforeFirst()
        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );

        // test afterLast()
        cursor.afterLast();
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );
        assertEquals( 5, ( long ) cursor.get().getId() );

        // keep testing previous
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );
        assertEquals( 6, ( long ) cursor.get().getId() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );
        assertEquals( 8, ( long ) cursor.get().getId() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ----------- organizationName attribute

        node = new PresenceNode( SchemaConstants.O_AT_OID );
        evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        cursor = new PresenceCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( 1, ( long ) cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.CN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        entry.setId( ( long ) 3 );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        entry.setId( ( long ) 5 );
        assertTrue( evaluator.evaluate( entry ) );
    }


    @Test
    public void testEvaluatorSystemIndexed() throws Exception
    {
        testEvaluatorSystemIndexed( SchemaConstants.OBJECT_CLASS_AT_OID );
        testEvaluatorSystemIndexed( SchemaConstants.ENTRY_UUID_AT_OID );
        testEvaluatorSystemIndexed( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    private void testEvaluatorSystemIndexed( String oid ) throws Exception
    {
        PresenceNode node = new PresenceNode( oid );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );

        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        // no need to set a value or id, because the evaluator must always evaluate to true
        // as each entry contains an objectClass, entryUUID, and entryCSN attribute
        assertTrue( evaluator.evaluate( entry ) );

        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( oid );
        entry.setId( ( long ) 5 );
        assertTrue( evaluator.evaluate( entry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.NAME_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.NAME_AT_OID );
        entry.setId( ( long ) 3 );
        assertTrue( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.NAME_AT_OID );
        entry.setId( ( long ) 5 );
        assertTrue( evaluator.evaluate( entry ) );

        node = new PresenceNode( SchemaConstants.SEARCHGUIDE_AT_OID );
        evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( ( long ) 3 );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( ( long ) 5 );
        entry.setObject( store.lookup( ( long ) 5 ) );
        assertFalse( evaluator.evaluate( entry ) );

        node = new PresenceNode( SchemaConstants.ST_AT_OID );
        evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.ST_AT_OID );
        entry.setId( ( long ) 3 );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.ST_AT_OID );
        entry.setId( ( long ) 5 );
        entry.setObject( store.lookup( ( long ) 5 ) );
        assertFalse( evaluator.evaluate( entry ) );
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.SN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );
        cursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException2() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.CN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.SN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );

        // test before()
        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        PresenceNode node = new PresenceNode( SchemaConstants.SN_AT_OID );
        PresenceEvaluator<Long> evaluator = new PresenceEvaluator<Long>( node, store, schemaManager );
        PresenceCursor<Long> cursor = new PresenceCursor<Long>( store, evaluator );

        // test before()
        ForwardIndexEntry<String, ServerEntry, Long> entry = new ForwardIndexEntry<String, ServerEntry, Long>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.after( entry );
    }
}
