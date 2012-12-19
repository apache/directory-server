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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getNsdkWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.naming.ReferralException;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPResponseListener;
import netscape.ldap.LDAPSearchConstraints;

import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.util.Strings;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the server to make sure standard compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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
        "objectClass: uidObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: akarasuluref",
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system",

        // Entry # 4
        "dn: uid=elecharny,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: person",
        "objectClass: top",
        "uid: elecharny",
        "cn: Emmanuel Lecharny",
        "sn: lecharny"
})
public class ModifyDnReferralIT extends AbstractLdapTestUnit
{
@Rule
public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );
private static final Logger LOG = LoggerFactory.getLogger( ModifyDnReferralIT.class );


/**
 * Tests ModifyDN operation on referral entry with the ManageDsaIT control.
 */
@Test
public void testOnReferralWithManageDsaITControl() throws Exception
{
    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPSearchConstraints();
    constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    conn.setConstraints( constraints );

    // ModifyDN success
    conn.rename( "uid=akarasuluref,ou=users,ou=system", "uid=ref", true, constraints );
    LDAPEntry entry = conn.read( "uid=ref,ou=users,ou=system", ( LDAPSearchConstraints ) constraints );
    assertNotNull( entry );
    assertEquals( "uid=ref,ou=users,ou=system", entry.getDN() );

    conn.disconnect();
}


/**
 * Tests ModifyDN operation with newSuperior on referral entry with the
 * ManageDsaIT control.
 */
@Test
public void testNewSuperiorOnReferralWithManageDsaITControl() throws Exception
{
    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPSearchConstraints();
    constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    conn.setConstraints( constraints );

    // ModifyDN success
    try
    {
        conn.rename( "uid=elecharny,ou=users,ou=system", "uid=newuser",
            "uid=akarasuluref,ou=users,ou=system", true, constraints );
    }
    catch ( LDAPException le )
    {
        assertEquals( LDAPException.AFFECTS_MULTIPLE_DSAS, le.getLDAPResultCode() );
    }

    conn.disconnect();
}


/**
 * Tests ModifyDN operation on normal and referral entries without the
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

    // referrals failure
    LDAPResponseListener listener = null;
    LDAPResponse response = null;

    listener = conn.rename( "uid=akarasuluref,ou=users,ou=system", "uid=ref", true, null, constraints );
    response = listener.getResponse();
    assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

    assertEquals( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
    assertEquals( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
    assertEquals( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

    conn.disconnect();
}


/**
 * Tests ModifyDN operation on normal and referral entries without the
 * ManageDsaIT control. Referrals are sent back to the client with a
 * non-success result code.
 */
@Test
public void testNewSupierorOnReferral() throws Exception
{
    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPConstraints();
    constraints.setReferrals( false );
    conn.setConstraints( constraints );

    // referrals failure
    try
    {
        conn.rename( "uid=elecharny,ou=users,ou=system", "uid=ref",
            "uid=akarasuluref,ou=users,ou=system", true, constraints );
    }
    catch ( LDAPException e )
    {
        assertEquals( LDAPException.AFFECTS_MULTIPLE_DSAS, e.getLDAPResultCode() );
    }

    conn.disconnect();
}


/**
 * Tests ModifyDN operation on normal and referral entries without the
 * ManageDsaIT control using JNDI instead of the Netscape API. Referrals
 * are sent back to the client with a non-success result code.
 */
@Test
public void testThrowOnReferralWithJndi() throws Exception
{
    LdapContext ctx = getWiredContextThrowOnRefferal( getLdapServer() );

    // ModifyDN referrals failure
    try
    {
        ctx.rename( "uid=akarasuluref,ou=users,ou=system", "uid=ref,ou=users,ou=system" );
        fail( "Should never get here due to ModifyDN failure on ReferralException" );
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

    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPConstraints();
    conn.setConstraints( constraints );

    // referrals failure
    LDAPResponseListener listener = null;
    LDAPResponse response = null;

    listener = conn.rename( "ou=Computers,uid=akarasuluref,ou=users,ou=system", "ou=Machines", true, null, constraints );
    response = listener.getResponse();
    assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

    assertEquals( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
    assertEquals( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
    assertEquals( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

    conn.disconnect();
}


/**
 * Tests referral handling when an ancestor is a referral.
 */
@Test
public void testNewSuperiorAncestorReferral() throws Exception
{
    LOG.debug( "" );

    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPConstraints();
    conn.setConstraints( constraints );

    // referrals failure
    try
    {
        conn.rename( "uid=elecharny,ou=users,ou=system", "ou=Machines",
            "ou=Computers,uid=akarasuluref,ou=users,ou=system", true, constraints );
        fail( "Should never get here to affectsMultipleDSA error result code" );
    }
    catch ( LDAPException e )
    {
        assertEquals( LDAPException.AFFECTS_MULTIPLE_DSAS, e.getLDAPResultCode() );
    }

    conn.disconnect();
}
}
