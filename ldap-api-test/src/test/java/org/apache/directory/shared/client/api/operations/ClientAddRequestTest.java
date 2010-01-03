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

import java.util.concurrent.Semaphore;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.AddListener;
import org.apache.directory.shared.ldap.client.api.messages.AddRequest;
import org.apache.directory.shared.ldap.client.api.messages.AddResponse;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the add operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.CLASS)
public class ClientAddRequestTest
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
        connection.bind( bindDn.getName(), "secret" );
        
        session = ldapServer.getDirectoryService().getSession();
    }
    
    
    @Test
    public void testAdd() throws Exception
    {
        LdapDN dn = new LdapDN( "cn=testadd,ou=system" );
        Entry entry = new DefaultClientEntry( dn ); 
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
        LdapDN dn = new LdapDN( "cn=testAsyncAdd,ou=system" );
        Entry entry = new DefaultClientEntry( dn ); 
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "testAsyncAdd_cn" );
        entry.add( SchemaConstants.SN_AT, "testAsyncAdd_sn" );
        
        assertFalse( session.exists( dn ) );

        final Semaphore lock = new Semaphore( 1 );
        lock.acquire();
        
        connection.add( new AddRequest( entry ), new AddListener()
        {
            public void entryAdded( LdapConnection connection, AddResponse response ) throws LdapException
            {
                assertNotNull( response );
                assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
                lock.release();
            }
        });

        lock.acquire();
        assertTrue( session.exists( dn ) );
    }
}
