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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.cursor.PresenceCursor;
import org.apache.directory.server.xdbm.search.evaluator.PresenceEvaluator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests PresenceCursor and PresenceEvaluator functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Execution(ExecutionMode.SAME_THREAD)
public class PresenceTest
{
    private static final Logger LOG = LoggerFactory.getLogger( PresenceTest.class );

    File wkdir;
    Store store;
    static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;
    private static String NORMALIZED_CN_OID;


    @BeforeAll
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
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        dnFactory = new DefaultDnFactory( schemaManager, 100 );

        NORMALIZED_CN_OID = schemaManager.getAttributeType( ApacheSchemaConstants.APACHE_PRESENCE_AT ).getEquality()
            .getNormalizer().normalize( SchemaConstants.CN_AT_OID );
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

        store.addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID, false ) );
        store.addIndex( new AvlIndex<String>( SchemaConstants.CN_AT_OID, false ) );
        ( ( Partition ) store ).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ( ( Partition ) store ).initialize();

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
    public void testIndexedServerEntry() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();
        
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 10 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // test first()
        cursor.first();
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );

        // test last()
        cursor.last();
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );

        // test beforeFirst()
        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );

        // test afterLast()
        cursor.afterLast();
        assertFalse( cursor.available() );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );

        // test before()
        IndexEntry<String, String> entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.CN_AT_OID );
        cursor.before( entry );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );

        // test after()
        entry = new IndexEntry<String, String>();
        cursor.after( entry );
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        assertEquals( NORMALIZED_CN_OID, cursor.get().getKey() );
        cursor.close();

        node = new PresenceNode( schemaManager.getAttributeType( "ou" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        cursor = new PresenceCursor( txn, store, evaluator );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 2 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 3 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 4 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 5 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 6 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 7 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 8 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 9 ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 11 ), cursor.get().getId() );
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
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( oid ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        List<String> ids = new ArrayList<String>();

        while ( cursor.next() && cursor.available() )
        {
            ids.add( cursor.get().getId() );
        }

        assertEquals( 11, ids.size() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
    }


    @Test
    public void testNonIndexedServerEntry() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        PresenceCursor cursor = new PresenceCursor( txn, store, evaluator );

        assertEquals( node, evaluator.getExpression() );

        cursor.beforeFirst();

        Set<String> set = new HashSet<String>();

        while ( cursor.next() )
        {
            assertTrue( cursor.available() );
            assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getKey() );
            set.add( cursor.get().getId() );
        }

        assertEquals( 3, set.size() );
        assertTrue( set.contains( Strings.getUUID( 5L ) ) );
        assertTrue( set.contains( Strings.getUUID( 6L ) ) );
        assertTrue( set.contains( Strings.getUUID( 8L ) ) );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // test first()
        cursor.first();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getKey() );

        // test last()
        cursor.last();
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getKey() );

        // test beforeFirst()
        cursor.beforeFirst();
        assertFalse( cursor.available() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getKey() );

        // test afterLast()
        set.clear();
        cursor.afterLast();
        assertFalse( cursor.available() );

        while ( cursor.previous() )
        {
            assertTrue( cursor.available() );
            assertEquals( SchemaConstants.SN_AT_OID, cursor.get().getKey() );
            set.add( cursor.get().getId() );
        }

        assertEquals( 3, set.size() );
        assertTrue( set.contains( Strings.getUUID( 5L ) ) );
        assertTrue( set.contains( Strings.getUUID( 6L ) ) );
        assertTrue( set.contains( Strings.getUUID( 8L ) ) );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.close();

        // ----------- organizationName attribute

        node = new PresenceNode( schemaManager.getAttributeType( "o" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        cursor = new PresenceCursor( txn, store, evaluator );

        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        assertEquals( Strings.getUUID( 1 ), cursor.get().getId() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        assertFalse( cursor.isClosed() );
        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testEvaluatorIndexed() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        IndexEntry<String, String> entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.CN_AT_OID );
        entry.setId( Strings.getUUID( 3L ) );
        assertFalse( evaluator.evaluate( txn, entry ) );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.CN_AT_OID );
        entry.setId( Strings.getUUID( 5 ) );
        assertTrue( evaluator.evaluate( txn, entry ) );
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
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( oid ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );

        IndexEntry<String, String> entry = new IndexEntry<String, String>();
        // no need to set a value or id, because the evaluator must always evaluate to true
        // as each entry contains an objectClass, entryUUID, and entryCSN attribute
        assertFalse( evaluator.evaluate( txn, entry ) );

        entry = new IndexEntry<String, String>();
        entry.setKey( oid );
        entry.setId( Strings.getUUID( 5 ) );
        assertTrue( evaluator.evaluate( txn, entry ) );
    }


    @Test
    public void testEvaluatorNotIndexed() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "name" ) );
        PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
        IndexEntry<String, String> entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.NAME_AT_OID );
        entry.setId( Strings.getUUID( 3 ) );
        assertTrue( evaluator.evaluate( txn, entry ) );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.NAME_AT_OID );
        entry.setId( Strings.getUUID( 5 ) );
        assertTrue( evaluator.evaluate( txn, entry ) );

        node = new PresenceNode( schemaManager.getAttributeType( "searchGuide" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( Strings.getUUID( 3 ) );
        assertFalse( evaluator.evaluate( txn, entry ) );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.SEARCHGUIDE_AT_OID );
        entry.setId( Strings.getUUID( 5 ) );
        entry.setEntry( store.fetch( txn, Strings.getUUID( 5 ) ) );
        assertFalse( evaluator.evaluate( txn, entry ) );

        node = new PresenceNode( schemaManager.getAttributeType( "st" ) );
        evaluator = new PresenceEvaluator( node, store, schemaManager );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.ST_AT_OID );
        entry.setId( Strings.getUUID( 3 ) );
        assertFalse( evaluator.evaluate( txn, entry ) );
        entry = new IndexEntry<String, String>();
        entry.setKey( SchemaConstants.ST_AT_OID );
        entry.setId( Strings.getUUID( 5 ) );
        entry.setEntry( store.fetch( txn, Strings.getUUID( 5 ) ) );
        assertFalse( evaluator.evaluate( txn, entry ) );
    }


    @Test
    public void testInvalidCursorPositionException() throws Exception
    {
        assertThrows( InvalidCursorPositionException.class, () ->
        {
            PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
            PresenceCursor cursor = null;
    
            try
            {
                PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
                PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
                cursor = new PresenceCursor( txn, store, evaluator );
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
            PresenceCursor cursor = null;
    
            try
            {
                PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "cn" ) );
                PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
                cursor = new PresenceCursor( txn, store, evaluator );
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
            PresenceCursor cursor = null;
    
            try
            {
                PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
                PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
                cursor = new PresenceCursor( txn, store, evaluator );
    
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
            PresenceCursor cursor = null;
    
            try
            {
                PresenceNode node = new PresenceNode( schemaManager.getAttributeType( "sn" ) );
                PresenceEvaluator evaluator = new PresenceEvaluator( node, store, schemaManager );
                cursor = new PresenceCursor( txn, store, evaluator );
    
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
