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


import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests configuration of Authentication Service (AS) policy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticationPolicyTest extends AbstractAuthenticationServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link AuthenticationPolicyTest}.
     */
    public AuthenticationPolicyTest()
    {
        config = new KdcServer( null, null, null );
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new DummySession();
    }


    /**
     * Tests when forwardable tickets are disallowed that requests for
     * forwardable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testForwardableTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setForwardableAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxiable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testProxiableTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setProxiableAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * ALLOW-POSTDATE tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testAllowPostdate() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setPostdatedAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * postdated tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testPostdate() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setPostdatedAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * RENEWABLE-OK tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testRenewableOk() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setRenewableAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testRenewableTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setPaEncTimestampRequired( false );
        config.setRenewableAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        modifier.setRtime( requestedRenewTillTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed that requests with no addresses
     * fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testEmptyAddresses() throws Exception
    {
        // Deny empty addresses in policy.
        config.setPaEncTimestampRequired( false );
        config.setEmptyAddressesAllowed( false );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        modifier.setRtime( requestedRenewTillTime );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }
}
