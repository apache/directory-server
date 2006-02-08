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
package org.apache.directory.shared.ldap.name;


import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.util.DNUtils;
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
     * @param chars
     *            The char array to parse
     * @param pos
     *            The current position in the byte buffer
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    private static int parseOidValue( char[] chars, int pos )
    {
        // <attributType> ::= [0-9] <digits> <oids>
        if ( StringTools.isDigit( chars, pos ) == false )
        {
            // Nope... An error
            return DNUtils.PARSING_ERROR;
        }
        else
        {
            // Let's process an oid
            pos++;

            while ( StringTools.isDigit( chars, pos ) )
            {
                pos++;
            }

            // <oids> ::= '.' [0-9] <digits> <oids> | e
            if ( StringTools.isCharASCII( chars, pos, '.' ) == false )
            {
                return pos;
            }
            else
            {
                do
                {
                    pos++;

                    if ( StringTools.isDigit( chars, pos ) == false )
                    {
                        return DNUtils.PARSING_ERROR;
                    }
                    else
                    {
                        pos++;

                        while ( StringTools.isDigit( chars, pos ) )
                        {
                            pos++;
                        }
                    }
                }
                while ( StringTools.isCharASCII( chars, pos, '.' ) );

                return pos;
            }
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;oidPrefix&gt; ::= 'OID.' | 'oid.' | e
     * </p>
     * 
     * @param bytes
     *            The buffer to parse
     * @param pos
     *            The current position in the char array
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    private static int parseOidPrefix( char[] chars, int pos )
    {
        if ( ( StringTools.areEquals( chars, pos, DNUtils.OID_LOWER ) == DNUtils.PARSING_ERROR )
            && ( StringTools.areEquals( chars, pos, DNUtils.OID_UPPER ) == DNUtils.PARSING_ERROR ) )
        {
            return DNUtils.PARSING_ERROR;
        }
        else
        {
            pos += DNUtils.OID_LOWER.length;

            return pos;
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
     * @param chars
     *            The char array to parse
     * @param pos
     *            The current position in the char array
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    private static int parseAttributeType( char[] chars, int pos )
    {
        // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9] <digits>
        // <oids> | [0-9] <digits> <oids>

        if ( StringTools.isAlphaASCII( chars, pos ) )
        {
            // <attributType> ::= [a-zA-Z] <keychars> | <oidPrefix> [0-9]
            // <digits> <oids>

            // We have got an Alpha char, it may be the begining of an OID ?
            int oldPos = pos;

            if ( ( pos = parseOidPrefix( chars, oldPos ) ) != DNUtils.PARSING_ERROR )
            {
                return parseOidValue( chars, pos );
            }
            else
            {
                // It's not an oid, it's a String (ASCII)
                // <attributType> ::= [a-zA-Z] <keychars>
                // <keychars> ::= [a-zA-Z] <keychar> | [0-9] <keychar> | '-'
                // <keychar> | e
                pos = oldPos + 1;

                while ( StringTools.isAlphaDigitMinus( chars, pos ) )
                {
                    pos++;
                }

                return pos;
            }
        }
        else
        {

            // An oid
            // <attributType> ::= [0-9] <digits> <oids>
            return parseOidValue( chars, pos );
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#'
     * &lt;hexstring&gt; |'"' &lt;quotechar-or-pairs&gt; '"' <br>
     * &lt;pairs-or-strings&gt; ::= '\' &lt;pairchar&gt;
     * &lt;pairs-or-strings&gt; | &lt;stringchar&gt; &lt;pairs-or-strings&gt; | |
     * e <br>
     * &lt;quotechar-or-pairs&gt; ::= &lt;quotechar&gt;
     * &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
     * &lt;quotechar-or-pairs&gt; | e <br>
     * </p>
     * 
     * @param chars
     *            The char array to parse
     * @param pos
     *            The current position in the char array
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    private static int parseAttributeValue( char[] chars, int pos )
    {
        if ( StringTools.isCharASCII( chars, pos, '#' ) )
        {
            pos++;

            // <attributeValue> ::= '#' <hexstring>
            if ( ( pos = DNUtils.parseHexString( chars, pos ) ) == DNUtils.PARSING_ERROR )
            {

                return DNUtils.PARSING_ERROR;
            }

            return StringTools.trimLeft( chars, pos );
        }
        else if ( StringTools.isCharASCII( chars, pos, '"' ) )
        {
            pos++;
            int nbBytes = 0;

            // <attributeValue> ::= '"' <quotechar-or-pair> '"'
            // <quotechar-or-pairs> ::= <quotechar> <quotechar-or-pairs> | '\'
            // <pairchar> <quotechar-or-pairs> | e
            while ( true )
            {
                if ( StringTools.isCharASCII( chars, pos, '\\' ) )
                {
                    pos++;

                    if ( DNUtils.isPairChar( chars, pos ) )
                    {
                        pos++;
                    }
                    else
                    {
                        return DNUtils.PARSING_ERROR;
                    }
                }
                else if ( ( nbBytes = DNUtils.isQuoteChar( chars, pos ) ) != DNUtils.PARSING_ERROR )
                {
                    pos += nbBytes;
                }
                else
                {
                    break;
                }
            }

            if ( StringTools.isCharASCII( chars, pos, '"' ) )
            {
                pos++;

                return StringTools.trimLeft( chars, pos );
            }
            else
            {
                return DNUtils.PARSING_ERROR;
            }
        }
        else
        {
            while ( true )
            {
                if ( StringTools.isCharASCII( chars, pos, '\\' ) )
                {
                    // '\' <pairchar> <pairs-or-strings>
                    pos++;

                    if ( DNUtils.isPairChar( chars, pos ) == false )
                    {
                        return DNUtils.PARSING_ERROR;
                    }
                    else
                    {
                        pos++;
                    }
                }
                else
                {
                    int nbChars = 0;

                    // <stringchar> <pairs-or-strings>
                    if ( ( nbChars = DNUtils.isStringChar( chars, pos ) ) != DNUtils.PARSING_ERROR )
                    {
                        // A special case : if we have some spaces before the
                        // '+' character,
                        // we MUST skip them.
                        if ( StringTools.isCharASCII( chars, pos, ' ' ) )
                        {
                            pos = StringTools.trimLeft( chars, pos );

                            if ( ( DNUtils.isStringChar( chars, pos ) == DNUtils.PARSING_ERROR )
                                && ( StringTools.isCharASCII( chars, pos, '\\' ) == false ) )
                            {
                                // Ok, we are done with the stringchar.
                                return pos;
                            }
                        }
                        else
                        {
                            // An unicode char could be more than one byte long
                            pos += nbChars;
                        }
                    }
                    else
                    {
                        return pos;
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
     * @param chars
     *            The char buffer to parse
     * @param pos
     *            The current position in the byte buffer
     * @return The new position in the char buffer, or PARSING_ERROR if the rule
     *         does not apply to the char buffer
     */
    private static int parseNameComponents( char[] chars, int pos, Rdn rdn ) throws InvalidNameException
    {
        int newPos = 0;
        String type = null;
        String value = null;

        while ( true )
        {
            pos = StringTools.trimLeft( chars, pos );

            if ( StringTools.isCharASCII( chars, pos, '+' ) )
            {
                pos++;
            }
            else
            {
                // <attributeTypeAndValues> ::= e
                return pos;
            }

            pos = StringTools.trimLeft( chars, pos );

            if ( ( newPos = parseAttributeType( chars, pos ) ) == DNUtils.PARSING_ERROR )
            {
                return DNUtils.PARSING_ERROR;
            }

            if ( rdn != null )
            {
                type = new String( chars, pos, newPos - pos );
            }

            pos = StringTools.trimLeft( chars, newPos );

            if ( StringTools.isCharASCII( chars, pos, '=' ) )
            {
                pos++;
            }
            else
            {
                return DNUtils.PARSING_ERROR;
            }

            pos = StringTools.trimLeft( chars, pos );

            newPos = parseAttributeValue( chars, pos );

            if ( newPos != DNUtils.PARSING_ERROR )
            {
                if ( rdn != null )
                {
                    newPos = StringTools.trimRight( chars, newPos );
                    value = new String( chars, pos, newPos - pos );

                    rdn.addAttributeTypeAndValue( type, value );
                }
            }

            pos = newPos;
        }
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#'
     * &lt;hexstring&gt; |'"' &lt;quotechar-or-pairs&gt; '"' <br>
     * &lt;pairs-or-strings&gt; ::= '\' &lt;pairchar&gt;
     * &lt;pairs-or-strings&gt; | &lt;stringchar&gt; &lt;pairs-or-strings&gt; | |
     * e <br>
     * &lt;quotechar-or-pairs&gt; ::= &lt;quotechar&gt;
     * &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
     * &lt;quotechar-or-pairs&gt; | e <br>
     * </p>
     * 
     * @param chars
     *            The char array to parse
     * @param pos
     *            The current position in the char array
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    public static int unescapeValue( String value ) throws IllegalArgumentException
    {
        char[] chars = value.toCharArray();
        int pos = 0;

        if ( StringTools.isCharASCII( chars, pos, '#' ) )
        {
            pos++;

            // <attributeValue> ::= '#' <hexstring>
            if ( ( pos = DNUtils.parseHexString( chars, pos ) ) == DNUtils.PARSING_ERROR )
            {

                throw new IllegalArgumentException();
            }

            return StringTools.trimLeft( chars, pos );
        }
        else if ( StringTools.isCharASCII( chars, pos, '"' ) )
        {
            pos++;
            int nbBytes = 0;

            // <attributeValue> ::= '"' <quotechar-or-pair> '"'
            // <quotechar-or-pairs> ::= <quotechar> <quotechar-or-pairs> | '\'
            // <pairchar> <quotechar-or-pairs> | e
            while ( true )
            {
                if ( StringTools.isCharASCII( chars, pos, '\\' ) )
                {
                    pos++;

                    if ( DNUtils.isPairChar( chars, pos ) )
                    {
                        pos++;
                    }
                    else
                    {
                        return DNUtils.PARSING_ERROR;
                    }
                }
                else if ( ( nbBytes = DNUtils.isQuoteChar( chars, pos ) ) != DNUtils.PARSING_ERROR )
                {
                    pos += nbBytes;
                }
                else
                {
                    break;
                }
            }

            if ( StringTools.isCharASCII( chars, pos, '"' ) )
            {
                pos++;

                return StringTools.trimLeft( chars, pos );
            }
            else
            {
                return DNUtils.PARSING_ERROR;
            }
        }
        else
        {
            while ( true )
            {
                if ( StringTools.isCharASCII( chars, pos, '\\' ) )
                {
                    // '\' <pairchar> <pairs-or-strings>
                    pos++;

                    if ( DNUtils.isPairChar( chars, pos ) == false )
                    {
                        return DNUtils.PARSING_ERROR;
                    }
                    else
                    {
                        pos++;
                    }
                }
                else
                {
                    int nbChars = 0;

                    // <stringchar> <pairs-or-strings>
                    if ( ( nbChars = DNUtils.isStringChar( chars, pos ) ) != DNUtils.PARSING_ERROR )
                    {
                        // A special case : if we have some spaces before the
                        // '+' character,
                        // we MUST skip them.
                        if ( StringTools.isCharASCII( chars, pos, ' ' ) )
                        {
                            pos = StringTools.trimLeft( chars, pos );

                            if ( ( DNUtils.isStringChar( chars, pos ) == DNUtils.PARSING_ERROR )
                                && ( StringTools.isCharASCII( chars, pos, '\\' ) == false ) )
                            {
                                // Ok, we are done with the stringchar.
                                return pos;
                            }
                        }
                        else
                        {
                            // An unicode char could be more than one byte long
                            pos += nbChars;
                        }
                    }
                    else
                    {
                        return pos;
                    }
                }
            }
        }
    }


    /**
     * Parse a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     * 
     * @param bytes
     *            The buffer to parse
     * @param pos
     *            The current position in the buffer
     * @return The new position in the char array, or PARSING_ERROR if the rule
     *         does not apply to the char array
     */
    public static int parse( char[] chars, int pos, Rdn rdn ) throws InvalidNameException
    {
        int newPos = 0;
        String type = null;
        String value = null;
        int start = pos;

        pos = StringTools.trimLeft( chars, pos );

        if ( ( newPos = parseAttributeType( chars, pos ) ) == DNUtils.PARSING_ERROR )
        {
            return DNUtils.PARSING_ERROR;
        }

        if ( rdn != null )
        {
            type = new String( chars, pos, newPos - pos );
        }

        pos = StringTools.trimLeft( chars, newPos );

        if ( StringTools.isCharASCII( chars, pos, '=' ) == false )
        {
            return DNUtils.PARSING_ERROR;
        }
        else
        {
            pos++;
        }

        pos = StringTools.trimLeft( chars, pos );

        if ( ( newPos = parseAttributeValue( chars, pos ) ) == DNUtils.PARSING_ERROR )
        {
            return DNUtils.PARSING_ERROR;
        }

        if ( rdn != null )
        {
            newPos = StringTools.trimRight( chars, newPos );
            value = new String( chars, pos, newPos - pos );

            rdn.addAttributeTypeAndValue( type, value );
        }

        int end = parseNameComponents( chars, newPos, rdn );
        rdn.setUpName( new String( chars, start, end - start ) );
        rdn.normalizeString();
        return end;
    }


    /**
     * Parse a NameComponent : <br>
     * <p>
     * &lt;name-component&gt; ::= &lt;attributeType&gt; &lt;spaces&gt; '='
     * &lt;spaces&gt; &lt;attributeValue&gt; &lt;nameComponents&gt;
     * </p>
     * 
     * @param string
     *            The buffer to parse
     * @param rdn
     *            The RDN to fill. Beware that if the RDN is not empty, the new
     *            AttributeTypeAndValue will be added.
     */
    public static void parse( String string, Rdn rdn ) throws InvalidNameException
    {
        parse( string.toCharArray(), 0, rdn );
        rdn.normalizeString();
    }
}
