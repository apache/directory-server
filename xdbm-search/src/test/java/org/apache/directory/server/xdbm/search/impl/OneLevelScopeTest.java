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
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CollectiveSchema;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;


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
    Store<ServerEntry> store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;


    public OneLevelScopeTest() throws Exception
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
        store.setSyncOnWrite( true );

        store.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.CN_AT_OID ) );
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
    public void testCursorNoDeref() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        assertTrue( cursor.isElementReused() );


        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long,ServerEntry> indexEntry = cursor.get();
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
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=engineering," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        assertTrue( cursor.isElementReused() );


        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long,ServerEntry> indexEntry = cursor.get();
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
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING,
            SchemaConstants.OU_AT_OID + "=board of directors," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        assertTrue( cursor.isElementReused() );


        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long,ServerEntry> indexEntry = cursor.get();
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
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING,
            SchemaConstants.OU_AT_OID + "=apache," +
            SchemaConstants.OU_AT_OID + "=board of directors," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        assertTrue( cursor.isElementReused() );


        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long,ServerEntry> indexEntry = cursor.get();
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
        LdapDN dn = new LdapDN(
            SchemaConstants.CN_AT_OID + "=jd," +
            SchemaConstants.OU_AT_OID + "=board of directors," +
            SchemaConstants.O_AT_OID  + "=good times co."
        );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        
        ServerEntry attrs = new DefaultServerEntry( registries, dn );
        attrs.add( "objectClass", "alias", "extensibleObject" );
        attrs.add( "cn", "jd" );
        attrs.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        store.add( attrs );

        dn = new LdapDN(
            SchemaConstants.CN_AT_OID + "=jdoe," +
            SchemaConstants.OU_AT_OID + "=board of directors," +
            SchemaConstants.O_AT_OID  + "=good times co."
        );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        
        attrs = new DefaultServerEntry( registries, dn );
        attrs.add( "objectClass", "person" );
        attrs.add( "cn", "jdoe" );
        attrs.add( "sn", "doe" );
        attrs.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        attrs.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        store.add( attrs );

        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_IN_SEARCHING,
            SchemaConstants.OU_AT_OID + "=board of directors," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        assertTrue( cursor.isElementReused() );


        // --------- Test beforeFirst() ---------

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        IndexEntry<Long,ServerEntry> indexEntry = cursor.get();
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
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );

        ForwardIndexEntry<Long,ServerEntry> indexEntry = new ForwardIndexEntry<Long,ServerEntry>();
        indexEntry.setId( 6L );
        assertTrue( evaluator.evaluate( indexEntry ) );
    }


    @Test
    public void testEvaluatorWithDereferencing() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.DEREF_ALWAYS,
            SchemaConstants.OU_AT_OID + "=engineering," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        assertEquals( node, evaluator.getExpression() );

        /*
         * Although immediately subordinate to the base, the OneLevelEvaluator
         * will not accept an alias candidate because aliases are not returned
         * when alias dereferencing while searching is enabled.
         */
        ForwardIndexEntry<Long,ServerEntry> indexEntry = new ForwardIndexEntry<Long,ServerEntry>();
        indexEntry.setId( 11L );
        assertFalse( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long,ServerEntry>();
        indexEntry.setId( 8L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long,ServerEntry>();
        indexEntry.setId( 5L );
        assertTrue( evaluator.evaluate( indexEntry ) );

        indexEntry = new ForwardIndexEntry<Long,ServerEntry>();
        indexEntry.setId( 6L );
        assertFalse( evaluator.evaluate( indexEntry ) );
    }


    @Test ( expected = InvalidCursorPositionException.class )
    public void testInvalidCursorPositionException() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );
        cursor.get();
    }


    @Test ( expected = UnsupportedOperationException.class )
    public void testUnsupportBeforeWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test before()
        ForwardIndexEntry<Long,ServerEntry> entry = new ForwardIndexEntry<Long,ServerEntry>();
        entry.setValue( 3L );
        cursor.before( entry );
    }


    @Test ( expected = UnsupportedOperationException.class )
    public void testUnsupportAfterWithoutIndex() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.ONELEVEL );
        OneLevelScopeEvaluator<ServerEntry> evaluator = new OneLevelScopeEvaluator<ServerEntry>( store, node );
        OneLevelScopeCursor cursor = new OneLevelScopeCursor( store, evaluator );

        // test after()
        ForwardIndexEntry<Long,ServerEntry> entry = new ForwardIndexEntry<Long,ServerEntry>();
        entry.setValue( 3L );
        cursor.after( entry );
    }


    @Test ( expected = IllegalStateException.class )
    public void testIllegalStateBadScope() throws Exception
    {
        ScopeNode node = new ScopeNode( AliasDerefMode.NEVER_DEREF_ALIASES,
            SchemaConstants.OU_AT_OID + "=sales," +
            SchemaConstants.O_AT_OID  + "=good times co.", SearchScope.SUBTREE );
        new OneLevelScopeEvaluator<ServerEntry>( store, node );
    }
}