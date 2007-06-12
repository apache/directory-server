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
package org.apache.directory.server.changepw.io;


import java.io.IOException;
import java.nio.ByteBuffer;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.value.ChangePasswordData;
import org.apache.directory.server.kerberos.shared.io.encoder.PrincipalNameEncoder;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.shared.asn1.der.ASN1OutputStream;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePasswordDataEncoder
{
    /**
     * Encodes a {@link ChangePasswordData} into a {@link ByteBuffer}.
     *
     * @param data
     * @param out
     * @throws IOException
     */
    public void encode( ChangePasswordData data, ByteBuffer out ) throws IOException
    {
        ASN1OutputStream aos = new ASN1OutputStream( out );

        DERSequence sequence = encodeDataSequence( data );
        aos.writeObject( sequence );

        aos.close();
    }


    private DERSequence encodeDataSequence( ChangePasswordData data )
    {
        DERSequence sequence = new DERSequence();
        sequence.add( new DERTaggedObject( 0, new DEROctetString( data.getPassword() ) ) );

        PrincipalName name = data.getPrincipalName();
        KerberosPrincipal principal = new KerberosPrincipal( name.getNameComponent(), name.getNameType() );
        sequence.add( new DERTaggedObject( 1, PrincipalNameEncoder.encode( principal ) ) );

        sequence.add( new DERTaggedObject( 2, DERGeneralString.valueOf( data.getRealm() ) ) );

        return sequence;
    }
}
