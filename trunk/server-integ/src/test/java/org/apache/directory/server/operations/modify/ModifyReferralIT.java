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
package org.apache.directory.server.operations.modify;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.operations.compare.CompareIT;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test taken from DIRSERVER-630: If one tries to add an attribute to an
 * entry, and does not provide a value, it is assumed that the server does
 * not modify the entry. We have a situation here using Sun ONE Directory
 * SDK for Java, where adding a description attribute without value to a
 * person entry like this,
 * <code>
 * dn: cn=Kate Bush,dc=example,dc=com
 * objectclass: person
 * objectclass: top
 * sn: Bush
 * cn: Kate Bush
 * </code>
 * does not fail (modify call does not result in an exception). Instead, a
 * description attribute is created within the entry. At least the new
 * attribute is readable with Netscape SDK (it is not visible to most UIs,
 * because it is invalid ...).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
//@CreateDS( name="ModifyReferralIT-class", enableChangeLog=false )
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
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
        "objectClass: extensibleObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: akarasuluref",
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system" })
public class ModifyReferralIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( CompareIT.class );


    /**
     * Tests modify operation on referral entry with the ManageDsaIT control.
     */
    @Test
    public void testOnReferralWithManageDsaITControl() throws Exception
    {
        LdapConnection conn = getWiredConnection( getLdapServer() );

        ManageDsaIT manageDSAIT = new ManageDsaITImpl();
        manageDSAIT.setCritical( true );

        // modify success
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( new Dn( "uid=akarasuluref,ou=users,ou=system" ) );
        modifyRequest.add( "description", "referral to akarasulu" );
        modifyRequest.addControl( manageDSAIT );

        conn.modify( modifyRequest );

        assertTrue( conn.compare( "uid=akarasuluref,ou=users,ou=system", "description", "referral to akarasulu" ) );

        conn.close();
    }


    /**
     * Tests modify operation on referral entries without the
     * ManageDsaIT control. Referrals are sent back to the client with a
     * non-success result code.
     */
    @Test
    public void testOnReferral() throws Exception
    {
        LdapConnection conn = getWiredConnection( getLdapServer() );

        // referrals failure
        // modify success
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( new Dn( "uid=akarasuluref,ou=users,ou=system" ) );
        modifyRequest.add( "description", "referral to akarasulu" );

        ModifyResponse modifyResponse = conn.modify( modifyRequest );

        assertEquals( ResultCodeEnum.REFERRAL, modifyResponse.getLdapResult().getResultCode() );

        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }


    /**
     * Tests modify operation on normal and referral entries without the
     * ManageDsaIT control using JNDI instead of the Netscape API. Referrals
     * are sent back to the client with a non-success result code.
     */
    @Test
    public void testThrowOnReferralWithJndi() throws Exception
    {
        LdapContext ctx = getWiredContextThrowOnRefferal( getLdapServer() );

        // modify failure
        Attribute attr = new BasicAttribute( "description", "referral to akarasulu" );
        ModificationItem mod = new ModificationItem( DirContext.ADD_ATTRIBUTE, attr );

        try
        {
            ctx.modifyAttributes( "uid=akarasuluref,ou=users,ou=system", new ModificationItem[]
                { mod } );
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

        // referrals failure
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( new Dn( "ou=Computers,uid=akarasuluref,ou=users,ou=system" ) );
        modifyRequest.add( "ou", "Machines" );

        ModifyResponse modifyResponse = conn.modify( modifyRequest );

        assertEquals( ResultCodeEnum.REFERRAL, modifyResponse.getLdapResult().getResultCode() );

        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );
        assertTrue( modifyResponse.getLdapResult().getReferral().getLdapUrls()
            .contains( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system" ) );

        conn.close();
    }
}
