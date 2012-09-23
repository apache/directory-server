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
package org.apache.directory.server.operations.ldapsdk;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getNsdkWiredConnection;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContextThrowOnRefferal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPResponseListener;

import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.operations.compare.CompareIT;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.util.Strings;
import org.junit.Rule;
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
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system"
})
public class ModifyReferralIT extends AbstractLdapTestUnit
{
@Rule
public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );
private static final Logger LOG = LoggerFactory.getLogger( CompareIT.class );


/**
 * Tests modify operation on referral entry with the ManageDsaIT control.
 */
@Test
public void testOnReferralWithManageDsaITControl() throws Exception
{
    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPConstraints();
    constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, Strings.EMPTY_BYTES ) );
    conn.setConstraints( constraints );

    // modify success
    LDAPAttribute attribute = new LDAPAttribute( "description", "referral to akarasulu" );
    LDAPModification mod = new LDAPModification( LDAPModification.ADD, attribute );
    conn.modify( "uid=akarasuluref,ou=users,ou=system", mod, constraints );

    assertTrue( conn.compare( "uid=akarasuluref,ou=users,ou=system", attribute, constraints ) );

    conn.disconnect();
}


/**
 * Tests modify operation on referral entries without the
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
    // modify success
    LDAPAttribute attribute = new LDAPAttribute( "description", "referral to akarasulu" );
    LDAPModification mod = new LDAPModification( LDAPModification.ADD, attribute );
    LDAPResponseListener listener = conn.modify( "uid=akarasuluref,ou=users,ou=system", mod, null, constraints );
    LDAPResponse response = listener.getResponse();

    assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

    assertEquals( "ldap://localhost:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
    assertEquals( "ldap://foo:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
    assertEquals( "ldap://bar:10389/uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

    conn.disconnect();
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

    LDAPConnection conn = getNsdkWiredConnection( getLdapServer() );
    LDAPConstraints constraints = new LDAPConstraints();
    conn.setConstraints( constraints );

    // referrals failure
    LDAPAttribute attribute = new LDAPAttribute( "ou", "Machines" );
    LDAPModification mod = new LDAPModification( LDAPModification.ADD, attribute );
    LDAPResponseListener listener = null;
    LDAPResponse response = null;

    listener = conn.modify( "ou=Computers,uid=akarasuluref,ou=users,ou=system", mod, null, constraints );
    response = listener.getResponse();
    assertEquals( ResultCodeEnum.REFERRAL.getValue(), response.getResultCode() );

    assertEquals( "ldap://localhost:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[0] );
    assertEquals( "ldap://foo:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[1] );
    assertEquals( "ldap://bar:10389/ou=Computers,uid=akarasulu,ou=users,ou=system", response.getReferrals()[2] );

    conn.disconnect();
}
}
