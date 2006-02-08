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

import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;
import org.apache.kerberos.messages.value.PreAuthenticationData;

public class PreAuthenticationDataEncoder
{
    public static byte[] encode( PreAuthenticationData[] preAuth ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( encodeSequence( preAuth ) );
        aos.close();

        return baos.toByteArray();
    }

    protected static DERSequence encodeSequence( PreAuthenticationData[] preAuth )
    {
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < preAuth.length; ii++ )
        {
            sequence.add( encode( preAuth[ ii ] ) );
        }

        return sequence;
    }

    /**
     * PA-DATA ::=        SEQUENCE {
     *         padata-type[1]        INTEGER,
     *         padata-value[2]       OCTET STRING
     * }
     */
    protected static DERSequence encode( PreAuthenticationData preAuth )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( preAuth.getDataType().getOrdinal() ) ) );

        if ( preAuth.getDataValue() != null )
        {
            sequence.add( new DERTaggedObject( 2, new DEROctetString( preAuth.getDataValue() ) ) );
        }

        return sequence;
    }
}
