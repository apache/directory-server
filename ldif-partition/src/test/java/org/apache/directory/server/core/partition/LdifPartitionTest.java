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

package org.apache.directory.server.core.partition;

import java.io.File;
import java.util.UUID;

import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.MockCoreSession;
import org.apache.directory.server.core.MockDirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.schema.loader.ldif.LdifSchemaLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Unit test cases for the LDIF partition test
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class LdifPartitionTest
{
    private static final Logger LOG = LoggerFactory.getLogger( LdifPartitionTest.class.getSimpleName() );

    private static File wkdir;
    private static LdifPartition partition;
    private static Registries registries = null;
    private static CsnFactory defaultCSNFactory;


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
        SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        registries = new Registries();
        loader.loadAllEnabled( registries );

        defaultCSNFactory = new CsnFactory( 0 );
    }

    
    @Before
    public void createStore() throws Exception
    {
        String contextEntry = 
            "dn: ou=test, ou=system\n" +
            "objectclass: organizationalUnit\n" +
            "objectclass: top\n" +
            "ou: test";

        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        // initialize the store
        // initialize the partition
        partition = new LdifPartition();
        partition.setId( "test-ldif" );
        partition.setSuffix( "ou=test,ou=system" );
        partition.setRegistries( registries );
        partition.setWorkingDirectory( wkdir.getAbsolutePath() );
        
        partition.setContextEntry( contextEntry );
        partition.initialize();

        LOG.debug( "Created new LDIF partition" );
    }


    @After
    public void destroyStore() throws Exception
    {
        // Delete the directory and its content
        FileUtils.deleteDirectory( wkdir );
    }

    
    private ClonedServerEntry createEntry( String dn ) throws Exception
    {
        ServerEntry entry = new DefaultServerEntry( registries );
        entry.setDn( new LdapDN( dn ).normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() ) );
        entry.put( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
        entry.add( SchemaConstants.ENTRY_UUID_AT, SchemaUtils.uuidToBytes( UUID.randomUUID() ) );
        
        ClonedServerEntry clonedEntry = new ClonedServerEntry( entry );

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
        LdapDN adminDn = new LdapDN( "uid=admin,ou=system" ).normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        CoreSession session = new MockCoreSession( new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), new MockDirectoryService( 1 ) );
        AddOperationContext addCtx = new AddOperationContext( session );
        
        ClonedServerEntry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );
        
        partition.add( addCtx );

        ClonedServerEntry entry2 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test" );
        addCtx.setEntry( entry2 );
        
        partition.add( addCtx );
        
        assertTrue( new File( wkdir, "ou=test,ou=system" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system.ldif" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test.ldif" ).exists() );
        assertFalse( new File( wkdir, "ou=test,ou=system/dc=test/dc=test" ).exists() );
        assertTrue( new File( wkdir, "ou=test,ou=system/dc=test/dc=test.ldif" ).exists() );
    }

    
    /**
     * Test that we can't add an existing entry 
     *
     * @throws Exception
     */
    @Test
    public void testLdifAddExistingEntry() throws Exception
    {
        LdapDN adminDn = new LdapDN( "uid=admin,ou=system" ).normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        CoreSession session = new MockCoreSession( new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), new MockDirectoryService( 1 ) );
        AddOperationContext addCtx = new AddOperationContext( session );
        
        ClonedServerEntry entry1 = createEntry( "dc=test,ou=test,ou=system" );
        entry1.put( "ObjectClass", "top", "domain" );
        entry1.put( "dc", "test" );
        addCtx.setEntry( entry1 );
        
        partition.add( addCtx );

        ClonedServerEntry entry2 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry2.put( "ObjectClass", "top", "domain" );
        entry2.put( "dc", "test" );
        addCtx.setEntry( entry2 );
        
        partition.add( addCtx );
        
        ClonedServerEntry entry3 = createEntry( "dc=test,dc=test,ou=test,ou=system" );
        entry3.put( "ObjectClass", "top", "domain" );
        entry3.put( "dc", "test" );
        addCtx.setEntry( entry3 );
        
        try
        {
            partition.add( addCtx );
            fail();
        }
        catch ( NamingException ne )
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
}
