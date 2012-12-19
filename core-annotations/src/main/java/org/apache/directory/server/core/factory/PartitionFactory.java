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

import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.xdbm.Index;


/**
 * A factory used to generate {@link Partition}s and their {@link Index}es.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface PartitionFactory
{

    /**
     * Creates a new Partition.
     * 
     * @param schemaManager The SchemaManager instance
     * @param id the partition id
     * @param suffix the suffix
     * @param cacheSize the cache size
     * @param workingDirectory the working directory
     * @return the partition
     * @throws Exception the exception
     */
    Partition createPartition( SchemaManager schemaManager, String id, String suffix, int cacheSize,
        File workingDirectory ) throws Exception;


    /**
     * Adds a partition-specific index to the partition.
     * 
     * @param partition the partition
     * @param attributeId the attribute id
     * @param cacheSize the cache size
     * @throws Exception the exception
     */
    void addIndex( Partition partition, String attributeId, int cacheSize ) throws Exception;

}
