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

import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.KdcReq;


/**
 * TGS-REQ message. It's just a KDC-REQ message with a message type set to 12.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * TGS-REQ         ::= [APPLICATION 12] <KDC-REQ>
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TgsReq extends KdcReq
{
    // Storage for computed lengths
    private int kdcReqLength;
    private int tgsReqLength;

    /**
     * Creates a new instance of TGS-REQ.
     */
    public TgsReq()
    {
        super( KerberosMessageType.TGS_REQ );
    }

    
    /**
     * Compute the TGS-REQ length
     * <pre>
     * TGS-REQ :
     * 
     * 0x6A L1 TGS-REQ message
     *  |
     *  +-->  0x30 L2 KDC-REQ sequence
     * </pre>
     */
    public int computeLength()
    {
        kdcReqLength = 0;
        tgsReqLength = 0;
        
        kdcReqLength = super.computeLength();
        tgsReqLength = 1 + TLV.getNbBytes( kdcReqLength ) + kdcReqLength;
        
        return tgsReqLength;
    }
    
    
    /**
     * Encode the TGS-REQ component
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
        
        // The TGS-REQ SEQ Tag
        buffer.put( (byte)KerberosConstants.TGS_REQ_TAG );
        buffer.put( TLV.getBytes( kdcReqLength ) );
        
        // The KDC-REQ --------------------------------------------------------
        super.encode( buffer );
        
        return buffer;
    }
}
