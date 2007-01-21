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

import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class PrivateMessageEncoder
{
    public byte[] encode( PrivateMessage message ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence privateMessageSequence = encodePrivateMessageSequence( message );
        aos
            .writeObject( DERApplicationSpecific
                .valueOf( message.getMessageType().getOrdinal(), privateMessageSequence ) );
        aos.close();

        return baos.toByteArray();
    }


    private DERSequence encodePrivateMessageSequence( PrivateMessage message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( message.getProtocolVersionNumber() ) ) );
        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( message.getMessageType().getOrdinal() ) ) );
        sequence.add( new DERTaggedObject( 3, EncryptedDataEncoder.encodeSequence( message.getEncryptedPart() ) ) );

        return sequence;
    }
}
