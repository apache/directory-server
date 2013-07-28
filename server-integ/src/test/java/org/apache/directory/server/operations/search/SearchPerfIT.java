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
package org.apache.directory.server.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.ldap.client.api.EntryCursorImpl;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcase with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class SearchPerfIT extends AbstractLdapTestUnit
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );


    /**
     * test a search request perf.
     */
    @Test
    public void testSearchRequestObjectScopePerf() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        long deltaSearch = 0L;
        long deltaGet = 0L;
        long deltaClose = 0L;

        try
        {
            // Use the client API as JNDI cannot be used to do a search without
            // first binding. (hmmm, even client API won't allow searching without binding)
            connection.bind( "uid=admin,ou=system", "secret" );

            // Searches for all the entries in ou=system
            EntryCursor cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)",
                SearchScope.OBJECT, "*" );

            int i = 0;

            while ( cursor.next() )
            {
                cursor.get();
                ++i;
            }

            cursor.close();
            assertEquals( 1, i );

            for ( int j = 0; j < 10000; j++ )
            {
                cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );

                while ( cursor.next() )
                {
                }

                cursor.close();
            }

            Dn dn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );

            SearchRequest searchRequest = new SearchRequestImpl();

            searchRequest.setBase( dn );
            searchRequest.setFilter( "(ObjectClass=*)" );
            searchRequest.setScope( SearchScope.OBJECT );
            searchRequest.addAttributes( "*" );
            searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

            long t0 = System.currentTimeMillis();
            long t00 = 0L;
            long tt0 = System.currentTimeMillis();
            int nbIterations = 200000;
            int count = 0;

            for ( int j = 0; j < nbIterations; j++ )
            {
                if ( j % 10000 == 0 )
                {
                    long tt1 = System.currentTimeMillis();

                    System.out.println( j + ", " + ( tt1 - tt0 ) );
                    tt0 = tt1;
                }

                if ( j == 50000 )
                {
                    t00 = System.currentTimeMillis();
                }

                long dt0 = System.nanoTime();
                cursor = new EntryCursorImpl( connection.search( searchRequest ) );
                long dt1 = System.nanoTime();

                deltaSearch += Math.abs( dt1 - dt0 );

                while ( cursor.next() )
                {
                    long dt2 = System.nanoTime();
                    cursor.get();
                    count++;
                    long dt3 = System.nanoTime();

                    deltaGet += Math.abs( dt3 - dt2 );
                }

                long dt4 = System.nanoTime();
                cursor.close();
                long dt5 = System.nanoTime();

                deltaClose += Math.abs( dt5 - dt4 );
            }

            long t1 = System.currentTimeMillis();

            Long deltaWarmed = ( t1 - t00 );
            System.out.println( "OBJECT level - Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed )
                + " per s ) /" + ( t1 - t0 ) + ", count : " + count );

            System.out.println( "DeltaSearch : " + ( deltaSearch / nbIterations ) );
            System.out.println( "DeltaGet : " + ( deltaGet / nbIterations ) );
            System.out.println( "DeltaClose : " + ( deltaClose / nbIterations ) );
        }
        catch ( LdapException e )
        {
            e.printStackTrace();
            fail( "Should not have caught exception." );
        }
        finally
        {
            connection.unBind();
            connection.close();
        }
    }


    /**
     * test a search request perf.
     */
    @Test
    public void testSearchRequestOneLevelScopePerf() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

        try
        {
            // Use the client API as JNDI cannot be used to do a search without
            // first binding. (hmmm, even client API won't allow searching without binding)
            connection.bind( "uid=admin,ou=system", "secret" );

            // Searches for all the entries in ou=system
            EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
                SearchScope.ONELEVEL, "*" );

            int i = 0;

            while ( cursor.next() )
            {
                cursor.get();
                ++i;
            }

            cursor.close();
            assertEquals( 5, i );

            for ( int j = 0; j < 10000; j++ )
            {
                cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.ONELEVEL, "*" );

                while ( cursor.next() )
                {
                    cursor.get();
                }

                cursor.close();
            }

            Dn dn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );

            SearchRequest searchRequest = new SearchRequestImpl();

            searchRequest.setBase( dn );
            searchRequest.setFilter( "(ObjectClass=*)" );
            searchRequest.setScope( SearchScope.ONELEVEL );
            searchRequest.addAttributes( "*" );
            searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

            long t0 = System.currentTimeMillis();
            long t00 = 0L;
            long tt0 = System.currentTimeMillis();
            int nbIterations = 200000;
            int count = 0;

            for ( int j = 0; j < nbIterations; j++ )
            {
                if ( j % 10000 == 0 )
                {
                    long tt1 = System.currentTimeMillis();

                    System.out.println( j + ", " + ( tt1 - tt0 ) );
                    tt0 = tt1;
                }

                if ( j == 50000 )
                {
                    t00 = System.currentTimeMillis();
                }

                cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.ONELEVEL, "*" );

                while ( cursor.next() )
                {
                    count++;
                    cursor.get();
                }

                cursor.close();
            }

            long t1 = System.currentTimeMillis();

            Long deltaWarmed = ( t1 - t00 );
            System.out.println( "ONE level - Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed ) * 5
                + " per s ) /" + ( t1 - t0 ) + ", count : " + count );
        }
        catch ( LdapException e )
        {
            e.printStackTrace();
            fail( "Should not have caught exception." );
        }
        finally
        {
            connection.unBind();
            connection.close();
        }
    }


    /**
     * test a search request perf.
     */
    @Test
    public void testSearchRequestSubtreeLevelScopePerf() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        connection.setTimeOut( 0 );

        try
        {
            // Use the client API as JNDI cannot be used to do a search without
            // first binding. (hmmm, even client API won't allow searching without binding)
            connection.bind( "uid=admin,ou=system", "secret" );

            // Searches for all the entries in ou=system
            EntryCursor cursor = connection.search( "ou=system", "(ObjectClass=*)",
                SearchScope.SUBTREE, "*" );

            int i = 0;

            while ( cursor.next() )
            {
                cursor.get();
                ++i;
            }

            cursor.close();
            assertEquals( 10, i );

            for ( int j = 0; j < 10000; j++ )
            {
                cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.SUBTREE, "*" );

                while ( cursor.next() )
                {
                    cursor.get();
                }

                cursor.close();
            }

            Dn dn = new Dn( getService().getSchemaManager(), "uid=admin,ou=system" );

            SearchRequest searchRequest = new SearchRequestImpl();

            searchRequest.setBase( dn );
            searchRequest.setFilter( "(ObjectClass=*)" );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.addAttributes( "*" );
            searchRequest.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );

            long t0 = System.currentTimeMillis();
            long t00 = 0L;
            long tt0 = System.currentTimeMillis();
            int nbIterations = 200000;
            int count = 0;

            for ( int j = 0; j < nbIterations; j++ )
            {
                if ( j % 10000 == 0 )
                {
                    long tt1 = System.currentTimeMillis();

                    System.out.println( j + ", " + ( tt1 - tt0 ) );
                    tt0 = tt1;
                }

                if ( j == 50000 )
                {
                    t00 = System.currentTimeMillis();
                }

                cursor = connection.search( "ou=system", "(ObjectClass=*)", SearchScope.SUBTREE, "*" );

                while ( cursor.next() )
                {
                    count++;
                    cursor.get();
                }

                cursor.close();
            }

            long t1 = System.currentTimeMillis();

            Long deltaWarmed = ( t1 - t00 );
            System.out.println( "SUB level - Delta : " + deltaWarmed + "( " + ( ( ( nbIterations - 50000 ) * 1000 ) / deltaWarmed )
                * 10
                + " per s ) /" + ( t1 - t0 ) + ", count : " + count );
        }
        catch ( LdapException e )
        {
            e.printStackTrace();
            fail( "Should not have caught exception." );
        }
        finally
        {
            connection.unBind();
            connection.close();
        }
    }
}
