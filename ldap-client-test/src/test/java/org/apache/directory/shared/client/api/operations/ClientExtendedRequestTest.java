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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.naming.ldap.StartTlsRequest;

import org.apache.directory.api.ldap.extras.extended.startTls.StartTlsRequestImpl;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.PooledLdapConnection;
import org.apache.directory.ldap.client.api.future.ExtendedFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the extended operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS")
},
    extendedOpHandlers =
        { StartTlsHandler.class })
public class ClientExtendedRequestTest extends AbstractLdapTestUnit
{
    private PooledLdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        connection = ( PooledLdapConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @AfterEach
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
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
        ExtendedRequest extendedRequest = new StartTlsRequestImpl();
        extendedRequest.setRequestName( StartTlsRequest.OID );

        ExtendedFuture extendedFuture = ( ( LdapNetworkConnection ) connection.wrapped() ).extendedAsync( extendedRequest );

        ExtendedResponse extendedResponse = ( ExtendedResponse ) extendedFuture.get( 1000, TimeUnit.MILLISECONDS );

        assertNotNull( extendedResponse );
        assertEquals( ResultCodeEnum.SUCCESS, extendedResponse.getLdapResult().getResultCode() );
        assertTrue( connection.isAuthenticated() );
    }
}