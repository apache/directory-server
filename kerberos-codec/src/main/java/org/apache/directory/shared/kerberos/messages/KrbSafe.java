/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.messages;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.components.KrbSafeBody;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing KRB-SAFE message
 * 
 * <pre>
 * KRB-SAFE        ::= [APPLICATION 20] SEQUENCE {
 *      pvno            [0] INTEGER (5),
 *      msg-type        [1] INTEGER (20),
 *      safe-body       [2] KRB-SAFE-BODY,
 *      cksum           [3] Checksum
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbSafe extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbError.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** body of this message */
    private KrbSafeBody krbSafeBody;

    /** the checksum */
    private Checksum checksum;

    // Storage for computed lengths
    private int pvnoLen;
    private int msgTypeLength;
    private int krbSafeBodyLen;
    private int checksumLen;
    private int krbSafeSeqLen;
    private int krbSafeLen;


    /**
     * Creates a new instance of KrbSafe.
     */
    public KrbSafe()
    {
        super( KerberosMessageType.KRB_SAFE );
    }


    /**
     * @return the krbSafeBody
     */
    public KrbSafeBody getSafeBody()
    {
        return krbSafeBody;
    }


    /**
     * @param safeBody the KrbSafeBody to set
     */
    public void setSafeBody( KrbSafeBody safeBody )
    {
        this.krbSafeBody = safeBody;
    }


    /**
     * @return the checksum
     */
    public Checksum getChecksum()
    {
        return checksum;
    }


    /**
     * @param checksum the checksum to set
     */
    public void setChecksum( Checksum checksum )
    {
        this.checksum = checksum;
    }

    
    /**
     * Compute the KRB-SAFE length
     * <pre>
     * KRB-SAFE :
     * 
     * 0x74 L1 KRB-SAFE APPLICATION[20]
     *  |
     *  +--> 0x30 L2 KRB-ERROR sequence
     *        |
     *        +--> 0xA0 0x03 pvno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno (5)
     *        |
     *        +--> 0xA1 0x03 msg-type tag
     *        |     |
     *        |     +--> 0x02 0x01 0x14 msg-type (20)
     *        |     
     *        +--> 0xA2 L3 safe-body tag
     *        |     |
     *        |     +--> 0x30 L3-1 safe-body (KRB-SAFE-BODY)
     *        |
     *        +--> 0xA3 L4 cksum tag
     *              |
     *              +--> 0x30 L4-1 cksum (CHECKSUM)
     * </pre>
     */
    @Override
    public int computeLength()
    {
        pvnoLen = 1 + 1 + 1;
        krbSafeSeqLen = 1 + TLV.getNbBytes( pvnoLen ) + pvnoLen;

        msgTypeLength = 1 + 1 + Value.getNbBytes( getMessageType().getValue() );
        krbSafeSeqLen += 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;

        krbSafeBodyLen = krbSafeBody.computeLength();
        krbSafeSeqLen += 1 + TLV.getNbBytes( krbSafeBodyLen ) + krbSafeBodyLen;

        checksumLen = checksum.computeLength();
        krbSafeSeqLen += 1 + TLV.getNbBytes( checksumLen ) + checksumLen;

        krbSafeLen = 1 + TLV.getNbBytes( krbSafeSeqLen ) + krbSafeSeqLen;

        return 1 + TLV.getNbBytes( krbSafeLen ) + krbSafeLen;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The KRB-SAFE APPLICATION tag
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_TAG );
            buffer.put( TLV.getBytes( krbSafeLen ) );

            // The KRB-SAFE sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( krbSafeSeqLen ) );

            // pvno tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLen ) );
            Value.encode( buffer, getProtocolVersionNumber() );

            // msg-type tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_MSGTYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLength ) );
            Value.encode( buffer, getMessageType().getValue() );

            // safe-body tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_SAFE_BODY_TAG );
            buffer.put( TLV.getBytes( krbSafeBodyLen ) );
            krbSafeBody.encode( buffer );

            // cksum tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_CKSUM_TAG );
            buffer.put( TLV.getBytes( checksumLen ) );
            checksum.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_736_CANNOT_ENCODE_KRBSAFE, 1 + TLV.getNbBytes( krbSafeLen )
                + krbSafeLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbSafe encoding : {}", Strings.dumpBytes(buffer.array()) );
            log.debug( "KrbSafe initial value : {}", toString() );
        }

        return buffer;
    }
    
    


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KRB-SAFE : {\n" );
        sb.append( "    pvno: " ).append( getProtocolVersionNumber() ).append( '\n' );
        sb.append( "    msgType: " ).append( getMessageType() ).append( '\n' );

        if ( krbSafeBody != null )
        {
            sb.append( "    safe-body: " ).append( krbSafeBody ).append( '\n' );
        }

        if ( checksum != null )
        {
            sb.append( "    cusec: " ).append( checksum ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
