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
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.util.Strings;
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
        private int dataType;

        /** the TypedData data */
        private byte[] dataValue;


        /**
         * @return the TD type
         */
        public int getDataType()
        {
            return dataType;
        }


        /**
         * @return the TD data
         */
        public byte[] getDataValue()
        {
            return dataValue;
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
    private int dataTypeTagLength[];
    private int dataValueTagLength[];
    private int typedDataSeqLength[];
    private int typedDataSeqSeqLength;


    /**
     * @return the currentTD type
     */
    public int getCurrentDataType()
    {
        return currentTD.dataType;
    }


    /**
     * Set the current TD type
     */
    public void setCurrentDataType( int tdType )
    {
        currentTD.dataType = tdType;
    }


    /**
     * @return the currentTD data
     */
    public byte[] getCurrentDataValue()
    {
        return currentTD.dataValue;
    }


    /**
     * Set the current TD data
     */
    public void setCurrentDataValue( byte[] tdData )
    {
        currentTD.dataValue = tdData;
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
        typedDataSeqLength = new int[typedDataList.size()];
        dataTypeTagLength = new int[typedDataList.size()];
        dataValueTagLength = new int[typedDataList.size()];
        typedDataSeqSeqLength = 0;

        for ( TD td : typedDataList )
        {
            int adTypeLen = Value.getNbBytes( td.dataType );
            dataTypeTagLength[i] = 1 + TLV.getNbBytes( adTypeLen ) + adTypeLen;
            typedDataSeqLength[i] = 1 + TLV.getNbBytes( dataTypeTagLength[i] ) + dataTypeTagLength[i];

            if ( td.dataValue != null )
            {
                dataValueTagLength[i] = 1 + TLV.getNbBytes( td.dataValue.length ) + td.dataValue.length;
                typedDataSeqLength[i] += 1 + TLV.getNbBytes( dataValueTagLength[i] ) + dataValueTagLength[i];
            }

            typedDataSeqSeqLength += 1 + TLV.getNbBytes( typedDataSeqLength[i] ) + typedDataSeqLength[i];
            i++;
        }

        return 1 + TLV.getNbBytes( typedDataSeqSeqLength ) + typedDataSeqSeqLength;
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
            buffer.put( TLV.getBytes( typedDataSeqSeqLength ) );

            int i = 0;
            for ( TD td : typedDataList )
            {
                buffer.put( UniversalTag.SEQUENCE.getValue() );
                buffer.put( TLV.getBytes( typedDataSeqLength[i] ) );

                // the tdType
                buffer.put( ( byte ) KerberosConstants.TYPED_DATA_TDTYPE_TAG );
                buffer.put( TLV.getBytes( dataTypeTagLength[i] ) );
                Value.encode( buffer, td.dataType );

                if ( td.dataValue != null )
                {
                    // the tdData
                    buffer.put( ( byte ) KerberosConstants.TYPED_DATA_TDDATA_TAG );
                    buffer.put( TLV.getBytes( dataValueTagLength[i] ) );
                    Value.encode( buffer, td.dataValue );
                }

                i++;
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_743_CANNOT_ENCODE_TYPED_DATA, 1 + TLV.getNbBytes( typedDataSeqSeqLength )
                + typedDataSeqSeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "TypedData encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "TypedData initial value : {}", toString() );
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

        sb.append( tabs ).append( "TypedData : \n" );

        for ( TD td : typedDataList )
        {
            sb.append( tabs ).append( "    {\n" );
            sb.append( tabs ).append( "        tdType: " ).append( td.dataType ).append( '\n' );
            if ( td.dataValue != null )
            {
                sb.append( tabs ).append( "        tdData: " ).append( Strings.dumpBytes( td.dataValue ) )
                    .append( '\n' );
            }
            sb.append( tabs ).append( "    }\n" );
        }

        return sb.toString();
    }
}
