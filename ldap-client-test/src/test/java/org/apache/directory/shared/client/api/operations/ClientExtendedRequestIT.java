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

import javax.naming.ldap.StartTlsRequest;

import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.ExtendedFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.apache.directory.shared.ldap.model.message.ExtendedRequest;
import org.apache.directory.shared.ldap.model.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.model.message.ExtendedResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the extended operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { 
      @CreateTransport(protocol = "LDAP"), 
      @CreateTransport(protocol = "LDAPS") 
    }, 
    extendedOpHandlers = { StartTlsHandler.class })
public class ClientExtendedRequestIT extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    @Test
    public void testExtended() throws Exception
    {
        try
        {
            ExtendedResponse response = connection.extended( StartTlsRequest.OID );
            assertNotNull( response );
            assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        }
        finally
        {
            // close connection to stop TLS
            connection.close();
        }
    }


    @Test
    public void testExtendedAsync() throws Exception
    {
        try
        {
            ExtendedRequest extendedRequest = new ExtendedRequestImpl();
            extendedRequest.setRequestName( StartTlsRequest.OID );

            ExtendedFuture extendedFuture = connection.extendedAsync( extendedRequest );

            ExtendedResponse extendedResponse = ( ExtendedResponse ) extendedFuture.get( 1000, TimeUnit.MILLISECONDS );

            assertNotNull( extendedResponse );
            assertEquals( ResultCodeEnum.SUCCESS, extendedResponse.getLdapResult().getResultCode() );
            assertTrue( connection.isAuthenticated() );
        }
        finally
        {
            // close connection to stop TLS
            connection.close();
        }

    }
}