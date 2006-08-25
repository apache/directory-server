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
package org.apache.directory.shared.asn1.ber.tlv;


import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;

import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;


/**
 * This class stores the data decoded from a TLV.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Value implements Cloneable, Serializable
{
    public static final long serialVersionUID = 1L;

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /**
     * The data buffer. TODO Create a streamed data to store large data
     */
    private byte[] data;

    /** The current position of the last byte in the data buffer */
    private int currentPos;

    /** The encoded byte for a TRUE value */
    public static final byte TRUE_VALUE = ( byte ) 0xFF;

    /** The encoded byte for a FALSE value */
    public static final byte FALSE_VALUE = ( byte ) 0x00;

    /** Pre-encoded PDUs for a TRUE and FALSE TLV */
    private static final byte[] ENCODED_TRUE = new byte[]
        { 0x01, 0x01, TRUE_VALUE };

    private static final byte[] ENCODED_FALSE = new byte[]
        { 0x01, 0x01, FALSE_VALUE };

    /** Integer limits for encoding */
    private static final int ONE_BYTE_MAX = ( 1 << 7 ) - 1; // 0x7F

    private static final int ONE_BYTE_MIN = -( 1 << 7 );

    private static final int TWO_BYTE_MAX = ( 1 << 15 ) - 1; // 0x7FFF

    private static final int TWO_BYTE_MIN = -( 1 << 15 );

    private static final int THREE_BYTE_MAX = ( 1 << 23 ) - 1; // 0x7FFFFF

    private static final int THREE_BYTE_MIN = -( 1 << 23 );

    private static final int FOUR_BYTE_MAX = ( 1 << 31 ) - 1; // 0x7FFFFFFF

    private static final int FOUR_BYTE_MIN = Integer.MIN_VALUE;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * The constructor.
     */
    public Value(byte[] value)
    {
        data = value;
        currentPos = 0;
    }


    /**
     * The constructor.
     */
    public Value()
    {
        data = null;
        currentPos = 0;
    }


    /**
     * Initialize the Value
     * 
     * @param size
     *            The data size to allocate.
     */
    public void init( int size )
    {
        data = new byte[size];
        currentPos = 0;
    }


    /**
     * Reset the Value so that it can be reused
     */
    public void reset()
    {
        data = null;
        currentPos = 0;
    }


    /**
     * Clone the Value
     * 
     * @return An object that is a copy of this Value
     * @throws CloneNotSupportedException
     *             Thrown when the cloning failed
     */
    public Object clone() throws CloneNotSupportedException
    {

        return super.clone();
    }


    /**
     * Get the Values'data
     * 
     * @return Returns the data.
     */
    public byte[] getData()
    {
        return data;
    }


    /**
     * Set a block of bytes in the Value
     * 
     * @param data
     *            The data to set.
     */
    public void setData( ByteBuffer data )
    {
        int length = data.remaining();
        data.get( this.data, 0, length );
        currentPos = length;
    }


    /**
     * Append some bytes to the data buffer.
     * 
     * @param data
     *            The data to append.
     */
    public void addData( ByteBuffer data )
    {
        int length = data.remaining();
        data.get( this.data, currentPos, length );
        currentPos += length;
    }


    /**
     * Set a block of bytes in the Value
     * 
     * @param data
     *            The data to set.
     */
    public void setData( byte[] data )
    {
        System.arraycopy( data, 0, this.data, 0, data.length );
        currentPos = data.length;
    }


    /**
     * Append some bytes to the data buffer.
     * 
     * @param data
     *            The data to append.
     */
    public void addData( byte[] data )
    {
        System.arraycopy( data, 0, this.data, currentPos, data.length );
        currentPos = data.length;
    }


    /**
     * @return The number of bytes actually stored
     */
    public int getCurrentLength()
    {
        return currentPos;
    }


    /**
     * Utility function that return the number of bytes necessary to store an
     * integer value. Note that this value must be in [Integer.MIN_VALUE,
     * Integer.MAX_VALUE].
     * 
     * @param value
     *            The value to store in a byte array
     * @param sign
     *            The integer value sign
     * @return The number of bytes necessary to store the value.
     */
    public static int getNbBytes( int value )
    {
        if ( value >= ONE_BYTE_MIN && value <= ONE_BYTE_MAX )
        {
            return 1;
        }
        else if ( value >= TWO_BYTE_MIN && value <= TWO_BYTE_MAX )
        {
            return 2;
        }
        else if ( value >= THREE_BYTE_MIN && value <= THREE_BYTE_MAX )
        {
            return 3;
        }
        else if ( value >= FOUR_BYTE_MIN && value <= FOUR_BYTE_MAX )
        {
            return 4;
        }
        else
        {
            return 5;
        }
    }


    /**
     * Utility function that return a byte array representing the Value We must
     * respect the ASN.1 BER encoding scheme : 1) positive integer - [0 - 0x7F] :
     * 0xVV - [0x80 - 0xFF] : 0x00 0xVV - [0x0100 - 0x7FFF] : 0xVV 0xVV -
     * [0x8000 - 0xFFFF] : 0x00 0xVV 0xVV - [0x010000 - 0x7FFFFF] : 0xVV 0xVV
     * 0xVV - [0x800000 - 0xFFFFFF] : 0x00 0xVV 0xVV 0xVV - [0x01000000 -
     * 0x7FFFFFFF] : 0xVV 0xVV 0xVV 0xVV - [0x80000000 - 0xFFFFFFFF] : 0x00 0xVV
     * 0xVV 0xVV 0xVV 2) Negative number - (~value) + 1
     * 
     * @param value
     *            The value to store in a byte array
     * @param sign
     *            The value sign : positive or negative
     * @return The byte array representing the value.
     */
    public static byte[] getBytes( int value )
    {
        byte[] bytes = null;

        if ( value >= ONE_BYTE_MIN && value <= ONE_BYTE_MAX )
        {
            bytes = new byte[1];
            bytes[0] = ( byte ) value;
        }
        else if ( value >= TWO_BYTE_MIN && value <= TWO_BYTE_MAX )
        {
            bytes = new byte[2];
            bytes[1] = ( byte ) value;
            bytes[0] = ( byte ) ( value >> 8 );
        }
        else if ( value >= THREE_BYTE_MIN && value <= THREE_BYTE_MAX )
        {
            bytes = new byte[3];
            bytes[2] = ( byte ) value;
            bytes[1] = ( byte ) ( value >> 8 );
            bytes[0] = ( byte ) ( value >> 16 );
        }
        else if ( value >= FOUR_BYTE_MIN && value <= FOUR_BYTE_MAX )
        {
            bytes = new byte[4];
            bytes[3] = ( byte ) value;
            bytes[2] = ( byte ) ( value >> 8 );
            bytes[1] = ( byte ) ( value >> 16 );
            bytes[0] = ( byte ) ( value >> 24 );
        }

        return bytes;
    }


    /**
     * Encode a String value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param string
     *            The String to be encoded. It is supposed to be UTF-8
     */
    public static void encode( ByteBuffer buffer, String string ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( UniversalTag.OCTET_STRING_TAG );

            byte[] value = Asn1StringUtils.getBytesUtf8( string );

            buffer.put( Length.getBytes( value.length ) );

            if ( value.length != 0 )
            {
                buffer.put( value );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode an OctetString value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param byte[]
     *            The bytes to be encoded
     */
    public static void encode( ByteBuffer buffer, byte[] bytes ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( UniversalTag.OCTET_STRING_TAG );

            if ( ( bytes == null ) || ( bytes.length == 0 ) )
            {
                buffer.put( ( byte ) 0 );
            }
            else
            {
                buffer.put( Length.getBytes( bytes.length ) );
                buffer.put( bytes );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode an OID value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param string
     *            The OID to be encoded
     */
    public static void encode( ByteBuffer buffer, OID oid ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( UniversalTag.OCTET_STRING_TAG );
            buffer.put( Length.getBytes( oid.getOIDLength() ) );

            if ( oid.getOIDLength() != 0 )
            {
                buffer.put( oid.getOID() );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode an integer value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param value
     *            The integer to be encoded
     */
    public static void encode( ByteBuffer buffer, int value ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( UniversalTag.INTEGER_TAG );
            buffer.put( ( byte ) getNbBytes( value ) );
            buffer.put( getBytes( value ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode an integer value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param tag
     *            The tag if it's not an UNIVERSAL one
     * @param value
     *            The integer to be encoded
     */
    public static void encode( ByteBuffer buffer, byte tag, int value ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( tag );
            buffer.put( ( byte ) getNbBytes( value ) );
            buffer.put( getBytes( value ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode an enumerated value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param value
     *            The integer to be encoded
     */
    public static void encodeEnumerated( ByteBuffer buffer, int value ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( UniversalTag.ENUMERATED_TAG );
            buffer.put( Length.getBytes( getNbBytes( value ) ) );
            buffer.put( getBytes( value ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Encode a boolean value
     * 
     * @param buffer
     *            The PDU in which the value will be put
     * @param bool
     *            The boolean to be encoded
     */
    public static void encode( ByteBuffer buffer, boolean bool ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            buffer.put( bool ? ENCODED_TRUE : ENCODED_FALSE );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return;
    }


    /**
     * Return a string representing the Value
     * 
     * @return A string representing the value
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();
        sb.append( "DATA" );

        if ( data != null )
        {
            sb.append( '[' );
            sb.append( Asn1StringUtils.dumpBytes( data ) );
            sb.append( ']' );
        }
        else
        {

            return "[]";
        }

        return sb.toString();
    }
}