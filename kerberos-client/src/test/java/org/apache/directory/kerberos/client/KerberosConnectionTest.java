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

package org.apache.directory.kerberos.client;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for KerberosConnection.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "KerberosConnectionTest-class",
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
                        @CreateIndex(attribute = "ou")
                })
    },
    additionalInterceptors =
        {
            KeyDerivationInterceptor.class
    })
@CreateKdcServer(
    transports =
        {
            @CreateTransport(protocol = "UDP", port = 6088),
            @CreateTransport(protocol = "TCP", port = 6088)
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP", port = 10389),
            @CreateTransport(protocol = "LDAPS", port = 10636)
    })
@ApplyLdifs(
    {
        "dn: ou=Users,dc=example,dc=com",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: Users",

        "dn: uid=hnelson,ou=Users,dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "objectClass: inetOrgPerson",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "cn: Horatio Nelson",
        "sn: Nelson",
        "uid: hnelson",
        "userPassword: secret",
        "krb5PrincipalName: hnelson@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0",

        "dn: uid=krbtgt,ou=Users,dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "objectClass: inetOrgPerson",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "cn: KDC Service",
        "sn: Service",
        "uid: krbtgt",
        "userPassword: secret",
        "krb5PrincipalName: krbtgt/EXAMPLE.COM@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0",

        "dn: uid=ldap,ou=Users,dc=example,dc=com",
        "objectClass: top",
        "objectClass: person",
        "objectClass: inetOrgPerson",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "cn: LDAP",
        "sn: Service",
        "uid: ldap",
        "userPassword: randall",
        "krb5PrincipalName: ldap/localhost@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0"
})
public class KerberosConnectionTest extends AbstractLdapTestUnit
{
    private KerberosConnection kerberosConnection;
    private LdapConnection ldapConnection;
    
    private KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
    
    private KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
    
    
    @Before
    public void createConnection() throws Exception
    {
        kerberosConnection = new KerberosConnection( "localhost", 6088 );
        kerberosConnection.connect();
        ldapConnection = new LdapNetworkConnection( "localhost", 10389 );
        ldapConnection.setTimeOut( 0L );
        ldapConnection.connect();
    }
    
    
    @Test
    public void testGetTgt() throws Exception
    {
        kerberosConnection.getTicketGrantingTicket( clientPrincipal, serverPrincipal, "secret", new ClientRequestOptions() );
    }
    
    
    @Test
    public void testGetTgtAfterPasswordChange() throws Exception
    {
        ldapConnection.bind( "uid=admin,ou=system", "secret" );
        ldapConnection.modify( "uid=hnelson,ou=Users,dc=example,dc=com",
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userPassword", "otherSecret" ) );
    
        kerberosConnection.getTicketGrantingTicket( clientPrincipal, serverPrincipal, "otherSecret",
            new ClientRequestOptions() );
    }
}
