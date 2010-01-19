/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test class for ConfigPartitionReader
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigPartitionReaderTest
{

    private static DirectoryService dirService;

    private static LdapServer server;

    private static SchemaManager schemaManager;


    @BeforeClass
    public static void readConfig() throws Exception
    {
        File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );
        workDir.mkdir();

        String workingDirectory = workDir.getPath();
        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        
        if ( schemaRepository.exists() )
        {
            FileUtils.deleteDirectory( schemaRepository );
        }

        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }

        LdifConfigExtractor.extract( workDir, true );

        LdifPartition configPartition = new LdifPartition();
        configPartition.setId( "config" );
        configPartition.setSuffix( "ou=config" );
        configPartition.setSchemaManager( schemaManager );
        configPartition.setWorkingDirectory( workingDirectory + "/config" );
        configPartition.setPartitionDir( new File( configPartition.getWorkingDirectory() ) );

        configPartition.initialize();

        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );
        dirService = cpReader.getDirectoryService();

        SchemaPartition schemaPartition = dirService.getSchemaService().getSchemaPartition();

        // Init the schema partition's wrapped LdifPartition
        LdifPartition wrappedPartition = new LdifPartition();
        wrappedPartition.setWorkingDirectory( new File( workDir, schemaPartition.getId() ).getAbsolutePath() );
        schemaPartition.setWrappedPartition( wrappedPartition );
        schemaPartition.setSchemaManager( schemaManager );

        dirService.setWorkingDirectory( workDir );
        dirService.setSchemaManager( schemaManager );
        dirService.startup();

        server = cpReader.getLdapServer();
        server.setDirectoryService( dirService );

        // this is a hack to use a different port than the one
        // configured in the actual configuration data
        // in case the configured port is already in use during the test run
        Transport[] transports = server.getTransports();
        for( Transport t : transports )
        {
            int port = t.getPort();
            port = AvailablePortFinder.getNextAvailable( port );
            t.setPort( port );
            t.init();
        }

        server.start();
    }


    @AfterClass
    public static void cleanup() throws Exception
    {
        server.stop();
        dirService.shutdown();
    }


    @Test
    public void testDirService()
    {
        assertTrue( dirService.isStarted() );
        assertEquals( "default", dirService.getInstanceId() );
    }
    
    
    @Test
    public void testLdapServer()
    {
        assertTrue( server.isStarted() );
        assertEquals( dirService, server.getDirectoryService() );
    }
}
