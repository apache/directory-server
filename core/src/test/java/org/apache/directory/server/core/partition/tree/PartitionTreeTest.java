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
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.XdbmPartition;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.tree.DnBranchNode;
import org.apache.directory.shared.ldap.util.tree.DnLeafNode;
import org.apache.directory.shared.ldap.util.tree.DnNode;
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
    @Test public void testNewPartitionTree() throws Exception
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        LdapDN suffix = new LdapDN( "dc=example, dc=com" );
        TestPartition partition = new TestPartition();
        partition.setSuffix( suffix.getUpName() );
        
        partitionLookupTree.add( suffix, partition );
        
        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "dc=com" ) );
        
        DnNode<Partition> child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example" ) );

        child = ((DnBranchNode<Partition>)child).getChild( "dc=example" );
        TestPartition lookedUp = ( TestPartition ) ( ( DnLeafNode<Partition> ) child ).getElement();
        assertEquals( "dc=example, dc=com", lookedUp.getSuffix() );
    }


    /**
     * Test the addition of a two disjointed partition
     */
    @Test public void testNewPartitionTree2Nodes() throws Exception
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        LdapDN suffix1 = new LdapDN( "dc=example, dc=com" );
        TestPartition partition1 = new TestPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        LdapDN suffix2 = new LdapDN( "ou=system" );
        TestPartition partition2 = new TestPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
        partitionLookupTree.add( suffix2, partition2 );

        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "ou=system" ) );
        assertTrue( ((DnBranchNode<Partition>)partitionLookupTree).contains( "dc=com" ) );
        
        DnNode<Partition> child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "ou=system" );
        assertTrue( child instanceof DnLeafNode );
        assertEquals( "ou=system", ( ( TestPartition ) ((DnLeafNode<Partition>)child).getElement() ).getSuffix() );

        child = ((DnBranchNode<Partition>)partitionLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<Partition>)child).contains( "dc=example" ) );
        
        child = ((DnBranchNode<Partition>)child).getChild( "dc=example" );
        assertTrue( child instanceof DnLeafNode );
        assertEquals( "dc=example, dc=com", ( ( TestPartition ) ((DnLeafNode<Partition>)child).getElement() ).getSuffix() );
    }


    /**
     * Test the addition of a two overlapping partitions
     */
    @Test public void testNewPartitionTree2OverlapingNodes() throws Exception
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        LdapDN suffix1 = new LdapDN( "dc=com" );
        TestPartition partition1 = new TestPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        LdapDN suffix2 = new LdapDN( "dc=example, dc=com" );
        TestPartition partition2 = new TestPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
        try
        {
            partitionLookupTree.add( suffix2, partition2 );
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
    @Test public void testNewPartitionTree2NodesWithSameRoot() throws Exception
    {
        /** A structure to hold all the partitions */
        DnBranchNode<Partition> partitionLookupTree = new DnBranchNode<Partition>();
        
        LdapDN suffix1 = new LdapDN( "dc=example1, dc=com" );
        TestPartition partition1 = new TestPartition();
        partition1.setSuffix( suffix1.getUpName() );
        
        partitionLookupTree.add( suffix1, partition1 );
        
        LdapDN suffix2 = new LdapDN( "dc=example2, dc=com" );
        TestPartition partition2 = new TestPartition();
        partition2.setSuffix( suffix2.getUpName() );
        
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
        assertEquals( "dc=example1, dc=com", ( ( TestPartition ) ((DnLeafNode<Partition>)child1).getElement() ).getSuffix() );

        DnNode<Partition> child2 = ((DnBranchNode<Partition>)child).getChild( "dc=example1" );
        assertTrue( child2 instanceof DnLeafNode );
        assertEquals( "dc=example1, dc=com", ( ( TestPartition ) ((DnLeafNode<Partition>)child2).getElement() ).getSuffix() );
    }
    
    
    class TestPartition extends XdbmPartition
    {
        /**
         * @{inhertDoc}
         */
        public void initialize( Registries registries ) throws Exception
        {
        }
        
        
        private String suffix;
        
        
        public void setSuffix( String dn )
        {
            this.suffix = dn;
        }
        
        
        public String getSuffix()
        {
            return suffix;
        }
    }
}
