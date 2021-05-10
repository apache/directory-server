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
package org.apache.directory.server.operations.ldapsdk;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getNsdkWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.ReferralException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPResponseListener;

import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.util.Network;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the server to make sure standard compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=akarasulu,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: person",
        "objectClass: top",
        "uid: akarasulu",
        "cn: Alex Karasulu",
        "sn: karasulu",
        // Entry # 2
        "dn: ou=Computers,uid=akarasulu,ou=users,ou=system",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: computers",
        "description: Computers for Alex",
        "seeAlso: ou=Machines,uid=akarasulu,ou=users,ou=system",
        // Entry # 3
        "dn: uid=akarasuluref,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: akarasuluref",
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system" })
public class CompareIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( CompareIT.class );


    /**
     * Tests normal compare operation on normal non-referral entries without
     * the ManageDsaIT control.
     */
    @Test
    public void testNormalCompare() throws Exception
    {
        LdapConnection conn = getAdminConnection( getLdapServer() );

        // comparison success
        boolean response = conn.compare( "uid=akarasulu,ou=users,ou=system", "sn", "karasulu" );
        assertTrue( response );

        // comparison failure
        response = conn.compare( "uid=akarasulu,ou=users,ou=system", "sn", "lecharny" );
        assertFalse( response );

        conn.unBind();
        conn.close();
    }


    /**
     * Tests normal compare operation on normal non-referral entries without
     * the ManageDsaIT control using an attribute that does not exist in the
     * entry.
     */
    @Test
    public void testNormalCompareMissingAttribute() throws Exception
    {
        LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );

        // comparison success
        LDAPAttribute attribute = new LDAPAttribute( "sn", "karasulu" );
        assertTrue( conn.compare( "uid=akarasulu,ou=users,ou=system", attribute ) );

        // non-existing attribute
        attribute = new LDAPAttribute( "mail", "akarasulu@apache.org" );

        try
        {
            conn.compare( "uid=akarasulu,ou=users,ou=system", attribute );
            fail( "Should never get here" );
        }
        catch ( LDAPException e )
        {
            assertEquals( ResultCodeEnum.NO_SUCH_ATTRIBUTE.getValue(), e.getLDAPResultCode() );
        }

        conn.disconnect();
    }


    /**
     * Tests compare operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
        conn.setConstraints( constraints );

        // comparison success
        LDAPAttribute attribute = new LDAPAttribute( "uid", "akarasuluref" );
        assertTrue( conn.compare( "uid=akarasuluref,ou=users,ou=system", attribute, constraints ) );

        // comparison failure
        attribute = new LDAPAttribute( "uid", "elecharny" );
        assertFalse( conn.compare( "uid=akarasuluref,ou=users,ou=system", attribute, constraints ) );

        conn.disconnect();
    }


    /**
     * Tests compare operation on normal and referral entries without the
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setReferrals( false );
        conn.setConstraints( constraints );

        // comparison success
        LDAPAttribute attribute = new LDAPAttribute( "uid", "akarasulu" );
        assertTrue( conn.compare( "uid=akarasulu,ou=users,ou=system", attribute, constraints ) );

        // referrals failure
        attribute = new LDAPAttribute( "uid", "akarasulu" );
        LDAPResponseListener listener = null;
        LDAPResponse response = null;

        listener = conn.compare( "uid=akarasuluref,ou=users,ou=system", attribute, null, constraints );
        response = listener.getResponse();
        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

        conn.disconnect();
    }


    /**
     * Tests compare operation on normal and referral entries without the
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( getLdapServer() );
        SearchControls controls = new SearchControls();
        controls.setReturningAttributes( new String[0] );
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );

        // comparison success
        NamingEnumeration<SearchResult> answer = ctx.search( "uid=akarasulu,ou=users,ou=system", "(uid=akarasulu)",
            controls );
        assertTrue( answer.hasMore() );
        SearchResult result = answer.next();
        assertEquals( "", result.getName() );
        assertEquals( 0, result.getAttributes().size() );
        assertFalse( answer.hasMore() );
        answer.close();

        // referrals failure
        try
        {
            answer = ctx.search( "uid=akarasuluref,ou=users,ou=system", "(uid=akarasuluref)", controls );
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
     * Check that operation are not executed if we are now allowed to bind
     * anonymous
     * @throws LDAPException
     */
    @Test
    public void testCompareWithoutAuthentication() throws LDAPException
    {
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );
        LDAPConnection conn = new LDAPConnection();
        conn.connect( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
        LDAPAttribute attr = new LDAPAttribute( "uid", "admin" );

        try
        {
            conn.compare( "uid=admin,ou=system", attr );
            fail( "Compare success without authentication" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "no permission exception", 50, e.getLDAPResultCode() );
        }
    }


    /**
     * Tests referral handling when an ancestor is a referral.
     */
    @Test
    public void testAncestorReferral() throws Exception
    {
        LOG.debug( "" );

        LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
        LDAPConstraints constraints = new LDAPConstraints();
        conn.setConstraints( constraints );

        // referrals failure
        LDAPAttribute attribute = new LDAPAttribute( "ou", "Computers" );
        LDAPResponseListener listener = null;
        LDAPResponse response = null;

        listener = conn.compare( "ou=Computers,uid=akarasuluref,ou=users,ou=system", attribute, null, constraints );
        response = listener.getResponse();
        assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

        assertEquals( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system",
            response.getReferrals()[0] );
        assertEquals( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
        assertEquals( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

        conn.disconnect();
    }
}
