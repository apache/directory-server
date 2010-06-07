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
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the search() methods of the provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS( name="SearchDS" )
public class SearchPerfIT extends AbstractLdapTestUnit
{
    /**
    * A basic search for one single entry
    */
   @Test
   public void testSearchPerf() throws Exception
   {
       LdapConnection connection = IntegrationUtils.getAdminConnection( service );

       Cursor<SearchResponse> cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );

       int i = 0;
       
       while ( cursor.next() )
       {
           SearchResponse response = cursor.get();
           ++i;
       }
       
       cursor.close();

       assertEquals( 1, i );

       for ( int j = 0; j < 10000; j++ )
       {
           cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );
           cursor.close();
       }

       long t0 = System.currentTimeMillis();
       
       for ( int j = 0; j < 1000000; j++ )
       {
           if ( j % 10000 == 0 )
           {
               System.out.println(j);
           }
           
           cursor = connection.search( "uid=admin,ou=system", "(ObjectClass=*)", SearchScope.OBJECT, "*" );
           cursor.close();
       }
       
       long t1 = System.currentTimeMillis();
       
       System.out.println( "Delta = " + ( t1 - t0 ) );
       connection.close();
   }
}
