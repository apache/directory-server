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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.ModifyFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.util.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the modify operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs({
    "dn: uid=billyd,ou=users,ou=system",
    "objectClass: person",
    "objectClass: organizationalPerson",
    "objectClass: inetOrgPerson",
    "uid: billyd",
    "userPassword: secret",
    "sn: billyd",
    "cn: billyd"
})
public class ClientModifyRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;

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
    public void testModify() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modRequest = new ModifyRequestImpl();
        modRequest.setName( dn );
        modRequest.replace( SchemaConstants.SN_AT, expected );

        connection.modify( modRequest );

        Entry entry = session.lookup( dn );

        String actual = entry.get( SchemaConstants.SN_AT ).getString();

        assertEquals( expected, actual );
    }


    @Test
    public void testModifyWithEntry() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        Entry entry = new DefaultEntry( dn );

        String expectedSn = String.valueOf( System.currentTimeMillis() );
        String expectedCn = String.valueOf( System.currentTimeMillis() );

        entry.add( SchemaConstants.SN_AT, expectedSn );

        entry.add( SchemaConstants.CN_AT, expectedCn );

        connection.modify( entry, ModificationOperation.REPLACE_ATTRIBUTE );

        Entry lookupEntry = session.lookup( dn );

        String actualSn = lookupEntry.get( SchemaConstants.SN_AT ).getString();
        assertEquals( expectedSn, actualSn );

        String actualCn = lookupEntry.get( SchemaConstants.CN_AT ).getString();
        assertEquals( expectedCn, actualCn );
    }


    @Test
    public void testModifyReplaceRemove() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        Entry entry = new DefaultEntry( dn );

        entry.add( "givenName", "test" );

        connection.modify( entry, ModificationOperation.REPLACE_ATTRIBUTE );

        Entry lookupEntry = session.lookup( dn );

        String gn = lookupEntry.get( "givenName" ).getString();
        assertEquals( "test", gn );

        // Now, replace the givenName
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( dn );
        modifyRequest.replace( "givenName" );
        connection.modify( modifyRequest );

        lookupEntry = session.lookup( dn );
        EntryAttribute giveName = lookupEntry.get( "givenName" );
        assertNull( giveName );
    }


    @Test
    public void modifyAsync() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        String expected = String.valueOf( System.currentTimeMillis() );
        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( dn );
        modifyRequest.replace( SchemaConstants.SN_AT, expected );

        assertTrue( session.exists( dn ) );

        ModifyFuture modifyFuture = connection.modifyAsync( modifyRequest );

        try
        {
            ModifyResponse response = modifyFuture.get( 1000, TimeUnit.MILLISECONDS );

            assertNotNull( response );

            Entry entry = session.lookup( dn );

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


    /**
     * ApacheDS doesn't allow modifying entryUUID and entryCSN AT
     */
    @Test
    public void testModifyEntryUUIDAndEntryCSN() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( dn );
        modifyRequest.replace( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );

        ModifyResponse modResp = connection.modify( modifyRequest );
        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, modResp.getLdapResult().getResultCode() );

        modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( dn );
        modifyRequest.replace( SchemaConstants.ENTRY_CSN_AT, new CsnFactory( 0 ).newInstance().toString() );

        // admin can modify the entryCsn
        modResp = connection.modify( modifyRequest );
        assertEquals( ResultCodeEnum.SUCCESS, modResp.getLdapResult().getResultCode() );

        connection.close();
        
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );

        Dn bindDn = new Dn( "uid=billyd,ou=users,ou=system" );
        connection.bind( bindDn.getName(), "secret" );
        
        // non-admin user cannot modify entryCSN
        modResp = connection.modify( modifyRequest );
        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, modResp.getLdapResult().getResultCode() );
    }


    /**
     * ApacheDS allows modifying the modifiersName and modifyTimestamp operational AT
     */
    @Test
    public void testModifyModifierNameAndModifyTimestamp() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        String modifierName = "uid=x,ou=system";
        String modifiedTime = DateUtils.getGeneralizedTime();

        ModifyRequest modifyRequest = new ModifyRequestImpl();
        modifyRequest.setName( dn );
        modifyRequest.replace( SchemaConstants.MODIFIERS_NAME_AT, modifierName );
        modifyRequest.replace( SchemaConstants.MODIFY_TIMESTAMP_AT, modifiedTime );

        ModifyResponse modResp = connection.modify( modifyRequest );
        assertEquals( ResultCodeEnum.SUCCESS, modResp.getLdapResult().getResultCode() );

        Entry loadedEntry = connection.lookup( dn.getName(), "+" );

        assertEquals( modifierName, loadedEntry.get( SchemaConstants.MODIFIERS_NAME_AT ).getString() );
        assertEquals( modifiedTime, loadedEntry.get( SchemaConstants.MODIFY_TIMESTAMP_AT ).getString() );
    }

}
