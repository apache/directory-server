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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateAuthenticator;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.authn.DelegatingAuthenticator;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the Delegated authenticator using SSL
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    allowAnonAccess = true,
    name = "DelegatedAuthIT-class",
    authenticators =
        {
            @CreateAuthenticator(
                type = DelegatingAuthenticator.class,
                delegatePort = 10201,
                delegateSsl = false,
                delegateTls = true) })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=emmanuel,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: person",
        "objectClass: top",
        "uid: emmanuel",
        "cn: Emmanuel Lecharny",
        "sn: Lecharny",
        "userPassword: sesame" })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 10200)
    },
    allowAnonymousAccess = true)
public class DelegatedAuthOverTlsIT extends AbstractLdapTestUnit
{
    /**
     * Test with bindDn which is not even found under any namingContext of the
     * server.
     * 
     * @throws Exception
     */
    @CreateDS(
        allowAnonAccess = true,
        name = "DelegatedAuthIT-method")
    @ApplyLdifs(
        {
            // Entry # 1
            "dn: uid=antoine,ou=users,ou=system",
            "objectClass: uidObject",
            "objectClass: person",
            "objectClass: top",
            "uid: antoine",
            "cn: Antoine Levy-Lambert",
            "sn: Levy-Lambert",
            "userPassword: secret" })
    @CreateLdapServer(
        transports =
            {
                @CreateTransport(protocol = "LDAP", port = 10201)
        },
        extendedOpHandlers =
            {
                StartTlsHandler.class
        }
        )
        @Test
        public void testDelegatedTlsAuthentication() throws Exception
    {
        assertTrue( getService().isStarted() );
        assertEquals( "DelegatedAuthIT-method", getService().getInstanceId() );
        LdapConnection ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, 10200 );

        ldapConnection.bind( "uid=antoine,ou=users,ou=system", "secret" );

        assertTrue( ldapConnection.isAuthenticated() );

        ldapConnection.unBind();

        try
        {
            ldapConnection.bind( "uid=antoine,ou=users,ou=system", "sesame" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        ldapConnection.unBind();
        ldapConnection.close();
    }
}
