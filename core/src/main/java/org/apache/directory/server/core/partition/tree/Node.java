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
package org.apache.directory.server.core.partition.tree;


import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An interface for nodes in a tree designed to quickly lookup partitions.
 * Branch nodes in this tree contain other nodes.  Leaf nodes in the tree
 * contain a referrence to a partition whose suffix is the path through the 
 * nodes of the tree from the root.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Node
{
    /**
     * Tells if the implementation is a leaf node. If it's a branch node
     * then false is returned.
     *
     * @return <code>true</code> if the class is a leaf node, false otherwise.
     */
    boolean isLeaf();
    
    /**
     * Add a new Partition to the current container.
     *
     * @param name The partition name
     * @param children The PartitionStructure object (it should be a PartitionHandler)
     * @return The current PartitionStructure to which a Partition has been added.
     */
    Node addNode( String name, Node children );
    
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
    Node getChildOrThis( String name );
    
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
    Node buildNode( Node current, LdapDN dn, int index, Partition partition );
}
