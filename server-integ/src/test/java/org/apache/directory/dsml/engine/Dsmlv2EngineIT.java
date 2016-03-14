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

package org.apache.directory.dsml.engine;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.directory.api.dsmlv2.Dsmlv2ResponseParser;
import org.apache.directory.api.dsmlv2.engine.Dsmlv2Engine;
import org.apache.directory.api.dsmlv2.response.BatchResponseDsml;
import org.apache.directory.api.dsmlv2.response.SearchResponse;
import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test for Dsmlv2Engine.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "Dsmlv2EngineTest-DS")
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class Dsmlv2EngineIT extends AbstractLdapTestUnit
{
    private LdapConnection connection;

    private Dsmlv2Engine engine;


    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
        engine = new Dsmlv2Engine( connection, "uid=admin,ou=system", "secret" );
    }


    @After
    public void unbind() throws Exception
    {
        connection.unBind();
        connection.close();
    }


    @Test
    public void testEngineWithSearchRequest() throws Exception
    {
        InputStream dsmlIn = getClass().getClassLoader().getResourceAsStream( "dsml-search-req.xml" );

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        engine.processDSML( dsmlIn, byteOut );

        Dsmlv2ResponseParser respParser = new Dsmlv2ResponseParser( LdapApiServiceFactory.getSingleton() );
        respParser.setInput( byteOut.toString() );

        respParser.parseAllResponses();

        BatchResponseDsml batchResp = respParser.getBatchResponse();

        assertNotNull( batchResp );

        assertEquals( 101, batchResp.getRequestID() );

        SearchResponse searchResp = ( SearchResponse ) batchResp.getCurrentResponse().getDecorated();

        assertEquals( 5, searchResp.getSearchResultEntryList().size() );
    }


    @Test
    public void testEngineWithSearchResponseInSoapEnvelope() throws Exception
    {
        InputStream dsmlIn = getClass().getClassLoader().getResourceAsStream( "dsml-search-req.xml" );

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        engine.setGenerateSoapResp( true );

        engine.processDSML( dsmlIn, byteOut );

        engine.setGenerateSoapResp( false );

        String resp = byteOut.toString();

        Dsmlv2ResponseParser respParser = new Dsmlv2ResponseParser( LdapApiServiceFactory.getSingleton() );
        respParser.setInput( resp );

        respParser.parseAllResponses();

        BatchResponseDsml batchResp = respParser.getBatchResponse();

        assertNotNull( batchResp );

        assertEquals( 101, batchResp.getRequestID() );

        SearchResponse searchResp = ( SearchResponse ) batchResp.getCurrentResponse().getDecorated();

        assertEquals( 5, searchResp.getSearchResultEntryList().size() );
    }
}
