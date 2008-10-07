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
package org.apache.directory.shared.ldap.util.tree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test the Dn Nodes 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TestDnNode
{
    /** A structure to hold all the DNs */
    DnBranchNode<LdapDN> dnLookupTree;
    LdapDN dn1;
    LdapDN dn2;
    LdapDN dn3;
    LdapDN dn4;
    LdapDN dn5;
    LdapDN dn6;

    /**
     * Create the elements we will test
     */
    @Before
    public void setUp()  throws Exception
    {
        dnLookupTree = new DnBranchNode<LdapDN>();
        
        dn1 = new LdapDN( "dc=directory,dc=apache,dc=org" );
        dn2 = new LdapDN( "dc=mina,dc=apache,dc=org" );
        dn3 = new LdapDN( "dc=test,dc=com" );
        dn4 = new LdapDN( "dc=acme,dc=com" );
        dn5 = new LdapDN( "dc=acme,c=us,dc=com" );
        dn6 = new LdapDN( "dc=empty" );

        dnLookupTree.add( dn1, dn1 );
        dnLookupTree.add( dn2, dn2 );
        dnLookupTree.add( dn3, dn3 );
        dnLookupTree.add( dn4, dn4 );
        dnLookupTree.add( dn5, dn5 );
        dnLookupTree.add( dn6, dn6 );
    }
    
    
    /**
     * Clean the tree
     *
     */
    @After
    public void tearDown()
    {
        dnLookupTree = null;
    }
    
    
    /**
     * Test the addition of a single DN
     */
    @Test public void testNewTree() throws NamingException
    {
        /** A structure to hold all the DNs */
        DnBranchNode<LdapDN> dnLookupTree = new DnBranchNode<LdapDN>();
        
        LdapDN suffix = new LdapDN( "dc=example, dc=com" );
        
        dnLookupTree.add( suffix, suffix );
        
        assertNotNull( dnLookupTree );
        assertTrue( dnLookupTree instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<LdapDN>)dnLookupTree).contains( "dc=com" ) );
        
        DnNode<LdapDN> child = ((DnBranchNode<LdapDN>)dnLookupTree).getChild( "dc=com" );
        assertTrue( child instanceof DnBranchNode );
        assertTrue( ((DnBranchNode<LdapDN>)child).contains( "dc=example" ) );

        child = ((DnBranchNode<LdapDN>)child).getChild( "dc=example" );
        assertEquals( suffix, ((DnLeafNode<LdapDN>)child).getElement() );
    }


    /**
     * Test additions in a tree 
     */
    @Test
    public void testComplexTreeCreation() throws NamingException
    {
        
        assertTrue( dnLookupTree.hasParentElement( dn1 ) );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn3 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertTrue( dnLookupTree.hasParentElement( dn5 ) );
        assertTrue( dnLookupTree.hasParentElement( dn6 ) );
        assertTrue( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );
    }
    
    
    /**
     * Test that we can add an entry twice without any problem
     * TODO testAddEntryTwice.
     *
     */
    @Test
    public void testAddEntryTwice() throws NamingException
    {
        assertEquals( 6, dnLookupTree.size() );

        dnLookupTree.add( dn1, dn1 );
        
        assertEquals( 6, dnLookupTree.size() );
    }

    /**
     * test the deletion of elements in a tree
     */
    @Test
    public void testComplexTreeDeletion() throws NamingException
    {
        dnLookupTree.remove( dn3 );
        assertEquals( 5, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn1 ) );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertTrue( dnLookupTree.hasParentElement( dn5 ) );
        assertTrue( dnLookupTree.hasParentElement( dn6 ) );
        assertTrue( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        dnLookupTree.remove( dn6 );
        assertEquals( 4, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn1 ) );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertTrue( dnLookupTree.hasParentElement( dn5 ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        dnLookupTree.remove( dn1 );
        assertEquals( 3, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertTrue( dnLookupTree.hasParentElement( dn5 ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        // Should not change anything
        dnLookupTree.remove( dn3 );
        assertEquals( 3, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertTrue( dnLookupTree.hasParentElement( dn5 ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        dnLookupTree.remove( dn5 );
        assertEquals( 2, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn2 ) );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        dnLookupTree.remove( dn2 );
        assertEquals( 1, dnLookupTree.size() );
        assertTrue( dnLookupTree.hasParentElement( dn4 ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );

        dnLookupTree.remove( dn4 );
        assertEquals( 0, dnLookupTree.size() );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN( "dc=nothing,dc=empty" ) ) );
        assertFalse( dnLookupTree.hasParentElement( new LdapDN(  "dc=directory,dc=apache,dc=root" ) ) );
    }
}
