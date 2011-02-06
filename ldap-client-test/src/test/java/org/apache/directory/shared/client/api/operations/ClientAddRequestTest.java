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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.AddFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.AddRequestImpl;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the add operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class ClientAddRequestTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;
    private CoreSession session;


    @Before
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( ldapServer );
        session = ldapServer.getDirectoryService().getAdminSession();
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, ldapServer );
    }


    @Test
    public void testAdd() throws Exception
    {
        Dn dn = new Dn( "cn=testadd,ou=system" );
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
        Dn dn = new Dn( "cn=testAsyncAdd,ou=system" );
        Entry entry = new DefaultEntry( dn );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "testAsyncAdd_cn" );
        entry.add( SchemaConstants.SN_AT, "testAsyncAdd_sn" );

        assertFalse( session.exists( dn ) );
        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );

        AddFuture addFuture = connection.addAsync( addRequest );

        AddResponse addResponse = addFuture.get( 1000, TimeUnit.MILLISECONDS );

        assertNotNull( addResponse );
        assertEquals( ResultCodeEnum.SUCCESS, addResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
        assertTrue( session.exists( dn ) );
    }


    @ApplyLdifs(
        { "dn: cn=kayyagari,ou=system", "objectClass: person", "objectClass: top", "cn: kayyagari",
            "description: dbugger", "sn: dbugger", "userPassword: secret" })
    @Test
    /**
     * tests adding entryUUID, entryCSN, creatorsName and createTimestamp attributes
     */
    public void testAddSystemOperationalAttributes() throws Exception
    {
        //test as admin first
        Dn dn = new Dn( "cn=x,ou=system" );
        String uuid = UUID.randomUUID().toString();
        String csn = new CsnFactory( 0 ).newInstance().toString();
        String creator = dn.getName();
        String createdTime = DateUtils.getGeneralizedTime();

        Entry entry = new DefaultEntry( dn );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "x" );
        entry.add( SchemaConstants.SN_AT, "x" );
        entry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
        entry.add( SchemaConstants.ENTRY_CSN_AT, csn );
        entry.add( SchemaConstants.CREATORS_NAME_AT, creator );
        entry.add( SchemaConstants.CREATE_TIMESTAMP_AT, createdTime );

        connection.add( entry );

        Entry loadedEntry = connection.lookup( dn.getName(), "+" );

        // successful for admin
        assertEquals( uuid, loadedEntry.get( SchemaConstants.ENTRY_UUID_AT ).getString() );
        assertEquals( csn, loadedEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
        assertEquals( creator, loadedEntry.get( SchemaConstants.CREATORS_NAME_AT ).getString() );
        assertEquals( createdTime, loadedEntry.get( SchemaConstants.CREATE_TIMESTAMP_AT ).getString() );

        connection.delete( dn );
        connection.unBind();

        // connect as non admin user and try to add entry with uuid and csn
        BindResponse bindResp = connection.bind( "cn=kayyagari,ou=system", "secret" );
        assertEquals( ResultCodeEnum.SUCCESS, bindResp.getLdapResult().getResultCode() );

        AddResponse resp = connection.add( entry );
        assertEquals( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, resp.getLdapResult().getResultCode() );
    }

}
