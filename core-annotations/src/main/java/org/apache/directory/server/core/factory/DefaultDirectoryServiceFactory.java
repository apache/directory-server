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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;


/**
 * A Default factory for DirectoryService
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultDirectoryServiceFactory implements DirectoryServiceFactory
{
    private DirectoryService directoryService;

    /**
     * The default factory returns stock instances of a directory
     * service with smart defaults
     */
    public static final DirectoryServiceFactory DEFAULT = new DefaultDirectoryServiceFactory();

    /* default access */ DefaultDirectoryServiceFactory()
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
    
    
    public void init( String name ) throws Exception
    {
        if ( ( directoryService != null ) && ( directoryService.isStarted() ) )
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
            String path = DefaultDirectoryServiceFactory.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 ) + "/server-work-" + name;
        }
        
        directoryService.setWorkingDirectory( new File( workingDirectory ) );
    }
    
    
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
            throw new Exception( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }
    }
    
    
    private void initChangeLog()
    {
        directoryService.getChangeLog().setEnabled( true );
    }
    
    
    private void initSystemPartition() throws Exception
    {
        // change the working directory to something that is unique
        // on the system and somewhere either under target directory
        // or somewhere in a temp area of the machine.

        // Inject the System Partition
        Partition systemPartition = new JdbmPartition();
        systemPartition.setId( "system" );
        ( ( JdbmPartition ) systemPartition ).setCacheSize( 500 );
        systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
        systemPartition.setSchemaManager( directoryService.getSchemaManager() );
        ( ( JdbmPartition ) systemPartition ).setPartitionDir( 
            new File( directoryService.getWorkingDirectory(), "system" ) );

        // Add objectClass attribute for the system partition
        Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
        indexedAttrs.add( new JdbmIndex<Object, ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
        ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );

        directoryService.setSystemPartition( systemPartition );
    }

    
    public void initJdbmPartition( String name, String suffix ) throws Exception
    {
        Partition partition = new JdbmPartition();
        partition.setId( name );
        partition.setSuffix( suffix );
        partition.setSchemaManager( directoryService.getSchemaManager() );
        ( ( JdbmPartition ) partition ).setPartitionDir( 
            new File( directoryService.getWorkingDirectory(), name ) );
        directoryService.addPartition( partition );

    }

    
    public void build( String name ) throws Exception
    {
        directoryService.setInstanceId( name );
        buildWorkingDirectory( name );
        
        // Erase the working directory to be sure that we don't have some
        // remaining data from a previous run
        String workingDirectoryPath = directoryService.getWorkingDirectory().getPath();
        File workingDirectory = new File( workingDirectoryPath );
        FileUtils.deleteDirectory( workingDirectory );
        
        // Init the sevice now
        initSchema();
        initChangeLog();
        initSystemPartition();
        
        directoryService.startup();
    }

    
    public DirectoryService getDirectoryService() throws Exception
    {
        return directoryService;
    }
}
