/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.client.api.operations;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.DeleteFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.model.message.DeleteResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case for client delete operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    { "dn: cn=parent,ou=system", "objectClass: person", "cn: parent_cn", "sn: parent_sn",

    "",

    "dn: cn=child1,cn=parent,ou=system", "objectClass: person", "cn: child1_cn", "sn: child1_sn",

    "",

    "dn: cn=child2,cn=parent,ou=system", "objectClass: person", "cn: child2_cn", "sn: child2_sn",

    "",

    "dn: cn=grand_child11,cn=child1,cn=parent,ou=system", "objectClass: person", "cn: grand_child11_cn",
        "sn: grand_child11_sn",

        "",

        "dn: cn=grand_child12,cn=child1,cn=parent,ou=system", "objectClass: person", "cn: grand_child12_cn",
        "sn: grand_child12_sn" })
public class ClientDeleteRequestTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;

    private CoreSession session;


    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        Dn bindDn = new Dn( "uid=admin,ou=system" );
        connection.bind( bindDn.getName(), "secret" );

        session = ldapServer.getDirectoryService().getAdminSession();
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


    @Test
    public void testDeleteLeafNode() throws Exception
    {
        Dn dn = new Dn( "cn=grand_child12,cn=child1,cn=parent,ou=system" );

        assertTrue( session.exists( dn ) );

        DeleteResponse response = connection.delete( dn.getName() );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertFalse( session.exists( dn ) );
    }


    @Test
    public void testDeleteNonLeafFailure() throws Exception
    {
        Dn dn = new Dn( "cn=child1,cn=parent,ou=system" ); // has children
        assertTrue( session.exists( dn ) );

        DeleteResponse response = connection.delete( dn.getName() );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, response.getLdapResult().getResultCode() );

        assertTrue( session.exists( dn ) );
    }


    @Test
    @Ignore
    public void testDeleteWithCascadeControl() throws Exception
    {
        Dn dn = new Dn( "cn=parent,ou=system" );

        assertTrue( session.exists( dn ) );

        if ( connection.isControlSupported( "1.2.840.113556.1.4.805" ) )
        {
            DeleteResponse response = connection.deleteTree( dn );
            assertNotNull( response );
            assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

            assertFalse( session.exists( dn ) );
        }
    }


    /**
     * this method uses reflection to test deleteChildren method without using the
     * convenient method delete( dn, true ), cause the convenient method checks 
     * whether the server supports the CascadeControl.
     * 
     * Cause ADS supports this control, delete(dn, true) will never call the method
     * deleteChildren() (which has private scope) 
     * To test the manual deletion of the entries in the absence of this CascadeControl
     * reflection was used to invoke the private method deleteChildren().
     * 
     */
    @Test
    @Ignore
    public void testDeleteWithoutCascadeControl() throws Exception
    {
        Dn dn = new Dn( "cn=parent,ou=system" );

        assertTrue( session.exists( dn ) );

        Method deleteChildrenMethod = connection.getClass().getDeclaredMethod( "deleteRecursive", Dn.class, Map.class );
        deleteChildrenMethod.setAccessible( true );

        DeleteResponse response = ( DeleteResponse ) deleteChildrenMethod.invoke( connection, dn, null, null );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertFalse( session.exists( dn ) );
    }


    /**
     * @see #testDeleteWithoutCascadeControl()
     */
    @Test
    @Ignore
    public void testDeleteAsyncWithoutCascadeControl() throws Exception
    {
        Dn dn = new Dn( "cn=parent,ou=system" );

        assertTrue( session.exists( dn ) );

        Method deleteChildrenMethod = connection.getClass().getDeclaredMethod( "deleteRecursive", Dn.class, Map.class );
        deleteChildrenMethod.setAccessible( true );

        final AtomicInteger count = new AtomicInteger();

        try
        {
            connection.deleteTree( dn );
            fail();
        }
        catch ( LdapException le )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testDeleteAsync() throws Exception
    {
        Dn dn = new Dn( "cn=grand_child12,cn=child1,cn=parent,ou=system" );

        assertTrue( session.exists( dn ) );

        DeleteRequest deleteRequest = new DeleteRequestImpl();
        deleteRequest.setName( dn );

        DeleteFuture deleteFuture = connection.deleteAsync( deleteRequest );

        try
        {
            DeleteResponse deleteResponse = deleteFuture.get( 1000, TimeUnit.MILLISECONDS );

            assertNotNull( deleteResponse );
            assertEquals( ResultCodeEnum.SUCCESS, deleteResponse.getLdapResult().getResultCode() );
            assertTrue( connection.isAuthenticated() );
            assertFalse( session.exists( dn ) );
        }
        catch ( TimeoutException toe )
        {
            fail();
        }
    }
}
