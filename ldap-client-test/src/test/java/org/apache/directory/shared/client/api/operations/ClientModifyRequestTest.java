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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.ModifyFuture;
import org.apache.directory.ldap.client.api.message.ModifyRequest;
import org.apache.directory.ldap.client.api.message.ModifyResponse;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the modify operation
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
public class ClientModifyRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;
    
    private CoreSession session;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        DN bindDn = new DN( "uid=admin,ou=system" );
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
        catch( Exception ioe )
        {
            fail();
        }
    }

    
    @Test
    public void testModify() throws Exception
    {
        DN dn = new DN( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequest( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );

        connection.modify( modRequest );

        ServerEntry entry = session.lookup( dn );

        String actual = entry.get( SchemaConstants.SN_AT ).getString();

        assertEquals( expected, actual );
    }


    @Test
    public void testModifyWithEntry() throws Exception
    {
        DN dn = new DN( "uid=admin,ou=system" );
        
        Entry entry = new DefaultClientEntry( dn );
        
        String expectedSn = String.valueOf( System.currentTimeMillis() );
        String expectedCn = String.valueOf( System.currentTimeMillis() );
        
        entry.add( SchemaConstants.SN_AT, expectedSn );
        
        entry.add( SchemaConstants.CN_AT, expectedCn );
        
        connection.modify( entry, ModificationOperation.REPLACE_ATTRIBUTE );
        
        ServerEntry lookupEntry = session.lookup( dn );

        String actualSn = lookupEntry.get( SchemaConstants.SN_AT ).getString();
        assertEquals( expectedSn, actualSn );
        
        String actualCn = lookupEntry.get( SchemaConstants.CN_AT ).getString();
        assertEquals( expectedCn, actualCn );
    }
    
    
    @Test
    public void modifyAsync() throws Exception
    {
        DN dn = new DN( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequest( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );
        
        assertTrue( session.exists( dn ) );
        
        ModifyFuture modifyFuture = connection.modifyAsync( modRequest );
        
        try
        {
            ModifyResponse response = modifyFuture.get( 1000, TimeUnit.MILLISECONDS );
            
            assertNotNull( response );

            ServerEntry entry = session.lookup( dn );

            String actual = entry.get( SchemaConstants.SN_AT ).getString();

            assertEquals( expected, actual );

            assertTrue( connection.isAuthenticated() );
            assertTrue( session.exists( dn ) );
        }
        catch ( TimeoutException toe )
        {
            fail();
        }
    }
}
