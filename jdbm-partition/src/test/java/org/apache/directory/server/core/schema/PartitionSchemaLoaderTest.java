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
package org.apache.directory.server.core.schema;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.LdapExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests the partition schema loader.
 * 
 * TODO move this to core-integ does not belong here and get rid of all the static 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
//@Ignore ( "Ignore this test until we get the LDIF partition in place." )
public class PartitionSchemaLoaderTest
{
    private static SchemaManager schemaManager;
    private static DirectoryService directoryService;


    @BeforeClass public static void setUp() throws Exception
    {
        // setup working directory
        directoryService = new DefaultDirectoryService();
        File workingDirectory = new File( System.getProperty( "workingDirectory", System.getProperty( "user.dir" ) ) );
        
        if ( ! workingDirectory.exists() )
        {
            workingDirectory.mkdirs();
        }
        
        directoryService.setWorkingDirectory( workingDirectory );
        
        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------

        if ( workingDirectory == null )
        {
            String path = PartitionSchemaLoaderTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = new File( path.substring( 0, targetPos + 6 ) );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workingDirectory );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + LdapExceptionUtils.printErrors( schemaManager.getErrors() ) );
        }
        
        directoryService.setSchemaManager( schemaManager );
    }
    
    
    @Test 
    public void testGetSchemas() throws Exception
    {
        SchemaLoader loader = directoryService.getSchemaManager().getLoader();
        
        Schema schema = loader.getSchema( "mozilla" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "mozilla" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "core" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "core" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "apachedns" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apachedns" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "autofs" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "autofs" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "apache" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apache" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;

        schema = loader.getSchema( "cosine" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "cosine" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "krb5kdc" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "krb5kdc" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "samba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "samba" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "collective" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "collective" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "java" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "java" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "dhcp" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "dhcp" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "corba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "corba" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "nis" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "nis" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "inetorgperson" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "inetorgperson" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "system" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "system" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = loader.getSchema( "apachemeta" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apachemeta" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
    }
}
