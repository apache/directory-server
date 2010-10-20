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

import org.apache.directory.shared.ldap.name.DN;

/**
 * A class used to store the Partition configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class PartitionBean extends AdsBaseBean
{
    /** The Partition identifier */
    private String partitionid;
    
    /** The Partition suffix */
    private DN partitionsuffix;
    
    /** Tells if the data should be flushed to disk immediately */
    private boolean partitionsynconwrite;

    /**
     * Create a new PartitionBean instance
     */
    public PartitionBean()
    {
    }

    /**
     * @return the partitionId
     */
    public String getPartitionId()
    {
        return partitionid;
    }

    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId( String partitionId )
    {
        this.partitionid = partitionId;
    }

    /**
     * @return the partitionSuffix
     */
    public DN getPartitionSuffix()
    {
        return partitionsuffix;
    }

    /**
     * @param partitionSuffix the partitionSuffix to set
     */
    public void setPartitionSuffix( DN partitionSuffix )
    {
        this.partitionsuffix = partitionSuffix;
    }

    /**
     * @return the partitionSyncOnWrite
     */
    public boolean isPartitionSyncOnWrite()
    {
        return partitionsynconwrite;
    }

    /**
     * @param partitionSyncOnWrite the partitionSyncOnWrite to set
     */
    public void setPartitionSyncOnWrite( boolean partitionSyncOnWrite )
    {
        this.partitionsynconwrite = partitionSyncOnWrite;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "partition ID" ).append( partitionid ).append( '\n' );
        sb.append( tabs ).append( "suffix : " ).append( partitionsuffix ).append( '\n' );
        sb.append( tabs ).append( "sync on write : " );
        
        if ( partitionsynconwrite )
        {
            sb.append(  "TRUE" );
        }
        else
        {
            sb.append(  "FALSE" );
        }
        
        sb.append( '\n' );
        
        return sb.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
