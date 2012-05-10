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
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
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
    Store<Entry, Long> store;
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
        store.setSyncOnWrite( true );

        store.addIndex( new AvlIndex<String, Entry>( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex<String, Entry>( SchemaConstants.CN_AT_OID ) );
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
    public void testCursorNoDeref() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        
        cursor.close();

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        
        try
        {
            cursor.afterLast();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }
        
        /*
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        
        try
        {
            cursor.last();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }

        /*
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        
        try
        {
            cursor.previous();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }
        
        /*
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 5L, ( long ) indexEntry.getId() );
        assertEquals( 2L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testCursorNoDerefReturnAliases() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=engineering," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        
        cursor.close();

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        
        try
        {
            cursor.afterLast();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }

        /*
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        
        try
        {
            cursor.last();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }

        /*
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        
        try
        {
            cursor.previous();
            fail();
        }
        catch ( UnsupportedOperationException uoe )
        {
            // expected
            cursor.close();
        }

        /*
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 11L, ( long ) indexEntry.getId() );
        assertEquals( 4L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testCursorWithDereferencing() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=board of directors," + SchemaConstants.O_AT_OID + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.DEREF_IN_SEARCHING, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
        
        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */
    }


    @Test
    public void testCursorWithDereferencing2() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=apache," + SchemaConstants.OU_AT_OID 
            + "=board of directors," 
            + SchemaConstants.O_AT_OID
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.DEREF_IN_SEARCHING, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 7L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */
    }


    @Test
    public void testCursorWithDereferencing3() throws Exception
    {
        Dn dn = new Dn( schemaManager, SchemaConstants.CN_AT_OID + "=jd," + SchemaConstants.OU_AT_OID
            + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );

        Entry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "alias", "extensibleObject" );
        entry.add( "cn", "jd" );
        entry.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        ( ( Partition ) store ).add( addContext );

        dn = new Dn( schemaManager, SchemaConstants.CN_AT_OID + "=jdoe," + SchemaConstants.OU_AT_OID
            + "=board of directors,"
            + SchemaConstants.O_AT_OID + "=good times co." );

        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "person" );
        entry.add( "cn", "jdoe" );
        entry.add( "sn", "doe" );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", UUID.randomUUID().toString() );

        addContext = new AddOperationContext( null, entry );
        ( ( Partition ) store ).add( addContext );

        dn = new Dn( SchemaConstants.OU_AT_OID
            + "=board of directors," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.DEREF_IN_SEARCHING, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        OneLevelScopeCursor<Long> cursor = new OneLevelScopeCursor<Long>( store, evaluator );

        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long, Long> indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test first() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.first();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();

        // --------- Test afterLast() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test last() ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.last();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test previous() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        
        cursor.previous();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );
        cursor.close();

        /*
        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );
        */

        // --------- Test next() before positioning ---------

        cursor = new OneLevelScopeCursor<Long>( store, evaluator );
        assertFalse( cursor.available() );
        cursor.next();

        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 7L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 13L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 6L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        indexEntry = cursor.get();
        assertNotNull( indexEntry );
        assertEquals( 8L, ( long ) indexEntry.getId() );
        assertEquals( 3L, ( long ) indexEntry.getKey() );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );
        cursor.close();
    }


    @Test
    public void testEvaluatorNoDereferencing() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );

        ForwardIndexEntry<Long, Long> indexEntry = new ForwardIndexEntry<Long, Long>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDereferencing() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=engineering," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );
        
        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.DEREF_ALWAYS, dn, baseId, SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
            node );
        assertEquals( node, evaluator.getExpression() );

        /*
         * Although immediately subordinate to the base, the OneLevelEvaluator
         * will not accept an alias candidate because aliases are not returned
         * when alias dereferencing while searching is enabled.
         */
        ForwardIndexEntry<Long, Long> indexEntry = new ForwardIndexEntry<Long, Long>();
        indexEntry.setId( 11L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, Long>();
        indexEntry.setId( 8L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, Long>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long, Long>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test(expected = InvalidCursorPositionException.class)
    public void testInvalidCursorPositionException() throws Exception
    {
        OneLevelScopeCursor<Long> cursor = null;
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        try
        {
            ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
            OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
                node );
            cursor = new OneLevelScopeCursor<Long>( store, evaluator );
            cursor.get();
        }
        finally
        {
            cursor.close();
        }
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        OneLevelScopeCursor<Long> cursor = null;
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        try
        {
            ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
            OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
                node );
            cursor = new OneLevelScopeCursor<Long>( store, evaluator );
    
            // test before()
            ForwardIndexEntry<Long, Long> entry = new ForwardIndexEntry<Long, Long>();
            entry.setKey( 3L );
            cursor.before( entry );
        }
        finally
        {
            cursor.close();
        }
    }


    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        OneLevelScopeCursor<Long> cursor = null;
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        try
        {
            ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.ONELEVEL );
            OneLevelScopeEvaluator<Entry, Long> evaluator = new OneLevelScopeEvaluator<Entry, Long>( store,
                node );
            cursor = new OneLevelScopeCursor<Long>( store, evaluator );
    
            // test after()
            ForwardIndexEntry<Long, Long> entry = new ForwardIndexEntry<Long, Long>();
            entry.setKey( 3L );
            cursor.after( entry );
        }
        finally
        {
            cursor.close();
        }
    }


    @Test(expected = IllegalStateException.class)
    public void testIllegalStateBadScope() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_AT_OID
            + "=sales," 
            + SchemaConstants.O_AT_OID 
            + "=good times co." );
        long baseId = store.getEntryId( dn );

        ScopeNode<Long> node = new ScopeNode<Long>( AliasDerefMode.NEVER_DEREF_ALIASES, dn, baseId, SearchScope.SUBTREE );
        new OneLevelScopeEvaluator<Entry, Long>( store, node );
    }
}