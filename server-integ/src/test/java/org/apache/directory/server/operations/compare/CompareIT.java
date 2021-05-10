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
package org.apache.directory.server.operations.compare;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
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

import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.CompareRequest;
import org.apache.directory.api.ldap.model.message.CompareRequestImpl;
import org.apache.directory.api.ldap.model.message.CompareResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Assertions;
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
        try ( LdapConnection conn = getAdminConnection( getLdapServer() ) )
        {
            // comparison success
            boolean response = conn.compare( "uid=akarasulu,ou=users,ou=system", "sn", "karasulu" );
            assertTrue( response );
    
            // comparison failure
            response = conn.compare( "uid=akarasulu,ou=users,ou=system", "sn", "lecharny" );
            assertFalse( response );
    
            conn.unBind();
        }
    }


    /**
     * Tests normal compare operation on normal non-referral entries without
     * the ManageDsaIT control using an attribute that does not exist in the
     * entry.
     */
    @Test
    public void testNormalCompareMissingAttribute() throws Exception
    {
        try ( LdapConnection conn = getWiredConnection( getLdapServer() ) )
        {
            // comparison success
            assertTrue( conn.compare( "uid=akarasulu,ou=users,ou=system", "sn", "karasulu" ) );
    
            // non-existing attribute
            try
            {
                conn.compare( "uid=akarasulu,ou=users,ou=system", "mail", "akarasulu@apache.org" );
                fail( "Should never get here" );
            }
            catch ( LdapOperationException e )
            {
                assertEquals( ResultCodeEnum.NO_SUCH_ATTRIBUTE, e.getResultCode() );
            }
        }
    }


    /**
     * Tests compare operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        try ( LdapConnection conn = getWiredConnection( getLdapServer() ) )
        {
            // comparison success
            assertTrue( conn.compare( "uid=akarasuluref,ou=users,ou=system", "uid", "akarasuluref" ) );
    
            // comparison failure
            CompareRequest compareRequest = new CompareRequestImpl();
            compareRequest.setName( new Dn( "uid=akarasuluref,ou=users,ou=system" ) );
            compareRequest.setAttributeId( "uid" );
            compareRequest.setAssertionValue( "elecharny" );
            ManageDsaIT manageDSAIT = new ManageDsaITImpl();
            manageDSAIT.setCritical( true );
            compareRequest.addControl( manageDSAIT );
    
            CompareResponse compareResponse = conn.compare( compareRequest );
            assertEquals( ResultCodeEnum.COMPARE_FALSE, compareResponse.getLdapResult().getResultCode() );
        }
    }


    /**
     * Tests compare operation on normal and referral entries without the
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        try ( LdapConnection conn = getWiredConnection( getLdapServer() ) )
        {
            // comparison success
            CompareRequest compareRequest = new CompareRequestImpl();
            compareRequest.setName( new Dn( "uid=akarasulu,ou=users,ou=system" ) );
            compareRequest.setAttributeId( "uid" );
            compareRequest.setAssertionValue( "akarasulu" );
            ManageDsaIT manageDSAIT = new ManageDsaITImpl();
            manageDSAIT.setCritical( false );
            compareRequest.addControl( manageDSAIT );
    
            CompareResponse compareResponse = conn.compare( compareRequest );
            assertEquals( ResultCodeEnum.COMPARE_TRUE, compareResponse.getLdapResult().getResultCode() );
    
            // referrals failure
            compareRequest = new CompareRequestImpl();
            compareRequest.setName( new Dn( "uid=akarasuluREF,ou=users,ou=system" ) );
            compareRequest.setAttributeId( "uid" );
            compareRequest.setAssertionValue( "akarasulu" );
    
            compareResponse = conn.compare( compareRequest );
            assertEquals( ResultCodeEnum.REFERRAL, compareResponse.getLdapResult().getResultCode() );
    
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system" ) );
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system" ) );
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system" ) );
        }
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
     * @throws LdapException
     */
    @Test
    public void testCompareWithoutAuthentication() throws LdapException, Exception
    {
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );
        
        try ( LdapConnection conn = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() ) )
        {
            Assertions.assertThrows( LdapNoPermissionException.class, () ->
            {
                conn.compare( "uid=admin,ou=system", "uid", "admin" );
                fail( "Compare success without authentication" );
            } );
        }
    }


    /**
     * Tests referral handling when an ancestor is a referral.
     */
    @Test
    public void testAncestorReferral() throws Exception
    {
        LOG.debug( "" );

        try ( LdapConnection conn = getWiredConnection( getLdapServer() ) )
        {
            // referrals failure
            CompareRequest compareRequest = new CompareRequestImpl();
            compareRequest.setName( new Dn( "ou=Computers,uid=akarasuluref,ou=users,ou=system" ) );
            compareRequest.setAttributeId( "ou" );
            compareRequest.setAssertionValue( "Computers" );
    
            CompareResponse compareResponse = conn.compare( compareRequest );
            assertEquals( ResultCodeEnum.REFERRAL, compareResponse.getLdapResult().getResultCode() );
    
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
            assertTrue( compareResponse.getLdapResult().getReferral().getLdapUrls()
                .contains( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        }
    }
}
