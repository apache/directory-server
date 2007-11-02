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
package org.apache.directory.shared.ldap.name;


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.util.DNUtils;
import org.apache.directory.shared.ldap.util.Position;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * This class parse the name-component part or the following BNF grammar (as of
 * RFC2253, par. 3, and RFC1779, fig. 1) : <br> - &lt;name-component&gt; ::=
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; <br> -
 * &lt;attributeTypeAndValues&gt; ::= &lt;spaces&gt; '+' &lt;spaces&gt;
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; | e <br> -
 * &lt;attributeType&gt; ::= [a-zA-Z] &lt;keychars&gt; | &lt;oidPrefix&gt; [0-9]
 * &lt;digits&gt; &lt;oids&gt; | [0-9] &lt;digits&gt; &lt;oids&gt; <br> -
 * &lt;keychars&gt; ::= [a-zA-Z] &lt;keychars&gt; | [0-9] &lt;keychars&gt; | '-'
 * &lt;keychars&gt; | e <br> - &lt;oidPrefix&gt; ::= 'OID.' | 'oid.' | e <br> -
 * &lt;oids&gt; ::= '.' [0-9] &lt;digits&gt; &lt;oids&gt; | e <br> -
 * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#' &lt;hexstring&gt;
 * |'"' &lt;quotechar-or-pairs&gt; '"' <br> - &lt;pairs-or-strings&gt; ::= '\'
 * &lt;pairchar&gt; &lt;pairs-or-strings&gt; | &lt;stringchar&gt;
 * &lt;pairs-or-strings&gt; | e <br> - &lt;quotechar-or-pairs&gt; ::=
 * &lt;quotechar&gt; &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
 * &lt;quotechar-or-pairs&gt; | e <br> - &lt;pairchar&gt; ::= ',' | '=' | '+' |
 * '&lt;' | '&gt;' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F] <br> -
 * &lt;hexstring&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; <br> -
 * &lt;hexpairs&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; | e <br> -
 * &lt;digits&gt; ::= [0-9] &lt;digits&gt; | e <br> - &lt;stringchar&gt; ::=
 * [0x00-0xFF] - [,=+&lt;&gt;#;\"\n\r] <br> - &lt;quotechar&gt; ::= [0x00-0xFF] -
 * [\"] <br> - &lt;separator&gt; ::= ',' | ';' <br> - &lt;spaces&gt; ::= ' '
 * &lt;spaces&gt; | e <br>
 * <br>
 * A RDN is a part of a DN. It can be composed of many types, as in the RDN
 * following RDN :<br>
 * ou=value + cn=other value<br>
 * <br>
 * In this case, we have to store an 'ou' and a 'cn' in the RDN.<br>
 * <br>
 * The types are case insensitive. <br>
 * Spaces before and after types and values are not stored.<br>
 * Spaces before and after '+' are not stored.<br>
 * <br>
 * Thus, we can consider that the following RDNs are equals :<br>
 * <br>
 * 'ou=test 1'<br> ' ou=test 1'<br>
 * 'ou =test 1'<br>
 * 'ou= test 1'<br>
 * 'ou=test 1 '<br> ' ou = test 1 '<br>
 * <br>
 * So are the following :<br>
 * <br>
 * 'ou=test 1+cn=test 2'<br>
 * 'ou = test 1 + cn = test 2'<br> ' ou =test 1+ cn =test 2 ' <br>
 * 'cn = test 2 +ou = test 1'<br>
 * <br>
 * but the following are not equal :<br>
 * 'ou=test 1' <br>
 * 'ou=test 1'<br>
 * because we have more than one spaces inside the value.<br>
 * <br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnParser
{
    /**
     * Parse this rule : <br>
     * <p>
     * &lt;oidValue&gt; ::= [0-9] &lt;digits&gt; &lt;oids&gt;
     * </p>
     *
     * @param bytes The bytes array to parse
     * @param pos The current position in the byte buffer
     * @return The new position in the vyte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    private static String parseOidValue( byte[] bytes, Position pos )
    {
        pos.start += pos.length;
        pos.end = pos.start;

        // <attributType> ::= [0-9] <digits> <oids>
        if ( StringTools.isDigit( bytes, pos.start ) == false )
        {
            // Nope... An error
            return null;
        }
        else
        {
            // Let's process an oid
            pos.end++;

            while ( StringTools.isDigit( bytes, pos.end ) )
            {
                pos.end++;
            }

            // <oids> ::= '.' [0-9] <digits> <oids> | e
            if ( StringTools.isCharASCII( bytes, pos.end, '.' ) == false )
            {
                return null;
            }
            else
            {
                do
                {
                    pos.end++;

                    if ( StringTools.isDigit( bytes, pos.end ) == false )
                    {
                        return null;
                    }
                    else
                    {
                        pos.end++;

                        while ( StringTools.isDigit( bytes, pos.end ) )
                        {
                            pos.end++;
                        }
                    }

                }
                while ( StringTools.isCharASCII( bytes, pos.end, '.' ) );

                return StringTools.utf8ToString( bytes, pos.start - pos.length, pos.end - pos.start + pos.length );
            }
        }
    }

    
    /**
     * Validate this rule : <br>
     * <p>
     * &lt;oidValue&gt; ::= [0-9] &lt;digits&gt; &lt;oids&gt;
     * </p>
     *
     * @param bytes The byte array to parse
     * @param pos The current position in the byte buffer
     * @return <code>true</code> if this is a valid OID
     */
    private static boolean isValidOidValue( byte[] bytes, Position pos )
    {
        pos.start += pos.length;
        pos.end = pos.start;

        // <attributType> ::= [0-9] <digits> <oids>
        if ( StringTools.isDigit( bytes, pos.start ) == false )
        {
            // Nope... An error
            return false;
        }
        else
        {
            // Let's process an oid
            pos.end++;

            while ( StringTools.isDigit( bytes, pos.end ) )
            {
                pos.end++;
            }

            // <oids> ::= '.' [0-9] <digits> <oids> | e
            if ( StringTools.isCharASCII( bytes, pos.end, '.' ) == false )
            {
                return false;
            }
            else
            {
                do
                {
                    pos.end++;

                    if ( StringTools.isDigit( bytes, pos.end ) == false )
                    {
                        return false;
                    }
                    else
                    {
                        pos.end++;

                        while ( StringTools.isDigit( bytes, pos.end ) )
                        {
                            pos.end++;
                        }
                    }

                }
                while ( StringTools.isCharASCII( bytes, pos.end, '.' ) );

                return true;
            }
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;oidPrefix&gt; ::= 'OID.' | 'oid.' | e
     * </p>
     *
     * @param bytes The buffer to parse
     * @param pos The current position in the byte array
     * @return The new position in the byte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    private static int parseOidPrefix( byte[] bytes, Position pos )
    {
        if ( StringTools.isICharASCII( bytes, pos.start, 'O' ) &&
             StringTools.isICharASCII( bytes, pos.start + 1, 'I' ) && 
             StringTools.isICharASCII( bytes, pos.start + 2, 'D' ) &&
             StringTools.isICharASCII( bytes, pos.start + 3, '.' ) )
        {
            pos.end += DNUtils.OID_LOWER.length();
            return DNUtils.PARSING_OK;
        }
        else
        {
            return DNUtils.PARSING_ERROR;
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;attributType&gt; ::= [a-zA-Z] &lt;keychars&gt; | &lt;oidPrefix&gt;
     * [0-9] &lt;digits&gt; &lt;oids&gt; | [0-9] &lt;digits&gt; &lt;oids&gt;
     * </p>
     * The string *MUST* be an ASCII string, not an unicode string.
     *
     * @param bytes The byte array to parse
     * @param pos The current position in the byte array
     * @return The new position in the byte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    private static String parseAttributeType( byte[] bytes, Position pos )
    {
        // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9] <digits>
        // <oids> | [0-9] <digits> <oids>
        if ( StringTools.isAlphaASCII( bytes, pos.start ) )
        {
            // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9]
            // <digits> <oids>

            // We have got an Alpha char, it may be the begining of an OID ?
            if ( parseOidPrefix( bytes, pos ) != DNUtils.PARSING_ERROR )
            {
                pos.length = 4;

                return parseOidValue( bytes, pos );
            }
            else
            {
                // It's not an oid, it's a String (ASCII)
                // <attributType> ::= [a-zA-Z] <keychars>
                // <keychars> ::= [a-zA-Z] <keychar> | [0-9] <keychar> | '-'
                // <keychar> | e
                pos.end++;

                while ( StringTools.isAlphaDigitMinus( bytes, pos.end ) )
                {
                    pos.end++;
                }

                return StringTools.utf8ToString( bytes, pos.start, pos.end - pos.start );
            }
        }
        else
        {
            // An oid
            // <attributType> ::= [0-9] <digits> <oids>
            return parseOidValue( bytes, pos );
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;attributType&gt; ::= [a-zA-Z] &lt;keychars&gt; | &lt;oidPrefix&gt;
     * [0-9] &lt;digits&gt; &lt;oids&gt; | [0-9] &lt;digits&gt; &lt;oids&gt;
     * </p>
     * The string *MUST* be an ASCII string, not an unicode string.
     *
     * @param bytes The byte array to parse
     * @param pos The current position in the byte array
     * @return The new position in the byte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    private static boolean isValidAttributeType( byte[] bytes, Position pos )
    {
        // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9] <digits>
        // <oids> | [0-9] <digits> <oids>
        if ( StringTools.isAlphaASCII( bytes, pos.start ) )
        {
            // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9]
            // <digits> <oids>

            // We have got an Alpha char, it may be the begining of an OID ?
            if ( parseOidPrefix( bytes, pos ) != DNUtils.PARSING_ERROR )
            {
                pos.length = 4;

                return isValidOidValue( bytes, pos );
            }
            else
            {
                // It's not an oid, it's a String (ASCII)
                // <attributType> ::= [a-zA-Z] <keychars>
                // <keychars> ::= [a-zA-Z] <keychar> | [0-9] <keychar> | '-'
                // <keychar> | e
                pos.end++;

                while ( StringTools.isAlphaDigitMinus( bytes, pos.end ) )
                {
                    pos.end++;
                }

                return true;
            }
        }
        else
        {
            // An oid
            // <attributType> ::= [0-9] <digits> <oids>
            return isValidOidValue( bytes, pos );
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#'
     *     &lt;hexstring&gt; |'"' &lt;quotechar-or-pairs&gt; '"' <br>
     * &lt;pairs-or-strings&gt; ::= '\' &lt;pairchar&gt;
     * &lt;pairs-or-strings&gt; | &lt;stringchar&gt; &lt;pairs-or-strings&gt; | |
     * e <br>
     * &lt;quotechar-or-pairs&gt; ::= &lt;quotechar&gt;
     * &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
     * &lt;quotechar-or-pairs&gt; | e <br>
     * </p>
     *
     * @param bytes The byte array to parse
     * @param pos The current position in the byte array
     * @return The new position in the byte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    private static Object parseAttributeValue( byte[] bytes, Position pos )
    {
        //StringBuffer sb = new StringBuffer();
        byte c = bytes[pos.start];
        byte[] buffer = new byte[bytes.length];
        int currPos = 0;

        if ( c == '#' )
        {
            pos.start++;
            int nbHex = 0;
            int currentPos = pos.start;

            // First, we will count the number of hexPairs
            while ( DNUtils.parseHexPair( bytes, currentPos ) >= 0 )
            {
                nbHex++;
                currentPos += DNUtils.TWO_CHARS;
            }

            byte[] hexValue = new byte[nbHex];

            // Now, convert the value
            // <attributeValue> ::= '#' <hexstring>
            if ( DNUtils.parseHexString( bytes, hexValue, pos ) == DNUtils.PARSING_ERROR )
            {
                return null;
            }

            pos.start--;
            StringTools.trimRight( bytes, pos );
            pos.length = pos.end - pos.start;

            return hexValue;
        }
        else if ( c == '"' )
        {
            pos.start++;
            pos.length = 0;
            pos.end = pos.start;
            int nbBytes = 0;

            // <attributeValue> ::= '"' <quotechar-or-pair> '"'
            // <quotechar-or-pairs> ::= <quotechar> <quotechar-or-pairs> | '\'
            //                                                  <pairchar> <quotechar-or-pairs> | e
            while ( true )
            {
                if ( StringTools.isCharASCII( bytes, pos.end, '\\' ) )
                {
                    pos.end++;
                    int nbChars = 0;

                    if ( ( nbChars = DNUtils.countPairChar( bytes, pos.start ) ) != DNUtils.PARSING_ERROR )
                    {
                        pos.end += nbChars;
                    }
                    else
                    {
                        return null;
                    }
                }
                else if ( ( nbBytes = DNUtils.isQuoteChar( bytes, pos.end ) ) != DNUtils.PARSING_ERROR )
                {
                    pos.end += nbBytes;
                }
                else
                {
                    pos.length = pos.end - pos.start;
                    break;
                }
            }

            if ( StringTools.isCharASCII( bytes, pos.end, '"' ) )
            {
                pos.end++;
                return StringTools.utf8ToString( bytes, pos.start, pos.length );
            }
            else
            {
                return null;
            }
        }
        else
        {
            int escapedSpace = -1;
            boolean hasPairChar = false;

            while ( true )
            {
                if ( StringTools.isCharASCII( bytes, pos.end, '\\' ) )
                {
                    // '\' <pairchar> <pairs-or-strings>
                    pos.end++;

                    int nbChars = 0;
                    if ( ( nbChars = DNUtils.countPairChar( bytes, pos.end ) ) == DNUtils.PARSING_ERROR )
                    {
                        return null;
                    }
                    else
                    {
                        if ( nbChars == 1 )
                        {
                            buffer[currPos++] = bytes[pos.end];
                        }
                        else
                        {
                            if ( hasPairChar == false )
                            {
                                hasPairChar = true;
                            }

                            byte b = StringTools.getHexValue( bytes[pos.end], bytes[pos.end + 1] );

                            buffer[currPos++] = b;
                        }

                        if ( bytes[pos.end] == ' ' )
                        {
                            escapedSpace = currPos;
                        }
                        else
                        {
                            escapedSpace = -1;
                        }

                        pos.end += nbChars;
                    }
                }
                else
                {
                    int nbChars = 0;

                    // <stringchar> <pairs-or-strings>
                    if ( ( nbChars = DNUtils.isStringChar( bytes, pos.end ) ) != DNUtils.PARSING_ERROR )
                    {
                        // A special case : if we have some spaces before the
                        // '+' character,
                        // we MUST skip them.
                        if ( StringTools.isCharASCII( bytes, pos.end, '+' ) )
                        {
                            //StringTools.trimLeft( string, pos );

                            if ( ( DNUtils.isStringChar( bytes, pos.end ) == DNUtils.PARSING_ERROR )
                                && ( StringTools.isCharASCII( bytes, pos.end, '\\' ) == false ) )
                            {
                                // Ok, we are done with the stringchar.
                                String result = StringTools.trimRight( StringTools.utf8ToString( bytes, pos.start, pos.start + pos.length ) );
                                
                                return result;
                            }
                            else
                            {
                                for ( int i = 0; i < nbChars; i++ )
                                {
                                    buffer[currPos++] = bytes[pos.end];
                                    pos.end ++;
                                }
                            }
                        }
                        else
                        {
                            for ( int i = 0; i < nbChars; i++ )
                            {
                                buffer[currPos++] = bytes[pos.end];
                                pos.end ++;
                            }
                        }
                    }
                    else
                    {
                        pos.length = pos.end - pos.start;
                        
                        if ( escapedSpace == -1 )
                        {
                            String result = StringTools.trimRight( StringTools.utf8ToString( buffer, currPos ) );
                            return result;
                        }
                        
                        String result = StringTools.utf8ToString( buffer, escapedSpace );

                        return result;
                    }
                }
            }
        }
    }


    /**
     * Validate this rule : <br>
     * <p>
     * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#'
     *     &lt;hexstring&gt; |'"' &lt;quotechar-or-pairs&gt; '"' <br>
     * &lt;pairs-or-strings&gt; ::= '\' &lt;pairchar&gt;
     * &lt;pairs-or-strings&gt; | &lt;stringchar&gt; &lt;pairs-or-strings&gt; | |
     * e <br>
     * &lt;quotechar-or-pairs&gt; ::= &lt;quotechar&gt;
     * &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
     * &lt;quotechar-or-pairs&gt; | e <br>
     * </p>
     *
     * @param bytes The byte array to parse
     * @param pos The current position in the byte array
     * @return <code>true</code> if the rule is valid
     */
    private static boolean isValidAttributeValue( byte[] bytes, Position pos )
    {
        byte c = bytes[pos.start];

        if ( c == '#' )
        {
            pos.start++;
            int nbHex = 0;
            int currentPos = pos.start;

            // First, we will count the number of hexPairs
            while ( DNUtils.parseHexPair( bytes, currentPos ) >= 0 )
            {
                nbHex++;
                currentPos += DNUtils.TWO_CHARS;
            }

            byte[] hexValue = new byte[nbHex];

            // Now, convert the value
            // <attributeValue> ::= '#' <hexstring>
            if ( DNUtils.parseHexString( bytes, hexValue, pos ) == DNUtils.PARSING_ERROR )
            {
                return false;
            }

            pos.start--;
            StringTools.trimRight( bytes, pos );
            pos.length = pos.end - pos.start;

            return true;
        }
        else if ( c == '"' )
        {
            pos.start++;
            pos.length = 0;
            pos.end = pos.start;
            int nbBytes = 0;

            // <attributeValue> ::= '"' <quotechar-or-pair> '"'
            // <quotechar-or-pairs> ::= <quotechar> <quotechar-or-pairs> | '\'
            //                                                  <pairchar> <quotechar-or-pairs> | e
            while ( true )
            {
                if ( StringTools.isCharASCII( bytes, pos.end, '\\' ) )
                {
                    pos.end++;
                    int nbChars = 0;

                    if ( ( nbChars = DNUtils.countPairChar( bytes, pos.start ) ) != DNUtils.PARSING_ERROR )
                    {
                        pos.end += nbChars;
                    }
                    else
                    {
                        return false;
                    }
                }
                else if ( ( nbBytes = DNUtils.isQuoteChar( bytes, pos.end ) ) != DNUtils.PARSING_ERROR )
                {
                    pos.end += nbBytes;
                }
                else
                {
                    pos.length = pos.end - pos.start;
                    break;
                }
            }

            if ( StringTools.isCharASCII( bytes, pos.end, '"' ) )
            {
                pos.end++;
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            while ( true )
            {
                if ( StringTools.isCharASCII( bytes, pos.end, '\\' ) )
                {
                    // '\' <pairchar> <pairs-or-strings>
                    pos.end++;

                    int nbChars = 0;
                    
                    if ( ( nbChars = DNUtils.countPairChar( bytes, pos.end ) ) == DNUtils.PARSING_ERROR )
                    {
                        return false;
                    }
                    else
                    {
                        pos.end += nbChars;
                    }
                }
                else
                {
                    int nbChars = 0;

                    // <stringchar> <pairs-or-strings>
                    if ( ( nbChars = DNUtils.isStringChar( bytes, pos.end ) ) != DNUtils.PARSING_ERROR )
                    {
                        // A special case : if we have some spaces before the
                        // '+' character,
                        // we MUST skip them.
                        if ( StringTools.isCharASCII( bytes, pos.end, '+' ) )
                        {
                            //StringTools.trimLeft( string, pos );

                            if ( ( DNUtils.isStringChar( bytes, pos.end ) == DNUtils.PARSING_ERROR )
                                && ( StringTools.isCharASCII( bytes, pos.end, '\\' ) == false ) )
                            {
                                // Ok, we are done with the stringchar.
                                return true;
                            }
                            else
                            {
                                pos.end++;
                            }
                        }
                        else
                        {
                            pos.end += nbChars;
                        }
                    }
                    else
                    {
                        return true;
                    }
                }
            }
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;nameComponents&gt; ::= &lt;spaces&gt; '+' &lt;spaces&gt;
     * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
     * &lt;attributeValue&gt; &lt;nameComponents&gt; | e
     * </p>
     *
     * @param bytes The byte buffer to parse
     * @param pos The current position in the byte buffer
     * @return The new position in the byte buffer, or PARSING_ERROR if the rule
     *         does not apply to the byte buffer
     */
    private static int parseNameComponents( byte[] bytes, Position pos, Rdn rdn ) throws InvalidNameException
    {
        int newStart = 0;
        String type = null;
        Object value = null;

        while ( true )
        {
            StringTools.trimLeft( bytes, pos );

            if ( StringTools.isCharASCII( bytes, pos.end, '+' ) )
            {
                pos.start++;
            }
            else
            {
                // <attributeTypeAndValues> ::= e
                rdn.normalize();
                return DNUtils.PARSING_OK;
            }

            StringTools.trimLeft( bytes, pos );

            if ( ( type = parseAttributeType( bytes, pos ) ) == null )
            {
                return DNUtils.PARSING_ERROR;
            }

            pos.start = pos.end;

            StringTools.trimLeft( bytes, pos );

            if ( StringTools.isCharASCII( bytes, pos.end, '=' ) )
            {
                pos.start++;
            }
            else
            {
                return DNUtils.PARSING_ERROR;
            }

            StringTools.trimLeft( bytes, pos );

            value = parseAttributeValue( bytes, pos );

            newStart = pos.end;

            if ( value != null )
            {
                if ( rdn != null )
                {
                    rdn.addAttributeTypeAndValue( type, type, value, value );
                }
            }

            pos.start = newStart;
            pos.end = newStart;
        }
    }


    /**
     * Validate this rule : <br>
     * <p>
     * &lt;nameComponents&gt; ::= &lt;spaces&gt; '+' &lt;spaces&gt;
     * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
     * &lt;attributeValue&gt; &lt;nameComponents&gt; | e
     * </p>
     *
     * @param bytes The byte buffer to parse
     * @param pos The current position in the byte buffer
     * @return <code>true</code> if the rule is valid
     */
    private static boolean isValidNameComponents( byte[] bytes, Position pos, boolean isFirstRdn )
    {
        int newStart = 0;

        while ( true )
        {
            StringTools.trimLeft( bytes, pos );

            if ( StringTools.isCharASCII( bytes, pos.end, '+' ) )
            {
                pos.start++;
            }
            else
            {
                // <attributeTypeAndValues> ::= e
                return true;
            }

            StringTools.trimLeft( bytes, pos );

            if ( !isValidAttributeType( bytes, pos ) )
            {
                return false;
            }

            pos.start = pos.end;

            StringTools.trimLeft( bytes, pos );

            if ( StringTools.isCharASCII( bytes, pos.end, '=' ) )
            {
                pos.start++;
            }
            else
            {
                return false;
            }

            StringTools.trimLeft( bytes, pos );

            if ( !isValidAttributeValue( bytes, pos ) )
            {
                return false;
            }

            newStart = pos.end;
            pos.start = newStart;
            pos.end = newStart;
        }
    }


    /**
     * Parse a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The String to parse
     * @param pos The current position in the buffer
     * @param rdn The constructed RDN
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    public static int parse( String dn, Position pos, Rdn rdn ) throws InvalidNameException
    {
        return parse( StringTools.getBytesUtf8( dn ), pos, rdn );
    }


    /**
     * Parse a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The byte array to parse
     * @param pos The current position in the buffer
     * @param rdn The constructed RDN
     * @return The new position in the byte array, or PARSING_ERROR if the rule
     *         does not apply to the byte array
     */
    public static int parse( byte[] dn, Position pos, Rdn rdn ) throws InvalidNameException
    {
        String type = null;
        Object value = null;
        int start = pos.start;

        StringTools.trimLeft( dn, pos );

        pos.end = pos.start;
        pos.length = 0;
        
        if ( ( type = parseAttributeType( dn, pos ) ) == null )
        {
            return DNUtils.PARSING_ERROR;
        }

        if ( rdn != null )
        {
            pos.start = pos.end;
        }

        StringTools.trimLeft( dn, pos );

        if ( StringTools.isCharASCII( dn, pos.start, '=' ) == false )
        {
            return DNUtils.PARSING_ERROR;
        }
        else
        {
            pos.start++;
        }

        StringTools.trimLeft( dn, pos );

        pos.end = pos.start;

        if ( ( value = parseAttributeValue( dn, pos ) ) == null )
        {
            return DNUtils.PARSING_ERROR;
        }

        if ( rdn != null )
        {
            rdn.addAttributeTypeAndValue( type, type, value, value );
            rdn.normalize();

            pos.start = pos.end;
            pos.length = 0;
        }

        if ( parseNameComponents( dn, pos, rdn ) == DNUtils.PARSING_ERROR )
        {
            return DNUtils.PARSING_ERROR;
        }
        
        rdn.setUpName( StringTools.utf8ToString( dn, start, pos.end - start ) );
        pos.start = pos.end;
        return DNUtils.PARSING_OK;
    }


    /**
     * Validate a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The String to parse
     * @param pos The current position in the buffer
     * @return <code>true</code> if the RDN is valid
     */
    public static boolean isValid( String dn, Position pos, boolean isfirstRdn )
    {
        return isValid( StringTools.getBytesUtf8( dn ), pos, isfirstRdn );
    }


    /**
     * Validate a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The byte array to parse
     * @param pos The current position in the buffer
     * @return <code>true</code> if the RDN is valid
     */
    public static boolean isValid( byte[] dn, Position pos, boolean isfirstRdn )
    {
        StringTools.trimLeft( dn, pos );

        pos.end = pos.start;
        pos.length = 0;
        
        if ( !isValidAttributeType( dn, pos ) )
        {
            return false;
        }

        pos.start = pos.end;

        StringTools.trimLeft( dn, pos );

        if ( StringTools.isCharASCII( dn, pos.start, '=' ) == false )
        {
            return false;
        }
        else
        {
            pos.start++;
        }

        StringTools.trimLeft( dn, pos );

        pos.end = pos.start;

        if ( !isValidAttributeValue( dn, pos ) )
        {
            return false;
        }

        pos.start = pos.end;
        pos.length = 0;

        if ( !isValidNameComponents( dn, pos, isfirstRdn )  )
        {
            return false;
        }
        
        pos.start = pos.end;
        return true;
    }


    /**
     * Parse a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The String to parse
     * @param rdn The RDN to fill. Beware that if the RDN is not empty, the new
     *            AttributeTypeAndValue will be added.
     */
    public static void parse( String dn, Rdn rdn ) throws InvalidNameException
    {
        parse( StringTools.getBytesUtf8( dn ), new Position(), rdn );
    }

    /**
     * Validtae a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     *
     * @param dn The string to parse
     * @return <code>true</code> if the RDN is valid
     */
    public static boolean isValid( String dn )
    {
        return isValid( StringTools.getBytesUtf8( dn ), new Position(), false );
    }
}
