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
package org.apache.directory.server.kerberos.shared.io.encoder;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERBitString;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTicketPartEncoder implements Encoder, EncoderFactory
{
    /**
     * Application code constant for the {@link EncTicketPart} (3).
     */
    private static final int APPLICATION_CODE = 3;


    public byte[] encode( Encodable ticketPart ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence ticketSequence = encodeInitialSequence( ( EncTicketPart ) ticketPart );
        aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, ticketSequence ) );
        aos.close();

        return baos.toByteArray();
    }


    public Encoder getEncoder()
    {
        return new EncTicketPartEncoder();
    }


    /**
     * Encodes an {@link EncTicketPart} into a {@link DERSequence}.
     * 
     * -- Encrypted part of ticket
     * EncTicketPart ::=     [APPLICATION 3] SEQUENCE {
     *                       flags[0]             TicketFlags,
     *                       key[1]               EncryptionKey,
     *                       crealm[2]            Realm,
     *                       cname[3]             PrincipalName,
     *                       transited[4]         TransitedEncoding,
     *                       authtime[5]          KerberosTime,
     *                       starttime[6]         KerberosTime OPTIONAL,
     *                       endtime[7]           KerberosTime,
     *                       renew-till[8]        KerberosTime OPTIONAL,
     *                       caddr[9]             HostAddresses OPTIONAL,
     *                       authorization-data[10]   AuthorizationData OPTIONAL
     * }
     * 
     * EncTicketPart :
     *
     * 0x63 L1 EncTicketPart
     * |
     * +--> 0x30 L2 EncTicketPart SEQUENCE
     * |
     * +--> 0xA0 L3 flags tag
     * |     |
     * |     +--> 0x05 L3-1 flags (bitstring)
     * |
     * +--> 0xA1 L4 key (EncryptionKey)
     * |
     * +--> 0xA2 L5 crealm tag
     * |     |
     * |     +--> 0x1B L5-1 crealm (generalizedString)
     * |
     * +--> 0xA3 L6 cname (PrincipalName)
     * |
     * +--> 0xA4 L7 transited (TransitedEncoding)
     * |
     * +--> 0xA5 L8 authtime tag
     * |     |
     * |     +--> 0x18 L8-1 authtime (generalizedTime)
     * |
     * +--> [0xA6 L9 starttime tag
     * |      |
     * |      +--> 0x18 L9-1 starttime (generalizedTime)]
     * |
     * +--> 0xA7 L10 endtime tag
     * |     |
     * |     +--> 0x18 L10-1 endtime (generalizedTime)
     * |
     * +--> [0xA8 L11 renew-till tag
     * |     |
     * |     +--> 0x18 L11-1 renew-till (generalizedTime)]
     * |
     * +--> [0xA9 L12 caddr:HostAddresses]
     * |
     * +--> [0xAA L13 authorization-data:AuthorizationData]
     * @param ticketPart 
     * @return The {@link DERSequence}.
     */
    public DERSequence encodeInitialSequence( EncTicketPart ticketPart )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, new DERBitString( 32, ticketPart.getFlags().getIntValue() ) ) );
        sequence.add( new DERTaggedObject( 1, EncryptionKeyEncoder.encodeSequence( ticketPart.getSessionKey() ) ) );
        sequence.add( new DERTaggedObject( 2, DERGeneralString.valueOf( ticketPart.getClientRealm().toString() ) ) );
        sequence.add( new DERTaggedObject( 3, PrincipalNameEncoder.encode( ticketPart.getClientPrincipal() ) ) );
        sequence.add( new DERTaggedObject( 4, TransitedEncodingEncoder.encode( ticketPart.getTransitedEncoding() ) ) );
        sequence.add( new DERTaggedObject( 5, KerberosTimeEncoder.encode( ticketPart.getAuthTime() ) ) );

        // OPTIONAL
        if ( ticketPart.getStartTime() != null )
        {
            sequence.add( new DERTaggedObject( 6, KerberosTimeEncoder.encode( ticketPart.getStartTime() ) ) );
        }

        sequence.add( new DERTaggedObject( 7, KerberosTimeEncoder.encode( ticketPart.getEndTime() ) ) );

        // OPTIONAL
        if ( ticketPart.getRenewTill() != null )
        {
            sequence.add( new DERTaggedObject( 8, KerberosTimeEncoder.encode( ticketPart.getRenewTill() ) ) );
        }

        // OPTIONAL
        if ( ticketPart.getClientAddresses() != null )
        {
            sequence
                .add( new DERTaggedObject( 9, HostAddressesEncoder.encodeSequence( ticketPart.getClientAddresses() ) ) );
        }

        // OPTIONAL
        if ( ticketPart.getAuthorizationData() != null )
        {
            sequence
                .add( new DERTaggedObject( 10, AuthorizationDataEncoder.encode( ticketPart.getAuthorizationData() ) ) );
        }

        return sequence;
    }
}
