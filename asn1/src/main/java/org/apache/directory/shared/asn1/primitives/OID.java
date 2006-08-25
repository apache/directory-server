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

import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * This class implement an OID (Object Identifier). An OID is encoded as a list
 * of bytes representing integers. An OID has a numeric representation where
 * number are separated with dots : SPNEGO Oid = 1.3.6.1.5.5.2 Translating from
 * a byte list to a dot separated list of number follows the rules : - the first
 * number is in [0..2] - the second number is in [0..39] if the first number is
 * 0 or 1 - the first byte has a value equal to : number 1 * 40 + number two -
 * the upper bit of a byte is set if the next byte is a part of the number For
 * instance, the SPNEGO Oid (1.3.6.1.5.5.2) will be encoded : 1.3 -> 0x2B (1*40 +
 * 3 = 43 = 0x2B) .6 -> 0x06 .1 -> 0x01 .5 -> 0x05 .5 -> 0x05 .2 -> 0x02 The
 * Kerberos V5 Oid (1.2.840.48018.1.2.2) will be encoded : 1.2 -> 0x2A (1*40 + 2 =
 * 42 = 0x2A) 840 -> 0x86 0x48 (840 = 6 * 128 + 72 = (0x06 | 0x80) 0x48 = 0x86
 * 0x48 48018 -> 0x82 0xF7 0x12 (2 * 128 * 128 + 119 * 128 + 18 = (0x02 | 0x80)
 * (0x77 | 0x80) 0x12
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OID implements Serializable
{
    private static final long serialVersionUID = 1L;

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The OID as a array of int */
    private long[] oidValues;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new OID object.
     */
    public OID()
    {

        // We should not create this kind of object directly, it must
        // be created through the factory.
    }


    /**
     * Create a new OID object from a byte array
     * 
     * @param oid
     */
    public OID(byte[] oid) throws DecoderException
    {
        setOID( oid );
    }


    /**
     * Create a new OID object from a String
     * 
     * @param oid
     *            The String which is supposed to be an OID
     */
    public OID(String oid) throws DecoderException
    {
        setOID( oid );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * Set the OID. It will be translated from a byte array to an internal
     * representation.
     * 
     * @param oid
     *            The bytes containing the OID
     */
    public void setOID( byte[] oid ) throws DecoderException
    {

        if ( oid == null )
        {
            throw new DecoderException( "Null OID" );
        }

        if ( oid.length < 1 )
        {
            throw new DecoderException( "Invalid OID : " + oid );
        }

        // First, we have to calculate the number of int to allocate
        int nbValues = 1;

        int pos = 0;

        while ( pos < oid.length )
        {

            if ( oid[pos] >= 0 )
            {
                nbValues++;
            }

            pos++;
        }

        oidValues = new long[nbValues];

        nbValues = 0;
        pos = 0;

        int accumulator = 0;

        if ( ( oid[0] < 0 ) || ( oid[0] >= 80 ) )
        {
            oidValues[nbValues++] = 2;

            while ( pos < oid.length )
            {

                if ( oid[pos] >= 0 )
                {
                    oidValues[nbValues++] = ( ( accumulator << 7 ) + oid[pos] ) - 80;
                    accumulator = 0;
                    pos++;
                    break;
                }
                else
                {
                    accumulator = ( accumulator << 7 ) + ( oid[pos] & 0x007F );
                }

                pos++;
            }
        }
        else if ( oid[0] < 40 )
        {
            oidValues[nbValues++] = 0;
            oidValues[nbValues++] = oid[pos++]; // itu-t
        }
        else
        // oid[0] is < 80
        {
            oidValues[nbValues++] = 1;
            oidValues[nbValues++] = oid[pos++] - 40; // iso
        }

        while ( pos < oid.length )
        {

            if ( oid[pos] >= 0 )
            {
                oidValues[nbValues++] = ( accumulator << 7 ) + oid[pos];
                accumulator = 0;
            }
            else
            {
                accumulator = ( accumulator << 7 ) + ( oid[pos] & 0x007F );
            }

            pos++;
        }
    }


    /**
     * Set the OID. It will be translated from a String to an internal
     * representation. The syntax will be controled in respect with this rule :
     * OID = ( [ '0' | '1' ] '.' [ 0 .. 39 ] | '2' '.' int) ( '.' int )*
     * 
     * @param oid
     *            The String containing the OID
     */
    public void setOID( String oid ) throws DecoderException
    {

        if ( ( oid == null ) || ( oid.length() == 0 ) )
        {
            throw new DecoderException( "Null OID" );
        }

        int nbValues = 1;
        byte[] bytes = oid.getBytes();
        boolean dotSeen = false;

        // Count the number of int to allocate.
        for ( int i = 0; i < bytes.length; i++ )
        {

            if ( bytes[i] == '.' )
            {

                if ( dotSeen )
                {

                    // Two dots, that's an error !
                    throw new DecoderException( "Invalid OID : " + oid );
                }

                nbValues++;
                dotSeen = true;
            }
            else
            {
                dotSeen = false;
            }
        }

        // We must have at least 2 ints
        if ( nbValues < 2 )
        {
            throw new DecoderException( "Invalid OID : " + oid );
        }

        oidValues = new long[nbValues];

        int pos = 0;
        int intPos = 0;

        // This flag is used to forbid a second value above 39 if the
        // first value is 0 or 1 (itu_t or iso arcs)
        boolean ituOrIso = false;

        // The first value
        switch ( bytes[pos] )
        {

            case '0': // itu-t
            case '1': // iso
                ituOrIso = true;
            // fallthrough

            case '2': // joint-iso-itu-t
                oidValues[intPos++] = bytes[pos++] - '0';
                break;

            default: // error, this value is not allowed
                throw new DecoderException( "Invalid OID : " + oid );
        }

        // We must have a dot
        if ( bytes[pos++] != '.' )
        {
            throw new DecoderException( "Invalid OID : " + oid );
        }

        dotSeen = true;

        int value = 0;

        for ( int i = pos; i < bytes.length; i++ )
        {

            if ( bytes[i] == '.' )
            {

                if ( dotSeen )
                {

                    // Two dots, that's an error !
                    throw new DecoderException( "Invalid OID : " + oid );
                }

                if ( ituOrIso && value > 39 )
                {
                    throw new DecoderException( "Invalid OID : " + oid );
                }
                else
                {
                    ituOrIso = false;
                }

                nbValues++;
                dotSeen = true;
                oidValues[intPos++] = value;
                value = 0;
            }
            else if ( ( bytes[i] >= 0x30 ) && ( bytes[i] <= 0x39 ) )
            {
                dotSeen = false;
                value = ( ( value * 10 ) + bytes[i] ) - '0';

            }
            else
            {

                // We don't have a number, this is an error
                throw new DecoderException( "Invalid OID : " + oid );
            }
        }

        oidValues[intPos++] = value;
    }


    /**
     * Get an array of int from the OID
     * 
     * @return An array of int representing the OID
     */
    public long[] getOIDValues()
    {
        return oidValues;
    }


    /**
     * Get the number of bytes necessary to store the OID
     * 
     * @return An int representing the length of the OID
     */
    public int getOIDLength()
    {
        long value = oidValues[0] * 40 + oidValues[1];
        int nbBytes = 0;

        if ( value < 128 )
        {
            nbBytes = 1;
        }
        else if ( value < 16384 )
        {
            nbBytes = 2;
        }
        else if ( value < 2097152 )
        {
            nbBytes = 3;
        }
        else if ( value < 268435456 )
        {
            nbBytes = 4;
        }
        else
        {
            nbBytes = 5;
        }

        for ( int i = 2; i < oidValues.length; i++ )
        {
            value = oidValues[i];

            if ( value < 128 )
            {
                nbBytes += 1;
            }
            else if ( value < 16384 )
            {
                nbBytes += 2;
            }
            else if ( value < 2097152 )
            {
                nbBytes += 3;
            }
            else if ( value < 268435456 )
            {
                nbBytes += 4;
            }
            else
            {
                nbBytes += 5;
            }
        }

        return nbBytes;
    }


    /**
     * Get an array of bytes from the OID
     * 
     * @return An array of int representing the OID
     */
    public byte[] getOID()
    {
        long value = oidValues[0] * 40 + oidValues[1];
        long firstValues = value;

        byte[] bytes = new byte[getOIDLength()];
        int pos = 0;

        if ( oidValues[0] < 2 )
        {
            bytes[pos++] = ( byte ) ( oidValues[0] * 40 + oidValues[1] );
        }
        else
        {
            if ( firstValues < 128 )
            {
                bytes[pos++] = ( byte ) ( firstValues );
            }
            else if ( firstValues < 16384 )
            {
                bytes[pos++] = ( byte ) ( ( firstValues >> 7 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( firstValues & 0x007F );
            }
            else if ( value < 2097152 )
            {
                bytes[pos++] = ( byte ) ( ( firstValues >> 14 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( firstValues & 0x007F );
            }
            else if ( value < 268435456 )
            {
                bytes[pos++] = ( byte ) ( ( firstValues >> 21 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 14 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( firstValues & 0x007F );
            }
            else
            {
                bytes[pos++] = ( byte ) ( ( firstValues >> 28 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 21 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 14 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( firstValues >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( firstValues & 0x007F );
            }
        }

        for ( int i = 2; i < oidValues.length; i++ )
        {
            value = oidValues[i];

            if ( value < 128 )
            {
                bytes[pos++] = ( byte ) ( value );
            }
            else if ( value < 16384 )
            {
                bytes[pos++] = ( byte ) ( ( value >> 7 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( value & 0x007F );
            }
            else if ( value < 2097152 )
            {
                bytes[pos++] = ( byte ) ( ( value >> 14 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( value & 0x007F );
            }
            else if ( value < 268435456 )
            {
                bytes[pos++] = ( byte ) ( ( value >> 21 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 14 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( value & 0x007F );
            }
            else
            {
                bytes[pos++] = ( byte ) ( ( value >> 28 ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 21 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 14 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( ( ( value >> 7 ) & 0x007F ) | 0x0080 );
                bytes[pos++] = ( byte ) ( value & 0x007F );
            }
        }

        return bytes;
    }


    /**
     * Get the OID as a String
     * 
     * @return A String representing the OID
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        if ( oidValues != null )
        {
            sb.append( oidValues[0] );

            for ( int i = 1; i < oidValues.length; i++ )
            {
                sb.append( '.' ).append( oidValues[i] );
            }
        }

        return sb.toString();
    }


    public static boolean isOID( String oid )
    {
        if ( ( oid == null ) || ( oid.length() == 0 ) )
        {
            return false;
        }

        int nbValues = 1;
        byte[] bytes = oid.getBytes();
        boolean dotSeen = false;

        // Count the number of int to allocate.
        for ( int i = 0; i < bytes.length; i++ )
        {

            if ( bytes[i] == '.' )
            {

                if ( dotSeen )
                {

                    // Two dots, that's an error !
                    return false;
                }

                nbValues++;
                dotSeen = true;
            }
            else
            {
                dotSeen = false;
            }
        }

        // We must have at least 2 ints
        if ( nbValues < 2 )
        {
            return false;
        }

        int pos = 0;

        // This flag is used to forbid a second value above 39 if the
        // first value is 0 or 1 (itu_t or iso arcs)
        boolean ituOrIso = false;

        // The first value
        switch ( bytes[pos++] )
        {

            case '0': // itu-t
            case '1': // iso
                ituOrIso = true;
            // fallthrough

            case '2': // joint-iso-itu-t
                break;

            default: // error, this value is not allowed
                return false;
        }

        // We must have a dot
        if ( bytes[pos++] != '.' )
        {
            return false;
        }

        dotSeen = true;

        long value = 0;

        for ( int i = pos; i < bytes.length; i++ )
        {

            if ( bytes[i] == '.' )
            {

                if ( dotSeen )
                {
                    // Two dots, that's an error !
                    return false;
                }

                if ( ituOrIso && value > 39 )
                {
                    return false;
                }
                else
                {
                    ituOrIso = false;
                }

                nbValues++;
                dotSeen = true;
                value = 0;
            }
            else if ( ( bytes[i] >= 0x30 ) && ( bytes[i] <= 0x39 ) )
            {
                dotSeen = false;

                value = ( ( value * 10 ) + bytes[i] ) - '0';
            }
            else
            {
                // We don't have a number, this is an error
                return false;
            }
        }

        return true;
    }
}
