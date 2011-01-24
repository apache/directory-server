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
package org.apache.directory.shared.client.api.operations.bind;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.BindFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the Simple BindRequest operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=superuser,ou=system", "objectClass: person", "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson", "objectClass: top", "cn: superuser", "sn: administrator",
        "displayName: Directory Superuser", "uid: superuser", "userPassword: test" })
public class SimpleBindRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;


    /**
     * Create the LdapConnection
     */
    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
    }


    /**
     * Close the LdapConnection
     */
    @After
    public void shutdown()
    {
        try
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
        catch ( Exception ioe )
        {
            fail();
        }
    }


    /**
     * Test a successful synchronous bind request. the server allows it.
     */
    @Test
    public void testSyncBindRequest() throws Exception
    {
        BindResponse bindResponse = connection.bind( "uid=admin,ou=system", "secret" );

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 1, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a successful asynchronous bind request, 10 times.
     */
    @Test
    public void testAsyncBindRequest() throws Exception
    {
        int i = 0;
        int nbLoop = 10;

        for ( ; i < nbLoop; i++ )
        {
            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setName( new Dn( "uid=admin,ou=system" ) );
            bindRequest.setCredentials( "secret" );

            BindFuture bindFuture = connection.bindAsync( bindRequest );

            try
            {
                BindResponse bindResponse = bindFuture.get( 1000, TimeUnit.MILLISECONDS );

                assertNotNull( bindResponse );
                assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
                assertTrue( connection.isAuthenticated() );
            }
            catch ( TimeoutException toe )
            {
                fail();
            }
        }
    }


    /**
     * Test an Anonymous BindRequest
     */
    @Test
    public void testSimpleBindAnonymous() throws Exception
    {
        for ( int i = 0; i < 5; i++ )
        {
            //System.out.println( "------------------Create connection" + i + "-------------" );
            LdapConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
            //System.out.println( "------------------Bind" + i + "-------------" );

            // Try with no parameters
            BindResponse bindResponse = connection.bind();

            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );
            assertTrue( connection.isAuthenticated() );

            //System.out.println( "----------------Unbind" + i + "-------------" );
            connection.unBind();
            assertFalse( connection.isConnected() );
            connection.close();

            // Try with empty strings
            connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
            bindResponse = connection.bind( "", "" );

            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );
            assertTrue( connection.isAuthenticated() );

            connection.unBind();
            assertFalse( connection.isConnected() );
            connection.close();

            // Try with null parameters
            connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
            bindResponse = connection.bind( ( String ) null, ( String ) null );

            assertNotNull( bindResponse );
            assertNotNull( bindResponse.getLdapResult() );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertEquals( 1, bindResponse.getMessageId() );
            assertTrue( connection.isAuthenticated() );
            assertTrue( connection.isConnected() );

            connection.unBind();
            assertFalse( connection.isConnected() );
            connection.close();

            connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

            //System.out.println( "----------------Unbind done" + i + "-------------" );
            assertFalse( connection.isConnected() );
            connection.close();
            //System.out.println( "----------------Unconnected" + i + "-------------" );

        }
    }


    /**
     * A bind with no name and a password is invalid
     */
    @Test
    public void testSimpleBindNoNamePassword() throws Exception
    {
        BindResponse response = connection.bind( ( String ) null, "abc" );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertFalse( connection.isAuthenticated() );
        assertTrue( connection.isConnected() );
    }


    /**
     * Test an unauthenticated bind (name, no password)
     */
    @Test
    public void testSimpleBindUnauthenticated() throws Exception
    {
        BindResponse response = connection.bind( "uid=admin,ou=system", ( String ) null );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertFalse( connection.isAuthenticated() );
        assertTrue( connection.isConnected() );
    }


    /**
     * Test a valid bind
     */
    @Test
    public void testSimpleBindValid() throws Exception
    {
        BindResponse response = connection.bind( "uid=admin,ou=system", "secret" );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.SUCCESS, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a bind with a valid user but a wrong password
     */
    @Test
    public void testSimpleBindValidUserWrongPassword() throws Exception
    {
        BindResponse response = connection.bind( "uid=admin,ou=system", "badpassword" );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertFalse( connection.isAuthenticated() );
        assertTrue( connection.isConnected() );
    }


    /**
     * Test a bind with an invalid user
     */
    @Test
    public void testSimpleBindInvalidUser() throws Exception
    {
        BindResponse response = connection.bind( "uid=wrong,ou=system", "secret" );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertFalse( connection.isAuthenticated() );
        assertTrue( connection.isConnected() );
    }


    /**
     * Test a valid bind followed by another valid bind
     */
    @Test
    public void testDoubleSimpleBindValid() throws Exception
    {
        BindResponse response1 = connection.bind( "uid=admin,ou=system", "secret" );
        LdapResult ldapResult1 = response1.getLdapResult();
        assertEquals( ResultCodeEnum.SUCCESS, ldapResult1.getResultCode() );
        assertEquals( 1, response1.getMessageId() );
        assertTrue( connection.isAuthenticated() );

        // The messageId must have been incremented
        BindResponse response2 = connection.bind( "uid=admin,ou=system", "secret" );
        LdapResult ldapResult2 = response2.getLdapResult();
        assertEquals( ResultCodeEnum.SUCCESS, ldapResult2.getResultCode() );
        assertEquals( 2, response2.getMessageId() );
        assertTrue( connection.isAuthenticated() );

        // Now, unbind
        connection.unBind();
        assertFalse( connection.isAuthenticated() );
        assertFalse( connection.isConnected() );

        // And Bind again. The messageId should be 1 
        BindResponse response3 = connection.bind( "uid=admin,ou=system", "secret" );
        LdapResult ldapResult3 = response3.getLdapResult();
        assertEquals( ResultCodeEnum.SUCCESS, ldapResult3.getResultCode() );
        assertEquals( 1, response3.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test that we can't send another request until the BindResponse arrives
     */
    @Test
    public void testRequestWhileBinding() throws Exception
    {
        try
        {
            // Inject the interceptor that waits 1 second when binding 
            // in order to be able to send a request before we get the response
            service.getInterceptorChain().addFirst( new BaseInterceptor()
            {
                /**
                 * Wait 1 second before going any further
                 */
                public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
                {
                    // Wait 1 second
                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch ( InterruptedException ie )
                    {
                        // Ok, get out
                    }

                    next.bind( bindContext );
                }
            } );

            // Send another BindRequest
            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setName( new Dn( "uid=admin,ou=system" ) );
            bindRequest.setCredentials( "secret" );

            BindFuture bindFuture = connection.bindAsync( bindRequest );

            // Wait a bit to be sure the server is processing the bind request
            Thread.sleep( 200 );

            // It will take 1 seconds to bind, let's send another bind request : it should fail
            BindResponse response = connection.bind( "uid=admin,ou=system", "secret" );

            assertFalse( connection.isAuthenticated() );
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, response.getLdapResult().getResultCode() );

            // Now get back the BindResponse
            try
            {
                BindResponse bindResponse = bindFuture.get( 2000, TimeUnit.MILLISECONDS );

                assertNotNull( bindResponse );
                assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
                assertTrue( connection.isAuthenticated() );
            }
            catch ( TimeoutException toe )
            {
                fail();
            }
        }
        finally
        {
            service.getInterceptorChain().remove( this.getClass().getName() + "$1" );
        }
    }


    /**
     * Bind with a new user when the connection is establish with an anonymous authent.
     */
    @Test
    public void testBindUserWhenAnonymous() throws Exception
    {
        // Bind anonymous
        BindResponse bindResponse = connection.bind();

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 1, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );

        // Now bind with some credentials
        bindResponse = connection.bind( "uid=admin, ou=system", "secret" );

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 2, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );

        //And back to anonymous
        bindResponse = connection.bind();

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 3, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Bind with a new user when the connection is establish with an anonymous authent.
     */
    @Test
    public void testBindUserWhenAlreadyBound() throws Exception
    {
        // Bind with some credentials
        BindResponse bindResponse = connection.bind( "uid=admin, ou=system", "secret" );

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 1, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );

        // Bind with another user
        bindResponse = connection.bind( "uid=superuser,ou=system", "test" );

        assertNotNull( bindResponse );
        assertNotNull( bindResponse.getLdapResult() );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertEquals( 2, bindResponse.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }
    
    
    /**
     * DIRSERVER-1548
     */
    @Test
    public void testSimpleBindInvalidFwdByValidOnSameCon() throws Exception
    {
        connection.setTimeOut( Integer.MAX_VALUE );
        BindResponse response = connection.bind( "uid=admin,ou=system", "wrongpwd" );
        LdapResult ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, ldapResult.getResultCode() );
        assertEquals( 1, response.getMessageId() );
        assertFalse( connection.isAuthenticated() );
        
        response = connection.bind( "uid=admin,ou=system", "secret" );
        ldapResult = response.getLdapResult();
        assertEquals( ResultCodeEnum.SUCCESS, ldapResult.getResultCode() );
        assertEquals( 2, response.getMessageId() );
        assertTrue( connection.isAuthenticated() );
    }

}
