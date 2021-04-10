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
package org.apache.directory.server.core.operations.add;


import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the add operation with some concurrent searches
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
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
                        @CreateIndex(attribute = "cn")
                })

    },
    enableChangeLog = false)
public class ConcurrentAddSearchIT extends AbstractLdapTestUnit
{
    private int nbAdded = 0;

    private class AddThread extends Thread
    {
        public void run()
        {
            try
            {
                LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

                try
                {
                    for ( int i = 0; i < 10000; i++ )
                    {
                        Dn dn = new Dn( "cn=test" + i + ",dc=example,dc=com" );
                        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn,
                            "ObjectClass: top",
                            "ObjectClass: person",
                            "sn: TEST",
                            "cn", "test" + i );

                        connection.add( entry );
                        nbAdded++;
                    }
                }
                finally
                {
                    connection.close();
                }
            }
            catch ( LdapException le )
            {
                System.out.println( "-------------> LdapException occured on" + nbAdded + " addition : "
                    + le.getMessage() );
                le.printStackTrace();
            }
            catch ( Exception e )
            {
                System.out.println( "-------------> Exception occured on" + nbAdded + " addition : " + e.getMessage() );
                e.printStackTrace();
            }
        }
    }


    /**
     * Test some concurrent search and add operation performance
     */
    @Test
    @Disabled
    public void testConcurrentSearch() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        AddThread addThread = new AddThread();

        addThread.start();

        // Now that we have started the add thread, let's do some searches

        for ( int i = 0; i < 100; i++ )
        {
            try
            {
                EntryCursor results = connection.search( "dc=example,dc=com", "(cn=*)", SearchScope.SUBTREE, "*" );

                int nbFound = 0;

                while ( results.next() && ( nbFound < 1000 ) )
                {
                    Entry result = results.get();
                    nbFound++;
                }

                System.out.println( "Running " + i + "th search, getting back " + nbFound
                    + " entries, nb added entries : " + nbAdded );

                results.close();

                if ( nbAdded >= 10000 )
                {
                    break;
                }

                Thread.sleep( 1000 );
            }
            catch ( Exception e )
            {
                System.out.println( "-------------> Exception occured on " + i + "th search : " + e.getMessage() );
                e.printStackTrace();
            }
        }

        connection.close();
    }
}
