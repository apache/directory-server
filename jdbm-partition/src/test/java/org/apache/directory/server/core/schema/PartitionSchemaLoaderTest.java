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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests the partition schema loader.
 * 
 * TODO move this to core-integ does not belong here and get rid of all the static 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@Ignore ( "Ignore this test until we get the LDIF partition in place." )
public class PartitionSchemaLoaderTest
{
    private static SchemaManager schemaManager;
    private static DirectoryService directoryService;
    private static JdbmPartition schemaPartition;


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
            fail( "Schema load failed : " + ExceptionUtils.printErrors( schemaManager.getErrors() ) );
        }

        // --------------------------------------------------------------------
        // TODO add code here to start up the LDIF schema partition
        // --------------------------------------------------------------------

        throw new NotImplementedException();
    }
    
    
    @Test public void testGetSchemas() throws Exception
    {
        PartitionSchemaLoader loader = new PartitionSchemaLoader( schemaPartition, schemaManager );
        Map<String,Schema> schemas = loader.getSchemas();
        
        Schema schema = schemas.get( "mozilla" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "mozilla" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "core" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "core" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "apachedns" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apachedns" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "autofs" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "autofs" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "apache" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apache" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;

        schema = schemas.get( "cosine" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "cosine" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "krb5kdc" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "krb5kdc" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "samba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "samba" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "collective" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "collective" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "java" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "java" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "dhcp" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "dhcp" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "corba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "corba" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "nis" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "nis" );
        //assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "inetorgperson" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "inetorgperson" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "system" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "system" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "apachemeta" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "apachemeta" );
        assertFalse( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
    }
    
    
    @Test public void testGetSchemaNames() throws Exception
    {
        PartitionSchemaLoader loader = new PartitionSchemaLoader( schemaPartition, schemaManager );
        Set<String> schemaNames = loader.getSchemaNames();
        assertTrue( schemaNames.contains( "mozilla" ) );
        assertTrue( schemaNames.contains( "core" ) );
        assertTrue( schemaNames.contains( "apachedns" ) );
        assertTrue( schemaNames.contains( "autofs" ) );
        assertTrue( schemaNames.contains( "apache" ) );
        assertTrue( schemaNames.contains( "cosine" ) );
        assertTrue( schemaNames.contains( "krb5kdc" ) );
        assertTrue( schemaNames.contains( "samba" ) );
        assertTrue( schemaNames.contains( "collective" ) );
        assertTrue( schemaNames.contains( "java" ) );
        assertTrue( schemaNames.contains( "dhcp" ) );
        assertTrue( schemaNames.contains( "corba" ) );
        assertTrue( schemaNames.contains( "nis" ) );
        assertTrue( schemaNames.contains( "inetorgperson" ) );
        assertTrue( schemaNames.contains( "system" ) );
        assertTrue( schemaNames.contains( "apachemeta" ) );
    }
}
