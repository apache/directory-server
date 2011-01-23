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
package org.apache.directory.server.core.operations.moveAndRename;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the move operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "MovePerfDS", 
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
            }) 
   }, 
   enableChangeLog = false)
public class MoveAndRenamePerfIT extends AbstractLdapTestUnit
{
    /**
     * Test a move operation performance
     */
    @Test
    public void testMovePerf() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( service );

        Dn oldDn = new Dn( "cn=testOld,ou=system" );
        Dn newDn = new Dn( "cn=testNew,ou=users,ou=system" );

        Entry entry = new DefaultEntry( service.getSchemaManager(), oldDn );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "sn", "TESTOld" );
        entry.add( "cn", "testOld" );

        connection.add( entry );
        int nbIterations = 25000;

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

            if ( i == 15000 )
            {
                t00 = System.currentTimeMillis();
            }

            long ttt0 = System.nanoTime();

            connection.moveAndRename( oldDn, newDn );

            Entry oldEntry = connection.lookup( oldDn.getName() );
            Entry newEntry = connection.lookup( newDn.getName() );

            assertNull( oldEntry );
            assertNotNull( newEntry );
            long ttt1 = System.nanoTime();

            // Swap the dn
            Dn tmpDn = newDn;
            newDn = oldDn;
            oldDn = tmpDn;

            //System.out.println("added " + i + ", delta = " + (ttt1-ttt0)/1000);
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 15000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }
}
