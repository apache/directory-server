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
import org.apache.directory.api.ldap.model.message.SearchScope;
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

import static org.junit.Assert.assertTrue;


/**
 * Tests various search scenarios.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS( name = "SearchWithIndicesIT" )
public class SearchWithIndicesPersonIT extends AbstractLdapTestUnit
{
    private static LdapConnection connection;

    @Before
    public void createData() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
        Partition systemPartition = getService().getSystemPartition();
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
        dsFactory.getPartitionFactory().addIndex( systemPartition, "cn", 100 );

        // Restart the service so that the index is created
        getService().shutdown();
        getService().startup();

        // -------------------------------------------------------------------
        // Add a bunch of persons
        // -------------------------------------------------------------------
        for ( int i = 0; i < 1000; i++ )
        {
            addPerson( "name" + i, i );
        }
    }


    private void addPerson( String name, int id ) throws Exception
    {
        connection.add(
            new DefaultEntry(
                "cn=" + name + ",ou=system",
                "objectClass: top",
                "objectClass: person",
                "cn", name,
                "sn", name
                ) );
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
    public Set<String> searchPersons( String filter ) throws Exception
    {
        Set<String> results = new HashSet<String>();

        long t0 = System.nanoTime();
        EntryCursor cursor = connection.search( "ou=system", filter, SearchScope.SUBTREE, "1.1" );
        long t1 = System.nanoTime();
        
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
        Set<String> results = searchPersons( "(&(cn=name10*)(objectClass=person))" );
        Set<String> expected = new HashSet<String>();
        expected.add( "cn=name10,ou=system" );
        expected.add( "cn=name100,ou=system" );
        expected.add( "cn=name101,ou=system" );
        expected.add( "cn=name102,ou=system" );
        expected.add( "cn=name103,ou=system" );
        expected.add( "cn=name104,ou=system" );
        expected.add( "cn=name105,ou=system" );
        expected.add( "cn=name106,ou=system" );
        expected.add( "cn=name107,ou=system" );
        expected.add( "cn=name108,ou=system" );
        expected.add( "cn=name109,ou=system" );
        
        for ( String person : results )
        {
            assertTrue( expected.contains( person ) );
            
            expected.remove( person );
        }
        
        assertTrue( expected.size() == 0 );
    }
}
