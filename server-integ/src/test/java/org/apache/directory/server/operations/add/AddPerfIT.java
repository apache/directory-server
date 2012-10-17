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
package org.apache.directory.server.operations.add;


import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the add operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports =
{ @CreateTransport(protocol = "LDAP") })
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
                    //@CreateIndex( attribute = "sn" ),
                    @CreateIndex( attribute = "cn" )
                } )
                
        },
        enableChangeLog = false )
public class AddPerfIT extends AbstractLdapTestUnit
{
    /**
     * Test an add operation performance
     */
    @Test
    public void testAddPerf() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

        Dn dn = new Dn( "cn=test,dc=example,dc=com" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
            "ObjectClass: top", 
            "ObjectClass: person",
            "sn: TEST",
            "cn: test" );

        connection.bind( "uid=admin,ou=system", "secret" );
        connection.add( entry );
        int nbIterations = 1500;

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

            if ( i == 5000 )
            {
                t00 = System.currentTimeMillis();
            }

            String name = "test" + i;
            dn = new Dn( "cn=" + name + ",dc=example,dc=com" );
            entry = new DefaultEntry( getService().getSchemaManager(), dn,
                "ObjectClass: top", 
                "ObjectClass: person",
                "sn", name.toUpperCase(),
                "cn", name );

            long ttt0 = System.nanoTime();
            connection.add( entry );
            long ttt1 = System.nanoTime();
            //System.out.println("added " + i + ", delta = " + (ttt1-ttt0)/1000);
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 5000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );
        
        int nbFound = 0;
        long t2 = System.currentTimeMillis();
        EntryCursor result = connection.search( "dc=example,dc=com", "(sn=test123*)", SearchScope.SUBTREE, "*" );
        
        while ( result.next() )
        {
            Entry res = result.get();
            
            System.out.println( res.getDn() );
            nbFound++;
        }
        
        result.close();
        long t3 = System.currentTimeMillis();
        System.out.println( "Delta search : " + ( t3 - t2 ) + " for " + nbFound + " entries");
        
        connection.close();
    }
}
