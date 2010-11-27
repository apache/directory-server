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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TYPED-DATA      ::= SEQUENCE SIZE (1..MAX) OF SEQUENCE {
 *         data-type       [0] Int32,
 *         data-value      [1] OCTET STRING OPTIONAL
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TypedData extends AbstractAsn1Object
{

    // The inner class storing the individual TDs
    public class TD
    {
        /** the type of TypedData */
        private int tdType;

        /** the TypedData data */
        private byte[] tdData;


        /**
         * @return the TD type
         */
        public int getTdType()
        {
            return tdType;
        }


        /**
         * @return the TD data
         */
        public byte[] getTdData()
        {
            return tdData;
        }
    }

    /** The list of TypedData elements */
    private List<TD> typedDataList = new ArrayList<TD>();

    /** The current TD being processed */
    private TD currentTD;

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( TypedData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    // Storage for computed lengths
    private transient int tdTypeTagLen[];
    private transient int tdDataTagLen[];
    private transient int typedDataSeqLen[];
    private transient int typedDataSeqSeqLen;


    /**
     * Compute the TypedData length
     * <pre>
     * 0x30 L1 TypedData sequence
     *  |
     *  +-- 0x30 L2 The TD sequence
     *       |
     *       +--> 0xA0 L3 tdType tag
     *       |     |
     *       |     +--> 0x02 L3-2 tdType (int)
     *       |
     *       +--> [0xA1 L4 tdData tag
     *             |
     *             +--> 0x04 L4-2 tdData (OCTET STRING)]
     * </pre>
     */
    @Override
    public int computeLength()
    {
        int i = 0;
        typedDataSeqLen = new int[typedDataList.size()];
        tdTypeTagLen = new int[typedDataList.size()];
        tdDataTagLen = new int[typedDataList.size()];

        for ( TD td : typedDataList )
        {
            int adTypeLen = Value.getNbBytes( td.tdType );
            tdTypeTagLen[i] = 1 + TLV.getNbBytes( adTypeLen ) + adTypeLen;
            typedDataSeqLen[i] = 1 + TLV.getNbBytes( tdTypeTagLen[i] ) + tdTypeTagLen[i];
            
            if ( td.tdData != null )
            {
                tdDataTagLen[i] = 1 + TLV.getNbBytes( td.tdData.length ) + td.tdData.length;
                typedDataSeqLen[i] += 1 + TLV.getNbBytes( tdDataTagLen[i] ) + tdDataTagLen[i];
            }

            typedDataSeqSeqLen += 1 + TLV.getNbBytes( typedDataSeqLen[i] ) + typedDataSeqLen[i];
            i++;
        }

        return 1 + TLV.getNbBytes( typedDataSeqSeqLen ) + typedDataSeqSeqLen;
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
            // The AuthorizationData SEQ OF Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( typedDataSeqSeqLen ) );

            int i = 0;
            for ( TD td : typedDataList )
            {
                buffer.put( UniversalTag.SEQUENCE.getValue() );
                buffer.put( TLV.getBytes( typedDataSeqLen[i] ) );

                // the tdType
                buffer.put( ( byte ) KerberosConstants.TYPED_DATA_TDTYPE_TAG );
                buffer.put( TLV.getBytes( tdTypeTagLen[i] ) );
                Value.encode( buffer, td.tdType );

                if ( td.tdData != null )
                {
                    // the tdData
                    buffer.put( ( byte ) KerberosConstants.TYPED_DATA_TDDATA_TAG );
                    buffer.put( TLV.getBytes( tdDataTagLen[i] ) );
                    Value.encode( buffer, td.tdData );
                }
                
                i++;
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_743_CANNOT_ENCODE_TYPED_DATA, 1 + TLV.getNbBytes( typedDataSeqSeqLen )
                + typedDataSeqSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "TypedData encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "TypedData initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the currentTD type
     */
    public int getCurrentTdType()
    {
        return currentTD.tdType;
    }


    /**
     * Set the current TD type
     */
    public void setCurrentTdType( int tdType )
    {
        currentTD.tdType = tdType;
    }


    /**
     * @return the currentTD data
     */
    public byte[] getCurrentTdData()
    {
        return currentTD.tdData;
    }


    /**
     * Set the current TD data
     */
    public void setCurrentTdData( byte[] tdData )
    {
        currentTD.tdData = tdData;
    }


    /**
     * @return the currentTD
     */
    public TD getCurrentTD()
    {
        return currentTD;
    }


    /**
     * Create a new currentTD
     */
    public void createNewTD()
    {
        currentTD = new TD();
        typedDataList.add( currentTD );
    }


    /**
     * @return the TypedData
     */
    public List<TD> getTypedData()
    {
        return typedDataList;
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

        sb.append( tabs ).append( "TypedData : \n" );

        for ( TD td : typedDataList )
        {
            sb.append( tabs ).append( "    {\n" );
            sb.append( tabs ).append( "        tdType: " ).append( td.tdType ).append( '\n' );
            if ( td.tdData != null )
            {
                sb.append( tabs ).append( "        tdData: " ).append( StringTools.dumpBytes( td.tdData ) ).append( '\n' );
            }
            sb.append( tabs ).append( "    }\n" );
        }

        return sb.toString();
    }

}
