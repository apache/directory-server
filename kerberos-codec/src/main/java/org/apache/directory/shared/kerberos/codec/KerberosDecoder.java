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
package org.apache.directory.shared.kerberos.codec;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.encryptionKey.EncryptionKeyContainer;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosDecoder
{
    /**
     * Decode an EncryptionKey structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncryptionKey
     * @throws KerberosException If the decoding fails
     */
    public static EncryptionKey decodeEncryptionKey( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncryptionKey Container
        Asn1Container encryptionKeyContainer = new EncryptionKeyContainer();

        // Decode the EncryptionKey PDU
        try
        {
            Asn1Decoder.decode( stream, encryptionKeyContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncryptionKey
        return ( ( EncryptionKeyContainer ) encryptionKeyContainer ).getEncryptionKey();
    }
    
    
    /**
     * Decode an PrincipalName structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of PrincipalName
     * @throws KerberosException If the decoding fails
     */
    public static PrincipalName decodePrincipalName( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        try
        {
            Asn1Decoder.decode( stream, principalNameContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded PrincipalName
        return ( ( PrincipalNameContainer ) principalNameContainer ).getPrincipalName();
    }
}
