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

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERInteger;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncryptionKeyEncoder
{
    /**
     * Encodes an {@link EncryptionKey} into a byte array.
     *
     * @param key
     * @return The byte array.
     * @throws IOException
     */
    public static byte[] encode( EncryptionKey key ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( encodeSequence( key ) );
        aos.close();

        return baos.toByteArray();
    }


    protected static DERSequence encodeSequence( EncryptionKey key )
    {
        DERSequence vector = new DERSequence();

        vector.add( new DERTaggedObject( 0, DERInteger.valueOf( key.getKeyType().getOrdinal() ) ) );
        vector.add( new DERTaggedObject( 1, new DEROctetString( key.getKeyValue() ) ) );

        return vector;
    }
}
