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

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchScope;
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
    /**
     * test a search request perf.
     */
    @Test
    public void testSearchRequestPerf() throws Exception
    {
        //getLdapServer().getDirectoryService().getInterceptorChain().addFirst( new TimerInterceptor( "Start" ) );
        //getLdapServer().getDirectoryService().getInterceptorChain().addLast( new TimerInterceptor( "End" ) );
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

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

            long t0 = System.currentTimeMillis();
            int nbIterations = 200000;

            for ( int j = 0; j < nbIterations; j++ )
            {
                if ( j % 10000 == 0 )
                {
                    System.out.println( j );
                }

                cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );
                while ( cursor.next() )
                {
                }
                cursor.close();
            }

            long t1 = System.currentTimeMillis();

            Long deltaWarmed = ( t1 - t0 );
            System.out.println( "Delta : " + deltaWarmed + "( " + ( ( nbIterations * 1000 ) / deltaWarmed )
                + " per s ) /" + ( t1 - t0 ) );
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
