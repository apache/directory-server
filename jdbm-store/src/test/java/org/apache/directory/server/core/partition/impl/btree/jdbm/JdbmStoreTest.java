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
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.tools.StoreUtils;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.name.Rdn;

import javax.naming.directory.Attributes;
import java.lang.reflect.Method;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;


/**
 * Unit test cases for JdbmStore
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
@SuppressWarnings("unchecked")
public class JdbmStoreTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStoreTest.class.getSimpleName() );

    File wkdir;
    JdbmStore<ServerEntry> store;
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
        destroyStore();

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
        store.addIndex( new JdbmIndex( SchemaConstants.UID_AT_OID ) );
        StoreUtils.loadExampleData( store, registries );
        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
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
        JdbmStore<Attributes> store = new JdbmStore<Attributes>();
        store.setSyncOnWrite( true ); // for code coverage
        
        assertNull( store.getAliasIndex() );
        store.setAliasIndex( new JdbmIndex<String,Attributes>( "alias" ) );
        assertNotNull( store.getAliasIndex() );

        assertEquals( JdbmStore.DEFAULT_CACHE_SIZE, store.getCacheSize() );
        store.setCacheSize( 24 );
        assertEquals( 24, store.getCacheSize() );

        assertNull( store.getPresenceIndex() );
        store.setPresenceIndex( new JdbmIndex<String,Attributes>( "existence" ) );
        assertNotNull( store.getPresenceIndex() );

        assertNull( store.getOneLevelIndex() );
        store.setOneLevelIndex( new JdbmIndex<Long,Attributes>( "hierarchy" ) );
        assertNotNull( store.getOneLevelIndex() );
        
        assertNull( store.getSubLevelIndex() );
        store.setSubLevelIndex( new JdbmIndex<Long,Attributes>( "sublevel" ) );
        assertNotNull( store.getSubLevelIndex() );

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
        Set<Index<?,Attributes>> set = new HashSet<Index<?,Attributes>>();
        set.add( new JdbmIndex<Object,Attributes>( "foo" ) );
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
        try { store.setAliasIndex( new JdbmIndex<String,ServerEntry>( "alias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertEquals( 10, store.getCacheSize() );
        try { store.setCacheSize( 24 ); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getPresenceIndex() );
        try { store.setPresenceIndex( new JdbmIndex<String,ServerEntry>( "existence" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getOneLevelIndex() );
        try { store.setOneLevelIndex( new JdbmIndex<Long,ServerEntry>( "hierarchy" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSubLevelIndex() );
        try { store.setSubLevelIndex( new JdbmIndex<Long,ServerEntry>( "sublevel" ) ); fail(); }
        catch( IllegalStateException e ) {}
        
        assertNotNull( store.getName() );
        try { store.setName( "foo" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getNdnIndex() );
        try { store.setNdnIndex( new JdbmIndex<String,ServerEntry>( "ndn" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getOneAliasIndex() );
        try { store.setOneAliasIndex( new JdbmIndex<Long,ServerEntry>( "oneAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSubAliasIndex() );
        try { store.setSubAliasIndex( new JdbmIndex<Long,ServerEntry>( "subAlias" ) ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getSuffixDn() );
        try { store.setSuffixDn( "dc=example,dc=com" ); fail(); }
        catch( IllegalStateException e ) {}

        assertNotNull( store.getUpdnIndex() );
        try { store.setUpdnIndex( new JdbmIndex<String,ServerEntry>( "updn" ) ); fail(); }
        catch( IllegalStateException e ) {}
        Iterator<String> systemIndices = store.systemIndices();
        
        for ( int ii = 0; ii < 11; ii++ )
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
    public void testPersistentProperties() throws Exception
    {
        assertNull( store.getProperty( "foo" ) );
        store.setProperty( "foo", "bar" );
        assertEquals( "bar", store.getProperty( "foo" ) );
    }


    @Test
    public void testFreshStore() throws Exception
    {
        LdapDN dn = new LdapDN( "o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        assertEquals( 1L, ( long ) store.getEntryId( dn.toNormName() ) );
        assertEquals( 11, store.count() );
        assertEquals( "o=Good Times Co.", store.getEntryUpdn( dn.toNormName() ) );
        assertEquals( dn.toNormName(), store.getEntryDn( 1L ) );
        assertEquals( dn.getUpName(), store.getEntryUpdn( 1L ) );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( 0L, ( long ) store.getParentId( dn.toNormName() ) );
        assertNull( store.getParentId( 0L ) );

        // should NOW be allowed
        store.delete( 1L ); 
    }


    @Test
    public void testEntryOperations() throws Exception
    {
        assertEquals( 3, store.getChildCount( 1L ) );

        Cursor<IndexEntry<Long,ServerEntry>> cursor = store.list( 1L );
        assertNotNull( cursor );
        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertEquals( 2L, ( long ) cursor.get().getId() );
        assertTrue( cursor.next() );
        assertEquals( 3, store.getChildCount( 1L ) );
        
        store.delete( 2L );
        assertEquals( 2, store.getChildCount( 1L ) );
        assertEquals( 10, store.count() );
        
        // add an alias and delete to test dropAliasIndices method
        LdapDN dn = new LdapDN( "commonName=Jack Daniels,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Apache" );
        entry.add( "commonName",  "Jack Daniels");
        entry.add( "aliasedObjectName", "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        store.add( entry );
        
        store.delete( 12L ); // drops the alias indices
        
    }
    

    @Test
    public void testSubLevelIndex() throws Exception
    {
      Index idx = store.getSubLevelIndex();
      
      assertEquals( 19, idx.count() );
      
      Cursor<IndexEntry<Long,Attributes>> cursor = idx.forwardCursor( 2L );
      
      assertTrue( cursor.next() );
      assertEquals( 2, ( long ) cursor.get().getId() );
      
      assertTrue( cursor.next() );
      assertEquals( 5, ( long ) cursor.get().getId() );
      
      assertTrue( cursor.next() );
      assertEquals( 6, ( long ) cursor.get().getId() );

      assertFalse( cursor.next() );
      
      idx.drop( 5L );
      
      cursor = idx.forwardCursor( 2L );

      assertTrue( cursor.next() );
      assertEquals( 2, ( long ) cursor.get().getId() );
      
      assertTrue( cursor.next() );
      assertEquals( 6, ( long ) cursor.get().getId() );
      
      assertFalse( cursor.next() );
      
      // dn id 12
      LdapDN martinDn = new LdapDN( "cn=Marting King,ou=Sales,o=Good Times Co." );
      martinDn.normalize( attributeRegistry.getNormalizerMapping() );
      DefaultServerEntry entry = new DefaultServerEntry( registries, martinDn );
      entry.add( "objectClass", "top", "person", "organizationalPerson" );
      entry.add( "ou", "Sales" );
      entry.add( "cn",  "Martin King");
      entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
      entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
      store.add( entry );
      
      cursor = idx.forwardCursor( 2L);
      cursor.afterLast();
      assertTrue( cursor.previous() );
      assertEquals( 12, ( long ) cursor.get().getId() );
      
      LdapDN newParentDn = new LdapDN( "ou=Board of Directors,o=Good Times Co." );
      newParentDn.normalize( attributeRegistry.getNormalizerMapping() );
      
      store.move( martinDn, newParentDn );
      cursor = idx.forwardCursor( 3L);
      cursor.afterLast();
      assertTrue( cursor.previous() );
      assertEquals( 12, ( long ) cursor.get().getId() );
      
      // dn id 13
      LdapDN marketingDn = new LdapDN( "ou=Marketing,ou=Sales,o=Good Times Co." );
      marketingDn.normalize( attributeRegistry.getNormalizerMapping() );
      entry = new DefaultServerEntry( registries, marketingDn );
      entry.add( "objectClass", "top", "organizationalUnit" );
      entry.add( "ou", "Marketing" );
      entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
      entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
      store.add( entry );

      // dn id 14
      LdapDN jimmyDn = new LdapDN( "cn=Jimmy Wales,ou=Marketing, ou=Sales,o=Good Times Co." );
      jimmyDn.normalize( attributeRegistry.getNormalizerMapping() );
      entry = new DefaultServerEntry( registries, jimmyDn );
      entry.add( "objectClass", "top", "person", "organizationalPerson" );
      entry.add( "ou", "Marketing" );
      entry.add( "cn",  "Jimmy Wales");
      entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
      entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
      store.add( entry );
      
      store.move( marketingDn, newParentDn );

      cursor = idx.forwardCursor( 3L);
      cursor.afterLast();

      assertTrue( cursor.previous() );
      assertEquals( 14, ( long ) cursor.get().getId() );
      
      assertTrue( cursor.previous() );
      assertEquals( 13, ( long ) cursor.get().getId() );

      assertTrue( cursor.previous() );
      assertEquals( 12, ( long ) cursor.get().getId() );

      assertTrue( cursor.previous() );
      assertEquals( 10, ( long ) cursor.get().getId() );

      assertTrue( cursor.previous() );
      assertEquals( 9, ( long ) cursor.get().getId() );

      assertTrue( cursor.previous() );
      assertEquals( 7, ( long ) cursor.get().getId() );

      assertTrue( cursor.previous() );
      assertEquals( 3, ( long ) cursor.get().getId() );
      
      assertFalse( cursor.previous() );
    }
   
    
    @Test
    public void testConvertIndex() throws Exception
    {
        Index nonJdbmIndex = new Index()
        {

            public void add( Object attrVal, Long id ) throws Exception { }

            public void close() throws Exception { }

            public int count() throws Exception
            {
                return 0;
            }

            public int count( Object attrVal ) throws Exception
            {
                return 0;
            }

            public void drop( Long id ) throws Exception { }

            public void drop( Object attrVal, Long id ) throws Exception { }

            public IndexCursor forwardCursor() throws Exception
            {
                return null;
            }

            public IndexCursor forwardCursor( Object key ) throws Exception
            {
                return null;
            }

            public Long forwardLookup( Object attrVal ) throws Exception
            {
                return null;
            }

            public Cursor forwardValueCursor( Object key ) throws Exception
            {
                return null;
            }


            public boolean forward( Object attrVal ) throws Exception
            {
                return false;
            }


            public boolean forward( Object attrVal, Long id ) throws Exception
            {
                return false;
            }


            public boolean reverse( Long id ) throws Exception
            {
                return false;
            }


            public boolean reverse( Long id, Object attrVal ) throws Exception
            {
                return false;
            }


            public boolean forwardGreaterOrEq( Object attrVal ) throws Exception
            {
                return false;
            }


            public boolean forwardGreaterOrEq( Object attrVal, Long id ) throws Exception
            {
                return false;
            }


            public boolean reverseGreaterOrEq( Long id ) throws Exception
            {
                return false;
            }


            public boolean reverseGreaterOrEq( Long id, Object attrVal ) throws Exception
            {
                return false;
            }


            public boolean forwardLessOrEq( Object attrVal ) throws Exception
            {
                return false;
            }


            public boolean forwardLessOrEq( Object attrVal, Long id ) throws Exception
            {
                return false;
            }


            public boolean reverseLessOrEq( Long id ) throws Exception
            {
                return false;
            }


            public boolean reverseLessOrEq( Long id, Object attrVal ) throws Exception
            {
                return false;
            }


            public AttributeType getAttribute()
            {
                return null;
            }

            public String getAttributeId()
            {
                return "ou";
            }

            public int getCacheSize()
            {
                return 10;
            }

            public Object getNormalized( Object attrVal ) throws Exception
            {
                return null;
            }

            public File getWkDirPath()
            {
                return new File(".");
            }

            public int greaterThanCount( Object attrVal ) throws Exception
            {
                return 0;
            }

            public boolean isCountExact()
            {
                return false;
            }

            public int lessThanCount( Object attrVal ) throws Exception
            {
                return 0;
            }

            public IndexCursor reverseCursor() throws Exception
            {
                return null;
            }

            public IndexCursor reverseCursor( Long id ) throws Exception
            {
                return null;
            }

            public Object reverseLookup( Long id ) throws Exception
            {
                return null;
            }

            public Cursor reverseValueCursor( Long id ) throws Exception
            {
                return null;
            }

            public void setAttributeId( String attributeId ) { }

            public void setCacheSize( int cacheSize ) { }

            public void setWkDirPath( File wkDirPath ) { }

            public void sync() throws Exception { }
            
        };
        
        Method convertIndex = store.getClass().getDeclaredMethod( "convertIndex", Index.class );
        convertIndex.setAccessible( true );
        Object obj = convertIndex.invoke( store, nonJdbmIndex );
        
        assertNotNull( obj );
        assertEquals( JdbmIndex.class, obj.getClass() );
    }
    
    
    @Test( expected = LdapNameNotFoundException.class )
    public void testAddWithoutParentId() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=Marting King,ou=Not Present,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Not Present" );
        entry.add( "cn",  "Martin King");
        store.add( entry );
    }
    
    
    @Test( expected = LdapSchemaViolationException.class )
    public void testAddWithoutObjectClass() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=Martin King,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "ou", "Sales" );
        entry.add( "cn",  "Martin King");
        store.add( entry );
    }
        
    
    @Test
    public void testModifyAddOUAttrib() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );

        List<Modification> mods = new ArrayList<Modification>();
        ServerAttribute attrib = new DefaultServerAttribute( SchemaConstants.OU_AT,
            attributeRegistry.lookup( SchemaConstants.OU_AT_OID ) );
        attrib.add( "Engineering" );
        
        Modification add = new ServerModification( ModificationOperation.ADD_ATTRIBUTE, attrib );
        
        mods.add( add );
        
        store.modify( dn, mods );
    }
    
    
    @Test
    public void testRename() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=Pivate Ryan,ou=Engineering,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn",  "Private Ryan");
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );

        store.add( entry );
        
        Rdn rdn = new Rdn("sn=James");
        
        store.rename( dn, rdn, true );
    }
    
    
    @Test
    public void testMove() throws Exception
    {
        LdapDN childDn = new LdapDN( "cn=Pivate Ryan,ou=Engineering,o=Good Times Co." );
        childDn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry childEntry = new DefaultServerEntry( registries, childDn );
        childEntry.add( "objectClass", "top", "person", "organizationalPerson" );
        childEntry.add( "ou", "Engineering" );
        childEntry.add( "cn",  "Private Ryan");
        childEntry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        childEntry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );

        store.add( childEntry );

        LdapDN parentDn = new LdapDN( "ou=Sales,o=Good Times Co." );
        parentDn.normalize( attributeRegistry.getNormalizerMapping() );

        Rdn rdn = new Rdn("cn=Ryan");

        store.move( childDn, parentDn, rdn, true );

        // to drop the alias indices   
        childDn = new LdapDN( "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        childDn.normalize( attributeRegistry.getNormalizerMapping() );
        
        parentDn = new LdapDN( "ou=Engineering,o=Good Times Co." );
        parentDn.normalize( attributeRegistry.getNormalizerMapping() );
        
        assertEquals( 3, store.getSubAliasIndex().count() );
        
        store.move( childDn, parentDn);
        
        assertEquals( 4, store.getSubAliasIndex().count() );
    }
    
    
    @Test
    public void testModifyAdd() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );

        List<Modification> mods = new ArrayList<Modification>();
        ServerAttribute attrib = new DefaultServerAttribute( SchemaConstants.SURNAME_AT,
            attributeRegistry.lookup( SchemaConstants.SURNAME_AT ) );
        
        String attribVal = "Walker";
        attrib.add( attribVal );
        
        Modification add = new ServerModification( ModificationOperation.ADD_ATTRIBUTE, attrib );
        mods.add( add );
        
        ServerEntry lookedup = store.lookup( store.getEntryId( dn.toNormName() ) );

        store.modify( dn, mods );
        assertTrue( lookedup.get( "sn" ).contains( attribVal ) );
        
        // testing the store.modify( dn, mod, entry ) API
        ServerEntry entry = new DefaultServerEntry( registries, dn );
        attribVal = "+1974045779";
        entry.add( "telephoneNumber", attribVal );
        
        store.modify( dn, ModificationOperation.ADD_ATTRIBUTE, entry );
        lookedup = store.lookup( store.getEntryId( dn.toNormName() ) );
        assertTrue( lookedup.get( "telephoneNumber" ).contains( attribVal ) );
    }
    
    
    @Test
    public void testModifyReplace() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );

        List<Modification> mods = new ArrayList<Modification>();
        ServerAttribute attrib = new DefaultServerAttribute( SchemaConstants.SN_AT,
            attributeRegistry.lookup( SchemaConstants.SN_AT_OID ) );
        
        String attribVal = "Johnny";
        attrib.add( attribVal );
        
        Modification add = new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );
        mods.add( add );
        
        ServerEntry lookedup = store.lookup( store.getEntryId( dn.toNormName() ) );
        
        assertEquals( "WAlkeR", lookedup.get( "sn" ).get().getString() ); // before replacing
        
        store.modify( dn, mods );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getString() );
        
        // testing the store.modify( dn, mod, entry ) API
        ServerEntry entry = new DefaultServerEntry( registries, dn );
        attribVal = "JWalker";
        entry.add( "sn", attribVal );
        
        store.modify( dn, ModificationOperation.REPLACE_ATTRIBUTE, entry );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getString() );
    }
    
    
    @Test
    public void testModifyRemove() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );

        List<Modification> mods = new ArrayList<Modification>();
        ServerAttribute attrib = new DefaultServerAttribute( SchemaConstants.SN_AT,
            attributeRegistry.lookup( SchemaConstants.SN_AT_OID ) );
        
        Modification add = new ServerModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );
        mods.add( add );
        
        ServerEntry lookedup = store.lookup( store.getEntryId( dn.toNormName() ) );
        
        assertNotNull( lookedup.get( "sn" ).get() );
        
        store.modify( dn, mods );
        assertNull( lookedup.get( "sn" ) );
        
        // testing the store.modify( dn, mod, entry ) API
        ServerEntry entry = new DefaultServerEntry( registries, dn );
        
        // add an entry for the sake of testing the remove operation
        entry.add( "sn", "JWalker" );
        store.modify( dn, ModificationOperation.ADD_ATTRIBUTE, entry );
        assertNotNull( lookedup.get( "sn" ) );
        
        store.modify( dn, ModificationOperation.REMOVE_ATTRIBUTE, entry );
        assertNull( lookedup.get( "sn" ) );
    }

    
    @Test
    public void testModifyReplaceNonExistingIndexAttribute() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=Tim B,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "cn", "Tim B");
        entry.add( "entryCSN", new CsnFactory( 1 ).newInstance().toString() );
        entry.add( "entryUUID", SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        
        store.add( entry );
        
        List<Modification> mods = new ArrayList<Modification>();
        ServerAttribute attrib = new DefaultServerAttribute( SchemaConstants.OU_AT,
            attributeRegistry.lookup( SchemaConstants.OU_AT_OID ) );
        
        String attribVal = "Marketing";
        attrib.add( attribVal );
        
        Modification add = new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );
        mods.add( add );
        
        ServerEntry lookedup = store.lookup( store.getEntryId( dn.toNormName() ) );
        
        assertNull( lookedup.get( "ou" ) ); // before replacing
        
        store.modify( dn, mods );
        assertEquals( attribVal, lookedup.get( "ou" ).get().getString() );
    }
}
