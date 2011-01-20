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
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store a list of METHOD-DATA
 * 
 * The ASN.1 grammar is :
 * <pre>
 * METHOD-DATA     ::= SEQUENCE OF <PA-DATA>
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MethodData extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( MethodData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** List of all PA-DATA stored */
    private List<PaData> paDatas;

    // Storage for computed lengths
    private transient int methodDataLength;


    /**
     * Creates a new instance of MethodData.
     */
    public MethodData()
    {
        this.paDatas = new ArrayList<PaData>();
    }
    

    /**
     * Adds an {@link PaData} to the list
     * @param paData The PaData to add
     */
    public void addPaData( PaData paData )
    {
        paDatas.add( paData );
    }


    /**
     * Returns true if this {@link PaData} contains a specified {@link PaData}.
     *
     * @param address The paData we are looking for in the existing list
     * @return true if this {@link PaData} contains a specified {@link PaData}.
     */
    public boolean contains( PaData paData )
    {
        if ( paDatas != null )
        {
            return paDatas.contains( paData );
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;
        
        if ( paDatas != null )
        {
            hash = hash * 17 + paDatas.size();
            
            for ( PaData paData : paDatas )
            {
                hash = hash * 17 + paData.hashCode();
            }
        }
        
        return hash;
    }


    /**
     * Returns true if two {@link MethodData} are equal.
     *
     * @param that The {@link MethodData} we want to compare with the current one
     * @return true if two {@link MethodData} are equal.
     */
    public boolean equals( MethodData that )
    {
        if ( that == null ) 
        {
            return false;
        }
        
        // infoEntries can't be null after creation
        if ( paDatas.size() != that.paDatas.size() )
        {
            return false;
        }

        for ( int i = 0; i < paDatas.size(); i++ )
        {
            if ( !paDatas.get( i ).equals( that.paDatas.get( i ) ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns the contained {@link PaData}s as an array.
     *
     * @return An array of {@link PaData}s.
     */
    public PaData[] getPaDatas()
    {
        return paDatas.toArray( new PaData[0] );
    }


    /**
     * Compute the METHOD-DATA length
     * <pre>
     * METHOD-DATA :
     * 
     * 0x30 L1 METHOD-DATA sequence of PA-DATA
     *  |
     *  +--> 0x30 L2[1] PA-DATA[1]
     *  |
     *  +--> 0x30 L2[2] PA-DATA[2]
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2[n] PA-DATA[n]
     *        
     *  where L1 = sum( L2[1], l2[2], ..., L2[n] )
     * </pre>
     */
    public int computeLength()
    {
        // Compute the PA-DATA length.
        methodDataLength = 0;

        if ( ( paDatas != null ) && ( paDatas.size() != 0 ) )
        {
            for ( PaData paData : paDatas )
            {
                int length = paData.computeLength();
                methodDataLength += length;
            }
        }

        return 1 + TLV.getNbBytes( methodDataLength ) + methodDataLength;
    }


    /**
     * Encode the METHOD-DATA message to a PDU. 
     * <pre>
     * METHOD-DATA :
     * 
     * 0x30 LL
     *   0x30 LL PA-DATA[1] 
     *   0x30 LL PA-DATA[1]
     *   ... 
     *   0x30 LL PA-DATA[1] 
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
            // The METHOD-DATA SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( methodDataLength ) );

            // The PA-DATA list, if it's not empty
            if ( ( paDatas != null ) && ( paDatas.size() != 0 ) )
            {
                for ( PaData paData : paDatas )
                {
                    paData.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_144, 1 + TLV.getNbBytes( methodDataLength )
                + methodDataLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "METHOD-DATA encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "METHOD-DATA initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        sb.append( "METHOD-DATA : " );

        for ( PaData paData : paDatas )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( paData.toString() );
        }

        return sb.toString();
    }
}
