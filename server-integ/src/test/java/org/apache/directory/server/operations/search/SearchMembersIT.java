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
package org.apache.directory.server.operations.search;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.directory.server.integ.ServerIntegrationUtils.getAdminConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testcase with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(partitions =
{
    @CreatePartition(
        name = "example",
        suffix = "dc=example,dc=com",
        indexes =
            {
                @CreateIndex(attribute = "objectClass"),
                @CreateIndex(attribute = "dc"),
                @CreateIndex(attribute = "ou"),
                @CreateIndex(attribute = "uniqueMember")
        },
        contextEntry = @ContextEntry(entryLdif =
            "dn: dc=example,dc=com\n" +
                "objectClass: domain\n" +
                "dc: example"))
})
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })

public class SearchMembersIT extends AbstractLdapTestUnit
{
    /**
     * Test for DIRSERVER-1844
     */
    @ApplyLdifs({
        "dn: ou=users,dc=example,dc=com",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: users",

        "dn: cn=User1,ou=users,dc=example,dc=com",
        "objectClass: person",
        "objectClass: top",
        "cn: User1",
        "sn: user 1",
        "description: User1",

        "dn: ou=groups,dc=example,dc=com",
        "ObjectClass: top",
        "ObjectClass: organizationalUnit",
        "ou: groups",

        "dn: cn=Group1,ou=groups,dc=example,dc=com",
        "objectClass: groupOfUniqueNames",
        "objectClass: top",
        "cn: Group1",
        "uniqueMember: cn=user1,ou=users,dc=example,dc=com",
        "description: Group"
    })

    @Test
    public void testSearchMemberOf() throws Exception
    {
        LdapConnection connection = getAdminConnection( getLdapServer() );
        SearchRequest req = new SearchRequestImpl();
        req.setBase( new Dn( "dc=example,dc=com" ) );
        req.setFilter( "(&(objectClass=person)(memberOf=CN=Group1,ou=groups,DC=example,DC=com))");
        req.setScope( SearchScope.SUBTREE );

        SearchCursor cursor = connection.search( req );
        int count = 0;
        
        while( cursor.next() )
        {
            Entry result = cursor.getEntry();
            count++;
            assertTrue(result.contains("memberOf" , "CN=Group1,ou=groups,DC=example,DC=com"));
        }

        assertEquals( 1, count );

        cursor.close();

        connection.close();
    }
}
