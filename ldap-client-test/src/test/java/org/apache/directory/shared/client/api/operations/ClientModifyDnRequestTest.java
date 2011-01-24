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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.ModifyDnFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.model.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcase for modifyDn operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@ApplyLdifs(
    { "dn: cn=modDn,ou=system", "objectClass: person", "cn: modDn", "sn: snModDn" })
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class ClientModifyDnRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;

    private CoreSession session;

    private String dn = "cn=modDn,ou=system";


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
    public void testRename() throws Exception
    {
        ModifyDnResponse resp = connection.rename( dn, "cn=modifyDnWithString" );
        assertNotNull( resp );
        assertFalse( session.exists( new Dn( dn ) ) );
        assertTrue( session.exists( new Dn( "cn=modifyDnWithString,ou=system" ) ) );
    }


    @Test
    public void testRenameWithoutDeleteOldRdn() throws Exception
    {
        ModifyDnResponse resp = connection.rename( dn, "cn=modifyDnWithString", false );
        assertNotNull( resp );

        Dn oldDn = new Dn( dn );
        assertFalse( session.exists( oldDn ) );

        Entry entry = session.lookup( new Dn( "cn=modifyDnWithString,ou=system" ) );
        assertNotNull( entry );

        Rdn oldRdn = oldDn.getRdn();
        assertTrue( entry.contains( oldRdn.getUpType(), oldRdn.getNormValue() ) );
    }


    @Test
    public void testMove() throws Exception
    {
        ModifyDnResponse resp = connection.move( dn, "ou=users,ou=system" );
        assertNotNull( resp );

        Dn oldDn = new Dn( dn );
        assertFalse( session.exists( oldDn ) );

        assertTrue( session.exists( new Dn( "cn=modDn,ou=users,ou=system" ) ) );
    }


    @Test
    public void testModifyDnAsync() throws Exception
    {
        Dn oldDn = new Dn( dn );
        Dn newDn = new Dn( "cn=modifyDnWithString,ou=system" );

        ModifyDnRequest modDnReq = new ModifyDnRequestImpl();
        modDnReq.setName( oldDn );
        modDnReq.setNewRdn( new Rdn( "cn=modifyDnWithString" ) );
        modDnReq.setDeleteOldRdn( true );

        ModifyDnFuture modifyDnFuture = connection.modifyDnAsync( modDnReq );

        try
        {
            ModifyDnResponse response = modifyDnFuture.get( 1000, TimeUnit.MILLISECONDS );

            assertNotNull( response );

            assertTrue( connection.isAuthenticated() );
            assertFalse( session.exists( oldDn ) );
            assertTrue( session.exists( newDn ) );
        }
        catch ( TimeoutException toe )
        {
            fail();
        }

        assertTrue( session.exists( new Dn( "cn=modifyDnWithString,ou=system" ) ) );
    }
}
