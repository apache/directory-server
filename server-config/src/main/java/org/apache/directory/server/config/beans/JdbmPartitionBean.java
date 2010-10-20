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
package org.apache.directory.server.config.beans;

import java.util.Set;

import org.apache.directory.shared.ldap.entry.Entry;


/**
 * A class used to store the JdbmPartition configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartitionBean extends PartitionBean
{
    /** The Entry cache size for this partition */
    private int partitioncachesize = -1;

    /** Tells if the optimizer is enabled or not */
    private boolean jdbmpartitionoptimizerenabled = true;
    
    /** The set of indexed attributes */
    private Set<JdbmIndexBean<String, Entry>> jdbmindexes;

    /**
     * Create a new JdbmPartitionBean instance
     */
    public JdbmPartitionBean()
    {
    }


    /**
     * Used to specify the entry cache size for a Partition.  Various Partition
     * implementations may interpret this value in different ways: i.e. total cache
     * size limit verses the number of entries to cache.
     *
     * @param partitionCacheSize the maximum size of the cache in the number of entries
     */
    public void setPartitionCacheSize( int partitionCacheSize )
    {
        this.partitioncachesize = partitionCacheSize;
    }


    /**
     * Gets the entry cache size for this JdbmPartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    public int getPartitionCacheSize()
    {
        return partitioncachesize;
    }

    
    /**
     * @return <code>true</code> if the optimizer is enabled
     */
    public boolean isJdbmPartitionOptimizerEnabled()
    {
        return jdbmpartitionoptimizerenabled;
    }


    /**
     * Enable or disable the optimizer
     * 
     * @param jdbmPartitionOptimizerEnabled True or false
     */
    public void setJdbmPartitionOptimizerEnabled( boolean jdbmPartitionOptimizerEnabled )
    {
        this.jdbmpartitionoptimizerenabled = jdbmPartitionOptimizerEnabled;
    }
    
    
    /**
     * Stores the list of index defined for this partition
     * 
     * @param jdbmIndexes The list of indexes to store
     */
    public void setIndexedAttributes( Set<JdbmIndexBean<String, Entry>> jdbmIndexes )
    {
        this.jdbmindexes = jdbmIndexes;
    }


    /**
     * Add some indexes to this partition
     * 
     * @param jdbmIndexes The added jdbmIndexes
     */
    public void addJdbmIndexes( JdbmIndexBean<String, Entry>... jdbmIndexes )
    {
        for ( JdbmIndexBean<String, Entry> jdbmIndex : jdbmIndexes )
        {
            this.jdbmindexes.add( jdbmIndex );
        }
    }


    /**
     * Get the list of index defined for this partition
     * 
     * @return The list of defined indexes
     */
    public Set<JdbmIndexBean<String, Entry>> getJdbmIndexes()
    {
        return jdbmindexes;
    }
}
