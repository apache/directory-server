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
package org.apache.directory.server.kerberos.shared.messages;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosRequestBody;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AS-REQ message.
 * 
 * The ASN.1 grammar is the following :
 * 
 * AS-REQ          ::= [APPLICATION 10] KDC-REQ
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class AuthServerRequest extends KdcRequest
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AuthServerRequest.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    // Storage for computed lengths
    private transient int asReqAppLength;
    
    /**
     * Creates a new instance of AuthServerRequest.
     *
     * @param paData
     * @param clientPrincipal
     * @param ticket
     * @param encPart
     */
    public AuthServerRequest( List<PreAuthenticationData> paData, KerberosRequestBody requestBody )
    {
        super( MessageType.KRB_AS_REQ, paData, requestBody );
    }
    
    /**
     * Return the length of a AS-REQ message .
     * 
     * 0x6A L1
     *  |
     *  +--> 0x30 L2 KDC-REQ
     */
    public int computeLength()
    {
        // Compute the KDC-REQ length
        asReqAppLength = super.computeLength();
        
        return 1 + TLV.getNbBytes( asReqAppLength ) + asReqAppLength;
    }

    /**
     * Encode the AS-REQ message to a PDU. 
     * 
     * AS-REQ :
     * 
     * 0x6A LL
     *   0x30 LL KDC-REQ 
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        try
        {
            // The AS-REQ application Tag
            buffer.put( (byte)0x6A );
            buffer.put( TLV.getBytes( asReqAppLength ) );
            
            // Encode the requestBody
            super.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the AS-REQ object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( asReqAppLength ) + asReqAppLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "AS-REQ encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AS-REQ initial value : {}", toString() );
        }

        return buffer;
    }

    /**
     * @see Object#toString()
     */
public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "AS-REQ\n" );
        sb.append( super.toString( tabs + "    " ) );
        
        return sb.toString();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
