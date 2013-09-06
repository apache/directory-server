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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.NameComponentNormalizer;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
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
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Unit test cases for the LDIF partition test
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifPartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( LdifPartitionTest.class );

    private static File wkdir;
    private static LdifPartition partition;
    private static SchemaManager schemaManager = null;
    private static DnFactory dnFactory;
    private static CsnFactory defaultCSNFactory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = LdifPartitionTest.class.getResource( "" ).getPath();
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

        CacheService cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache" ) );
    }


    @Before
    public void createStore() throws Exception
    {
        // setup the working directory for the store
        wkdir = folder.newFile( "db" );
        wkdir = folder.getRoot();

        // initialize the store
        // initialize the partition
        partition = new LdifPartition( schemaManager, dnFactory );
        partition.setId( "test-ldif" );
        partition.setSuffixDn( new Dn( "ou=test,ou=system" ) );
        partition.setSchemaManager( schemaManager );
        partition.setPartitionPath( wkdir.toURI() );

        partition.initialize();

        Entry entry = createEntry( "ou=test, ou=system" );

        entry.put( "objectClass", "top", "organizationalUnit" );
        entry.put( "cn", "test" );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        partition.add( addContext );

        LOG.debug( "Created new LDIF partition" );
    }


    private Entry createEntry( String dn ) throws Exception
    {
        Entry entry = new DefaultEntry( schemaManager, dn,
            SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString(),
            SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );

        Entry clonedEntry = new ClonedServerEntry( entry );

        return clonedEntry;
    }


    //-------------------------------------------------------------------------
    // Partition.add() tests
    //-------------------------------------------------------------------------
    /**
     * Test some entries creation
     *
     * @throws Exception
     */
    @Test
    public void testLdifAddEntries() throws Exception
    {
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

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

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain.ldif" ).exists() );
    }


    /**
     * Test that we can't add an existing entry
     *
     * @throws Exception
     */
    @Test
    public void testLdifAddExistingEntry() throws Exception
    {
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

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

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=test.ldif" ).exists() );
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
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

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

        DeleteOperationContext delCtx = new DeleteOperationContext( session );

        Dn dn = new Dn( schemaManager, "dc=test1,dc=test,ou=test,ou=system" );

        delCtx.setDn( dn );

        partition.delete( delCtx );

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test1.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test2" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=test2.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain.ldif" ).exists() );

        dn = new Dn( schemaManager, "dc=test2,dc=test,ou=test,ou=system" );

        delCtx.setDn( dn );

        partition.delete( delCtx );

        dn = new Dn( schemaManager, "dc=mvrdn+objectClass=domain,dc=test,ou=test,ou=system" );

        delCtx.setDn( dn );

        partition.delete( delCtx );

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test2" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test2.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=mvrdn%2bobjectclass=domain.ldif" ).exists() );
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
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

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

        SearchOperationContext searchCtx = new SearchOperationContext( session );

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
        CoreSession session = injectEntries();

        Entry childEntry1 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=child1,ou=test,ou=system" ) ) );
        Entry childEntry2 = partition.fetch( partition.getEntryId( new Dn( schemaManager,
            "dc=child2,ou=test,ou=system" ) ) );

        MoveOperationContext moveOpCtx = new MoveOperationContext( session, childEntry1.getDn(), childEntry2.getDn() );
        partition.move( moveOpCtx );

        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild11/dc=greatgrandchild111" )
            .exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=child1/dc=grandchild11/dc=greatgrandchild111.ldif" )
            .exists() );
    }


    @Test
    public void testLdifRenameAndDeleteOldDN() throws Exception
    {
        CoreSession session = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( session, childDn1, newRdn, true );
        partition.rename( renameOpCtx );

        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );

        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11/dc=greatgrandchild111" )
            .exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11/dc=greatgrandchild111.ldif" )
            .exists() );
    }


    @Test
    public void testLdifRenameAndRetainOldDN() throws Exception
    {
        CoreSession session = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Rdn newRdn = new Rdn( "dc=renamedChild1" );
        RenameOperationContext renameOpCtx = new RenameOperationContext( session, childDn1, newRdn, false );
        partition.rename( renameOpCtx );

        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );

        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11/dc=greatgrandchild111" )
            .exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=renamedchild1/dc=grandchild11/dc=greatgrandchild111.ldif" )
            .exists() );

        // the renamed LDIF must contain the old an new Rdn attribute
        String content = FileUtils.readFileToString( new File( wkdir, "ou=test,ou=system/dc=renamedchild1.ldif" ) );
        assertTrue( content.contains( "dc: child1" ) );
        assertTrue( content.contains( "dc: renamedChild1" ) );
    }


    @Test
    public void testLdifMoveAndRenameWithDeletingOldDN() throws Exception
    {
        CoreSession session = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "dc=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( SchemaConstants.DC_AT + "=" + "movedChild1" );
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( session, childDn1,
            childDn2, newRdn, true );
        partition.moveAndRename( moveAndRenameOpCtx );

        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );

        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir,
            "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11/dc=greatgrandchild111" ).exists() );
        assertTrue( new File( wkdir,
            "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11/dc=greatgrandchild111.ldif" ).exists() );
    }


    @Test
    public void testLdifMoveAndRenameRetainingOldDN() throws Exception
    {
        CoreSession session = injectEntries();

        Dn childDn1 = new Dn( schemaManager, "dc=child1,ou=test,ou=system" );

        Dn childDn2 = new Dn( schemaManager, "dc=child2,ou=test,ou=system" );

        Rdn newRdn = new Rdn( "dc=movedChild1" );
        MoveAndRenameOperationContext moveAndRenameOpCtx = new MoveAndRenameOperationContext( session, childDn1,
            childDn2, newRdn, false );
        partition.moveAndRename( moveAndRenameOpCtx );

        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );

        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir,
            "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11/dc=greatgrandchild111" ).exists() );
        assertTrue( new File( wkdir,
            "ou=test,ou=system/dc=child2/dc=movedchild1/dc=grandchild11/dc=greatgrandchild111.ldif" ).exists() );

        // the renamed LDIF must contain the old an new Rdn attribute
        String content = FileUtils
            .readFileToString( new File( wkdir, "ou=test,ou=system/dc=child2/dc=movedchild1.ldif" ) );
        assertTrue( content.contains( "dc: child1" ) );
        assertTrue( content.contains( "dc: movedChild1" ) );
    }


    /**
     * Test for DIRSERVER-1551 (LdifPartition file names on Unix and Windows).
     * Ensure that special characters (http://en.wikipedia.org/wiki/Filenames) are encoded.
     */
    @Test
    public void testSpecialCharacters() throws Exception
    {
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

        String rdnWithForbiddenChars = "dc=- -\\\"-%-&-(-)-*-\\+-/-:-\\;-\\<-\\>-?-[-\\5C-]-|-";
        String rdnWithEscapedChars = "dc=-%20-%22-%25-%26-%28-%29-%2a-%2b-%2f-%3a-%3b-%3c-%3e-%3f-%5b-%5c-%5d-%7c-";

        Entry entry1 = createEntry( rdnWithForbiddenChars + ",ou=test,ou=system" );
        entry1.put( "objectClass", "top", "domain" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/" + rdnWithEscapedChars ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/" + rdnWithEscapedChars + ".ldif" ).exists() );
    }


    /**
     * Test for DIRSERVER-1551 (LdifPartition file names on Unix and Windows).
     * Ensure that C0 control characters (http://en.wikipedia.org/wiki/Control_characters) are encoded.
     */
    @Test
    public void testControlCharacters() throws Exception
    {
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

        String rdnWithControlChars = "userPassword=-\u0000-\u0001-\u0002-\u0003-\u0004-\u0005-\u0006-\u0007" +
            "-\u0008-\u0009-\n-\u000B-\u000C-\r-\u000E-\u000F" +
            "-\u0010-\u0011-\u0012-\u0013-\u0014-\u0015-\u0016-\u0017" +
            "-\u0018-\u0019-\u001A-\u001B-\u001C-\u001D-\u001E-\u001F" +
            "-\u007F";

        String rdnWithEscapedChars = "userpassword=-%00-%01-%02-%03-%04-%05-%06-%07-%08-%09-%0a-%0b-%0c-%0d-%0e-%0f" +
            "-%10-%11-%12-%13-%14-%15-%16-%17-%18-%19-%1a-%1b-%1c-%1d-%1e-%1f-%7f";

        Entry entry1 = createEntry( rdnWithControlChars + ",ou=test,ou=system" );
        entry1.put( "objectClass", "top", "person" );
        entry1.put( "cn", "test" );
        entry1.put( "sn", "test" );
        addCtx.setEntry( entry1 );

        partition.add( addCtx );

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/" + rdnWithEscapedChars ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/" + rdnWithEscapedChars + ".ldif" ).exists() );
    }


    private CoreSession injectEntries() throws Exception
    {
        Dn adminDn = new Dn( schemaManager, "uid=admin,ou=system" );
        DirectoryService directoryService = new MockDirectoryService( 1 );
        directoryService.setSchemaManager( schemaManager );
        CoreSession session = new MockCoreSession( new LdapPrincipal( schemaManager, adminDn,
            AuthenticationLevel.STRONG ),
            directoryService );
        AddOperationContext addCtx = new AddOperationContext( session );

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

        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child2" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child2.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild11" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild11.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild12" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild12.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild11/dc=greatgrandchild111" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=child1/dc=grandchild11/dc=greatgrandchild111.ldif" )
            .exists() );

        return session;
    }
}
