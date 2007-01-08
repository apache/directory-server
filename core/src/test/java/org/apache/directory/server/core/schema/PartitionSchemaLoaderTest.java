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


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.DirectoryServiceListener;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.MutableStartupConfiguration;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.bootstrap.partition.SchemaPartitionExtractor;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;

import junit.framework.TestCase;


/**
 * Tests the partition schema loader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionSchemaLoaderTest extends TestCase
{
    private Registries registries;
    private MutableStartupConfiguration startupConfiguration = new MutableStartupConfiguration();
    private DirectoryServiceConfiguration configuration;
    private JdbmPartition schemaPartition;


    public void setUp() throws Exception
    {
        super.setUp();

        // setup working directory
        File workingDirectory = new File( System.getProperty( "workingDirectory" ) );
        if ( ! workingDirectory.exists() )
        {
            workingDirectory.mkdirs();
        }
        startupConfiguration.setWorkingDirectory( workingDirectory );
        
        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------

        // setup temporary loader and temp registry 
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        registries = new DefaultRegistries( "bootstrap", loader );
        
        // load essential bootstrap schemas 
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );
        
        // run referential integrity tests
        java.util.List errors = registries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();
            e.setRootCause( ( Throwable ) errors.get( 0 ) );
            throw e;
        }

        SerializableComparator.setRegistry( registries.getComparatorRegistry() );
        configuration = new TestConfiguration( registries, startupConfiguration );
        
        // --------------------------------------------------------------------
        // If not present extract schema partition from jar
        // --------------------------------------------------------------------

        SchemaPartitionExtractor extractor = null; 
        try
        {
            extractor = new SchemaPartitionExtractor( startupConfiguration.getWorkingDirectory() );
            extractor.extract();
        }
        catch ( IOException e )
        {
            NamingException ne = new NamingException( "Failed to extract pre-loaded schema partition." );
            ne.setRootCause( e );
            throw ne;
        }
        
        // --------------------------------------------------------------------
        // Initialize schema partition
        // --------------------------------------------------------------------
        
        MutablePartitionConfiguration pc = new MutablePartitionConfiguration();
        pc.setName( "schema" );
        pc.setCacheSize( 1000 );
        pc.setIndexedAttributes( extractor.getDbFileListing().getIndexedAttributes() );
        pc.setOptimizerEnabled( true );
        pc.setSuffix( "ou=schema" );
        
        Attributes entry = new LockableAttributesImpl();
        entry.put( "objectClass", "top" );
        entry.get( "objectClass" ).add( "organizationalUnit" );
        entry.put( "ou", "schema" );
        pc.setContextEntry( entry );
        schemaPartition = new JdbmPartition();
        schemaPartition.init( configuration, pc );
    }
    
    
    public void testGetSchemas() throws NamingException
    {
        PartitionSchemaLoader loader = new PartitionSchemaLoader( schemaPartition, registries );
        Map<String,Schema> schemas = loader.getSchemas();
        
        Schema schema = schemas.get( "mozilla" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "mozilla" );
        assertTrue( schema.isDisabled() );
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
        assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "autofs" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "autofs" );
        assertTrue( schema.isDisabled() );
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
        assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "samba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "samba" );
        assertTrue( schema.isDisabled() );
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
        assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "corba" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "corba" );
        assertTrue( schema.isDisabled() );
        assertEquals( schema.getOwner(), "uid=admin,ou=system" );
        schema = null;
        
        schema = schemas.get( "nis" );
        assertNotNull( schema );
        assertEquals( schema.getSchemaName(), "nis" );
        assertTrue( schema.isDisabled() );
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
    
    
    public void testGetSchemaNames() throws NamingException
    {
        PartitionSchemaLoader loader = new PartitionSchemaLoader( schemaPartition, registries );
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
    
    
    class TestConfiguration implements DirectoryServiceConfiguration
    {
        Registries registries;
        StartupConfiguration startupConfiguration;
        
        
        public TestConfiguration( Registries registries, StartupConfiguration startupConfiguration )
        {
            this.registries = registries;
            this.startupConfiguration = startupConfiguration;
        }
        
        public Hashtable getEnvironment()
        {
            return new Hashtable();
        }

        public String getInstanceId()
        {
            return "default";
        }

        public InterceptorChain getInterceptorChain()
        {
            return null;
        }

        public PartitionNexus getPartitionNexus()
        {
            return null;
        }

        public Registries getRegistries()
        {
            return registries;
        }

        public DirectoryService getService()
        {
            return null;
        }

        public DirectoryServiceListener getServiceListener()
        {
            return null;
        }

        public StartupConfiguration getStartupConfiguration()
        {
            return startupConfiguration;
        }

        public boolean isFirstStart()
        {
            return false;
        }

        public SchemaManager getSchemaManager()
        {
            return null;
        }
    }
}
