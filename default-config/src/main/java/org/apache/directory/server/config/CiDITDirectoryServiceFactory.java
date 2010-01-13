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

import java.io.File;
import java.util.List;

import javax.naming.directory.SearchControls;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO CiDITDirectoryServiceFactory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CiDITDirectoryServiceFactory
{

    private DirectoryService directoryService;

    private File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/server-work" );
    
    private static final Logger LOG = LoggerFactory.getLogger( CiDITDirectoryServiceFactory.class );
    
    
    /* default access */ CiDITDirectoryServiceFactory()
    {
        try
        {
            // creating the instance here so that
            // we we can set some properties like accesscontrol, anon access
            // before starting up the service
            directoryService = new DefaultDirectoryService();
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.factory.DirectoryServiceFactory#getDirectoryService()
     */
    public DirectoryService getDirectoryService() throws Exception
    {
        return directoryService;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.factory.DirectoryServiceFactory#init(java.lang.String)
     */
    public void init( String name ) throws Exception
    {
        initSchemaNConfig();
    }

    
    
    private void initSchemaNConfig() throws Exception
    {
        workDir.mkdir();
        LOG.warn( "schemaTempDir {}", workDir );

        String workingDirectory = workDir.getPath();
        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        if( schemaRepository.exists() )
        {
            FileUtils.deleteDirectory( schemaRepository );
        }
        
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        directoryService.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }
        
        LdifConfigExtractor.extract( workDir, false );
        
        LdifPartition configPartition = new LdifPartition();
        configPartition.setId( "config" );
        configPartition.setSuffix( "ou=config" );
        configPartition.setSchemaManager( schemaManager );
        configPartition.setWorkingDirectory( workingDirectory + "/config" );
        configPartition.setPartitionDir( new File( configPartition.getWorkingDirectory() ) );
        
        configPartition.initialize();

        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );
        DirectoryService dirService = cpReader.getDirectoryService();
        
        SchemaPartition schemaPartition = dirService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        ldifPartition.setWorkingDirectory( new File( workDir, schemaPartition.getId() ).getAbsolutePath() );
        schemaPartition.setWrappedPartition( ldifPartition );
        schemaPartition.setSchemaManager( schemaManager );

        dirService.setWorkingDirectory( workDir );
        dirService.setSchemaManager( schemaManager );
        dirService.startup();
        
        System.out.println( dirService.isStarted() );
        
        dirService.shutdown();
        System.out.println( dirService.isStarted() );
    }

    
    public static void main( String[] args ) throws Exception
    {
        CiDITDirectoryServiceFactory ciditDSFactory = new CiDITDirectoryServiceFactory();
        ciditDSFactory.init( null );
    }
}
