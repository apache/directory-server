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
package org.apache.directory.server.kerberos.shared.store;


import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import org.apache.directory.server.kerberos.shared.KerberosConstants;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.encoder.TicketEncoder;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TicketFactory
{
    /** One day in milliseconds, used for default end time. */
    private static final int ONE_DAY = 86400000;

    /** One week in milliseconds, used for default renewal period. */
    private static final int ONE_WEEK = 86400000 * 7;

    private CipherTextHandler cipherTextHandler = new CipherTextHandler();


    /**
     * Returns a server key derived from a server principal and server password.
     *
     * @param serverPrincipal
     * @param serverPassword
     * @return The server's {@link EncryptionKey}.
     */
    public EncryptionKey getServerKey( KerberosPrincipal serverPrincipal, String serverPassword )
    {
        KerberosKey serverKerberosKey = new KerberosKey( serverPrincipal, serverPassword.toCharArray(), "DES" );
        byte[] serverKeyBytes = serverKerberosKey.getEncoded();
        EncryptionKey serverKey = new EncryptionKey( EncryptionType.DES_CBC_MD5, serverKeyBytes );

        return serverKey;
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
    public Ticket getTicket( KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal,
        EncryptionKey serverKey ) throws KerberosException
    {
        EncTicketPartModifier encTicketModifier = new EncTicketPartModifier();

        TicketFlags ticketFlags = new TicketFlags();
        ticketFlags.set( TicketFlags.RENEWABLE );
        encTicketModifier.setFlags( ticketFlags );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        encTicketModifier.setSessionKey( sessionKey );
        encTicketModifier.setClientPrincipal( clientPrincipal );
        encTicketModifier.setTransitedEncoding( new TransitedEncoding() );
        encTicketModifier.setAuthTime( new KerberosTime() );

        long now = System.currentTimeMillis();
        KerberosTime endTime = new KerberosTime( now + ONE_DAY );
        encTicketModifier.setEndTime( endTime );

        KerberosTime renewTill = new KerberosTime( now + ONE_WEEK );
        encTicketModifier.setRenewTill( renewTill );

        EncTicketPart encTicketPart = encTicketModifier.getEncTicketPart();

        EncryptedData encryptedTicketPart = cipherTextHandler.seal( serverKey, encTicketPart, KeyUsage.NUMBER2 );

        Ticket ticket = new Ticket();
        ticket.setTktVno( KerberosConstants.KERBEROS_V5 );
        ticket.setServerPrincipal( serverPrincipal );
        ticket.setEncPart( encryptedTicketPart );

        ticket.setEncTicketPart( encTicketPart );

        return ticket;
    }


    /**
     * Convert an Apache Directory Kerberos {@link Ticket} into a {@link KerberosTicket}.
     *
     * @param ticket
     * @return The {@link KerberosTicket}.
     * @throws IOException 
     */
    public KerberosTicket getKerberosTicket( Ticket ticket ) throws IOException
    {
        byte[] asn1Encoding = TicketEncoder.encodeTicket( ticket );

        KerberosPrincipal client = ticket.getEncTicketPart().getClientPrincipal();
        KerberosPrincipal server = ticket.getServerPrincipal();
        byte[] sessionKey = ticket.getEncTicketPart().getSessionKey().getKeyValue();
        int keyType = ticket.getEncTicketPart().getSessionKey().getKeyType().getOrdinal();

        boolean[] flags = new boolean[32];

        for ( int ii = 0; ii < flags.length; ii++ )
        {
            flags[ii] = ticket.getEncTicketPart().getFlags().get( ii );
        }

        Date authTime = ticket.getEncTicketPart().getAuthTime().toDate();
        Date endTime = ticket.getEncTicketPart().getEndTime().toDate();

        Date startTime = ( ticket.getEncTicketPart().getStartTime() != null ? ticket.getEncTicketPart().getStartTime().toDate() : null );

        Date renewTill = null;

        if ( ticket.getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) )
        {
            renewTill = ( ticket.getEncTicketPart().getRenewTill() != null ? ticket.getEncTicketPart().getRenewTill().toDate() : null );
        }

        InetAddress[] clientAddresses = new InetAddress[0];

        return new KerberosTicket( asn1Encoding, client, server, sessionKey, keyType, flags, authTime, startTime,
            endTime, renewTill, clientAddresses );
    }
}
