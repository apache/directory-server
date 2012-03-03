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
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Default factory for DirectoryService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultDirectoryServiceFactory implements DirectoryServiceFactory
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDirectoryServiceFactory.class );

    /** The directory service. */
    private DirectoryService directoryService;

    /** The partition factory. */
    private PartitionFactory partitionFactory;


    public DefaultDirectoryServiceFactory()
    {
        try
        {
            // creating the instance here so that
            // we we can set some properties like accesscontrol, anon access
            // before starting up the service
            directoryService = new DefaultDirectoryService();

            // no need to register a shutdown hook during tests because this 
            // starts a lot of threads and slows down test execution
            directoryService.setShutdownHookEnabled( false );
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
        if ( ( directoryService != null ) && directoryService.isStarted() )
        {
            return;
        }

        build( name );
    }


    /**
     * Build the working directory
     */
    private void buildInstanceDirectory( String name ) throws IOException
    {
        String instanceDirectory = System.getProperty( "workingDirectory" );

        if ( instanceDirectory == null )
        {
            instanceDirectory = System.getProperty( "java.io.tmpdir" ) + "/server-work-" + name;
        }

        InstanceLayout instanceLayout = new InstanceLayout( instanceDirectory );

        if ( instanceLayout.getInstanceDirectory().exists() )
        {
            try
            {
                FileUtils.deleteDirectory( instanceLayout.getInstanceDirectory() );
            }
            catch ( IOException e )
            {
                LOG.warn( "couldn't delete the instance directory before initializing the DirectoryService", e );
            }
        }

        directoryService.setInstanceLayout( instanceLayout );
    }


    /**
     * Inits the schema and schema partition.
     */
    private void initSchema() throws Exception
    {
        File workingDirectory = directoryService.getInstanceLayout().getPartitionsDirectory();

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workingDirectory );

        try
        {
            extractor.extractOrCopy();
        }
        catch ( IOException ioe )
        {
            // The schema has already been extracted, bypass
        }

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        directoryService.setSchemaManager( schemaManager );

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition( schemaManager, getTxnManagerFactory(),
            getOperationExecutionManagerFactory() );
        ldifPartition.setPartitionPath( new File( workingDirectory, "schema" ).toURI() );
        SchemaPartition schemaPartition = new SchemaPartition( schemaManager );
        schemaPartition.setWrappedPartition( ldifPartition );
        directoryService.setSchemaPartition( schemaPartition );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err( I18n.ERR_317, Exceptions.printErrors( errors ) ) );
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
        Partition systemPartition = partitionFactory.createPartition( directoryService.getSchemaManager(),
            "system", ServerDNConstants.SYSTEM_DN, 500,
            new File( directoryService.getInstanceLayout().getPartitionsDirectory(), "system" ),
            getTxnManagerFactory(), getOperationExecutionManagerFactory() );
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
        buildInstanceDirectory( name );

        // Initialize the txn subsystem and the operation execution manager
        TxnManagerFactory txnManagerFactory = new TxnManagerFactory( directoryService.getInstanceLayout()
            .getTxnLogDirectory().getPath(),
            DefaultDirectoryService.TXN_LOG_BUFFER_SIZE, DefaultDirectoryService.TXN_LOG_FILE_SIZE );
        OperationExecutionManagerFactory executionManagerFactory = new OperationExecutionManagerFactory(
            txnManagerFactory );

        ( ( DefaultDirectoryService ) directoryService ).setTxnManagerFactory( txnManagerFactory );
        ( ( DefaultDirectoryService ) directoryService ).setExecutionManagerFactory( executionManagerFactory );

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


    /**
     * {@inheritDoc}
     */
    public TxnManagerFactory getTxnManagerFactory()
    {
        return ( ( DefaultDirectoryService ) directoryService ).getTxnManagerFactory();
    }


    /**
     * {@inheritDoc}
     */
    public OperationExecutionManagerFactory getOperationExecutionManagerFactory()
    {
        return ( ( DefaultDirectoryService ) directoryService ).getOperationExecutionManagerFactory();
    }

}
