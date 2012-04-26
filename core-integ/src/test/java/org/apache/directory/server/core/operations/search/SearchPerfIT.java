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


import static org.junit.Assert.assertEquals;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SearchPerfDS")
public class SearchPerfIT extends AbstractLdapTestUnit
{
    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfObjectScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)",
            SearchScope.OBJECT, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 1, i );

        int nbIterations = 1500000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );
            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfOneLevelScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
            SearchScope.ONELEVEL, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 5, i );

        int nbIterations = 1500000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.ONELEVEL, "*" );
            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }


    /**
    * A basic search for one single entry
    */
    @Test
    public void testSearchPerfSublevelScope() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
            SearchScope.SUBTREE, "*" );

        int i = 0;

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            ++i;
        }

        cursor.close();

        assertEquals( 10, i );

        int nbIterations = 1500000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();

        for ( i = 0; i < nbIterations; i++ )
        {
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.SUBTREE, "*" );
            cursor.close();
        }

        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed )
            + " per s ) /" + ( t1 - t0 ) );
        connection.close();
    }
}
