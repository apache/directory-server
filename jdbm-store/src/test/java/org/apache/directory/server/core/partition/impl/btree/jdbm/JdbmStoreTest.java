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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.commons.io.FileUtils;
import org.apache.directory.server.schema.bootstrap.*;
import org.apache.directory.server.schema.registries.*;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.partition.impl.btree.IndexNotFoundException;
import org.apache.directory.server.constants.CoreSchemaConstants;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.constants.SchemaConstants;

import java.io.File;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Unit test cases for JdbmStore
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class JdbmStoreTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStoreTest.class.getSimpleName() );

    File wkdir;
    JdbmStore store;
    Registries registries = null;
    AttributeTypeRegistry attributeRegistry;


    public JdbmStoreTest() throws Exception
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
        store = new JdbmStore();
        store.setName( "example" );
        store.setCacheSize( 10 );
        store.setSuffixDn( "dc=example,dc=com" );
        store.setWorkingDirectory( wkdir );
        store.setSyncOnWrite( false );

        DefaultServerEntry contextEntry = new DefaultServerEntry( registries,
            new LdapDN( "dc=example,dc=com" ) );
        contextEntry.add( "objectClass", "domain" );
        contextEntry.add( "dc", "example" );
        store.setContextEntry( contextEntry );
        store.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new JdbmIndex( SchemaConstants.UID_AT_OID ) );
        store.init( registries.getOidRegistry(), attributeRegistry );
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
    public void testSimplePropertiesUnlocked() throws Exception
    {
        JdbmStore store = new JdbmStore();

        assertNull( store.getAliasIndex() );
        store.setAliasIndex( new JdbmIndex<String>( "alias" ) );
        assertNotNull( store.getAliasIndex() );

        assertEquals( JdbmStore.DEFAULT_CACHE_SIZE, store.getCacheSize() );
        store.setCacheSize( 24 );
        assertEquals( 24, store.getCacheSize() );

        assertNull( store.getContextEntry() );
        store.setContextEntry( new DefaultServerEntry( registries, new LdapDN() ) );
        assertNotNull( store.getContextEntry() );

        assertNull( store.getExistanceIndex() );
        store.setExistanceIndex( new JdbmIndex<String>( "existence" ) );
        assertNotNull( store.getExistanceIndex() );

        assertNull( store.getHierarchyIndex() );
        store.setHierarchyIndex( new JdbmIndex<Long>( "hierarchy" ) );
        assertNotNull( store.getHierarchyIndex() );

        assertNull( store.getName() );
        store.setName( "foo" );
        assertEquals( "foo", store.getName() );

        assertNull( store.getNdnIndex() );
        store.setNdnIndex( new JdbmIndex<String>( "ndn" ) );
        assertNotNull( store.getNdnIndex() );

        assertNull( store.getOneAliasIndex() );
        store.setOneAliasIndex( new JdbmIndex<Long>( "oneAlias" ) );
        assertNotNull( store.getNdnIndex() );

        assertNull( store.getSubAliasIndex() );
        store.setSubAliasIndex( new JdbmIndex<Long>( "subAlias" ) );
        assertNotNull( store.getSubAliasIndex() );

        assertNull( store.getSuffixDn() );
        store.setSuffixDn( "dc=example,dc=com" );
        assertEquals( "dc=example,dc=com", store.getSuffixDn() );

        assertNull( store.getUpdnIndex() );
        store.setUpdnIndex( new JdbmIndex<String>( "updn" ) );
        assertNotNull( store.getUpdnIndex() );

        assertNull( store.getUpSuffix() );
        assertNull( store.getSuffix() );

        assertEquals( 0, store.getUserIndices().size() );
        Set<JdbmIndex> set = new HashSet<JdbmIndex>();
        set.add( new JdbmIndex( "foo" ) );
        store.setUserIndices( set );
        assertEquals( set.size(), store.getUserIndices().size() );

        assertNull( store.getWorkingDirectory() );
        store.setWorkingDirectory( new File( "." ) );
        assertEquals( new File( "." ), store.getWorkingDirectory() );

        assertFalse( store.isInitialized() );
        assertTrue( store.isSyncOnWrite() );
        store.setSyncOnWrite( false );
        assertFalse( store.isSyncOnWrite() );

        store.sync();
        store.destroy();
    }


    @Test
    public void testSimplePropertiesLocked() throws Exception
    {
        assertNotNull( store.getAliasIndex() );
        try { store.setAliasIndex( new JdbmIndex<String>( "alias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertEquals( 10, store.getCacheSize() );
        try { store.setCacheSize( 24 ); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getContextEntry() );
        try { store.setContextEntry( new DefaultServerEntry( registries, new LdapDN() ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getExistanceIndex() );
        try { store.setExistanceIndex( new JdbmIndex<String>( "existence" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getHierarchyIndex() );
        try { store.setHierarchyIndex( new JdbmIndex<Long>( "hierarchy" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getName() );
        try { store.setName( "foo" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getNdnIndex() );
        try { store.setNdnIndex( new JdbmIndex<String>( "ndn" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getOneAliasIndex() );
        try { store.setOneAliasIndex( new JdbmIndex<Long>( "oneAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSubAliasIndex() );
        try { store.setSubAliasIndex( new JdbmIndex<Long>( "subAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSuffixDn() );
        try { store.setSuffixDn( "dc=example,dc=com" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getUpdnIndex() );
        try { store.setUpdnIndex( new JdbmIndex<String>( "updn" ) ); fail(); }
        catch( IllegalStateException e ) {}
        Iterator<String> systemIndices = store.systemIndices();
        for ( int ii = 0; ii < 7; ii++ )
        {
            assertTrue( systemIndices.hasNext() );
            assertNotNull( systemIndices.next() );
        }
        assertFalse( systemIndices.hasNext() );
        assertNotNull( store.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT ) );
        try { store.getSystemIndex( "bogus" ); fail(); }
        catch ( IndexNotFoundException e ) {}
        try { store.getSystemIndex( "dc" ); fail(); }
        catch ( IndexNotFoundException e ) {}

        assertNotNull( store.getUpSuffix() );
        assertNotNull( store.getSuffix() );

        assertEquals( 2, store.getUserIndices().size() );
        assertFalse( store.hasUserIndexOn( "dc" ) );
        assertTrue( store.hasUserIndexOn( SchemaConstants.OU_AT ) );
        assertTrue( store.hasSystemIndexOn( ApacheSchemaConstants.APACHE_ALIAS_AT ) );
        Iterator<String> userIndices = store.userIndices();
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertTrue( userIndices.hasNext() );        
        assertNotNull( userIndices.next() );
        assertFalse( userIndices.hasNext() );        
        assertNotNull( store.getUserIndex( SchemaConstants.OU_AT ) );
        try { store.getUserIndex( "bogus" ); fail(); }
        catch ( IndexNotFoundException e ) {}
        try { store.getUserIndex( "dc" ); fail(); }
        catch ( IndexNotFoundException e ) {}

        assertNotNull( store.getWorkingDirectory() );
        try { store.setWorkingDirectory( new File( "." ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertTrue( store.isInitialized() );
        assertFalse( store.isSyncOnWrite() );

        store.sync();
    }


    @Test
    public void testEmptyStore() throws Exception
    {
        LdapDN dn = new LdapDN( "dc=example,dc=com" );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        assertEquals( 1L, ( long ) store.getEntryId( dn.toNormName() ) );
    }
}
