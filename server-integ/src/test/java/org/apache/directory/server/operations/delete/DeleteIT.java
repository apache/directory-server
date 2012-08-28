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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.naming.NameNotFoundException;
import javax.naming.ReferralException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.model.message.DeleteResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Integration tests for delete operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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
public class DeleteIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( DeleteIT.class );


    /**
     * Tests normal delete operation on normal non-referral entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testNormalDeleteFailContextNotEmpty() throws Exception
    {
        LdapConnection conn = getAdminConnection( getLdapServer() );

        // delete failure on non-leaf entry
        try
        {
            conn.delete( "uid=akarasulu,ou=users,ou=system" );
            fail();
        }
        catch ( LdapContextNotEmptyException lcnee )
        {
            assertTrue( true );
        }

        conn.unBind();
        conn.close();
    }


    /**
     * Tests normal delete operation on normal non-referral entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testNormalDelete() throws Exception
    {
        LdapConnection conn = getAdminConnection( getLdapServer() );

        // delete success
        conn.delete( "ou=computers,uid=akarasulu,ou=users,ou=system" );

        // delete failure non-existant entry
        try
        {
            conn.delete( "uid=elecharny,ou=users,ou=system" );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }

        conn.unBind();
        conn.close();
    }


    /**
     * Tests normal delete operation on non-existent entries without 
     * the ManageDsaIT control.
     */
    @Test
    public void testDeleteNonExistent() throws Exception
    {
        LdapConnection conn = getAdminConnection( getLdapServer() );

        // delete failure non-existent entry
        try
        {
            conn.delete( "uid=elecharny,ou=users,ou=system" );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }

        conn.unBind();
        conn.close();
    }


    /**
     * Tests delete operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        LdapConnection conn = getWiredConnection( getLdapServer() );

        ManageDsaIT manageDSAIT = new ManageDsaITImpl();
        manageDSAIT.setCritical( true );

        // delete success
        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( new Dn( "uid=akarasuluref,ou=users,ou=system" ) );
        deleteRequest.addControl( manageDSAIT );
        conn.delete( deleteRequest );

        assertNull( conn.lookup( "uid=akarasuluref,ou=users,ou=system", new Control[]
            { manageDSAIT } ) );

        conn.close();
    }


    /**
     * Tests delete operation on normal and referral entries without the 
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LdapConnection conn = getWiredConnection( getLdapServer() );

        // delete success
        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( new Dn( "uid=akarasuluref,ou=users,ou=system" ) );
        DeleteResponse deleteResponse = conn.delete( deleteRequest );

        assertEquals( ResultCodeEnum.REFERRAL, deleteResponse.getLdapResult().getResultCode() );

        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }


    /**
     * Tests delete operation on normal and referral entries without the 
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals 
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( getLdapServer() );

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

        LdapConnection conn = getWiredConnection( getLdapServer() );

        // delete success
        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( new Dn( "ou=Computers,uid=akarasuluref,ou=users,ou=system" ) );
        DeleteResponse deleteResponse = conn.delete( deleteRequest );

        assertEquals( ResultCodeEnum.REFERRAL, deleteResponse.getLdapResult().getResultCode() );

        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( deleteResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }


    /**
     * Try to delete an entry with invalid Dn. The operation fails
     * during parsing the given Dn
     */
    @Test
    public void testDeleteWithIllegalName() throws Exception
    {
        LdapConnection conn = getAdminConnection( getLdapServer() );

        try
        {
            conn.delete( "This is an illegal name,dc=example,dc=com" );
            fail( "deletion should fail" );
        }
        catch ( Exception e )
        {
            // expected
        }

        conn.close();
    }
}
