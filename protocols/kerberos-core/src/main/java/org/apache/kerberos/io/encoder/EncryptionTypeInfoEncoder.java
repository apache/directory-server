/*
 *   Copyright 2005 The Apache Software Foundation
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
import org.apache.asn1.der.DERInteger;
import org.apache.asn1.der.DEROctetString;
import org.apache.asn1.der.DERSequence;
import org.apache.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.value.EncryptionTypeInfoEntry;

public class EncryptionTypeInfoEncoder
{
    public static byte[] encode( EncryptionTypeInfoEntry[] entries ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );
        aos.writeObject( encodeSequence( entries ) );
        aos.close();

        return baos.toByteArray();
    }

    /**
     * ETYPE-INFO              ::= SEQUENCE OF ETYPE-INFO-ENTRY
     */
    protected static DERSequence encodeSequence( EncryptionTypeInfoEntry[] entries )
    {
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < entries.length; ii++ )
        {
            sequence.add( encode( entries[ ii ] ) );
        }

        return sequence;
    }

    /**
     * ETYPE-INFO-ENTRY        ::= SEQUENCE {
     *     etype               [0] Int32,
     *     salt                [1] OCTET STRING OPTIONAL
     * }
     */
    protected static DERSequence encode( EncryptionTypeInfoEntry entry )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( entry.getEncryptionType().getOrdinal() ) ) );

        if ( entry.getSalt() != null )
        {
            sequence.add( new DERTaggedObject( 1, new DEROctetString( entry.getSalt() ) ) );
        }

        return sequence;
    }
}
