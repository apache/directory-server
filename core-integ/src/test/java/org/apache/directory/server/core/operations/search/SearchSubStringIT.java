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
package org.apache.directory.server.core.operations.search;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "SearchDS",
    partitions =
        {
            @CreatePartition(
                cacheSize = 12000,
                name = "sonatype",
                suffix = "o=sonatype",
                contextEntry = @ContextEntry(
                    entryLdif =
                        "dn: o=sonatype\n" +
                        "o: sonatype\n" +
                        "objectClass: top\n" +
                        "objectClass: organization\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass", cacheSize = 2000),
                        @CreateIndex(attribute = "sn", cacheSize = 2000),
                        @CreateIndex(attribute = "cn", cacheSize = 2000),
                        @CreateIndex(attribute = "displayName", cacheSize = 2000)
                })

    })
@ApplyLdifs(
    {
        "dn: ou=people,o=sonatype",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: people",
        "description: Contains entries which describe persons",

        "dn: ou=peopleA,ou=people,o=sonatype",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: peopleA",
        "description: Contains entries which describe persons in org A",

        "dn: ou=peopleB,ou=people,o=sonatype",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: peopleB",
        "description: Contains entries which describe persons in org B",

        "dn: cn=cstamas,ou=peopleA,ou=people,o=sonatype",
        "objectclass: inetOrgPerson",
        "cn: cstamas",
        "sn: Tamas Cservenak",
        "uid: cstamas",
        "userpassword: cstamas123",
        "mail: cstamas@sonatype.com",
        "description: This is Tamas",

        "dn: cn=brianf,ou=peopleB,ou=people,o=sonatype",
        "objectclass: inetOrgPerson",
        "cn: brianf",
        "sn: Brian Fox",
        "uid: brianf",
        "userpassword: brianf123",
        "mail: brianf@sonatype.com",
        "description: This is Brian",

        "dn: cn=jvanzyl,ou=peopleA,ou=people,o=sonatype",
        "objectclass: inetOrgPerson",
        "cn: jvanzyl",
        "sn: Jason Van Zyl",
        "uid: jvanzyl",
        "userpassword: jvanzyl123",
        "mail: jvanzyl@sonatype.com",
        "description: This is Jason",

        "dn: ou=groups,o=sonatype",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: groups",
        "description: Contains entries which describe groups",

        "dn: cn=public,ou=groups,o=sonatype",
        "objectClass: groupOfUniqueNames",
        "cn: public",
        "uniqueMember: cn=cstamas,ou=peopleA,ou=people,o=sonatype",
        "uniqueMember: cn=brianf,ou=peopleB,ou=people,o=sonatype",
        "uniqueMember: cn=jvanzyl,ou=peopleA,ou=people,o=sonatype",
        "description: Public group",

        "dn: cn=releases,ou=groups,o=sonatype",
        "objectClass: groupOfUniqueNames",
        "cn: releases",
        "uniqueMember: cn=jvanzyl,ou=peopleA,ou=people,o=sonatype",
        "uniqueMember: cn=brianf,ou=peopleB,ou=people,o=sonatype",
        "description: Releases group",

        "dn: cn=snapshots,ou=groups,o=sonatype",
        "objectClass: groupOfUniqueNames",
        "cn: snapshots",
        "uniqueMember: cn=jvanzyl,ou=peopleA,ou=people,o=sonatype",
        "uniqueMember: cn=cstamas,ou=peopleA,ou=people,o=sonatype",
        "description: Snapshots group"
    })
public class SearchSubStringIT extends AbstractLdapTestUnit
{
    /**
     * Search for a uniqueMember either using a valid filter or a filter with a substringMR, which
     * does not exist
     */
    @Test
    public void testSearchSubString() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        connection.bind( "uid=admin,ou=system", "secret" );

        // Do some search with an invalid filter
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( new Dn( "o=sonatype" ) );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
        searchRequest.setFilter( "(|(&(&(cn=*)(uniqueMember=cn=cstamas*))(objectClass=groupOfUniqueNames))(objectClass=referral))" );
        int count = 0;

        try ( SearchCursor cursor = connection.search( searchRequest ) )
        {
            while ( cursor.next() )
            {
                cursor.getEntry();
                
                count++;
            }
        }
        
        // We should not find any user 
        assertEquals( 0, count );
        
        // Now search with a valid filter. We should find 2 entries
        count = 0;

        searchRequest.setBase( new Dn( "o=sonatype" ) );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
        searchRequest.setFilter( "(|(&(&(cn=*)(uniqueMember=cn=cstamas,ou=peopleA,ou=people,o=sonatype))(objectClass=groupOfUniqueNames))(objectClass=referral))" );

        try ( SearchCursor cursor = connection.search( searchRequest ) )
        {
            while ( cursor.next() )
            {
                cursor.getEntry();
                
                count++;
            }
        }

        assertEquals( 2, count );

        connection.close();
    }
}
