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
package org.apache.directory.server.operations.lookup;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotPartition;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Testcase for the lookup operation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(allowAnonAccess = true, name = "LookupMavibotIT-class", partitions =
{
    @CreatePartition(
        name = "example",
        type = MavibotPartition.class,
        suffix = "dc=example,dc=com",
        contextEntry = @ContextEntry(
            entryLdif = "dn: dc=example,dc=com\n" +
                "dc: example\n" +
                "objectClass: top\n" +
                "objectClass: domain\n\n"),
        indexes =
            {
                @CreateIndex(type = MavibotIndex.class, attribute = "objectClass"),
                @CreateIndex(type = MavibotIndex.class, attribute = "dc"),
                @CreateIndex(type = MavibotIndex.class, attribute = "ou")
        }),
    @CreatePartition(
        name = "directory",
        type = MavibotPartition.class,
        suffix = "dc=directory,dc=apache,dc=org",
        contextEntry = @ContextEntry(
            entryLdif = "dn: dc=directory,dc=apache,dc=org\n" +
                "dc: directory\n" +
                "objectClass: top\n" +
                "objectClass: domain\n\n"),
        indexes =
            {
                @CreateIndex(type = MavibotIndex.class, attribute = "objectClass"),
                @CreateIndex(type = MavibotIndex.class, attribute = "dc"),
                @CreateIndex(type = MavibotIndex.class, attribute = "ou")
        }) })

@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class LookupMavibotIT extends AbstractLdapTestUnit
{
    /**
     * Fetch an existing entry
     */
    @Test
    public void testLookupExistingEntryAPI() throws Exception
    {
        LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );

        Entry entry = connection.lookup( "uid=admin,ou=system", "name" );
        assertNotNull( entry );

        assertEquals( 2, entry.size() );
        assertTrue( entry.containsAttribute( "cn", "sn" ) );
        assertTrue( entry.contains( "cn", "system administrator" ) );
        assertTrue( entry.contains( "sn", "administrator" ) );

        connection.close();
    }


    /**
     * Fetch the RootDSE entry
     */
    @Test
    public void testLookupRootDSE() throws Exception
    {
        LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );

        Entry entry = connection.lookup( "" );
        assertNotNull( entry );

        connection.close();
    }


    /**
     * Fetch a non existing entry
     */
    @Test
    public void testLookupNonExistingEntryAPI() throws Exception
    {
        LdapConnection connection = ServerIntegrationUtils.getAdminConnection( getLdapServer() );

        Entry entry = connection.lookup( "uid=absent,ou=system", "name" );
        assertNull( entry );

        connection.close();
    }
}
