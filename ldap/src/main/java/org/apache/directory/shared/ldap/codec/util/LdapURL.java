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
package org.apache.directory.shared.ldap.codec.util;


import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.InvalidNameException;
import javax.naming.directory.SearchControls;


/**
 * Decodes a LdapUrl, and checks that it complies with the RFC 2255. The grammar
 * is the following : ldapurl = scheme "://" [hostport] ["/" [dn ["?"
 * [attributes] ["?" [scope] ["?" [filter] ["?" extensions]]]]]] scheme = "ldap"
 * attributes = attrdesc *("," attrdesc) scope = "base" / "one" / "sub" dn =
 * LdapDN hostport = hostport from Section 5 of RFC 1738 attrdesc =
 * AttributeDescription from Section 4.1.5 of RFC 2251 filter = filter from
 * Section 4 of RFC 2254 extensions = extension *("," extension) extension =
 * ["!"] extype ["=" exvalue] extype = token / xtoken exvalue = LDAPString token =
 * oid from section 4.1 of RFC 2252 xtoken = ("X-" / "x-") token
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapURL extends LdapString
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** A null LdapURL */
    public static final transient LdapURL EMPTY_URL = new LdapURL();

    /** The filter parser */
    private static FilterParserImpl filterParser = new FilterParserImpl();

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The scheme */
    private String scheme;

    /** The host */
    private String host;

    /** The port */
    private int port;

    /** The DN */
    private LdapDN dn;

    /** The attributes */
    private ArrayList attributes;

    /** The scope */
    private int scope;

    /** The filter as a string */
    private String filter;

    /** The extensions */
    private HashMap extensions;

    /** The criticals extensions */
    private HashMap criticalExtensions;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Construct an empty LdapURL
     */
    public LdapURL()
    {
        super();
        host = null;
        port = -1;
        dn = null;
        attributes = new ArrayList();
        scope = SearchControls.OBJECT_SCOPE;
        filter = null;
        extensions = new HashMap();
        criticalExtensions = new HashMap();
    }


    public void parse( char[] chars ) throws LdapURLEncodingException
    {
        host = null;
        port = -1;
        dn = null;
        attributes = new ArrayList();
        scope = SearchControls.OBJECT_SCOPE;
        filter = null;
        extensions = new HashMap();
        criticalExtensions = new HashMap();

        if ( ( chars == null ) || ( chars.length == 0 ) )
        {
            host = "";
            return;
        }

        // ldapurl = scheme "://" [hostport] ["/"
        // [dn ["?" [attributes] ["?" [scope]
        // ["?" [filter] ["?" extensions]]]]]]
        // scheme = "ldap"

        int pos = 0;

        // The scheme
        if ( ( ( pos = StringTools.areEquals( chars, 0, "ldap://" ) ) == StringTools.NOT_EQUAL )
            && ( ( pos = StringTools.areEquals( chars, 0, "ldaps://" ) ) == StringTools.NOT_EQUAL ) )
        {
            throw new LdapURLEncodingException( "A LdapUrl must start with \"ldap://\" or \"ldaps://\"" );
        }
        else
        {
            scheme = new String( chars, 0, pos );
        }

        // The hostport
        if ( ( pos = parseHostPort( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "The hostport is invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }

        // An optional '/'
        if ( StringTools.isCharASCII( chars, pos, '/' ) == false )
        {
            throw new LdapURLEncodingException( "Bad character, position " + pos + ", '" + chars[pos]
                + "', '/' expected" );
        }

        pos++;

        if ( pos == chars.length )
        {
            return;
        }

        // An optional DN
        if ( ( pos = parseDN( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "The DN is invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }

        // Optionals attributes
        if ( StringTools.isCharASCII( chars, pos, '?' ) == false )
        {
            throw new LdapURLEncodingException( "Bad character, position " + pos + ", '" + chars[pos]
                + "', '?' expected" );
        }

        pos++;

        if ( ( pos = parseAttributes( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "Attributes are invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }

        // Optional scope
        if ( StringTools.isCharASCII( chars, pos, '?' ) == false )
        {
            throw new LdapURLEncodingException( "Bad character, position " + pos + ", '" + chars[pos]
                + "', '?' expected" );
        }

        pos++;

        if ( ( pos = parseScope( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "Scope is invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }

        // Optional filter
        if ( StringTools.isCharASCII( chars, pos, '?' ) == false )
        {
            throw new LdapURLEncodingException( "Bad character, position " + pos + ", '" + chars[pos]
                + "', '?' expected" );
        }

        pos++;

        if ( pos == chars.length )
        {
            return;
        }

        if ( ( pos = parseFilter( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "Filter is invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }

        // Optional extensions
        if ( StringTools.isCharASCII( chars, pos, '?' ) == false )
        {
            throw new LdapURLEncodingException( "Bad character, position " + pos + ", '" + chars[pos]
                + "', '?' expected" );
        }

        pos++;

        if ( ( pos = parseExtensions( chars, pos ) ) == -1 )
        {
            throw new LdapURLEncodingException( "Extensions are invalid" );
        }

        if ( pos == chars.length )
        {
            return;
        }
        else
        {
            throw new LdapURLEncodingException( "Invalid character at the end of the ldapUrl" );
        }
    }


    /**
     * Create a new LdapURL from a String after having parsed it.
     * 
     * @param string
     *            TheString that contains the LDAPURL
     * @return A MutableString containing the LDAPURL
     * @throws DecoderException
     *             If the String does not comply with RFC 2255
     */
    public LdapURL(String string) throws LdapURLEncodingException
    {
        if ( string == null )
        {
            throw new LdapURLEncodingException( "The string is empty : this is not a valid LdapURL." );
        }

        try
        {
            bytes = string.getBytes( "UTF-8" );
            this.string = string;
            parse( string.toCharArray() );
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new LdapURLEncodingException( "Bad Ldap URL : " + string );
        }
    }


    /**
     * Create a new LdapURL after having parsed it.
     * 
     * @param bytes
     *            The byte buffer that contains the LDAPURL
     * @return A MutableString containing the LDAPURL
     * @throws DecoderException
     *             If the byte array does not comply with RFC 2255
     */
    public LdapURL(byte[] bytes) throws LdapURLEncodingException
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) )
        {
            throw new LdapURLEncodingException( "The byte array is empty : this is not a valid LdapURL." );
        }

        try
        {
            string = new String( bytes, "UTF-8" );
            this.bytes = bytes;
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new LdapURLEncodingException( "The byte array is not an UTF-8 encoded Unicode String : "
                + uee.getMessage() );
        }

        parse( string.toCharArray() );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Parse this rule : <br>
     * <p>
     * &lt;host&gt; ::= &lt;hostname&gt; ':' &lt;hostnumber&gt;<br>
     * &lt;hostname&gt; ::= *[ &lt;domainlabel&gt; "." ] &lt;toplabel&gt;<br>
     * &lt;domainlabel&gt; ::= &lt;alphadigit&gt; | &lt;alphadigit&gt; *[
     * &lt;alphadigit&gt; | "-" ] &lt;alphadigit&gt;<br>
     * &lt;toplabel&gt; ::= &lt;alpha&gt; | &lt;alpha&gt; *[ &lt;alphadigit&gt; |
     * "-" ] &lt;alphadigit&gt;<br>
     * &lt;hostnumber&gt; ::= &lt;digits&gt; "." &lt;digits&gt; "."
     * &lt;digits&gt; "." &lt;digits&gt;
     * </p>
     * 
     * @param chars
     *            The buffer to parse
     * @param pos
     *            The current position in the byte buffer
     * @return The new position in the byte buffer, or -1 if the rule does not
     *         apply to the byte buffer TODO check that the topLabel is valid
     *         (it must start with an alpha)
     */
    private int parseHost( char[] chars, int pos )
    {

        int start = pos;
        boolean hadDot = false;
        boolean hadMinus = false;
        boolean isHostNumber = true;
        boolean invalidIp = false;
        int nbDots = 0;
        int[] ipElem = new int[4];

        // The host will be followed by a '/' or a ':', or by nothing if it's
        // the end.
        // We will search the end of the host part, and we will check some
        // elements.
        if ( StringTools.isCharASCII( chars, pos, '-' ) )
        {

            // We can't have a '-' on first position
            return -1;
        }

        while ( ( pos < chars.length ) && ( chars[pos] != ':' ) && ( chars[pos] != '/' ) )
        {

            if ( StringTools.isCharASCII( chars, pos, '.' ) )
            {

                if ( ( hadMinus ) || ( hadDot ) )
                {

                    // We already had a '.' just before : this is not allowed.
                    // Or we had a '-' before a '.' : ths is not allowed either.
                    return -1;
                }

                // Let's check the string we had before the dot.
                if ( isHostNumber )
                {

                    if ( nbDots < 4 )
                    {

                        // We had only digits. It may be an IP adress? Check it
                        if ( ipElem[nbDots] > 65535 )
                        {
                            invalidIp = true;
                        }
                    }
                }

                hadDot = true;
                nbDots++;
                pos++;
                continue;
            }
            else
            {

                if ( hadDot && StringTools.isCharASCII( chars, pos, '-' ) )
                {

                    // We can't have a '-' just after a '.'
                    return -1;
                }

                hadDot = false;
            }

            if ( StringTools.isDigit( chars, pos ) )
            {

                if ( isHostNumber && ( nbDots < 4 ) )
                {
                    ipElem[nbDots] = ( ipElem[nbDots] * 10 ) + ( chars[pos] - '0' );

                    if ( ipElem[nbDots] > 65535 )
                    {
                        invalidIp = true;
                    }
                }

                hadMinus = false;
            }
            else if ( StringTools.isAlphaDigitMinus( chars, pos ) )
            {
                isHostNumber = false;

                if ( StringTools.isCharASCII( chars, pos, '-' ) )
                {
                    hadMinus = true;
                }
                else
                {
                    hadMinus = false;
                }
            }
            else
            {
                return -1;
            }

            pos++;
        }

        if ( start == pos )
        {

            // An empty host is valid
            return pos;
        }

        // Checks the hostNumber
        if ( isHostNumber )
        {

            // As this is a host number, we must have 3 dots.
            if ( nbDots != 3 )
            {
                return -1;
            }

            if ( invalidIp )
            {
                return -1;
            }
        }

        // Check if we have a '.' or a '-' in last position
        if ( hadDot || hadMinus )
        {
            return -1;
        }

        host = new String( chars, start, pos - start );

        return pos;
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;port&gt; ::= &lt;digits&gt;<br>
     * &lt;digits&gt; ::= &lt;digit&gt; &lt;digits-or-null&gt;<br>
     * &lt;digits-or-null&gt; ::= &lt;digit&gt; &lt;digits-or-null&gt; | e<br>
     * &lt;digit&gt; ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
     * </p>
     * The port must be between 0 and 65535.
     * 
     * @param chars
     *            The buffer to parse
     * @param pos
     *            The current position in the byte buffer
     * @return The new position in the byte buffer, or -1 if the rule does not
     *         apply to the byte buffer
     */
    private int parsePort( char[] chars, int pos )
    {

        if ( StringTools.isDigit( chars, pos ) == false )
        {
            return -1;
        }

        port = chars[pos] - '0';

        pos++;

        while ( StringTools.isDigit( chars, pos ) )
        {
            port = ( port * 10 ) + ( chars[pos] - '0' );

            if ( port > 65535 )
            {
                return -1;
            }

            pos++;
        }

        return pos;
    }


    /**
     * Parse this rule : <br>
     * <p>
     * &lt;hostport&gt; ::= &lt;host&gt; ':' &lt;port&gt;
     * </p>
     * 
     * @param chars
     *            The char array to parse
     * @param pos
     *            The current position in the byte buffer
     * @return The new position in the byte buffer, or -1 if the rule does not
     *         apply to the byte buffer
     */
    private int parseHostPort( char[] chars, int pos )
    {

        if ( ( pos = parseHost( chars, pos ) ) == -1 )
        {
            return -1;
        }

        // We may have a port.
        if ( StringTools.isCharASCII( chars, pos, ':' ) )
        {
            pos++;
        }
        else
        {
            return pos;
        }

        // As we have a ':', we must have a valid port (between 0 and 65535).
        if ( ( pos = parsePort( chars, pos ) ) == -1 )
        {
            return -1;
        }

        return pos;
    }


    /**
     * From commons-httpclients. Converts the byte array of HTTP content
     * characters to a string. If the specified charset is not supported,
     * default system encoding is used.
     * 
     * @param data
     *            the byte array to be encoded
     * @param offset
     *            the index of the first byte to encode
     * @param length
     *            the number of bytes to encode
     * @param charset
     *            the desired character encoding
     * @return The result of the conversion.
     * @since 3.0
     */
    public static String getString( final byte[] data, int offset, int length, String charset )
    {
        if ( data == null )
        {
            throw new IllegalArgumentException( "Parameter may not be null" );
        }

        if ( charset == null || charset.length() == 0 )
        {
            throw new IllegalArgumentException( "charset may not be null or empty" );
        }

        try
        {
            return new String( data, offset, length, charset );
        }
        catch ( UnsupportedEncodingException e )
        {
            return new String( data, offset, length );
        }
    }


    /**
     * From commons-httpclients. Converts the byte array of HTTP content
     * characters to a string. If the specified charset is not supported,
     * default system encoding is used.
     * 
     * @param data
     *            the byte array to be encoded
     * @param charset
     *            the desired character encoding
     * @return The result of the conversion.
     * @since 3.0
     */
    public static String getString( final byte[] data, String charset )
    {
        return getString( data, 0, data.length, charset );
    }


    /**
     * Converts the specified string to byte array of ASCII characters.
     * 
     * @param data
     *            the string to be encoded
     * @return The string as a byte array.
     * @since 3.0
     */
    public static byte[] getAsciiBytes( final String data )
    {

        if ( data == null )
        {
            throw new IllegalArgumentException( "Parameter may not be null" );
        }

        try
        {
            return data.getBytes( "US-ASCII" );
        }
        catch ( UnsupportedEncodingException e )
        {
            throw new HttpClientError( "HttpClient requires ASCII support" );
        }
    }


    /**
     * From commons-codec. Decodes an array of URL safe 7-bit characters into an
     * array of original bytes. Escaped characters are converted back to their
     * original representation.
     * 
     * @param bytes
     *            array of URL safe characters
     * @return array of original bytes
     * @throws DecoderException
     *             Thrown if URL decoding is unsuccessful
     */
    private static final byte[] decodeUrl( byte[] bytes ) throws UrlDecoderException
    {
        if ( bytes == null )
        {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        for ( int i = 0; i < bytes.length; i++ )
        {
            int b = bytes[i];

            if ( b == '+' )
            {
                buffer.write( ' ' );
            }
            else if ( b == '%' )
            {
                try
                {
                    int u = Character.digit( ( char ) bytes[++i], 16 );
                    int l = Character.digit( ( char ) bytes[++i], 16 );

                    if ( u == -1 || l == -1 )
                    {
                        throw new UrlDecoderException( "Invalid URL encoding" );
                    }

                    buffer.write( ( char ) ( ( u << 4 ) + l ) );
                }
                catch ( ArrayIndexOutOfBoundsException e )
                {
                    throw new UrlDecoderException( "Invalid URL encoding" );
                }
            }
            else
            {
                buffer.write( b );
            }
        }

        return buffer.toByteArray();
    }


    /**
     * From commons-httpclients. Unescape and decode a given string regarded as
     * an escaped string with the default protocol charset.
     * 
     * @param escaped
     *            a string
     * @return the unescaped string
     * @throws URIException
     *             if the string cannot be decoded (invalid)
     * @see URI#getDefaultProtocolCharset
     */
    private static String decode( String escaped ) throws URIException
    {
        try
        {
            byte[] rawdata = decodeUrl( getAsciiBytes( escaped ) );
            return getString( rawdata, "UTF-8" );
        }
        catch ( UrlDecoderException e )
        {
            throw new URIException( e.getMessage() );
        }
    }


    /**
     * Parse a string and check that it complies with RFC 2253. Here, we will
     * just call the LdapDN parser to do the job.
     * 
     * @param chars
     *            The char array to be checked
     * @param pos
     *            the starting position
     * @return -1 if the char array does not contains a DN
     */
    private int parseDN( char[] chars, int pos )
    {

        int end = pos;

        for ( int i = pos; ( i < chars.length ) && ( chars[i] != '?' ); i++ )
        {
            end++;
        }

        try
        {
            dn = new LdapDN( decode( new String( chars, pos, end - pos ) ) );
        }
        catch ( URIException ue )
        {
            return -1;
        }
        catch ( InvalidNameException de )
        {
            return -1;
        }

        return end;
    }


    /**
     * Parse the attributes part
     * 
     * @param chars
     *            The char array to be checked
     * @param pos
     *            the starting position
     * @return -1 if the char array does not contains attributes
     */
    private int parseAttributes( char[] chars, int pos )
    {

        int start = pos;
        int end = pos;
        HashSet hAttributes = new HashSet();
        boolean hadComma = false;

        try
        {

            for ( int i = pos; ( i < chars.length ) && ( chars[i] != '?' ); i++ )
            {

                if ( StringTools.isCharASCII( chars, i, ',' ) )
                {
                    hadComma = true;

                    if ( ( end - start ) == 0 )
                    {

                        // An attributes must not be null
                        return -1;
                    }
                    else
                    {
                        String attribute = null;

                        // get the attribute. It must not be blank
                        attribute = new String( chars, start, end - start ).trim();

                        if ( attribute.length() == 0 )
                        {
                            return -1;
                        }

                        String decodedAttr = decode( attribute );

                        if ( hAttributes.contains( decodedAttr ) == false )
                        {
                            attributes.add( decodedAttr );
                            hAttributes.add( decodedAttr );
                        }
                    }

                    start = i + 1;
                }
                else
                {
                    hadComma = false;
                }

                end++;
            }

            if ( hadComma )
            {

                // We are not allowed to have a comma at the end of the
                // attributes
                return -1;
            }
            else
            {

                if ( end == start )
                {

                    // We don't have any attributes. This is valid.
                    return end;
                }

                // Store the last attribute
                // get the attribute. It must not be blank
                String attribute = null;

                attribute = new String( chars, start, end - start ).trim();

                if ( attribute.length() == 0 )
                {
                    return -1;
                }

                String decodedAttr = decode( attribute );

                if ( hAttributes.contains( decodedAttr ) == false )
                {
                    attributes.add( decodedAttr );
                    hAttributes.add( decodedAttr );
                }
            }

            return end;
        }
        catch ( URIException ue )
        {
            return -1;
        }
    }


    /**
     * Parse the filter part. We will use the FilterParserImpl class
     * 
     * @param chars
     *            The char array to be checked
     * @param pos
     *            the starting position
     * @return -1 if the char array does not contains a filter
     */
    private int parseFilter( char[] chars, int pos )
    {

        int end = pos;

        for ( int i = pos; ( i < chars.length ) && ( chars[i] != '?' ); i++ )
        {
            end++;
        }

        try
        {
            filter = decode( new String( chars, pos, end - pos ) );
            filterParser.parse( filter );
        }
        catch ( URIException ue )
        {
            return -1;
        }
        catch ( IOException ioe )
        {
            return -1;
        }
        catch ( ParseException pe )
        {
            return -1;
        }

        return end;
    }


    /**
     * Parse the scope part.
     * 
     * @param chars
     *            The char array to be checked
     * @param pos
     *            the starting position
     * @return -1 if the char array does not contains a scope
     */
    private int parseScope( char[] chars, int pos )
    {

        if ( StringTools.isCharASCII( chars, pos, 'b' ) || StringTools.isCharASCII( chars, pos, 'B' ) )
        {
            pos++;

            if ( StringTools.isCharASCII( chars, pos, 'a' ) || StringTools.isCharASCII( chars, pos, 'A' ) )
            {
                pos++;

                if ( StringTools.isCharASCII( chars, pos, 's' ) || StringTools.isCharASCII( chars, pos, 'S' ) )
                {
                    pos++;

                    if ( StringTools.isCharASCII( chars, pos, 'e' ) || StringTools.isCharASCII( chars, pos, 'E' ) )
                    {
                        pos++;
                        scope = SearchControls.OBJECT_SCOPE;
                        return pos;
                    }
                }
            }
        }
        else if ( StringTools.isCharASCII( chars, pos, 'o' ) || StringTools.isCharASCII( chars, pos, 'O' ) )
        {
            pos++;

            if ( StringTools.isCharASCII( chars, pos, 'n' ) || StringTools.isCharASCII( chars, pos, 'N' ) )
            {
                pos++;

                if ( StringTools.isCharASCII( chars, pos, 'e' ) || StringTools.isCharASCII( chars, pos, 'E' ) )
                {
                    pos++;

                    scope = SearchControls.ONELEVEL_SCOPE;
                    return pos;
                }
            }
        }
        else if ( StringTools.isCharASCII( chars, pos, 's' ) || StringTools.isCharASCII( chars, pos, 'S' ) )
        {
            pos++;

            if ( StringTools.isCharASCII( chars, pos, 'u' ) || StringTools.isCharASCII( chars, pos, 'U' ) )
            {
                pos++;

                if ( StringTools.isCharASCII( chars, pos, 'b' ) || StringTools.isCharASCII( chars, pos, 'B' ) )
                {
                    pos++;

                    scope = SearchControls.SUBTREE_SCOPE;
                    return pos;
                }
            }
        }
        else if ( StringTools.isCharASCII( chars, pos, '?' ) )
        {

            // An empty scope. This is valid
            return pos;
        }

        // The scope is not one of "one", "sub" or "base". It's an error
        return -1;
    }


    /**
     * Parse extensions and critical extensions. The grammar is : extensions ::=
     * extension [ ',' extension ]* extension ::= [ '!' ] ( token | ( 'x-' |
     * 'X-' ) token ) ) [ '=' exvalue ]
     * 
     * @param char
     *            The char array to be checked
     * @param pos
     *            the starting position
     * @return -1 if the char array does not contains valid extensions or
     *         critical extensions
     */
    private int parseExtensions( char[] chars, int pos )
    {

        int start = pos;
        boolean isCritical = false;
        boolean isNewExtension = true;
        boolean hasValue = false;
        String extension = null;
        String value = null;

        if ( pos == chars.length )
        {
            return pos;
        }

        try
        {

            for ( int i = pos; ( i < chars.length ); i++ )
            {

                if ( StringTools.isCharASCII( chars, i, ',' ) )
                {

                    if ( isNewExtension )
                    {

                        // a ',' is not allowed when we have already had one
                        // or if we just started to parse the extensions.
                        return -1;
                    }
                    else
                    {
                        value = new String( decode( new String( chars, start, i - start ) ) ).trim();

                        if ( value.length() == 0 )
                        {
                            return -1;
                        }

                        if ( isCritical )
                        {
                            criticalExtensions.put( extension, value );
                        }
                        else
                        {
                            extensions.put( extension, value );
                        }

                        isNewExtension = true;
                        hasValue = false;
                        isCritical = false;
                        start = i + 1;
                        extension = null;
                        value = null;
                    }
                }
                else if ( StringTools.isCharASCII( chars, i, '=' ) )
                {

                    if ( hasValue )
                    {

                        // We may have two '=' for the same extension
                        continue;
                    }

                    // An optionnal value
                    extension = new String( decode( new String( chars, start, i - start ) ) ).trim();

                    if ( extension.length() == 0 )
                    {

                        // We must have an extension
                        return -1;
                    }

                    isNewExtension = false;
                    hasValue = true;
                    start = i + 1;
                }
                else if ( StringTools.isCharASCII( chars, i, '!' ) )
                {

                    if ( isNewExtension == false )
                    {

                        // '!' must appears first
                        return -1;
                    }

                    isCritical = true;
                    start++;
                }
            }

            if ( extension == null )
            {
                extension = new String( decode( new String( chars, start, chars.length - start ) ) ).trim();
            }
            else
            {
                value = new String( decode( new String( chars, start, chars.length - start ) ) ).trim();
            }

            if ( isCritical )
            {
                criticalExtensions.put( extension, value );
            }
            else
            {
                extensions.put( extension, value );
            }

            return chars.length;
        }
        catch ( URIException ue )
        {
            return -1;
        }
    }


    /**
     * Encode a String to avoid special characters *NOTE* : this is an ugly
     * function, just needed because the RFC 2255 is VERY unclear about the way
     * LDAP searches are to be encoded. Some references to RFC 1738 are made,
     * but they are really useless and inadequat.
     * 
     * @param string
     *            The String to encode
     * @param doubleEncode
     *            Set if we need to encode the comma
     * @return An encoded string
     */
    private String urlEncode( String string, boolean doubleEncode )
    {
        StringBuffer sb = new StringBuffer();

        for ( int i = 0; i < string.length(); i++ )
        {
            char c = string.charAt( i );

            switch ( c )
            {
                case ' ':
                    sb.append( "%20" );
                    break;

                case '?':
                    sb.append( "%3f" );
                    break;

                case '\\':
                    sb.append( "%5c" );
                    break;

                case ',':
                    if ( doubleEncode )
                    {
                        sb.append( "%2c" );
                    }
                    else
                    {
                        sb.append( c );
                    }
                    break;

                default:
                    sb.append( c );
            }
        }

        return sb.toString();
    }


    /**
     * Get a string representation of a LdapURL.
     * 
     * @return A LdapURL string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer( "ldap://" );

        sb.append( ( host == null ) ? "" : host );

        if ( port != -1 )
        {
            sb.append( ':' ).append( port );
        }

        if ( dn != null )
        {
            sb.append( '/' ).append( urlEncode( dn.toString(), false ) );

            if ( ( attributes.size() != 0 )
                || ( ( scope != SearchControls.OBJECT_SCOPE ) || ( filter != null ) || ( extensions.size() != 0 ) || ( criticalExtensions
                    .size() != 0 ) ) )
            {
                sb.append( '?' );

                for ( int i = 0; i < attributes.size(); i++ )
                {

                    if ( i > 0 )
                    {
                        sb.append( ',' );
                    }

                    sb.append( urlEncode( ( String ) attributes.get( i ), false ) );
                }
            }

            if ( ( scope != SearchControls.OBJECT_SCOPE ) || ( filter != null ) || ( extensions.size() != 0 )
                || ( criticalExtensions.size() != 0 ) )
            {
                sb.append( '?' );

                switch ( scope )
                {

                    case SearchControls.OBJECT_SCOPE:

                        // This is the default value.
                        break;

                    case SearchControls.ONELEVEL_SCOPE:
                        sb.append( "one" );
                        break;

                    case SearchControls.SUBTREE_SCOPE:
                        sb.append( "sub" );
                        break;
                }

                if ( ( filter != null ) || ( ( extensions.size() != 0 ) || ( criticalExtensions.size() != 0 ) ) )
                {
                    sb.append( "?" );

                    if ( filter != null )
                    {
                        sb.append( urlEncode( filter, false ) );
                    }

                    if ( ( extensions.size() != 0 ) || ( criticalExtensions.size() != 0 ) )
                    {
                        sb.append( '?' );

                        boolean isFirst = true;

                        if ( extensions.size() != 0 )
                        {

                            Iterator keys = extensions.keySet().iterator();

                            while ( keys.hasNext() )
                            {

                                if ( isFirst == false )
                                {
                                    sb.append( ',' );
                                }
                                else
                                {
                                    isFirst = false;
                                }

                                String key = ( String ) keys.next();

                                sb.append( urlEncode( key, false ) ).append( '=' ).append(
                                    urlEncode( ( String ) extensions.get( key ), true ) );
                            }
                        }

                        isFirst = true;

                        if ( criticalExtensions.size() != 0 )
                        {

                            Iterator keys = criticalExtensions.keySet().iterator();

                            while ( keys.hasNext() )
                            {

                                if ( isFirst == false )
                                {
                                    sb.append( ",!" );
                                }
                                else
                                {
                                    sb.append( '!' );
                                    isFirst = false;
                                }

                                String key = ( String ) keys.next();

                                sb.append( urlEncode( key, false ) ).append( '=' ).append(
                                    urlEncode( ( String ) criticalExtensions.get( key ), true ) );
                            }
                        }
                    }
                }
            }
        }
        else
        {
            sb.append( '/' );
        }

        return sb.toString();
    }


    /**
     * @return Returns the attributes.
     */
    public ArrayList getAttributes()
    {
        return attributes;
    }


    /**
     * @return Returns the criticalExtensions.
     */
    public HashMap getCriticalExtensions()
    {
        return criticalExtensions;
    }


    /**
     * @return Returns the dn.
     */
    public LdapDN getDn()
    {
        return dn;
    }


    /**
     * @return Returns the extensions.
     */
    public HashMap getExtensions()
    {
        return extensions;
    }


    /**
     * @return Returns the filter.
     */
    public String getFilter()
    {
        return filter;
    }


    /**
     * @return Returns the host.
     */
    public String getHost()
    {
        return host;
    }


    /**
     * @return Returns the port.
     */
    public int getPort()
    {
        return port;
    }


    /**
     * @return Returns the scope.
     */
    public int getScope()
    {
        return scope;
    }


    /**
     * @return Returns the scheme.
     */
    public String getScheme()
    {
        return scheme;
    }
}
