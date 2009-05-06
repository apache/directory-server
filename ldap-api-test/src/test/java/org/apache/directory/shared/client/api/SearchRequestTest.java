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
package org.apache.directory.shared.client.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.SearchListener;
import org.apache.directory.shared.ldap.client.api.messages.Response;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequest;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequestImpl;
import org.apache.directory.shared.ldap.client.api.messages.SearchResponse;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultDone;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultReference;
import org.apache.directory.shared.ldap.client.api.messages.future.SearchFuture;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

/**
 * Test the LdapConnection class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
public class SearchRequestTest
{
    /** The server instance */
    public static LdapService ldapService;

    /** A list containing asynchronous results */
    private List<SearchResponse> entries;
    
    /** A flag set to true when the searchResultDone has been received for an async search operation */
    private boolean done = false;
    
    //------------------------------------------------------------------------
    // Synchronous Search
    //------------------------------------------------------------------------
    /**
     * Test a search request on rootDSE
     */
    @Test
    public void testSearchNoArgs()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            SearchResultEntry rootDSE = connection.search();
            
            assertNotNull( rootDSE );

            assertTrue(  rootDSE instanceof SearchResultEntry );
            
            assertEquals( 2, rootDSE.getMessageId() );
            Entry entry = rootDSE.getEntry();
            
            assertNotNull( entry );
            assertEquals( "", entry.getDn().toString() );
            assertTrue( entry.contains( "ObjectClass", "top", "extensibleObject" ) );
            assertFalse( entry.contains( "vendorName", "Apache Software Foundation" ) );
            
            connection.unBind();
        }
        catch ( LdapException le )
        {
            le.printStackTrace();
            fail();
        }
        catch ( Exception e )
        {
            fail();
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch( IOException ioe )
            {
                fail();
            }
        }
    }

    
    /**
     * Test a simple search request
     */
    @Test
    public void testSearchRequest()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            Cursor<SearchResponse> cursor = 
                connection.search( "uid=admin,ou=system", "(objectClass=*)", SearchScope.SUBTREE, "*" );
            
            assertNotNull( cursor );

            SearchResponse response = null;
            int count = 0;

            while ( cursor.available() )
            {
                response = cursor.get();
                assertNotNull( response );
                assertTrue(  response instanceof SearchResultEntry );
                SearchResultEntry searchResultEntry = (SearchResultEntry)response;
                
                assertEquals( 2, searchResultEntry.getMessageId() );
                Entry entry = searchResultEntry.getEntry();
                
                assertNotNull( entry );
                assertEquals( "uid=admin,ou=system", entry.getDn().toString() );
                count++;
                
                cursor.next();
            } 
            
            assertEquals( 1, count );
            
            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
        catch ( Exception e )
        {
            fail();
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch( IOException ioe )
            {
                fail();
            }
        }
    }


    /**
     * Test a simple async search request
     */
    @Test
    public void testAsyncSearchRequest()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBaseDn( "uid=admin,ou=system" );
            searchRequest.setFilter( "(objectClass=*)" );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.addAttributes( "*" );

            entries = new ArrayList<SearchResponse>();
            
            SearchFuture searchFuture = connection.search( searchRequest, new SearchListener() 
            {
                public void entryFound( LdapConnection connection, SearchResultEntry searchResultEntry ) throws LdapException
                {
                    assertFalse( done );
                    assertNotNull( searchResultEntry );
                    assertEquals( 2, searchResultEntry.getMessageId() );
                    entries.add( searchResultEntry );
                    
                    System.out.println( "Received : " + searchResultEntry );
                }
                
                public void referralFound( LdapConnection connection, SearchResultReference searchResultReference ) throws LdapException
                {
                    assertFalse( done );
                    assertNotNull( searchResultReference );
                    assertEquals( 2, searchResultReference.getMessageId() );
                    entries.add( searchResultReference );
                }

                public void searchDone( LdapConnection connection, SearchResultDone searchResultDone ) throws LdapException
                {
                    assertFalse( done );
                    assertNotNull( searchResultDone );
                    assertEquals( 2, searchResultDone.getMessageId() );
                    done = true;
                }
            } );

            int count = 0;
            
            // Wait for the search to be done
            while ( ! searchFuture.isDone() )
            {
                Response response = searchFuture.get();
                
                if ( response instanceof SearchResultDone )
                {
                    break;
                }
                
                count ++;
            }
            
            assertEquals( 1, count );
            assertEquals( count, entries.size() );
            
            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
        catch ( Exception e )
        {
            fail();
        }
        finally
        {
            entries = null;
            done = false;
            
            try
            {
                connection.close();
            }
            catch( IOException ioe )
            {
                fail();
            }
        }
    }
}
