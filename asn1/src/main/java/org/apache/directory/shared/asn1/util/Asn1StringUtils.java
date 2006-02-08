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

import java.io.UnsupportedEncodingException;

/**
 * Little helper class. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Asn1StringUtils
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Hex chars */
    private static final byte[] HEX_CHAR =
        new byte[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Helper function that dump a byte in hex form
     * 
     * @param octet The byte to dump
     * @return A string representation of the byte
     */
    public static String dumpByte( byte octet )
    {
        return new String(
                new byte[] { '0', 'x', HEX_CHAR[( octet & 0x00F0 ) >> 4], HEX_CHAR[octet & 0x000F] } );
    }

    /**
     * Helper function that dump an array of bytes in hex form
     * 
     * @param buffer The bytes array to dump
     * @return A string representation of the array of bytes
     */
    public static String dumpBytes( byte[] buffer )
    {
        if( buffer == null )
        {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        
        for (int i = 0; i < buffer.length ; i++)
        {
            sb.append("0x").append((char)(HEX_CHAR[( buffer[i] & 0x00F0 ) >> 4])).append((char)(HEX_CHAR[buffer[i] & 0x000F])).append(" ");
        }
        
        return sb.toString();
    }
    
    /**
     * Return UTF-8 encoded byte[] representation of a String
     * @param string The string to be transformed to a byte array
     * @return The transformed byte array 
     */
    public static byte[] getBytesUtf8( String string )
    {
        if( string == null )
        {
            return new byte[0];
        }

        try
        {
            return string.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            return new byte[]{};
        }
    }
}
