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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockCoreSession;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.MockPartitionReadTxn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Unit test cases for MavibotStore
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("unchecked")
public class MavibotStoreTest
{
    private static final Logger LOG = LoggerFactory.getLogger( MavibotStoreTest.class );

    private File wkdir;

    private MavibotPartition store;

    private CoreSession session;

    private static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;
    private static LdifSchemaLoader loader;
    private static Dn EXAMPLE_COM;

    /** The OU AttributeType instance */
    private static AttributeType OU_AT;

    /** The ApacheAlias AttributeType instance */
    private static AttributeType APACHE_ALIAS_AT;

    /** The DC AttributeType instance */
    private static AttributeType DC_AT;

    /** The SN AttributeType instance */
    private static AttributeType SN_AT;

    private RecordManager recordMan;

    private static CacheService cacheService;
    private PartitionTxn partitionTxn;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();


    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = MavibotStoreTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        EXAMPLE_COM = new Dn( schemaManager, "dc=example,dc=com" );

        OU_AT = schemaManager.getAttributeType( SchemaConstants.OU_AT );
        DC_AT = schemaManager.getAttributeType( SchemaConstants.DC_AT );
        SN_AT = schemaManager.getAttributeType( SchemaConstants.SN_AT );
        APACHE_ALIAS_AT = schemaManager.getAttributeType( ApacheSchemaConstants.APACHE_ALIAS_AT );

        cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache", String.class, Dn.class ) );
    }


    @Before
    public void createStore() throws Exception
    {
        StoreUtils.createdExtraAttributes( schemaManager );
        
        // setup the working directory for the store
        wkdir = tmpDir.newFolder( getClass().getSimpleName() );

        // initialize the store
        store = new MavibotPartition( schemaManager, dnFactory );
        store.setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        MavibotIndex ouIndex = new MavibotIndex( SchemaConstants.OU_AT_OID, false );
        ouIndex.setWkDirPath( wkdir.toURI() );
        store.addIndex( ouIndex );

        MavibotIndex uidIndex = new MavibotIndex( SchemaConstants.UID_AT_OID, false );
        uidIndex.setWkDirPath( wkdir.toURI() );
        store.addIndex( uidIndex );

        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        store.setSuffixDn( suffixDn );

        store.setCacheService( cacheService );
        store.initialize();

        recordMan = store.getRecordMan();

        StoreUtils.loadExampleData( store, schemaManager );

        DirectoryService directoryService = new MockDirectoryService();
        directoryService.setSchemaManager( schemaManager );
        session = new MockCoreSession( new LdapPrincipal(), directoryService );
        
        partitionTxn = new MockPartitionReadTxn();

        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            // make sure all files are closed so that they can be deleted on Windows.
            //store.destroy();
        }

        store = null;
        wkdir = null;
    }


    /**
     * Tests a suffix with two name components: dc=example,dc=com.
     * When reading this entry back from the store the Dn must
     * consist of two RDNs.
     */
    @Test
    public void testTwoComponentSuffix() throws Exception
    {
        // setup the working directory for the 2nd store
        File wkdir2 = tmpDir.newFolder( getClass().getSimpleName() + "-store2" );

        // initialize the 2nd store
        MavibotPartition store2 = new MavibotPartition( schemaManager, dnFactory );
        store2.setId( "example2" );
        store2.setCacheSize( 10 );
        store2.setPartitionPath( wkdir2.toURI() );
        store2.setSyncOnWrite( false );
        store2.addIndex( new MavibotIndex( SchemaConstants.OU_AT_OID, false ) );
        store2.addIndex( new MavibotIndex( SchemaConstants.UID_AT_OID, false ) );
        store2.setSuffixDn( EXAMPLE_COM );
        store2.setCacheService( cacheService );
        store2.initialize();

        // inject context entry
        Dn suffixDn = new Dn( schemaManager, "dc=example,dc=com" );
        Entry entry = new DefaultEntry( schemaManager, suffixDn,
            "objectClass: top",
            "objectClass: domain",
            "dc: example",
            SchemaConstants.ENTRY_CSN_AT, new CsnFactory( 0 ).newInstance().toString(),
            SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store2 );
        addContext.setTransaction( store2.beginWriteTransaction() );

        store2.add( addContext );

        // lookup the context entry
        String id = store2.getEntryId( partitionTxn, suffixDn );
        Entry lookup = store2.fetch( partitionTxn, id, suffixDn );
        assertEquals( 2, lookup.getDn().size() );

        // make sure all files are closed so that they can be deleted on Windows.
        // wkdir2 is deleted by TemporaryFolder rule
    }


    @Test
    public void testSimplePropertiesUnlocked() throws Exception
    {
        MavibotPartition MavibotPartition = new MavibotPartition( schemaManager, dnFactory );
        MavibotPartition.setSyncOnWrite( true ); // for code coverage

        assertNull( MavibotPartition.getAliasIndex() );
        Index<Dn, String> index = new MavibotIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
            true );
        ( ( Store ) MavibotPartition ).addIndex( index );
        assertNotNull( MavibotPartition.getAliasIndex() );

        assertEquals( MavibotPartition.DEFAULT_CACHE_SIZE, MavibotPartition.getCacheSize() );
        MavibotPartition.setCacheSize( 24 );
        assertEquals( 24, MavibotPartition.getCacheSize() );

        assertNull( MavibotPartition.getPresenceIndex() );
        MavibotPartition
            .addIndex( new MavibotIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, false ) );
        assertNotNull( MavibotPartition.getPresenceIndex() );

        assertNull( MavibotPartition.getId() );
        MavibotPartition.setId( "foo" );
        assertEquals( "foo", MavibotPartition.getId() );

        assertNull( MavibotPartition.getRdnIndex() );
        MavibotPartition.addIndex( new MavibotRdnIndex() );
        assertNotNull( MavibotPartition.getRdnIndex() );

        assertNull( MavibotPartition.getOneAliasIndex() );
        ( ( Store ) MavibotPartition ).addIndex( new MavibotIndex<Long>(
            ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, true ) );
        assertNotNull( MavibotPartition.getOneAliasIndex() );

        assertNull( MavibotPartition.getSubAliasIndex() );
        MavibotPartition
            .addIndex( new MavibotIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, true ) );
        assertNotNull( MavibotPartition.getSubAliasIndex() );

        assertNull( MavibotPartition.getSuffixDn() );
        MavibotPartition.setSuffixDn( EXAMPLE_COM );
        assertEquals( "dc=example,dc=com", MavibotPartition.getSuffixDn().getName() );

        assertNotNull( MavibotPartition.getSuffixDn() );

        assertFalse( MavibotPartition.getUserIndices().hasNext() );
        MavibotPartition.addIndex( new MavibotIndex<Object>( "2.5.4.3", false ) );
        assertEquals( true, MavibotPartition.getUserIndices().hasNext() );

        assertNull( MavibotPartition.getPartitionPath() );
        MavibotPartition.setPartitionPath( new File( "." ).toURI() );
        assertEquals( new File( "." ).toURI(), MavibotPartition.getPartitionPath() );

        assertFalse( MavibotPartition.isInitialized() );
        assertTrue( MavibotPartition.isSyncOnWrite() );
        MavibotPartition.setSyncOnWrite( false );
        assertFalse( MavibotPartition.isSyncOnWrite() );

        MavibotPartition.sync();
        // make sure all files are closed so that they can be deleted on Windows.
        MavibotPartition.destroy( partitionTxn );
    }


    @Test
    public void testSimplePropertiesLocked() throws Exception
    {
        assertNotNull( store.getAliasIndex() );
        try
        {
            store.addIndex( new MavibotIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertEquals( 10, store.getCacheSize() );
        try
        {
            store.setCacheSize( 24 );
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getPresenceIndex() );
        try
        {
            store.addIndex( new MavibotIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, false ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getId() );
        try
        {
            store.setId( "foo" );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getRdnIndex() );
        try
        {
            store.addIndex( new MavibotRdnIndex() );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getOneAliasIndex() );
        try
        {
            store.addIndex( new MavibotIndex<Long>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getSubAliasIndex() );
        try
        {
            store.addIndex( new MavibotIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( store.getSuffixDn() );
        try
        {
            store.setSuffixDn( EXAMPLE_COM );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        Iterator<String> systemIndices = store.getSystemIndices();

        for ( int i = 0; i < 8; i++ )
        {
            assertTrue( systemIndices.hasNext() );
            assertNotNull( systemIndices.next() );
        }

        assertFalse( systemIndices.hasNext() );
        assertNotNull( store.getSystemIndex( APACHE_ALIAS_AT ) );

        try
        {
            store.getSystemIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }
        try
        {
            store.getSystemIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNotNull( store.getSuffixDn() );

        Iterator<String> userIndices = store.getUserIndices();
        int count = 0;

        while ( userIndices.hasNext() )
        {
            userIndices.next();
            count++;
        }

        assertEquals( 2, count );
        assertFalse( store.hasUserIndexOn( DC_AT ) );
        assertTrue( store.hasUserIndexOn( OU_AT ) );
        assertTrue( store.hasSystemIndexOn( APACHE_ALIAS_AT ) );
        userIndices = store.getUserIndices();
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertFalse( userIndices.hasNext() );
        assertNotNull( store.getUserIndex( OU_AT ) );

        try
        {
            store.getUserIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }
        try
        {
            store.getUserIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNotNull( store.getPartitionPath() );
        try
        {
            store.setPartitionPath( new File( "." ).toURI() );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertTrue( store.isInitialized() );
        assertFalse( store.isSyncOnWrite() );

        store.sync();
    }


    @Test
    public void testFreshStore() throws Exception
    {
        Dn dn = new Dn( schemaManager, "o=Good Times Co." );
        assertEquals( Strings.getUUID( 1L ), store.getEntryId( partitionTxn, dn ) );
        assertEquals( 11, store.count( partitionTxn ) );
        assertEquals( "o=Good Times Co.", store.getEntryDn( partitionTxn, Strings.getUUID( 1L ) ).getName() );
        assertEquals( dn.getName(), store.getEntryDn( partitionTxn, Strings.getUUID( 1L ) ).getName() );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( Strings.getUUID( 0L ), store.getParentId( partitionTxn, store.getEntryId( partitionTxn, dn ) ) );
        assertNull( store.getParentId( partitionTxn, Strings.getUUID( 0L ) ) );

        // should NOW be allowed
        store.delete( partitionTxn, Strings.getUUID( 1L ) );
    }


    /*
    @Test
    public void testEntryOperations() throws Exception
    {
        assertEquals( 3, store.getChildCount( Strings.getUUID( 1L ) ) );

        Cursor<IndexEntry<String, String>> cursor = store.list( Strings.getUUID( 1L ) );
        assertNotNull( cursor );
        cursor.beforeFirst();
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 3L ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 4L ), cursor.get().getId() );
        assertTrue( cursor.next() );
        assertEquals( Strings.getUUID( 2L ), cursor.get().getId() );
        assertFalse( cursor.next() );

        cursor.close();

        assertEquals( 3, store.getChildCount( Strings.getUUID( 1L ) ) );

        store.delete( Strings.getUUID( 2L ) );
        assertEquals( 2, store.getChildCount( Strings.getUUID( 1L ) ) );
        assertEquals( 10, store.count( partitionTxn ) );

        // add an alias and delete to test dropAliasIndices method
        Dn dn = new Dn( schemaManager, "commonName=Jack Daniels,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "ou: Apache",
            "commonName: Jack Daniels",
            "aliasedObjectName: cn=Jack Daniels,ou=Engineering,o=Good Times Co.",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", Strings.getUUID( 12L ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        store.add( addContext );

        store.delete( Strings.getUUID( 12L ) ); // drops the alias indices
    }
    */

    @Test(expected = LdapNoSuchObjectException.class)
    public void testAddWithoutParentId() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Marting King,ou=Not Present,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Not Present",
            "cn: Martin King" );
        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );
        
        store.add( addContext );
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testAddWithoutObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Martin King,ou=Sales,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "ou: Sales",
            "cn: Martin King" );
        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );
        
        store.add( addContext );
    }


    @Test
    public void testModifyAddOUAttrib() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );
        attrib.add( "Engineering" );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        store.modify( partitionTxn, dn, add );
    }


    @Test
    public void testRename() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Private Ryan,ou=Engineering,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Engineering",
            "cn: Private Ryan",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );
        
        store.add( addContext );

        Rdn rdn = new Rdn( schemaManager, "sn=James" );

        store.rename( partitionTxn, dn, rdn, true, null );

        dn = new Dn( schemaManager, "sn=James,ou=Engineering,o=Good Times Co." );
        Entry renamed;
        
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        lookupContext.setPartition( store );
        
        try ( PartitionTxn partitionTxn = store.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );
            renamed = store.lookup( lookupContext );
        }
        
        assertNotNull( renamed );
        assertEquals( "James", renamed.getDn().getRdn().getValue() );
    }


    @Test
    public void testRenameEscaped() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Private Ryan,ou=Engineering,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Engineering",
            "cn: Private Ryan",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );
        
        store.add( addContext );

        Rdn rdn = new Rdn( schemaManager, "sn=Ja\\+es" );

        store.rename( partitionTxn, dn, rdn, true, null );

        Dn dn2 = new Dn( schemaManager, "sn=Ja\\+es,ou=Engineering,o=Good Times Co." );
        String id = store.getEntryId( partitionTxn, dn2 );
        assertNotNull( id );
        Entry entry2 = store.fetch( partitionTxn, id, dn2 );
        assertEquals( "Ja+es", entry2.get( "sn" ).getString() );
    }


    @Test
    public void testMove() throws Exception
    {
        Dn childDn = new Dn( schemaManager, "cn=Private Ryan,ou=Engineering,o=Good Times Co." );
        Entry childEntry = new DefaultEntry( schemaManager, childDn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou", "Engineering",
            "cn", "Private Ryan",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        assertEquals( 3, store.getSubAliasIndex().count( partitionTxn ) );

        AddOperationContext addContext = new AddOperationContext( null, childEntry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );

        store.add( addContext );

        assertEquals( 3, store.getSubAliasIndex().count( partitionTxn ) );

        Dn parentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );

        Rdn rdn = new Rdn( schemaManager, "cn=Ryan" );

        // The cn=Ryan RDN that will be added. The cn=Private Ryan RDN will be removed
        Map<String, List<ModDnAva>> modDnAvas = new HashMap<>();

        List<ModDnAva> modAvas = new ArrayList<>();
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, rdn.getAva()) );
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.DELETE, childDn.getRdn().getAva()) );
        modDnAvas.put( SchemaConstants.CN_AT_OID, modAvas );

        store.moveAndRename( partitionTxn, childDn, parentDn, rdn, modDnAvas, childEntry );

        // to drop the alias indices
        childDn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );

        parentDn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );

        assertEquals( 3, store.getSubAliasIndex().count( partitionTxn ) );

        Dn newDn = parentDn.add( childDn.getRdn() );

        store.move( partitionTxn, childDn, parentDn, newDn, null );

        assertEquals( 3, store.getSubAliasIndex().count( partitionTxn ) );
    }


    @Test
    public void testModifyAdd() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( "sn", SN_AT );

        String attribVal = "Walker";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        Entry lookedup = store.fetch( partitionTxn, store.getEntryId( partitionTxn, dn ), dn );

        lookedup = store.modify( partitionTxn, dn, add );
        assertTrue( lookedup.get( "sn" ).contains( attribVal ) );
    }


    @Test
    public void testModifyReplace() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, SN_AT );

        String attribVal = "Johnny";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = store.fetch( partitionTxn, store.getEntryId( partitionTxn, dn ), dn );

        assertEquals( "WAlkeR", lookedup.get( "sn" ).get().getValue() ); // before replacing

        lookedup = store.modify( partitionTxn, dn, add );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getValue() );

        // testing the store.modify( dn, mod, entry ) API
        Modification replace = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SN_AT, "JWalker" );

        lookedup = store.modify( partitionTxn, dn, replace );
        assertEquals( "JWalker", lookedup.get( "sn" ).get().getValue() );
        assertEquals( 1, lookedup.get( "sn" ).size() );
    }


    @Test
    public void testModifyRemove() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, SN_AT );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        Entry lookedup = store.fetch( partitionTxn, store.getEntryId( partitionTxn, dn ), dn );

        assertNotNull( lookedup.get( "sn" ).get() );

        lookedup = store.modify( partitionTxn, dn, add );
        assertNull( lookedup.get( "sn" ) );

        // add an entry for the sake of testing the remove operation
        add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, SN_AT, "JWalker" );
        lookedup = store.modify( partitionTxn, dn, add );
        assertNotNull( lookedup.get( "sn" ) );

        Modification remove = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, SN_AT );
        lookedup = store.modify( partitionTxn, dn, remove );
        assertNull( lookedup.get( "sn" ) );
    }


    @Test
    public void testModifyReplaceNonExistingIndexAttribute() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Tim B,ou=Sales,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "cn", "Tim B",
            "entryCSN", new CsnFactory( 1 ).newInstance().toString(),
            "entryUUID", UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( store );
        addContext.setTransaction( store.beginWriteTransaction() );
        
        store.add( addContext );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "Marketing";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = store.fetch( partitionTxn, store.getEntryId( partitionTxn, dn ), dn );

        assertNull( lookedup.get( "ou" ) ); // before replacing

        lookedup = store.modify( partitionTxn, dn, add );
        assertEquals( attribVal, lookedup.get( "ou" ).get().getValue() );
    }


    @Test
    @Ignore("Ignore till mavibot file nam extensions are frozen")
    public void testDeleteUnusedIndexFiles() throws Exception
    {
        File ouIndexDbFile = new File( wkdir, SchemaConstants.OU_AT_OID + ".db" );
        File ouIndexTxtFile = new File( wkdir, SchemaConstants.OU_AT_OID + "-ou.txt" );
        File uuidIndexDbFile = new File( wkdir, SchemaConstants.ENTRY_UUID_AT_OID + ".db" );

        assertTrue( ouIndexDbFile.exists() );
        assertTrue( ouIndexTxtFile.exists() );

        // destroy the store to manually start the init phase
        // by keeping the same work dir
        store.destroy( partitionTxn );

        // just assert again that ou files exist even after destroying the store
        assertTrue( ouIndexDbFile.exists() );
        assertTrue( ouIndexTxtFile.exists() );

        store = new MavibotPartition( schemaManager, dnFactory );
        store.setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );
        // do not add ou index this time
        store.addIndex( new MavibotIndex( SchemaConstants.UID_AT_OID, false ) );

        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        store.setSuffixDn( suffixDn );
        // init the store to call deleteUnusedIndexFiles() method
        store.initialize();

        assertFalse( ouIndexDbFile.exists() );
        assertFalse( ouIndexTxtFile.exists() );
    }
}
