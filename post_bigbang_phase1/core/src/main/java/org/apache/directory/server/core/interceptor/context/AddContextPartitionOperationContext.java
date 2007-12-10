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


import org.apache.directory.server.core.partition.Partition;


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
    /** the instantiated partition class */
    private Partition partition;
       
    
    /**
     * Creates a new instance of AddContextPartitionOperationContext.
     *
     * @param partition The partition to add
     */
    public AddContextPartitionOperationContext( Partition partition )
    {
        super();
        this.partition = partition;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "AddContextPartitionOperationContext for partition context '" + partition.getId() + "'";
    }

    
    /**
     * @return The partition
     */
    public Partition getPartition()
    {
        return partition;
    }

    
    /**
     * Set the partition.
     * 
     * @param partition the partition
     */
    public void setPartitionConfiguration( Partition partition )
    {
        this.partition = partition;
    }
}
