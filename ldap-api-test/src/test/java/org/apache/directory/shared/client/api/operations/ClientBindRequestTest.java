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

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.BindListener;
import org.apache.directory.shared.ldap.client.api.messages.BindRequest;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.client.api.messages.Response;
import org.apache.directory.shared.ldap.client.api.messages.future.ResponseFuture;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the BindRequest operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkRunner.class ) 
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" ), 
        @CreateTransport( protocol = "LDAPS" ) 
    })
public class ClientBindRequestTest extends AbstractLdapTestUnit
{
    private LdapConnection connection;

    
    /**
     * Create the LdapConnection
     */
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
    }

    
    /**
     * Close the LdapConnection
     */
    @After
    public void shutdown()
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

    
    /**
     * Test a successful synchronous bind request
     *
     * @throws IOException
     */
    @Test
    public void testSyncBindRequest() throws Exception
    {
        try
        {
            BindResponse bindResponse = connection.bind( "uid=admin,ou=system", "secret" );
            
            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );

            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
    }

    
    /**
     * Test a successful asynchronous bind request
     *
     * @throws IOException
     */
    @Test
    @Ignore
    public void testAsyncBindRequest() throws Exception
    {
        int i = 0;
        int nbLoop = 10;

        try
        {
            for ( ; i < nbLoop; i++)
            {
                BindRequest bindRequest = new BindRequest();
                bindRequest.setName( "uid=admin,ou=system" );
                bindRequest.setCredentials( "secret" );
                final int loop = i;

                ResponseFuture bindFuture = connection.bind( bindRequest, new BindListener()
                {
                    public void bindCompleted( LdapConnection connection, BindResponse bindResponse ) throws LdapException
                    {
                        assertNotNull( bindResponse );
                        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
                        assertEquals( 1, bindResponse.getMessageId() );
                        System.out.println( "Bound " + loop );
                    }
                } );
                
                Response bindResponse = (Response)bindFuture.get();
                bindResponse.wait();
                
                System.out.println( "Unbinding " + loop );
                connection.unBind();
            }
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
    }
    
    
    @Test
    public void testSimpleBindAnonymous() throws Exception
    {
        try
        {
            BindResponse bindResponse = connection.bind();
            
            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );

            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
    }
    
    
    @Test
    public void testSimpleBindAnonymous2() throws Exception
    {
        try
        {
            BindResponse bindResponse = connection.bind( "", "" );
            
            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );

            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
    }
    
    
    @Test
    public void testSimpleBindAnonymous3() throws Exception
    {
        try
        {
            BindResponse bindResponse = connection.bind( (String)null, (String)null );
            
            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );

            connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
    }
}
