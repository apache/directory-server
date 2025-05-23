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

package org.apache.directory.shared.client.api.operations.bind;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO Test.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.SaslPlainRequest;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the SASL BindRequest operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "SASLBindDS",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example,dc=com\n" +
                    "dc: example\n" +
                    "objectClass: top\n" +
                    "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "sn"),
                        @CreateIndex(attribute = "cn"),
                        @CreateIndex(attribute = "displayName")
                })
    },
    enableChangeLog = true)
@CreateLdapServer(
        saslMechanisms =
        {
            @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class)
        }, 
        transports = 
        {
            @CreateTransport(protocol = "LDAP"), 
            @CreateTransport(protocol = "LDAPS")
        },
        searchBaseDn = "ou=users,ou=system"
    )
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=superuser,ou=users,ou=system",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "objectClass: top",
        "cn: superuser",
        "sn: administrator",
        "displayName: Directory Superuser",
        "uid: superuser",
        "userPassword: test",
        "",
        // Entry # 2
        "dn: uid=superuser2,ou=users,ou=system",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "objectClass: top",
        "cn: superuser2",
        "sn: administrator",
        "displayName: Directory Superuser",
        "uid: superuser2",
        "userPassword: test1",
        "userPassword: test2" })
public class SaslBindRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;

    /**
     * Create the LdapConnection
     */
    @BeforeEach
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
    }


    /**
     * Close the LdapConnection
     */
    @AfterEach
    public void shutdown() throws Exception
    {
        if ( connection != null )
        {
            connection.close();
        }
    }

    
    /**
     * Test a successful SASL PLAIN bind request.
     */
    @Test
    public void testSaslPlainBindRequest() throws Exception
    {
        SaslPlainRequest saslPlainRequest = new SaslPlainRequest();
        saslPlainRequest.setAuthorizationId(null);
        saslPlainRequest.setUsername( "superuser" );
        saslPlainRequest.setCredentials( "test" );

        BindResponse bindResponse = connection.bind( saslPlainRequest );

        assertNotNull( bindResponse );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
    }
}
