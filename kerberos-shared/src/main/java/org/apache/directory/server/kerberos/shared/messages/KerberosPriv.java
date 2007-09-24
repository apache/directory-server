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

import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KRB-PRIV message. The ASN.1 grammar is the following :
 * 
 * KRB-PRIV        ::= [APPLICATION 21] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (21),
 *                         -- NOTE: there is no [2] tag
 *         enc-part        [3] EncryptedData -- EncKrbPrivPart
 * }
 *  
 * pvno and msg-type are inherited from KerberosMessage
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class KerberosPriv extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KerberosPriv.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The encrypted data part */
    private EncryptedData encPart;

    // Storage for computed lengths
    private transient int encPartTagLength = 0;
    
    private transient int kerberosPrivSeqLength;
    private transient int kerberosPrivApplLength;
    
    
    
    /**
     * Creates a new instance of KerberosPriv.
     */
    public KerberosPriv()
    {
        super( MessageType.KRB_PRIV );
        encPart = null;
    }

    /**
     * @return The encrypted part
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }

    /**
     * Set the encrypted part
     * @param encPart The encrypted part
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart;
    }
    
    /**
     * Return the length of a Kerberos Priv message .
     * 
     * 0x75 L1
     *  |
     *  +--> 0x30 L2
     *        |
     *        +--> 0xA0 0x03
     *        |     |
     *        |     +--> 0x02 0x01 pvno (integer)
     *        |
     *        +--> 0xA1 0x03
     *        |     |
     *        |     +--> 0x02 0x01 msg-type (integer)
     *        |
     *        +--> 0xA3 L3
     *              | 
     *              +--> 0x02 L3-1 enc-part (EncryptedData)
     */
    public int computeLength()
    {
        // First compute the KerberosMessage length
        kerberosPrivSeqLength = super.computeLength();
        
        // The encrypted data length
        if ( encPart == null )
        {
            return -1;
        }
        
        encPartTagLength = encPart.computeLength();
        
        kerberosPrivSeqLength += 
            1 + TLV.getNbBytes( encPartTagLength ) + encPartTagLength;


        kerberosPrivApplLength = 1 + TLV.getNbBytes( kerberosPrivSeqLength ) + kerberosPrivSeqLength;
        return 1 + TLV.getNbBytes( kerberosPrivApplLength ) + kerberosPrivApplLength;
    }
    
    /**
     * Encode the KerberosPriv message to a PDU. 
     * 
     * KRB-PRIV :
     * 
     * 0x75 LL
     *   0x30 LL
     *     0xA0 LL pvno 
     *     0xA1 LL msg-type
     *     0xA3 LL enc-part
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            int bufferCapacity = computeLength();
            
            if ( bufferCapacity == -1 )
            {
                log.error( "Cannot compute the buffer size" );
                throw new EncoderException( "Cannot compute the buffer size" );
            }
            buffer = ByteBuffer.allocate( bufferCapacity );
        }

        try
        {
            // The KerberosPriv APPLICATION Tag
            buffer.put( (byte)0x75 );
            buffer.put( TLV.getBytes( kerberosPrivApplLength ) );

            // The KerberosPriv SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( kerberosPrivSeqLength ) );

            // The pvno Tag and value
            super.encode(  buffer );

            // Encrypted Data encoding
            buffer.put( ( byte )0xA3 );
            buffer.put( TLV.getBytes( encPartTagLength ) );
            
            if ( encPart != null )
            {
                encPart.encode( buffer );
            }
            else
            {
                log.error( "Null Encrypted Data part" );
                throw new EncoderException( "The encrypted Data must not be null" );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the KRB-PRIV object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( kerberosPrivApplLength ) + kerberosPrivApplLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KRB-PRIV encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KRB-PRIV initial value : {}", toString() );
        }

        return buffer;
    }
}
