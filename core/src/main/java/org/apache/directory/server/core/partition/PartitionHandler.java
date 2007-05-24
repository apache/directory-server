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

import javax.naming.NamingException;

/**
 * 
 * Stores a real Partition. This object is itself stored into a Pazrtition Container.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionHandler extends AbstractPartitionStructure
{
    /** The stored partition */
    private Partition partition;
    
    /**
     * 
     * Creates a new instance of PartitionHandler.
     *
     * @param partition The partition to store
     */
    public PartitionHandler( Partition partition )
    {
        this.partition = partition;
    }

    /**
     * @see PartitionStructure#isPartition()
     */
    public boolean isPartition()
    {
        return true;
    }
    
    /**
     * @see PartitionStructure#contains( String )
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
     * @see PartitionStructure#addPartitionHandler( String, PartitionStructure )
     */
    public PartitionStructure addPartitionHandler( String name, PartitionStructure partition )
    {
        return this;
    }
    
    /**
     * @see PartitionStructure#getPartition()
     */
    public Partition getPartition()
    {
        return partition;
    }

    /**
     * @see PartitionStructure#getPartition( String )
     */
    public PartitionStructure getPartition( String name )
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
