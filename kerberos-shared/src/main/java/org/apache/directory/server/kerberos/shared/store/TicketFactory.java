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
import java.text.ParseException;
import java.util.Date;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.encoder.TicketEncoder;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.components.TicketModifier;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlag;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;


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
        EncTicketPart ticketPart = new EncTicketPart();

        TicketFlags ticketFlags = new TicketFlags();
        ticketFlags.setFlag( TicketFlag.RENEWABLE );
        
        ticketPart.setFlags( ticketFlags );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        ticketPart.setSessionKey( sessionKey );
        
        try
        {
            ticketPart.setClientPrincipal( clientPrincipal );
        }
        catch ( ParseException pe )
        {
            throw new KerberosException( KerberosErrorType.KRB_ERR_GENERIC, "Bad principal name : " + clientPrincipal );
        }
        
        ticketPart.setTransitedEncoding( new TransitedEncoding() );
        ticketPart.setAuthTime( new KerberosTime() );

        long now = System.currentTimeMillis();
        KerberosTime endTime = new KerberosTime( now + ONE_DAY );
        ticketPart.setEndTime( endTime );

        KerberosTime renewTill = new KerberosTime( now + ONE_WEEK );
        ticketPart.setRenewTill( renewTill );

        EncryptedData encryptedTicketPart = cipherTextHandler.seal( serverKey, ticketPart, KeyUsage.NUMBER2 );

        TicketModifier ticketModifier = new TicketModifier();
        ticketModifier.setTicketVersionNumber( 5 );
        ticketModifier.setServerPrincipal( serverPrincipal );
        ticketModifier.setEncPart( encryptedTicketPart );

        Ticket ticket = ticketModifier.getTicket();

        ticket.setEncTicketPart( ticketPart );

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

        KerberosPrincipal clientPrincipal = 
            new KerberosPrincipal( ticket.getClientPrincipalName().getNameComponent() + '@' + ticket.getClientRealm() );
        KerberosPrincipal server = ticket.getServerPrincipal();
        byte[] sessionKey = ticket.getSessionKey().getKeyValue();
        int keyType = ticket.getSessionKey().getKeyType().getOrdinal();

        boolean[] flags = new boolean[32];

        for ( int ii = 0; ii < flags.length; ii++ )
        {
            flags[ii] = ticket.getFlag( ii );
        }

        Date authTime = ticket.getAuthTime().toDate();
        Date endTime = ticket.getEndTime().toDate();

        Date startTime = ( ticket.getStartTime() != null ? ticket.getStartTime().toDate() : null );

        Date renewTill = null;

        if ( ticket.getFlags().isRenewable() )
        {
            renewTill = ( ticket.getRenewTill() != null ? ticket.getRenewTill().toDate() : null );
        }

        InetAddress[] clientAddresses = new InetAddress[0];

        return new KerberosTicket( asn1Encoding, clientPrincipal, server, sessionKey, keyType, flags, authTime, startTime,
            endTime, renewTill, clientAddresses );
    }
}
