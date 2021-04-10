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


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
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
 * Tests the search() method with Delete done in the middle
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "SearchAddDS",
    partitions =
        {
            @CreatePartition(
                cacheSize = 12000,
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
                        @CreateIndex(attribute = "objectClass", cacheSize = 2000),
                        @CreateIndex(attribute = "sn", cacheSize = 2000),
                        @CreateIndex(attribute = "cn", cacheSize = 2000),
                        @CreateIndex(attribute = "displayName", cacheSize = 2000)
                })

    })
public class SearchWithAddDelete extends AbstractLdapTestUnit
{
    @Test
    public void testSearchWithDeletion() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        connection.bind( "uid=admin,ou=system", "secret" );

        Entry rootPeople = new DefaultEntry(
            connection.getSchemaManager(),
            "ou=People,dc=example,dc=com",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: People" );

        connection.add( rootPeople );
        int nbUsers = 1000;

        for ( int i = 0; i < nbUsers; i++ )
        {
            Entry user = new DefaultEntry(
                connection.getSchemaManager(),
                "uid=user." + i + ",ou=People,dc=example,dc=com",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "givenName: Aaccf",
                "sn: Amar",
                "cn", "user" + i,
                "initials: AA",
                "uid", "user." + i,
                "mail: user.1@cs.hacettepe.edu.tr",
                "userPassword: password",
                "telephoneNumber: 314-796-3178",
                "homePhone: 514-847-0518",
                "pager: 784-600-5445",
                "mobile: 801-755-4931",
                "street: 00599 First Street",
                "l: Augusta",
                "st: MN",
                "postalCode: 30667",
                "postalAddress: Aaccf Amar$00599 First Street$Augusta, MN  30667",
                "description: This is the description for Aaccf Amar." );

            try
            {
                connection.add( user );
            }
            catch ( NullPointerException npe )
            {
                System.out.println( i );
                npe.printStackTrace();
                throw npe;
            }
        }

        // Now do some search
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( new Dn( "ou=people,dc=example,dc=com" ) );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
        searchRequest.setFilter( "(objectClass=*)" );
        int count = 0;

        try ( SearchCursor cursor = connection.search( searchRequest ) )
        {
            while ( cursor.next() )
            {
                cursor.getEntry();
                
                count++;
                
                if ( count == 300 )
                {
                    // Now delete 90 entries while doing a search
                    for ( int j = 10; j <100; j++ )
                    {
                        connection.delete( "uid=user." + j + ",ou=People,dc=example,dc=com" );
                    }
                }
            }
        }
        
        // Deletion of entries while doing a search should result in a smaller result set 
        assertTrue( count < nbUsers + 1 );
        connection.close();
    }
    
    
    @Test
    public void testSearchWithAddition() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        connection.bind( "uid=admin,ou=system", "secret" );

        Entry rootPeople = new DefaultEntry(
            connection.getSchemaManager(),
            "ou=People,dc=example,dc=com",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: People" );

        connection.add( rootPeople );
        int nbUsers = 1000;

        for ( int i = 0; i < nbUsers; i++ )
        {
            Entry user = new DefaultEntry(
                connection.getSchemaManager(),
                "uid=user." + i + ",ou=People,dc=example,dc=com",
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "givenName: Aaccf",
                "sn: Amar",
                "cn", "user" + i,
                "initials: AA",
                "uid", "user." + i,
                "mail: user.1@cs.hacettepe.edu.tr",
                "userPassword: password",
                "telephoneNumber: 314-796-3178",
                "homePhone: 514-847-0518",
                "pager: 784-600-5445",
                "mobile: 801-755-4931",
                "street: 00599 First Street",
                "l: Augusta",
                "st: MN",
                "postalCode: 30667",
                "postalAddress: Aaccf Amar$00599 First Street$Augusta, MN  30667",
                "description: This is the description for Aaccf Amar." );

            try
            {
                connection.add( user );
            }
            catch ( NullPointerException npe )
            {
                System.out.println( i );
                npe.printStackTrace();
                throw npe;
            }
        }

        // Now do some search
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( new Dn( "ou=people,dc=example,dc=com" ) );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
        searchRequest.setFilter( "(objectClass=*)" );
        int count = 0;

        try ( SearchCursor cursor = connection.search( searchRequest ) )
        {
            while ( cursor.next() )
            {
                cursor.getEntry();
                
                count++;
                
                if ( count == 300 )
                {
                    // Now delete 90 entries while doing a search
                    for ( int j = 2000; j <2100; j++ )
                    {
                        Entry user = new DefaultEntry(
                            connection.getSchemaManager(),
                            "uid=user." + j + ",ou=People,dc=example,dc=com",
                            "objectClass: top",
                            "objectClass: person",
                            "objectClass: organizationalPerson",
                            "objectClass: inetOrgPerson",
                            "givenName: Aaccf",
                            "sn: Amar",
                            "cn", "user" + j,
                            "initials: AA",
                            "uid", "user." + j,
                            "mail: user.1@cs.hacettepe.edu.tr",
                            "userPassword: password",
                            "telephoneNumber: 314-796-3178",
                            "homePhone: 514-847-0518",
                            "pager: 784-600-5445",
                            "mobile: 801-755-4931",
                            "street: 00599 First Street",
                            "l: Augusta",
                            "st: MN",
                            "postalCode: 30667",
                            "postalAddress: Aaccf Amar$00599 First Street$Augusta, MN  30667",
                            "description: This is the description for Aaccf Amar." );
                        connection.add( user );
                    }
                }
            }
        }
        
        // Addition of entries during a search should not change anything
        assertEquals( nbUsers + 1, count );
        
        connection.close();
    }
}
