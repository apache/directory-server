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
package org.apache.directory.shared.asn1.primitives;


import java.io.Serializable;


/**
 * Implement the Bit String primitive type. A BitString is internally stored as
 * an array of int.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BitString implements Serializable
{
    private static final long serialVersionUID = 1L;

    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** A null MutableString */
    public static final BitString EMPTY_STRING = new BitString( 1 );

    /**
     * A flag to mark the OctetString as Streamed (for OctetString larger than
     * 1024 chars)
     */
    // TODO implement the streaming...
    public static final boolean STREAMED = true;

    /** The default length of a BitString */
    private static final int DEFAULT_LENGTH = 1024;

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The number of unused ints */
    private int nbUnusedBits;

    /** Tells if the OctetString is streamed or not */
    private boolean isStreamed;

    /** The string is stored in a byte array */
    private byte[] bytes;

    /** Actual length of the byte array */
    private int nbBytes;

    /** Actual length of the bit string */
    private int nbBits;


    // ~ Constructors
    // -------------------------------------------------------------------------------*
    /**
     * A private constructor used to initialized the empty BitString
     */
    private BitString()
    {
        nbBits = 0;
        nbBytes = 0;
        nbUnusedBits = 8;
        isStreamed = false;
        bytes = new byte[]{};
    }
    
    /**
     * Creates a BitString with a specific length (length is the number of
     * bits).
     * 
     * @param length The BitString length (it's a number of bits)
     */
    public BitString( int length ) 
    {
        if ( length <= 0 )
        {
            // This is not allowed
            throw new IndexOutOfBoundsException( "Null or negative length are not allowed" );
        }
        
        nbBits = length;

        // As we store values in bytes, we must divide the length by 8
        nbBytes = ( length / 8 ) + ( ( ( length % 8 ) != 0 ) ? 1 : 0 );

        nbUnusedBits = ( 8 - length % 8 ) % 8;

        if ( nbBytes > DEFAULT_LENGTH )
        {

            // TODO : implement the streaming
            isStreamed = true;
            bytes = new byte[nbBytes];
        }
        else
        {
            isStreamed = false;
            bytes = new byte[nbBytes];
        }
    }


    /**
     * Creates a streamed BitString with a specific length. Actually, it's just
     * a simple BitString. TODO Implement streaming.
     * 
     * @param length
     *            The BitString length, in number of bits
     * @param isStreamed
     *            Tells if the BitString must be streamed or not
     */
    public BitString( int length, boolean isStreamed )
    {
        if ( length <= 0 )
        {
            // This is not allowed
            throw new IndexOutOfBoundsException( "Null or negative length are not allowed" );
        }
        
        nbBits = length;
        this.isStreamed = isStreamed;
        nbBytes = ( length / 8 ) + ( ( ( length % 8 ) != 0 ) ? 1 : 0 );

        nbUnusedBits = length % 8;

        if ( isStreamed )
        {

            // TODO : implement the streaming
            bytes = new byte[nbBytes];
        }
        else
        {
            bytes = new byte[nbBytes];
        }
    }


    /**
     * Creates a BitString with a value.
     * 
     * @param bytes The value to store. The first byte contains the number of
     * unused bits
     */
    public BitString( byte[] bytes )
    {
        nbBytes = bytes.length - 1;

        if ( nbBytes > DEFAULT_LENGTH )
        {
            isStreamed = true;

            // It will be a streamed OctetString.
            // TODO : implement the streaming
            bytes = new byte[nbBytes];
        }
        else
        {
            isStreamed = false;

            bytes = new byte[nbBytes];
        }

        setBytes( bytes, nbBytes );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Set the value into the bytes.
     * 
     * @param bytes The bytes to copy
     * @param nbBytes Number of bytes to copy
     */
    private void setBytes( byte[] bytes, int nbBytes )
    {

        // The first byte contains the number of unused ints
        nbUnusedBits = bytes[0] & 0x07;
        nbBits = ( nbBytes * 8 ) - nbUnusedBits;

        // We have to transfer the data from bytes to ints
        for ( int i = 0; i < nbBytes; i++ )
        {
            this.bytes[i] = bytes[i + 1];
        }
    }


    /**
     * Set a new BitString in the BitString. It will replace the old BitString,
     * and reset the current length with the new one.
     * 
     * @param bytes The string to store
     */
    public void setData( byte[] bytes )
    {

        if ( ( bytes == null ) || ( bytes.length == 0 ) )
        {
            nbBits = -1;
            return;
        }

        int nbBytes = bytes.length - 1;

        if ( ( nbBytes > DEFAULT_LENGTH ) && ( bytes.length < nbBytes ) )
        {

            // The current size is too small.
            // We have to allocate more space
            // TODO : implement the streaming
            bytes = new byte[nbBytes];
        }

        setBytes( bytes, nbBytes );
    }


    /**
     * Get the representation of a BitString
     * 
     * @return A byte array which represent the BitString
     */
    public byte[] getData()
    {
        return bytes;
    }


    /**
     * Get the number of unused bits
     * 
     * @return A byte which represent the number of unused bits
     */
    public byte getUnusedBits()
    {
        return ( byte ) nbUnusedBits;
    }

    /**
     * Set a bit at a specified position. 
     * The bits are stored from left to right.
     * For instance, if we have 10 bits, then they are coded as b0 b1 b2 b3 b4 b5 b6 b7 - b8 b9 x x x x x x
     * 
     * @param pos The bit to set
     */
    public void setBit( int pos )
    {
        if ( ( pos < 0 ) || ( pos > nbBits ) )
        {
            throw new IndexOutOfBoundsException( "Bad bit number : out of bound" );
        }
        
        int posInt = pos / 8;

        int bitNumber = 7 - ( pos % 8 );
        bytes[posInt] |= ( 1 << bitNumber );
    }
    
    /**
     * Clear a bit at a specified position. 
     * The bits are stored from left to right.
     * For instance, if we have 10 bits, then they are coded 
     * as b0 b1 b2 b3 b4 b5 b6 b7 - b8 b9 x x x x x x
     * 
     * @param pos The bit to clear
     */
    public void clearBit( int pos )
    {
        if ( ( pos < 0 ) || ( pos > nbBits ) )
        {
            throw new IndexOutOfBoundsException( "Bad bit number : out of bound" );
        }
        
        int posInt = pos / 8;

        int bitNumber = 7 - ( pos % 8 );
        bytes[posInt] &= ~( 1 << bitNumber );
    }
    
    /**
     * Get the bit stored into the BitString at a specific position. 
     * The bits are stored from left to right.
     * For instance, if we have 10 bits, then they are coded as b0 b1 b2 b3 b4 b5 b6 b7 - b8 b9 x x x x x x
     * 
     * With '1001 000x', where x is an unused bit, 
     *       ^ ^    ^^ 
     *       | |    || 
     *       | |    |+---- getBit(7) IndexOutOfBoundException  
     *       | |    +----- getBit(6) = 0 
     *       | +---------- getBit(2) = 0 
     *       +------------ getBit(0) = 1
     *       
     * getBit(7) -> IndexOutOfBoundsException
     * 
     * @param pos The position of the requested bit.  
     * 
     * @return <code>true</code> if the bit is set, <code>false</code>
     *         otherwise
     */
    public boolean getBit( int pos )
    {

        if ( pos > nbBits )
        {
            throw new IndexOutOfBoundsException( "Cannot get a bit at position " + pos + " when the BitString contains only "
                + nbBits + " ints" );
        }

        int posInt = pos / 8;

        int bitNumber = 7 - ( pos % 8 );
        int res = bytes[posInt] & ( 1 << bitNumber );
        return res != 0;
    }
    
    /**
     * @return The number of bytes used to encode this BitString
     */
    public int size()
    {
        return nbBytes;
    }


    /**
     * Return a native String representation of the BitString.
     * 
     * @return A String representing the BitString
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        for ( int i = 0; i < nbBits; i++ )
        {

            if ( getBit( i ) )
            {
                sb.append( '1' );
            }
            else
            {
                sb.append( '0' );
            }
        }

        return sb.toString();
    }


    /**
     * Tells if the OctetString is streamed or not
     * 
     * @return <code>true</code> if the OctetString is streamed.
     */
    public boolean isStreamed()
    {
        return isStreamed;
    }
}
