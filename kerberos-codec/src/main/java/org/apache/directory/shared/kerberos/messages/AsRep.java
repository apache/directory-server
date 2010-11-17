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

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.KdcRep;


/**
 * AS-REQ message. It's just a KDC-REP message with a message type set to 11.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * AS-REP          ::= [APPLICATION 11] <KDC-REP>
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AsRep extends KdcRep
{
    // Storage for computed lengths
    private transient int kdcRepLength;
    private transient int asRepLength;

    /**
     * Creates a new instance of AS-REP.
     */
    public AsRep() 
    {
        super( KerberosMessageType.AS_REP );
    }

    
    /**
     * Compute the AS-REP length
     * <pre>
     * AS-REP :
     * 
     * 0x6B L1 AS-REP message
     *  |
     *  +-->  0x30 L2 KDC-REP sequence
     * </pre>
     */
    public int computeLength()
    {
        kdcRepLength = super.computeLength();
        asRepLength = 1 + TLV.getNbBytes( kdcRepLength ) + kdcRepLength;
        
        return asRepLength;
    }
    
    
    /**
     * Encode the AS-REP component
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
        
        // The AS-REP SEQ Tag
        buffer.put( (byte)KerberosConstants.AS_REP_TAG );
        buffer.put( TLV.getBytes( kdcRepLength ) );
        
        // The KDC-REP --------------------------------------------------------
        super.encode( buffer );
        
        return buffer;
    }
}
