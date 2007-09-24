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
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncKrbPrivPartEncoder implements Encoder, EncoderFactory
{
    private static final int APPLICATION_CODE = 28;


    public Encoder getEncoder()
    {
        return new EncKrbPrivPartEncoder();
    }


    public byte[] encode( Encodable privPart ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence privPartSequence = encodePrivatePartSequence( ( EncKrbPrivPart ) privPart );
        aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, privPartSequence ) );
        aos.close();

        return baos.toByteArray();
    }


    /**
     * Encodes an {@link EncKrbPrivPart} into a {@link DERSequence}.
     * 
     * EncKrbPrivPart  ::= [APPLICATION 28] SEQUENCE {
     *         user-data       [0] OCTET STRING,
     *         timestamp       [1] KerberosTime OPTIONAL,
     *         usec            [2] Microseconds OPTIONAL,
     *         seq-number      [3] UInt32 OPTIONAL,
     *         s-address       [4] HostAddress -- sender's addr --,
     *         r-address       [5] HostAddress OPTIONAL -- recip's addr
     * }
     *
     * @param message
     * @return The {@link DERSequence};
     */
    private DERSequence encodePrivatePartSequence( EncKrbPrivPart message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, new DEROctetString( message.getUserData() ) ) );

        if ( message.getTimestamp() != null )
        {
            sequence.add( new DERTaggedObject( 1, KerberosTimeEncoder.encode( message.getTimestamp() ) ) );
        }

        if ( message.getMicroSecond() != -1 )
        {
            sequence.add( new DERTaggedObject( 2, DERInteger.valueOf( message.getMicroSecond() ) ) );
        }

        if ( message.getSequenceNumber() != -1 )
        {
            sequence.add( new DERTaggedObject( 3, DERInteger.valueOf( message.getSequenceNumber() ) ) );
        }

        sequence.add( new DERTaggedObject( 4, HostAddressesEncoder.encode( message.getSenderAddress() ) ) );

        if ( message.getRecipientAddress() != null )
        {
            sequence.add( new DERTaggedObject( 5, HostAddressesEncoder.encode( message.getRecipientAddress() ) ) );
        }

        return sequence;
    }
}
