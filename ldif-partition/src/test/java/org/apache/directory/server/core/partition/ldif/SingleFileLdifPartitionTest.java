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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockCoreSession;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
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

    private static CsnFactory defaultCSNFactory;

    private static CoreSession mockSession;

    private static Entry contextEntry;

    private static LdifReader reader = new LdifReader();

    /** the file in use during the current test method's execution */
    private File ldifFileInUse;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


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

        SingleFileLdifPartition partition = new SingleFileLdifPartition( schemaManager );
        partition.setId( "test-ldif" );
        partition.setPartitionPath( new File( fileName ).toURI() );
        partition.setSuffixDn( new Dn( "ou=test,ou=system" ) );
        partition.setSchemaManager( schemaManager );
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

        Entry fetched = partition.lookup( opCtx );

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
        
        assertEquals( entry, fetched );
    }


    private void assertExists( SingleFileLdifPartition partition, String dn ) throws LdapException
    {
        LookupOperationContext opCtx = new LookupOperationContext( mockSession );
        opCtx.setDn( new Dn( schemaManager, dn ) );

        Entry fetched = partition.lookup( opCtx );

        assertNotNull( fetched );
    }


    private void assertNotExists( SingleFileLdifPartition partition, Entry entry ) throws LdapException
    {
        LookupOperationContext opCtx = new LookupOperationContext( mockSession );
        opCtx.setDn( entry.getDn() );

        Entry fetched = partition.lookup( opCtx );

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

        partition.add( addCtx );

        String id = partition.getEntryId( contextEntry.getDn() );
        assertNotNull( id );
        assertEquals( contextEntry, partition.fetch( id ) );

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

        partition.add( addCtx );

        Entry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entryMvrdn = createEntry( "dc=mvrdn+objectClass=domain,dc=test,ou=test,ou=system" );
        entryMvrdn.put( "ObjectClass", "top", "domain" );
        entryMvrdn.put( "dc", "mvrdn" );
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

        Entry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test" );
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

        partition.add( addCtx );

        Entry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "domain" );
        entry3.put( "dc", "test" );
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

        partition.add( addCtx );

        DeleteOperationContext delOpCtx = new DeleteOperationContext( mockSession );
        delOpCtx.setDn( contextEntry.getDn() );

        partition.delete( delOpCtx );
        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );

        assertEquals( 0L, file.length() );

        file.close();

        addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );

        partition.add( addCtx );

        Entry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "dc=test1,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test1" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "dc=test2,dc=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "domain" );
        entry3.put( "dc", "test2" );
        addCtx.setEntry( entry3 );

        partition.add( addCtx );

        Entry entryMvrdn = createEntry( "dc=mvrdn+objectClass=domain,dc=test,ou=test,ou=system" );
        entryMvrdn.put( "ObjectClass", "top", "domain" );
        entryMvrdn.put( "dc", "mvrdn" );
        addCtx.setEntry( entryMvrdn );

        partition.add( addCtx );

        DeleteOperationContext delCtx = new DeleteOperationContext( mockSession );
        delCtx.setDn( entryMvrdn.getDn() );

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

        partition.add( addCtx );

        Entry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        Entry entry2 = createEntry( "dc=test1,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test1" );
        addCtx.setEntry( entry2 );

        partition.add( addCtx );

        Entry entry3 = createEntry( "dc=test2,dc=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "domain" );
        entry3.put( "dc", "test2" );
        addCtx.setEntry( entry3 );

        partition.add( addCtx );

        SearchOperationContext searchCtx = new SearchOperationContext( mockSession );

        Dn dn = new Dn( "dc=test,ou=test,ou=system" );
        dn.apply( schemaManager );
        searchCtx.setDn( dn );
        ExprNode filter = FilterParser.parse( schemaManager, "(ObjectClass=domain)" );
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        FilterNormalizingVisitor visitor = new FilterNormalizingVisitor( ncn, schemaManager );
        filter.accept( visitor );
        searchCtx.setFilter( filter );
        searchCtx.setScope( SearchScope.SUBTREE );

        EntryFilteringCursor cursor = partition.search( searchCtx );

        assertNotNull( cursor );

        Set<String> expectedDns = new HashSet<String>();
        expectedDns.add( entry1.getDn().getNormName() );
        expectedDns.add( entry2.getDn().getNormName() );
        expectedDns.add( entry3.getDn().getNormName() );

        cursor.beforeFirst();
        int nbRes = 0;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            assertNotNull( entry );
            nbRes++;

            expectedDns.remove( entry.getDn().getNormName() );
        }

        assertEquals( 3, nbRes );
        assertEquals( 0, expectedDns.size() );

        cursor.close();
    }


    @Test
    public void testLdifMoveEntry() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Entry childEntry1 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=child1,ou=test,ou=system" ) ) );
        Entry childEntry2 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=child2,ou=test,ou=system" ) ) );

        MoveOperationContext moveOpCtx = new MoveOperationContext( mockSession, childEntry1.getDn(),
            childEntry2.getDn() );
        partition.move( moveOpCtx );

        partition = reloadPartition();
        assertExists( partition, childEntry2 );
        assertNotExists( partition, childEntry1 );

        assertExists( partition, "dc=child1,dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild11,dc=child1,dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild12,dc=child1,dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=greatGrandChild111,dc=grandChild11,dc=child1,dc=child2,ou=test,ou=system" );
    }


    @Test
    public void testLdifMoveSubChildEntry() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Entry childEntry1 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=grandChild11,dc=child1,ou=test,ou=system" ) ) );
        Entry childEntry2 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=child2,ou=test,ou=system" ) ) );

        MoveOperationContext moveOpCtx = new MoveOperationContext( mockSession, childEntry1.getDn(),
            childEntry2.getDn() );
        partition.move( moveOpCtx );

        partition = reloadPartition();
        assertExists( partition, childEntry2 );
        assertNotExists( partition, childEntry1 );

        assertExists( partition, "dc=child1,ou=test,ou=system" );
        assertExists( partition, "dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild11,dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild12,dc=child1,ou=test,ou=system" );
        assertExists( partition, "dc=greatGrandChild111,dc=grandChild11,dc=child2,ou=test,ou=system" );
    }


    @Test
    public void testLdifRenameAndDeleteOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( mockSession, childDn1, newRdn, true );
        partition.rename( renameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "dc=renamedChild1,ou=test,ou=system" );

        Entry entry = partition.lookup( new LookupOperationContext( mockSession, childDn1 ) );

        assertNotNull( entry );
        assertFalse( entry.get( "dc" ).contains( "child1" ) );
    }


    @Test
    public void testLdifRenameAndRetainOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( mockSession, childDn1, newRdn, false );
        partition.rename( renameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "dc=renamedChild1,ou=test,ou=system" );

        Entry entry = partition.lookup( new LookupOperationContext( mockSession, childDn1 ) );

        assertNotNull( entry );
        assertTrue( entry.get( "dc" ).contains( "child1" ) );
    }


    @Test
    public void testLdifMoveAndRenameWithDeletingOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "dc=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "movedChild1" );
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( mockSession, childDn1,
            childDn2, newRdn, true );
        partition.moveAndRename( moveAndRenameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "dc=movedChild1,dc=child2,ou=test,ou=system" );

        Entry entry = partition.lookup( new LookupOperationContext( mockSession, childDn1 ) );

        assertNotNull( entry );
        Attribute dc = entry.get( "dc" );
        assertFalse( dc.contains( "child1" ) );
        assertTrue( dc.contains( "movedChild1" ) );
    }


    @Test
    public void testLdifMoveAndRenameRetainingOldRDN() throws Exception
    {
        SingleFileLdifPartition partition = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "dc=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "movedChild1" );
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( mockSession, childDn1,
            childDn2, newRdn, false );
        partition.moveAndRename( moveAndRenameOpCtx );

        partition = reloadPartition();

        childDn1 = new Dn( schemaManager, "dc=movedChild1,dc=child2,ou=test,ou=system" );

        Entry entry = partition.lookup( new LookupOperationContext( mockSession, childDn1 ) );

        assertNotNull( entry );
        Attribute dc = entry.get( "dc" );
        assertTrue( dc.contains( "child1" ) );
        assertTrue( dc.contains( "movedChild1" ) );
    }


    @Test
    public void testEnableRewritingFlag() throws Exception
    {
        SingleFileLdifPartition partition = createPartition( null, true );

        // disable writing
        partition.setEnableRewriting( false );

        AddOperationContext addCtx = new AddOperationContext( mockSession );
        addCtx.setEntry( contextEntry );

        partition.add( addCtx );

        // search works fine
        String id = partition.getEntryId( contextEntry.getDn() );
        assertNotNull( id );
        assertEquals( contextEntry, partition.fetch( id ) );

        RandomAccessFile file = new RandomAccessFile( new File( partition.getPartitionPath() ), "r" );

        // but the file will be empty
        assertFalse( getEntryLdifLen( contextEntry ) == file.length() );

        partition = reloadPartition();
        assertNotExists( partition, contextEntry );

        // try adding on the reloaded partition
        partition.add( addCtx );

        // eable writing, this will let the partition write data back to disk
        partition.setEnableRewriting( false );
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
        LookupOperationContext lookupCtx = new LookupOperationContext( mockSession );
        lookupCtx.setDn( new Dn( "dc=threadDoModify,ou=test,ou=system" ) );

        Entry entry = partition.lookup( lookupCtx );
        assertNotNull( entry );
        assertEquals( "description no 999", entry.get( "description" ).getString() );
        assertExists( partition, contextEntry.getDn().getName() );
        assertExists( partition, "dc=child1,ou=test,ou=system" );
        assertExists( partition, "dc=child2,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild11,dc=child1,ou=test,ou=system" );
        assertExists( partition, "dc=grandChild12,dc=child1,ou=test,ou=system" );
        assertExists( partition, "dc=greatGrandChild111,dc=grandChild11,dc=child1,ou=test,ou=system" );
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

                    Entry childEntry1 = createEntry( "dc=threadDoModify,ou=test,ou=system" );
                    childEntry1.put( "ObjectClass", "top", "domain" );
                    childEntry1.put( "dc", "threadDoModify" );
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
                        Entry entry = createEntry( "dc=threadDoAddAndDelete,ou=test,ou=system" );
                        entry.put( "ObjectClass", "top", "domain" );
                        entry.put( "dc", "threadDoAddAndDelete" );
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
                    Dn dn = new Dn( schemaManager, "dc=grandChild12,dc=child1,ou=test,ou=system" );

                    Rdn oldRdn = new Rdn( SchemaConstants.DC_AT + "=" + "grandChild12" );

                    Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "renamedGrandChild12" );

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
                    Dn originalDn = new Dn( schemaManager, "dc=grandChild11,dc=child1,ou=test,ou=system" );

                    Dn originalParent = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );
                    Dn newParent = new Dn( schemaManager, "dc=child2,ou=test,ou=system" );

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

        partition.add( addCtx );

        Entry childEntry1 = createEntry( "dc=child1,ou=test,ou=system" );
        childEntry1.put( "ObjectClass", "top", "domain" );
        childEntry1.put( "dc", "child1" );
        addCtx.setEntry( childEntry1 );

        partition.add( addCtx );

        Entry childEntry2 = createEntry( "dc=child2,ou=test,ou=system" );
        childEntry2.put( "ObjectClass", "top", "domain" );
        childEntry2.put( "dc", "child2" );
        addCtx.setEntry( childEntry2 );

        partition.add( addCtx );

        Entry grandChild11 = createEntry( "dc=grandChild11,dc=child1,ou=test,ou=system" );
        grandChild11.put( "ObjectClass", "top", "domain" );
        grandChild11.put( "dc", "grandChild11" );
        addCtx.setEntry( grandChild11 );

        partition.add( addCtx );

        Entry grandChild12 = createEntry( "dc=grandChild12,dc=child1,ou=test,ou=system" );
        grandChild12.put( "ObjectClass", "top", "domain" );
        grandChild12.put( "dc", "grandChild12" );
        addCtx.setEntry( grandChild12 );

        partition.add( addCtx );

        Entry greatGrandChild111 = createEntry( "dc=greatGrandChild111,dc=grandChild11,dc=child1,ou=test,ou=system" );
        greatGrandChild111.put( "ObjectClass", "top", "domain" );
        greatGrandChild111.put( "dc", "greatGrandChild111" );
        addCtx.setEntry( greatGrandChild111 );

        partition.add( addCtx );

        return partition;
    }
}
