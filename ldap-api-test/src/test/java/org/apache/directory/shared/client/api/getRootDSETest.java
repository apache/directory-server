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

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

/**
 * Test the getRootDSE methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
public class getRootDSETest
{
    /** The server instance */
    public static LdapService ldapService;
    
    //------------------------------------------------------------------------
    // Synchronous getRootDSE()
    //------------------------------------------------------------------------
    /**
     * Test a simple getRootDSE() call.
     */
    @Test
    public void testGetRootDSE()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            SearchResultEntry rootDSE = connection.getRootDSE();
            
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
     * Test a getRootDSE() call where we want all the operational and users attributes .
     */
    @Test
    public void testGetRootDSEAllAttrs()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            SearchResultEntry rootDSE = connection.getRootDSE( "*", "+" );
            
            assertNotNull( rootDSE );

            assertTrue(  rootDSE instanceof SearchResultEntry );
            
            assertEquals( 2, rootDSE.getMessageId() );
            Entry entry = rootDSE.getEntry();
            
            assertNotNull( entry );
            assertEquals( "", entry.getDn().toString() );
            assertTrue( entry.contains( "ObjectClass", "top", "extensibleObject" ) );
            assertTrue( entry.contains( "subschemaSubentry", "cn=schema" ) );
            assertTrue( entry.contains( "vendorName", "Apache Software Foundation" ) );
            assertTrue( entry.contains( "supportedLDAPVersion", "3" ) );
            
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
     * Test a getRootDSE() call where we want all the operational attributes .
     */
    @Test
    public void testGetRootDSEOperAttrs()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            SearchResultEntry rootDSE = connection.getRootDSE( "+" );
            
            assertNotNull( rootDSE );

            assertTrue(  rootDSE instanceof SearchResultEntry );
            
            assertEquals( 2, rootDSE.getMessageId() );
            Entry entry = rootDSE.getEntry();
            
            assertNotNull( entry );
            assertEquals( "", entry.getDn().toString() );
            assertFalse( entry.contains( "ObjectClass", "top", "extensibleObject" ) );
            assertTrue( entry.contains( "subschemaSubentry", "cn=schema" ) );
            assertTrue( entry.contains( "vendorName", "Apache Software Foundation" ) );
            assertTrue( entry.contains( "supportedLDAPVersion", "3" ) );
            
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


    //------------------------------------------------------------------------
    // Asynchronous getRootDSE()
    //------------------------------------------------------------------------
}
