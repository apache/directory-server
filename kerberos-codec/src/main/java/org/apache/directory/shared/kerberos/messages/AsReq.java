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

import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.KdcReq;


/**
 * AS-REQ message. It's just a KDC-REQ message with a message type set to 10.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * AS-REQ          ::= [APPLICATION 10] <KDC-REQ>
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AsReq extends KdcReq
{
    // Storage for computed lengths
    private int kdcReqLength;


    /**
     * Creates a new instance of AS-REQ.
     */
    public AsReq()
    {
        super( KerberosMessageType.AS_REQ );
    }


    /**
     * Compute the AS-REQ length
     * <pre>
     * AS-REQ :
     * 
     * 0x6A L1 AS-REQ message
     *  |
     *  +-->  0x30 L2 KDC-REQ sequence
     * </pre>
     */
    public int computeLength()
    {
        kdcReqLength = super.computeLength();
        return 1 + TLV.getNbBytes( kdcReqLength ) + kdcReqLength;
    }


    /**
     * Encode the AS-REQ component
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

        // The AS-REQ SEQ Tag
        buffer.put( ( byte ) KerberosConstants.AS_REQ_TAG );
        buffer.put( TLV.getBytes( kdcReqLength ) );

        // The KDC-REQ --------------------------------------------------------
        super.encode( buffer );

        return buffer;
    }
}
