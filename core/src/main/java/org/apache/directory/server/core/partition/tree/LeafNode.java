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


import javax.naming.NamingException;

import org.apache.directory.server.core.partition.Partition;


/**
 * A leaf node which stores a Partition. These objects are stored in BranchNodes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LeafNode extends AbstractNode
{
    /** The stored partition */
    private Partition partition;

    
    /**
     * Creates a new instance of LeafNode.
     *
     * @param partition the partition to store
     */
    public LeafNode( Partition partition )
    {
        this.partition = partition;
    }

    
    /**
     * @see Node#isLeaf()
     */
    public boolean isLeaf()
    {
        return true;
    }
    
    
    /**
     * @see Node#contains( String )
     */
    public boolean contains( String name )
    {
        try
        {
            return partition.getSuffix().getNormName().equals(  name  );
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }
    

    /**
     * @see Node#addNode( String, Node )
     */
    public Node addNode( String name, Node partition )
    {
        return this;
    }
    
    
    /**
     * @see Node#getPartition()
     */
    public Partition getPartition()
    {
        return partition;
    }
    

    /**
     * @see Node#getChildOrThis( String )
     */
    public Node getChildOrThis( String name )
    {
        try
        {
            if ( partition.getSuffix().getNormName().equals( name ) )
            {
                return this;
            }
            else
            {
                return null;
            }
        }
        catch ( NamingException ne )
        {
            return null;
        }
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        try
        {
            return partition.getSuffix().getUpName();
        }
        catch ( NamingException ne )
        {
            return "Unkown partition";
        }
    }
}
