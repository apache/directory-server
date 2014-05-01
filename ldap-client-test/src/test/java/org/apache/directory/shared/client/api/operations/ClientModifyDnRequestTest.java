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

import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.ModifyDnFuture;
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
 * Testcase for modifyDn operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@ApplyLdifs(
    { 
        "dn: cn=modDn,ou=system", 
        "objectClass: person", 
        "cn: modDn", 
        "sn: snModDn",
        "",
        "dn: employeeNumber=test,ou=system", 
        "objectClass: person", 
        "objectClass: inetorgPerson",
        "cn: modDn",
        "employeeNumber: test",
        "sn: snModDn"
    })
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class ClientModifyDnRequestTest extends AbstractLdapTestUnit
{
    private static final String DN = "cn=modDn,ou=system";
    private static final String DN_EMPLOYEE = "employeeNumber=test,ou=system";
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
    public void testRename() throws Exception
    {
        connection.rename( DN, "cn=modifyDnWithString" );
        Entry entry = session.lookup( new Dn( "cn=modifyDnWithString,ou=system" ), "*" );
        assertTrue( session.exists( new Dn( "cn=modifyDnWithString,ou=system" ) ) );
        assertTrue( entry.contains( "cn", "modifyDnWithString" ) );
        assertFalse( entry.contains( "cn", "modDn" ) );
    }


    /**
     * Check that if we try to modify the RDN which contains a single-value attribute,
     * and if we set the deleteOldRdn to false (leading to the injection of a new single-value
     * attribute while we keep the old one), then we get a failure.
     * @throws Exception
     */
    @Test(expected=LdapInvalidAttributeValueException.class)
    public void testRenameSingleValue() throws Exception
    {
        connection.rename( DN_EMPLOYEE, "employeeNumber=newValue", false );
    }


    @Test
    public void testRenameWithoutDeleteOldRdn() throws Exception
    {
        connection.rename( DN, "cn=modifyDnWithString", false );

        Dn oldDn = new Dn( DN );
        assertFalse( session.exists( oldDn ) );

        Entry entry = session.lookup( new Dn( "cn=modifyDnWithString,ou=system" ) );
        assertNotNull( entry );

        Rdn oldRdn = oldDn.getRdn();
        assertTrue( entry.contains( oldRdn.getType(), oldRdn.getNormValue() ) );
    }


    @Test
    public void testMove() throws Exception
    {
        connection.move( DN, "ou=users,ou=system" );

        Dn oldDn = new Dn( DN );
        assertFalse( session.exists( oldDn ) );

        assertTrue( session.exists( new Dn( "cn=modDn,ou=users,ou=system" ) ) );
    }


    @Test
    public void testMoveAndRename() throws Exception
    {
        Dn origDn = new Dn( "cn=testadd,ou=users,ou=system" );
        Entry entry = new DefaultEntry( origDn );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.PERSON_OC );
        entry.add( SchemaConstants.CN_AT, "testadd" );
        entry.add( SchemaConstants.SN_AT, "testadd_sn" );

        connection.add( entry );

        Dn newDn = new Dn( "cn=testaddMovedAndRenamed,ou=system" );
        connection.moveAndRename( origDn, newDn );

        assertFalse( session.exists( origDn ) );

        entry = session.lookup( newDn, "+" );

        assertTrue( entry.containsAttribute( SchemaConstants.MODIFIERS_NAME_AT ) );
        assertTrue( entry.containsAttribute( SchemaConstants.MODIFY_TIMESTAMP_AT ) );
    }


    @Test
    public void testModifyDnAsync() throws Exception
    {
        Dn oldDn = new Dn( DN );
        Dn newDn = new Dn( "cn=modifyDnWithString,ou=system" );

        ModifyDnRequest modDnReq = new ModifyDnRequestImpl();
        modDnReq.setName( oldDn );
        modDnReq.setNewRdn( new Rdn( "cn=modifyDnWithString" ) );
        modDnReq.setDeleteOldRdn( true );

        ModifyDnFuture modifyDnFuture = connection.modifyDnAsync( modDnReq );

        ModifyDnResponse response = modifyDnFuture.get( 1000, TimeUnit.MILLISECONDS );

        assertNotNull( response );

        assertTrue( connection.isAuthenticated() );
        assertFalse( session.exists( oldDn ) );
        assertTrue( session.exists( newDn ) );

        assertTrue( session.exists( new Dn( "cn=modifyDnWithString,ou=system" ) ) );
    }
}
