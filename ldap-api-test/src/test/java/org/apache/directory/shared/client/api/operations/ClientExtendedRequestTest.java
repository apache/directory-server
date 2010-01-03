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

import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.ldap.StartTlsRequest;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.ExtendedListener;
import org.apache.directory.shared.ldap.client.api.messages.ExtendedRequest;
import org.apache.directory.shared.ldap.client.api.messages.ExtendedResponse;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the extended operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.CLASS)
public class ClientExtendedRequestTest
{
    /** The server instance */
    public static LdapServer ldapServer;

    private LdapConnection connection;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getName(), "secret" );
    }
    
    
    @Test
    public void testExtended() throws Exception
    {
        ExtendedResponse response = connection.extended( StartTlsRequest.OID );
        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
    }

    
    @Test
    public void testExtendedAsync() throws Exception
    {
        ExtendedRequest extendedRequest = new ExtendedRequest( StartTlsRequest.OID );
        
        final AtomicBoolean done = new AtomicBoolean( false );
        
        ExtendedListener listener = new ExtendedListener()
        {
            public void extendedOperationCompleted( LdapConnection connection, ExtendedResponse response ) throws LdapException
            {
                assertNotNull( response );
                assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
                done.set( true );
            }
        };

        ExtendedResponse response = connection.extended( extendedRequest, listener );
        assertNull( response );
        
        while( !done.get() )
        {
            Thread.sleep( 1000 );
        }
    }
}
