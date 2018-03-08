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


import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.EntryCursorImpl;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "SearchPerfDS",
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

    },
    enableChangeLog = false)
public class SearchPerfIT extends AbstractLdapTestUnit
{
    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfObjectScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)",
            SearchScope.OBJECT, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 1, i );

        int nbIterations = 1500000;

        Dn dn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );

        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( dn );
        searchRequest.setFilter( "(ObjectClass=*)" );
        searchRequest.setScope( SearchScope.OBJECT );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
        int count = 0;

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = new EntryCursorImpl( connection.search( searchRequest ) );

            while ( cursor.next() )
            {
                cursor.get();
                count++;
            }

            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "OBJECT level - Delta : " + deltaWarmed + "( "
            + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) + ", count : " + count );
        connection.close();
    }


    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfOneLevelScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
            SearchScope.ONELEVEL, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 5, i );

        int nbIterations = 150000;
        Dn dn = new Dn( getService().getSchemaManager(), "ou=system" );
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( dn );
        searchRequest.setFilter( "(ObjectClass=*)" );
        searchRequest.setScope( SearchScope.ONELEVEL );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
        int count = 0;

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 10000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 50000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = new EntryCursorImpl( connection.search( searchRequest ) );

            while ( cursor.next() )
            {
                cursor.get();
                count++;
            }

            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "ONE level - Delta : " + deltaWarmed + "( "
            + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed ) * 5
            + " per s ) /" + ( t1 - t0 ) + ", count : " + count );
        connection.close();
    }


    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfSublevelScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
            SearchScope.SUBTREE, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 10, i );

        int nbIterations = 150000;
        Dn dn = new Dn( getService().getSchemaManager(), "ou=system" );
        SearchRequest searchRequest = new SearchRequestImpl();

        searchRequest.setBase( dn );
        searchRequest.setFilter( "(ObjectClass=*)" );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( "*" );
        searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
        int count = 0;

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 10000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 50000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = new EntryCursorImpl( connection.search( searchRequest ) );

            while ( cursor.next() )
            {
                cursor.get();
                count++;
            }

            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "SUB level - Delta : " + deltaWarmed + "( "
            + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed )
            * 10
            + " per s ) /" + ( t1 - t0 ) + ", count : " + count );
        connection.close();
    }


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
