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
     * <safe-init-char> ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] |
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

    /** <safe-char> ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F] */
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
     * <base64-char> ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-0x5A] |
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
            ONE_CHAR,      ONE_CHAR,      PARSING_ERROR, PARSING_ERROR,// 20 -> 23
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 24 -> 27
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 28 -> 2B
            PARSING_ERROR, ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 2C -> 2F
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 30 -> 33
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      ONE_CHAR,     // 34 -> 37
            ONE_CHAR,      ONE_CHAR,      ONE_CHAR,      PARSING_ERROR,// 38 -> 3B
            PARSING_ERROR, PARSING_ERROR, PARSING_ERROR, ONE_CHAR      // 3C -> 3F
        };

    /** "oid." static */
    public static final String OID_LOWER = "oid.";

    /** "OID." static */
    public static final String OID_UPPER = "OID.";

    /** "oid." static */
    public static final byte[] OID_LOWER_BYTES = new byte[]
        { 'o', 'i', 'd', '.' };

    /** "OID." static */
    public static final byte[] OID_UPPER_BYTES = new byte[]
        { 'O', 'I', 'D', '.' };



    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Walk the buffer while characters are Safe String characters :
     * <safe-string> ::= <safe-init-char> <safe-chars> <safe-init-char> ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B |
     * [0x3D-0x7F] <safe-chars> ::= <safe-char> <safe-chars> | <safe-char> ::=
     * [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The position of the first character which is not a Safe Char
     */
    public static int parseSafeString( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return -1;
        }
        else
        {
            byte c = byteArray[index];

            if ( ( c > 127 ) || ( SAFE_INIT_CHAR[c] == false ) )
            {
                return -1;
            }

            index++;

            while ( index < byteArray.length )
            {
                c = byteArray[index];

                if ( ( c > 127 ) || ( SAFE_CHAR[c] == false ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }


    /**
     * Walk the buffer while characters are Alpha characters : <alpha> ::=
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The position of the first character which is not an Alpha Char
     */
    public static int parseAlphaASCII( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return -1;
        }
        else
        {
            byte c = byteArray[index++];

            if ( ( c > 127 ) || ( StringTools.ALPHA[c] == false ) )
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
     * Walk the buffer while characters are Alpha characters : <alpha> ::=
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param charArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The position of the first character which is not an Alpha Char
     */
    public static int parseAlphaASCII( char[] charArray, int index )
    {
        if ( ( charArray == null ) || ( charArray.length == 0 ) || ( index < 0 ) || ( index >= charArray.length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = charArray[index++];

            if ( ( c > 127 ) || ( StringTools.ALPHA[c] == false ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                return index;
            }
        }
    }


    /**
     * Check if the current character is a Pair Char <pairchar> ::= ',' | '=' |
     * '+' | '<' | '>' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return <code>true</code> if the current character is a Pair Char
     */
    public static boolean isPairChar( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return false;
        }
        else
        {
            byte c = byteArray[index];

            if ( ( c > 127 ) || ( PAIR_CHAR[c] == false ) )
            {
                return false;
            }
            else
            {
                if ( StringTools.isHex( byteArray, index++ ) )
                {
                    return StringTools.isHex( byteArray, index );
                }
                else
                {
                    return true;
                }
            }
        }
    }


    /**
     * Check if the current character is a Pair Char <pairchar> ::= ',' | '=' |
     * '+' | '<' | '>' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param charArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return <code>true</code> if the current character is a Pair Char
     */
    public static boolean isPairChar( char[] charArray, int index )
    {
        if ( ( charArray == null ) || ( charArray.length == 0 ) || ( index < 0 ) || ( index >= charArray.length ) )
        {
            return false;
        }
        else
        {
            char c = charArray[index];

            if ( ( c > 127 ) || ( PAIR_CHAR[c] == false ) )
            {
                return false;
            }
            else
            {
                if ( StringTools.isHex( charArray, index++ ) )
                {
                    return StringTools.isHex( charArray, index );
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
     * <pairchar> ::= ' ' | ',' | '=' | '+' | '<' | '>' | '#' | ';' | 
     *                  '\' | '"' | [0-9a-fA-F] [0-9a-fA-F]
     * 
     * @param string
     *            The string which contains the data
     * @param index
     *            Current position in the string
     * @return <code>true</code> if the current character is a Pair Char
     */
    public static int isPairChar( String string, int index )
    {
        int length = string.length();
        
        if ( ( string == null ) || ( length == 0 ) || ( index < 0 ) || ( index >= length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = string.charAt( index );

            if ( ( c > 127 ) || ( PAIR_CHAR[c] == false ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                if ( StringTools.isHex( string, index++ ) )
                {
                    return StringTools.isHex( string, index ) ? 2 : PARSING_ERROR;
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
     * ASCII. <stringchar> ::= [0x00-0xFFFF] - [,=+<>#;\"\n\r]
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isStringChar( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return -1;
        }
        else
        {
            byte c = byteArray[index];

            if ( c < 0x40 )
            {
                return STRING_CHAR[ c ];
            }
            else
            {
                return StringTools.countBytesPerChar( byteArray, index );
            }
        }
    }


    /**
     * Check if the current character is a String Char. Chars are Unicode, not
     * ASCII. <stringchar> ::= [0x00-0xFFFF] - [,=+<>#;\"\n\r]
     * 
     * @param charArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isStringChar( char[] charArray, int index )
    {
        if ( ( charArray == null ) || ( charArray.length == 0 ) || ( index < 0 ) || ( index >= charArray.length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = charArray[index];

            if ( ( c == 0x0A ) || ( c == 0x0D ) || ( c == '"' ) || ( c == '#' ) || ( c == '+' ) || ( c == ',' )
                || ( c == ';' ) || ( c == '<' ) || ( c == '=' ) || ( c == '>' ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                return ONE_CHAR;
            }
        }
    }

    /**
     * Check if the current character is a String Char. Chars are Unicode, not
     * ASCII. <stringchar> ::= [0x00-0xFFFF] - [,=+<>#;\"\n\r]
     * 
     * @param string
     *            The string which contains the data
     * @param index
     *            Current position in the string
     * @return The current char if it is a String Char, or '#' (this is simpler
     *         than throwing an exception :)
     */
    public static int isStringChar( String string, int index )
    {
        int length = string.length();
        
        if ( ( string == null ) || ( length == 0 ) || ( index < 0 ) || ( index >= length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = string.charAt( index );

            if ( c < 0x40 )
            {
                return STRING_CHAR[ c ];
            }
            else
            {
                return ONE_CHAR;
            }
        }
    }


    /**
     * Check if the current character is a Quote Char We are testing Unicode
     * chars <quotechar> ::= [0x00-0xFFFF] - [\"]
     * 
     * @param byteArray The buffer which contains the data
     * @param index Current position in the buffer
     *
     * @return <code>true</code> if the current character is a Quote Char
     */
    public static int isQuoteChar( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return -1;
        }
        else
        {
            byte c = byteArray[index];

            if ( ( c == '\\' ) || ( c == '"' ) )
            {
                return -1;
            }
            else
            {
                return StringTools.countBytesPerChar( byteArray, index );
            }
        }
    }


    /**
     * Check if the current character is a Quote Char We are testing Unicode
     * chars <quotechar> ::= [0x00-0xFFFF] - [\"]
     * 
     * @param charArray The buffer which contains the data
     * @param index Current position in the buffer
     *
     * @return <code>true</code> if the current character is a Quote Char
     */
    public static int isQuoteChar( char[] charArray, int index )
    {
        if ( ( charArray == null ) || ( charArray.length == 0 ) || ( index < 0 ) || ( index >= charArray.length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = charArray[index];

            if ( ( c == '\\' ) || ( c == '"' ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                return ONE_CHAR;
            }
        }
    }

    /**
     * Check if the current character is a Quote Char We are testing Unicode
     * chars <quotechar> ::= [0x00-0xFFFF] - [\"]
     * 
     * @param string The string which contains the data
     * @param index Current position in the string
     *
     * @return <code>true</code> if the current character is a Quote Char
     */
    public static int isQuoteChar( String string, int index )
    {
        int length = string.length();

        if ( ( string == null ) || ( length == 0 ) || ( index < 0 ) || ( index >= length ) )
        {
            return PARSING_ERROR;
        }
        else
        {
            char c = string.charAt( index );

            if ( ( c == '\\' ) || ( c == '"' ) )
            {
                return PARSING_ERROR;
            }
            else
            {
                return ONE_CHAR;
            }
        }
    }


    /**
     * Parse an hex pair <hexpair> ::= <hex> <hex>
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The new position, -1 if the buffer does not contain an HexPair,
     *         -2 if the buffer contains an hex byte but not two.
     */
    public static int parseHexPair( byte[] byteArray, int index )
    {
        if ( StringTools.isHex( byteArray, index ) )
        {
            if ( StringTools.isHex( byteArray, index + 1 ) )
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
     * Parse an hex pair <hexpair> ::= <hex> <hex>
     * 
     * @param charArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The new position, -1 if the buffer does not contain an HexPair,
     *         -2 if the buffer contains an hex byte but not two.
     */
    public static int parseHexPair( char[] charArray, int index )
    {
        if ( StringTools.isHex( charArray, index ) )
        {
            if ( StringTools.isHex( charArray, index + 1 ) )
            {
                return index + TWO_CHARS;
            }
            else
            {
                return BAD_HEX_PAIR;
            }
        }
        else
        {
            return PARSING_ERROR;
        }
    }

    /**
     * Parse an hex pair <hexpair> ::= <hex> <hex>
     * 
     * @param string
     *            The string which contains the data
     * @param index
     *            Current position in the string
     * @return The new position, -1 if the string does not contain an HexPair,
     *         -2 if the string contains an hex byte but not two.
     */
    public static int parseHexPair( String string, int index )
    {
        if ( StringTools.isHex( string, index ) )
        {
            if ( StringTools.isHex( string, index + 1 ) )
            {
                return index + TWO_CHARS;
            }
            else
            {
                return BAD_HEX_PAIR;
            }
        }
        else
        {
            return PARSING_ERROR;
        }
    }

    /**
     * Parse an hex pair <hexpair> ::= <hex> <hex>
     * 
     * @param string
     *            The string which contains the data
     * @param index
     *            Current position in the string
     * @return The new position, -1 if the string does not contain an HexPair,
     *         -2 if the string contains an hex byte but not two.
     */
    private static byte getHexPair( String string, int index )
    {
    	return (byte)((StringTools.HEX_VALUE[string.charAt( index )] << 4) | 
    				(StringTools.HEX_VALUE[string.charAt( index + 1 )]) );
    }

    /**
     * Parse an hex string, which is a list of hex pairs <hexstring> ::=
     * <hexpair> <hexpairs> <hexpairs> ::= <hexpair> <hexpairs> | e
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( byte[] byteArray, int index )
    {
        int result = parseHexPair( byteArray, index );

        if ( result < 0 )
        {
            return -1;
        }
        else
        {
            index += 2;
        }

        while ( ( result = parseHexPair( byteArray, index ) ) >= 0 )
        {
            index += 2;
        }

        return ( ( result == -2 ) ? -1 : index );
    }


    /**
     * Parse an hex string, which is a list of hex pairs <hexstring> ::=
     * <hexpair> <hexpairs> <hexpairs> ::= <hexpair> <hexpairs> | e
     * 
     * @param charArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( char[] charArray, int index )
    {
        int result = parseHexPair( charArray, index );

        if ( result < 0 )
        {
            return PARSING_ERROR;
        }
        else
        {
            index += TWO_CHARS;
        }

        while ( ( result = parseHexPair( charArray, index ) ) >= 0 )
        {
            index += TWO_CHARS;
        }

        return ( ( result == BAD_HEX_PAIR ) ? PARSING_ERROR : index );
    }

    /**
     * Parse an hex string, which is a list of hex pairs <hexstring> ::=
     * <hexpair> <hexpairs> <hexpairs> ::= <hexpair> <hexpairs> | e
     * 
     * @param string
     *            The string which contains the data
     * @param index
     *            Current position in the string
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( String string, Position pos )
    {
        pos.end = pos.start;
        int result = parseHexPair( string, pos.start );

        if ( result < 0 )
        {
            return PARSING_ERROR;
        }
        else
        {
            pos.end += TWO_CHARS;
        }

        while ( ( result = parseHexPair( string, pos.end ) ) >= 0 )
        {
            pos.end += TWO_CHARS;
        }

        return ( ( result == BAD_HEX_PAIR ) ? PARSING_ERROR : PARSING_OK );
    }

    /**
     * Parse an hex string, which is a list of hex pairs <hexstring> ::=
     * <hexpair> <hexpairs> <hexpairs> ::= <hexpair> <hexpairs> | e
     * 
     * @param string The string which contains the data
     * @param hex The result as a byte array
     * @param Position Current position in the string
     * @return Return the first position which is not an hex pair, or -1 if
     *         there is no hexpair at the beginning or if an hexpair is invalid
     *         (if we have only one hex instead of 2)
     */
    public static int parseHexString( String string, byte[] hex, Position pos )
    {
    	int i = 0;
        pos.end = pos.start;
        int result = parseHexPair( string, pos.start );

        if ( result < 0 )
        {
            return PARSING_ERROR;
        }
        else
        {
        	hex[i++] = getHexPair( string, pos.end );
            pos.end += TWO_CHARS;
        }

        while ( ( result = parseHexPair( string, pos.end ) ) >= 0 )
        {
        	hex[i++] = getHexPair( string, pos.end );
            pos.end += TWO_CHARS;
        }

        return ( ( result == BAD_HEX_PAIR ) ? PARSING_ERROR : PARSING_OK );
    }

    /**
     * Walk the buffer while characters are Base64 characters : <base64-string>
     * ::= <base64-char> <base64-chars> <base64-chars> ::= <base64-char>
     * <base64-chars> | <base64-char> ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D |
     * [0x41-0x5A] | [0x61-0x7A]
     * 
     * @param byteArray
     *            The buffer which contains the data
     * @param index
     *            Current position in the buffer
     * @return The position of the first character which is not a Base64 Char
     */
    public static int parseBase64String( byte[] byteArray, int index )
    {
        if ( ( byteArray == null ) || ( byteArray.length == 0 ) || ( index < 0 ) || ( index >= byteArray.length ) )
        {
            return -1;
        }
        else
        {
            byte c = byteArray[index];

            if ( ( c > 127 ) || ( BASE64_CHAR[c] == false ) )
            {
                return -1;
            }

            index++;

            while ( index < byteArray.length )
            {
                c = byteArray[index];

                if ( ( c > 127 ) || ( BASE64_CHAR[c] == false ) )
                {
                    break;
                }

                index++;
            }

            return index;
        }
    }
}
