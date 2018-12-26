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


import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.config.beans.ChangePasswordServerBean;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Test class for ConfigPartitionReader
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ChangePasswordConfigReaderTest
{
    private static File workDir;

    private static SchemaManager schemaManager;
    private static DnFactory dnFactory;
    private static CacheService cacheService;


    @BeforeClass
    public static void readConfig() throws Exception
    {
        workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work-"
            + ChangePasswordConfigReaderTest.class.getSimpleName() );
        FileUtils.deleteDirectory( workDir );
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
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + Exceptions.printErrors( errors ) );
        }

        cacheService = new CacheService();
        cacheService.initialize( null );
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache", String.class, Dn.class ) );
    }


    @AfterClass
    public static void cleanup() throws Exception
    {
        FileUtils.deleteDirectory( workDir );
    }


    @Test
    public void testChangePasswordServer() throws Exception
    {
        File configDir = new File( workDir, "changePasswordServer" ); // could be any directory, cause the config is now in a single file
        String configFile = LdifConfigExtractor.extractSingleFileConfig( configDir, "changePasswordServer.ldif", true );

        SingleFileLdifPartition configPartition = new SingleFileLdifPartition( schemaManager, dnFactory );
        configPartition.setId( "config" );
        configPartition.setPartitionPath( new File( configFile ).toURI() );
        configPartition.setSuffixDn( new Dn( schemaManager, "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );

        configPartition.setCacheService( cacheService );
        configPartition.initialize();
        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );

        ConfigBean configBean = cpReader.readConfig( new Dn( schemaManager,
            "ou=servers,ads-directoryServiceId=default,ou=config" ),
            ConfigSchemaConstants.ADS_CHANGE_PASSWORD_SERVER_OC.getValue() );

        assertNotNull( configBean );
        ChangePasswordServerBean changePasswordServerBean = ( ChangePasswordServerBean ) configBean
            .getDirectoryServiceBeans().get( 0 );
        assertNotNull( changePasswordServerBean );

        configPartition.destroy( configPartition.beginReadTransaction() );
    }
}
