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


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * 
 * The Partition Container holds entries which can be either Partitions or 
 * Containers. 
 * 
 * We can see them as directories, where Partitions are the files.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BranchNode implements Node
{
    /** Stores the list of all the descendant partitions and containers */
    private Map<String, Node> children;
    
    
    /**
     * Creates a new instance of a BranchNode.
     */
    public BranchNode()
    {
        children = new HashMap<String, Node>(3);
    }

    
    /**
     * @see Node#isLeaf()
     */
    public boolean isLeaf()
    {
        return false;
    }
    
    
    /**
     * Recursively adds new nodes to the partition lookup tree data structure.  
     * When called it will add a partition to the tree in the appropriate leaf 
     * node position based on the DN passed in as an argument.
     *
     * @param current The current node having a partition added to it
     * @param dn The DN associated with the partition
     * @param index The index of the current RDN being processed 
     * @param partition The associated partition to add as a tree node
     * @return The modified tree structure.
     */
    public BranchNode recursivelyAddPartition( BranchNode current, LdapDN dn, int index, Partition partition )
    {
        String rdnAtIndex = dn.getRdn( index ).toString();
        
        if ( index == dn.size() - 1 )
        {
            return current.addNode( rdnAtIndex, new LeafNode( partition ) );
        }
        else
        {
            Node child = recursivelyAddPartition( new BranchNode(), dn, index + 1, partition );
            return current.addNode( rdnAtIndex, child );
        }
    }
    
    
    /**
     * Directly adds a new child Node to the current BranchNode.
     *
     * @param rdn The rdn of the child node to add 
     * @param child The child node to add
     * @return The modified branch node after the insertion
     */
    public BranchNode addNode( String rdn, Node child )
    {
        children.put( rdn, child );
        return this;
    }
    
    
    /**
     * Tells if the current BranchNode contains another node associated 
     * with an rdn.
     *
     * @param rdn The name we are looking for
     * @return <code>true</code> if the PartitionStructure instance contains this name
     */
    public boolean contains( String rdn )
    {
        return children.containsKey( rdn );
    }

    
    /**
     * Get's a child using an rdn string.
     * 
     * @param the rdn to use as the node key
     * @return the child node corresponding to the rdn.
     */
    public Node getChild( String rdn )
    {
        if ( children.containsKey( rdn ) )
        {
            return children.get( rdn );
        }

        return null;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( " { " );
        boolean isFirst = true;
        
        for ( Node child:children.values() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append(  ", " );
            }

            if ( child instanceof BranchNode )
            {
                sb.append( "Branch:").append( child.toString() );
            }
            else
            {
                sb.append( "Leaf: " ).append( "'" ).append( child.toString() ).append( "'" );
            }
        }

        sb.append( " } " );
        return sb.toString();
    }
}
