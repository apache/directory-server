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

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.HttpServerBean;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.core.shared.DefaultDnFactory;
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
public class HttpServerConfigReaderTest
{
    private static SchemaManager schemaManager;
    private static DnFactory dnFactory;
    private static CacheService cacheService;
    
    private static File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );


    @BeforeClass
    public static void readConfig() throws Exception
    {
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
        dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache" ) );
    }


    @Test
    public void testHttpServer() throws Exception
    {
        File configDir = new File( workDir, "httpServer" ); // could be any directory, cause the config is now in a single file
        String configFile = LdifConfigExtractor.extractSingleFileConfig( configDir, "httpServer.ldif", true );

        SingleFileLdifPartition configPartition = new SingleFileLdifPartition( schemaManager, dnFactory );
        configPartition.setId( "config" );
        configPartition.setPartitionPath( new File( configFile ).toURI() );
        configPartition.setSuffixDn( new Dn( "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );

        configPartition.setCacheService( cacheService );
        configPartition.initialize();

        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );

        ConfigBean configBean = cpReader.readConfig( new Dn( schemaManager,
            "ou=servers,ads-directoryServiceId=default,ou=config" ), ConfigSchemaConstants.ADS_HTTP_SERVER_OC
            .getValue() );

        assertNotNull( configBean );
        HttpServerBean httpServerBean = ( HttpServerBean ) configBean.getDirectoryServiceBeans().get( 0 );
        assertNotNull( httpServerBean );

        configPartition.destroy();
    }
}
