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
package org.apache.directory.shared.ldap.util;

import org.apache.directory.shared.ldap.util.Position;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Utility class used by the LdapDN Parser.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DNUtils
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------
    /** A value if we got an error while parsing */
    public static final int PARSING_ERROR = -1;

    /** A value if we got a correct parsing */
    public static final int PARSING_OK = 0;

    /** If an hex pair contains only one char, this value is returned */
    public static final int BAD_HEX_PAIR = -2;

    /** A constant representing one char length */
    public static final int ONE_CHAR = 1;

    /** A constant representing two chars length */
    public static final int TWO_CHARS = 2;

    /** A constant representing one byte length */
    public static final int ONE_BYTE = 1;

    /** A constant representing two bytes length */
    public static final int TWO_BYTES = 2;

    /**
     * &lt;safe-init-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] |
     * [0x21-0x39] | 0x3B | [0x3D-0x7F]
     */
    private static final boolean[] SAFE_INIT_CHAR =
        { 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  true,  false, true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  false, true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true 
        };

    /** &lt;safe-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F] */
    private static final boolean[] SAFE_CHAR =
        { 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  true,  false, true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
        };

    /**
     * &lt;base64-char&gt; ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-0x5A] |
     * [0x61-0x7A]
     */
    private static final boolean[] BASE64_CHAR =
        { 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, true,  false, false, false, true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, false, false, true,  false, false, 
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  false, false, false, false, false,
            false, true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  true,  false, false, false, false, false 
        };

    /**
     * ' ' | '"' | '#' | '+' | ',' | [0-9] | ';' | '<' | '=' | '>' | [A-F] | '\' |
     * [a-f] 0x22 | 0x23 | 0x2B | 0x2C | [0x30-0x39] | 0x3B | 0x3C | 0x3D | 0x3E |
     * [0x41-0x46] | 0x5C | [0x61-0x66]
     */
    private static final boolean[] PAIR_CHAR =
        { 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            true,  false, true,  true,  false, false, false, false, 
            false, false, false, true,  true,  false, false, false, 
            true,  true,  true,  true,  true,  true,  true,  true, 
            true,  true,  false, true,  true,  true,  true,  false, 
            false, true,  true,  true,  true,  true,  true,  false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, true,  false, false, false, 
            false, true,  true,  true,  true,  true,  true,  false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false, 
            false, false, false, false, false, false, false, false 
        };

    /**
     * '"' | '#' | '+' | ',' | [0-9] | ';' | '<' | '=' | '>' | [A-F] | '\' |
     * [a-f] 0x22 | 0x23 | 0x2B | 0x2C | [0x30-0x39] | 0x3B | 0x3C | 0x3D | 0x3E |
     * [0x41-0x46] | 0x5C | [0x61-0x66]
     */
    private static final int[] STRING_CHAR =
        { 
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 00 -> 03
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 04 -> 07
            ONE_CHAR,      ONE_CHAR,      PARSING_ERROR, ONE_CHAR,     // 08 -> 0B
            ONE_CHAR,      PARSING_ERROR, ONE_CHAR,      ONE_CHAR,     // 0C -> 0F
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 10 -> 13
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 14 -> 17
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 18 -> 1B
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 1C -> 1F
            ONE_CHAR,      ONE_CHAR,      PARSING_ERROR, ONE_CHAR,     // 20 -> 23
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 24 -> 27
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 28 -> 2B
            PARSING_ERROR, ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 2C -> 2F
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 30 -> 33
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 34 -> 37
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 38 -> 3B
            PARSING_ERROR, ONE_CHAR,      PARSING_ERROR, ONE_CHAR      // 3C -> 3F
        };

    /** "oid." static */
    public static final String OID_LOWER = "oid.";

    /** "OID." static */
    public static final String OID_UPPER = "OID.";

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Walk the buffer while characters are Safe String characters :
     * &lt;safe-string&gt; ::= &lt;safe-init-char&gt; &lt;safe-chars&gt; &lt;safe-init-char&gt; ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B |
     * [0x3D-0x7F] &lt;safe-chars&gt; ::= &lt;safe-char&gt; &lt;safe-chars&gt; | &lt;safe-char&gt; ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not a Safe Char
     */
    public static int parseSafeString( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F ) || ( SAFE_INIT_CHAR[c] == false ) )
            {
                return -1;
            }

            index++;

            while ( index < bytes.length )
            {
                c = bytes[index];

                if ( ( ( c | 0x7F ) != 0x7F ) || ( SAFE_CHAR[c] == false ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }


    /**
     * Walk the buffer while characters are Alpha characters : &lt;alpha&gt; ::=
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not an Alpha Char
     */
    public static int parseAlphaASCII( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte b = bytes[index++];

            if ( StringTools.isAlpha( b ) == false )
            {
                return -1;
            }
            else
            {
                return index;
            }
        }
    }


    /**
     * Check if the current character is a Pair Char &lt;pairchar&gt; ::= ',' | '=' |
     * '+' | '<' | '>' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return <code>true</code> if the current character is a Pair Char
     */
    public static boolean isPairChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return false;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( PAIR_CHAR[c] == false ) )
            {
                return false;
            }
            else
            {
                if ( StringTools.isHex( bytes, index++ ) )
                {
                    return StringTools.isHex( bytes, index );
                }
                else
                {
                    return true;
                }
            }
        }
    }


    /**
     * Check if the current character is a Pair Char 
     * 
     * &lt;pairchar&gt; ::= ' ' | ',' | '=' | '+' | '<' | '>' | '#' | ';' | 
     *                  '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param bytes The byte array which contains the data
     * @param index Current position in the byte array
     * @return <code>true</code> if the current byte is a Pair Char
     */
    public static int countPairChar( byte[] bytes, int index )
    {
        if ( bytes == null )
        {
            return PARSING_ERROR;
        }

        int length = bytes.length;
        
        if ( ( length == 0 ) || ( index < 0 ) || ( index >= length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( PAIR_CHAR[c] == false ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                if ( StringTools.isHex( bytes, index++ ) )
                {
                    return StringTools.isHex( bytes, index ) ? 2 : PARSING_ERROR;
                }
                else
                {
                    return 1;
                }
            }
        }
    }


    /**
     * Check if the current character is a String Char. Chars are Unicode, not
     * ASCII. &lt;stringchar&gt; ::= [0x00-0xFFFF] - [,=+<>#;\"\n\r]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isStringChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( c | 0x3F ) == 0x3F )
            {
                return STRING_CHAR[ c ];
            }
            else
            {
                return StringTools.countBytesPerChar( bytes, index );
            }
        }
    }


    /**
     * Check if the current character is a Quote Char We are testing Unicode
     * chars &lt;quotechar&gt; ::= [0x00-0xFFFF] - [\"]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     *
     * @return <code>true</code> if the current character is a Quote Char
     */
    public static int isQuoteChar( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( c == '\\' ) || ( c == '"' ) )
            {
                return -1;
            }
            else
            {
                return StringTools.countBytesPerChar( bytes, index );
            }
        }
    }


    /**
     * Parse an hex pair &lt;hexpair&gt; ::= &lt;hex&gt; &lt;hex&gt;
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The new position, -1 if the buffer does not contain an HexPair,
     *         -2 if the buffer contains an hex byte but not two.
     */
    public static int parseHexPair( byte[] bytes, int index )
    {
        if ( StringTools.isHex( bytes, index ) )
        {
            if ( StringTools.isHex( bytes, index + 1 ) )
            {
                return index + 2;
            }
            else
            {
                return -2;
            }
        }
        else
        {
            return -1;
        }
    }


    /**
     * Parse an hex pair &lt;hexpair&gt; ::= &lt;hex&gt; &lt;hex&gt;
     * 
     * @param bytes The byte array which contains the data
     * @param index Current position in the byte array
     * @return The new position, -1 if the byte array does not contain an HexPair,
     *         -2 if the byte array contains an hex byte but not two.
     */
    private static byte getHexPair( byte[] bytes, int index )
    {
        return StringTools.getHexValue( bytes[index], bytes[index + 1] );
    }

    
    /**
     * Parse an hex string, which is a list of hex pairs &lt;hexstring&gt; ::=
     * &lt;hexpair&gt; &lt;hexpairs&gt; &lt;hexpairs&gt; ::= &lt;hexpair&gt; &lt;hexpairs&gt; | e
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( byte[] bytes, int index )
    {
        int result = parseHexPair( bytes, index );

        if ( result < 0 )
        {
            return -1;
        }
        else
        {
            index += 2;
        }

        while ( ( result = parseHexPair( bytes, index ) ) >= 0 )
        {
            index += 2;
        }

        return ( ( result == -2 ) ? -1 : index );
    }


    /**
     * Parse an hex string, which is a list of hex pairs &lt;hexstring&gt; ::=
     * &lt;hexpair&gt; &lt;hexpairs&gt; &lt;hexpairs&gt; ::= &lt;hexpair&gt; &lt;hexpairs&gt; | e
     * 
     * @param bytes The byte array which contains the data
     * @param hex The result as a byte array
     * @param pos Current position in the string
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( byte[] bytes, byte[] hex, Position pos )
    {
        int i = 0;
        pos.end = pos.start;
        int result = parseHexPair( bytes, pos.start );

        if ( result < 0 )
        {
            return PARSING_ERROR;
        }
        else
        {
            hex[i++] = getHexPair( bytes, pos.end );
            pos.end += TWO_CHARS;
        }

        while ( ( result = parseHexPair( bytes, pos.end ) ) >= 0 )
        {
            hex[i++] = getHexPair( bytes, pos.end );
            pos.end += TWO_CHARS;
        }

        return ( ( result == BAD_HEX_PAIR ) ? PARSING_ERROR : PARSING_OK );
    }

    
    /**
     * Walk the buffer while characters are Base64 characters : &lt;base64-string&gt;
     * ::= &lt;base64-char&gt; &lt;base64-chars&gt; &lt;base64-chars&gt; ::= &lt;base64-char&gt;
     * &lt;base64-chars&gt; | &lt;base64-char&gt; ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D |
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param bytes The buffer which contains the data
     * @param index Current position in the buffer
     * @return The position of the first character which is not a Base64 Char
     */
    public static int parseBase64String( byte[] bytes, int index )
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) || ( index < 0 ) || ( index >= bytes.length ) )
        {
            return -1;
        }
        else
        {
            byte c = bytes[index];

            if ( ( ( c | 0x7F ) != 0x7F )  || ( BASE64_CHAR[c] == false ) )
            {
                return -1;
            }

            index++;

            while ( index < bytes.length )
            {
                c = bytes[index];

                if ( ( ( c | 0x7F ) != 0x7F )  || ( BASE64_CHAR[c] == false ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }
}
