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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test for MV attributes with index
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SearchMVWithIndicesIT")
public class SearchWithIndexedMVAttributeIT extends AbstractLdapTestUnit
{
    private static LdapConnection connection;


    @Before
    public void createData() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );

        Partition systemPartition = getService().getSystemPartition();
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();

        // Add an index for the Member AT
        dsFactory.getPartitionFactory().addIndex( systemPartition, "member", 100 );

        // Restart the service so that the index is created
        getService().shutdown();
        getService().startup();

        // -------------------------------------------------------------------
        // Add an entry with a groupOfNames OC
        // -------------------------------------------------------------------

        addGroupOfNames( "testGroup0", 0 );
        addGroupOfNames( "testGroup1", 1 );
        addGroupOfNames( "testGroup2", 2 );
        addGroupOfNames( "testGroup4", 4 );
        addGroupOfNames( "testGroup5", 5 );

        // now, add thousands of members in some of those entries 
    }


    private void addGroupOfNames( String name, int number ) throws Exception
    {
        String dn = "cn=" + name + ",ou=groups,ou=system";
        connection.add(
            new DefaultEntry(
                dn,
                "objectClass: top",
                "objectClass: groupOfnames",
                "cn", name,
                "member", "cn=test,ou=users,ou=system"
            ) );

        // now, add thousands of members in some of those entries
        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( new Dn( dn ) );

        for ( int i = 0; i < number * 320; i++ )
        {
            modRequest.add( "member", "cn=test" + i + ",ou=users,ou=system" );

        }

        connection.modify( modRequest );
    }


    /**
     *  Convenience method that performs a one level search using the
     *  specified filter returning their DNs as Strings in a set.
     *
     * @param controls the search controls
     * @param filter the filter expression
     * @return the set of groups
     * @throws Exception if there are problems conducting the search
     */
    public Set<String> searchGroups( String filter ) throws Exception
    {
        Set<String> results = new HashSet<String>();

        EntryCursor cursor = connection.search( "ou=groups,ou=system", filter, SearchScope.SUBTREE, "1.1" );

        while ( cursor.next() )
        {
            results.add( cursor.get().getDn().getName() );
        }

        cursor.close();

        return results;
    }


    @Test
    public void testSearch() throws Exception
    {
        long t0 = System.currentTimeMillis();
        EntryCursor cursor = connection.search( "ou=system",
            "(&(member=cn=test74,ou=users,ou=system)(objectClass=groupOfNames))",
            SearchScope.SUBTREE,
            "member" );

        while ( cursor.next() )
        {
            System.out.println( cursor.get().getDn() );
        }

        cursor.close();
        long t1 = System.currentTimeMillis();
        System.out.println( "Search done in " + ( t1 - t0 ) + "msec" );
    }
}
