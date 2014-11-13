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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.text.ParseException;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.components.TransitedEncoding;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.TgsReq;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * Abstract base class for Ticket-Granting Service (TGS) tests, with utility methods
 * for generating message components.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractTicketGrantingServiceTest
{
    protected CipherTextHandler lockBox;
    protected static final SecureRandom random = new SecureRandom();

    /** Session attributes that must be verified. */
    protected EncryptionKey sessionKey;
    protected EncryptionKey subSessionKey;
    protected int sequenceNumber;
    protected KerberosTime now;
    protected int clientMicroSeconds = 0;


    protected Ticket getTgt( KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal, String serverPassword )
        throws Exception
    {
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, serverPassword );
        return getTicket( clientPrincipal, serverPrincipal, serverKey );
    }


    /**
     * Returns an encryption key derived from a principal name and passphrase.
     *
     * @param principal
     * @param passPhrase
     * @return The server's {@link EncryptionKey}.
     */
    protected EncryptionKey getEncryptionKey( KerberosPrincipal principal, String passPhrase )
    {
        KerberosKey kerberosKey = new KerberosKey( principal, passPhrase.toCharArray(), "AES128" );
        byte[] keyBytes = kerberosKey.getEncoded();
        return new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, keyBytes );
    }


    /**
     * Build the service ticket.  The service ticket contains the session key generated
     * by the KDC for the client and service to use.  The service will unlock the
     * authenticator with the session key from the ticket.  The principal in the ticket
     * must equal the authenticator client principal.
     * 
     * If set in the AP Options, the Ticket can also be sealed with the session key.
     * 
     * @param clientPrincipal
     * @param serverPrincipal
     * @param serverKey 
     * @return The {@link Ticket}.
     * @throws KerberosException
     */
    protected Ticket getTicket( KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal,
        EncryptionKey serverKey ) throws KerberosException, ParseException
    {
        EncTicketPart encTicketPart = new EncTicketPart();

        TicketFlags ticketFlags = new TicketFlags();
        ticketFlags.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setFlags( ticketFlags );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        encTicketPart.setKey( sessionKey );
        encTicketPart.setCName( new PrincipalName( clientPrincipal ) );
        encTicketPart.setCRealm( clientPrincipal.getRealm() );
        encTicketPart.setTransited( new TransitedEncoding() );
        encTicketPart.setAuthTime( new KerberosTime() );

        long now = System.currentTimeMillis();
        KerberosTime endTime = new KerberosTime( now + KerberosTime.DAY );
        encTicketPart.setEndTime( endTime );

        KerberosTime renewTill = new KerberosTime( now + KerberosTime.WEEK );
        encTicketPart.setRenewTill( renewTill );

        EncryptedData encryptedTicketPart = lockBox.seal( serverKey, encTicketPart,
            KeyUsage.AS_OR_TGS_REP_TICKET_WITH_SRVKEY );

        Ticket ticket = new Ticket();
        ticket.setSName( new PrincipalName( serverPrincipal.getName(), serverPrincipal.getNameType() ) );
        ticket.setRealm( serverPrincipal.getRealm() );
        ticket.setEncPart( encryptedTicketPart );

        ticket.setEncTicketPart( encTicketPart );

        return ticket;
    }


    protected EncTicketPart getTicketArchetype( KerberosPrincipal clientPrincipal ) throws KerberosException,
        ParseException
    {
        EncTicketPart encTicketPart = new EncTicketPart();

        TicketFlags ticketFlags = new TicketFlags();
        ticketFlags.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setFlags( ticketFlags );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        encTicketPart.setKey( sessionKey );
        encTicketPart.setCName( new PrincipalName( clientPrincipal ) );
        encTicketPart.setCRealm( clientPrincipal.getRealm() );
        encTicketPart.setTransited( new TransitedEncoding() );
        encTicketPart.setAuthTime( new KerberosTime() );

        long now = System.currentTimeMillis();
        KerberosTime endTime = new KerberosTime( now + KerberosTime.DAY );
        encTicketPart.setEndTime( endTime );

        KerberosTime renewTill = new KerberosTime( now + KerberosTime.WEEK );
        encTicketPart.setRenewTill( renewTill );

        return encTicketPart;
    }


    protected Ticket getTicket( EncTicketPart encTicketPart, KerberosPrincipal serverPrincipal,
        EncryptionKey serverKey ) throws KerberosException, ParseException
    {
        EncryptedData encryptedTicketPart = lockBox.seal( serverKey, encTicketPart,
            KeyUsage.AS_OR_TGS_REP_TICKET_WITH_SRVKEY );

        Ticket ticket = new Ticket();
        ticket.setTktVno( 5 );
        ticket.setSName( new PrincipalName( serverPrincipal.getName(), PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket.setRealm( serverPrincipal.getRealm() );
        ticket.setEncPart( encryptedTicketPart );

        ticket.setEncTicketPart( encTicketPart );

        return ticket;
    }


    protected KdcReq getKdcRequest( Ticket tgt, KdcReqBody requestBody ) throws Exception
    {
        return getKdcRequest( tgt, requestBody, ChecksumType.RSA_MD5 );
    }


    /**
     * Create a KdcReq, suitable for requesting a service Ticket.
     */
    protected KdcReq getKdcRequest( Ticket tgt, KdcReqBody kdcReqBody, ChecksumType checksumType )
        throws Exception
    {
        // Get the session key from the service ticket.
        sessionKey = tgt.getEncTicketPart().getKey();

        // Generate a new sequence number.
        sequenceNumber = random.nextInt();
        now = new KerberosTime();

        EncryptedData authenticator = getAuthenticator(
            KerberosUtils.getKerberosPrincipal( tgt.getEncTicketPart().getCName(), tgt.getEncTicketPart().getCRealm() ),
            kdcReqBody, checksumType );

        PaData[] paDatas = getPreAuthenticationData( tgt, authenticator );

        KdcReq message = new TgsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        return message;
    }


    /**
     * Build the authenticator.  The authenticator communicates the sub-session key the
     * service will use to unlock the private message.  The service will unlock the
     * authenticator with the session key from the ticket.  The authenticator client
     * principal must equal the principal in the ticket.  
     *
     * @param clientPrincipal
     * @return The {@link EncryptedData} containing the {@link Authenticator}.
     * @throws KerberosException
     */
    protected EncryptedData getAuthenticator( KerberosPrincipal clientPrincipal, KdcReqBody requestBody,
        ChecksumType checksumType ) throws EncoderException, KerberosException
    {
        Authenticator authenticator = new Authenticator();

        clientMicroSeconds = random.nextInt( 999999 );

        authenticator.setVersionNumber( 5 );
        authenticator.setCName( new PrincipalName( clientPrincipal.getName(), clientPrincipal.getNameType() ) );
        authenticator.setCRealm( clientPrincipal.getRealm() );
        authenticator.setCTime( now );
        authenticator.setCusec( clientMicroSeconds );
        authenticator.setSubKey( subSessionKey );
        authenticator.setSeqNumber( sequenceNumber );

        Checksum checksum = getBodyChecksum( requestBody, checksumType );
        authenticator.setCksum( checksum );

        EncryptedData encryptedAuthenticator = lockBox.seal( sessionKey, authenticator,
            KeyUsage.TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_TGS_SESS_KEY );

        return encryptedAuthenticator;
    }


    protected Checksum getBodyChecksum( KdcReqBody kdcReqBody, ChecksumType checksumType ) throws EncoderException,
        KerberosException
    {
        ByteBuffer buffer = ByteBuffer.allocate( kdcReqBody.computeLength() );
        byte[] bodyBytes = kdcReqBody.encode( buffer ).array();

        ChecksumHandler checksumHandler = new ChecksumHandler();

        return checksumHandler
            .calculateChecksum( checksumType, bodyBytes, null, KeyUsage.TGS_REP_ENC_PART_TGS_SESS_KEY );
    }


    /**
     * Make new AP_REQ, aka the "auth header," and package it into pre-authentication data.
     *
     * @param ticket
     * @param authenticator
     * @return
     * @throws IOException
     */
    protected PaData[] getPreAuthenticationData( Ticket ticket, EncryptedData authenticator )
        throws EncoderException
    {
        ApReq applicationRequest = new ApReq();
        applicationRequest.setApOptions( new ApOptions() );
        applicationRequest.setTicket( ticket );
        applicationRequest.setAuthenticator( authenticator );

        ByteBuffer buffer = ByteBuffer.allocate( applicationRequest.computeLength() );
        byte[] encodedApReq = applicationRequest.encode( buffer ).array();

        PaData[] paData = new PaData[1];

        PaData preAuth = new PaData();
        preAuth.setPaDataType( PaDataType.PA_TGS_REQ );
        preAuth.setPaDataValue( encodedApReq );

        paData[0] = preAuth;

        return paData;
    }


    protected PrincipalName getPrincipalName( String name )
    {
        PrincipalName principalName = new PrincipalName();
        principalName.addName( name );
        principalName.setNameType( PrincipalNameType.KRB_NT_PRINCIPAL );

        return principalName;
    }
}
