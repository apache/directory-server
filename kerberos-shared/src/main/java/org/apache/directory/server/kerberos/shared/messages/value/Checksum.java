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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Checksum structure is used to store a checksum associated to a type.
 * 
 * The ASN.1 grammar is :
 * Checksum        ::= SEQUENCE {
 *       cksumtype       [0] Int32,
 *       checksum        [1] OCTET STRING
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Checksum extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( Checksum.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The checksum type used */
    private ChecksumType cksumtype;

    /** The byte array containing the checksum */
    private byte[] checksum;

    // Storage for computed lengths
    private transient int checksumTypeLength;
    private transient int checksumBytesLength;
    private transient int checksumLength;


    /**
     * Creates a new instance of Checksum.
     */
    public Checksum()
    {
    }

    
    /**
     * Creates a new instance of Checksum.
     *
     * @param checksumType The checksum type used
     * @param checksum The checksum value
     */
    public Checksum( ChecksumType cksumtype, byte[] checksum )
    {
        this.cksumtype = cksumtype;
        this.checksum = checksum;
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( ( o == null ) || !( o instanceof Checksum ) )
        {
            return false;
        }

        Checksum that = ( Checksum ) o;

        return ( cksumtype == that.cksumtype ) && ( Arrays.equals( checksum, that.checksum ) );
    }


    /**
     * Returns the checksum value.
     *
     * @return The checksum value.
     */
    public byte[] getChecksumValue()
    {
        return checksum;
    }


    /**
     * Set the checksum Value.
     *
     * @param checksum The checksum value
     */
    public void setChecksumValue( byte[] checksum )
    {
        this.checksum = checksum;
    }


    /**
     * Returns the {@link ChecksumType}.
     *
     * @return The {@link ChecksumType}.
     */
    public ChecksumType getChecksumType()
    {
        return cksumtype;
    }


    /**
     * Set the {@link ChecksumType}.
     *
     * @param cksumtype The checksum algorithm used
     */
    public void setChecksumType( ChecksumType cksumType )
    {
        this.cksumtype = cksumType;
    }


    /**
     * Compute the checksum length
     * 
     * Checksum :
     * 
     * 0x30 L1 checksum sequence
     *  |
     *  +--> 0xA0 L2 cksumtype tag
     *  |     |
     *  |     +--> 0x02 L2-1 cksumtype (int)
     *  |
     *  +--> 0xA1 L3 checksum tag
     *        |
     *        +--> 0x04 L3-1 checksum (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the checksulType. The Length will always be contained in 1 byte
        checksumTypeLength = 1 + 1 + Value.getNbBytes( cksumtype.getOrdinal() );
        checksumLength = 1 + TLV.getNbBytes( checksumTypeLength ) + checksumTypeLength;

        // Compute the checksum Value
        if ( checksum == null )
        {
            checksumBytesLength = 1 + 1;
        }
        else
        {
            checksumBytesLength = 1 + TLV.getNbBytes( checksum.length ) + checksum.length;
        }

        checksumLength += 1 + TLV.getNbBytes( checksumBytesLength ) + checksumBytesLength;

        // Compute the whole sequence length
        int checksumSeqLength = 1 + Value.getNbBytes( checksumLength ) + checksumLength;

        return checksumSeqLength;

    }


    /**
     * Encode the Checksum message to a PDU. 
     * 
     * Checksum :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 cksumtype
     *   0xA1 LL 
     *     0x04 LL Checksum
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The Checksum SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( checksumLength ) );

            // The cksumtype, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( checksumTypeLength ) );
            Value.encode( buffer, cksumtype.getOrdinal() );

            // The checksum, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( checksumBytesLength ) );
            Value.encode( buffer, checksum );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the Checksum object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( checksumLength ) + checksumLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "Checksum encoding : {}", StringTools.dumpBytes( buffer.array() ) );
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

        sb.append( tabs ).append( "Checksum : {\n" );
        sb.append( tabs ).append( "    cksumtype: " ).append(  cksumtype ).append( '\n' );

        if ( checksum != null )
        {
            sb.append( tabs + "    checksum:" ).append( StringTools.dumpBytes( checksum ) ).append( '\n' );
        }

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
