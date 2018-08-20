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


import java.text.ParseException;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.components.TransitedEncoding;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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

        return new EncryptionKey( EncryptionType.DES_CBC_MD5, serverKeyBytes );
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
        EncryptionKey serverKey ) throws KerberosException, ParseException
    {
        EncTicketPart encTicketPart = new EncTicketPart();

        TicketFlags ticketFlags = new TicketFlags();
        ticketFlags.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setFlags( ticketFlags );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        encTicketPart.setKey( sessionKey );
        encTicketPart.setCName( new PrincipalName( clientPrincipal.getName(), PrincipalNameType.KRB_NT_PRINCIPAL ) );
        encTicketPart.setTransited( new TransitedEncoding() );
        encTicketPart.setAuthTime( new KerberosTime() );

        long now = System.currentTimeMillis();
        KerberosTime endTime = new KerberosTime( now + ONE_DAY );
        encTicketPart.setEndTime( endTime );

        KerberosTime renewTill = new KerberosTime( now + ONE_WEEK );
        encTicketPart.setRenewTill( renewTill );

        EncryptedData encryptedTicketPart = cipherTextHandler.seal( serverKey, encTicketPart,
            KeyUsage.AS_OR_TGS_REP_TICKET_WITH_SRVKEY );

        Ticket ticket = new Ticket();
        ticket.setTktVno( KerberosConstants.KERBEROS_V5 );
        ticket.setSName( new PrincipalName( serverPrincipal.getName(), PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket.setRealm( serverPrincipal.getRealm() );
        ticket.setEncPart( encryptedTicketPart );

        return ticket;
    }
}
