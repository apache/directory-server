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
package org.apache.directory.mitosis.service;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import junit.framework.Assert;

import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * A test case for {@link ReplicationServiceITest}
 * 
 * @author The Apache Directory Project Team (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationServiceITest extends AbstractReplicationServiceTestCase
{
    protected void setUp() throws Exception
    {
        createReplicas( new String[] { "A", "B", "C" } );
    }

    public void testOneWay() throws Exception
    {
        String dn1 = "cn=test,ou=system";
        String dn2 = "cn=test2,ou=system";
        testOneWayBind( dn1 );
        testOneWayModify( dn1 );
        testOneWayRename( dn1, dn2, true );
        testOneWayRename( dn2, dn1, false );
        testOneWayUnbind( dn1 );
    }
    
    /**
     * Test that the entry created last will win in the case of a conflict.
     * 
     * NOTE: This test is DISABLED as there is an occasional problem when a message is acknowledged
     * too quickly, meaning no further messages can be sent until it has timed out (DIRSERVER-998).
     *
     * @throws Exception
     */
    public void disabled_testTwoWayBind() throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );

        Attributes entryA = new AttributesImpl( true );
        entryA.put( "cn", "test" );
        entryA.put( "sn", "test" );
        entryA.put( "ou", "A" );
        
        Attribute oc = new AttributeImpl( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "organizationalPerson" );

        entryA.put( oc );
        
        ctxA.bind( "cn=test,ou=system", null, entryA );
        
        // Ensure the second bind is undebatebly the second.
        Thread.sleep( 100 );

        Attributes entryB = new AttributesImpl( true );
        entryB.put( "cn", "test" );
        entryB.put( "sn", "test" );
        entryB.put( "ou", "B" );
        entryB.put( oc );
        ctxB.bind( "cn=test,ou=system", null, entryB );

        // Let both replicas replicate.  Note that a replica can only receive
        // logs from one peer at a time so we must delay between replications.
        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );
        
        replicationServices.get( "B" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertEquals( "B", getAttributeValue( ctxA, "cn=test,ou=system", "ou" ) );
        Assert.assertEquals( "B", getAttributeValue( ctxB, "cn=test,ou=system", "ou" ) );
        Assert.assertEquals( "B", getAttributeValue( ctxC, "cn=test,ou=system", "ou" ) );
    }
    
    private void testOneWayBind( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        Attributes entry = new AttributesImpl( true );
        entry.put( "cn", "test" );
        entry.put( "sn", "test" );
        
        Attribute oc = new AttributeImpl( "objectClass" );
        oc.add( "top" );
        oc.add( "person" );
        oc.add( "organizationalPerson" );
        
        entry.put( oc );
        
        ctxA.bind( dn, null, entry );

        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertNotNull( ctxA.lookup( dn ) );
        Assert.assertNotNull( ctxB.lookup( dn ) );
        Assert.assertNotNull( ctxC.lookup( dn ) );
    }

    private void testOneWayModify( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        String newValue = "anything";
        
        ctxA.modifyAttributes( dn, new ModificationItem[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "ou", newValue ))} );

        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertEquals( newValue, getAttributeValue( ctxB, dn, "ou" ) );
        Assert.assertEquals( newValue, getAttributeValue( ctxC, dn, "ou" ) );
    }

    private void testOneWayRename( String dn1, String dn2, boolean deleteRDN ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        String oldRDNValue = (String) new LdapDN(dn1).getRdn().getUpValue();
        
        ctxA.addToEnvironment( "java.naming.ldap.deleteRDN", Boolean.toString( deleteRDN ) );
        ctxA.rename( dn1, dn2 );
        
        replicationServices.get( "A" ).replicate();

        Thread.sleep( 5000 );
        
        assertNotExists( ctxA, dn1 );
        assertNotExists( ctxB, dn1 );
        assertNotExists( ctxC, dn1 );
        Assert.assertNotNull( ctxA.lookup( dn2 ) );
        Assert.assertNotNull( ctxB.lookup( dn2 ) );
        Assert.assertNotNull( ctxC.lookup( dn2 ) );

        Attribute oldRDNAttributeA = ctxA.getAttributes( dn2 ).get( new LdapDN(dn1).getRdn().getUpType() );
        Attribute oldRDNAttributeB = ctxB.getAttributes( dn2 ).get( new LdapDN(dn1).getRdn().getUpType() );
        Attribute oldRDNAttributeC = ctxC.getAttributes( dn2 ).get( new LdapDN(dn1).getRdn().getUpType() );
        boolean oldRDNExistsA = attributeContainsValue( oldRDNAttributeA, oldRDNValue );
        boolean oldRDNExistsB = attributeContainsValue( oldRDNAttributeB, oldRDNValue );
        boolean oldRDNExistsC = attributeContainsValue( oldRDNAttributeC, oldRDNValue );
        
        if ( deleteRDN )
        {
            Assert.assertFalse( oldRDNExistsA );
            Assert.assertFalse( oldRDNExistsB );
            Assert.assertFalse( oldRDNExistsC );
        }
        else
        {
            Assert.assertTrue( oldRDNExistsA );
            Assert.assertTrue( oldRDNExistsB );
            Assert.assertTrue( oldRDNExistsC );
        }
    }
    
    private void testOneWayUnbind( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        ctxA.unbind( dn );
        
        replicationServices.get( "A" ).replicate();

        Thread.sleep( 5000 );
        
        assertNotExists( ctxA, dn );
        assertNotExists( ctxB, dn );
        assertNotExists( ctxC, dn );
    }
    
    private void assertNotExists( LdapContext ctx, String dn ) throws NamingException
    {
        try
        {
            ctx.lookup( dn );
        }
        catch ( LdapNameNotFoundException e )
        {
            // This is expected so return immediately.
            return;
        }
        throw new AssertionError( "The entry exists" );
    }
    
    private String getAttributeValue( LdapContext ctx, String name, String attrName ) throws Exception
    {
        Attribute attr = ctx.getAttributes( name ).get( attrName );
        return ( String ) attr.get();
    }
    
    private boolean attributeContainsValue( Attribute attribute, Object value ) throws NamingException
    {
        boolean foundValue = false;
        for ( NamingEnumeration ne = attribute.getAll(); ne.hasMore(); )
        {
            if ( value.equals( ne.next() ) )
            {
                foundValue = true;
            }
        }
        return foundValue;
    }
}
