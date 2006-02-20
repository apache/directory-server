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
package org.apache.directory.server.kerberos.shared.io.encoder;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


public class EncryptedDataEncoder
{
    public static byte[] encode( EncryptedData encryptedData ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( encodeSequence( encryptedData ) );
        aos.close();

        return baos.toByteArray();
    }


    /**
     * EncryptedData ::=   SEQUENCE {
     *             etype[0]     INTEGER, -- EncryptionEngine
     *             kvno[1]      INTEGER OPTIONAL,
     *             cipher[2]    OCTET STRING -- ciphertext
     * }
     */
    public static DERSequence encodeSequence( EncryptedData encryptedData )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( encryptedData.getEncryptionType().getOrdinal() ) ) );

        if ( encryptedData.getKeyVersion() > 0 )
        {
            sequence.add( new DERTaggedObject( 1, DERInteger.valueOf( encryptedData.getKeyVersion() ) ) );
        }

        sequence.add( new DERTaggedObject( 2, new DEROctetString( encryptedData.getCipherText() ) ) );

        return sequence;
    }
}
