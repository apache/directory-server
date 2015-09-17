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

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.BindFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
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
        "dn: uid=superuser,ou=system",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "objectClass: top",
        "cn: superuser",
        "sn: administrator",
        "displayName: Directory Superuser",
        "uid: superuser",
        "userPassword: test",
        "",
        // Entry # 2
        "dn: uid=superuser2,ou=system",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "objectClass: top",
        "cn: superuser2",
        "sn: administrator",
        "displayName: Directory Superuser",
        "uid: superuser2",
        "userPassword: test1",
        "userPassword: test2" })
public class SimpleBindRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;


    /**
     * Create the LdapConnection
     */
    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.setTimeOut( 0L );
    }


    /**
     * Close the LdapConnection
     */
    @After
    public void shutdown() throws Exception
    {
        if ( connection != null )
        {
            connection.close();
        }
    }


    /**
     * Test a successful synchronous bind request. the server allows it.
     */
    @Test
    public void testSyncBindRequest() throws Exception
    {
        connection.bind( "uid=admin,ou=system", "secret" );

        assertTrue( connection.isAuthenticated() );

        connection.unBind();
        connection.close();
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
            bindRequest.setDn( new Dn( "uid=admin,ou=system" ) );
            bindRequest.setCredentials( "secret" );

            BindFuture bindFuture = connection.bindAsync( bindRequest );

            BindResponse bindResponse = bindFuture.get( 1000, TimeUnit.MILLISECONDS );

            assertNotNull( bindResponse );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertTrue( connection.isAuthenticated() );
        }
    }


    /**
     * Test a successful simple bind request.
     */
    @Test
    public void testSimpleBindRequest() throws Exception
    {
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setDn( new Dn( "uid=admin,ou=system" ) );
        bindRequest.setCredentials( "secret" );

        BindResponse bindResponse = connection.bind( bindRequest );

        assertNotNull( bindResponse );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a successful simple bind request when the user has 2 passwords.
     */
    @Test
    public void testSimpleBindRequest2Passwords() throws Exception
    {
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setDn( new Dn( "uid=superUser2,ou=system" ) );
        bindRequest.setCredentials( "test1" );

        BindResponse bindResponse = connection.bind( bindRequest );

        assertNotNull( bindResponse );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );

        bindRequest = new BindRequestImpl();
        bindRequest.setDn( new Dn( "uid=superUser2,ou=system" ) );
        bindRequest.setCredentials( "test2" );

        bindResponse = connection.bind( bindRequest );

        assertNotNull( bindResponse );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a successful blank (anonymous) bind request.
     */
    @Test
    public void testBlankBindRequest() throws Exception
    {
        BindRequest bindRequest = new BindRequestImpl();

        BindResponse bindResponse = connection.bind( bindRequest );

        assertNotNull( bindResponse );
        assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a failing blank (anonymous) bind request.
     */
    @Test
    public void testBlankBindRequestNotAllowed() throws Exception
    {
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );

        try
        {
            connection.bind();
            fail();
        }
        catch ( LdapOperationException le )
        {
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, le.getResultCode() );
        }

        getLdapServer().getDirectoryService().setAllowAnonymousAccess( true );

        connection.bind();
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test an Anonymous BindRequest
     */
    @Test
    public void testSimpleBindAnonymous() throws Exception
    {
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( true );

        //System.out.println( "------------------Create connection" + i + "-------------" );
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        //System.out.println( "------------------Bind" + i + "-------------" );

        // Try with no parameters
        connection.anonymousBind();

        assertTrue( connection.isAuthenticated() );

        //System.out.println( "----------------Unbind" + i + "-------------" );
        connection.unBind();
        assertFalse( connection.isConnected() );
        connection.close();

        // Try with empty strings
        connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.bind( "", "" );

        assertTrue( connection.isAuthenticated() );

        connection.unBind();
        assertFalse( connection.isConnected() );
        connection.close();

        // Try with null parameters
        connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
        connection.bind( ( String ) null, ( String ) null );

        assertTrue( connection.isAuthenticated() );
        assertTrue( connection.isConnected() );

        connection.unBind();
        assertFalse( connection.isConnected() );
        connection.close();

        connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );

        //System.out.println( "----------------Unbind done" + i + "-------------" );
        assertFalse( connection.isConnected() );
        connection.close();
        //System.out.println( "----------------Unconnected" + i + "-------------" );
    }


    /**
     * Test for DIRAPI-49 (LdapNetworkConnection.anonymousBind() uses name and credentials
     * from configuration instead of empty values).
     */
    @Test
    public void testDIRAPI47() throws Exception
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( InetAddress.getLocalHost().getHostName() );
        config.setLdapPort( getLdapServer().getPort() );
        config.setName( "uid=nonexisting,dc=example,dc=com" );

        connection = new LdapNetworkConnection( config );
        connection.anonymousBind();

        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a failing Anonymous BindRequest
     */
    @Test
    public void testSimpleBindAnonymousNotAllowed() throws Exception
    {
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );

        try
        {
            connection.anonymousBind();
            fail();
        }
        catch ( LdapOperationException le )
        {
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, le.getResultCode() );
        }

        getLdapServer().getDirectoryService().setAllowAnonymousAccess( true );

        connection.anonymousBind();
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * A bind with no name and a password is invalid
     */
    @Test
    public void testSimpleBindNoNamePassword() throws Exception
    {
        try
        {
            connection.bind( ( String ) null, "abc" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            ResultCodeEnum resultCode = lae.getResultCode();
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resultCode );
            assertFalse( connection.isAuthenticated() );
            assertTrue( connection.isConnected() );
        }
    }


    /**
     * Test an unauthenticated bind (name, no password)
     */
    @Test
    public void testSimpleBindUnauthenticated() throws Exception
    {
        try
        {
            connection.bind( "uid=admin,ou=system" );
            fail();
        }
        catch ( LdapUnwillingToPerformException lutpe )
        {
            ResultCodeEnum resultCode = lutpe.getResultCode();
            assertEquals( ResultCodeEnum.UNWILLING_TO_PERFORM, resultCode );
            assertFalse( connection.isAuthenticated() );
            assertTrue( connection.isConnected() );
        }
    }


    /**
     * Test an bind with no password
     */
    @Test(expected = LdapUnwillingToPerformException.class)
    public void testSimpleBindNoPassword() throws Exception
    {
        connection.bind( "uid=admin,ou=system", ( String ) null );
    }


    /**
     * Test a valid bind
     */
    @Test
    public void testSimpleBindValid() throws Exception
    {
        connection.bind( "uid=admin,ou=system", "secret" );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Test a bind with a valid user but a wrong password
     */
    @Test
    public void testSimpleBindValidUserWrongPassword() throws Exception
    {
        try
        {
            connection.bind( "uid=admin,ou=system", "badpassword" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            ResultCodeEnum resultCode = lae.getResultCode();
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resultCode );
            assertFalse( connection.isAuthenticated() );
            assertTrue( connection.isConnected() );
        }
    }


    /**
     * Test a bind with an invalid user
     */
    @Test
    public void testSimpleBindInvalidUser() throws Exception
    {
        try
        {
            connection.bind( "uid=wrong,ou=system", "secret" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            ResultCodeEnum resultCode = lae.getResultCode();
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resultCode );
            assertFalse( connection.isAuthenticated() );
            assertTrue( connection.isConnected() );
        }
    }


    /**
     * Test a valid bind followed by another valid bind
     */
    @Test
    public void testDoubleSimpleBindValid() throws Exception
    {
        BindRequest br1 = new BindRequestImpl();
        br1.setDn( new Dn( "uid=admin,ou=system" ) );
        br1.setCredentials( Strings.getBytesUtf8( "secret" ) );

        BindResponse response1 = connection.bind( br1 );
        assertTrue( connection.isAuthenticated() );
        int messageId1 = response1.getMessageId();

        // The messageId must have been incremented
        BindRequest br2 = new BindRequestImpl();
        br2.setDn( new Dn( "uid=admin,ou=system" ) );
        br2.setCredentials( Strings.getBytesUtf8( "secret" ) );

        BindResponse response2 = connection.bind( br2 );
        int messageId2 = response2.getMessageId();

        assertTrue( messageId2 > messageId1 );
        assertTrue( connection.isAuthenticated() );

        // Now, unbind
        connection.unBind();
        assertFalse( connection.isAuthenticated() );
        assertFalse( connection.isConnected() );

        // And Bind again. The messageId should be 1
        BindRequest br3 = new BindRequestImpl();
        br3.setDn( new Dn( "uid=admin,ou=system" ) );
        br3.setCredentials( Strings.getBytesUtf8( "secret" ) );

        BindResponse response3 = connection.bind( br3 );
        int messageId = response3.getMessageId();
        assertEquals( 1, messageId );
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
            Interceptor interceptor = new BaseInterceptor( "test" )
            {
                /**
                 * Wait 1 second before going any further
                 */
                public void bind( BindOperationContext bindContext ) throws LdapException
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

                    next( bindContext );
                }
            };

            getService().addFirst( interceptor );

            // Send another BindRequest
            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setDn( new Dn( "uid=admin,ou=system" ) );
            bindRequest.setCredentials( "secret" );

            BindFuture bindFuture = connection.bindAsync( bindRequest );

            // Wait a bit to be sure the server is processing the bind request
            Thread.sleep( 200 );

            // It will take 1 seconds to bind, let's send another bind request : it should fail
            try
            {
                connection.bind( "uid=admin,ou=system", "secret" );
                fail();
            }
            catch ( LdapUnwillingToPerformException lutpe )
            {
                assertFalse( connection.isAuthenticated() );
                assertTrue( true );
            }

            // Now get back the BindResponse
            BindResponse bindResponse = bindFuture.get( 2000, TimeUnit.MILLISECONDS );

            assertNotNull( bindResponse );
            assertEquals( ResultCodeEnum.SUCCESS, bindResponse.getLdapResult().getResultCode() );
            assertTrue( connection.isAuthenticated() );
        }
        finally
        {
            getService().remove( "test" );
        }
    }


    /**
     * Bind with a new user when the connection is establish with an anonymous authent.
     */
    @Test
    public void testBindUserWhenAnonymous() throws Exception
    {
        // Bind anonymous
        connection.bind();
        assertTrue( connection.isAuthenticated() );

        // Now bind with some credentials
        connection.bind( "uid=admin, ou=system", "secret" );

        assertTrue( connection.isAuthenticated() );

        // And back to anonymous
        connection.bind();

        assertTrue( connection.isAuthenticated() );
    }


    /**
     * Bind with a new user when the connection is establish with an anonymous authent.
     */
    @Test
    public void testBindUserWhenAlreadyBound() throws Exception
    {
        // Bind with some credentials
        connection.bind( "uid=admin, ou=system", "secret" );

        assertTrue( connection.isAuthenticated() );

        // Bind with another user
        connection.bind( "uid=superuser,ou=system", "test" );

        assertTrue( connection.isAuthenticated() );
    }


    /**
     * DIRSERVER-1548
     */
    @Test
    public void testSimpleBindInvalidFwdByValidOnSameCon() throws Exception
    {
        try
        {
            connection.bind( "uid=admin,ou=system", "wrongpwd" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertFalse( connection.isAuthenticated() );
        }

        connection.bind( "uid=admin,ou=system", "secret" );
        assertTrue( connection.isAuthenticated() );
    }


    /**
     * DIRAPI-236
     *
     * @throws Exception
     */
    @Test
    public void testUnbindDuringSearch() throws Exception
    {
        connection.bind( "uid=admin, ou=system", "secret" );

        assertTrue( connection.isAuthenticated() );

        EntryCursor cursor1 = connection.search( new Dn( "ou=system" ), "(uid=*)", SearchScope.SUBTREE, "*" );
        EntryCursor cursor2 = connection.search( new Dn( "ou=system" ), "(uid=*)", SearchScope.ONELEVEL, "*" );
        EntryCursor cursor3 = connection.search( new Dn( "ou=system" ), "(ObjectClass=*)", SearchScope.OBJECT, "*" );

        connection.unBind();

        // this call hangs forever
        assertFalse( cursor1.next() );
        assertFalse( cursor2.next() );
        assertFalse( cursor3.next() );
    }
}
