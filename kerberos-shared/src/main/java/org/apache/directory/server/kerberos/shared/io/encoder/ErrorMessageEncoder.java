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
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ErrorMessageEncoder
{
    /**
     * Encodes an {@link ErrorMessage} into a {@link ByteBuffer}.
     *
     * @param message
     * @param out
     * @throws IOException
     */
    public void encode( ErrorMessage message, ByteBuffer out ) throws IOException
    {
        ASN1OutputStream aos = new ASN1OutputStream( out );

        DERSequence errorReply = encodeErrorMessageSequence( message );
        aos.writeObject( DERApplicationSpecific.valueOf( message.getMessageType().getValue(), errorReply ) );

        aos.close();
    }


    /**
     * Encodes an {@link ErrorMessage} into a byte array.
     *
     * 0x7E L1
     *  |
     *  +--> 0x30 L2
     *        |
     *        +--> 0xA0 0x03
     *        |       |
     *        |       +--> 0x02 0x01 pvno (integer)
     *        |
     *        +--> 0xA1 0x03
     *        |       |
     *        |       +--> 0x02 0x01 messageType (integer)
     *        |
     *       [+--> 0xA2 0x11
     *        |       |
     *        |       +--> 0x18 0x0F ctime (KerberosTime, optionnal)]
     *        |
     *       [+--> 0xA3 L3
     *        |       |
     *        |       +--> 0x02 L3-1 cusec (integer, optionnal)]
     *        |
     *        +--> 0xA4 L4
     *        |       |
     *        |       +--> 0x18 L4-1 stime (KerberosTime)
     *        |
     *        +--> 0xA5 L5
     *        |       |
     *        |       +--> 0x02 L5-1 susec (integer)
     *        |
     *        +--> 0xA6 L6
     *        |       |
     *        |       +--> 0x02 L6-1 error-code (integer) 
     *        |
     *       [+--> 0xA7 L7
     *        |       | 
     *        |       +--> 0x1B L7-1 crealm (String, optionnal)]
     *        | 
     *       [+--> 0xA8 L8
     *        |       | 
     *        |       +--> 0x1B L8-1 cname (String, optionnal)]
     *        |
     *        +--> 0xA9 L9
     *        |       |
     *        |       +--> 0x1B L9-1 realm (String)
     *        |
     *        +--> 0xAA L10
     *        |       |
     *        |       +--> 0x1B L10-1 sname (String)
     *        |
     *       [+--> 0xAB L11
     *        |       |
     *        |       +--> 0x1B L11-1 e-text (String, optionnal)]
     *        |
     *       [+--> 0xAC L12
     *        |
     *        +--> 0x04 L12-1 e-data (OCTET-STRING, optionnal)]
     *        
     * @param message
     * @return The byte array.
     * @throws IOException
     */
    public byte[] encode( ErrorMessage message ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence errorReply = encodeErrorMessageSequence( message );
        aos.writeObject( DERApplicationSpecific.valueOf( message.getMessageType().getValue(), errorReply ) );

        aos.close();

        return baos.toByteArray();
    }


    private DERSequence encodeErrorMessageSequence( ErrorMessage message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( message.getProtocolVersionNumber() ) ) );

        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( message.getMessageType().getValue() ) ) );

        if ( message.getClientTime() != null )
        {
            sequence.add( new DERTaggedObject( 2, KerberosTimeEncoder.encode( message.getClientTime() ) ) );
        }

        if ( message.getClientMicroSecond() != null )
        {
            sequence.add( new DERTaggedObject( 3, DERInteger.valueOf( message.getClientMicroSecond().intValue() ) ) );
        }

        sequence.add( new DERTaggedObject( 4, KerberosTimeEncoder.encode( message.getServerTime() ) ) );

        sequence.add( new DERTaggedObject( 5, DERInteger.valueOf( message.getServerMicroSecond() ) ) );

        sequence.add( new DERTaggedObject( 6, DERInteger.valueOf( message.getErrorCode() ) ) );

        if ( message.getClientPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 7, DERGeneralString.valueOf( message.getClientPrincipal().getRealm()
                .toString() ) ) );
        }

        if ( message.getClientPrincipal() != null )
        {
            sequence.add( new DERTaggedObject( 8, PrincipalNameEncoder.encode( message.getClientPrincipal() ) ) );
        }

        sequence.add( new DERTaggedObject( 9, DERGeneralString.valueOf( message.getServerPrincipal().getRealm() ) ) );

        sequence.add( new DERTaggedObject( 10, PrincipalNameEncoder.encode( message.getServerPrincipal() ) ) );

        if ( message.getExplanatoryText() != null )
        {
            sequence.add( new DERTaggedObject( 11, DERGeneralString.valueOf( message.getExplanatoryText() ) ) );
        }

        if ( message.getExplanatoryData() != null )
        {
            sequence.add( new DERTaggedObject( 12, new DEROctetString( message.getExplanatoryData() ) ) );
        }

        return sequence;
    }
}
