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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store a list of ETYPE-INFO2.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * ETYPE-INFO2              ::= SEQUENCE SIZE (1..MAX) OF ETYPE-INFO2-ENTRY
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo2 extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ETypeInfo2.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** List of all ETYPE-INFO2-ENTRY stored */
    private List<ETypeInfo2Entry> etypeInfo2Entries;

    // Storage for computed lengths
    private int etypeInfo2Length;


    /**
     * Creates a new instance of ETypeInfo2.
     */
    public ETypeInfo2()
    {
        this.etypeInfo2Entries = new ArrayList<ETypeInfo2Entry>();
    }


    /**
     * Creates a new instance of ETypeInfo2.
     *
     * @param etypeInfo2Entries The associated etypeInfo2Entries
     */
    public ETypeInfo2( ETypeInfo2Entry[] etypeInfo2Entries )
    {
        if ( etypeInfo2Entries == null )
        {
            this.etypeInfo2Entries = new ArrayList<ETypeInfo2Entry>();
        }
        else
        {
            this.etypeInfo2Entries = Arrays.asList( etypeInfo2Entries );
        }
    }


    /**
     * Adds an {@link ETypeInfo2Entry} to the list
     * @param etypeInfo2Entry The ETypeInfo2Entry to add
     */
    public void addETypeInfo2Entry( ETypeInfo2Entry etypeInfo2Entry )
    {
        etypeInfo2Entries.add( etypeInfo2Entry );
    }


    /**
     * Returns true if this {@link ETypeInfo2Entry} contains a specified {@link ETypeInfo2Entry}.
     *
     * @param address The etypeInfo2Entry we are looking for in the existing list
     * @return true if this {@link ETypeInfo2Entry} contains a specified {@link ETypeInfo2Entry}.
     */
    public boolean contains( ETypeInfo2Entry etypeInfo2Entry )
    {
        if ( etypeInfo2Entries != null )
        {
            return etypeInfo2Entries.contains( etypeInfo2Entry );
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

        if ( etypeInfo2Entries != null )
        {
            hash = hash * 17 + etypeInfo2Entries.size();

            for ( ETypeInfo2Entry etypeInfo2Entry : etypeInfo2Entries )
            {
                hash = hash * 17 + etypeInfo2Entry.hashCode();
            }
        }

        return hash;
    }


    /**
     * Returns true if two {@link ETypeInfo2} are equal.
     *
     * @param that The {@link ETypeInfo2} we want to compare with the current one
     * @return true if two {@link ETypeInfo2} are equal.
     */
    public boolean equals( ETypeInfo2 that )
    {
        if ( that == null )
        {
            return false;
        }

        // infoEntries can't be null after creation
        if ( etypeInfo2Entries.size() != that.etypeInfo2Entries.size() )
        {
            return false;
        }

        for ( int i = 0; i < etypeInfo2Entries.size(); i++ )
        {
            if ( !etypeInfo2Entries.get( i ).equals( that.etypeInfo2Entries.get( i ) ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Returns the contained {@link ETypeInfo2Entry}s as an array.
     *
     * @return An array of {@link ETypeInfo2Entry}s.
     */
    public ETypeInfo2Entry[] getETypeInfo2Entries()
    {
        return etypeInfo2Entries.toArray( new ETypeInfo2Entry[0] );
    }


    /**
     * Compute the ETypeInfo2 length
     * <pre>
     * ETypeInfo2 :
     * 
     * 0x30 L1 ETypeInfo2 sequence of ETypeInfo2Entry
     *  |
     *  +--> 0x30 L2[1] ETypeInfo2Entry[1]
     *  |
     *  +--> 0x30 L2[2] ETypeInfo2Entry[2]
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2[n] ETypeInfo2Entry[n]
     *        
     *  where L1 = sum( L2[1], l2[2], ..., L2[n] )
     * </pre>
     */
    public int computeLength()
    {
        // Compute the ETypeInfo2 length.
        etypeInfo2Length = 0;

        if ( ( etypeInfo2Entries != null ) && ( etypeInfo2Entries.size() != 0 ) )
        {
            for ( ETypeInfo2Entry info2Entry : etypeInfo2Entries )
            {
                int length = info2Entry.computeLength();
                etypeInfo2Length += length;
            }
        }

        return 1 + TLV.getNbBytes( etypeInfo2Length ) + etypeInfo2Length;
    }


    /**
     * Encode the ETypeInfo2 message to a PDU. 
     * <pre>
     * ETypeInfo2 :
     * 
     * 0x30 LL
     *   0x30 LL ETypeInfo2Entry[1] 
     *   0x30 LL ETypeInfo2Entry[1]
     *   ... 
     *   0x30 LL ETypeInfo2Entry[1] 
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
            // The ETypeInfo2Entry SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( etypeInfo2Length ) );

            // The ETypeInfo2Entry list, if it's not empty
            if ( ( etypeInfo2Entries != null ) && ( etypeInfo2Entries.size() != 0 ) )
            {
                for ( ETypeInfo2Entry info2Entry : etypeInfo2Entries )
                {
                    info2Entry.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_144, 1 + TLV.getNbBytes( etypeInfo2Length )
                + etypeInfo2Length, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
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

        for ( ETypeInfo2Entry info2Entry : etypeInfo2Entries )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( info2Entry.toString() );
        }

        return sb.toString();
    }
}
