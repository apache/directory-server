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
package org.apache.kerberos.io.decoder;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;
import org.apache.kerberos.crypto.encryption.EncryptionType;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptedDataModifier;

public class EncryptedDataDecoder
{
    public static EncryptedData decode( byte[] encodedEncryptedData ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedEncryptedData );

        DERSequence sequence = (DERSequence) ais.readObject();

        return decode( sequence );
    }
	
	/**
	 * EncryptedData ::=   SEQUENCE {
	 *             etype[0]     INTEGER, -- EncryptionEngine
	 *             kvno[1]      INTEGER OPTIONAL,
	 *             cipher[2]    OCTET STRING -- ciphertext
	 * }
	 */
	public static EncryptedData decode( DERSequence sequence )
    {
        EncryptedDataModifier modifier = new EncryptedDataModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = (DERTaggedObject) e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger etype = (DERInteger) derObject;
                    modifier.setEncryptionType( EncryptionType.getTypeByOrdinal( etype.intValue() ) );
                    break;
                case 1:
                    DERInteger kvno = (DERInteger) derObject;
                    modifier.setKeyVersion( kvno.intValue() );
                    break;
                case 2:
                    DEROctetString cipher = (DEROctetString) derObject;
                    modifier.setCipherText( cipher.getOctets() );
                    break;
            }
        }

        return modifier.getEncryptedData();
    }
}
