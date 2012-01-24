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
 * AP-REP message.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * AP-REP          ::= [APPLICATION 15] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (15),
 *         enc-part        [2] <EncryptedData> -- EncAPRepPart
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApRep extends KerberosMessage
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ApRep.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The encryptedData, an encrypted EncAPRepPart */
    private EncryptedData encPart;

    // Storage for computed lengths
    private int pvnoLength;
    private int msgTypeLength;
    private int encPartLength;
    private int apRepLength;
    private int apRepSeqLength;


    /**
     * Creates a new instance of AP-REP.
     */
    public ApRep()
    {
        super( KerberosMessageType.AP_REP );
    }


    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }


    /**
     * Sets the {@link EncryptedData}.
     *
     * @param encPart The encrypted part
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart;
    }


    /**
     * Compute the AP-REP length
     * <pre>
     * AP-REP :
     * 
     * 0x6F L1 AP-REP message
     *  |
     *  +--> 0x30 L2 
     *        |
     *        +--> 0xA0 0x03 
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno
     *        |
     *        +--> 0xA1 0x03
     *        |     |
     *        |     +--> 0x02 0x01 0x0E msg-type
     *        |
     *        +--> 0xA2 L3
     *              |
     *              +--> 0x30 L3-1 enc-part
     *         
     * </pre>
     */
    public int computeLength()
    {
        // Compute the PVNO length.
        pvnoLength = 1 + 1 + Value.getNbBytes( getProtocolVersionNumber() );

        // Compute the msg-type length
        msgTypeLength = 1 + 1 + Value.getNbBytes( getMessageType().getValue() );

        // Compute the enc-part length
        encPartLength = encPart.computeLength();

        // Compute the sequence size
        apRepLength =
            1 + TLV.getNbBytes( pvnoLength ) + pvnoLength +
                1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength +
                1 + TLV.getNbBytes( encPartLength ) + encPartLength;

        apRepSeqLength = 1 + TLV.getNbBytes( apRepLength ) + apRepLength;

        return 1 + TLV.getNbBytes( apRepSeqLength ) + apRepSeqLength;
    }


    /**
     * Encode the AP-REP component
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

        try
        {
            // The AP-REP Tag
            buffer.put( ( byte ) KerberosConstants.AP_REP_TAG );
            buffer.put( TLV.getBytes( apRepSeqLength ) );

            // The AP-REP SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( apRepLength ) );

            // The PVNO -------------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REP_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLength ) );

            // The value
            Value.encode( buffer, getProtocolVersionNumber() );

            // The msg-type ---------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REP_MSG_TYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLength ) );

            // The value
            Value.encode( buffer, getMessageType().getValue() );

            // The enc-part ---------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REP_ENC_PART_TAG );
            buffer.put( TLV.getBytes( encPartLength ) );

            // The value
            encPart.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_137, 1 + TLV.getNbBytes( apRepLength ) + apRepLength,
                buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AP-REP encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "AP-REP initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "AP-REP :\n" );
        sb.append( "  pvno : " ).append( getProtocolVersionNumber() ).append( "\n" );
        sb.append( "  msg-type : " ).append( getMessageType() ).append( "\n" );
        sb.append( "  enc-part : " ).append( encPart ).append( "\n" );

        return sb.toString();
    }
}
