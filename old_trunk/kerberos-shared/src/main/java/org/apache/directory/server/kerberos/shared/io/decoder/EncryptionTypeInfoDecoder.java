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
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionTypeInfoEntry;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class EncryptionTypeInfoDecoder
{
    /**
     * Decodes a byte array into an array of {@link EncryptionTypeInfoEntry}.
     *
     * @param encodedEntries
     * @return The array of {@link EncryptionTypeInfoEntry}.
     * @throws IOException
     */
    public EncryptionTypeInfoEntry[] decode( byte[] encodedEntries ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedEntries );

        DERSequence sequence = ( DERSequence ) ais.readObject();

        return decodeSequence( sequence );
    }


    /**
     * ETYPE-INFO              ::= SEQUENCE OF ETYPE-INFO-ENTRY
     */
    protected static EncryptionTypeInfoEntry[] decodeSequence( DERSequence sequence )
    {
        EncryptionTypeInfoEntry[] entrySequence = new EncryptionTypeInfoEntry[sequence.size()];

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
     * ETYPE-INFO-ENTRY        ::= SEQUENCE {
     *     etype               [0] Int32,
     *     salt                [1] OCTET STRING OPTIONAL
     * }
     */
    protected static EncryptionTypeInfoEntry decode( DERSequence sequence )
    {
        EncryptionType encryptionType = EncryptionType.NULL;
        byte[] salt = new byte[0];

        for ( Enumeration<DERTaggedObject> e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = e.nextElement();
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();

            switch ( tag )
            {
                case 0:
                    DERInteger type = ( DERInteger ) derObject;
                    encryptionType = EncryptionType.getTypeByOrdinal( type.intValue() );
                    break;
                case 1:
                    DEROctetString value = ( DEROctetString ) derObject;
                    salt = value.getOctets();
                    break;
            }
        }

        return new EncryptionTypeInfoEntry( encryptionType, salt );
    }
}
