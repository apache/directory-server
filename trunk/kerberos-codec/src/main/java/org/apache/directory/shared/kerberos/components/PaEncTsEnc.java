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
package org.apache.directory.shared.kerberos.components;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The PaEncTsEnc structure is used to store a PA-ENC-TS-ENC associated to a type.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * PA-ENC-TS-ENC           ::= SEQUENCE {
 *         patimestamp     [0] KerberosTime -- client's time --,
 *         pausec          [1] Microseconds OPTIONAL
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PaEncTsEnc implements Asn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( PaEncTsEnc.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The patimestamp */
    private KerberosTime patimestamp;

    /** The pausec */
    private Integer pausec;

    // Storage for computed lengths
    private int paTimestampLength;
    private int paUsecLength;
    private int paEncTsEncLength;


    /**
     * Creates a new instance of PaEncTsEnc.
     */
    public PaEncTsEnc()
    {
    }


    /**
     * Creates a new instance of PaEncTsEnc.
     */
    public PaEncTsEnc( KerberosTime paTimestamp, int pausec )
    {
        this.patimestamp = paTimestamp;
        this.pausec = pausec;
    }


    /**
     * Returns the patimestamp value.
     *
     * @return The patimestamp value.
     */
    public KerberosTime getPaTimestamp()
    {
        return patimestamp;
    }


    /**
     * Set the patimestamp.
     *
     * @param patimestamp The patimestamp value
     */
    public void setPaTimestamp( KerberosTime patimestamp )
    {
        this.patimestamp = patimestamp;
    }


    /**
     * @return the pausec
     */
    public int getPausec()
    {
        if ( pausec == null )
        {
            return -1;
        }

        return pausec;
    }


    /**
     * @param pausec the pausec to set
     */
    public void setPausec( int pausec )
    {
        this.pausec = pausec;
    }


    /**
     * Compute the PA-ENC-TS-ENC length
     * <pre>
     * PA-ENC-TS-ENC :
     * 
     * 0x30 L1 PA-ENC-TS-ENC sequence
     *  |
     *  +--> 0xA0 0x11 patimestamp tag
     *  |     |
     *  |     +--> 0x18 0x0F patimestamp value (KerberosTime)
     *  |
     *  +--> 0xA1 L2 pausec tag
     *        |
     *        +--> 0x02 L2-1 pausec (INTEGER)
     *        
     *  </pre>
     */
    public int computeLength()
    {
        // The paTimestamp
        paTimestampLength = 0x11;

        paEncTsEncLength = 1 + TLV.getNbBytes( paTimestampLength ) + paTimestampLength;

        // The pausec, if any
        if ( pausec != null )
        {
            int pausecLength = BerValue.getNbBytes( pausec );
            paUsecLength = 1 + TLV.getNbBytes( pausecLength ) + pausecLength;
            paEncTsEncLength += 1 + TLV.getNbBytes( paUsecLength ) + paUsecLength;
        }

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( paEncTsEncLength ) + paEncTsEncLength;
    }


    /**
     * Encode the PA-ENC-TS-ENC message to a PDU. 
     * 
     * <pre>
     * PA-ENC-TS-ENC :
     * 
     * 0x30 LL
     *   0xA0 0x11 
     *     0x18 0x0F patimestamp
     *  [0xA1 LL 
     *     0x02 LL pausec]
     * </pre>
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The PA-ENC-TS-ENC SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( paEncTsEncLength ) );

            // The patimestamp, first the tag, then the value
            buffer.put( ( byte ) KerberosConstants.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG );
            buffer.put( ( byte ) 0x11 );

            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( patimestamp.getBytes() );

            // The pausec, first the tag, then the value, if any
            if ( pausec != null )
            {
                buffer.put( ( byte ) KerberosConstants.PA_ENC_TS_ENC_PA_USEC_TAG );
                buffer.put( TLV.getBytes( paUsecLength ) );
                BerValue.encode( buffer, pausec );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_140, 1 + TLV.getNbBytes( paEncTsEncLength ) + paEncTsEncLength,
                buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "Checksum encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "Checksum initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "PA-ENC-TS-ENC : {\n" );
        sb.append( tabs ).append( "    patimestamp : " ).append( patimestamp ).append( '\n' );

        if ( pausec != null )
        {
            sb.append( tabs + "    pausec :" ).append( pausec ).append( '\n' );
        }

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
