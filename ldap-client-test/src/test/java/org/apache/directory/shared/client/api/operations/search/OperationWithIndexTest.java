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
package org.apache.directory.shared.client.api.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "AddPerfDS",
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
/**
 * Test some Add operations using an index
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OperationWithIndexTest extends AbstractLdapTestUnit
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


    /**
     * Test an add operation performance
     */
    @Test
    @Ignore
    public void testAddPerf() throws Exception
    {
        Dn dn = new Dn( "cn=test,ou=system" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.add( entry );

        int nbIterations = 8000;

        //BufferedWriter out = new BufferedWriter( new FileWriter("/tmp/out.txt") );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 1000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500 )
            {
                t00 = System.currentTimeMillis();
            }

            dn = new Dn( "uid=" + i + ",dc=example,dc=com" );
            entry = new DefaultEntry(
                getService().getSchemaManager(),
                dn,
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "uid",
                Integer.toString( i ),
                "mail: A-A-R.Awg-Rosli@acme.com",
                "title: Snr Operations Technician (D)",
                "sn: Awg-Rosli",
                "departmentNumber: SMDS - UIA/G/MMO52D",
                "cn: Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D",
                "description: UI - S",
                "telephoneNumber: 555-1212",
                "givenName: Awg-Abd-Rahim",
                "businessCategory: Ops MDS (Malaysia) Sdn Bhd",
                "displayName",
                i + "Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D",
                "employeeNumber: A-A-R.Awg-Rosli",
                "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

            //out.write( LdifUtils.convertToLdif( entry ) );
            connection.add( entry );
        }

        //out.flush();
        //out.close();

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 500 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );

        Entry entry1 = null;
        Entry entry2 = null;
        Entry entry3 = null;

        long ns0 = System.currentTimeMillis();
        EntryCursor results = connection.search( "dc=example,dc=com",
            "(displayName=1234Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            if ( entry1 == null )
            {
                entry1 = results.get();
            }
        }

        results.close();

        long ns1 = System.currentTimeMillis();

        System.out.println( "Delta search : " + ( ns1 - ns0 ) );

        long ns2 = System.currentTimeMillis();
        results = connection.search( "dc=example,dc=com", "(displayName=3456*)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            if ( entry2 == null )
            {
                entry2 = results.get();
            }
        }

        results.close();
        long ns3 = System.currentTimeMillis();

        System.out.println( "Delta search substring : " + ( ns3 - ns2 ) );

        long ns4 = System.currentTimeMillis();
        results = connection.search( "dc=example,dc=com", "(uid=6789)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            if ( entry3 == null )
            {
                entry3 = results.get();
            }
        }

        results.close();
        long ns5 = System.currentTimeMillis();

        System.out.println( "Delta search no index : " + ( ns5 - ns4 ) );

        //System.out.println( "Entry 1 : " + entry1 );
        //System.out.println( "Entry 2 : " + entry2 );
        //System.out.println( "Entry 3 : " + entry3 );
        connection.close();

        // Now, shutdown and restart the server once more
        System.out.println( "--------------> Shuting Down" );
        long ns6 = System.currentTimeMillis();
        getService().shutdown();
        long ns7 = System.currentTimeMillis();
        System.out.println( "--------------> completed in " + ( ns7 - ns6 ) );

        long ns8 = System.currentTimeMillis();
        getService().startup();
        long ns9 = System.currentTimeMillis();
        System.out.println( "--------------> Starting up completed in " + ( ns9 - ns8 ) );

        // and do a search again
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );

        long ns10 = System.currentTimeMillis();
        results = connection.search( "dc=example,dc=com", "(displayName=345*)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            entry3 = results.get();
            break;
        }

        results.close();
        long ns11 = System.currentTimeMillis();
        System.out.println( "New Delta search substring : " + ( ns11 - ns10 ) );

        connection.close();

        // And again

        // Now, shutdown and restart the server once more
        System.out.println( "--------------> Shuting Down 2" );
        long ns12 = System.currentTimeMillis();
        getService().shutdown();
        long ns13 = System.currentTimeMillis();
        System.out.println( "--------------> completed in " + ( ns13 - ns12 ) );

        long ns14 = System.currentTimeMillis();
        getService().startup();
        long ns15 = System.currentTimeMillis();
        System.out.println( "--------------> Starting up completed in " + ( ns15 - ns14 ) );

        // and do a search again
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );

        long ns16 = System.currentTimeMillis();
        results = connection.search( "dc=example,dc=com", "(displayName=345*)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            entry3 = results.get();
            break;
        }

        results.close();
        long ns17 = System.currentTimeMillis();
        System.out.println( "New Delta search substring : " + ( ns17 - ns16 ) );

        connection.close();
    }


    @Test
    public void testModify() throws Exception
    {
        // Add the entry
        Dn dn = new Dn( "uid=1,dc=example,dc=com" );
        Entry entry = new DefaultEntry(
            getService().getSchemaManager(),
            dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid: 1",
            "mail: test@acme.com",
            "title: technician",
            "sn: Test",
            "departmentNumber: Dep1",
            "cn: entryTest",
            "description: Test entry",
            "telephoneNumber: 123 456",
            "givenName: Test user",
            "businessCategory: Test ops",
            "displayName: testUser",
            "employeeNumber: Test user",
            "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

        connection.add( entry );

        EntryCursor results = connection.search( "dc=example,dc=com", "(displayName=T*)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "testUser" ) );
        }

        results.close();

        // Now, modify it
        connection
            .modify( dn,
                new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "displayName", "anotherTest" ) );

        results = connection.search( "dc=example,dc=com", "(displayName=a*)", SearchScope.SUBTREE, "*" );

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "anotherTest" ) );
        }

        results.close();

        results = connection.search( "dc=example,dc=com", "(displayName=T*)", SearchScope.SUBTREE, "*" );

        assertFalse( results.next() );

        results.close();

        // Delete the entry
        connection.delete( dn );
    }


    /**
     * Test that the index are correctly updated after a modify operation when we replace an index
     * AT values
     * @throws IOException 
     * @throws CursorException 
     */
    @Test
    public void testModifyReplace() throws LdapException, CursorException, IOException
    {
        // Add the entry
        Dn dn = new Dn( "uid=1,dc=example,dc=com" );
        Entry entry = new DefaultEntry(
            getService().getSchemaManager(),
            dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid: 1",
            "mail: test@acme.com",
            "title: technician",
            "sn: Test",
            "departmentNumber: Dep1",
            "cn: entryTest",
            "description: Test entry",
            "telephoneNumber: 123 456",
            "givenName: Test user",
            "businessCategory: Test ops",
            "displayName: testUser",
            "employeeNumber: Test user",
            "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

        connection.add( entry );

        // Check the search using the cn index
        EntryCursor results = connection.search( "dc=example,dc=com", "(cn=e*)", SearchScope.SUBTREE, "*" );

        int nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        results.close();

        assertEquals( 1, nbFound );

        // Ok, now replace the cn
        Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "cn", "New cn" );

        connection.modify( dn, modification );

        // The Substring index on CN should still work
        // The old cn should not be present anymore
        results = connection.search( "dc=example,dc=com", "(cn=e*)", SearchScope.SUBTREE, "*" );

        assertFalse( results.next() );

        results.close();

        // Check that we can find the new cn
        results = connection.search( "dc=example,dc=com", "(cn=n*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "New cn" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Now, check the presence index
        results = connection.search( "dc=example,dc=com", "(cn=*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "New cn" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Delete the entry
        connection.delete( dn );
    }


    /**
     * Test that the index are correctly updated after a modify operation when we add an index
     * AT values
     */
    @Test
    public void testModifyAdd() throws LdapException, CursorException, IOException
    {
        // Add the entry
        Dn dn = new Dn( "uid=1,dc=example,dc=com" );
        Entry entry = new DefaultEntry(
            getService().getSchemaManager(),
            dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid: 1",
            "mail: test@acme.com",
            "title: technician",
            "sn: Test",
            "departmentNumber: Dep1",
            "cn: entryTest",
            "description: Test entry",
            "telephoneNumber: 123 456",
            "givenName: Test user",
            "businessCategory: Test ops",
            "employeeNumber: Test user",
            "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

        connection.add( entry );

        // Check the search using the cn index
        EntryCursor results = connection.search( "dc=example,dc=com", "(cn=e*)", SearchScope.SUBTREE, "*" );

        int nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        results.close();

        assertEquals( 1, nbFound );

        // Ok, now replace the cn
        Modification modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "cn", "New cn" );

        connection.modify( dn, modification );

        // The Substring index on CN should still work
        // The old cn should still be present anymore
        results = connection.search( "dc=example,dc=com", "(cn=e*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Check that we can find the new cn
        results = connection.search( "dc=example,dc=com", "(cn=n*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "New cn" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Now, check the presence index
        results = connection.search( "dc=example,dc=com", "(cn=*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "New cn", "entryTest" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Now, check that the index on displayName is correctly updated
        modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "displayName", "testUser" );

        connection.modify( dn, modification );

        // Check the displayName index
        results = connection.search( "dc=example,dc=com", "(displayName=t*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "testUser" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Now, check the presence index
        results = connection.search( "dc=example,dc=com", "(displayName=*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "testUser" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Delete the entry
        connection.delete( dn );
    }


    /**
     * Test that the index are correctly updated after a modify operation when we delete an index
     * AT values
     */
    @Test
    public void testModifyDelete() throws LdapException, CursorException, IOException
    {
        // Add the entry
        Dn dn = new Dn( "uid=1,dc=example,dc=com" );
        Entry entry = new DefaultEntry(
            getService().getSchemaManager(),
            dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid: 1",
            "mail: test@acme.com",
            "title: technician",
            "sn: Test",
            "departmentNumber: Dep1",
            "cn: entryTest",
            "cn: test2",
            "description: Test entry",
            "telephoneNumber: 123 456",
            "givenName: Test user",
            "displayName: testEntry",
            "businessCategory: Test ops",
            "employeeNumber: Test user",
            "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

        connection.add( entry );

        // Check the search using the cn index
        EntryCursor results = connection.search( "dc=example,dc=com", "(cn=e*)", SearchScope.SUBTREE, "*" );

        int nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        results.close();

        assertEquals( 1, nbFound );

        // Ok, now replace the cn
        Modification modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "displayName",
            "testEntry" );

        connection.modify( dn, modification );

        // We should not find anything using the substring filter for the displayName AT
        results = connection.search( "dc=example,dc=com", "(displayName=t*)", SearchScope.SUBTREE, "*" );

        assertFalse( results.next() );

        results.close();

        // Check that we cannot find the displayName using the presence index
        results = connection.search( "dc=example,dc=com", "(displayName=n*)", SearchScope.SUBTREE, "*" );

        assertFalse( results.next() );

        results.close();

        // Now, Delete one value from the cn index
        modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "cn", "test2" );

        connection.modify( dn, modification );

        // Check the cn index using the remaining value
        results = connection.search( "dc=example,dc=com", "(cn=E*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertFalse( result.contains( "cn", "test2" ) );
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Check the cn index using the removed value
        results = connection.search( "dc=example,dc=com", "(cn=t*)", SearchScope.SUBTREE, "*" );

        assertFalse( results.next() );

        results.close();

        // Now, check the presence index
        results = connection.search( "dc=example,dc=com", "(cn=*)", SearchScope.SUBTREE, "*" );

        nbFound = 0;

        while ( results.next() )
        {
            Entry result = results.get();
            assertFalse( result.contains( "cn", "test2" ) );
            assertTrue( result.contains( "cn", "entryTest" ) );
            nbFound++;
        }

        assertEquals( 1, nbFound );

        results.close();

        // Delete the entry
        connection.delete( dn );
    }


    /**
     * Check that we can find entries in more than one partition 
     */
    @Test
    public void testSimpleSearch() throws Exception
    {
        // Add an entry in ou=system
        Dn dn1 = new Dn( "cn=test,ou=system" );
        Entry entry1 = new DefaultEntry( getService().getSchemaManager(), dn1,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.add( entry1 );

        // Add an entry in dc=test
        Dn dn2 = new Dn( "cn=test,dc=test,dc=com" );
        Entry entry2 = new DefaultEntry( getService().getSchemaManager(), dn2,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.add( entry2 );

        // Add an entry in dc=example
        Dn dn3 = new Dn( "cn=test,dc=example,dc=com" );
        Entry entry3 = new DefaultEntry( getService().getSchemaManager(), dn3,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.add( entry3 );

        // Now search the entry from the root
        EntryCursor cursor = connection.search( "", "(cn=test)", SearchScope.SUBTREE );
        List<String> entries = new ArrayList<String>();

        while ( cursor.next() )
        {
            Entry entryFound = cursor.get();
            assertNotNull( entryFound );
            entries.add( entryFound.getDn().getName() );
        }

        SearchResultDone done = cursor.getSearchResultDone();

        assertNotNull( done );
        assertEquals( ResultCodeEnum.SUCCESS, done.getLdapResult().getResultCode() );
        assertEquals( 3, entries.size() );
        assertTrue( entries.contains( "cn=test,dc=test,dc=com" ) );
        assertTrue( entries.contains( "cn=test,dc=example,dc=com" ) );
        assertTrue( entries.contains( "cn=test,ou=system" ) );
        cursor.close();
    }


    /**
     * Check that we can find entries in more than one partition 
     */
    @Test
    public void testSearchWithIndex() throws Exception
    {
        int nbIterations = 1000;

        //BufferedWriter out = new BufferedWriter( new FileWriter("/tmp/out.txt") );

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 100 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500 )
            {
                t00 = System.currentTimeMillis();
            }

            String cnStr = "user" + i;
            String rdnStr = "cn=" + cnStr;
            Dn dn = new Dn( rdnStr + ",dc=test,dc=com" );
            Entry entry = new DefaultEntry(
                getService().getSchemaManager(),
                dn,
                "objectClass: top",
                "objectClass: groupOfUniqueNames",
                "cn", cnStr,
                "uniqueMember", dn.toString() );

            connection.add( entry );
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 500 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );


        // Now search the entry from the root
        EntryCursor cursor = connection.search( "", "(uniqueMember=cn=user784,dc=test,dc=com)", SearchScope.SUBTREE );
        List<String> entries = new ArrayList<String>();

        while ( cursor.next() )
        {
            Entry entryFound = cursor.get();
            assertNotNull( entryFound );
            entries.add( entryFound.getDn().getName() );
        }

        SearchResultDone done = cursor.getSearchResultDone();

        assertNotNull( done );
        assertEquals( ResultCodeEnum.SUCCESS, done.getLdapResult().getResultCode() );
        assertEquals( 1, entries.size() );
        assertTrue( entries.contains( "cn=user784,dc=test,dc=com" ) );
        cursor.close();
    }
}
