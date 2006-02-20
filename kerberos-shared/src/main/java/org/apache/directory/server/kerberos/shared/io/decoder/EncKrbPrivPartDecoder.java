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
package org.apache.directory.server.kerberos.shared.io.decoder;


import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPartModifier;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DERApplicationSpecific;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralizedTime;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class EncKrbPrivPartDecoder implements Decoder, DecoderFactory
{
    public Decoder getDecoder()
    {
        return new EncKrbPrivPartDecoder();
    }


    public Encodable decode( byte[] encodedPrivatePart ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedPrivatePart );

        DERApplicationSpecific app = ( DERApplicationSpecific ) ais.readObject();

        DERSequence privatePart = ( DERSequence ) app.getObject();

        return decodePrivatePartSequence( privatePart );
    }


    private EncKrbPrivPart decodePrivatePartSequence( DERSequence sequence )
    {
        EncKrbPrivPartModifier modifier = new EncKrbPrivPartModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( DERTaggedObject ) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DEROctetString tag0 = ( DEROctetString ) derObject;
                    modifier.setUserData( tag0.getOctets() );
                    break;
                case 1:
                    DERGeneralizedTime tag1 = ( DERGeneralizedTime ) derObject;
                    modifier.setTimestamp( KerberosTimeDecoder.decode( tag1 ) );
                    break;
                case 2:
                    DERInteger tag2 = ( DERInteger ) derObject;
                    modifier.setMicroSecond( new Integer( tag2.intValue() ) );
                    break;
                case 3:
                    DERInteger tag3 = ( DERInteger ) derObject;
                    modifier.setSequenceNumber( new Integer( tag3.intValue() ) );
                    break;
                case 4:
                    DERSequence tag4 = ( DERSequence ) derObject;
                    modifier.setSenderAddress( HostAddressDecoder.decode( tag4 ) );
                    break;
                case 5:
                    DERSequence tag5 = ( DERSequence ) derObject;
                    modifier.setRecipientAddress( HostAddressDecoder.decode( tag5 ) );
                    break;
            }
        }
        return modifier.getEncKrbPrivPart();
    }
}
