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

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A principal Name, composed of a type and N names.
 * <pre>
 * PrincipalName   ::= SEQUENCE {
 *        name-type       [0] Int32,
 *        name-string     [1] SEQUENCE OF KerberosString
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalName implements Asn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( PrincipalName.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The type for this principal */
    private PrincipalNameType nameType;

    /** The principal name - we may have more than one - */
    private List<String> nameString = new ArrayList<>();

    /** The principal name as a byte[], for encoding purpose */
    private List<byte[]> nameBytes;

    // Storage for computed lengths
    private int principalNameSeqLength;
    private int principalTypeTagLength;
    private int principalTypeLength;
    private int principalStringsTagLength;
    private int principalStringsSeqLength;


    /**
     * Creates a new empty instance of PrincipalName.
     */
    public PrincipalName()
    {
    }


    /**
     * Returns the type of the {@link PrincipalName}.
     *
     * @return The type of the {@link PrincipalName}.
     */
    public PrincipalNameType getNameType()
    {
        return nameType;
    }


    /** 
     * Set the Principal name Type
     * @param nameType the Principal name Type
     */
    public void setNameType( PrincipalNameType nameType )
    {
        this.nameType = nameType;
    }


    /**
     * @return A String representing the principal names as a String 
     */
    public String getNameString()
    {
        if ( ( nameString == null ) || nameString.isEmpty() )
        {
            return "";
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;

            for ( String name : nameString )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( '/' );
                }

                sb.append( name );
            }

            return sb.toString();
        }
    }


    /**
     * Add a new name to the PrincipalName
     * @param name The name to add
     */
    public void addName( String name )
    {
        if ( nameString == null )
        {
            nameString = new ArrayList<>();
        }

        nameString.add( name );
    }


    /**
     * Compute the PrincipalName length
     * <pre>
     * PrincipalName :
     * 
     * 0x30 L1 PrincipalName sequence
     *  |
     *  +--&gt; 0xA1 L2 name-type tag
     *  |     |
     *  |     +--&gt; 0x02 L2-1 addressType (int)
     *  |
     *  +--&gt; 0xA2 L3 name-string tag
     *        |
     *        +--&gt; 0x30 L3-1 name-string (SEQUENCE OF KerberosString)
     *              |
     *              +--&gt; 0x1B L4[1] value (KerberosString)
     *              |
     *              +--&gt; 0x1B L4[2] value (KerberosString)
     *              |
     *              ...
     *              |
     *              +--&gt; 0x1B L4[n] value (KerberosString)
     * </pre>
     */
    public int computeLength()
    {
        // The principalName can't be empty.
        principalTypeLength = BerValue.getNbBytes( nameType.getValue() );
        principalTypeTagLength = 1 + 1 + principalTypeLength;

        principalNameSeqLength = 1 + TLV.getNbBytes( principalTypeTagLength ) + principalTypeTagLength;

        // Compute the keyValue
        if ( ( nameString == null ) || nameString.isEmpty() )
        {
            principalStringsSeqLength = 0;
        }
        else
        {
            principalStringsSeqLength = 0;
            nameBytes = new ArrayList<>( nameString.size() );

            for ( String name : nameString )
            {
                if ( name != null )
                {
                    byte[] bytes = Strings.getBytesUtf8( name );
                    nameBytes.add( bytes );
                    principalStringsSeqLength += 1 + TLV.getNbBytes( bytes.length ) + bytes.length;
                }
                else
                {
                    nameBytes.add( Strings.EMPTY_BYTES );
                    principalStringsSeqLength += 1 + 1;
                }
            }
        }

        principalStringsTagLength = 1 + TLV.getNbBytes( principalStringsSeqLength ) + principalStringsSeqLength;
        principalNameSeqLength += 1 + TLV.getNbBytes( principalStringsTagLength ) + principalStringsTagLength;

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( principalNameSeqLength ) + principalNameSeqLength;
    }


    /**
     * Encode the PrincipalName message to a PDU. 
     * <pre>
     * PrincipalName :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 name-type (integer)
     *   0xA1 LL 
     *     0x30 LL name-string (SEQUENCE OF KerberosString)
     *       0x1B LL name-string[1]
     *       0x1B LL name-string[2]
     *       ...
     *       0x1B LL name-string[n]
     * </pre>
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_32003_CANNOT_PUT_PDU_IN_NULL_BUFFER ) );
        }

        try
        {
            // The PrincipalName SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( principalNameSeqLength ) );

            // The name-type, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( principalTypeTagLength ) );
            BerValue.encode( buffer, nameType.getValue() );

            // The name-string tag
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( principalStringsTagLength ) );

            // The name-string sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );

            if ( ( nameString == null ) || nameString.isEmpty() )
            {
                buffer.put( ( byte ) 0x00 );
            }
            else
            {
                buffer.put( TLV.getBytes( principalStringsSeqLength ) );

                // The kerberosStrings
                for ( byte[] name : nameBytes )
                {
                    buffer.put( UniversalTag.GENERAL_STRING.getValue() );

                    if ( ( name == null ) || ( name.length == 0 ) )
                    {
                        buffer.put( ( byte ) 0x00 );
                    }
                    else
                    {
                        buffer.put( TLV.getBytes( name.length ) );
                        buffer.put( name );
                    }
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_32002_CANNOT_ENCODE__PRINCIPAL_NAME, 1 + TLV.getNbBytes( principalNameSeqLength )
                + principalNameSeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_32000_PDU_BUFFER_SIZE_TOO_SMALL ), boe );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "PrinipalName encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "PrinipalName initial value : {}", this );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "{ " );

        sb.append( "name-type: " ).append( nameType.name() );

        if ( ( nameString != null ) && !nameString.isEmpty() )
        {
            sb.append( ", name-string : <" );
            boolean isFirst = true;

            for ( String name : nameString )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( '\'' ).append( name ).append( '\'' );
            }

            sb.append( ">" );
        }
        else
        {
            sb.append( " no name-string" );
        }

        sb.append( " }" );

        return sb.toString();
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( nameString == null ) ? 0 : nameString.hashCode() );
        result = prime * result + ( ( nameType == null ) ? 0 : nameType.hashCode() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof PrincipalName ) )
        {
            return false;
        }

        PrincipalName other = ( PrincipalName ) obj;

        if ( nameString == null )
        {
            if ( other.nameString != null )
            {
                return false;
            }
        }
        else if ( !nameString.equals( other.nameString ) )
        {
            return false;
        }

        return nameType == other.nameType;
    }

}
