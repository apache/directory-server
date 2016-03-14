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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the addition of a child after a shutdown and restart
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(
    name = "AddAfterShutdownIT",
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
                        "objectClass: domain\n\n"))
    }, enableChangeLog = false)
@ApplyLdifs(
    {
        // A test branch
        "dn: dc=test,dc=example,dc=com",
        "objectClass: top",
        "objectClass: domain",
        "dc: test",
        "",
        // The first level
        "dn: ou=groups,dc=test,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: groups",
        "",
        // entry child
        "dn: cn=imadmin,ou=groups,dc=test,dc=example,dc=com",
        "objectClass: top",
        "objectClass: groupOfUniqueNames",
        "uniqueMember: uid=dummy",
        "description: AdministrationGroup",
        "cn: imadmin",
        "" })
public class AddAfterShutdownIT extends AbstractLdapTestUnit
{

    // The shared LDAP user connection
    protected static LdapConnection userConnection;
    private Map<String, Entry> getAllEntries( LdapConnection connection, String dn ) throws Exception
    {
        Map<String, Entry> results = new HashMap<String, Entry>();

        EntryCursor responses = connection.search( dn, "(objectClass=*)", SearchScope.SUBTREE, "+", "*" );

        while ( responses.next() )
        {
            Entry entry = responses.get();

            results.put( entry.getDn().getName(), entry );
        }

        return results;
    }

    
    /**
     * Add a child
     */
    @Test
    public void testAddChild() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );
        
        Map<String, Entry> results = getAllEntries( connection, "dc=test,dc=example,dc=com" );

//        System.out.println( "Entries found :");
//        System.out.println( "--------------");
//        
//        for ( String dn : results.keySet() )
//        {
//            System.out.println( dn );
//        }
//
//        connection.close();

//        System.out.println( "Stopping the service---------------------------------");
//        System.out.println();
        
        // Shutdown the DirectoryService
        getService().shutdown();
        assertFalse( getService().isStarted() );

//        System.out.println( "Starting the service---------------------------------");

        // And restart it
        getService().startup();
        assertTrue( getService().isStarted() );

        connection = IntegrationUtils.getAdminConnection( getService() );

        // Add the child
        Entry child2 = new DefaultEntry(
            "cn=child2,cn=imadmin,ou=groups,dc=test,dc=example,dc=com",
            "objectClass: top",
            "objectClass: groupOfUniqueNames",
            "cn: child2",
            "uniqueMember: uid=dummy2",
            "description: child2" );

//        SchemaManager schemaManager = getService().getSchemaManager();
//        Dn contextDn = new Dn( schemaManager, "dc=example,dc=com" );
//        MavibotPartition partition = ( MavibotPartition ) getService().getPartitionNexus().getPartition( contextDn );
//        partition.dumpRdnIdx( Partition.ROOT_ID, "" );
        
//        System.out.println( "Adding child2 : " + child2.getDn() );
        connection.add( child2 );

//        partition.dumpRdnIdx( Partition.ROOT_ID, "" );
        
//        Entry found = connection.lookup( "cn=child2,cn=imadmin,ou=groups,dc=test,dc=example,dc=com" );
//        System.out.println( "Child2 exists :\n" + found);
        
        assertTrue( connection.exists( "cn=child2,cn=imadmin,ou=groups,dc=test,dc=example,dc=com" ) );
        results = getAllEntries( connection, "dc=test,dc=example,dc=com" );

//        System.out.println( "Entries found :");
//        System.out.println( "--------------");
//        
//        for ( String dn : results.keySet() )
//        {
//            System.out.println( dn );
//        }
        
        // --------------------------------------------------------------------
        // Make sure entries not selected by subentryA do not have the mark
        // --------------------------------------------------------------------
        connection.close();
        
//        System.out.println( "Stopping the service---------------------------------");
//        System.out.println();

        // Now shutdown the DirectoryService
        getService().shutdown();
        assertFalse( getService().isStarted() );

//        System.out.println( "Starting the service---------------------------------");

        // And restart it
        getService().startup();
        assertTrue( getService().isStarted() );

        // Fetch the entries again
        connection = IntegrationUtils.getAdminConnection( getService() );

        Entry found = connection.lookup( "cn=child2,cn=imadmin,ou=groups,dc=test,dc=example,dc=com" );
//        System.out.println( "Child2 STILL exists :\n" + found);
        

        // Check the resulting modifications
        results = getAllEntries( connection, "dc=test,dc=example,dc=com" );

//        System.out.println();

//        System.out.println( "Entries found :");
//        System.out.println( "--------------");
        
        int count = 0;
        
        for ( String dn : results.keySet() )
        {
            count++;
//            System.out.println( dn );
        }

        connection.close();
        
        assertEquals( 4, count );
    }
}
