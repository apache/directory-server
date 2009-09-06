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
package org.apache.directory.server.core.builder;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.avl.AvlPartition;
import org.apache.directory.server.core.schema.DefaultSchemaChangeManager;
import org.apache.directory.server.core.schema.PartitionSchemaLoader;
import org.apache.directory.server.core.schema.SchemaChangeManager;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.core.schema.SchemaPartitionDao;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * TODO DirectoryServiceBuilder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DirectoryServiceBuilder
{
    private DirectoryService directoryService = null;
    private RegistryFactory registryFactory = null;
    private BootstrapRegistryLoader bootstrapRegistryLoader = null;
    private Partition wrappedPartition = null;
    private SchemaPartition schemaPartition = null;
    private SchemaChangeManager schemaChangeManger = null;
    private PartitionSchemaLoader partitionSchemaLoader = null;
    private SchemaPartitionDao schemaPartitionDao = null;
    
    
    public DirectoryServiceBuilder() throws Exception
    {
        directoryService = new DefaultDirectoryService();
        registryFactory = new DefaultRegistryFactory();
        bootstrapRegistryLoader = new JarLdifBootstrapRegistryLoader();
        wrappedPartition = new AvlPartition();
        schemaPartition = new SchemaPartition();
    }
    
    
    public void build() throws Exception
    {
        Registries registries = registryFactory.getRegistries();
        bootstrapRegistryLoader.loadBootstrapSchema( registries );
    
        schemaPartitionDao = new SchemaPartitionDao( wrappedPartition, registries );
        partitionSchemaLoader = new PartitionSchemaLoader( wrappedPartition, registries );
        schemaChangeManger = new DefaultSchemaChangeManager( registries, partitionSchemaLoader, schemaPartitionDao );
        
        schemaPartition.setWrappedPartition( wrappedPartition );
        schemaPartition.setRegistries( registries );
        schemaPartition.setSchemaChangeManager( schemaChangeManger );
        schemaPartition.initialize();
        
        partitionSchemaLoader.loadAllEnabled( registries );
    }
    
    
    public DirectoryService getDirectoryService() throws Exception
    {
        return directoryService;
    }
}
