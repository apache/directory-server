/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.interceptor.context;


import javax.naming.NamingException;

import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A AddContextPartition context used for Interceptors. It contains all the informations
 * needed for the addContextPartition operation, and used by all the interceptors.  If 
 * it does not have a partition set for it, then it will load and instantiate it 
 * automatically using the information in the partition configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddContextPartitionOperationContext  extends EmptyOperationContext
{
    private static final Logger log = LoggerFactory.getLogger( AddContextPartitionOperationContext.class );
    
    /** The context partition configuration */
    private PartitionConfiguration partitionConfiguration;
    /** the instantiated partition class */
    private Partition partition;
       
    
    /**
     * Creates a new instance of AddContextPartitionOperationContext.
     *
     * @param entryDn The partition configuration to add
     */
    public AddContextPartitionOperationContext( PartitionConfiguration cfg )
    {
        super();
        this.partitionConfiguration = cfg;
    }
    
    
    /**
     * Creates a new instance of AddContextPartitionOperationContext.
     *
     * @param entryDn The partition configuration to add
     */
    public AddContextPartitionOperationContext( PartitionConfiguration cfg, Partition partition )
    {
        this( cfg );
        this.partition = partition;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "AddContextPartitionOperationContext for partition context '" + partitionConfiguration.getName() + "'";
    }

    
    /**
     * @return The partition configuration
     */
    public PartitionConfiguration getPartitionConfiguration()
    {
        return partitionConfiguration;
    }

    
    /**
     * Set the partition configuration
     * 
     * @param partitionConfiguration The configuration
     */
    public void setPartitionConfiguration( PartitionConfiguration partitionConfiguration )
    {
        this.partitionConfiguration = partitionConfiguration;
    }


    /**
     * Get's the partition instance.
     *
     * @return the partition to add
     */
    public Partition getPartition() throws NamingException
    {
        if ( partition != null )
        {
            return partition;
        }
        
        if ( partitionConfiguration == null )
        {
            throw new IllegalStateException( "Cannot get instance of partition without a proper " +
                    "partition configuration." );
        }
        
        Class partitionClass;
        try
        {
            partitionClass = Class.forName( partitionConfiguration.getPartitionClassName() );
        }
        catch ( ClassNotFoundException e )
        {
            String msg = "Could not load partition implementation class '" 
                + partitionConfiguration.getPartitionClassName() + "' for partition with id " 
                + partitionConfiguration.getName();
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        
        try
        {
            partition = ( Partition ) partitionClass.newInstance();
        }
        catch ( InstantiationException e )
        {
            String msg = "No default constructor in partition implementation class '" 
                + partitionConfiguration.getPartitionClassName() + "' for partition with id " 
                + partitionConfiguration.getName();
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        catch ( IllegalAccessException e )
        {
            String msg = "Default constructor for partition implementation class '" 
                + partitionConfiguration.getPartitionClassName() + "' for partition with id " 
                + partitionConfiguration.getName() + " is not publicly accessible.";
            log.error( msg );
            throw new LdapConfigurationException( msg, e );
        }
        
        return partition;
    }
}
