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


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the JdbmPartition configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartitionBean extends PartitionBean
{
    /** The Entry cache size for this partition */
    @ConfigurationElement(attributeType = "ads-partitionCacheSize", isOptional = true, defaultValue = "-1")
    private int partitionCacheSize = -1;

    /** Tells if the optimizer is enabled or not */
    @ConfigurationElement(attributeType = "ads-jdbmPartitionOptimizerEnabled", isOptional = true, defaultValue = "true")
    private boolean jdbmPartitionOptimizerEnabled = true;


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
        this.partitionCacheSize = partitionCacheSize;
    }


    /**
     * Gets the entry cache size for this JdbmPartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    public int getPartitionCacheSize()
    {
        return partitionCacheSize;
    }


    /**
     * @return <code>true</code> if the optimizer is enabled
     */
    public boolean isJdbmPartitionOptimizerEnabled()
    {
        return jdbmPartitionOptimizerEnabled;
    }


    /**
     * Enable or disable the optimizer
     * 
     * @param jdbmPartitionOptimizerEnabled True or false
     */
    public void setJdbmPartitionOptimizerEnabled( boolean jdbmPartitionOptimizerEnabled )
    {
        this.jdbmPartitionOptimizerEnabled = jdbmPartitionOptimizerEnabled;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "JdbmPartitionBean :\n" );
        sb.append( super.toString( tabs ) );
        sb.append( tabs ).append( "  partition cache size : " ).append( partitionCacheSize ).append( '\n' );
        sb.append( toString( tabs, "  jdbm partition optimizer enabled", jdbmPartitionOptimizerEnabled ) );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}
