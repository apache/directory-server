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
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticatorEncoder implements Encoder, EncoderFactory
{
    /**
     * Application code constant for the {@link Authenticator} (2).
     */
    private static final int APPLICATION_CODE = 2;


    /**
     * Encodes an {@link Authenticator} into a byte array.
     *
     * @param authenticator
     * @return The byte array.
     * @throws IOException
     */
    public byte[] encode( Encodable authenticator ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence replySequence = encodeInitialSequence( ( Authenticator ) authenticator );
        aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, replySequence ) );
        aos.close();

        return baos.toByteArray();
    }


    public Encoder getEncoder()
    {
        return new AuthenticatorEncoder();
    }


    /**
     * Encodes an {@link Authenticator} into a {@link DERSequence}.
     * 
     * -- Unencrypted authenticator
     * Authenticator ::=    [APPLICATION 2] SEQUENCE
     * {
     *                authenticator-vno[0]          INTEGER,
     *                crealm[1]                     Realm,
     *                cname[2]                      PrincipalName,
     *                cksum[3]                      Checksum OPTIONAL,
     *                cusec[4]                      INTEGER,
     *                ctime[5]                      KerberosTime,
     *                subkey[6]                     EncryptionKey OPTIONAL,
     *                seq-number[7]                 INTEGER OPTIONAL,
     *  
     *                authorization-data[8]         AuthorizationData OPTIONAL
     * }
     * Authenticator :
     *
     * 0x62 L1 Authenticator Tag (Application 2)
     * |
     * +--> 0x30 L2 Authenticator sequence
     * |
     * +--> 0xA0 L2 authenticator-vno tag
     * |     |
     * |     +--> 0x02 L2-1 authenticator-vno (int)
     * | 
     * +--> 0xA1 L3 crealm tag
     * |     |
     * |     +--> 0x1B L3-1 crealm (crealm)
     * | 
     * +--> 0xA2 L4 cname tag
     * |     |
     * |     +--> 0x30 L4-1 cname (PrincipalName)
     * | 
     * +--> [0xA3 L5 cksum tag
     * |     |
     * |     +--> 0x30 L5-1 cksum (Checksum)] (optional)
     * | 
     * +--> 0xA4 L6 cusec tag 
     * |     |
     * |     +--> 0x02 L6-1 cusec (int)
     * | 
     * +--> 0xA5 0x11 ctime tag
     * |     |
     * |     +--> 0x18 0x0F ctime (KerberosTime)
     * | 
     * +--> [0xA6 L7 subkey tag
     * |      |
     * |      +--> 0x30 L7-1 subkey (EncryptionKey)] (optional)
     * | 
     * +--> [0xA7 L8 seqNumber tag
     * |      |
     * |      +--> 0x02 L8-1 seqNulber (int > 0)] (optional)
     * | 
     * +--> [0xA8 L9 authorization-data tag
     * |
     * +--> 0x30 L9-1 authorization-data (AuthorizationData)] (optional)
     * 
     * @param authenticator 
     * @return The {@link DERSequence}.
     */
    private DERSequence encodeInitialSequence( Authenticator authenticator )
    {
        String clientRealm = authenticator.getClientPrincipal().getRealm();

        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( authenticator.getVersionNumber() ) ) );
        sequence.add( new DERTaggedObject( 1, DERGeneralString.valueOf( clientRealm ) ) );
        sequence.add( new DERTaggedObject( 2, PrincipalNameEncoder.encode( authenticator.getClientPrincipal() ) ) );

        // OPTIONAL
        if ( authenticator.getChecksum() != null )
        {
            sequence.add( new DERTaggedObject( 3, ChecksumEncoder.encode( authenticator.getChecksum() ) ) );
        }

        sequence.add( new DERTaggedObject( 4, DERInteger.valueOf( authenticator.getClientMicroSecond() ) ) );
        sequence.add( new DERTaggedObject( 5, KerberosTimeEncoder.encode( authenticator.getClientTime() ) ) );

        // OPTIONAL
        if ( authenticator.getSubSessionKey() != null )
        {
            sequence.add( new DERTaggedObject( 6, EncryptionKeyEncoder
                .encodeSequence( authenticator.getSubSessionKey() ) ) );
        }

        // OPTIONAL
        if ( authenticator.getSequenceNumber() > 0 )
        {
            sequence.add( new DERTaggedObject( 7, DERInteger.valueOf( authenticator.getSequenceNumber() ) ) );
        }

        // OPTIONAL
        if ( authenticator.getAuthorizationData() != null )
        {
            sequence.add( new DERTaggedObject( 8, AuthorizationDataEncoder
                .encode( authenticator.getAuthorizationData() ) ) );
        }

        return sequence;
    }
}
