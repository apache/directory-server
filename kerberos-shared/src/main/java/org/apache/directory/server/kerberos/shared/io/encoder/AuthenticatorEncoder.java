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
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
     * 
     * @param authenticator 
     * @return The {@link DERSequence}.
     */
    private DERSequence encodeInitialSequence( Authenticator authenticator )
    {
        String clientRealm = authenticator.getClientRealm();

        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( authenticator.getVersionNumber() ) ) );
        sequence.add( new DERTaggedObject( 1, DERGeneralString.valueOf( clientRealm ) ) );
        sequence.add( new DERTaggedObject( 2, PrincipalNameEncoder.encode( authenticator.getClientPrincipalName() ) ) );

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
