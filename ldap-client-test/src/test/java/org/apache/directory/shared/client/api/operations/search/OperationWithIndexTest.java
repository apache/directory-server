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
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(FrameworkRunner.class)
@CreateDS(
    name="AddPerfDS",
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
                    "objectClass: domain\n\n" ),
            indexes =
            {
                @CreateIndex( attribute = "objectClass" ),
                @CreateIndex( attribute = "sn" ),
                @CreateIndex( attribute = "cn" ),
                @CreateIndex( attribute = "displayName" )
            } )
            
    },
    enableChangeLog = false )
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
        connection = (LdapNetworkConnection)LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );

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
    
            if ( i == 5000 )
            {
                t00 = System.currentTimeMillis();
            }
    
            dn = new Dn( "uid=" + i + ",dc=example,dc=com" );
            entry = new DefaultEntry( getService().getSchemaManager(), dn,
                "objectClass: top",
                "objectClass: person",
                "objectClass: organizationalPerson",
                "objectClass: inetOrgPerson",
                "uid", Integer.toString( i ),
                "mail: A-A-R.Awg-Rosli@acme.com",
                "title: Snr Operations Technician (D)",
                "sn: Awg-Rosli",
                "departmentNumber: SMDS - UIA/G/MMO52D",
                "cn: Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D",
                "description: UI - S",
                "telephoneNumber: 555-1212",
                "givenName: Awg-Abd-Rahim",
                "businessCategory: Ops MDS (Malaysia) Sdn Bhd",
                "displayName", i + "Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D",
                "employeeNumber: A-A-R.Awg-Rosli",
                "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );
    
            //out.write( LdifUtils.convertToLdif( entry ) );
            connection.add( entry );
        }
        
        //out.flush();
        //out.close();
    
        long t1 = System.currentTimeMillis();
    
        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 5000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );

        Entry entry1 = null;
        Entry entry2 = null;
        Entry entry3 = null;
        
        long ns0 = System.currentTimeMillis();
        EntryCursor results = connection.search("dc=example,dc=com", "(displayName=1234Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D)", SearchScope.SUBTREE, "*" );
        
        while ( results.next() )
        {
            entry1 = results.get();
            break;
        }
        
        results.close();
        
        long ns1 = System.currentTimeMillis();
        
        System.out.println( "Delta search : " + ( ns1 - ns0 ) );

        long ns2 = System.currentTimeMillis();
        results = connection.search("dc=example,dc=com", "(displayName=3456*)", SearchScope.SUBTREE, "*" );
                
        while ( results.next() )
        {
            entry2 = results.get();
            break;
        }
        results.close();
        long ns3 = System.currentTimeMillis();
        
        System.out.println( "Delta search substring : " + ( ns3 - ns2 ) );

        long ns4 = System.currentTimeMillis();
        results = connection.search("dc=example,dc=com", "(uid=6789)", SearchScope.SUBTREE, "*" );
                
        while ( results.next() )
        {
            entry3 = results.get();
            break;
        }
        
        results.close();
        long ns5 = System.currentTimeMillis();
        
        System.out.println( "Delta search no index : " + ( ns5 - ns4 ) );

        System.out.println( "Entry 1 : " + entry1 );
        System.out.println( "Entry 2 : " + entry2 );
        System.out.println( "Entry 3 : " + entry3 );
        connection.close();
        
        // Now, shutdown and restart the server once more
        getService().shutdown();
        getService().startup();
        
        // and do a search again
        connection = (LdapNetworkConnection)LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );

        long ns6 = System.currentTimeMillis();
        results = connection.search("dc=example,dc=com", "(displayName=3456*)", SearchScope.SUBTREE, "*" );
                
        while ( results.next() )
        {
            entry3 = results.get();
            break;
        }
        
        results.close();
        long ns7 = System.currentTimeMillis();
        System.out.println( "New Delta search substring : " + ( ns7 - ns6 ) );

        connection.close();
    }
    
    
    @Test
    public void testModify() throws Exception
    {
        
        // Add the entry
        Dn dn = new Dn( "uid=1,dc=example,dc=com" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "uid: 1",
            "mail: A-A-R.Awg-Rosli@acme.com",
            "title: Snr Operations Technician (D)",
            "sn: Awg-Rosli",
            "departmentNumber: SMDS - UIA/G/MMO52D",
            "cn: Awg-Rosli, Awg-Abd-Rahim SMDS-UIA/G/MMO52D",
            "description: UI - S",
            "telephoneNumber: 555-1212",
            "givenName: Awg-Abd-Rahim",
            "businessCategory: Ops MDS (Malaysia) Sdn Bhd",
            "displayName: 1Awg-Rosli",
            "employeeNumber: A-A-R.Awg-Rosli",
            "pwdPolicySubEntry: ads-pwdId=cproint,ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );

        connection.add( entry );
        
        EntryCursor results = connection.search("dc=example,dc=com", "(displayName=1*)", SearchScope.SUBTREE, "*" );
        
        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "1Awg-Rosli" ) );
        }
        
        results.close();
        
        // Now, modify it
        connection.modify( dn, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "displayName", "test" ) );
        
        results = connection.search("dc=example,dc=com", "(displayName=t*)", SearchScope.SUBTREE, "*" );
        
        while ( results.next() )
        {
            Entry result = results.get();
            assertTrue( result.contains( "displayName", "test" ) );
        }
        
        results.close();

        results = connection.search("dc=example,dc=com", "(displayName=1*)", SearchScope.SUBTREE, "*" );
        
        assertFalse( results.next() );
        
        results.close();
    }
}
