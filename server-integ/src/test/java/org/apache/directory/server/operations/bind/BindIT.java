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
package org.apache.directory.server.operations.bind;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.naming.AuthenticationException;
import javax.naming.InvalidNameException;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPConstraints;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPException;

import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the server to make sure standard compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
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
        "objectClass: uidObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: akarasuluref",
        "userPassword: secret",
        "ref: ldap://localhost:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://foo:10389/uid=akarasulu,ou=users,ou=system",
        "ref: ldap://bar:10389/uid=akarasulu,ou=users,ou=system" })
@CreateDS(allowAnonAccess = true, name = "BindIT-class")
@CreateLdapServer(
    transports =
    {
        @CreateTransport(protocol = "LDAP")
    })
public class BindIT extends AbstractLdapTestUnit
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );


    /**
     * Test with bindDn which is not even found under any namingContext of the
     * server.
     * 
     * @throws Exception 
     */
    @Test
    public void testBadBindDnNotInContext() throws Exception
    {
        try
        {
            getWiredContext( getLdapServer(), "cn=bogus", "blah" );
            fail( "should never get here due to a " );
        }
        catch ( AuthenticationException e )
        {
        }
    }


    /**
     * Test bind with malformed bind Dn.
     */
    @Test
    public void testBadBindDnMalformed() throws Exception
    {
        try
        {
            getWiredContext( getLdapServer(), "system", "blah" );
            fail( "should never get here due to a " );
        }
        catch ( InvalidNameException e ){}
    }

    /**
     * Test with bindDn that is under a naming context but points to non-existant user.
     * 
     * @throws Exception 
     */
    @Test
    public void testBadBindDnInContext() throws Exception
    {
        try
        {
            getWiredContext( getLdapServer(), "cn=bogus,ou=system", "blah" );
            fail( "should never get here due to a " );
        }
        catch ( AuthenticationException e )
        {
        }
    }


    @Test
    public void testConnectWithIllegalLDAPVersion() throws Exception
    {
        LDAPConnection conn = null;

        try
        {
            conn = new LDAPConnection();
            conn.connect( 100, "localhost", getLdapServer().getPort(), "uid=admin,ou=system", "secret" );
            fail( "try to connect with illegal version number should fail" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.PROTOCOL_ERROR, e.getLDAPResultCode() );
        }
        finally
        {
            if ( conn != null )
            {
                conn.disconnect();
            }
        }
    }


    /**
     * Tests bind operation on referral entry.
     */
    @Test
    public void testOnReferralWithOrWithoutManageDsaItControl() throws Exception
    {
        LDAPConnection conn = new LDAPConnection();
        LDAPConstraints constraints = new LDAPConstraints();
        constraints.setClientControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        constraints.setServerControls( new LDAPControl( LDAPControl.MANAGEDSAIT, true, new byte[0] ) );
        conn.setConstraints( constraints );

        try
        {
            conn.connect( 3, "localhost", getLdapServer().getPort(),
                "uid=akarasuluref,ou=users,ou=system", "secret", constraints );
            fail( "try to connect with illegal version number should fail" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.INVALID_CREDENTIALS, e.getLDAPResultCode() );
        }

        try
        {
            conn.connect( 3, "localhost", getLdapServer().getPort(),
                "uid=akarasuluref,ou=users,ou=system", "secret" );
            fail( "try to connect with illegal version number should fail" );
        }
        catch ( LDAPException e )
        {
            assertEquals( "statuscode", LDAPException.INVALID_CREDENTIALS, e.getLDAPResultCode() );
        }
    }
}
