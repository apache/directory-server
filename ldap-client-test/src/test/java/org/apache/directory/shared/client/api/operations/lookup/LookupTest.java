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
package org.apache.directory.shared.client.api.operations.lookup;


import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "AddPerfDS",
    partitions =
        {
            @CreatePartition(
                name = "isp",
                suffix = "o=isp",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: o=isp\n" +
                    "o: isp\n" +
                    "objectClass: top\n" +
                    "objectClass: organization\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "sn"),
                        @CreateIndex(attribute = "cn"),
                        @CreateIndex(attribute = "displayName")
                }),
            @CreatePartition(
                name = "test",
                suffix = "dc=test,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=test,dc=com\n" +
                        "dc: test\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "sn"),
                        @CreateIndex(attribute = "cn"),
                        @CreateIndex(attribute = "uniqueMember"),
                        @CreateIndex(attribute = "displayName")
                })

    },
    enableChangeLog = true)
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS")
})
@ApplyLdifs(
    {
        // Entry # 1
        "dn: ou=People,o=isp",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: People",

        "DN: ou=Groups,o=isp",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: Groups",

        "DN: cn=testLDAPGroup,ou=Groups,o=isp",
        "objectClass: groupOfUniqueNames",
        "objectClass: top",
        "cn: testLDAPGroup",
        "uniqueMember: uid=admin,ou=system",
        "uniqueMember: uid=pullFromLDAP,ou=People,o=isp",
        "owner: uid=pullFromLDAP,ou=People,o=isp",

        "dn: uid=pullFromLDAP,ou=People,o=isp",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: inetOrgPerson",
        "objectClass: top",
        "cn: pullFromLDAP",
        "description: Active",
        "mail: pullFromLDAP@syncope.apache.org",
        "sn: Surname",
        "uid: pullFromLDAP",
        "userpassword:: cGFzc3dvcmQxMjM=",
        "givenname: pullFromLDAP",
        "title: odd",
        "registeredAddress:  5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8",
        "jpegPhoto:: /9j/4AAQSkZJRgABAQEBKwErAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoH",
        " BwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQk",
        " UDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wg",
        " ARCAAEAAQDAREAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACP/EABQBAQAAAAAAAAAAAAAAA",
        " AAAAAD/2gAMAwEAAhADEAAAAUuf/8QAFhABAQEAAAAAAAAAAAAAAAAAAwAS/9oACAEBAAEFAiLV",
        " /8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPwF//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/",
        " aAAgBAgEBPwF//8QAGhAAAQUBAAAAAAAAAAAAAAAAAgABESEiQf/aAAgBAQAGPwI9k2orq//EAB",
        " kQAAMAAwAAAAAAAAAAAAAAAAERIQBBYf/aAAgBAQABPyF20CYlpT3P/9oADAMBAAIAAwAAABCf/",
        " 8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAgBAwEBPxB//8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/a",
        " AAgBAgEBPxB//8QAGhABAAIDAQAAAAAAAAAAAAAAAREhAEFRYf/aAAgBAQABPxCUKGDcAUFrvhoz/9k="
        }
    )
/**
 * Test some Add operations using an index
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LookupTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );

        // Restart the service so that the index is created
        getService().shutdown();
        getService().startup();
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    @Test
    public void testLookup() throws Exception
    {
        try ( EntryCursor cursor = connection.search(
            "ou=groups,o=isp",
            "(&(objectClass=top)(cn=testLDAPGroup))", SearchScope.ONELEVEL, "*" ) ) 
        {
            assertTrue( cursor.next() );
            
            Entry result = cursor.get();
            
            assertTrue( result.contains( "cn", "testLDAPGroup" ) );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
}
