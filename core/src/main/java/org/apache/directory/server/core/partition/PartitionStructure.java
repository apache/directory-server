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
package org.apache.directory.server.core.partition;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * An interface for PartitionStructure implementations. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface PartitionStructure
{
    /**
     * Tells if the implementation is a Partition object. If it's a PartitionContainer
     * instance, it returns false.
     *
     * @return <code>true</code> if the class is an instance of PartitionHandler, false otherwise.
     */
    boolean isPartition();
    
    /**
     * Add a new Partition to the current container.
     *
     * @param name The partition name
     * @param children The PartitionStructure object (it should be a PartitionHandler)
     * @return The current PartitionStructure to which a Partition has been added.
     */
    PartitionStructure addPartitionHandler( String name, PartitionStructure children );
    
    /**
     * Tells if the current PartitionStructure contains the given partition.
     * 
     * If the PartitionStructure is an instance of PartitionHandler, returns true
     * if the partition's name equals the given name.
     *
     * If the PartitionStructure is an instance of PartitionContainer, returns true
     * if the container's children contains the given name.
     *
     * @param name The name we are looking for
     * @return <code>true</code> if the PartitionStructure instance contains this name
     */
    boolean contains( String name );
    
    /**
     * Returns the Partition associated with this name, if any, or null if the name is not 
     * found
     *
     * @param name The name we are looking for 
     * @return The associated PartitionHandler or PartitionContainer
     */
    PartitionStructure getPartition( String name );
    
    /**
     * @return Get the partition if the object is an instance of PartitionHandler, null otherwise
     */
    Partition getPartition();
    
    /**
     * Construct the global partition structure, assuming the DN passed as an argument is a partition
     * name.
     * 
     * This is a recursive method.
     *
     * @param current The current structure
     * @param dn The DN associated with the partition
     * @param index The current RDN being processed 
     * @param partition The associated partition
     * @return The modified global structure.
     */
    PartitionStructure buildPartitionStructure( PartitionStructure current, LdapDN dn, int index, Partition partition );
}
