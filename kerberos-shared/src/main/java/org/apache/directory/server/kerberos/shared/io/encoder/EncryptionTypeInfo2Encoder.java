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

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionTypeInfo2Entry;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-21 17:00:43 -0700 (Mon, 21 May 2007) $
 */
public class EncryptionTypeInfo2Encoder
{
    /**
     * Encodes an array of {@link EncryptionTypeInfo2Entry}s into a byte array.
     *
     * @param entries
     * @return The byte array.
     * @throws IOException
     */
    public static byte[] encode( EncryptionTypeInfo2Entry[] entries ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );
        aos.writeObject( encodeSequence( entries ) );
        aos.close();

        return baos.toByteArray();
    }


    /**
     * ETYPE-INFO2             ::= SEQUENCE SIZE (1..MAX) OF ETYPE-INFO2-ENTRY
     */
    protected static DERSequence encodeSequence( EncryptionTypeInfo2Entry[] entries )
    {
        DERSequence sequence = new DERSequence();

        for ( int ii = 0; ii < entries.length; ii++ )
        {
            sequence.add( encode( entries[ii] ) );
        }

        return sequence;
    }


    /**
     * ETYPE-INFO2-ENTRY       ::= SEQUENCE {
     *         etype           [0] Int32,
     *         salt            [1] KerberosString OPTIONAL,
     *         s2kparams       [2] OCTET STRING OPTIONAL
     * }
     */
    protected static DERSequence encode( EncryptionTypeInfo2Entry entry )
    {
        DERSequence sequence = new DERSequence();

        sequence.add( new DERTaggedObject( 0, DERInteger.valueOf( entry.getEncryptionType().getOrdinal() ) ) );

        if ( entry.getSalt() != null )
        {
            sequence.add( new DERTaggedObject( 1, DERGeneralString.valueOf( entry.getSalt() ) ) );
        }

        if ( entry.getS2kParams() != null )
        {
            sequence.add( new DERTaggedObject( 2, new DEROctetString( entry.getS2kParams() ) ) );
        }

        return sequence;
    }
}
