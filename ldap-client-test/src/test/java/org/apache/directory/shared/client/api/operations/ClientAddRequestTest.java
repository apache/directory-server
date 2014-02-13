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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.AddFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
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
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
        session = getLdapServer().getDirectoryService().getAdminSession();
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
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

        connection.add( entry );

        assertTrue( session.exists( dn ) );
        
        EntryCursor cursor = connection.search( entry.getDn(), "(objectClass=*)", SearchScope.OBJECT, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        assertTrue( cursor.next() );
        entry = cursor.get();
        
        cursor = connection.search( "ou=system", "(objectClass=*)", SearchScope.OBJECT, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        assertTrue( cursor.next() );
        Entry contextEntry = cursor.get();
        
        String expectedCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        String contextCsn = contextEntry.get( SchemaConstants.CONTEXT_CSN_AT ).getString();
        assertEquals( expectedCsn, contextCsn );
    }


    @Test
    public void testAddLdif() throws Exception
    {
        assertFalse( session.exists( "cn=testadd,ou=system" ) );

        connection.add(
            new DefaultEntry(
                "cn=testadd,ou=system",
                "ObjectClass : top",
                "ObjectClass : person",
                "cn: testadd_sn",
                "sn: testadd_sn"
            ) );

        assertTrue( session.exists( "cn=testadd,ou=system" ) );
    }


    @Test
    public void testAddWithControl() throws Exception
    {
        assertFalse( session.exists( "cn=testadd,ou=system" ) );

        Entry entry = new DefaultEntry(
            "cn=testadd,ou=system",
            "ObjectClass : top",
            "ObjectClass : person",
            "cn: testadd_sn",
            "sn: testadd_sn"
            );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );
        addRequest.addControl( new ManageDsaITImpl() );

        AddResponse response = connection.add( addRequest );

        assertNotNull( response );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        assertTrue( session.exists( "cn=testadd,ou=system" ) );
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


    @Test
    public void testAddAsyncLdif() throws Exception
    {
        Entry entry = new DefaultEntry(
            "cn=testAsyncAdd,ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: testAsyncAdd_cn",
            "sn: testAsyncAdd_sn" );

        assertFalse( session.exists( "cn=testAsyncAdd,ou=system" ) );
        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );

        AddFuture addFuture = connection.addAsync( addRequest );

        AddResponse addResponse = addFuture.get( 1000, TimeUnit.MILLISECONDS );

        assertNotNull( addResponse );
        assertEquals( ResultCodeEnum.SUCCESS, addResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
        assertTrue( session.exists( "cn=testAsyncAdd,ou=system" ) );
    }


    @ApplyLdifs(
        {
            "dn: cn=kayyagari,ou=system",
            "objectClass: person",
            "objectClass: top",
            "cn: kayyagari",
            "description: dbugger",
            "sn: dbugger",
            "userPassword: secret" })
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
        connection.bind( "cn=kayyagari,ou=system", "secret" );
        assertTrue( connection.isAuthenticated() );

        try
        {
            connection.add( entry );
            fail();
        }
        catch ( LdapNoPermissionException lnpe )
        {
            assertTrue( true );
        }
    }


    @Test
    /**
     * tests adding en entry with escaped chars in the RDN
     */
    public void testAddEntryWithRdnContainingEscapedChars() throws Exception
    {
        //test as admin first
        Dn dn = new Dn( "cn=a\\+B,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "sn: x" );

        connection.add( entry );

        Entry loadedEntry = connection.lookup( dn.getName(), "*" );
        assertNotNull( loadedEntry );
        assertTrue( loadedEntry.containsAttribute( "cn" ) );

        String cn = loadedEntry.get( "cn" ).get().getString();

        assertEquals( "a+B", cn );
    }


    @Test
    /**
     * tests adding en entry with escaped chars in the RDN
     */
    public void testAddEntryWithRdnContainingEscapedCharsExistingnEntry() throws Exception
    {
        //test as admin first
        Dn dn = new Dn( "cn=a\\+B,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: a+b",
            "sn: x" );

        connection.add( entry );

        Entry loadedEntry = connection.lookup( dn.getName(), "*" );
        assertNotNull( loadedEntry );
        assertTrue( loadedEntry.containsAttribute( "cn" ) );

        String cn = loadedEntry.get( "cn" ).get().getString();

        assertEquals( "a+b", cn );
    }


    @Test
    /**
     * tests adding en entry with escaped chars in the RDN
     */
    public void testAddEntryWithRdnContainingEscapedCharsMultiValued() throws Exception
    {
        //test as admin first
        Dn dn = new Dn( "cn=a\\+B,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: top",
            "ObjectClass: person",
            "cn: c",
            "sn: x" );

        connection.add( entry );

        Entry loadedEntry = connection.lookup( dn.getName(), "*" );
        assertNotNull( loadedEntry );
        assertTrue( loadedEntry.containsAttribute( "cn" ) );

        Attribute attribute = loadedEntry.get( "cn" );
        Set<String> expected = new HashSet<String>();
        expected.add( "a+B" );
        expected.add( "c" );
        int count = 0;

        for ( Value<?> value : attribute )
        {
            String val = value.getString();

            assertTrue( expected.contains( val ) );
            count++;

        }

        assertEquals( 2, count );
    }


    /**
     * the below test fails cause the API is failing to
     * preserve the UP name of the attribute of RDN
     * when the DN is schema-aware
     */
    @Test
    public void testPreserveRdnUpName() throws Exception
    {
        connection.setTimeOut( 0L );
        Dn dn = new Dn( getService().getSchemaManager(), "cn=testadd,ou=system" );
        Entry entry = new DefaultEntry( dn,
            "ObjectClass: person",
            "cn: testadd",
            "sn: testadd_sn" );

        connection.add( entry );

        assertTrue( session.exists( dn ) );

        entry = connection.lookup( dn );

        String ldif = LdifUtils.convertToLdif( entry );

        assertTrue( ldif.contains( dn.getName() ) );
    }


    @Test
    public void testAddNullValueSchemaAware() throws LdapException, IOException
    {
        connection.setTimeOut( 0L );
        connection.loadSchema();

        // Use the client API
        connection.bind( "uid=admin,ou=system", "secret" );

        // Add a new entry with some null values
        Entry entry = new DefaultEntry( getLdapServer().getDirectoryService().getSchemaManager(), "cn=test,ou=system",
            "ObjectClass: top",
            "ObjectClass: person",
            "ObjectClass: person",
            "ObjectClass: OrganizationalPerson",
            "ObjectClass: inetOrgPerson",
            "cn: test",
            "sn: Test",
            "userPassword:",
            "mail:" );

        connection.add( entry );

        // Now fetch the entry
        Entry found = connection.lookup( "cn=test,ou=system" );

        assertNotNull( found );
        assertNotNull( found.get( "userPassword" ) );
        assertNotNull( found.get( "mail" ) );
        byte[] userPassword = found.get( "userPassword" ).getBytes();
        String mail = found.get( "mail" ).getString();

        assertTrue( Strings.isEmpty( userPassword ) );
        assertTrue( Strings.isEmpty( mail ) );

        connection.close();
    }
}
