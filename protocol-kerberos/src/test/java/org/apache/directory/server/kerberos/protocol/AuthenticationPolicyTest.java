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
package org.apache.directory.server.kerberos.protocol;


import static org.junit.Assert.assertEquals;

import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.messages.AsReq;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests configuration of Authentication Service (AS) policy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationPolicyTest extends AbstractAuthenticationServiceTest
{
    private KerberosConfig config;
    private KdcServer kdcServer;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link AuthenticationPolicyTest}.
     */
    @Before
    public void setUp()
    {
        kdcServer = new KdcServer();
        config = kdcServer.getConfig();
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( kdcServer, store );
        session = new KrbDummySession();
    }


    /**
     * Shutdown the Kerberos server
     */
    @After
    public void shutDown()
    {
        kdcServer.stop();
    }


    /**
     * Tests when forwardable tickets are disallowed that requests for
     * forwardable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testForwardableTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setForwardableAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxiable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testProxiableTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setProxiableAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * ALLOW-POSTDATE tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testAllowPostdate() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setPostdatedAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * postdated tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testPostdate() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setPostdatedAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * RENEWABLE-OK tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testRenewableOk() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setRenewableAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testRenewableTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setRenewableAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed that requests with no addresses
     * fail with the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testEmptyAddresses() throws Exception
    {
        // Deny empty addresses in policy.
        config.setPaEncTimestampRequired( false );
        config.setEmptyAddressesAllowed( false );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }
}
