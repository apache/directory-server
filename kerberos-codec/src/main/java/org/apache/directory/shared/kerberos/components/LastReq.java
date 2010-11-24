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
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.LastReqType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The LastReq structure.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * LastReq         ::=     SEQUENCE OF SEQUENCE {
 *         lr-type         [0] Int32,
 *         lr-value        [1] KerberosTime
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LastReq extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( LastReq.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    // The inner class storing the individual LastReqs
    public class LR
    {
        /** The LastReq type. */
        private LastReqType lrType;

        /** The LastReq value */
        private KerberosTime lrValue;


        /**
         * @return the LastReqType
         */
        public LastReqType getLrType()
        {
            return lrType;
        }

        /**
         * @return the lr-value
         */
        public KerberosTime getLrValue()
        {
            return lrValue;
        }
    }
    
    /** The list of LastReq elements */
    private List<LR> lastReqs = new ArrayList<LR>();
    
    /** The current LR being processed */
    private LR currentLR;


    // Storage for computed lengths
    private transient int lrTypeTagLen[];
    private transient int lrValueTagLen[];
    private transient int lastReqSeqLen[];
    private transient int lastReqSeqSeqLen;


    /**
     * Creates a new instance of LastReq.
     */
    public LastReq()
    {
    }


    /**
     * @return the CurrentLr type
     */
    public LastReqType getCurrentLrType()
    {
        return currentLR.lrType;
    }


    /**
     * Set the CurrentLr type
     */
    public void setCurrentLrType( LastReqType lrType )
    {
        currentLR.lrType = lrType;
    }


    /**
     * @return the CurrentLr value
     */
    public KerberosTime getCurrentLrValue()
    {
        return currentLR.lrValue;
    }


    /**
     * Set the CurrentLr value
     */
    public void setCurrentLrValue( KerberosTime lrValue )
    {
        currentLR.lrValue = lrValue;
    }


    /**
     * @return the CurrentLR
     */
    public LR getCurrentLR()
    {
        return currentLR;
    }


    /**
     * Create a new currentLR
     */
    public void createNewLR()
    {
        currentLR = new LR();
        lastReqs.add( currentLR );
    }


    /**
     * @return the LastReqs
     */
    public List<LR> getLastReqs()
    {
        return lastReqs;
    }


    /**
     * Compute the LastReq length
     * 
     * <pre>
     * LastReq :
     * 
     * 0x30 L1 LastReq
     *  |
     *  +--> 0x30 L2 
     *        |
     *        +--> 0xA0 L3 lr-type tag
     *        |     |
     *        |     +--> 0x02 L3-1 lrType (int)
     *        |
     *        +--> 0xA1 0x11 lr-value tag
     *              |
     *              +--> 0x18 0x0F ttt (KerberosString)
     *  </pre>
     */
    public int computeLength()
    {
        int i = 0;
        lastReqSeqLen = new int[lastReqs.size()];
        lrTypeTagLen = new int[lastReqs.size()];
        lrValueTagLen = new int[lastReqs.size()];
        lastReqSeqLen = new int[lastReqs.size()];
        
        for ( LR lr : lastReqs )
        {
            int lrTypeLen = Value.getNbBytes( lr.lrType.getValue() );
            lrTypeTagLen[i] = 1 + TLV.getNbBytes( lrTypeLen ) + lrTypeLen;
            byte[] lrValyeBytes = lr.lrValue.getBytes();
            lrValueTagLen[i] = 1 + TLV.getNbBytes( lrValyeBytes.length ) + lrValyeBytes.length;
            
            lastReqSeqLen[i] = 1 + TLV.getNbBytes( lrTypeTagLen[i] ) + lrTypeTagLen[i] + 
                                         1 + TLV.getNbBytes( lrValueTagLen[i] ) + lrValueTagLen[i];
            
            lastReqSeqSeqLen += 1 + TLV.getNbBytes( lastReqSeqLen[i] ) + lastReqSeqLen[i];
            i++;
        }

        return 1 + TLV.getNbBytes( lastReqSeqSeqLen ) + lastReqSeqSeqLen;
    }


    /**
     * Encode the LastReq message to a PDU. 
     * 
     * <pre>
     * LastReq :
     * 
     * 0x30 LL
     *   0x30 LL
     *     0xA0 LL 
     *       0x02 0x01 lrType
     *     0xA1 0x11 
     *       0x18 0x0F lrValue
     * </pre>
     * 
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
            // The lastRequest SEQ OF Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( lastReqSeqSeqLen ) );
            
            int i = 0;
            
            for ( LR lr : lastReqs )
            {
                buffer.put( UniversalTag.SEQUENCE.getValue() );
                buffer.put( TLV.getBytes( lastReqSeqLen[i] ) );
                
                // the lrType
                buffer.put( ( byte ) KerberosConstants.LAST_REQ_LR_TYPE_TAG );
                buffer.put( TLV.getBytes( lrTypeTagLen[i] ) );
                Value.encode( buffer, lr.lrType.getValue() );
    
                // the lrValue tag
                buffer.put( ( byte ) KerberosConstants.LAST_REQ_LR_VALUE_TAG );
                buffer.put( TLV.getBytes( lrValueTagLen[i] ) );

                // the lrValue value
                buffer.put( ( byte ) UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( lr.lrValue.getBytes() );
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( lastReqSeqSeqLen )
                + lastReqSeqSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "LastReq encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "LastReq initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "LastReq : {\n" );
        
        for ( LR lr : lastReqs )
        {
            sb.append( "    {\n" );
            sb.append( "        lr-type: " ).append( lr.lrType ).append( '\n' );
            sb.append( "        lr-value: " ).append( lr.lrValue ).append( '\n');
            sb.append( "    }\n" );
        }

        return sb.toString();
    }
}
