/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.asn1.util;


import org.apache.directory.shared.asn1.ber.tlv.Value;


/**
 * Parse and decode an Integer value.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IntegerDecoder
{
    private static final int[] MASK = new int[]
        { 0x000000FF, 0x0000FFFF, 0x00FFFFFF, 0xFFFFFFFF };


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Parse a byte buffer and send back an integer, controling that this number
     * is in a specified interval.
     * 
     * @param value
     *            The byte buffer to parse
     * @param min
     *            Lowest value allowed, included
     * @param max
     *            Highest value allowed, included
     * @return An integer
     * @throws IntegerDecoderException
     *             Thrown if the byte stream does not contains an integer
     */
    public static int parse( Value value, int min, int max ) throws IntegerDecoderException
    {

        int result = 0;

        byte[] bytes = value.getData();

        if ( ( bytes == null ) || ( bytes.length == 0 ) )
        {
            throw new IntegerDecoderException( "The value is 0 byte long. This is not allowed for an integer" );
        }

        if ( bytes.length > 4 )
        {
            throw new IntegerDecoderException(
                "The value is more than 4 bytes long. This is not allowed for an integer" );
        }

        for ( int i = 0; ( i < bytes.length ) && ( i < 5 ); i++ )
        {
            result = ( result << 8 ) | ( bytes[i] & 0x00FF );
        }

        if ( ( bytes[0] & 0x80 ) == 0x80 )
        {
            result = -( ( ( ~result ) + 1 ) & MASK[bytes.length - 1] );
        }

        if ( ( result >= min ) && ( result <= max ) )
        {
            return result;
        }
        else
        {
            throw new IntegerDecoderException( "The value is not in the range [" + min + ", " + max + "]" );
        }
    }


    /**
     * Parse a byte buffer and send back an integer
     * 
     * @param value
     *            The byte buffer to parse
     * @return An integer
     * @throws IntegerDecoderException
     *             Thrown if the byte stream does not contains an integer
     */
    public static int parse( Value value ) throws IntegerDecoderException
    {
        return parse( value, Integer.MIN_VALUE, Integer.MAX_VALUE );
    }
}
