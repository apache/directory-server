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

package org.apache.directory.server.core.factory;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
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
public class CiDITDirectoryServiceFactory implements DirectoryServiceFactory
{

    private DirectoryService directoryService;
    
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
        File schemaTempDir = initSchema();
        initConfigPartition();
    }

    
    private void initConfigPartition()
    {
        // TODO create a LdifConfigLoader
        LdifPartition configPartition = new LdifPartition();
    }
    
    
    private File initSchema() throws Exception
    {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        File schemaTempDir = new File( System.getProperty( "java.io.tmpdir" ) + "/schema-" + UUID.randomUUID().toString() );
        schemaTempDir.mkdir();
        
        LOG.warn( "schemaTempDir {}", schemaTempDir );
        
        String workingDirectory = schemaTempDir.getPath();
        ldifPartition.setWorkingDirectory( workingDirectory );

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
        
        return schemaTempDir;
    }

    public static void main( String[] args ) throws Exception
    {
        CiDITDirectoryServiceFactory ciditDSFactory = new CiDITDirectoryServiceFactory();
        ciditDSFactory.init( null );
    }
}
