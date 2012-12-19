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
package org.apache.directory.shared.kerberos.messages;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;


/**
 * EncASRepPart message. 
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * EncASRepPart    ::= [APPLICATION 25] EncKDCRepPart
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncAsRepPart extends KerberosMessage
{
    /** The EncKdcRepPart */
    private EncKdcRepPart encKdcRepPart;

    // Storage for computed lengths
    private int encKdcRepPartLength;


    /**
     * Creates a new instance of EncAsRepPart.
     */
    public EncAsRepPart()
    {
        super( KerberosMessageType.ENC_AS_REP_PART );
    }


    /**
     * @return the encKdcRepPart
     */
    public EncKdcRepPart getEncKdcRepPart()
    {
        return encKdcRepPart;
    }


    /**
     * @param encKdcRepPart the encKdcRepPart to set
     */
    public void setEncKdcRepPart( EncKdcRepPart encKdcRepPart )
    {
        this.encKdcRepPart = encKdcRepPart;
    }


    /**
     * Compute the EncAsRepPart length
     * <pre>
     * EncAsRepPart :
     * 
     * 0x79 L1 EncAsRepPart message
     *  |
     *  +-->  0x30 L2 EncKdcRepPart sequence
     * </pre>
     */
    public int computeLength()
    {
        encKdcRepPartLength = encKdcRepPart.computeLength();
        return 1 + TLV.getNbBytes( encKdcRepPartLength ) + encKdcRepPartLength;
    }


    /**
     * Encode the EncAsRepPart component
     * 
     * @param buffer The buffer containing the encoded result
     * @return The encoded component
     * @throws EncoderException If the encoding failed
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        // The EncAsRepPart Tag
        buffer.put( ( byte ) KerberosConstants.ENC_AS_REP_PART_TAG );
        buffer.put( TLV.getBytes( encKdcRepPartLength ) );

        // The EncKdcRepPart --------------------------------------------------------
        encKdcRepPart.encode( buffer );

        return buffer;
    }
}
