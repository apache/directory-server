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
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing KRB-PRIV message
 * 
 * <pre>
 * KRB-PRIV        ::= [APPLICATION 21] SEQUENCE {
 *      pvno            [0] INTEGER (5),
 *      msg-type        [1] INTEGER (21),
 *                      -- NOTE: there is no [2] tag
 *      enc-part        [3] EncryptedData -- EncKrbPrivPart
 * }
 * </pre
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbPriv extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbError.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the encrypted EncKrbPrivPart component */
    private EncryptedData encPart;

    // Storage for computed lengths
    private int pvnoLen;
    private int msgTypeLength;
    private int encPartLen;
    private int krbPrivSeqLen;
    private int krbPrivLen;


    /**
     * Creates a new instance of KrbPriv.
     */
    public KrbPriv()
    {
        super( 5, KerberosMessageType.KRB_PRIV );
    }


    /**
     * @return the encPart
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }


    /**
     * @param encPart the encPart to set
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart;
    }


    /**
     * Compute the KRB-PRIV length
     * <pre>
     * KRB-PRIV :
     * 
     * 0x75 L1 KRB-PRIV APPLICATION[21]
     *  |
     *  +--> 0x30 L2 KRB-PRIV sequence
     *        |
     *        +--> 0xA0 0x03 pvno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno (5)
     *        |
     *        +--> 0xA1 0x03 msg-type tag
     *        |     |
     *        |     +--> 0x02 0x01 0x15 msg-type (21)
     *        |     
     *        +--> 0xA3 L3 enc-part (EncryptedData -- EncKrbPrivPart)
     */
    @Override
    public int computeLength()
    {
        pvnoLen = 1 + 1 + 1;
        krbPrivSeqLen = 1 + TLV.getNbBytes( pvnoLen ) + pvnoLen;

        msgTypeLength = 1 + 1 + Value.getNbBytes( getMessageType().getValue() );
        krbPrivSeqLen += 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;

        encPartLen = encPart.computeLength();
        krbPrivSeqLen += 1 + TLV.getNbBytes( encPartLen ) + encPartLen;

        krbPrivLen += 1 + TLV.getNbBytes( krbPrivSeqLen ) + krbPrivSeqLen;

        return 1 + TLV.getNbBytes( krbPrivLen ) + krbPrivLen;
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
            buffer.put( ( byte ) KerberosConstants.KRB_PRIV_TAG );
            buffer.put( TLV.getBytes( krbPrivLen ) );

            // The KRB-SAFE sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( krbPrivSeqLen ) );

            // pvno tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_PRIV_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLen ) );
            Value.encode( buffer, getProtocolVersionNumber() );

            // msg-type tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_PRIV_MSGTYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLength ) );
            Value.encode( buffer, getMessageType().getValue() );

            // enc-part
            buffer.put( ( byte ) KerberosConstants.KRB_PRIV_ENC_PART_TAG );
            buffer.put( TLV.getBytes( encPartLen ) );
            encPart.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_738_CANNOT_ENCODE_KRB_PRIV, 1 + TLV.getNbBytes( krbPrivLen )
                + krbPrivLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbPriv encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "KrbPriv initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KRB-PRIV : {\n" );
        sb.append( "    pvno: " ).append( getProtocolVersionNumber() ).append( '\n' );
        sb.append( "    msgType: " ).append( getMessageType() ).append( '\n' );
        sb.append( "    msgType: " ).append( getEncPart() ).append( '\n' );
        sb.append( "}\n" );

        return sb.toString();
    }
}
