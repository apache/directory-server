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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
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
 */
public class PresenceTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PresenceTest.class.getSimpleName() );

    File wkdir;
    Store store;
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

        // initialize the store
        store = new AvlPartition( schemaManager );
        ((Partition)store).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex( SchemaConstants.CN_AT_OID ) );
        ((Partition)store).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ((Partition)store).initialize();

        ((Partition)store).initialize();

        StoreUtils.loadExampleData( store, schemaManager );
        
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
    public void testIndexedServerEntry() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 5 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 6 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 8 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 9 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 10 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 11 ), cursor.get().getId() );
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
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        cursor.before( entry );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        // test after()
        entry = new ForwardIndexEntry<String>();
        cursor.after( entry );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.CN_AT_OID, cursor.get().getValue() );

        node = new PresenceNode( schemaManager.getAttributeType( "ou" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        cursor = new PresenceCursor( store, evaluator );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 2 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 3 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 4 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 5 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 6 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 7 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 8 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 9 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 11 ), cursor.get().getId() );
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
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( oid ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        List<UUID> ids = new ArrayList<UUID>();
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
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        Set<UUID> set = new HashSet<UUID>();
        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );
            set.add( cursor.get().getId() );
        }
        assertEquals( 3, set.size() );
        assertTrue( set.contains( StoreUtils.getUUIDString( 5 ) ) );
        assertTrue( set.contains( StoreUtils.getUUIDString( 6 ) ) );
        assertTrue( set.contains( StoreUtils.getUUIDString( 8 ) ) );

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
        set.clear();
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getValue() );
            set.add( cursor.get().getId() );
        }
        assertEquals( 3, set.size() );
        assertTrue( set.contains( StoreUtils.getUUIDString( 5 ) ) );
        assertTrue( set.contains( StoreUtils.getUUIDString( 6 ) ) );
        assertTrue( set.contains( StoreUtils.getUUIDString( 8 ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // ----------- organizationName attribute

        node = new PresenceNode( schemaManager.getAttributeType( "o" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        cursor = new PresenceCursor( store, evaluator );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( StoreUtils.getUUIDString( 1 ), cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 3 ) );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.CN_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 5 ) );
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
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( oid ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );

        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        // no need to set a value or id, because the evaluator must always evaluate to true
        // as each entry contains an objectClass, entryUUID, and entryCSN attribute
        assertTrue( evaluator.evaluate( entry ) );

        entry = new ForwardIndexEntry<String>();
        entry.setValue( oid );
        entry.setId( StoreUtils.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( entry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "name" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.NAME_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 3 ) );
        assertTrue( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.NAME_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( entry ) );

        node = new PresenceNode( schemaManager.getAttributeType( "searchGuide" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 3 ) );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 5 ) );
        entry.setEntry( store.lookup( StoreUtils.getUUIDString( 5 ) ) );
        assertFalse( evaluator.evaluate( entry ) );

        node = new PresenceNode( schemaManager.getAttributeType( "st" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.ST_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 3 ) );
        assertFalse( evaluator.evaluate( entry ) );
        entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.ST_AT_OID );
        entry.setId( StoreUtils.getUUIDString( 5 ) );
        entry.setEntry( store.lookup( StoreUtils.getUUIDString(53 ) ) );
        assertFalse( evaluator.evaluate( entry ) );
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );
        cursor.get();
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException2() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<String> entry = new ForwardIndexEntry<String>();
        entry.setValue( SchemaConstants.SN_AT_OID );
        cursor.after( entry );
    }
}
