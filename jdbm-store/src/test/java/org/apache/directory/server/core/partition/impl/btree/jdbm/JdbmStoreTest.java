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
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;

import javax.naming.directory.Attributes;
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
        store.setAliasIndex( new JdbmIndex<String,Attributes>( "alias" ) );
        assertNotNull( store.getAliasIndex() );

        assertEquals( JdbmStore.DEFAULT_CACHE_SIZE, store.getCacheSize() );
        store.setCacheSize( 24 );
        assertEquals( 24, store.getCacheSize() );

        assertNull( store.getContextEntry() );
        store.setContextEntry( new DefaultServerEntry( registries, new LdapDN() ) );
        assertNotNull( store.getContextEntry() );

        assertNull( store.getPresenceIndex() );
        store.setPresenceIndex( new JdbmIndex<String,Attributes>( "existence" ) );
        assertNotNull( store.getPresenceIndex() );

        assertNull( store.getOneLevelIndex() );
        store.setOneLevelIndex( new JdbmIndex<Long,Attributes>( "hierarchy" ) );
        assertNotNull( store.getOneLevelIndex() );

        assertNull( store.getName() );
        store.setName( "foo" );
        assertEquals( "foo", store.getName() );

        assertNull( store.getNdnIndex() );
        store.setNdnIndex( new JdbmIndex<String,Attributes>( "ndn" ) );
        assertNotNull( store.getNdnIndex() );

        assertNull( store.getOneAliasIndex() );
        store.setOneAliasIndex( new JdbmIndex<Long,Attributes>( "oneAlias" ) );
        assertNotNull( store.getNdnIndex() );

        assertNull( store.getSubAliasIndex() );
        store.setSubAliasIndex( new JdbmIndex<Long,Attributes>( "subAlias" ) );
        assertNotNull( store.getSubAliasIndex() );

        assertNull( store.getSuffixDn() );
        store.setSuffixDn( "dc=example,dc=com" );
        assertEquals( "dc=example,dc=com", store.getSuffixDn() );

        assertNull( store.getUpdnIndex() );
        store.setUpdnIndex( new JdbmIndex<String,Attributes>( "updn" ) );
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
        try { store.setAliasIndex( new JdbmIndex<String,Attributes>( "alias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertEquals( 10, store.getCacheSize() );
        try { store.setCacheSize( 24 ); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getContextEntry() );
        try { store.setContextEntry( new DefaultServerEntry( registries, new LdapDN() ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getPresenceIndex() );
        try { store.setPresenceIndex( new JdbmIndex<String,Attributes>( "existence" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getOneLevelIndex() );
        try { store.setOneLevelIndex( new JdbmIndex<Long,Attributes>( "hierarchy" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getName() );
        try { store.setName( "foo" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getNdnIndex() );
        try { store.setNdnIndex( new JdbmIndex<String,Attributes>( "ndn" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getOneAliasIndex() );
        try { store.setOneAliasIndex( new JdbmIndex<Long,Attributes>( "oneAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSubAliasIndex() );
        try { store.setSubAliasIndex( new JdbmIndex<Long,Attributes>( "subAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSuffixDn() );
        try { store.setSuffixDn( "dc=example,dc=com" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getUpdnIndex() );
        try { store.setUpdnIndex( new JdbmIndex<String,Attributes>( "updn" ) ); fail(); }
        catch( IllegalStateException e ) {}
        Iterator<String> systemIndices = store.systemIndices();
        for ( int ii = 0; ii < 8; ii++ )
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
    public void testGetIndicies() throws Exception
    {
        Attributes attrs = store.getIndices( 1L );
        assertNotNull( attrs );
        assertNotNull( attrs.get( "_nDn" ) );
        assertNotNull( attrs.get( "_upDn" ) );
        assertNotNull( attrs.get( "_parent" ) );
        LOG.debug( attrs.toString() );
    }


    @Test
    public void testPersistentProperties() throws Exception
    {
        assertNull( store.getProperty( "foo" ) );
        store.setProperty( "foo", "bar" );
        assertEquals( "bar", store.getProperty( "foo" ) );
    }


    @Test
    public void testFreshStore() throws Exception
    {
        LdapDN dn = new LdapDN( "dc=example,dc=com" );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        assertEquals( 1L, ( long ) store.getEntryId( dn.toNormName() ) );
        assertEquals( 1, store.count() );
        assertEquals( "dc=example,dc=com", store.getEntryUpdn( dn.toNormName() ) );
        assertEquals( dn.toNormName(), store.getEntryDn( 1L ) );
        assertEquals( dn.getUpName(), store.getEntryUpdn( 1L ) );
        assertNotNull( store.getSuffixEntry() );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( 0L, ( long ) store.getParentId( dn.toNormName() ) );
        assertNull( store.getParentId( 0L ) );

        // should not be allowed
        try { store.delete( 1L ); fail(); } catch( LdapOperationNotSupportedException e )
        {
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, e.getResultCode() );
        }
    }


    @Test
    public void testEntryOperations() throws Exception
    {
        assertEquals( 0, store.getChildCount( 1L ) );
        LdapDN dn = new LdapDN( "ou=Engineering,dc=example,dc=com" );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Engineering" );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        Cursor<IndexEntry<Long,Attributes>> cursor = store.list( 1L );
        assertNotNull( cursor );
        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertFalse( cursor.next() );
        assertEquals( 1, store.getChildCount( 1L ) );

        store.delete( 2L );
        assertEquals( 0, store.getChildCount( 1L ) );
        assertEquals( 1, store.count() );
    }
}
