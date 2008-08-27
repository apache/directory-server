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


import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.codec.util.HttpClientError;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.codec.util.URIException;
import org.apache.directory.shared.ldap.codec.util.UrlDecoderException;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Decodes a LdapUrl, and checks that it complies with
 * the RFC 2255. The grammar is the following :
 * ldapurl    = scheme "://" [hostport] ["/"
 *                   [dn ["?" [attributes] ["?" [scope]
 *                   ["?" [filter] ["?" extensions]]]]]]
 * scheme     = "ldap"
 * attributes = attrdesc *("," attrdesc)
 * scope      = "base" / "one" / "sub"
 * dn         = LdapDN
 * hostport   = hostport from Section 5 of RFC 1738
 * attrdesc   = AttributeDescription from Section 4.1.5 of RFC 2251
 * filter     = filter from Section 4 of RFC 2254
 * extensions = extension *("," extension)
 * extension  = ["!"] extype ["=" exvalue]
 * extype     = token / xtoken
 * exvalue    = LDAPString
 * token      = oid from section 4.1 of RFC 2252
 * xtoken     = ("X-" / "x-") token
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class LdapURL
{

    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The constant for "ldaps://" scheme. */
    public static final String LDAPS_SCHEME = "ldaps://";

    /** The constant for "ldap://" scheme. */
    public static final String LDAP_SCHEME = "ldap://";

    /** A null LdapURL */
    public static final LdapURL EMPTY_URL = new LdapURL();

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
    private List<String> attributes;

    /** The scope */
    private int scope;

    /** The filter as a string */
    private String filter;

    /** The extensions. */
    private List<Extension> extensionList;

    /** Stores the LdapURL as a String */
    private String string;

    /** Stores the LdapURL as a byte array */
    private byte[] bytes;

    /** modal parameter that forces explicit scope rendering in toString */
    private boolean forceScopeRendering;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Construct an empty LdapURL
     */
    public LdapURL()
    {
        scheme = LDAP_SCHEME;
        host = null;
        port = -1;
        dn = null;
        attributes = new ArrayList<String>();
        scope = SearchControls.OBJECT_SCOPE;
        filter = null;
        extensionList = new ArrayList<Extension>( 2 );
    }


    /**
     * Parse a LdapURL
     * @param chars The chars containing the URL
     * @throws LdapURLEncodingException If the URL is invalid
     */
    public void parse( char[] chars ) throws LdapURLEncodingException
    {
        scheme = LDAP_SCHEME;
        host = null;
        port = -1;
        dn = null;
        attributes = new ArrayList<String>();
        scope = SearchControls.OBJECT_SCOPE;
        filter = null;
        extensionList = new ArrayList<Extension>( 2 );

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
        if ( ( ( pos = StringTools.areEquals( chars, 0, LDAP_SCHEME ) ) == StringTools.NOT_EQUAL )
            && ( ( pos = StringTools.areEquals( chars, 0, LDAPS_SCHEME ) ) == StringTools.NOT_EQUAL ) )
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
        if ( !StringTools.isCharASCII( chars, pos, '/' ) )
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
        if ( !StringTools.isCharASCII( chars, pos, '?' ) )
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
        if ( !StringTools.isCharASCII( chars, pos, '?' ) )
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
        if ( !StringTools.isCharASCII( chars, pos, '?' ) )
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
        if ( !StringTools.isCharASCII( chars, pos, '?' ) )
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
     * @param string TheString that contains the LDAPURL
     * @throws LdapURLEncodingException If the String does not comply with RFC 2255
     */
    public LdapURL( String string ) throws LdapURLEncodingException
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
     * @param bytes The byte buffer that contains the LDAPURL
     * @throws LdapURLEncodingException If the byte array does not comply with RFC 2255
     */
    public LdapURL( byte[] bytes ) throws LdapURLEncodingException
    {
        if ( ( bytes == null ) || ( bytes.length == 0 ) )
        {
            throw new LdapURLEncodingException( "The byte array is empty : this is not a valid LdapURL." );
        }

        string = StringTools.utf8ToString( bytes );

        this.bytes = new byte[bytes.length];
        System.arraycopy( bytes, 0, this.bytes, 0, bytes.length );

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
     * @param chars The buffer to parse
     * @param pos The current position in the byte buffer
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
     * @param chars The buffer to parse
     * @param pos The current position in the byte buffer
     * @return The new position in the byte buffer, or -1 if the rule does not
     *         apply to the byte buffer
     */
    private int parsePort( char[] chars, int pos )
    {

        if ( !StringTools.isDigit( chars, pos ) )
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
     * @param chars The char array to parse
     * @param pos The current position in the byte buffer
     * @return The new position in the byte buffer, or -1 if the rule does not
     *         apply to the byte buffer
     */
    private int parseHostPort( char[] chars, int pos )
    {
        int hostPos = pos;

        if ( ( pos = parseHost( chars, pos ) ) == -1 )
        {
            return -1;
        }

        // We may have a port.
        if ( StringTools.isCharASCII( chars, pos, ':' ) )
        {
            if ( pos == hostPos )
            {
                // We should not have a port if we have no host
                return -1;
            }

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
     * @param data the byte array to be encoded
     * @param offset the index of the first byte to encode
     * @param length the number of bytes to encode
     * @param charset the desired character encoding
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
     * @param data the byte array to be encoded
     * @param charset the desired character encoding
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
     * @param data the string to be encoded
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
     * @param bytes array of URL safe characters
     * @return array of original bytes
     * @throws UrlDecoderException Thrown if URL decoding is unsuccessful
     */
    private static final byte[] decodeUrl( byte[] bytes ) throws UrlDecoderException
    {
        if ( bytes == null )
        {
            return StringTools.EMPTY_BYTES;
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
     * @param escaped a string
     * @return the unescaped string
     * @throws URIException if the string cannot be decoded (invalid)
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
     * @param chars The char array to be checked
     * @param pos the starting position
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
     * @param chars The char array to be checked
     * @param pos the starting position
     * @return -1 if the char array does not contains attributes
     */
    private int parseAttributes( char[] chars, int pos )
    {

        int start = pos;
        int end = pos;
        Set<String> hAttributes = new HashSet<String>();
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

                        if ( !hAttributes.contains( decodedAttr ) )
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

                if ( !hAttributes.contains( decodedAttr ) )
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
     * @param chars The char array to be checked
     * @param pos the starting position
     * @return -1 if the char array does not contains a filter
     */
    private int parseFilter( char[] chars, int pos )
    {

        int end = pos;

        for ( int i = pos; ( i < chars.length ) && ( chars[i] != '?' ); i++ )
        {
            end++;
        }

        if ( end == pos )
        {
            // We have no filter
            return end;
        }

        try
        {
            filter = decode( new String( chars, pos, end - pos ) );
            FilterParser.parse( filter );
        }
        catch ( URIException ue )
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
     * @param chars The char array to be checked
     * @param pos the starting position
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
        else if ( pos == chars.length )
        {
            // An empty scope at the end of the URL. This is valid
            return pos;
        }

        // The scope is not one of "one", "sub" or "base". It's an error
        return -1;
    }


    /**
     * Parse extensions and critical extensions. 
     * 
     * The grammar is : 
     * extensions ::= extension [ ',' extension ]* 
     * extension ::= [ '!' ] ( token | ( 'x-' | 'X-' ) token ) ) [ '=' exvalue ]
     * 
     * @param chars The char array to be checked
     * @param pos the starting position
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
                        if ( extension == null )
                        {
                            extension = decode( new String( chars, start, i - start ) ).trim();
                        }
                        else
                        {
                            value = decode( new String( chars, start, i - start ) ).trim();
                        }

                        Extension ext = new Extension( isCritical, extension, value );
                        extensionList.add( ext );

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
                    extension = decode( new String( chars, start, i - start ) ).trim();

                    if ( extension.length() == 0 )
                    {
                        // We must have an extension
                        return -1;
                    }

                    hasValue = true;
                    start = i + 1;
                }
                else if ( StringTools.isCharASCII( chars, i, '!' ) )
                {
                    if ( !isNewExtension )
                    {
                        // '!' must appears first
                        return -1;
                    }

                    isCritical = true;
                    start++;
                }
                else
                {
                    isNewExtension = false;
                }
            }

            if ( extension == null )
            {
                extension = decode( new String( chars, start, chars.length - start ) ).trim();
            }
            else
            {
                value = decode( new String( chars, start, chars.length - start ) ).trim();
            }

            Extension ext = new Extension( isCritical, extension, value );
            extensionList.add( ext );

            return chars.length;
        }
        catch ( URIException ue )
        {
            return -1;
        }
    }


    /**
     * Encode a String to avoid special characters 
     * 
     * *NOTE* : this is an ugly function, just needed because the RFC 2255 
     * is VERY unclear about the way LDAP searches are to be encoded. 
     * 
     * Some references to RFC 1738 are made, but they are really useless 
     * and inadequat.
     * 
     * @param url The String to encode
     * @param doubleEncode Set if we need to encode the comma
     * @return An encoded string
     */
    public static String urlEncode( String url, boolean doubleEncode )
    {
        StringBuffer sb = new StringBuffer();

        for ( int i = 0; i < url.length(); i++ )
        {
            char c = url.charAt( i );

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
                    break;
            }
        }

        return sb.toString();
    }


    /**
     * Get a string representation of a LdapURL.
     * 
     * @return A LdapURL string
     * @see LdapURL#forceScopeRendering
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( scheme );

        sb.append( ( host == null ) ? "" : host );

        if ( port != -1 )
        {
            sb.append( ':' ).append( port );
        }

        if ( dn != null )
        {
            sb.append( '/' ).append( urlEncode( dn.toString(), false ) );

            if ( attributes.size() != 0 || forceScopeRendering
                || ( ( scope != SearchControls.OBJECT_SCOPE ) || ( filter != null ) || ( extensionList.size() != 0 ) ) )
            {
                sb.append( '?' );

                boolean isFirst = true;

                for ( String attribute : attributes )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        sb.append( ',' );
                    }

                    sb.append( urlEncode( attribute, false ) );
                }
            }

            if ( forceScopeRendering )
            {
                sb.append( '?' );

                switch ( scope )
                {

                    case SearchControls.OBJECT_SCOPE:
                        sb.append( "base" );
                        break;

                    case SearchControls.ONELEVEL_SCOPE:
                        sb.append( "one" );
                        break;

                    case SearchControls.SUBTREE_SCOPE:
                        sb.append( "sub" );
                        break;

                    default:
                        break;
                }
            }

            else
            {
                if ( ( scope != SearchControls.OBJECT_SCOPE ) || ( filter != null ) || ( extensionList.size() != 0 ) )
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

                        default:
                            break;
                    }

                    if ( ( filter != null ) || ( ( extensionList.size() != 0 ) ) )
                    {
                        sb.append( "?" );

                        if ( filter != null )
                        {
                            sb.append( urlEncode( filter, false ) );
                        }

                        if ( ( extensionList.size() != 0 ) )
                        {
                            sb.append( '?' );

                            boolean isFirst = true;

                            if ( extensionList.size() != 0 )
                            {
                                for ( Extension extension : extensionList )
                                {
                                    if ( !isFirst )
                                    {
                                        sb.append( ',' );
                                    }
                                    else
                                    {
                                        isFirst = false;
                                    }

                                    if ( extension.isCritical )
                                    {
                                        sb.append( '!' );
                                    }
                                    sb.append( urlEncode( extension.type, false ) );

                                    if ( extension.value != null )
                                    {
                                        sb.append( '=' );
                                        sb.append( urlEncode( extension.value, true ) );
                                    }
                                }
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
    public List<String> getAttributes()
    {
        return attributes;
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
    public List<Extension> getExtensions()
    {
        return extensionList;
    }


    /**
     * Gets the extension.
     * 
     * @param type the extension type, case-insensitive
     * 
     * @return Returns the extension, null if this URL does not contain 
     *         such an extension.
     */
    public Extension getExtension( String type )
    {
        for ( Extension extension : getExtensions() )
        {
            if ( extension.getType().equalsIgnoreCase( type ) )
            {
                return extension;
            }
        }
        return null;
    }


    /**
     * Gets the extension value.
     * 
     * @param type the extension type, case-insensitive
     * 
     * @return Returns the extension value, null if this URL does not  
     *         contain such an extension or if the extension value is null.
     */
    public String getExtensionValue( String type )
    {
        for ( Extension extension : getExtensions() )
        {
            if ( extension.getType().equalsIgnoreCase( type ) )
            {
                return extension.getValue();
            }
        }
        return null;
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
     * Returns the scope, one of {@link SearchControls.OBJECT_SCOPE}, 
     * {@link SearchControls.ONELEVEL_SCOPE} or {@link SearchControls.SUBTREE_SCOPE}.
     * 
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


    /**
     * @return the number of bytes for this LdapURL
     */
    public int getNbBytes()
    {
        return ( bytes != null ? bytes.length : 0 );
    }


    /**
     * @return a reference on the interned bytes representing this LdapURL
     */
    public byte[] getBytesReference()
    {
        return bytes;
    }


    /**
     * @return a copy of the bytes representing this LdapURL
     */
    public byte[] getBytesCopy()
    {
        if ( bytes != null )
        {
            byte[] copy = new byte[bytes.length];
            System.arraycopy( bytes, 0, copy, 0, bytes.length );
            return copy;
        }
        else
        {
            return null;
        }
    }


    /**
     * @return the LdapURL as a String
     */
    public String getString()
    {
        return string;
    }


    /**
     * Compute the instance's hash code
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        return this.toString().hashCode();
    }


    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final LdapURL other = ( LdapURL ) obj;
        return this.toString().equals( other.toString() );
    }


    /**
     * Sets the scheme. Must be "ldap://" or "ldaps://", otherwise "ldap://" is assumed as default.
     * 
     * @param scheme the new scheme
     */
    public void setScheme( String scheme )
    {
        if ( scheme != null && LDAP_SCHEME.equals( scheme ) || LDAPS_SCHEME.equals( scheme ) )
        {
            this.scheme = scheme;
        }
        else
        {
            this.scheme = LDAP_SCHEME;
        }

    }


    /**
     * Sets the host.
     * 
     * @param host the new host
     */
    public void setHost( String host )
    {
        this.host = host;
    }


    /**
     * Sets the port. Must be between 1 and 65535, otherwise -1 is assumed as default.
     * 
     * @param port the new port
     */
    public void setPort( int port )
    {
        if ( port < 1 || port > 65535 )
        {
            this.port = -1;
        }
        else
        {
            this.port = port;
        }
    }


    /**
     * Sets the dn.
     * 
     * @param dn the new dn
     */
    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }


    /**
     * Sets the attributes, null removes all existing attributes.
     * 
     * @param attributes the new attributes
     */
    public void setAttributes( List<String> attributes )
    {
        if ( attributes == null )
        {
            this.attributes.clear();
        }
        else
        {
            this.attributes = attributes;
        }
    }


    /**
     * Sets the scope. Must be one of {@link SearchControls.OBJECT_SCOPE}, 
     * {@link SearchControls.ONELEVEL_SCOPE} or {@link SearchControls.SUBTREE_SCOPE},
     * otherwise {@link SearchControls.OBJECT_SCOPE} is assumed as default.
     * 
     * @param scope the new scope
     */
    public void setScope( int scope )
    {
        if ( scope == SearchControls.ONELEVEL_SCOPE || scope == SearchControls.SUBTREE_SCOPE )
        {
            this.scope = scope;
        }
        else
        {
            this.scope = SearchControls.OBJECT_SCOPE;
        }
    }


    /**
     * Sets the filter.
     * 
     * @param filter the new filter
     */
    public void setFilter( String filter )
    {
        this.filter = filter;
    }


    /**
     * If set to true forces the toString method to render the scope 
     * regardless of optional nature.  Use this when you want explicit
     * search URL scope rendering.
     * 
     * @param forceScopeRendering the forceScopeRendering to set
     */
    public void setForceScopeRendering( boolean forceScopeRendering )
    {
        this.forceScopeRendering = forceScopeRendering;
    }


    /**
     * If set to true forces the toString method to render the scope 
     * regardless of optional nature.  Use this when you want explicit
     * search URL scope rendering.
     * 
     * @return the forceScopeRendering
     */
    public boolean isForceScopeRendering()
    {
        return forceScopeRendering;
    }

    /**
     * An inner bean to hold extension information.
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public static class Extension
    {
        private boolean isCritical;
        private String type;
        private String value;


        /**
         * Creates a new instance of Extension.
         *
         * @param isCritical true for critical extension
         * @param type the extension type
         * @param value the extension value
         */
        public Extension( boolean isCritical, String type, String value )
        {
            super();
            this.isCritical = isCritical;
            this.type = type;
            this.value = value;
        }


        /**
         * Checks if is critical.
         * 
         * @return true, if is critical
         */
        public boolean isCritical()
        {
            return isCritical;
        }


        /**
         * Sets the critical.
         * 
         * @param isCritical the new critical
         */
        public void setCritical( boolean isCritical )
        {
            this.isCritical = isCritical;
        }


        /**
         * Gets the type.
         * 
         * @return the type
         */
        public String getType()
        {
            return type;
        }


        /**
         * Sets the type.
         * 
         * @param type the new type
         */
        public void setType( String type )
        {
            this.type = type;
        }


        /**
         * Gets the value.
         * 
         * @return the value
         */
        public String getValue()
        {
            return value;
        }


        /**
         * Sets the value.
         * 
         * @param value the new value
         */
        public void setValue( String value )
        {
            this.value = value;
        }
    }

}
