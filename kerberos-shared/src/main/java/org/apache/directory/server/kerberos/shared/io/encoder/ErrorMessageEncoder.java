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


public class ErrorMessageEncoder
{
    public void encode( ErrorMessage message, ByteBuffer out ) throws IOException
    {
        ASN1OutputStream aos = new ASN1OutputStream( out );

        DERSequence errorReply = encodeErrorMessageSequence( message );
        aos.writeObject( DERApplicationSpecific.valueOf( message.getMessageType().getOrdinal(), errorReply ) );

        aos.close();
    }


    public byte[] encode( ErrorMessage message ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence errorReply = encodeErrorMessageSequence( message );
        aos.writeObject( DERApplicationSpecific.valueOf( message.getMessageType().getOrdinal(), errorReply ) );

        aos.close();

        return baos.toByteArray();
    }


    private DERSequence encodeErrorMessageSequence( ErrorMessage message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( message.getProtocolVersionNumber() ) ) );

        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( message.getMessageType().getOrdinal() ) ) );

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
