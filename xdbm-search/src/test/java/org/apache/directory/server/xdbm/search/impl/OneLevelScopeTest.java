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
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.DN;
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
 * Tests to for OneLevelScopeEvaluator and OneLevelScopeCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class OneLevelScopeTest
{
    public static final Logger LOG = LoggerFactory.getLogger( OneLevelScopeTest.class );

    File wkdir;
    Store<ServerEntry, Long> store;
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
        store.setSyncOnWrite( true );

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
    public void testCursorNoDeref() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, ServerEntry, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

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
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorNoDerefReturnAliases() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=engineering,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, ServerEntry, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

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
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, SchemaConstants.OU_AT_OID
            + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, ServerEntry, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

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
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing2() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, SchemaConstants.OU_AT_OID + "=apache,"
            + SchemaConstants.OU_AT_OID + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co.",
            SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, ServerEntry, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getValue() );

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
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testCursorWithDereferencing3() throws Exception
    {
        DN dn = new DN( SchemaConstants.CN_AT_OID + "=jd," + SchemaConstants.OU_AT_OID + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );
        dn.normalize( schemaManager.getNormalizerMapping() );

        ServerEntry attrs = new DefaultServerEntry( schemaManager, dn );
        attrs.add( "objectClass", "alias", "extensibleObject" );
        attrs.add( "cn", "jd" );
        attrs.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", UUID.randomUUID().toString() );
        store.add( attrs );

        dn = new DN( SchemaConstants.CN_AT_OID + "=jdoe," + SchemaConstants.OU_AT_OID + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );
        dn.normalize( schemaManager.getNormalizerMapping() );

        attrs = new DefaultServerEntry( schemaManager, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "cn", "jdoe" );
        attrs.add( "sn", "doe" );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", UUID.randomUUID().toString() );
        store.add( attrs );

        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING, SchemaConstants.OU_AT_OID
            + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        assertTrue( cursor.isElementReused() );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, ServerEntry, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

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
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getValue() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
    }


    @Test
    public void testEvaluatorNoDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );

        ForwardIndexEntry<Long, ServerEntry, Long> indexEntry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_ALWAYS, SchemaConstants.OU_AT_OID + "=engineering,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        assertEquals( node, evaluator.getExpression() );

        /*
         * Although immediately subordinate to the base, the OneLevelEvaluator
         * will not accept an alias candidate because aliases are not returned
         * when alias dereferencing while searching is enabled.
         */
        ForwardIndexEntry<Long, ServerEntry, Long> indexEntry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        indexEntry.setId( 11L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        indexEntry.setId( 8L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.get();
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<Long, ServerEntry, Long> entry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        entry.setValue( 3L );
        cursor.before( entry );
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry, Long> evaluator = new OneLevelScopeEvaluator<ServerEntry, Long>( store,
            node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test after()
        ForwardIndexEntry<Long, ServerEntry, Long> entry = new ForwardIndexEntry<Long, ServerEntry, Long>();
        entry.setValue( 3L );
        cursor.after( entry );
    }


    @Test(expected = IllegalStateException.class)
    public void testIllegalStateBadScope() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES, SchemaConstants.OU_AT_OID + "=sales,"
            + SchemaConstants.O_AT_OID + "=good times co.", SearchScope.SUBTREE );
        new OneLevelScopeEvaluator<ServerEntry, Long>( store, node );
    }
}