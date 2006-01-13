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
package org.apache.kerberos.io.decoder;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.asn1.der.ASN1InputStream;
import org.apache.asn1.der.DERApplicationSpecific;
import org.apache.asn1.der.DEREncodable;
import org.apache.asn1.der.DERInteger;
import org.apache.asn1.der.DERSequence;
import org.apache.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.MessageType;
import org.apache.kerberos.messages.application.PrivateMessage;

public class PrivateMessageDecoder
{
    public PrivateMessage decode( byte[] encodedPrivateMessage ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedPrivateMessage );

        DERApplicationSpecific app = (DERApplicationSpecific) ais.readObject();

        DERSequence privateMessage = (DERSequence) app.getObject();

        return decodePrivateMessageSequence( privateMessage );
    }

    private PrivateMessage decodePrivateMessageSequence( DERSequence sequence )
    {
        PrivateMessage message = new PrivateMessage();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = (DERTaggedObject) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = (DERInteger) derObject;
                    message.setProtocolVersionNumber( tag0.intValue() );
                    break;
                case 1:
                    DERInteger tag1 = (DERInteger) derObject;
                    message.setMessageType( MessageType.getTypeByOrdinal( tag1.intValue() ) );
                    break;
                case 3:
                    DERSequence tag3 = (DERSequence) derObject;
                    message.setEncryptedPart( EncryptedDataDecoder.decode( tag3 ) );
                    break;
            }
        }

        return message;
    }
}
