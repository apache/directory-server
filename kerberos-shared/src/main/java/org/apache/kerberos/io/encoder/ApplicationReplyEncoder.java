/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.io.encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.asn1.der.ASN1OutputStream;
import org.apache.asn1.der.DERApplicationSpecific;
import org.apache.asn1.der.DERInteger;
import org.apache.asn1.der.DERSequence;
import org.apache.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.application.ApplicationReply;

public class ApplicationReplyEncoder
{
    public static final int APPLICATION_CODE = 15;

    public byte[] encode( ApplicationReply reply ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        DERSequence replySequence = encodeReplySequence( reply );
        aos.writeObject( DERApplicationSpecific.valueOf( APPLICATION_CODE, replySequence ) );
        aos.close();

        return baos.toByteArray();
    }

    private DERSequence encodeReplySequence( ApplicationReply message )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( message.getProtocolVersionNumber() ) ) );
        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( message.getMessageType().getOrdinal() ) ) );
        sequence.add( new DERTaggedObject( 2, EncryptedDataEncoder.encodeSequence( message.getEncPart() ) ) );

        return sequence;
    }
}
