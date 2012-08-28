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

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateAuthenticator;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.authn.AnonymousAuthenticator;
import org.apache.directory.server.core.authn.DelegatingAuthenticator;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.apache.directory.server.core.authn.StrongAuthenticator;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.junit.Ignore;
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
        "dn: uid=antoine,ou=users,ou=system",
        "objectClass: uidObject",
        "objectClass: person",
        "objectClass: top",
        "uid: antoine",
        "cn: Antoine Levy-Lambert",
        "sn: Levy-Lambert",
        "userPassword: secret" })
@CreateDS(allowAnonAccess = true, name = "DelegatedAuthIT-class")
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 10200)
    })
public class DelegatedAuthIT extends AbstractLdapTestUnit
{

    /**
     * Test with bindDn which is not even found under any namingContext of the
     * server.
     * 
     * @throws Exception 
     */
    @CreateDS(
        allowAnonAccess = true,
        name = "DelegatedAuthIT-method",
        authenticators =
            {
                @CreateAuthenticator(
                    type = DelegatingAuthenticator.class,
                    delegateHost = "localhost",
                    delegatePort = 10200) })
    @CreateLdapServer(
        transports =
            {
                @CreateTransport(protocol = "LDAP")
        })
    @Test
    @Ignore
    public void testDelegatedAuthentication() throws Exception
    {
        assertTrue( getService().isStarted() );
        assertEquals( "DelegatedAuthIT-method", getService().getInstanceId() );
        LdapConnection ldapConnection = LdapConnectionFactory.getNetworkConnection( "localhost", getLdapServer()
            .getPort() );
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

        try
        {
            ldapConnection.bind( "uid=ivanhoe,ou=users,ou=system", "secret" );
            fail();
        }
        catch ( Exception exc )
        {
            assertTrue( true );
        }

        ldapConnection.unBind();
        ldapConnection.close();
    }


    /**
     * Test with bindDn which is not even found under any namingContext of the
     * server.
     * 
     * @throws Exception 
     */
    @CreateDS(
        allowAnonAccess = true,
        name = "DelegatedAuthIT-MultipleAuthenticators-method",
        authenticators =
            {
                @CreateAuthenticator(type = AnonymousAuthenticator.class),
                @CreateAuthenticator(type = SimpleAuthenticator.class),
                @CreateAuthenticator(
                    type = DelegatingAuthenticator.class,
                    delegateHost = "localhost",
                    delegatePort = 10200),
                @CreateAuthenticator(type = StrongAuthenticator.class) })
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
                @CreateTransport(protocol = "LDAP")
        })
    @Test
    @Ignore
    public void testMultipleAuthenticators() throws Exception
    {
        assertTrue( getService().isStarted() );
        assertEquals( "DelegatedAuthIT-MultipleAuthenticators-method", getService().getInstanceId() );
        LdapConnection ldapConnection = LdapConnectionFactory.getNetworkConnection( "localhost", getLdapServer()
            .getPort() );
        ldapConnection.bind( "uid=emmanuel,ou=users,ou=system", "sesame" );

        assertTrue( ldapConnection.isAuthenticated() );

        ldapConnection.unBind();

        try
        {
            ldapConnection.bind( "uid=emmanuel,ou=users,ou=system", "crypto" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        ldapConnection.unBind();
        ldapConnection.bind();

        assertTrue( ldapConnection.isAuthenticated() );

        ldapConnection.unBind();
        ldapConnection.bind( "uid=antoine,ou=users,ou=system", "secret" );

        assertTrue( ldapConnection.isAuthenticated() );

        ldapConnection.unBind();

        try
        {
            ldapConnection.bind( "uid=antoine,ou=users,ou=system", "sesame" );
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        ldapConnection.unBind();

        try
        {
            try
            {
                ldapConnection.bind( "uid=ivanhoe,ou=users,ou=system", "secret" );
            }
            catch ( LdapAuthenticationException lae )
            {
                assertTrue( true );
            }

            ldapConnection.unBind();
        }
        catch ( Exception exc )
        {
            assertTrue( true );
        }

        ldapConnection.close();
    }
}
