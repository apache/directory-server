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

package org.apache.directory.server.core.partition.tree;

import javax.naming.NamingException;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Test the partition tree manipulations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PartitionTreeTest
{
    /**
     * Test the addition of a single partition
     */
    @Test public void testNewPartitionTree() throws NamingException
    {
        /** A structure to hold all the partitions */
        BranchNode partitionLookupTree = new BranchNode();
        
        LdapDN suffix = new LdapDN( "dc=example, dc=com" );
        Partition partition = new JdbmPartition();
        partition.setSuffix( suffix.getUpName() );
        
        Node node = partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix, 0, partition );
        
        assertNotNull( node );
        assertTrue( node instanceof BranchNode );
        assertTrue( ((BranchNode)node).contains( "dc=com" ) );
        
        Node child = ((BranchNode)node).getChild( "dc=com" );
        assertTrue( child instanceof BranchNode );
        assertTrue( ((BranchNode)child).contains( "dc=example" ) );

        child = ((BranchNode)child).getChild( "dc=example" );
        assertEquals( "dc=example, dc=com", ((LeafNode)child).getPartition().getSuffix() );
    }


    /**
     * Test the addition of a two disjointed partition
     */
    @Test public void testNewPartitionTree2Nodes() throws NamingException
    {
        /** A structure to hold all the partitions */
        BranchNode partitionLookupTree = new BranchNode();
        
        LdapDN suffix1 = new LdapDN( "dc=example, dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        Node node = partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix1, 0, partition1 );
        
        LdapDN suffix2 = new LdapDN( "ou=system" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
        node = partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix2, 0, partition2 );

        assertNotNull( node );
        assertTrue( node instanceof BranchNode );
        assertTrue( ((BranchNode)node).contains( "ou=system" ) );
        assertTrue( ((BranchNode)node).contains( "dc=com" ) );
        
        Node child = ((BranchNode)node).getChild( "ou=system" );
        assertTrue( child instanceof LeafNode );
        assertEquals( "ou=system", ((LeafNode)child).getPartition().getSuffix() );

        child = ((BranchNode)node).getChild( "dc=com" );
        assertTrue( child instanceof BranchNode );
        assertTrue( ((BranchNode)child).contains( "dc=example" ) );
        
        child = ((BranchNode)child).getChild( "dc=example" );
        assertTrue( child instanceof LeafNode );
        assertEquals( "dc=example, dc=com", ((LeafNode)child).getPartition().getSuffix() );
    }


    /**
     * Test the addition of a two overlapping partitions
     */
    @Test public void testNewPartitionTree2OverlapingNodes() throws NamingException
    {
        /** A structure to hold all the partitions */
        BranchNode partitionLookupTree = new BranchNode();
        
        LdapDN suffix1 = new LdapDN( "dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix1, 0, partition1 );
        
        LdapDN suffix2 = new LdapDN( "dc=example, dc=com" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
        try
        {
            partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix2, 0, partition2 );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of a two partitions with the same root
     */
    @Test public void testNewPartitionTree2NodesWithSameRoot() throws NamingException
    {
        /** A structure to hold all the partitions */
        BranchNode partitionLookupTree = new BranchNode();
        
        LdapDN suffix1 = new LdapDN( "dc=example1, dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix1, 0, partition1 );
        
        LdapDN suffix2 = new LdapDN( "dc=example2, dc=com" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
        Node node = partitionLookupTree.recursivelyAddPartition( partitionLookupTree, suffix2, 0, partition2 );

        assertNotNull( node );
        
        assertTrue( node instanceof BranchNode );
        assertTrue( ((BranchNode)node).contains( "dc=com" ) );
        
        Node child = ((BranchNode)node).getChild( "dc=com" );
        assertTrue( child instanceof BranchNode );

        child = ((BranchNode)node).getChild( "dc=com" );
        assertTrue( child instanceof BranchNode );
        assertTrue( ((BranchNode)child).contains( "dc=example1" ) );
        assertTrue( ((BranchNode)child).contains( "dc=example2" ) );
        
        Node child1 = ((BranchNode)child).getChild( "dc=example1" );
        assertTrue( child1 instanceof LeafNode );
        assertEquals( "dc=example1, dc=com", ((LeafNode)child1).getPartition().getSuffix() );

        Node child2 = ((BranchNode)child).getChild( "dc=example1" );
        assertTrue( child2 instanceof LeafNode );
        assertEquals( "dc=example1, dc=com", ((LeafNode)child2).getPartition().getSuffix() );
    }
}
