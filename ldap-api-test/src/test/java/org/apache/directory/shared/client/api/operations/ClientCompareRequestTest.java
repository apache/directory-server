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

import java.util.concurrent.Semaphore;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.CompareListener;
import org.apache.directory.shared.ldap.client.api.messages.CompareRequest;
import org.apache.directory.shared.ldap.client.api.messages.CompareResponse;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the compare operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.CLASS)
public class ClientCompareRequestTest
{
    /** The server instance */
    public static LdapServer ldapServer;

    private LdapConnection connection;
    
    private CoreSession session;
    
    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );
        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getUpName(), "secret" );
        
        session = ldapServer.getDirectoryService().getSession();
    }
    
    
    @Test
    public void testCompare() throws Exception
    {
        LdapDN dn = new LdapDN( "uid=admin,ou=system" );
        
        CompareResponse response = connection.compare( dn, SchemaConstants.UID_AT, "admin" );
        assertNotNull( response );
        assertTrue( response.isTrue() );
        
        response = connection.compare( dn.getUpName(), SchemaConstants.USER_PASSWORD_AT, "secret".getBytes() );
        assertNotNull( response );
        assertTrue( response.isTrue() );
    }

    
    @Test
    public void testCompareAsync() throws Exception
    {
        LdapDN dn = new LdapDN( "uid=admin,ou=system" );

        final Semaphore lock = new Semaphore( 1 );
        lock.acquire();
        CompareRequest compareRequest = new CompareRequest();
        compareRequest.setEntryDn( dn );
        compareRequest.setAttrName( SchemaConstants.UID_AT );
        compareRequest.setValue( "admin" );
        
        connection.compare( compareRequest, new CompareListener()
        {
            
            public void attributeCompared( LdapConnection connection, CompareResponse response ) throws LdapException
            {
                assertNotNull( response );
                assertTrue( response.isTrue() );
                lock.release();
            }
        });

        lock.acquire();
        assertTrue( session.exists( dn ) );
    }
}
