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
package org.apache.directory.server.kerberos.shared.io.decoder;


import java.io.IOException;
import java.util.Enumeration;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionTypeInfo2Entry;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class EncryptionTypeInfo2Decoder
{
    /**
     * Decodes a byte array into an array of {@link EncryptionTypeInfo2Entry}.
     *
     * @param encodedEntries
     * @return The array of {@link EncryptionTypeInfo2Entry}.
     * @throws IOException
     */
    public EncryptionTypeInfo2Entry[] decode( byte[] encodedEntries ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedEntries );

        DERSequence sequence = ( DERSequence ) ais.readObject();

        return decodeSequence( sequence );
    }


    /**
     * ETYPE-INFO2             ::= SEQUENCE SIZE (1..MAX) OF ETYPE-INFO2-ENTRY
     */
    protected static EncryptionTypeInfo2Entry[] decodeSequence( DERSequence sequence )
    {
        EncryptionTypeInfo2Entry[] entrySequence = new EncryptionTypeInfo2Entry[sequence.size()];

        int ii = 0;
        for ( Enumeration<DERSequence> e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERSequence object = e.nextElement();
            entrySequence[ii] = decode( object );
            ii++;
        }

        return entrySequence;
    }


    /**
     * ETYPE-INFO2-ENTRY       ::= SEQUENCE {
     *         etype           [0] Int32,
     *         salt            [1] KerberosString OPTIONAL,
     *         s2kparams       [2] OCTET STRING OPTIONAL
     * }
     */
    protected static EncryptionTypeInfo2Entry decode( DERSequence sequence )
    {
        EncryptionType encryptionType = EncryptionType.NULL;
        String salt = new String();
        byte[] s2kparams = new byte[0];

        for ( Enumeration<DERTaggedObject> e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger tag0 = ( DERInteger ) derObject;
                    encryptionType = EncryptionType.getTypeByOrdinal( tag0.intValue() );
                    break;
                case 1:
                    DERGeneralString tag1 = ( DERGeneralString ) derObject;
                    salt = tag1.getString();
                    break;
                case 2:
                    DEROctetString tag2 = ( DEROctetString ) derObject;
                    s2kparams = tag2.getOctets();
                    break;
            }
        }

        return new EncryptionTypeInfo2Entry( encryptionType, salt, s2kparams );
    }
}
