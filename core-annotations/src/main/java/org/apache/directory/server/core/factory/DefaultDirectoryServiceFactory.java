/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.factory;


import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
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
 * A Default factory for DirectoryService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultDirectoryServiceFactory implements DirectoryServiceFactory
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDirectoryServiceFactory.class );

    /**
     * The default factory returns stock instances of a directory
     * service with smart defaults
     */
    public static final DirectoryServiceFactory DEFAULT = new DefaultDirectoryServiceFactory();

    /** The directory service. */
    private DirectoryService directoryService;

    /** The partition factory. */
    private PartitionFactory partitionFactory;


    /* default access */DefaultDirectoryServiceFactory()
    {
        try
        {
            // creating the instance here so that
            // we we can set some properties like accesscontrol, anon access
            // before starting up the service
            directoryService = new DefaultDirectoryService();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        try
        {
            String typeName = System.getProperty( "apacheds.partition.factory" );
            if ( typeName != null )
            {
                Class<? extends PartitionFactory> type = ( Class<? extends PartitionFactory> ) Class.forName( typeName );
                partitionFactory = type.newInstance();
            }
            else
            {
                partitionFactory = new JdbmPartitionFactory();
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Error instantiating custom partiton factory", e );
            throw new RuntimeException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void init( String name ) throws Exception
    {
        if ( directoryService != null && directoryService.isStarted() )
        {
            return;
        }

        build( name );
    }


    /**
     * Build the working directory
     */
    private void buildWorkingDirectory( String name )
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            workingDirectory = System.getProperty( "java.io.tmpdir" ) + "/server-work-" + name;
        }

        directoryService.setWorkingDirectory( new File( workingDirectory ) );
    }


    /**
     * Inits the schema and schema partition.
     */
    private void initSchema() throws Exception
    {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = directoryService.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy();

        schemaPartition.setWrappedPartition( ldifPartition );

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        directoryService.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager( schemaManager );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err( I18n.ERR_317, ExceptionUtils.printErrors( errors ) ) );
        }
    }


    /**
     * Inits the system partition.
     * 
     * @throws Exception the exception
     */
    private void initSystemPartition() throws Exception
    {
        // change the working directory to something that is unique
        // on the system and somewhere either under target directory
        // or somewhere in a temp area of the machine.

        // Inject the System Partition
        Partition systemPartition = partitionFactory.createPartition( "system", ServerDNConstants.SYSTEM_DN, 500,
            new File( directoryService.getWorkingDirectory(), "system" ) );
        systemPartition.setSchemaManager( directoryService.getSchemaManager() );

        partitionFactory.addIndex( systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100 );

        directoryService.setSystemPartition( systemPartition );
    }


    /**
     * Builds the directory server instance.
     * 
     * @param name the instance name
     */
    private void build( String name ) throws Exception
    {
        directoryService.setInstanceId( name );
        buildWorkingDirectory( name );

        // Erase the working directory to be sure that we don't have some
        // remaining data from a previous run
        String workingDirectoryPath = directoryService.getWorkingDirectory().getPath();
        File workingDirectory = new File( workingDirectoryPath );
        FileUtils.deleteDirectory( workingDirectory );

        // Init the service now
        initSchema();
        initSystemPartition();

        directoryService.startup();
    }


    /**
     * {@inheritDoc}
     */
    public DirectoryService getDirectoryService() throws Exception
    {
        return directoryService;
    }


    /**
     * {@inheritDoc}
     */
    public PartitionFactory getPartitionFactory() throws Exception
    {
        return partitionFactory;
    }
}
