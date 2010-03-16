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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.tree.DnBranchNode;
import org.apache.directory.shared.ldap.util.tree.DnLeafNode;
import org.apache.directory.shared.ldap.util.tree.DnNode;
import org.junit.Test;


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
    @Test public void testNewPartitionTree() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        DN suffix = new DN( "dc=example, dc=com" );
        Partition partition = new JdbmPartition();
        partition.setSuffix( suffix.getName() );
        
        partitionLookupTree.add( suffix, partition );
        
        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "dc=com" ) );
        
        DnNode<Partition> child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example" ) );

        child = ((DnBranchNode<Partition>)child).getChild( "dc=example" );
        assertEquals( "dc=example, dc=com", ((DnLeafNode<Partition>)child).getElement().getSuffixDn().getName() );
    }


    /**
     * Test the addition of a two disjointed partition
     */
    @Test public void testNewPartitionTree2Nodes() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        DN suffix1 = new DN( "dc=example, dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        DN suffix2 = new DN( "ou=system" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getName() );
        
        partitionLookupTree.add( suffix2, partition2 );

        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "ou=system" ) );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "dc=com" ) );
        
        DnNode<Partition> child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "ou=system" );
        assertTrue( child instanceof DnLeafNode );
        assertEquals( "ou=system", ((DnLeafNode<Partition>)child).getElement().getSuffixDn().getName() );

        child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example" ) );
        
        child = ((DnBranchNode<Partition>)child).getChild( "dc=example" );
        assertTrue( child instanceof DnLeafNode );
        assertEquals( "dc=example, dc=com", ((DnLeafNode<Partition>)child).getElement().getSuffixDn().getName() );
    }


    /**
     * Test the addition of a two overlapping partitions
     */
    @Test public void testNewPartitionTree2OverlapingNodes() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        DN suffix1 = new DN( "dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        DN suffix2 = new DN( "dc=example, dc=com" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getName() );
        
        try
        {
            partitionLookupTree.add( suffix2, partition2 );
            fail();
        }
        catch ( LdapException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Test the addition of a two partitions with the same root
     */
    @Test public void testNewPartitionTree2NodesWithSameRoot() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        DN suffix1 = new DN( "dc=example1, dc=com" );
        Partition partition1 = new JdbmPartition();
        partition1.setSuffix( suffix1.getName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        DN suffix2 = new DN( "dc=example2, dc=com" );
        Partition partition2 = new JdbmPartition();
        partition2.setSuffix( suffix2.getName() );
        
        partitionLookupTree.add( suffix2, partition2 );

        assertNotNull( partitionLookupTree );
        
        assertTrue( partitionLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "dc=com" ) );
        
        DnNode<Partition> child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );

        child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example1" ) );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example2" ) );
        
        DnNode<Partition> child1 = ((DnBranchNode<Partition>)child).getChild( "dc=example1" );
        assertTrue( child1 instanceof DnLeafNode );
        assertEquals( "dc=example1, dc=com", ((DnLeafNode<Partition>)child1).getElement().getSuffixDn().getName() );

        DnNode<Partition> child2 = ((DnBranchNode<Partition>)child).getChild( "dc=example1" );
        assertTrue( child2 instanceof DnLeafNode );
        assertEquals( "dc=example1, dc=com", ((DnLeafNode<Partition>)child2).getElement().getSuffixDn().getName() );
    }
}
