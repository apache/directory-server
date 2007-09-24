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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Pre-authentication encrypted timestamp.
 * 
 * The ASN.1 grammar is the following :
 * 
 * PA-ENC-TIMESTAMP        ::= EncryptedData -- PA-ENC-TS-ENC
 * 
 * PA-ENC-TS-ENC           ::= SEQUENCE {
 *        patimestamp     [0] KerberosTime -- client's time --,
 *        pausec          [1] Microseconds OPTIONAL
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class PreAuthEncryptedTimestamp extends AbstractAsn1Object implements Encodable
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( PreAuthEncryptedTimestamp.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** client's time */
    private KerberosTime paTimestamp;
    
    /** Client's microseconds */
    private int pausec; //optional

    // Storage for computed lengths
    private transient int preAuthEncryptedTimestampSeqLength;
    
    private transient int paTimestampTagLength;
    private transient int paTimestampLength;
    
    private transient int pausecTagLength;

    
    /**
     * Creates a new instance of EncryptedTimeStamp.
     *
     * @param timeStamp
     * @param microSeconds
     */
    public PreAuthEncryptedTimestamp( KerberosTime paTimestamp, int pausec )
    {
        this.paTimestamp = paTimestamp;
        this.pausec = pausec;
    }


    /**
     * Returns the {@link KerberosTime}.
     *
     * @return The {@link KerberosTime}.
     */
    public KerberosTime getTimeStamp()
    {
        return paTimestamp;
    }

    /**
     * Set the client timestamp
     * @param paTimestamp The client timestamp
     */
    public void setTimestamp( KerberosTime paTimestamp )
    {
        this.paTimestamp = paTimestamp;
    }


    /**
     * Returns the microseconds.
     *
     * @return The microseconds.
     */
    public int getMicroSeconds()
    {
        return pausec;
    }


    /**
     * Set the client microseconds
     * @param pausec The client microseconds
     */
    public void setPausec( int pausec )
    {
        this.pausec = pausec;
    }
    
    /**
     * Compute the PA-ENC-TS-ENC length
     * 
     * PA-ENC-TS-ENC :
     * 
     * 0x30 L1 PA-ENC-TS-ENC Seq
     *  |
     *  +--> 0xA0 L2 apatimestamp tag
     *  |     |
     *  |     +--> 0x18 L2-1 patimestamp (KerberosTime)
     *  |
     *  +--> [0xA1 L3 pausec tag
     *        |
     *        +--> 0x1B L3-1 pausec (optional)]
     */
    public int computeLength()
    {
        preAuthEncryptedTimestampSeqLength = 0;

        // Compute the patimestamp length
        paTimestampLength = 15; 
        paTimestampTagLength = 1 + 1 + paTimestampLength;
        
        preAuthEncryptedTimestampSeqLength = 1 + TLV.getNbBytes( paTimestampTagLength ) + paTimestampTagLength;

        // Compute the pausec length, if any
        if ( pausec != KerberosUtils.NULL )
        {
            int pausecLength = Value.getNbBytes( pausec );
            pausecTagLength = 1 + TLV.getNbBytes( pausecLength ) + pausecLength;
            preAuthEncryptedTimestampSeqLength += 1 + TLV.getNbBytes( pausecTagLength ) + pausecTagLength;
        }

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( preAuthEncryptedTimestampSeqLength ) + preAuthEncryptedTimestampSeqLength;
    }
    
    /**
     * Encode the PA-ENC-TS-ENC message to a PDU. 
     * 
     * PA-ENC-TS-ENC :
     * 
     * 0x30 LL
     *   0xA0 0x11 
     *     0x18 0x0F (KerberosTime)
     *   [0xA1 LL
     *     0x02 LL pausec (int)] (optional)
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The PA-ENC-TS-ENC SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( preAuthEncryptedTimestampSeqLength ) );

            // The patimestamp encoding, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( paTimestampTagLength ) );
            buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
            buffer.put( TLV.getBytes( paTimestampLength ) );
            buffer.put( StringTools.getBytesUtf8( paTimestamp.toString() ) );
            
            // Client millisecond encoding, if any
            if ( pausec != KerberosUtils.NULL )
            {
                buffer.put( ( byte )0xA1 );
                buffer.put( TLV.getBytes( pausecTagLength ) );
                Value.encode( buffer, pausec );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the PA-ENC-TS-ENC object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( preAuthEncryptedTimestampSeqLength ) + preAuthEncryptedTimestampSeqLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "PA-ENC-TS-ENC encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "PA-ENC-TS-ENC initial value : {}", toString() );
        }

        return buffer;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "NYI";
    }
    
}
