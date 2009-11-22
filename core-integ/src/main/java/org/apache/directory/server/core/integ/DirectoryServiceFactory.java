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
package org.apache.directory.server.core.integ;


import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.schema.DefaultSchemaManager;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;


/**
 * A factory used to generate differently configured DirectoryService objects.
 * Since the DirectoryService itself is what is configured then a factory for
 * these objects acts as a configurator.  Tests can provide different factory
 * methods to be used.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface DirectoryServiceFactory
{
    /**
     * The default factory returns stock instances of a directory
     * service with smart defaults
     */
    DirectoryServiceFactory DEFAULT = new DirectoryServiceFactory() 
    {
        public DirectoryService newInstance() throws Exception
        {
            String workingDirectory = System.getProperty( "workingDirectory" );

            if ( workingDirectory == null )
            {
                String path = DirectoryServiceFactory.class.getResource( "" ).getPath();
                int targetPos = path.indexOf( "target" );
                workingDirectory = path.substring( 0, targetPos + 6 ) + "/server-work";
            }

            DirectoryService service = new DefaultDirectoryService();
            service.setWorkingDirectory( new File( workingDirectory ) );
            SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();
            
            // Init the LdifPartition
            LdifPartition ldifPartition = new LdifPartition();
            
            ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );
            
            // Extract the schema on disk (a brand new one) and load the registries
            File schemaRepository = new File( workingDirectory, "schema" );
            SchemaLdifExtractor extractor = new SchemaLdifExtractor( new File( workingDirectory ) );
            
            schemaPartition.setWrappedPartition( ldifPartition );
            
            JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
            SchemaManager schemaManager = new DefaultSchemaManager( loader );
            service.setSchemaManager( schemaManager );
            
            boolean loaded = schemaManager.loadAllEnabled();
            schemaPartition.setSchemaManager( schemaManager );
            
            List<Throwable> errors = schemaManager.getErrors();
            
            if ( errors.size() != 0 )
            {
                fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
            }

            extractor.extractOrCopy();

            service.getChangeLog().setEnabled( true );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.
            
            // Inject the System Partition
            Partition systemPartition = new JdbmPartition();
            systemPartition.setId( "system" );
            ((JdbmPartition)systemPartition).setCacheSize( 500 );
            systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
            systemPartition.setSchemaManager( schemaManager );
            ((JdbmPartition)systemPartition).setPartitionDir( new File( workingDirectory, "system" ) );
    
            // Add objectClass attribute for the system partition
            Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
            indexedAttrs.add( 
                new JdbmIndex<Object,ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );
            
            service.setSystemPartition( systemPartition );
            
            return service;
        }
    };

    DirectoryService newInstance() throws Exception;
}
