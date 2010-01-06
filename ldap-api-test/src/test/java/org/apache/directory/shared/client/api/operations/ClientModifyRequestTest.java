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
import static org.junit.Assert.assertNull;

import java.util.concurrent.Semaphore;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.ModifyListener;
import org.apache.directory.shared.ldap.client.api.messages.ModifyRequest;
import org.apache.directory.shared.ldap.client.api.messages.ModifyResponse;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
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
    private LdapConnection connection;
    
    private CoreSession session;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );

        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getName(), "secret" );
        
        session = ldapServer.getDirectoryService().getAdminSession();
    }

    
    @Test
    public void testModify() throws Exception
    {
        LdapDN dn = new LdapDN( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequest( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );

        connection.modify( modRequest, null );

        ServerEntry entry = session.lookup( dn );

        String actual = entry.get( SchemaConstants.SN_AT ).getString();

        assertEquals( expected, actual );
    }


    @Test
    public void testModifyWithEntry() throws Exception
    {
        LdapDN dn = new LdapDN( "uid=admin,ou=system" );
        
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
        LdapDN dn = new LdapDN( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequest( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );

        final Semaphore lock = new Semaphore(1);
        lock.acquire();

        ModifyResponse response = connection.modify( modRequest, new ModifyListener()
        {
            public void modifyCompleted( LdapConnection connection, ModifyResponse response ) throws LdapException
            {
                assertNotNull( response );
                assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
                lock.release();
            }
        });

        lock.acquire();
        assertNull( response );

        ServerEntry entry = session.lookup( dn );

        String actual = entry.get( SchemaConstants.SN_AT ).getString();

        assertEquals( expected, actual );
    }
}
