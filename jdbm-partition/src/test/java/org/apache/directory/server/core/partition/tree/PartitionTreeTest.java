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

import java.io.File;

import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.DupsContainerCursorTest;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.util.tree.DnNode;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test the partition tree manipulations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionTreeTest
{
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = DupsContainerCursorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    /**
     * Test the addition of a single partition
     */
    @Test
    public void testNewPartitionTree() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnNode<Partition> partitionLookupTree = new DnNode<Partition>();

        Dn suffix = new Dn( schemaManager, "dc=example, dc=com" );
        Partition partition = new JdbmPartition( schemaManager );
        partition.setSuffixDn( suffix );

        partitionLookupTree.add( suffix, partition );

        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree.hasChildren() );
        assertTrue( partitionLookupTree.contains( new Rdn( schemaManager, "dc=com" ) ) );

        DnNode<Partition> child = partitionLookupTree.getChild( new Rdn( schemaManager, "dc=com" ) );
        assertTrue( child.hasChildren() );
        assertTrue( child.contains( new Rdn( schemaManager, "dc=example" ) ) );

        child = child.getChild( new Rdn( schemaManager, "dc=example" ) );
        assertEquals( "dc=example, dc=com", child.getElement().getSuffixDn().getName() );
    }


    /**
     * Test the addition of a two disjointed partition
     */
    @Test
    public void testNewPartitionTree2Nodes() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnNode<Partition> partitionLookupTree = new DnNode<Partition>();

        Dn suffix1 = new Dn( schemaManager, "dc=example, dc=com" );
        Partition partition1 = new JdbmPartition( schemaManager );
        partition1.setSuffixDn( suffix1 );

        partitionLookupTree.add( suffix1, partition1 );

        Dn suffix2 = new Dn( schemaManager, "ou=system" );
        Partition partition2 = new JdbmPartition( schemaManager );
        partition2.setSuffixDn( suffix2 );

        partitionLookupTree.add( suffix2, partition2 );

        assertNotNull( partitionLookupTree );
        assertTrue( partitionLookupTree.hasChildren() );
        assertTrue( partitionLookupTree.contains( new Rdn( schemaManager, "ou=system" ) ) );
        assertTrue( partitionLookupTree.contains( new Rdn( schemaManager, "dc=com" ) ) );

        DnNode<Partition> child = partitionLookupTree.getChild( new Rdn( schemaManager, "ou=system" ) );
        assertTrue( child.isLeaf() );
        assertEquals( "ou=system", child.getElement().getSuffixDn().getName() );

        child = partitionLookupTree.getChild( new Rdn( schemaManager, "dc=com" ) );
        assertTrue( child.hasChildren() );
        assertTrue( child.contains( new Rdn( schemaManager, "dc=example" ) ) );

        child = child.getChild( new Rdn( schemaManager, "dc=example" ) );
        assertTrue( child.isLeaf() );
        assertEquals( "dc=example, dc=com", child.getElement().getSuffixDn().getName() );
    }


    /**
     * Test the addition of a two partitions with the same root
     */
    @Test
    public void testNewPartitionTree2NodesWithSameRoot() throws LdapException
    {
        /** A structure to hold all the partitions */
        DnNode<Partition> partitionLookupTree = new DnNode<Partition>();

        Dn suffix1 = new Dn( schemaManager, "dc=example1, dc=com" );
        Partition partition1 = new JdbmPartition( schemaManager );
        partition1.setSuffixDn( suffix1 );

        partitionLookupTree.add( suffix1, partition1 );

        Dn suffix2 = new Dn( schemaManager, "dc=example2, dc=com" );
        Partition partition2 = new JdbmPartition( schemaManager );
        partition2.setSuffixDn( suffix2 );

        partitionLookupTree.add( suffix2, partition2 );

        assertNotNull( partitionLookupTree );

        assertTrue( partitionLookupTree.hasChildren() );
        assertTrue( partitionLookupTree.contains( new Rdn( schemaManager, "dc=com" ) ) );

        DnNode<Partition> child = partitionLookupTree.getChild( new Rdn( schemaManager, "dc=com" ) );
        assertTrue( child.hasChildren() );

        child = partitionLookupTree.getChild( new Rdn( schemaManager, "dc=com" ) );
        assertTrue( child.hasChildren() );
        assertTrue( child.contains( new Rdn( schemaManager, "dc=example1" ) ) );
        assertTrue( child.contains( new Rdn( schemaManager, "dc=example2" ) ) );

        DnNode<Partition> child1 = child.getChild( new Rdn( schemaManager, "dc=example1" ) );
        assertTrue( child1.isLeaf() );
        assertEquals( "dc=example1, dc=com", child1.getElement().getSuffixDn().getName() );

        DnNode<Partition> child2 = child.getChild( new Rdn( schemaManager, "dc=example1" ) );
        assertTrue( child2.isLeaf() );
        assertEquals( "dc=example1, dc=com", child2.getElement().getSuffixDn().getName() );
    }
}
