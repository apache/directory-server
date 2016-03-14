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
import java.util.Arrays;
import java.util.List;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store a list of ETYPE-INFO
 * 
 * The ASN.1 grammar is :
 * <pre>
 * ETYPE-INFO              ::= SEQUENCE OF <ETYPE-INFO-ENTRY>
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo implements Asn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ETypeInfo.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** List of all ETYPE-INFO-ENTRY stored */
    private List<ETypeInfoEntry> etypeInfoEntries;

    // Storage for computed lengths
    private int etypeInfoLength;


    /**
     * Creates a new instance of ETypeInfo.
     */
    public ETypeInfo()
    {
        this.etypeInfoEntries = new ArrayList<ETypeInfoEntry>();
    }


    /**
     * Creates a new instance of ETypeInfo.
     *
     * @param etypeInfoEntries The associated etypeInfoEntries
     */
    public ETypeInfo( ETypeInfoEntry[] etypeInfoEntries )
    {
        if ( etypeInfoEntries == null )
        {
            this.etypeInfoEntries = new ArrayList<ETypeInfoEntry>();
        }
        else
        {
            this.etypeInfoEntries = Arrays.asList( etypeInfoEntries );
        }
    }


    /**
     * Adds an {@link ETypeInfoEntry} to the list
     * @param etypeInfoEntry The ETypeInfoEntry to add
     */
    public void addETypeInfoEntry( ETypeInfoEntry etypeInfoEntry )
    {
        etypeInfoEntries.add( etypeInfoEntry );
    }


    /**
     * Returns true if this {@link ETypeInfoEntry} contains a specified {@link ETypeInfoEntry}.
     *
     * @param address The etypeInfoEntry we are looking for in the existing list
     * @return true if this {@link ETypeInfoEntry} contains a specified {@link ETypeInfoEntry}.
     */
    public boolean contains( ETypeInfoEntry etypeInfoEntry )
    {
        if ( etypeInfoEntries != null )
        {
            return etypeInfoEntries.contains( etypeInfoEntry );
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

        if ( etypeInfoEntries != null )
        {
            hash = hash * 17 + etypeInfoEntries.size();

            for ( ETypeInfoEntry etypeInfoEntry : etypeInfoEntries )
            {
                hash = hash * 17 + etypeInfoEntry.hashCode();
            }
        }

        return hash;
    }


    /**
     * Returns true if two {@link ETypeInfo} are equal.
     *
     * @param that The {@link ETypeInfo} we want to compare with the current one
     * @return true if two {@link ETypeInfo} are equal.
     */
    public boolean equals( ETypeInfo that )
    {
        if ( that == null )
        {
            return false;
        }

        // infoEntries can't be null after creation
        if ( etypeInfoEntries.size() != that.etypeInfoEntries.size() )
        {
            return false;
        }

        for ( int i = 0; i < etypeInfoEntries.size(); i++ )
        {
            if ( !etypeInfoEntries.get( i ).equals( that.etypeInfoEntries.get( i ) ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns the contained {@link ETypeInfoEntry}s as an array.
     *
     * @return An array of {@link ETypeInfoEntry}s.
     */
    public ETypeInfoEntry[] getETypeInfoEntries()
    {
        return etypeInfoEntries.toArray( new ETypeInfoEntry[0] );
    }


    /**
     * Compute the ETypeInfo length
     * <pre>
     * ETypeInfo :
     * 
     * 0x30 L1 ETypeInfo sequence of ETypeInfoEntry
     *  |
     *  +--> 0x30 L2[1] ETypeInfoEntry[1]
     *  |
     *  +--> 0x30 L2[2] ETypeInfoEntry[2]
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2[n] ETypeInfoEntry[n]
     *        
     *  where L1 = sum( L2[1], l2[2], ..., L2[n] )
     * </pre>
     */
    public int computeLength()
    {
        // Compute the ETypeInfo length.
        etypeInfoLength = 0;

        if ( ( etypeInfoEntries != null ) && ( etypeInfoEntries.size() != 0 ) )
        {
            for ( ETypeInfoEntry infoEntry : etypeInfoEntries )
            {
                int length = infoEntry.computeLength();
                etypeInfoLength += length;
            }
        }

        return 1 + TLV.getNbBytes( etypeInfoLength ) + etypeInfoLength;
    }


    /**
     * Encode the ETypeInfo message to a PDU. 
     * <pre>
     * ETypeInfo :
     * 
     * 0x30 LL
     *   0x30 LL ETypeInfoEntry[1] 
     *   0x30 LL ETypeInfoEntry[1]
     *   ... 
     *   0x30 LL ETypeInfoEntry[1] 
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
            // The ETypeInfoEntry SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( etypeInfoLength ) );

            // The ETypeInfoEntry list, if it's not empty
            if ( ( etypeInfoEntries != null ) && ( etypeInfoEntries.size() != 0 ) )
            {
                for ( ETypeInfoEntry infoEntry : etypeInfoEntries )
                {
                    infoEntry.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_144, 1 + TLV.getNbBytes( etypeInfoLength )
                + etypeInfoLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ), boe );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "ETYPE-INFO encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "ETYPE-INFO initial value : {}", toString() );
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

        for ( ETypeInfoEntry infoEntry : etypeInfoEntries )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( infoEntry.toString() );
        }

        return sb.toString();
    }
}
