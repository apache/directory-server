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
package org.apache.directory.shared.client.api.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.BindListener;
import org.apache.directory.shared.ldap.client.api.messages.BindRequest;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the BindRequest operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
public class ClientBindRequestTest
{
    /** The server instance */
    public static LdapServer ldapServer;

    private LdapConnection connection;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
    }

    
    /**
     * Test a successful synchronous bind request
     *
     * @throws IOException
     */
    @Test
    public void testSyncBindRequest() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
        
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
    public void testAsyncBindRequest() throws Exception
    {
        int i = 0;
        int nbLoop = 10;
        final Semaphore lock = new Semaphore( 1 );

        try
        {
            long t0 = System.currentTimeMillis();
            lock.acquire();
            
            for ( ; i < nbLoop; i++)
            {
                connection = new LdapConnection( "localhost", ldapServer.getPort() );
                
                BindRequest bindRequest = new BindRequest();
                bindRequest.setName( "uid=admin,ou=system" );
                bindRequest.setCredentials( "secret" );
                
    
                connection.bind( bindRequest, new BindListener()
                {
                    public void bindCompleted( LdapConnection connection, BindResponse bindResponse ) throws LdapException
                    {
                        assertNotNull( bindResponse );
                        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
                        //System.out.println( "Bound" );
                        lock.release();
                    }
                } );
                
                lock.acquire();
                lock.release();
                connection.unBind();
                
                if ( i % 100 == 0 )
                {
                    System.out.println( i );
                }
            }
            
            long t1 = System.currentTimeMillis();
            System.out.println( (t1 - t0) );
        }
        catch ( LdapException le )
        {
            le.printStackTrace();
            fail();
        }
        catch( Exception e )
        {
            e.printStackTrace();
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
