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
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A class used to store the BTreePartitooBean configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BTreePartitionBean
{
    /** The partition ID */
    protected String id;
    
    /** The place this partition is stored on disk */
    protected String PartitionDir;

    /** The root DN for this partition */
    protected DN suffix;
    
    /** The Entry cache size for this partition */
    protected int cacheSize = -1;

    /** Tells if the optimizer is enabled or not */
    private boolean optimizerEnabled = true;
    
    /** Tells if we should flush data to disk as soon as they are written */
    private boolean syncOnWrite = true;
    
    /** The set of indexed attributes */
    private Set<JdbmIndexBean<String, Entry>> indexedAttributes;


    /**
     * Create a new BTreePartitionBean instance
     */
    public BTreePartitionBean()
    {
    }


    /**
     * Gets the unique identifier for this partition.
     *
     * @return the unique identifier for this partition
     */
    public String getId()
    {
        return id;
    }


    /**
     * Sets the unique identifier for this partition.
     *
     * @param id the unique identifier for this partition
     */
    public void setId( String id )
    {
        this.id = id;
    }

    
    /**
     * @return the partitionDir
     */
    public String getPartitionDir() {
        return PartitionDir;
    }


    /**
     * @param partitionDir the partitionDir to set
     */
    public void setPartitionDir( String partitionDir ) {
        PartitionDir = partitionDir;
    }


    /**
     * {@inheritDoc}
     */
    public void setSuffix( DN suffix ) throws LdapInvalidDnException
    {
        this.suffix = suffix;
    }


    /**
     * {@inheritDoc}
     */
    public DN getSuffix()
    {
        return suffix;
    }


    /**
     * Used to specify the entry cache size for a Partition.  Various Partition
     * implementations may interpret this value in different ways: i.e. total cache
     * size limit verses the number of entries to cache.
     *
     * @param cacheSize the maximum size of the cache in the number of entries
     */
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Gets the entry cache size for this BTreePartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    public int getCacheSize()
    {
        return cacheSize;
    }

    
    /**
     * @return <code>true</code> if the optimizer is enabled
     */
    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }


    /**
     * Enable or disable the optimizer
     * 
     * @param optimizerEnabled True or false
     */
    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    /**
     * @return the syncOnWrite
     */
    public boolean isSyncOnWrite() 
    {
        return syncOnWrite;
    }


    /**
     * @param syncOnWrite the syncOnWrite to set
     */
    public void setSyncOnWrite( boolean syncOnWrite ) 
    {
        this.syncOnWrite = syncOnWrite;
    }
    
    
    /**
     * Stores the list of index defined for this partition
     * 
     * @param indexedAttributes The list of indexes to store
     */
    public void setIndexedAttributes( Set<JdbmIndexBean<String, Entry>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    /**
     * Add some indexes to this partition
     * 
     * @param indexes The added indexes
     */
    public void addIndexedAttributes( JdbmIndexBean<String, Entry>... indexes )
    {
        for ( JdbmIndexBean<String, Entry> index : indexes )
        {
            indexedAttributes.add( index );
        }
    }


    /**
     * Get the list of index defind for this partition
     * 
     * @return The list of defined indexes
     */
    public Set<JdbmIndexBean<String, Entry>> getIndexedAttributes()
    {
        return indexedAttributes;
    }
}
