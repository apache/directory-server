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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the Partition configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class PartitionBean extends AdsBaseBean
{
    /** The Partition identifier */
    @ConfigurationElement(attributeType = "ads-partitionId", isRdn = true)
    private String partitionId;

    /** The Partition suffix */
    @ConfigurationElement(attributeType = "ads-partitionSuffix")
    private Dn partitionSuffix;

    /** Tells if the data should be flushed to disk immediately */
    @ConfigurationElement(attributeType = "ads-partitionSyncOnWrite", isOptional = true)
    private boolean partitionSyncOnWrite;

    /** The partition's ContextEntry */
    @ConfigurationElement(attributeType = "ads-contextEntry", isOptional = true)
    private String contextEntry;

    /** The list of declared indexes */
    @ConfigurationElement(objectClass = "ads-index", container = "indexes")
    private List<IndexBean> indexes = new ArrayList<IndexBean>();


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
        return partitionId;
    }


    /**
     * @param partitionId the partitionId to set
     */
    public void setPartitionId( String partitionId )
    {
        this.partitionId = partitionId;
    }


    /**
     * @return the partitionSuffix
     */
    public Dn getPartitionSuffix()
    {
        return partitionSuffix;
    }


    /**
     * @param partitionSuffix the partitionSuffix to set
     */
    public void setPartitionSuffix( Dn partitionSuffix )
    {
        this.partitionSuffix = partitionSuffix;
    }


    /**
     * @return the partitionSyncOnWrite
     */
    public boolean isPartitionSyncOnWrite()
    {
        return partitionSyncOnWrite;
    }


    /**
     * @param partitionSyncOnWrite the partitionSyncOnWrite to set
     */
    public void setPartitionSyncOnWrite( boolean partitionSyncOnWrite )
    {
        this.partitionSyncOnWrite = partitionSyncOnWrite;
    }


    /**
     * @return the indexes
     */
    public List<IndexBean> getIndexes()
    {
        return indexes;
    }


    /**
     * @param partitions the indexes to set
     */
    public void setIndexes( List<IndexBean> indexes )
    {
        this.indexes = indexes;
    }


    /**
     * @param contextEntry the contextEntry to set
     */
    public void setContextEntry( String contextEntry )
    {
        this.contextEntry = contextEntry;
    }


    /**
     * @return the contextEntry
     */
    public String getContextEntry()
    {
        return contextEntry;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  partition ID : " ).append( partitionId ).append( '\n' );
        sb.append( tabs ).append( "  suffix : " ).append( partitionSuffix.getName() ).append( '\n' );
        sb.append( toString( tabs, "  sync on write", partitionSyncOnWrite ) );
        sb.append( toString( tabs, "  contextEntry", contextEntry ) );

        sb.append( tabs ).append( "  indexes : \n" );

        if ( indexes != null )
        {
            for ( IndexBean index : indexes )
            {
                sb.append( index.toString( tabs + "    " ) );
            }
        }

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
