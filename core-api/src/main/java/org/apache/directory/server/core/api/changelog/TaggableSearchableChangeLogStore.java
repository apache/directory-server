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
package org.apache.directory.server.core.api.changelog;

import org.apache.directory.server.core.api.partition.Partition;



/**
 * TODO TaggableSearchableChangeLogStore.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TaggableSearchableChangeLogStore extends TaggableChangeLogStore, SearchableChangeLogStore
{
    /**
     * Get's the tag search engine for this TaggableSearchableChangeLogStore.
     *
     * @return the snapshot query engine for this store.
     */
    TagSearchEngine getTagSearchEngine();
    
    /**
     * 
     * Gets the read only Partition backed by this ChangeLogStore.
     * The init() method on this partition needs to be called by the caller.<br><br>
     * Note: This partition allows add/delete operation on the tags container
     *       The revisions container is read-only.<br><br>  
     * The default containers of the partition are
     *    <li>ou=changelog</li>
     *    <li>ou=tags,ou=changelog</li>
     *    <li>ou=revisions,ou=changelog</li> 
     * 
     * @param partitionSuffix the suffix of the partition e.x ou=chnagelog
     * @param revContainerName the container's name for holding the revisions ex. ou=revisions
     * @param tagContainerName the container's name for holding the tags ex. ou=tags
     */
    void createPartition( String partitionSuffix, String revContainerName, String tagContainerName );
    
    /**
     * Gets the partition associated with this store
     *
     * @return the partition associated with this store, null if not initialized
     */
    Partition getPartition();
    
}
