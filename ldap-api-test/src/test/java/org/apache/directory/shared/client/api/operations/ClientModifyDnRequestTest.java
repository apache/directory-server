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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Semaphore;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.ModifyDnListener;
import org.apache.directory.shared.ldap.client.api.messages.ModifyDnRequest;
import org.apache.directory.shared.ldap.client.api.messages.ModifyDnResponse;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for modifyDn operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith(SiRunner.class)
@CleanupLevel(Level.METHOD)
@ApplyLdifs( {
    "dn: cn=modDn,ou=system\n" +
    "objectClass: person\n" +
    "cn: modDn\n" +
    "sn: snModDn\n" 
})
public class ClientModifyDnRequestTest
{
    public static LdapServer ldapServer;
    
    private LdapConnection connection;
    
    private CoreSession session;
    
    private String dn = "cn=modDn,ou=system";

    @Before
    public void setup() throws Exception
    {
        connection = new LdapConnection( "localhost", ldapServer.getPort() );

        LdapDN bindDn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( bindDn.getUpName(), "secret" );
        
        session = ldapServer.getDirectoryService().getAdminSession();
    }
    
    
    @Test
    public void testRename() throws Exception
    {
        ModifyDnResponse resp = connection.rename( dn, "cn=modifyDnWithString" );
        assertNotNull( resp );
        assertFalse( session.exists( new LdapDN( dn ) ) );
        assertTrue( session.exists( new LdapDN( "cn=modifyDnWithString,ou=system" ) ) );
    }
    
    
    @Test
    public void testRenameWithoutDeleteOldRdn() throws Exception
    {
        ModifyDnResponse resp = connection.rename( dn, "cn=modifyDnWithString", false );
        assertNotNull( resp );
        
        LdapDN oldDn = new LdapDN( dn );
        assertFalse( session.exists( oldDn ) );
        
        Entry entry = session.lookup( new LdapDN( "cn=modifyDnWithString,ou=system" ) );
        assertNotNull( entry );
        
        Rdn oldRdn = oldDn.getRdn();
        assertTrue( entry.contains( oldRdn.getUpType(), ( String ) oldRdn.getValue() ) );
    }
    
    
    @Test
    public void testMove() throws Exception
    {
        ModifyDnResponse resp = connection.move( dn, "ou=users,ou=system" );
        assertNotNull( resp );
        
        LdapDN oldDn = new LdapDN( dn );
        assertFalse( session.exists( oldDn ) );
        
        assertTrue( session.exists( new LdapDN( "cn=modDn,ou=users,ou=system" ) ) );
        
        System.out.println( session.lookup( new LdapDN( "cn=modDn,ou=users,ou=system" ) ) );
    }
    
    
    @Test
    public void testModifyDnAsync() throws Exception
    {
        ModifyDnRequest modDnReq = new ModifyDnRequest();
        modDnReq.setEntryDn( new LdapDN( dn ) );
        modDnReq.setNewRdn( new Rdn( "cn=modifyDnWithString" ) );
        modDnReq.setDeleteOldRdn( true );

        final Semaphore lock = new Semaphore(1);
        lock.acquire();
        ModifyDnResponse resp = connection.modifyDn( modDnReq, new ModifyDnListener()
        {
            public void modifyDnCompleted( LdapConnection connection, ModifyDnResponse response ) throws LdapException
            {
                assertNotNull( response );
                assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
                lock.release();
            }
        });

        lock.acquire();
        assertNull( resp );
        assertFalse( session.exists( new LdapDN( dn ) ) );
        assertTrue( session.exists( new LdapDN( "cn=modifyDnWithString,ou=system" ) ) );
    }
}
