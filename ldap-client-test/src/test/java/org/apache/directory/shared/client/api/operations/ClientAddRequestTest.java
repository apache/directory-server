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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.AddFuture;
import org.apache.directory.ldap.client.api.message.AddRequest;
import org.apache.directory.ldap.client.api.message.AddResponse;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the add operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" ), 
        @CreateTransport( protocol = "LDAPS" ) 
    })
public class ClientAddRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;
    private CoreSession session;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        DN bindDn = new DN( "uid=admin,ou=system" );
        connection.bind( bindDn.getName(), "secret" );
        
        session = ldapServer.getDirectoryService().getSession();
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
        catch( Exception ioe )
        {
            fail();
        }
    }
    
    
    @Test
    public void testAdd() throws Exception
    {
        DN dn = new DN( "cn=testadd,ou=system" );
        Entry entry = new DefaultEntry( dn ); 
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "testadd_cn" );
        entry.add( SchemaConstants.SN_AT, "testadd_sn" );
        
        assertFalse( session.exists( dn ) );
        
        AddResponse response = connection.add( entry );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        assertTrue( session.exists( dn ) );
    }

    
    @Test
    public void testAddAsync() throws Exception
    {
        DN dn = new DN( "cn=testAsyncAdd,ou=system" );
        Entry entry = new DefaultEntry( dn ); 
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "testAsyncAdd_cn" );
        entry.add( SchemaConstants.SN_AT, "testAsyncAdd_sn" );
        
        assertFalse( session.exists( dn ) );

        AddFuture addFuture = connection.addAsync( new AddRequest( entry ));

        try
        {
            AddResponse addResponse = addFuture.get( 1000, TimeUnit.MILLISECONDS );
            
            assertNotNull( addResponse );
            assertEquals( ResultCodeEnum.SUCCESS, addResponse.getLdapResult().getResultCode() );
            assertTrue( connection.isAuthenticated() );
            assertTrue( session.exists( dn ) );
        }
        catch ( TimeoutException toe )
        {
            fail();
        }
    }
}
