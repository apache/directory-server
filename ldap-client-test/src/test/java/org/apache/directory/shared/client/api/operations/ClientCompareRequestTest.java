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

import java.util.concurrent.TimeUnit;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.CompareFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.model.message.CompareResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the compare operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class ClientCompareRequestTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;
    private CoreSession session;


    @Before
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
        session = getLdapServer().getDirectoryService().getAdminSession();
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    @Test
    public void testCompare() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        boolean response = connection.compare( dn, SchemaConstants.UID_AT, "admin" );
        assertTrue( response );

        response = connection.compare( dn.getName(), SchemaConstants.USER_PASSWORD_AT, "secret".getBytes() );
        assertNotNull( response );
        assertTrue( response );
    }


    @Test
    public void testCompareAsync() throws Exception
    {
        Dn dn = new Dn( "uid=admin,ou=system" );

        CompareRequest compareRequest = new CompareRequestImpl();
        compareRequest.setName( dn );
        compareRequest.setAttributeId( SchemaConstants.UID_AT );
        compareRequest.setAssertionValue( "admin" );

        connection.compare( compareRequest );

        assertTrue( session.exists( dn ) );

        CompareFuture compareFuture = connection.compareAsync( compareRequest );

        CompareResponse compareResponse = compareFuture.get( 1000, TimeUnit.MILLISECONDS );

        assertNotNull( compareResponse );
        assertEquals( ResultCodeEnum.COMPARE_TRUE, compareResponse.getLdapResult().getResultCode() );
    }
}
