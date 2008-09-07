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
package org.apache.directory.server.operations.delete;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NameNotFoundException;
import javax.naming.ReferralException;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPResponseListener;
import netscape.ldap.LDAPSearchConstraints;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Integration tests for delete operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 1
    "dn: uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "uid: akarasulu\n" +
    "cn: Alex Karasulu\n" +
    "sn: karasulu\n\n" + 
    // Entry # 2
    "dn: ou=Computers,uid=akarasulu,ou=users,ou=system\n" +
    "objectClass: organizationalUnit\n" +
    "objectClass: top\n" +
    "ou: computers\n" +
    "description: Computers for Alex\n" +
    "seeAlso: ou=Machines,uid=akarasulu,ou=users,ou=system\n\n" + 
    // Entry # 3
    "dn: uid=akarasuluref,ou=users,ou=system\n" +
    "objectClass: uidObject\n" +
    "objectClass: referral\n" +
    "objectClass: top\n" +
    "uid: akarasuluref\n" +
    "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system\n" + 
    "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system\n" +
    "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system\n\n" 
    }
)
public class DeleteIT
{
    private static final Logger LOG = LoggerFactory.getLogger( DeleteIT.class );
    
    public static LdapService ldapService;
    

    /**
     * Tests normal delete operation on normal non-referral entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testNormalDeleteFailContextNotEmpty() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        
        // delete failure on non-leaf entry
        try
        {
            conn.delete( "uid=akarasulu,ou=users,ou=system" );
            fail( "Should never get here." );
        }
        catch ( LDAPException e )
        {
            assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF.getValue(), e.getLDAPResultCode() );
        }
        
        conn.disconnect();
    }
    
    
    /**
     * Tests normal delete operation on normal non-referral entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testNormalDelete() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        
        // delete success
        conn.delete( "ou=computers,uid=akarasulu,ou=users,ou=system" );

        // delete failure non-existant entry
        try
        {
            conn.delete( "uid=elecharny,ou=users,ou=system" );
            fail( "Should never get here." );
        }
        catch ( LDAPException e )
        {
            assertEquals( ResultCodeEnum.NO_SUCH_OBJECT.getValue(), e.getLDAPResultCode() );
        }
        
        conn.disconnect();
    }
    
    
    /**
     * Tests normal delete operation on non-existent entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testDeleteNonExistent() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        
        // delete failure non-existent entry
        try
        {
            conn.delete( "uid=elecharny,ou=users,ou=system" );
            fail( "Should never get here." );
        }
        catch ( LDAPException e )
        {
            assertEquals( ResultCodeEnum.NO_SUCH_OBJECT.getValue(), e.getLDAPResultCode() );
        }
        
        conn.disconnect();
    }
    
    
    /**
     * Tests delete operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        LDAPConstraints constraints = new LDAPSearchConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        conn.setConstraints( constraints );
        
        // delete success
        conn.delete( "uid=akarasuluref,ou=users,ou=system", constraints );
        
        try
        {
            conn.read( "uid=akarasuluref,ou=users,ou=system", ( LDAPSearchConstraints ) constraints );
            fail( "Should never get here." );
        }
        catch ( LDAPException e )
        {
            assertEquals( ResultCodeEnum.NO_SUCH_OBJECT.getValue(), e.getLDAPResultCode() );
        }
        
        conn.disconnect();
    }
    
    
    /**
     * Tests delete operation on normal and referral entries without the 
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setReferrals( false );
        conn.setConstraints( constraints );
        
        // referrals failure
        LDAPResponseListener listener = null;
        LDAPResponse response = null;

        listener = conn.delete( "uid=akarasuluref,ou=users,ou=system", null, constraints );
        response = listener.getResponse();
        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

        conn.disconnect();
    }
    
    
    /**
     * Tests delete operation on normal and referral entries without the 
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals 
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( ldapService );
        
        // delete success
        ctx.destroySubcontext( "ou=computers,uid=akarasulu,ou=users,ou=system" );
        
        try
        {
            ctx.lookup( "ou=computers,uid=akarasulu,ou=users,ou=system" );
            fail( "Should never get here." );
        }
        catch ( NameNotFoundException e )
        {
        }

        // referrals failure on delete
        try
        {
            ctx.destroySubcontext( "uid=akarasuluref,ou=users,ou=system" );
            fail( "Should never get here" );
        }
        catch ( ReferralException e )
        {
            // seems JNDI only returns the first referral URL and not all so we test for it
            assertEquals( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system", e.getReferralInfo() );
        }

        ctx.close();
    }
    
    
    /**
     * Tests referral handling when an ancestor is a referral.
     */
    @Test 
    public void testAncestorReferral() throws Exception
    {
        LOG.debug( "" );

        LDAPConnection conn = getWiredConnection( ldapService );
        LDAPConstraints constraints = new LDAPConstraints();
        conn.setConstraints( constraints );

        // referrals failure
        LDAPResponseListener listener = null;
        LDAPResponse response = null;

        listener = conn.delete( "ou=Computers,uid=akarasuluref,ou=users,ou=system", null, constraints );
        response = listener.getResponse();
        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

        conn.disconnect();
    }
    
    

    /**
     * Try to delete an entry with invalid DN. Expected result code is 32
     * (NO_SUCH_OBJECT) or 34 (INVALID_DN_SYNTAX).
     */
    public void testDeleteWithIllegalName() throws Exception 
    {
        LDAPConnection conn = getWiredConnection( ldapService );
        
        try 
        {
            conn.delete("This is an illegal name,dc=example,dc=com" );
            fail( "deletion should fail" );
        } 
        catch ( LDAPException e ) 
        {
            assertTrue( e.getLDAPResultCode() == LDAPException.INVALID_DN_SYNTAX || 
                        e.getLDAPResultCode() == LDAPException.NO_SUCH_OBJECT );
        }
    }
}
