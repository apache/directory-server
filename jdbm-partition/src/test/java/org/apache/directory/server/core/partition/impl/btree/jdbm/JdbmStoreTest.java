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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.directory.api.util.FileUtils;
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
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.StoreUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdbm.recman.BaseRecordManager;


/**
 * Unit test cases for JdbmStore
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("unchecked")
public class JdbmStoreTest
{
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStoreTest.class );

    JdbmPartition partition;
    CoreSession session;

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

    private static CacheService cacheService;
    private PartitionTxn partitionTxn;
    
    /** The recordManager used */
    private BaseRecordManager recMan;
    
    /** The temporary directory the files will be created in */
    private static Path tempDir;
    

    @BeforeClass
    public static void setup() throws Exception
    {
        tempDir = Files.createTempDirectory( JdbmIndexTest.class.getSimpleName() );

        File schemaRepository = new File( tempDir.toFile(), "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( tempDir.toFile() );
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
        // setup the working directory for the store
        StoreUtils.createdExtraAttributes( schemaManager );
        
        // initialize the store
        partition = new JdbmPartition( schemaManager, dnFactory );
        partition.setId( "example" );
        partition.setCacheSize( 10 );
        partition.setPartitionPath( tempDir.toUri() );
        partition.setSyncOnWrite( false );

        JdbmIndex ouIndex = new JdbmIndex( SchemaConstants.OU_AT_OID, false );
        ouIndex.setWkDirPath( tempDir.toUri() );
        partition.addIndex( ouIndex );

        JdbmIndex uidIndex = new JdbmIndex( SchemaConstants.UID_AT_OID, false );
        uidIndex.setWkDirPath( tempDir.toUri() );
        partition.addIndex( uidIndex );

        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        partition.setSuffixDn( suffixDn );

        partition.setCacheService( cacheService );
        partition.initialize();

        StoreUtils.loadExampleData( partition, schemaManager );

        DirectoryService directoryService = new MockDirectoryService();
        directoryService.setSchemaManager( schemaManager );
        session = new MockCoreSession( new LdapPrincipal(), directoryService );
        
        partitionTxn = partition.beginReadTransaction();

        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( partition != null )
        {
            // make sure all files are closed so that they can be deleted on Windows.
            partition.destroy( partitionTxn );
        }
        
        File[] files = tempDir.toFile().listFiles();
        
        for ( File file : files )
        {
            if ( !file.isDirectory() )
            {
                file.delete();
            }
        }

        partition = null;
    }
    
    
    @AfterClass
    public static void cleanup() throws Exception
    {
        FileUtils.deleteDirectory( tempDir.toFile() );
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
        Path wkdir2 = Files.createTempDirectory( JdbmIndexTest.class.getSimpleName() + "_db2" );

        // initialize the 2nd partition
        JdbmPartition store2 = new JdbmPartition( schemaManager, dnFactory );
        store2.setId( "example2" );
        store2.setCacheSize( 10 );
        store2.setPartitionPath( wkdir2.toFile().toURI() );
        store2.setSyncOnWrite( false );
        store2.addIndex( new JdbmIndex( SchemaConstants.OU_AT_OID, false ) );
        store2.addIndex( new JdbmIndex( SchemaConstants.UID_AT_OID, false ) );
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
        store2.destroy( partitionTxn );
    }


    @Test
    public void testSimplePropertiesUnlocked() throws Exception
    {
        JdbmPartition jdbmPartition = new JdbmPartition( schemaManager, dnFactory );
        jdbmPartition.setSyncOnWrite( true ); // for code coverage

        assertNull( jdbmPartition.getAliasIndex() );
        Index<Dn, String> index = new JdbmIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
            true );
        ( ( Store ) jdbmPartition ).addIndex( index );
        assertNotNull( jdbmPartition.getAliasIndex() );

        assertEquals( JdbmPartition.DEFAULT_CACHE_SIZE, jdbmPartition.getCacheSize() );
        jdbmPartition.setCacheSize( 24 );
        assertEquals( 24, jdbmPartition.getCacheSize() );

        assertNull( jdbmPartition.getPresenceIndex() );
        jdbmPartition.addIndex( new JdbmIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, false ) );
        assertNotNull( jdbmPartition.getPresenceIndex() );

        assertNull( jdbmPartition.getId() );
        jdbmPartition.setId( "foo" );
        assertEquals( "foo", jdbmPartition.getId() );

        assertNull( jdbmPartition.getRdnIndex() );
        jdbmPartition.addIndex( new JdbmRdnIndex() );
        assertNotNull( jdbmPartition.getRdnIndex() );

        assertNull( jdbmPartition.getOneAliasIndex() );
        ( ( Store ) jdbmPartition ).addIndex( new JdbmIndex<Long>(
            ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, true ) );
        assertNotNull( jdbmPartition.getOneAliasIndex() );

        assertNull( jdbmPartition.getSubAliasIndex() );
        jdbmPartition.addIndex( new JdbmIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, true ) );
        assertNotNull( jdbmPartition.getSubAliasIndex() );

        assertNull( jdbmPartition.getSuffixDn() );
        jdbmPartition.setSuffixDn( EXAMPLE_COM );
        assertEquals( "dc=example,dc=com", jdbmPartition.getSuffixDn().getName() );

        assertNotNull( jdbmPartition.getSuffixDn() );

        assertFalse( jdbmPartition.getUserIndices().hasNext() );
        jdbmPartition.addIndex( new JdbmIndex<Object>( "2.5.4.3", false ) );
        assertEquals( true, jdbmPartition.getUserIndices().hasNext() );

        assertNull( jdbmPartition.getPartitionPath() );
        jdbmPartition.setPartitionPath( new File( "." ).toURI() );
        assertEquals( new File( "." ).toURI(), jdbmPartition.getPartitionPath() );

        assertFalse( jdbmPartition.isInitialized() );
        assertTrue( jdbmPartition.isSyncOnWrite() );
        jdbmPartition.setSyncOnWrite( false );
        assertFalse( jdbmPartition.isSyncOnWrite() );

        jdbmPartition.sync();
        // make sure all files are closed so that they can be deleted on Windows.
        jdbmPartition.destroy( partitionTxn );
    }


    @Test
    public void testSimplePropertiesLocked() throws Exception
    {
        assertNotNull( partition.getAliasIndex() );
        try
        {
            partition.addIndex( new JdbmIndex<Dn>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertEquals( 10, partition.getCacheSize() );
        try
        {
            partition.setCacheSize( 24 );
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getPresenceIndex() );
        try
        {
            partition.addIndex( new JdbmIndex<String>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, false ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getId() );
        try
        {
            partition.setId( "foo" );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getRdnIndex() );
        try
        {
            partition.addIndex( new JdbmRdnIndex() );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getOneAliasIndex() );
        try
        {
            partition.addIndex( new JdbmIndex<Long>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSubAliasIndex() );
        try
        {
            partition.addIndex( new JdbmIndex<Long>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, true ) );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertNotNull( partition.getSuffixDn() );
        try
        {
            partition.setSuffixDn( EXAMPLE_COM );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        Iterator<String> systemIndices = partition.getSystemIndices();

        for ( int i = 0; i < 8; i++ )
        {
            assertTrue( systemIndices.hasNext() );
            assertNotNull( systemIndices.next() );
        }

        assertFalse( systemIndices.hasNext() );
        assertNotNull( partition.getSystemIndex( APACHE_ALIAS_AT ) );

        try
        {
            partition.getSystemIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }
        try
        {
            partition.getSystemIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNotNull( partition.getSuffixDn() );

        Iterator<String> userIndices = partition.getUserIndices();
        int count = 0;

        while ( userIndices.hasNext() )
        {
            userIndices.next();
            count++;
        }

        assertEquals( 2, count );
        assertFalse( partition.hasUserIndexOn( DC_AT ) );
        assertTrue( partition.hasUserIndexOn( OU_AT ) );
        assertTrue( partition.hasSystemIndexOn( APACHE_ALIAS_AT ) );
        userIndices = partition.getUserIndices();
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertTrue( userIndices.hasNext() );
        assertNotNull( userIndices.next() );
        assertFalse( userIndices.hasNext() );
        assertNotNull( partition.getUserIndex( OU_AT ) );

        try
        {
            partition.getUserIndex( SN_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }
        try
        {
            partition.getUserIndex( DC_AT );
            fail();
        }
        catch ( IndexNotFoundException e )
        {
        }

        assertNotNull( partition.getPartitionPath() );
        try
        {
            partition.setPartitionPath( new File( "." ).toURI() );
            fail();
        }
        catch ( IllegalStateException e )
        {
        }

        assertTrue( partition.isInitialized() );
        assertFalse( partition.isSyncOnWrite() );

        partition.sync();
    }


    @Test
    public void testFreshStore() throws Exception
    {
        Dn dn = new Dn( schemaManager, "o=Good Times Co." );
        assertEquals( Strings.getUUID( 1L ), partition.getEntryId( partitionTxn, dn ) );
        assertEquals( 11, partition.count( partitionTxn ) );
        assertEquals( "o=Good Times Co.", partition.getEntryDn( partitionTxn, Strings.getUUID( 1L ) ).getName() );
        assertEquals( dn.getName(), partition.getEntryDn( partitionTxn, Strings.getUUID( 1L ) ).getName() );
        assertEquals( dn.getName(), partition.getEntryDn( partitionTxn, Strings.getUUID( 1L ) ).getName() );

        // note that the suffix entry returns 0 for it's parent which does not exist
        assertEquals( Strings.getUUID( 0L ), partition.getParentId( partitionTxn, partition.getEntryId( partitionTxn, dn ) ) );
        assertNull( partition.getParentId( partitionTxn, Strings.getUUID( 0L ) ) );

        // should NOW be allowed
        partition.delete( partitionTxn, Strings.getUUID( 1L ) );
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
        assertEquals( 10, store.count() );

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
        addContext.setPartition( store2 );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = store2.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            store2.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
        }

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
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
            throw e;
        }
    }


    @Test(expected = LdapSchemaViolationException.class)
    public void testAddWithoutObjectClass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=Martin King,ou=Sales,o=Good Times Co." );
        Entry entry = new DefaultEntry( schemaManager, dn,
            "ou: Sales",
            "cn: Martin King" );
        AddOperationContext addContext = new AddOperationContext( null, entry );
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
            throw e;
        }
    }


    @Test
    public void testModifyAddOUAttrib() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );
        attrib.add( "Engineering" );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        partition.modify( partitionTxn, dn, add );
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
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
        }

        Rdn rdn = new Rdn( schemaManager, "sn=James" );

        partition.rename( partitionTxn, dn, rdn, true, null );

        dn = new Dn( schemaManager, "sn=James,ou=Engineering,o=Good Times Co." );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        lookupContext.setPartition( partition );
        
        try ( PartitionTxn partitionTxn2 = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn2 );
        
            Entry renamed = partition.lookup( lookupContext );
            assertNotNull( renamed );
            assertEquals( "James", renamed.getDn().getRdn().getValue() );
        }
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
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
        }

        Rdn rdn = new Rdn( schemaManager, "sn=Ja\\+es" );

        partition.rename( partitionTxn, dn, rdn, true, null );

        Dn dn2 = new Dn( schemaManager, "sn=Ja\\+es,ou=Engineering,o=Good Times Co." );
        String id = partition.getEntryId( partitionTxn, dn2 );
        assertNotNull( id );
        Entry entry2 = partition.fetch( partitionTxn, id, dn2 );
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

        AddOperationContext addContext = new AddOperationContext( null, childEntry );
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
        }

        Dn parentDn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );

        Rdn rdn = new Rdn( schemaManager, "cn=Ryan" );

        // The cn=Ryan RDN that will be added. The cn=Private Ryan RDN will be removed
        Map<String, List<ModDnAva>> modDnAvas = new HashMap<>();

        List<ModDnAva> modAvas = new ArrayList<>();
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, rdn.getAva()) );
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.DELETE, childDn.getRdn().getAva()) );
        modDnAvas.put( SchemaConstants.CN_AT_OID, modAvas );

        partition.moveAndRename( partitionTxn, childDn, parentDn, rdn, modDnAvas, childEntry );

        // to drop the alias indices
        childDn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );

        parentDn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );

        assertEquals( 3, partition.getSubAliasIndex().count( partitionTxn ) );

        Dn newDn = parentDn.add( childDn.getRdn() );

        partition.move( partitionTxn, childDn, parentDn, newDn, null );

        assertEquals( 3, partition.getSubAliasIndex().count( partitionTxn ) );
    }


    @Test
    public void testModifyAdd() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( "sn", SN_AT );

        String attribVal = "Walker";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partitionTxn, partition.getEntryId(partitionTxn,  dn ), dn );

        partition.modify( partitionTxn, dn, add );
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

        Entry lookedup = partition.fetch( partitionTxn, partition.getEntryId( partitionTxn, dn ), dn );

        assertEquals( "WAlkeR", lookedup.get( "sn" ).get().getValue() ); // before replacing

        lookedup = partition.modify( partitionTxn, dn, add );
        assertEquals( attribVal, lookedup.get( "sn" ).get().getValue() );

        // testing the store.modify( dn, mod, entry ) API
        Modification replace = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, SN_AT, "JWalker" );

        lookedup = partition.modify( partitionTxn, dn, replace );
        assertEquals( "JWalker", lookedup.get( "sn" ).get().getValue() );
        assertEquals( 1, lookedup.get( "sn" ).size() );
    }


    @Test
    public void testModifyRemove() throws Exception
    {
        Dn dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );

        Attribute attrib = new DefaultAttribute( SchemaConstants.SN_AT, SN_AT );

        Modification add = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partitionTxn, partition.getEntryId( partitionTxn, dn ), dn );

        assertNotNull( lookedup.get( "sn" ).get() );

        lookedup = partition.modify( partitionTxn, dn, add );
        assertNull( lookedup.get( "sn" ) );

        // add an entry for the sake of testing the remove operation
        add = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, SN_AT, "JWalker" );
        lookedup = partition.modify( partitionTxn, dn, add );
        assertNotNull( lookedup.get( "sn" ) );

        Modification remove = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, SN_AT );
        lookedup = partition.modify( partitionTxn, dn, remove );
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
        addContext.setPartition( partition );
        PartitionTxn partitionTxn = null;
        
        try
        {
            partitionTxn = partition.beginWriteTransaction();
            addContext.setTransaction( partitionTxn );
        
            partition.add( addContext );
            partitionTxn.commit();
        }
        catch ( Exception e )
        {
            partitionTxn.abort();
        }

        Attribute attrib = new DefaultAttribute( SchemaConstants.OU_AT, OU_AT );

        String attribVal = "Marketing";
        attrib.add( attribVal );

        Modification add = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attrib );

        Entry lookedup = partition.fetch( partitionTxn, partition.getEntryId( partitionTxn, dn ), dn );

        assertNull( lookedup.get( "ou" ) ); // before replacing

        lookedup = partition.modify( partitionTxn, dn, add );
        assertEquals( attribVal, lookedup.get( "ou" ).get().getValue() );
    }
}
