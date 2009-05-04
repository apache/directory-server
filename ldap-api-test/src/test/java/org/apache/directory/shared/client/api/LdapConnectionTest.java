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
import org.apache.directory.shared.ldap.client.api.listeners.BindListener;
import org.apache.directory.shared.ldap.client.api.messages.BindRequest;
import org.apache.directory.shared.ldap.client.api.messages.BindRequestImpl;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.client.api.messages.future.BindFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
public class LdapConnectionTest
{
    /** The server instance */
    public static LdapService ldapService;
    
    private static boolean responseReceived = false;

    @Before
    public void init()
    {
        responseReceived = false;
    }
    
    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequest()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            BindResponse bindResponse = connection.bind( "uid=admin,ou=system", "secret" );
            
            assertNotNull( bindResponse );
            
            connection.unBind();
        }
        catch ( LdapException le )
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
     * Test a successful asynchronous bind request
     *
     * @throws IOException
     */
    @Test
    public void testAsyncBindRequest()
    {
        LdapConnection connection = new LdapConnection( "localhost", ldapService.getPort() );
        
        try
        {
            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setCredentials( "secret" );
            bindRequest.setName( "uid=admin,ou=system" );
            
            connection.bind( bindRequest, new BindListener() 
                {
                    public void bindCompleted( LdapConnection connection, BindResponse bindResponse ) throws LdapException
                    {
                        assertNotNull( bindResponse );
                        responseReceived = true;
                    }
                } );

            // Wait a bit
            Thread.sleep( 1000 );
            
            assertTrue( responseReceived );
            
            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
        catch ( InterruptedException ie )
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
}
