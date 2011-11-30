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
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.XdbmStoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ScopeNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
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
 * Tests to for OneLevelScopeEvaluator and OneLevelScopeCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OneLevelScopeTest
{
    public static final Logger LOG = LoggerFactory.getLogger( OneLevelScopeTest.class );

    File wkdir;
    Partition store;
    static SchemaManager schemaManager = null;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = OneLevelScopeTest.class.getResource( "" ).getPath();
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
        TxnManagerFactory.init( logDir.getPath(), 1 << 13, 1 << 14 );
        OperationExecutionManagerFactory.init();

        // initialize the store
        store = new AvlPartition( schemaManager );
        store.setId( "example" );
        ( ( Store )store).setCacheSize( 10 );
        ( ( Store )store).setPartitionPath( wkdir.toURI() );
        ( ( Store )store).setSyncOnWrite( true );

        ( ( Store )store).addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID ) );
        ( ( Store )store).addIndex( new AvlIndex<String>( SchemaConstants.CN_AT_OID ) );
        store.setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        store.initialize();

        store.initialize();

        XdbmStoreUtils.loadExampleData( store, schemaManager );
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
    public void testCursorNoDeref() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<UUID> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );


        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );


        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 5 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 2 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorNoDerefReturnAliases() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=engineering," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );
        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<UUID> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );
        
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 11 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 4 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, new Dn( SchemaConstants.OU_AT_OID
            + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<UUID> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing2() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, new Dn( SchemaConstants.OU_AT_OID
            + "=apache," + SchemaConstants.OU_AT_OID + "=board of directors," + SchemaConstants.O_AT_OID
            + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<UUID> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing3() throws Exception
    {
        Dn dn = new Dn( schemaManager, SchemaConstants.CN_AT_OID + "=jd," + SchemaConstants.OU_AT_OID + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );

        Entry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "alias", "extensibleObject" );
        entry.add( "cn", "jd" );
        entry.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 12 ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        OperationExecutionManagerFactory.instance().add( store, addContext );

        dn = new Dn( schemaManager, SchemaConstants.CN_AT_OID + "=jdoe," + SchemaConstants.OU_AT_OID + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );

        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "person" );
        entry.add( "cn", "jdoe" );
        entry.add( "sn", "doe" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", Strings.getUUIDString( 13 ).toString() );
        
        addContext = new AddOperationContext( null, entry );
        OperationExecutionManagerFactory.instance().add( store, addContext );

        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, new Dn( SchemaConstants.OU_AT_OID
            + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<UUID> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );
        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );
        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );
        
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 7 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 13 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 6 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( Strings.getUUIDString( 8 ), indexEntry.getId() );
        assertEquals( Strings.getUUIDString( 3 ), indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testEvaluatorNoDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );

        ForwardIndexEntry<UUID> indexEntry = new ForwardIndexEntry<UUID>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_ALWAYS, new Dn( SchemaConstants.OU_AT_OID
            + "=engineering," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        assertEquals( node, evaluator.getExpression() );

        /*
         * Although immediately subordinate to the base, the OneLevelEvaluator
         * will not accept an alias candidate because aliases are not returned
         * when alias dereferencing while searching is enabled.
         */
        ForwardIndexEntry<UUID> indexEntry = new ForwardIndexEntry<UUID>();
        indexEntry.setId( Strings.getUUIDString( 11 ));
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<UUID>();
        indexEntry.setId( Strings.getUUIDString( 8 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<UUID>();
        indexEntry.setId( Strings.getUUIDString( 5 ) );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<UUID>();
        indexEntry.setId( Strings.getUUIDString( 6 ) );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<UUID> entry = new ForwardIndexEntry<UUID>();
        entry.setValue( Strings.getUUIDString( 3 ) );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.ONELEVEL );
        OneLevelScopeEvaluator evaluator = new OneLevelScopeEvaluator( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test after()
        ForwardIndexEntry<UUID> entry = new ForwardIndexEntry<UUID>();
        entry.setValue( Strings.getUUIDString( 3 ) );
        cursor.after( entry );
    }


    @Test(expected = IllegalStateException.class)
    public void testIllegalStateBadScope() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID + "=good times co." ), SearchScope.SUBTREE );
        new OneLevelScopeEvaluator( store, node );
    }
}