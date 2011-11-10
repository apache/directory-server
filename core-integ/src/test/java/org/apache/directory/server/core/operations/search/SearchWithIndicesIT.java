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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests various search scenarios.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS( name = "SearchWithIndicesIT" )
public class SearchWithIndicesIT extends AbstractLdapTestUnit
{
    private static LdapConnection connection;

    @Before
    public void createData() throws Exception
    {
        // -------------------------------------------------------------------
        // Enable the nis schema
        // -------------------------------------------------------------------

        // check if nis is disabled
        connection = IntegrationUtils.getAdminConnection( getService() );

        Entry nisEntry = connection.lookup( "cn=nis,ou=schema" );

        boolean isNisDisabled = nisEntry.contains( "m-disabled", "TRUE" );

        // if nis is disabled then enable it
        if ( isNisDisabled )
        {
            connection.modify( "cn=nis,ou=schema", new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, "m-disabled", "TRUE" ) );
        }

        nisEntry = connection.lookup( "cn=nis,ou=schema" );
        isNisDisabled = nisEntry.contains( "m-disabled", "TRUE" );

        Partition systemPartition = getService().getSystemPartition();
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
        dsFactory.getPartitionFactory().addIndex( systemPartition, "gidNumber", 100 );
        
        // Restart the service so that the index is created
        getService().shutdown();
        getService().startup();

        // -------------------------------------------------------------------
        // Add a bunch of nis groups
        // -------------------------------------------------------------------

        addNisPosixGroup( "testGroup0", 0 );
        addNisPosixGroup( "testGroup1", 1 );
        addNisPosixGroup( "testGroup2", 2 );
        addNisPosixGroup( "testGroup4", 4 );
        addNisPosixGroup( "testGroup5", 5 );
    }


    private void addNisPosixGroup( String name, int gid ) throws Exception
    {
        connection.add(
            new DefaultEntry( 
                "cn=" + name + ",ou=groups, ou=system",
                "objectClass: top",
                "objectClass: posixGroup",
                "cn", name,
                "gidNumber", Integer.toString( gid )
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
    public Set<String> searchGroups( String filter ) throws Exception
    {
        Set<String> results = new HashSet<String>();

        EntryCursor cursor = connection.search( "ou=groups,ou=system", filter, SearchScope.SUBTREE, "1.1" );

        while ( cursor.next() )
        {
            results.add( cursor.get().getDn().getName() );
        }

        return results;
    }


    @Test
    public void testLessThanSearchWithIndices() throws Exception
    {
        Set<String> results = searchGroups( "(gidNumber<=5)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=4)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=3)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber<=-1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }


    @Test
    public void testGreaterThanSearchWithIndices() throws Exception
    {
        Set<String> results = searchGroups( "(gidNumber>=0)" );
        assertTrue( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=1)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=3)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertTrue( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );

        results = searchGroups( "(gidNumber>=6)" );
        assertFalse( results.contains( "cn=testGroup0,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup1,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup2,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup3,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup4,ou=groups,ou=system" ) );
        assertFalse( results.contains( "cn=testGroup5,ou=groups,ou=system" ) );
    }
}
