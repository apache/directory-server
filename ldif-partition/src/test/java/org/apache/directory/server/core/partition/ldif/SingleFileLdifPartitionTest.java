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

package org.apache.directory.server.core.partition.ldif;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockCoreSession;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Unit test cases for the partition implementation backed by a single LDIF file
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
//NOTE: do not use junit concurrent annotations
public class SingleFileLdifPartitionTest
{
    private static SchemaManager schemaManager = null;

    private static DnFactory dnFactory;

    private static CsnFactory defaultCSNFactory;

    private static CoreSession mockSession;

    private static Entry contextEntry;

    private static LdifReader reader;

    /** the file in use during the current test method's execution */
    private File ldifFileInUse;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static CacheService cacheService;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = SingleFileLdifPartitionTest.class.getResource( "" ).getPath();
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

        reader = new LdifReader( schemaManager );

        defaultCSNFactory = new CsnFactory( 0 );

        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        mockSession = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );

        String contextEntryStr =
            "dn: ou=test, ou=system\n" +
                "objectclass: organizationalUnit\n" +
                "objectclass: top\n" +
                "ou: test\n" +
                "entryUUID: 8c7b24a6-1687-461c-88ea-4d30fc234f9b\n" +
                "entryCSN: 20100919005926.530000Z#000000#000#000000";

        LdifEntry ldifEntry = reader.parseLdif( contextEntryStr ).get( 0 );

        contextEntry = new ClonedServerEntry( new DefaultEntry( schemaManager, ldifEntry.getEntry() ) );

        cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache", String.class, Dn.class ) );
    }


    @Before
    public void createStore() throws Exception
    {
        ldifFileInUse = folder.newFile( "partition.ldif" );
    }


    private Entry createEntry( String dn ) throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager );
        entry.setDn( new Dn( schemaManager, dn ) );
        entry.put( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
        entry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );

        Entry clonedEntry = new ClonedServerEntry( entry );

        return clonedEntry;
    }


    private long getEntryLdifLen( Entry entry ) throws LdapException
    {
        // Remove the entryDn attribute
        Entry copy = entry.clone();
        copy.removeAttributes( "entryDn" );

        // while writing to the file 1 extra newline char will be added

        String ldif = LdifUtils.convertToLdif( copy ) + "\n";
        byte[] data = Strings.getBytesUtf8( ldif );

        return data.length;
    }


    /**
     * creates a partition from the given ldif file. If the ldif file name is null
     * then creates a new file and initializes the partition. If the truncate flag is true
     * and the given file exists then it erases all the contents of the ldif file before
     * initializing the partition
     *
     * @param fileName the full path to the ldif file to be loaded
     * @param truncate the flag to determine to truncate the file or not
     * @return the ldif partition after loading all the data
     * @throws Exception
     */
    private SingleFileLdifPartition createPartition( String fileName, boolean truncate ) throws Exception
    {
        if ( fileName == null )
        {
            fileName = ldifFileInUse.getAbsolutePath();
        }

        if ( truncate )
        {
            RandomAccessFile rf = new RandomAccessFile( fileName, "rws" );
            rf.setLength( 0 );

            rf.close();
        }

        SingleFileLdifPartition partition = new SingleFileLdifPartition( schemaManager, dnFactory );
        partition.setId( "test-ldif" );
        partition.setPartitionPath( new File( fileName ).toURI() );
        partition.setSuffixDn( new Dn( schemaManager, "ou=test,ou=system" ) );
        partition.setSchemaManager( schemaManager );
        partition.setCacheService( cacheService );
        partition.initialize();

        return partition;
    }


    private SingleFileLdifPartition reloadPartition() throws Exception
    {
        return createPartition( ldifFileInUse.getAbsolutePath(), false );
    }


    private void assertExists( SingleFileLdifPartition partition, Entry entry ) throws LdapException
    {
        LookupOperationContext opCtx = new LookupOperationContext( mockSession, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        opCtx.setDn( entry.getDn() );
        opCtx.setPartition( partition );
        Entry fetched;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            opCtx.setTransaction( partitionTxn );

            fetched = partition.lookup( opCtx );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( fetched );

        // Check the EntryDn attribute
        Attribute entryDn = fetched.get( "entryDn" );

        assertNotNull( entryDn );
        assertEquals( entryDn.getString(), entry.getDn().getName() );

        if ( !entry.contains( entryDn ) )
        {
            // Removed the entryDn attribute to be able to compare the entries
            fetched.removeAttributes( "entryDn" );
        }

        if ( !entry.containsAttribute( SchemaConstants.CONTEXT_CSN_AT ) )
        {
            // Removed the entryDn attribute to be able to compare the entries
            fetched.removeAttributes( SchemaConstants.CONTEXT_CSN_AT );
        }

        assertEquals( entry, fetched );
    }


    private void assertExists( SingleFileLdifPartition partition, String dn ) throws LdapException
    {
        LookupOperationContext opCtx = new LookupOperationContext( mockSession );
        opCtx.setDn( new Dn( schemaManager, dn ) );
        opCtx.setPartition( partition );
        Entry fetched;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            opCtx.setTransaction( partitionTxn );

            fetched = partition.lookup( opCtx );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( fetched );
    }


    private void assertNotExists( SingleFileLdifPartition partition, Entry entry ) throws LdapException
    {
        LookupOperationContext opCtx = new LookupOperationContext( mockSession );
        opCtx.setDn( entry.getDn() );
        opCtx.setPartition( partition );
        Entry fetched;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            opCtx.setTransaction( partitionTxn );

            fetched = partition.lookup( opCtx );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNull( fetched );
    }


    //-------------------------------------------------------------------------
    // Partition.add() tests
    //-------------------------------------------------------------------------

    @Test
    public void testAddContextEntry() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );
        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        String id = partition.getEntryId( partition.beginReadTransaction(), contextEntry.getDn() );
        assertNotNull( id );

        Entry fetched = partition.fetch( partition.beginReadTransaction(), id );

        //remove the entryDn cause it is not present in the above hand made contextEntry
        fetched.removeAttributes( SchemaConstants.ENTRY_DN_AT );

        assertEquals( contextEntry, fetched );

        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );

        assertEquals( getEntryLdifLen( contextEntry ), file.length() );

        file.close();

        partition = reloadPartition();
        assertExists( partition, contextEntry );
    }


    /**
     * Test some entries creation
     *
     * @throws Exception
     */
    @Test
    public void testAddEntries() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );

        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        Entry entry1 = createEntry( "cn=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "person" );
        entry1.put( "cn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "cn=test,cn=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "person" );
        entry2.put( "cn", "test" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entryMvrdn = createEntry( "cn=mvrdn+objectClass=person,cn=test,ou=test,ou=system" );
        entryMvrdn.put( "ObjectClass", "top", "person" );
        entryMvrdn.put( "cn", "mvrdn" );
        addCtx.setEntry( entryMvrdn );

        partition.add( addCtx );

        partition = reloadPartition();
        assertExists( partition, contextEntry );
        assertExists( partition, entry1 );
        assertExists( partition, entry2 );
        assertExists( partition, entryMvrdn );
    }


    /**
     * Test modifying an entry present at various positions in the LDIF file
     * 1. Single entry at the start of the file
     * 2. modify an entry with and without causing the changes to its size
     * 3.modify an entry present in the middle of the file with increasing/decresing
     *   size
     * @throws Exception
     */
    @Test
    public void testModifyEntry() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );
        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        ModifyOperationContext modOpCtx = new ModifyOperationContext( mockSession );
        modOpCtx.setEntry( contextEntry );

        List<Modification> modItems = new ArrayList<Modification>();

        Attribute attribute = new DefaultAttribute( schemaManager.lookupAttributeTypeRegistry( "description" ) );
        attribute.add( "this is description" );

        Modification mod = new DefaultModification();
        mod.setOperation( ModificationOperation.ADD_ATTRIBUTE );
        mod.setAttribute( attribute );

        modItems.add( mod );
        modOpCtx.setModItems( modItems );

        modOpCtx.setDn( contextEntry.getDn() );

        partition.modify( modOpCtx );
        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );
        assertEquals( getEntryLdifLen( modOpCtx.getAlteredEntry() ), file.length() );

        // perform the above operation, this time without causing change to the entry's size
        modOpCtx = new ModifyOperationContext( mockSession );
        modOpCtx.setEntry( new ClonedServerEntry( contextEntry ) );

        modItems = new ArrayList<Modification>();

        attribute = new DefaultAttribute( schemaManager.lookupAttributeTypeRegistry( "description" ) );
        attribute.add( "siht si noitpircsed" ); // reversed "this is description"

        mod = new DefaultModification();
        mod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
        mod.setAttribute( attribute );

        modItems.add( mod );
        modOpCtx.setModItems( modItems );

        modOpCtx.setDn( contextEntry.getDn() );

        partition.modify( modOpCtx );
        assertEquals( getEntryLdifLen( modOpCtx.getAlteredEntry() ), file.length() );

        Entry entry1 = createEntry( "cn=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "person" );
        entry1.put( "cn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "cn=test,cn=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "person" );
        entry2.put( "cn", "test" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        // now perform a modification on the entry present in middle of LDIF file
        modOpCtx = new ModifyOperationContext( mockSession );
        modOpCtx.setEntry( new ClonedServerEntry( entry1 ) );
        modOpCtx.setDn( entry1.getDn() );

        modItems = new ArrayList<Modification>();

        attribute = new DefaultAttribute( schemaManager.lookupAttributeTypeRegistry( "description" ) );
        attribute.add( "desc of entry1" ); // reversed "this is description"

        mod = new DefaultModification();
        mod.setOperation( ModificationOperation.ADD_ATTRIBUTE );
        mod.setAttribute( attribute );

        modItems.add( mod );
        modOpCtx.setModItems( modItems );

        partition.modify( modOpCtx );

        long ctxEntryLen = getEntryLdifLen( contextEntry );
        long entry1Len = getEntryLdifLen( entry1 );

        file.seek( ctxEntryLen );

        byte[] entry1Data = new byte[( int ) entry1Len];

        file.read( entry1Data );

        String ldif = Strings.utf8ToString( entry1Data );

        LdifEntry ldifEntry = reader.parseLdif( ldif ).get( 0 );

        // Remove the EntryDN
        entry1.removeAttributes( "entryDn" );

        assertEquals( entry1, new DefaultEntry( schemaManager, ldifEntry.getEntry() ) );

        //"description: desc of entry1\n"

        modOpCtx = new ModifyOperationContext( mockSession );
        modOpCtx.setEntry( new ClonedServerEntry( entry1 ) );
        modOpCtx.setDn( entry1.getDn() );

        modItems = new ArrayList<Modification>();

        attribute = new DefaultAttribute( schemaManager.lookupAttributeTypeRegistry( "description" ) );
        attribute.add( "desc of entry1" ); // reversed "this is description"

        mod = new DefaultModification();
        mod.setOperation( ModificationOperation.REMOVE_ATTRIBUTE );
        mod.setAttribute( attribute );

        modItems.add( mod );
        modOpCtx.setModItems( modItems );

        partition.modify( modOpCtx );

        file.seek( ctxEntryLen );

        entry1Len = getEntryLdifLen( entry1 );
        entry1Data = new byte[( int ) entry1Len];

        file.read( entry1Data );

        ldif = Strings.utf8ToString( entry1Data );

        ldifEntry = reader.parseLdif( ldif ).get( 0 );

        // Remove the EntryDN
        entry1.removeAttributes( "entryDn" );

        assertEquals( entry1, new DefaultEntry( schemaManager, ldifEntry.getEntry() ) );

        partition = reloadPartition();
        assertExists( partition, contextEntry );
        assertExists( partition, entry1 );
        assertExists( partition, entry2 );

        file.close();
    }


    /**
     * Test that we can't add an existing entry
     *
     * @throws Exception
     */
    @Test
    public void testLdifAddExistingEntry() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );

        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        Entry entry1 = createEntry( "cn=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "person" );
        entry1.put( "cn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "cn=test,cn=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "person" );
        entry2.put( "cn", "test" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "cn=test,cn=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "person" );
        entry3.put( "cn", "test" );
        addCtx.setEntry( entry3 );

        try
        {
            partition.add( addCtx );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }

        partition = reloadPartition();
        assertExists( partition, contextEntry );
        assertExists( partition, entry1 );
        assertExists( partition, entry2 );
    }


    //-------------------------------------------------------------------------
    // Partition.delete() tests
    //-------------------------------------------------------------------------
    /**
     * Test that we can delete an existing entry
     *
     * @throws Exception
     */
    @Test
    public void testLdifDeleteExistingEntry() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );
        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        DeleteOperationContext delOpCtx = new DeleteOperationContext( mockSession );
        delOpCtx.setDn( contextEntry.getDn() );
        delOpCtx.setPartition( partition );
        delOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.delete( delOpCtx );
        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );

        assertEquals( 0L, file.length() );

        file.close();

        addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        Entry entry1 = createEntry( "cn=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "person" );
        entry1.put( "cn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "cn=test1,cn=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "person" );
        entry2.put( "cn", "test1" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "cn=test2,cn=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "person" );
        entry3.put( "cn", "test2" );
        addCtx.setEntry( entry3 );

        partition.add( addCtx );

        Entry entryMvrdn = createEntry( "cn=mvrdn+objectClass=person,cn=test,ou=test,ou=system" );
        entryMvrdn.put( "ObjectClass", "top", "person" );
        entryMvrdn.put( "cn", "mvrdn" );
        addCtx.setEntry( entryMvrdn );

        partition.add( addCtx );

        DeleteOperationContext delCtx = new DeleteOperationContext( mockSession );
        delCtx.setDn( entryMvrdn.getDn() );
        delCtx.setPartition( partition );
        delCtx.setTransaction( partition.beginWriteTransaction() );

        partition.delete( delCtx );

        partition = reloadPartition();
        assertExists( partition, entry1 );
        assertExists( partition, entry2 );
        assertExists( partition, entry3 );
    }


    //-------------------------------------------------------------------------
    // Partition.delete() tests
    //-------------------------------------------------------------------------
    /**
     * Test that we can search for an existing entry
     *
     * @throws Exception
     */
    @Test
    public void testLdifSearchExistingEntry() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );

        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        Entry entry1 = createEntry( "cn=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "person" );
        entry1.put( "cn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "cn=test1,cn=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "person" );
        entry2.put( "cn", "test1" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "cn=test2,cn=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "person" );
        entry3.put( "cn", "test2" );
        addCtx.setEntry( entry3 );

        partition.add( addCtx );

        SearchOperationContext searchCtx = new SearchOperationContext( mockSession );

        Dn dn = new Dn( schemaManager, "cn=test,ou=test,ou=system" );
        searchCtx.setDn( dn );
        ExprNode filter = FilterParser.parse( schemaManager, "(ObjectClass=person)" );
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        FilterNormalizingVisitor visitor = new FilterNormalizingVisitor( ncn, schemaManager );
        filter.accept( visitor );
        searchCtx.setFilter( filter );
        searchCtx.setScope( SearchScope.SUBTREE );

        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            searchCtx.setPartition( partition );
            searchCtx.setTransaction( partitionTxn );
    
            EntryFilteringCursor cursor = partition.search( searchCtx );
    
            assertNotNull( cursor );
    
            Set<Dn> expectedDns = new HashSet<>();
            expectedDns.add( entry1.getDn() );
            expectedDns.add( entry2.getDn() );
            expectedDns.add( entry3.getDn() );
    
            cursor.beforeFirst();
            int nbRes = 0;
    
            while ( cursor.next() )
            {
                Entry entry = cursor.get();
                assertNotNull( entry );
                nbRes++;
    
                expectedDns.remove( entry.getDn() );
            }
    
            assertEquals( 3, nbRes );
            assertEquals( 0, expectedDns.size() );
    
            cursor.close();
        }
    }


    @Test
    public void testLdifMoveEntry() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Entry childEntry1 = partition.fetch( partition.beginReadTransaction(), 
            partition.getEntryId( partition.beginReadTransaction(), new Dn( schemaManager,
            "cn=child1,ou=test,ou=system" ) ) );
        Entry childEntry2 = partition.fetch( partition.beginReadTransaction(), 
            partition.getEntryId( partition.beginReadTransaction(), new Dn( schemaManager,
            "cn=child2,ou=test,ou=system" ) ) );

        MoveOperationContext moveOpCtx = new MoveOperationContext( mockSession, childEntry1.getDn(),
            childEntry2.getDn() );
        moveOpCtx.setPartition( partition );
        moveOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.move( moveOpCtx );

        partition = reloadPartition();
        assertExists( partition, childEntry2 );
        assertNotExists( partition, childEntry1 );

        assertExists( partition, "cn=child1,cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild11,cn=child1,cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild12,cn=child1,cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=greatGrandChild111,cn=grandChild11,cn=child1,cn=child2,ou=test,ou=system" );
    }


    @Test
    public void testLdifMoveSubChildEntry() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Entry childEntry1 = partition.fetch( partition.beginReadTransaction(), 
            partition.getEntryId( partition.beginReadTransaction(), new Dn( schemaManager,
            "cn=grandChild11,cn=child1,ou=test,ou=system" ) ) );
        Entry childEntry2 = partition.fetch( partition.beginReadTransaction(), 
            partition.getEntryId( partition.beginReadTransaction(), new Dn( schemaManager,
            "cn=child2,ou=test,ou=system" ) ) ); 

        MoveOperationContext moveOpCtx = new MoveOperationContext( mockSession, childEntry1.getDn(),
            childEntry2.getDn() );
        moveOpCtx.setPartition( partition );
        moveOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.move( moveOpCtx );

        partition = reloadPartition();
        assertExists( partition, childEntry2 );
        assertNotExists( partition, childEntry1 );

        assertExists( partition, "cn=child1,ou=test,ou=system" );
        assertExists( partition, "cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild11,cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild12,cn=child1,ou=test,ou=system" );
        assertExists( partition, "cn=greatGrandChild111,cn=grandChild11,cn=child2,ou=test,ou=system" );
    }


    @Test
    public void testLdifRenameAndDeleteOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "cn=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.CN_AT + "=" + "renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( mockSession, childDn1, newRdn, true );
        renameOpCtx.setPartition( partition );
        renameOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.rename( renameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "cn=renamedChild1,ou=test,ou=system" );

        LookupOperationContext lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry entry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            entry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( entry );
        assertFalse( entry.get( "cn" ).contains( "child1" ) );
    }


    @Test
    public void testLdifRenameAndRetainOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "cn=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.CN_AT + "=" + "renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( mockSession, childDn1, newRdn, false );
        renameOpCtx.setPartition( partition );
        renameOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.rename( renameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "cn=renamedChild1,ou=test,ou=system" );
        
        LookupOperationContext lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry entry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            entry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( entry );
        assertTrue( entry.get( "cn" ).contains( "child1" ) );
    }


    @Test
    public void testLdifMoveAndRenameWithDeletingOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "cn=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "cn=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( schemaManager, "cn=movedChild1" );
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( mockSession, childDn1,
            childDn2, newRdn, true );
        
        LookupOperationContext lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry originalEntry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            originalEntry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        Entry modifiedEntry = originalEntry.clone();
        modifiedEntry.remove( "cn", "child1" );
        modifiedEntry.add( "cn", "movedChild1" );

        moveAndRenameOpCtx.setEntry( originalEntry );
        moveAndRenameOpCtx.setModifiedEntry( modifiedEntry );
        moveAndRenameOpCtx.setPartition( partition );
        moveAndRenameOpCtx.setTransaction( partition.beginWriteTransaction() );
        
        // The dc=movedChild1 RDN that will be added. The dc=child1 Ryan RDN will be removed
        Map<String, List<ModDnAva>> modDnAvas = new HashMap<>();

        List<ModDnAva> modAvas = new ArrayList<>();
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, newRdn.getAva() ) );
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.DELETE, childDn1.getRdn().getAva()) );
        modDnAvas.put( SchemaConstants.CN_AT_OID, modAvas );
        
        moveAndRenameOpCtx.setModifiedAvas( modDnAvas );
        
        partition.moveAndRename( moveAndRenameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "cn=movedChild1,cn=child2,ou=test,ou=system" );

        lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry entry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            entry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( entry );
        Attribute dc = entry.get( "cn" );
        assertFalse( dc.contains( "child1" ) );
        assertTrue( dc.contains( "movedChild1" ) );
    }


    @Test
    public void testLdifMoveAndRenameRetainingOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "cn=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "cn=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( schemaManager, "cn=movedChild1" );
        
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( mockSession, childDn1,
            childDn2, newRdn, true );
        
        LookupOperationContext lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry originalEntry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            originalEntry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        Entry modifiedEntry = originalEntry.clone();
        modifiedEntry.add( "cn", "movedChild1" );

        moveAndRenameOpCtx.setEntry( originalEntry );
        moveAndRenameOpCtx.setModifiedEntry( modifiedEntry );
        
        // The dc=movedChild1 RDN that will be added. The dc=child1 Ryan RDN will be removed
        Map<String, List<ModDnAva>> modDnAvas = new HashMap<>();

        List<ModDnAva> modAvas = new ArrayList<>();
        modAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, newRdn.getAva() ) );
        modDnAvas.put( SchemaConstants.CN_AT_OID, modAvas );
        
        moveAndRenameOpCtx.setModifiedAvas( modDnAvas );
        moveAndRenameOpCtx.setPartition( partition );
        moveAndRenameOpCtx.setTransaction( partition.beginWriteTransaction() );

        partition.moveAndRename( moveAndRenameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "cn=movedChild1,cn=child2,ou=test,ou=system" );

        lookupContext = new LookupOperationContext( mockSession, childDn1 );
        lookupContext.setPartition( partition );
        Entry entry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            entry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( entry );
        Attribute cn = entry.get( "cn" );
        assertTrue( cn.contains( "child1" ) );
        assertTrue( cn.contains( "movedchild1" ) );
    }


    @Test
    public void testEnableRewritingFlag() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );

        // disable writing
        partition.setEnableRewriting( partition.beginReadTransaction(), false );

        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        // search works fine
        String id = partition.getEntryId( partition.beginReadTransaction(), contextEntry.getDn() );
        assertNotNull( id );

        Entry fetched = partition.fetch( partition.beginReadTransaction(), id );

        //remove the entryDn cause it is not present in the above hand made contextEntry
        fetched.removeAttributes( SchemaConstants.ENTRY_DN_AT );

        assertEquals( contextEntry, fetched );

        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );

        // but the file will be empty
        assertFalse( getEntryLdifLen( contextEntry ) == file.length() );

        partition = reloadPartition();
        assertNotExists( partition, contextEntry );

        // try adding on the reloaded partition
        partition.add( addCtx );

        // eable writing, this will let the partition write data back to disk
        partition.setEnableRewriting( partition.beginReadTransaction(), false );
        assertTrue( getEntryLdifLen( contextEntry ) == file.length() );

        file.close();
    }


    /**
     * An important test to check the stability of the partition
     * under high concurrency
     *
     * @throws Exception
     */
    @Test
    @Ignore("Taking way too much time and very timing dependent")
    public void testConcurrentOperations() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        ThreadGroup tg = new ThreadGroup( "singlefileldifpartitionTG" );

        Thread modifyTask = new Thread( tg, getModifyTask( partition ), "modifyTaskThread" );
        Thread addAndDeleteTask = new Thread( tg, getAddAndDeleteTask( partition ), "addAndDeleteTaskThread" );
        Thread renameTask = new Thread( tg, getRenameTask( partition ), "renameTaskThread" );
        Thread moveTask = new Thread( tg, getMoveTask( partition ), "moveTaskThread" );

        modifyTask.start();
        addAndDeleteTask.start();
        renameTask.start();
        moveTask.start();

        while ( tg.activeCount() > 0 )
        {
            Thread.sleep( 2000 );
        }

        // tests to be performed after the threads finish their work
        partition = reloadPartition();

        // test the work of modify thread
        LookupOperationContext lookupContext = new LookupOperationContext( mockSession );
        lookupContext.setDn( new Dn( "cn=threadDoModify,ou=test,ou=system" ) );
        lookupContext.setPartition( partition );
        Entry entry;
        
        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupContext.setTransaction( partitionTxn );

            entry = partition.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        assertNotNull( entry );
        assertEquals( "description no 999", entry.get( "description" ).getString() );
        assertExists( partition, contextEntry.getDn().getName() );
        assertExists( partition, "cn=child1,ou=test,ou=system" );
        assertExists( partition, "cn=child2,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild11,cn=child1,ou=test,ou=system" );
        assertExists( partition, "cn=grandChild12,cn=child1,ou=test,ou=system" );
        assertExists( partition, "cn=greatGrandChild111,cn=grandChild11,cn=child1,ou=test,ou=system" );
    }


    /**
     * add and keep modifying an attribute's value for 1000 times
     */
    private Runnable getModifyTask( final SingleFileLdifPartition partition )
    {
        Runnable r = new Runnable()
        {

            public void run()
            {
                int i = 0;

                try
                {
                    AddOperationContext addCtx = new AddOperationContext( mockSession );

                    Entry childEntry1 = createEntry( "cn=threadDoModify,ou=test,ou=system" );
                    childEntry1.put( "ObjectClass", "top", "person" );
                    childEntry1.put( "cn", "threadDoModify" );
                    addCtx.setEntry( childEntry1 );
                    partition.add( addCtx );

                    ModifyOperationContext modOpCtx = new ModifyOperationContext( mockSession );
                    modOpCtx.setEntry( childEntry1 );

                    List<Modification> modItems = new ArrayList<Modification>();

                    Attribute attribute = new DefaultAttribute(
                        schemaManager.lookupAttributeTypeRegistry( "description" ) );

                    Modification mod = new DefaultModification();
                    mod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
                    mod.setAttribute( attribute );

                    modItems.add( mod );
                    modOpCtx.setModItems( modItems );

                    modOpCtx.setDn( childEntry1.getDn() );

                    for ( ; i < 1000; i++ )
                    {
                        attribute.clear();
                        attribute.add( "description no " + i );
                        partition.modify( modOpCtx );
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    fail( "error while running ModifyTask at iteration count " + i );
                }

            }

        };

        return r;
    }


    /**
     * adds and deletes the same entry 1000 times
     */
    private Runnable getAddAndDeleteTask( final SingleFileLdifPartition partition )
    {
        Runnable r = new Runnable()
        {

            public void run()
            {
                int i = 0;

                try
                {
                    AddOperationContext addCtx = new AddOperationContext( mockSession );
                    DeleteOperationContext deleteCtx = new DeleteOperationContext( mockSession );

                    for ( ; i < 1000; i++ )
                    {
                        Entry entry = createEntry( "cn=threadDoAddAndDelete,ou=test,ou=system" );
                        entry.put( "ObjectClass", "top", "person" );
                        entry.put( "cn", "threadDoAddAndDelete" );
                        addCtx.setEntry( entry );

                        // add first
                        partition.add( addCtx );

                        // then delete, net affect on the count of entries at the end is zero
                        deleteCtx.setDn( entry.getDn() );
                        partition.delete( deleteCtx );
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    fail( "error while running AddAndDeleteTask at iteration count " + i );
                }
            }

        };

        return r;

    }


    /**
     * performs rename operation on an entry 1000 times, at the end of the
     * last iteration the original entry should remain with the old Dn it has
     * before starting this method
     */
    private Runnable getRenameTask( final SingleFileLdifPartition partition )
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                int i = 0;

                try
                {
                    Dn dn = new Dn( schemaManager, "cn=grandChild12,cn=child1,ou=test,ou=system" );

                    Rdn oldRdn = new Rdn( SchemaConstants.CN_AT + "=" + "grandChild12" );

                    Rdn newRdn = new Rdn( SchemaConstants.CN_AT + "=" + "renamedGrandChild12" );

                    Dn tmpDn = dn;
                    Rdn tmpRdn = newRdn;

                    for ( ; i < 500; i++ )
                    {
                        RenameOperationContext renameOpCtx = new RenameOperationContext( mockSession, tmpDn, tmpRdn,
                            true );

                        partition.rename( renameOpCtx );
                        tmpDn = dn.getParent();
                        tmpDn = tmpDn.add( newRdn );
                        tmpRdn = oldRdn;

                        renameOpCtx = new RenameOperationContext( mockSession, tmpDn, tmpRdn, true );
                        partition.rename( renameOpCtx );
                        tmpDn = dn;
                        tmpRdn = newRdn;
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    fail( "error while running RenameTask at iteration count " + i );
                }
            }

        };

        return r;
    }


    /**
     * performs move operation on an entry 1000 times, at the end of the
     * last iteration the original entry should remain at the place where it
     * was before starting this method
     */
    private Runnable getMoveTask( final SingleFileLdifPartition partition )
    {
        Runnable r = new Runnable()
        {

            public void run()
            {
                int i = 0;

                try
                {
                    Dn originalDn = new Dn( schemaManager, "cn=grandChild11,cn=child1,ou=test,ou=system" );

                    Dn originalParent = new Dn( schemaManager, "cn=child1,ou=test,ou=system" );
                    Dn newParent = new Dn( schemaManager, "cn=child2,ou=test,ou=system" );

                    Dn tmpDn = originalDn;
                    Dn tmpParentDn = newParent;

                    for ( ; i < 500; i++ )
                    {
                        MoveOperationContext moveOpCtx = new MoveOperationContext( mockSession, tmpDn, tmpParentDn );
                        partition.move( moveOpCtx );
                        tmpDn = moveOpCtx.getNewDn();
                        tmpParentDn = originalParent;

                        moveOpCtx = new MoveOperationContext( mockSession, tmpDn, tmpParentDn );
                        partition.move( moveOpCtx );
                        tmpDn = moveOpCtx.getNewDn();
                        tmpParentDn = newParent;
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    fail( "error while running MoveTask at iteration count " + i );
                }
            }

        };

        return r;
    }


    private SingleFileLdifPartition injectEntries() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );
        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );
        addCtx.setPartition( partition );
        addCtx.setTransaction( partition.beginWriteTransaction() );

        partition.add( addCtx );

        Entry childEntry1 = createEntry( "cn=child1,ou=test,ou=system" );
        childEntry1.put( "ObjectClass", "top", "person" );
        childEntry1.put( "cn", "child1" );
        childEntry1.put( "sn", "child1" );
        addCtx.setEntry( childEntry1 );

        partition.add( addCtx );

        Entry childEntry2 = createEntry( "cn=child2,ou=test,ou=system" );
        childEntry2.put( "ObjectClass", "top", "person" );
        childEntry2.put( "cn", "child2" );
        childEntry1.put( "sn", "child2" );
        addCtx.setEntry( childEntry2 );

        partition.add( addCtx );

        Entry grandChild11 = createEntry( "cn=grandChild11,cn=child1,ou=test,ou=system" );
        grandChild11.put( "ObjectClass", "top", "person" );
        grandChild11.put( "cn", "grandChild11" );
        childEntry1.put( "sn", "grandChild11" );
        addCtx.setEntry( grandChild11 );

        partition.add( addCtx );

        Entry grandChild12 = createEntry( "cn=grandChild12,cn=child1,ou=test,ou=system" );
        grandChild12.put( "ObjectClass", "top", "person" );
        grandChild12.put( "cn", "grandChild12" );
        childEntry1.put( "sn", "grandChild12" );
        addCtx.setEntry( grandChild12 );

        partition.add( addCtx );

        Entry greatGrandChild111 = createEntry( "cn=greatGrandChild111,cn=grandChild11,cn=child1,ou=test,ou=system" );
        greatGrandChild111.put( "ObjectClass", "top", "person" );
        greatGrandChild111.put( "cn", "greatGrandChild111" );
        greatGrandChild111.put( "sn", "greatGrandChild111" );
        addCtx.setEntry( greatGrandChild111 );

        partition.add( addCtx );

        return partition;
    }
}
