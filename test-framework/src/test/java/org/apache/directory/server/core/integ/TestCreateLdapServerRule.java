/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the CreateLdapServerRule.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateDS(name = "classDS",
    enableChangeLog = true,
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example,dc=com\n" +
                        "objectClass: domain\n" +
                        "objectClass: top\n" +
                        "dc: example\n\n"
                ),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                }
            )
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@ApplyLdifs(
    {
        "dn: cn=class,ou=system",
        "objectClass: person",
        "cn: class",
        "sn: sn_class"
})
public class TestCreateLdapServerRule
{
    private static Logger LOG = LoggerFactory.getLogger( TestCreateLdapServerRule.class );
    @ClassRule
    public static CreateLdapServerRule classCreateLdapServerRule = new CreateLdapServerRule();
    
    @Rule
    public CreateLdapServerRule createLdapServerRule = new CreateLdapServerRule(
        classCreateLdapServerRule );
    
    
    @Test
    @CreateDS(name = "methodDS",
        enableChangeLog = true)
    @ApplyLdifs(
        {
            "dn: cn=methodDs,ou=system",
            "objectClass: person",
            "cn: methodDs",
            "sn: sn_methodDs"
    })
    public void testMethodDs()
    {
        assertEquals( createLdapServerRule.getLdapServer(), classCreateLdapServerRule.getLdapServer() );
    
        LdapConnection ldapConnection = null;
        try
        {
            LdapServer ldapServer = createLdapServerRule.getLdapServer();
            ldapServer.getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            Dn dn = new Dn( "cn=methodDs,ou=system" );
            Entry entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "methodDs", entry.get( "cn" ).get().getValue() );
    
            try
            {
                dn = new Dn( "cn=class,ou=system" );
                entry = ldapConnection.lookup( dn );
                assertNull( entry );
            }
            catch ( LdapNoSuchObjectException e )
            {
                // expected
            }
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    ldapConnection.close();
                }
                catch ( IOException e )
                {
                    // who cares!
                }
            }
        }
    }
    
    
    @Test
    @CreateLdapServer(
        transports =
            {
                @CreateTransport(protocol = "LDAP")
        })
    public void testMethodLdapServer()
    {
        assertNotEquals( createLdapServerRule.getLdapServer(), classCreateLdapServerRule.getLdapServer() );
        assertNotEquals( createLdapServerRule.getLdapServer().getPort(),
            classCreateLdapServerRule.getLdapServer().getPort() );
    
        LdapConnection ldapConnection = null;
        try
        {
            LdapServer ldapServer = createLdapServerRule.getLdapServer();
            ldapServer.getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    ldapConnection.close();
                }
                catch ( IOException e )
                {
                    // who cares!
                }
            }
        }
    }
    
    
    @Test
    public void testNetworkConnection()
    {
        assertEquals( classCreateLdapServerRule.getDirectoryService(), createLdapServerRule.getDirectoryService() );
        assertEquals( classCreateLdapServerRule.getLdapServer(), createLdapServerRule.getLdapServer() );
        LdapServer ldapServer = createLdapServerRule.getLdapServer();
        DirectoryService directoryService = ldapServer.getDirectoryService();
        assertEquals( classCreateLdapServerRule.getDirectoryService(), directoryService );
    
        LdapConnection ldapConnection = null;
        try
        {
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = directoryService.getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );
    
            LOG.debug( "getting network connection" );
            ldapServer.getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    ldapConnection.close();
                }
                catch ( IOException e )
                {
                    // who cares!
                }
            }
        }
    }
}
