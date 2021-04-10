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
package org.apache.directory.server.core.operations.compare;


import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
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
 * Tests performance of the compare operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "ComparePerfDS", 
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
                @CreateIndex(attribute = "cn") 
            } )
    }, 
    enableChangeLog = false )
public class ComparePerfIT extends AbstractLdapTestUnit
{
    /**
     * Compare a member attribute. This test is used to check DIRSERVER-1139
     */
    @Test
    public void testComparePerf() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "cn=test,dc=example,dc=com" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "sn", "TEST" );
        entry.add( "cn", "test" );

        connection.add( entry );
        int nbIterations = 150000;

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

            if ( i == 50000 )
            {
                t00 = System.currentTimeMillis();
            }

            connection.compare( dn, "sn", "TEST" );
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta compare: " + deltaWarmed + "( "
            + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );

        connection.close();
    }
}
