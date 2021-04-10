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


import java.util.Random;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
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
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "SearchPerfDS",
    partitions =
        {
            @CreatePartition(
                type = MavibotPartition.class,
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
                        @CreateIndex(type = MavibotIndex.class, attribute = "objectClass", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "sn", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "cn", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "displayName", cacheSize = 2000)
                })

    },
    enableChangeLog = false)
public class SearchMavibotPerfIT extends AbstractLdapTestUnit
{

    @Test
    public void testSearchCore100kUsers() throws Exception
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
        int nbUsers = 10000;

        System.out.println( "Sleeping..." );
        //Thread.sleep( 10000 );

        long tadd0 = System.currentTimeMillis();
        long tadd = tadd0;

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

            if ( i % 1000 == 0 )
            {
                long tadd1 = System.currentTimeMillis();
                System.out.println( "Injected " + i + " in " + ( tadd1 - tadd ) );
                tadd = tadd1;
            }
        }

        System.out.println( "Sleeping after add ..." );
        //Thread.sleep( 10000 );

        long tadd1 = System.currentTimeMillis();

        System.out.println( "Time to inject " + nbUsers + " entries : " + ( ( tadd1 - tadd0 ) / 1000 ) + "s" );

        //Thread.sleep( 10000 );

        // Now do a random search
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( new Dn( "dc=example,dc=com" ) );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
        int nbIterations = 400000;
        int count = 0;
        Random random = new Random();

        for ( int j = 0; j < nbIterations; j++ )
        {
            if ( j % 10000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( j + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( j == 100000 )
            {
                t00 = System.currentTimeMillis();
            }

            searchRequest.setFilter( "(cn=user" + random.nextInt( nbUsers ) + ")" );

            SearchCursor cursor = connection.search( searchRequest );

            boolean hasNext = firstNext( cursor );

            while ( hasNext )
            {
                count++;
                cursor.getEntry();
                hasNext = innerNext( cursor );
            }

            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 100000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) + ", count : " + count );

        // Delete all the entries
        for ( int i = 0; i < nbUsers; i++ )
        {
            connection.delete( "uid=user." + i + ",ou=People,dc=example,dc=com" );

            if ( i % 1000 == 0 )
            {
                tadd1 = System.currentTimeMillis();
                System.out.println( "Deleted " + i + " in " + ( tadd1 - tadd ) );
                tadd = tadd1;
            }
        }

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

            if ( i % 1000 == 0 )
            {
                tadd1 = System.currentTimeMillis();
                System.out.println( "Injected " + i + " in " + ( tadd1 - tadd ) );
                tadd = tadd1;
            }
        }

        System.out.println( "Sleeping after add ..." );
    }


    private boolean firstNext( SearchCursor cursor ) throws LdapException, CursorException
    {
        return cursor.next();
    }


    private boolean innerNext( SearchCursor cursor ) throws LdapException, CursorException
    {
        return cursor.next();
    }
}
