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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the CreateLdapServerRule.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
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
public class TestCreateLdapServerRule extends AbstractLdapTestUnit
{
    private static Logger LOG = LoggerFactory.getLogger( TestCreateLdapServerRule.class );
    
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
        LdapConnection ldapConnection = null;
        
        try
        {
            LdapServer ldapServer = getLdapServer();
            ldapServer.getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            Dn dn = new Dn( "cn=methodDs,ou=system" );
            Entry entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "methodDs", entry.get( "cn" ).get().getString() );
    
            try
            {
                dn = new Dn( "cn=class,ou=system" );
                entry = ldapConnection.lookup( dn );
                assertNotNull( entry );
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
        LdapConnection ldapConnection = null;
        
        try
        {
            LdapServer ldapServer = getLdapServer();
            ldapServer.getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getString() );
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
        DirectoryService directoryService = getLdapServer().getDirectoryService();
    
        LdapConnection ldapConnection = null;
        try
        {
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = directoryService.getAdminSession().lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getString() );
    
            LOG.debug( "getting network connection" );
            getLdapServer().getPort();
            ldapConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
            ldapConnection.connect();
            ldapConnection.bind( "uid=admin,ou=system", "secret" );
    
            entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getString() );
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
