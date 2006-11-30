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
package org.apache.directory.shared.ldap.ldif;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.ldap.Control;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.name.LdapDnParser;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 *  &lt;ldif-file&gt; ::= &quot;version:&quot; &lt;fill&gt; &lt;number&gt; &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;ldif-content-change&gt;
 *  
 *  &lt;ldif-content-change&gt; ::= 
 *    &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;ldif-attrval-record-e&gt; | 
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;ldif-attrval-record-e&gt; | 
 *    &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt; &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt; 
 *        &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; |
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt;
 *                              
 *  &lt;ldif-attrval-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;attributeType&gt; 
 *    &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; 
 *    &lt;ldif-attrval-record-e&gt; | e
 *                              
 *  &lt;ldif-change-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;controls-e&gt; 
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; | e
 *                              
 *  &lt;dn-spec&gt; ::= &quot;dn:&quot; &lt;fill&gt; &lt;safe-string&gt; | &quot;dn::&quot; &lt;fill&gt; &lt;base64-string&gt;
 *                              
 *  &lt;controls-e&gt; ::= &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt; 
 *    &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt; | e
 *                              
 *  &lt;criticality&gt; ::= &quot;true&quot; | &quot;false&quot; | e
 *                              
 *  &lt;oid&gt; ::= '.' &lt;number&gt; &lt;oid&gt; | e
 *                              
 *  &lt;attrval-specs-e&gt; ::= &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | 
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | e
 *                              
 *  &lt;value-spec-e&gt; ::= &lt;value-spec&gt; | e
 *  
 *  &lt;value-spec&gt; ::= ':' &lt;fill&gt; &lt;safe-string-e&gt; | 
 *    &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt; | 
 *    &quot;:&lt;&quot; &lt;fill&gt; &lt;url&gt;
 *  
 *  &lt;attributeType&gt; ::= &lt;number&gt; &lt;oid&gt; | &lt;alpha&gt; &lt;chars-e&gt;
 *  
 *  &lt;options-e&gt; ::= ';' &lt;char&gt; &lt;chars-e&gt; &lt;options-e&gt; |e
 *                              
 *  &lt;chars-e&gt; ::= &lt;char&gt; &lt;chars-e&gt; |  e
 *  
 *  &lt;changerecord-type&gt; ::= &quot;add&quot; &lt;sep&gt; &lt;attributeType&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | 
 *    &quot;delete&quot; &lt;sep&gt; | 
 *    &quot;modify&quot; &lt;sep&gt; &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; | 
 *    &quot;moddn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt; |
 *    &quot;modrdn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt;
 *  
 *  &lt;newrdn&gt; ::= ':' &lt;fill&gt; &lt;safe-string&gt; | &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt;
 *  
 *  &lt;newsuperior-e&gt; ::= &quot;newsuperior&quot; &lt;newrdn&gt; | e
 *  
 *  &lt;mod-specs-e&gt; ::= &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt; 
 *    &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; | e
 *  
 *  &lt;mod-type&gt; ::= &quot;add:&quot; | &quot;delete:&quot; | &quot;replace:&quot;
 *  
 *  &lt;url&gt; ::= &lt;a Uniform Resource Locator, as defined in [6]&gt;
 *  
 *  
 *  
 *  LEXICAL
 *  -------
 *  
 *  &lt;fill&gt;           ::= ' ' &lt;fill&gt; | e
 *  &lt;char&gt;           ::= &lt;alpha&gt; | &lt;digit&gt; | '-'
 *  &lt;number&gt;         ::= &lt;digit&gt; &lt;digits&gt;
 *  &lt;0-1&gt;            ::= '0' | '1'
 *  &lt;digits&gt;         ::= &lt;digit&gt; &lt;digits&gt; | e
 *  &lt;digit&gt;          ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
 *  &lt;seps&gt;           ::= &lt;sep&gt; &lt;seps-e&gt; 
 *  &lt;seps-e&gt;         ::= &lt;sep&gt; &lt;seps-e&gt; | e
 *  &lt;sep&gt;            ::= 0x0D 0x0A | 0x0A
 *  &lt;spaces&gt;         ::= ' ' &lt;spaces-e&gt;
 *  &lt;spaces-e&gt;       ::= ' ' &lt;spaces-e&gt; | e
 *  &lt;safe-string-e&gt;  ::= &lt;safe-string&gt; | e
 *  &lt;safe-string&gt;    ::= &lt;safe-init-char&gt; &lt;safe-chars&gt;
 *  &lt;safe-init-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B | [0x3D-0x7F]
 *  &lt;safe-chars&gt;     ::= &lt;safe-char&gt; &lt;safe-chars&gt; | e
 *  &lt;safe-char&gt;      ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
 *  &lt;base64-string&gt;  ::= &lt;base64-char&gt; &lt;base64-chars&gt;
 *  &lt;base64-chars&gt;   ::= &lt;base64-char&gt; &lt;base64-chars&gt; | e
 *  &lt;base64-char&gt;    ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-9x5A] | [0x61-0x7A]
 *  &lt;alpha&gt;          ::= [0x41-0x5A] | [0x61-0x7A]
 *  
 *  COMMENTS
 *  --------
 *  - The ldap-oid VN is not correct in the RFC-2849. It has been changed from 1*DIGIT 0*1(&quot;.&quot; 1*DIGIT) to
 *  DIGIT+ (&quot;.&quot; DIGIT+)*
 *  - The mod-spec lacks a sep between *attrval-spec and &quot;-&quot;.
 *  - The BASE64-UTF8-STRING should be BASE64-CHAR BASE64-STRING
 *  - The ValueSpec rule must accept multilines values. In this case, we have a LF followed by a 
 *  single space before the continued value.
 * </pre>
 */
public class LdifReader implements Iterator
{
    /** A logger */
    private static final Logger log = LoggerFactory.getLogger( LdifReader.class );

    /** A private class to track the current position in a line */
    private class Position
    {
        public int pos;

        public Position()
        {
            pos = 0;
        }

        public void inc()
        {
            pos++;
        }

        public void inc( int val )
        {
            pos += val;
        }
    }

    /** A list of read lines */
    private List<String> lines;

    /** The current position */
    private Position position;

    /** The ldif file version default value */
    private static final int DEFAULT_VERSION = 1;

    /** The ldif version */
    private int version;

    /** Type of element read */
    private static final int ENTRY = 0;

    private static final int CHANGE = 1;

    private static final int UNKNOWN = 2;

    /** Size limit for file contained values */
    private long sizeLimit = SIZE_LIMIT_DEFAULT;

    /** The default size limit : 1Mo */
    private static final long SIZE_LIMIT_DEFAULT = 1024000;

    /** State values for the modify operation */
    private static final int MOD_SPEC = 0;

    private static final int ATTRVAL_SPEC = 1;

    private static final int ATTRVAL_SPEC_OR_SEP = 2;

    /** Iterator prefetched entry */
    private Entry prefetched;

    /** The ldif Reader */
    private Reader in;

    /** A flag set if the ldif contains entries */
    private boolean containsEntries;

    /** A flag set if the ldif contains changes */
    private boolean containsChanges;

    /**
     * An Exception to handle error message, has Iterator.next() can't throw
     * exceptions
     */
    private Exception error;

    /**
     * Constructors
     */
    public LdifReader()
    {
        lines = new ArrayList<String>();
        position = new Position();
        version = DEFAULT_VERSION;
    }

    private void init( BufferedReader in ) throws NamingException
    {
        this.in = in;
        lines = new ArrayList<String>();
        position = new Position();
        version = DEFAULT_VERSION;
        containsChanges = false;
        containsEntries = false;

        // First get the version - if any -
        version = parseVersion();
        prefetched = parseEntry();
    }

    /**
     * A constructor which takes a file name
     * 
     * @param ldifFileName A file name containing ldif formated input
     * @throws NamingException
     *             If the file cannot be processed or if the format is incorrect
     */
    public LdifReader( String ldifFileName ) throws NamingException
    {
        File in = new File( ldifFileName );

        if ( in.exists() == false )
        {
            log.error( "File {} cannot be found", in.getAbsoluteFile() );
            throw new NamingException( "Cannot find file " + in.getAbsoluteFile() );
        }

        if ( in.canRead() == false )
        {
            log.error( "File {} cannot be read", in.getName() );
            throw new NamingException( "Cannot read file " + in.getName() );
        }

        try
        {
            init( new BufferedReader( new FileReader( in ) ) );
        }
        catch (FileNotFoundException fnfe)
        {
            log.error( "File {} cannot be found", in.getAbsoluteFile() );
            throw new NamingException( "Cannot find file " + in.getAbsoluteFile() );
        }
    }

    /**
     * A constructor which takes a BufferedReader
     * 
     * @param in
     *            A BufferedReader containing ldif formated input
     * @throws NamingException
     *             If the file cannot be processed or if the format is incorrect
     */
    public LdifReader( BufferedReader in ) throws NamingException
    {
        init( in );
    }

    /**
     * A constructor which takes a Reader
     * 
     * @param in
     *            A Reader containing ldif formated input
     * @throws NamingException
     *             If the file cannot be processed or if the format is incorrect
     */
    public LdifReader( Reader in ) throws NamingException
    {
        init( new BufferedReader( in ) );
    }

    /**
     * A constructor which takes an InputStream
     * 
     * @param in
     *            An InputStream containing ldif formated input
     * @throws NamingException
     *             If the file cannot be processed or if the format is incorrect
     */
    public LdifReader( InputStream in ) throws NamingException
    {
        init( new BufferedReader( new InputStreamReader( in ) ) );
    }

    /**
     * A constructor which takes a File
     * 
     * @param in
     *            A File containing ldif formated input
     * @throws NamingException
     *             If the file cannot be processed or if the format is incorrect
     */
    public LdifReader( File in ) throws NamingException
    {
        if ( in.exists() == false )
        {
            log.error( "File {} cannot be found", in.getAbsoluteFile() );
            throw new NamingException( "Cannot find file " + in.getAbsoluteFile() );
        }

        if ( in.canRead() == false )
        {
            log.error( "File {} cannot be read", in.getName() );
            throw new NamingException( "Cannot read file " + in.getName() );
        }

        try
        {
            init( new BufferedReader( new FileReader( in ) ) );
        }
        catch (FileNotFoundException fnfe)
        {
            log.error( "File {} cannot be found", in.getAbsoluteFile() );
            throw new NamingException( "Cannot find file " + in.getAbsoluteFile() );
        }
    }

    /**
     * @return The ldif file version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * @return The maximum size of a file which is used into an attribute value.
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }

    /**
     * Set the maximum file size that can be accepted for an attribute value
     * 
     * @param sizeLimit
     *            The size in bytes
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }

    // <fill> ::= ' ' <fill> | �
    private static void parseFill( char[] document, Position position )
    {

        while ( StringTools.isCharASCII( document, position.pos, ' ' ) )
        {
            position.inc();
        }
    }

    /**
     * Parse a number following the rules :
     * 
     * &lt;number&gt; ::= &lt;digit&gt; &lt;digits&gt; &lt;digits&gt; ::= &lt;digit&gt; &lt;digits&gt; | e &lt;digit&gt;
     * ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
     * 
     * Check that the number is in the interval
     */
    private static String parseNumber( char[] document, Position position )
    {
        int initPos = position.pos;

        while ( true )
        {
            if ( StringTools.isDigit( document, position.pos ) )
            {
                position.inc();
            }
            else
            {
                break;
            }
        }

        if ( position.pos == initPos )
        {
            return null;
        }
        else
        {
            return new String( document, initPos, position.pos - initPos );
        }
    }

    /**
     * Parse the changeType
     * 
     * @param line
     *            The line which contains the changeType
     * @return The operation.
     */
    private int parseChangeType( String line )
    {
        int operation = Entry.ADD;

        String modOp = StringTools.trim( line.substring( "changetype:".length() + 1 ) );

        if ( "add".equalsIgnoreCase( modOp ) )
        {
            operation = Entry.ADD;
        }
        else if ( "delete".equalsIgnoreCase( modOp ) )
        {
            operation = Entry.DELETE;
        }
        else if ( "modify".equalsIgnoreCase( modOp ) )
        {
            operation = Entry.MODIFY;
        }
        else if ( "moddn".equalsIgnoreCase( modOp ) )
        {
            operation = Entry.MODDN;
        }
        else if ( "modrdn".equalsIgnoreCase( modOp ) )
        {
            operation = Entry.MODRDN;
        }

        return operation;
    }

    /**
     * Parse the DN of an entry
     * 
     * @param line
     *            The line to parse
     * @return A DN
     * @throws NamingException
     *             If the DN is invalid
     */
    private String parseDn( String line ) throws NamingException
    {
        String dn = null;

        String lowerLine = line.toLowerCase();

        if ( lowerLine.startsWith( "dn:" ) || lowerLine.startsWith( "DN:" ) )
        {
            // Ok, we have a DN. Is it base 64 encoded ?
            int length = line.length();

            if ( length == 3 )
            {
                // The DN is empty : error
                log.error( "A ldif entry must have a non empty DN" );
                throw new NamingException( "No DN for entry" );
            }
            else if ( line.charAt( 3 ) == ':' )
            {
                if ( length > 4 )
                {
                    // This is a base 64 encoded DN.
                    String trimmedLine = line.substring( 4 ).trim();

                    try
                    {
                        dn = new String( Base64.decode( trimmedLine.toCharArray() ), "UTF-8" );
                    }
                    catch (UnsupportedEncodingException uee)
                    {
                        // The DN is not base 64 encoded
                        log.error( "The ldif entry is supposed to have a base 64 encoded DN" );
                        throw new NamingException( "Invalid base 64 encoded DN" );
                    }
                }
                else
                {
                    // The DN is empty : error
                    log.error( "A ldif entry must have a non empty DN" );
                    throw new NamingException( "No DN for entry" );
                }
            }
            else
            {
                dn = line.substring( 3 ).trim();
            }
        }
        else
        {
            log.error( "A ldif entry must start with a DN" );
            throw new NamingException( "No DN for entry" );
        }

        // Check that the DN is valid. If not, an exception will be thrown
        try
        {
            LdapDnParser.parseInternal( dn, new ArrayList<Rdn>() );
        }
        catch (InvalidNameException ine)
        {
            log.error( "The DN {} is not valid" );
            throw ine;
        }

        return dn;
    }

    /**
     * Parse the value part.
     * 
     * @param line
     *            The line which contains the value
     * @param pos
     *            The starting position in the line
     * @return A String or a byte[], depending of the kind of value we get
     */
    private static Object parseSimpleValue( String line, int pos )
    {
        if ( line.length() > pos + 1 )
        {
            char c = line.charAt( pos + 1 );

            if ( c == ':' )
            {
                String value = StringTools.trim( line.substring( pos + 2 ) );

                return Base64.decode( value.toCharArray() );
            }
            else
            {
                return StringTools.trim( line.substring( pos + 1 ) );
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Parse the value part.
     * 
     * @param line
     *            The line which contains the value
     * @param pos
     *            The starting position in the line
     * @return A String or a byte[], depending of the kind of value we get
     * @throws NamingException
     *             If something went wrong
     */
    private Object parseValue( String line, int pos ) throws NamingException
    {
        if ( line.length() > pos + 1 )
        {
            char c = line.charAt( pos + 1 );

            if ( c == ':' )
            {
                String value = StringTools.trim( line.substring( pos + 2 ) );

                return Base64.decode( value.toCharArray() );
            }
            else if ( c == '<' )
            {
                String urlName = StringTools.trim( line.substring( pos + 2 ) );

                try
                {
                    URL url = new URL( urlName );

                    if ( "file".equals( url.getProtocol() ) )
                    {
                        String fileName = url.getFile();

                        File file = new File( fileName );

                        if ( file.exists() == false )
                        {
                            log.error( "File {} not found", fileName );
                            throw new NamingException( "Bad URL, file not found" );
                        }
                        else
                        {
                            long length = file.length();

                            if ( length > sizeLimit )
                            {
                                log.error( "File {} is too big", fileName );
                                throw new NamingException( "File too big" );
                            }
                            else
                            {
                                byte[] data = new byte[(int) length];

                                try
                                {
                                    DataInputStream in = new DataInputStream( new FileInputStream( file ) );
                                    in.read( data );

                                    return data;
                                }
                                catch (FileNotFoundException fnfe)
                                {
                                    // We can't reach this point, the file
                                    // existence has already been
                                    // checked
                                    log.error( "File {} not found", fileName );
                                    throw new NamingException( "Bad URL, file not found" );
                                }
                                catch (IOException ioe)
                                {
                                    log.error( "File {} error reading", fileName );
                                    throw new NamingException( "Bad URL, file can't be read" );
                                }

                            }
                        }
                    }
                    else
                    {
                        log.error( "Protocols other than file: are not supported" );
                        throw new NamingException( "Unsupported URL protocol" );
                    }
                }
                catch (MalformedURLException mue)
                {
                    log.error( "Bad URL {}", urlName );
                    throw new NamingException( "Bad URL" );
                }
            }
            else
            {
                return StringTools.trim( line.substring( pos + 1 ) );
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * Parse a control. The grammar is : &lt;control&gt; ::= "control:" &lt;fill&gt;
     * &lt;ldap-oid&gt; &lt;critical-e&gt; &lt;value-spec-e&gt; &lt;sep&gt; &lt;critical-e&gt; ::= &lt;spaces&gt;
     * &lt;boolean&gt; | e &lt;boolean&gt; ::= "true" | "false" &lt;value-spec-e&gt; ::=
     * &lt;value-spec&gt; | e &lt;value-spec&gt; ::= ":" &lt;fill&gt; &lt;SAFE-STRING-e&gt; | "::"
     * &lt;fill&gt; &lt;BASE64-STRING&gt; | ":<" &lt;fill&gt; &lt;url&gt;
     * 
     * It can be read as : "control:" &lt;fill&gt; &lt;ldap-oid&gt; [ " "+ ( "true" |
     * "false") ] [ ":" &lt;fill&gt; &lt;SAFE-STRING-e&gt; | "::" &lt;fill&gt; &lt;BASE64-STRING&gt; | ":<"
     * &lt;fill&gt; &lt;url&gt; ]
     * 
     * @param line
     *            The line containing the control
     * @return A control
     */
    private Control parseControl( String line ) throws NamingException
    {
        String lowerLine = line.toLowerCase().trim();
        char[] controlValue = line.trim().toCharArray();
        int pos = 0;
        int length = controlValue.length;

        // Get the <ldap-oid>
        if ( pos > length )
        {
            // No OID : error !
            log.error( "The control does not have an OID" );
            throw new NamingException( "Bad control, no oid" );
        }

        int initPos = pos;

        while ( StringTools.isCharASCII( controlValue, pos, '.' ) || StringTools.isDigit( controlValue, pos ) )
        {
            pos++;
        }

        if ( pos == initPos )
        {
            // Not a valid OID !
            log.error( "The control does not have an OID" );
            throw new NamingException( "Bad control, no oid" );
        }

        // Create and check the OID
        String oidString = lowerLine.substring( 0, pos );

        OID oid = null;

        try
        {
            oid = new OID( oidString );
        }
        catch (DecoderException de)
        {
            log.error( "The OID {} is not valid", oidString );
            throw new NamingException( "Bad control oid" );
        }

        LdifControl control = new LdifControl( oid );

        // Get the criticality, if any
        // Skip the <fill>
        while ( StringTools.isCharASCII( controlValue, pos, ' ' ) )
        {
            pos++;
        }

        // Check if we have a "true" or a "false"
        int criticalPos = lowerLine.indexOf( ':' );

        int criticalLength = 0;

        if ( criticalPos == -1 )
        {
            criticalLength = length - pos;
        }
        else
        {
            criticalLength = criticalPos - pos;
        }

        if ( ( criticalLength == 4 ) && ( "true".equalsIgnoreCase( lowerLine.substring( pos, pos + 4 ) ) ) )
        {
            control.setCriticality( true );
        }
        else if ( ( criticalLength == 5 ) && ( "false".equalsIgnoreCase( lowerLine.substring( pos, pos + 5 ) ) ) )
        {
            control.setCriticality( false );
        }
        else if ( criticalLength != 0 )
        {
            // If we have a criticality, it should be either "true" or "false",
            // nothing else
            log.error( "The control muts have a valid criticality" );
            throw new NamingException( "Bad control criticality" );
        }

        if ( criticalPos > 0 )
        {
            // We have a value. It can be a normal value, a base64 encoded value
            // or a file contained value
            if ( StringTools.isCharASCII( controlValue, criticalPos + 1, ':' ) )
            {
                // Base 64 encoded value
                byte[] value = Base64.decode( line.substring( criticalPos + 2 ).toCharArray() );
                control.setValue( value );
            }
            else if ( StringTools.isCharASCII( controlValue, criticalPos + 1, '<' ) )
            {
                // File contained value
            }
            else
            {
                // Standard value
                byte[] value = new byte[length - criticalPos - 1];

                for ( int i = 0; i < length - criticalPos - 1; i++ )
                {
                    value[i] = (byte) controlValue[i + criticalPos + 1];
                }

                control.setValue( value );
            }
        }

        return control;
    }

    /**
     * Parse an AttributeType/AttributeValue
     * 
     * @param line The line to parse
     */
    public static Attribute parseAttributeValue( String line )
    {
        int colonIndex = line.indexOf( ':' );

        if ( colonIndex != -1 )
        {
            String attributeType = line.toLowerCase().substring( 0, colonIndex );
            Object attributeValue = parseSimpleValue( line, colonIndex );

            // Create an attribute
            return new BasicAttribute( attributeType, attributeValue );
        }
        else
        {
            return null;
        }
    }

    /**
     * Parse an AttributeType/AttributeValue
     * 
     * @param entry
     *            The entry where to store the value
     * @param line
     *            The line to parse
     * @param lowerLine
     *            The same line, lowercased
     * @throws NamingException
     *             If anything goes wrong
     */
    public void parseAttributeValue( Entry entry, String line, String lowerLine ) throws NamingException
    {
        int colonIndex = line.indexOf( ':' );

        String attributeType = lowerLine.substring( 0, colonIndex );

        // We should *not* have a DN twice
        if ( attributeType.equals( "dn" ) )
        {
            log.error( "An entry must not have two DNs" );
            throw new NamingException( "A ldif entry should not have two DN" );
        }

        Object attributeValue = parseValue( line, colonIndex );

        // Update the entry
        entry.addAttribute( attributeType, attributeValue );
    }

    /**
     * Parse a ModRDN operation
     * 
     * @param entry
     *            The entry to update
     * @param iter
     *            The lines iterator
     * @throws NamingException
     *             If anything goes wrong
     */
    private void parseModRdn( Entry entry, Iterator iter ) throws NamingException
    {
        // We must have two lines : one starting with "newrdn:" or "newrdn::",
        // and the second starting with "deleteoldrdn:"
        if ( iter.hasNext() )
        {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if ( lowerLine.startsWith( "newrdn::" ) || lowerLine.startsWith( "newrdn:" ) )
            {
                int colonIndex = line.indexOf( ':' );
                Object attributeValue = parseValue( line, colonIndex );
                entry.setNewRdn( attributeValue instanceof String ? (String) attributeValue : StringTools
                        .utf8ToString( (byte[]) attributeValue ) );
            }
            else
            {
                log.error( "A modrdn operation must start with a \"newrdn:\"" );
                throw new NamingException( "Bad modrdn operation" );
            }

        }
        else
        {
            log.error( "A modrdn operation must start with a \"newrdn:\"" );
            throw new NamingException( "Bad modrdn operation, no newrdn" );
        }

        if ( iter.hasNext() )
        {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if ( lowerLine.startsWith( "deleteoldrdn:" ) )
            {
                int colonIndex = line.indexOf( ':' );
                Object attributeValue = parseValue( line, colonIndex );
                entry.setDeleteOldRdn( "1".equals( attributeValue ) );
            }
            else
            {
                log.error( "A modrdn operation must contains a \"deleteoldrdn:\"" );
                throw new NamingException( "Bad modrdn operation, no deleteoldrdn" );
            }
        }
        else
        {
            log.error( "A modrdn operation must contains a \"deleteoldrdn:\"" );
            throw new NamingException( "Bad modrdn operation, no deleteoldrdn" );
        }

        return;
    }

    /**
     * Parse a modify change type.
     * 
     * The grammar is : &lt;changerecord&gt; ::= "changetype:" FILL "modify" SEP
     * &lt;mod-spec&gt; &lt;mod-specs-e&gt; &lt;mod-spec&gt; ::= "add:" &lt;mod-val&gt; | "delete:"
     * &lt;mod-val-del&gt; | "replace:" &lt;mod-val&gt; &lt;mod-specs-e&gt; ::= &lt;mod-spec&gt;
     * &lt;mod-specs-e&gt; | e &lt;mod-val&gt; ::= FILL ATTRIBUTE-DESCRIPTION SEP
     * ATTRVAL-SPEC &lt;attrval-specs-e&gt; "-" SEP &lt;mod-val-del&gt; ::= FILL
     * ATTRIBUTE-DESCRIPTION SEP &lt;attrval-specs-e&gt; "-" SEP &lt;attrval-specs-e&gt; ::=
     * ATTRVAL-SPEC &lt;attrval-specs&gt; | e *
     * 
     * @param entry
     *            The entry to feed
     * @param iter
     *            The lines
     */
    private void parseModify( Entry entry, Iterator iter ) throws NamingException
    {
        int state = MOD_SPEC;
        String modified = null;
        int modification = 0;

        // The following flag is used to deal with empty modifications
        boolean isEmptyValue = true;

        while ( iter.hasNext() )
        {
            String line = (String) iter.next();
            String lowerLine = line.toLowerCase();

            if ( lowerLine.startsWith( "-" ) )
            {
                if ( state != ATTRVAL_SPEC_OR_SEP )
                {
                    log.error( "Bad state : we should have come from an ATTRVAL_SPEC" );
                    throw new NamingException( "Bad modify separator" );
                }
                else
                {
                    if ( isEmptyValue )
                    {
                        // Update the entry
                        entry.addModificationItem( modification, modified, null );
                    }

                    state = MOD_SPEC;
                    isEmptyValue = true;
                    continue;
                }
            }
            else if ( lowerLine.startsWith( "add:" ) )
            {
                if ( ( state != MOD_SPEC ) && ( state != ATTRVAL_SPEC ) )
                {
                    log.error( "Bad state : we should have come from a MOD_SPEC or an ATTRVAL_SPEC" );
                    throw new NamingException( "Bad modify state" );
                }

                modified = StringTools.trim( line.substring( "add:".length() ) );
                modification = DirContext.ADD_ATTRIBUTE;

                state = ATTRVAL_SPEC;
            }
            else if ( lowerLine.startsWith( "delete:" ) )
            {
                if ( ( state != MOD_SPEC ) && ( state != ATTRVAL_SPEC ) )
                {
                    log.error( "Bad state : we should have come from a MOD_SPEC or an ATTRVAL_SPEC" );
                    throw new NamingException( "Bad modify state" );
                }

                modified = StringTools.trim( line.substring( "delete:".length() ) );
                modification = DirContext.REMOVE_ATTRIBUTE;

                state = ATTRVAL_SPEC_OR_SEP;
            }
            else if ( lowerLine.startsWith( "replace:" ) )
            {
                if ( ( state != MOD_SPEC ) && ( state != ATTRVAL_SPEC ) )
                {
                    log.error( "Bad state : we should have come from a MOD_SPEC or an ATTRVAL_SPEC" );
                    throw new NamingException( "Bad modify state" );
                }

                modified = StringTools.trim( line.substring( "replace:".length() ) );
                modification = DirContext.REPLACE_ATTRIBUTE;

                state = ATTRVAL_SPEC_OR_SEP;
            }
            else
            {
                if ( ( state != ATTRVAL_SPEC ) && ( state != ATTRVAL_SPEC_OR_SEP ) )
                {
                    log.error( "Bad state : we should have come from an ATTRVAL_SPEC" );
                    throw new NamingException( "Bad modify state" );
                }

                // A standard AttributeType/AttributeValue pair
                int colonIndex = line.indexOf( ':' );

                String attributeType = lowerLine.substring( 0, colonIndex );

                if ( attributeType.equals( modified ) == false )
                {
                    log.error( "The modified attribute and the attribute value spec must be equal" );
                    throw new NamingException( "Bad modify attribute" );
                }

                // We should *not* have a DN twice
                if ( attributeType.equals( "dn" ) )
                {
                    log.error( "An entry must not have two DNs" );
                    throw new NamingException( "A ldif entry should not have two DN" );
                }

                Object attributeValue = parseValue( line, colonIndex );

                // Update the entry
                entry.addModificationItem( modification, attributeType, attributeValue );
                isEmptyValue = false;

                state = ATTRVAL_SPEC_OR_SEP;
            }
        }
    }

    /**
     * Parse a change operation. We have to handle different cases depending on
     * the operation. 1) Delete : there should *not* be any line after the
     * "changetype: delete" 2) Add : we must have a list of AttributeType :
     * AttributeValue elements 3) ModDN : we must have two following lines: a
     * "newrdn:" and a "deleteoldrdn:" 4) ModRDN : the very same, but a
     * "newsuperior:" line is expected 5) Modify :
     * 
     * The grammar is : &lt;changerecord&gt; ::= "changetype:" FILL "add" SEP
     * &lt;attrval-spec&gt; &lt;attrval-specs-e&gt; | "changetype:" FILL "delete" |
     * "changetype:" FILL "modrdn" SEP &lt;newrdn&gt; SEP &lt;deleteoldrdn&gt; SEP | // To
     * be checked "changetype:" FILL "moddn" SEP &lt;newrdn&gt; SEP &lt;deleteoldrdn&gt; SEP
     * &lt;newsuperior&gt; SEP | "changetype:" FILL "modify" SEP &lt;mod-spec&gt;
     * &lt;mod-specs-e&gt; &lt;newrdn&gt; ::= "newrdn:" FILL RDN | "newrdn::" FILL
     * BASE64-RDN &lt;deleteoldrdn&gt; ::= "deleteoldrdn:" FILL "0" | "deleteoldrdn:"
     * FILL "1" &lt;newsuperior&gt; ::= "newsuperior:" FILL DN | "newsuperior::" FILL
     * BASE64-DN &lt;mod-specs-e&gt; ::= &lt;mod-spec&gt; &lt;mod-specs-e&gt; | e &lt;mod-spec&gt; ::=
     * "add:" &lt;mod-val&gt; | "delete:" &lt;mod-val&gt; | "replace:" &lt;mod-val&gt; &lt;mod-val&gt;
     * ::= FILL ATTRIBUTE-DESCRIPTION SEP ATTRVAL-SPEC &lt;attrval-specs-e&gt; "-" SEP
     * &lt;attrval-specs-e&gt; ::= ATTRVAL-SPEC &lt;attrval-specs&gt; | e
     * 
     * @param entry
     *            The entry to feed
     * @param iter
     *            The lines iterator
     * @param operation
     *            The change operation (add, modify, delete, moddn or modrdn)
     * @param control
     *            The associated control, if any
     * @return A modification entry
     */
    private void parseChange( Entry entry, Iterator iter, int operation, Control control ) throws NamingException
    {
        // The changetype and operation has already been parsed.
        entry.setChangeType( operation );

        switch ( operation )
        {
            case Entry.DELETE:
                // The change type will tell that it's a delete operation,
                // the dn is used as a key.
                return;

            case Entry.ADD:
                // We will iterate through all attribute/value pairs
                while ( iter.hasNext() )
                {
                    String line = (String) iter.next();
                    String lowerLine = line.toLowerCase();
                    parseAttributeValue( entry, line, lowerLine );
                }

                return;

            case Entry.MODIFY:
                parseModify( entry, iter );
                return;

            case Entry.MODRDN:// They are supposed to have the same syntax ???
            case Entry.MODDN:
                // First, parse the modrdn part
                parseModRdn( entry, iter );

                // The next line should be the new superior
                if ( iter.hasNext() )
                {
                    String line = (String) iter.next();
                    String lowerLine = line.toLowerCase();

                    if ( lowerLine.startsWith( "newsuperior:" ) )
                    {
                        int colonIndex = line.indexOf( ':' );
                        Object attributeValue = parseValue( line, colonIndex );
                        entry.setNewSuperior( attributeValue instanceof String ? (String) attributeValue : StringTools
                                .utf8ToString( (byte[]) attributeValue ) );
                    }
                    else
                    {
                        if ( operation == Entry.MODDN )
                        {
                            log.error( "A moddn operation must contains a \"newsuperior:\"" );
                            throw new NamingException( "Bad moddn operation, no newsuperior" );
                        }
                    }
                }
                else
                {
                    if ( operation == Entry.MODDN )
                    {
                        log.error( "A moddn operation must contains a \"newsuperior:\"" );
                        throw new NamingException( "Bad moddn operation, no newsuperior" );
                    }
                }

                return;

            default:
                // This is an error
                log.error( "Unknown operation" );
                throw new NamingException( "Bad operation" );
        }
    }

    /**
     * Parse a ldif file. The following rules are processed :
     * 
     * &lt;ldif-file&gt; ::= &lt;ldif-attrval-record&gt; &lt;ldif-attrval-records&gt; |
     * &lt;ldif-change-record&gt; &lt;ldif-change-records&gt; &lt;ldif-attrval-record&gt; ::=
     * &lt;dn-spec&gt; &lt;sep&gt; &lt;attrval-spec&gt; &lt;attrval-specs&gt; &lt;ldif-change-record&gt; ::=
     * &lt;dn-spec&gt; &lt;sep&gt; &lt;controls-e&gt; &lt;changerecord&gt; &lt;dn-spec&gt; ::= "dn:" &lt;fill&gt;
     * &lt;distinguishedName&gt; | "dn::" &lt;fill&gt; &lt;base64-distinguishedName&gt;
     * &lt;changerecord&gt; ::= "changetype:" &lt;fill&gt; &lt;change-op&gt;
     */
    private Entry parseEntry() throws NamingException
    {
        if ( ( lines == null ) || ( lines.size() == 0 ) )
        {
            log.debug( "The entry is empty : end of ldif file" );
            return null;
        }

        // The entry must start with a dn: or a dn::
        String line = lines.get( 0 );

        String dn = parseDn( line );

        // Ok, we have found a DN
        Entry entry = new Entry();
        entry.setDn( dn );

        // We remove this dn from the lines
        lines.remove( 0 );

        // Now, let's iterate through the other lines
        Iterator iter = lines.iterator();

        // This flag is used to distinguish between an entry and a change
        int type = UNKNOWN;

        // The following boolean is used to check that a control is *not*
        // found elswhere than just after the dn
        boolean controlSeen = false;

        // We use this boolean to check that we do not have AttributeValues
        // after a change operation
        boolean changeTypeSeen = false;

        int operation = Entry.ADD;
        String lowerLine = null;
        Control control = null;

        while ( iter.hasNext() )
        {
            // Each line could start either with an OID, an attribute type, with
            // "control:" or with "changetype:"
            line = (String) iter.next();
            lowerLine = line.toLowerCase();

            // We have three cases :
            // 1) The first line after the DN is a "control:"
            // 2) The first line after the DN is a "changeType:"
            // 3) The first line after the DN is anything else
            if ( lowerLine.startsWith( "control:" ) )
            {
                if ( containsEntries )
                {
                    log.error( "We cannot have changes when reading a file which already contains entries" );
                    throw new NamingException( "No changes withing entries" );
                }

                containsChanges = true;

                if ( controlSeen )
                {
                    log.error( "We already have had a control" );
                    throw new NamingException( "Control misplaced" );
                }

                // Parse the control
                control = parseControl( line.substring( "control:".length() ) );
                entry.setControl( control );

            }
            else if ( lowerLine.startsWith( "changetype:" ) )
            {
                if ( containsEntries )
                {
                    log.error( "We cannot have changes when reading a file which already contains entries" );
                    throw new NamingException( "No changes withing entries" );
                }

                containsChanges = true;

                if ( changeTypeSeen )
                {
                    log.error( "We already have had a changeType" );
                    throw new NamingException( "ChangeType misplaced" );
                }

                // A change request
                type = CHANGE;
                controlSeen = true;

                operation = parseChangeType( line );

                // Parse the change operation in a separate function
                parseChange( entry, iter, operation, control );
                changeTypeSeen = true;
            }
            else if ( line.indexOf( ':' ) > 0 )
            {
                if ( containsChanges )
                {
                    log.error( "We cannot have entries when reading a file which already contains changes" );
                    throw new NamingException( "No entries within changes" );
                }

                containsEntries = true;

                if ( controlSeen || changeTypeSeen )
                {
                    log.error( "We can't have a Attribute/Value pair after a control or a changeType" );
                    throw new NamingException( "AttributeType misplaced" );
                }

                parseAttributeValue( entry, line, lowerLine );
                type = ENTRY;
            }
            else
            {
                // Invalid attribute Value
                log.error( "Expecting an attribute type" );
                throw new NamingException( "Bad attribute" );
            }
        }

        if ( type == ENTRY )
        {
            log.debug( "Read an entry : {}", entry );
        }
        else if ( type == CHANGE )
        {
            entry.setChangeType( operation );
            log.debug( "Read a modification : {}", entry );
        }
        else
        {
            log.error( "Unknown entry type" );
            throw new NamingException( "Unknown entry" );
        }

        return entry;
    }

    /**
     * Parse the version from the ldif input.
     * 
     * @param in
     *            The input which contains the ldif data
     * @return A number representing the version (default to 1)
     * @throws NamingException
     *             If the version is incorrect
     * @throws IOException
     *             If the input is incorrect
     */
    private int parseVersion() throws NamingException
    {
        int version = DEFAULT_VERSION;

        // First, read a list of lines
        readLines();

        if ( lines.size() == 0 )
        {
            log.warn( "The ldif file is empty" );
            return version;
        }

        // get the first line
        String line = lines.get( 0 );

        // <ldif-file> ::= "version:" <fill> <number>
        char[] document = line.toCharArray();
        String versionNumber = null;

        if ( line.startsWith( "version:" ) )
        {
            position.inc( "version:".length() );
            parseFill( document, position );

            // Version number. Must be '1' in this version
            versionNumber = parseNumber( document, position );

            // We should not have any other chars after the number
            if ( position.pos != document.length )
            {
                log.error( "The version is not a number" );
                throw new NamingException( "Ldif parsing error" );
            }

            try
            {
                version = Integer.parseInt( versionNumber );
            }
            catch (NumberFormatException nfe)
            {
                log.error( "The version is not a number" );
                throw new NamingException( "Ldif parsing error" );
            }

            log.debug( "Ldif version : {}", versionNumber );

            // We have found the version, just discard the line from the list
            lines.remove( 0 );
        }
        else
        {
            log.warn( "No version information : assuming version: 1" );
        }

        return version;
    }

    /**
     * Reads an entry in a ldif buffer, and returns the resulting lines, without
     * comments, and unfolded.
     * 
     * The lines represent *one* entry.
     * 
     * @param in
     *            The buffer
     * @throws NamingException
     *             If something went wrong
     */
    private void readLines() throws NamingException
    {
        String line = null;
        boolean insideComment = true;
        boolean isFirstLine = true;

        lines.clear();
        StringBuffer sb = new StringBuffer();

        try
        {
            while ( ( line = ( (BufferedReader) in ).readLine() ) != null )
            {
                if ( line.length() == 0 )
                {
                    if ( isFirstLine )
                    {
                        continue;
                    }
                    else
                    {
                        // The line is empty, we have read an entry
                        insideComment = false;
                        break;
                    }
                }

                // We will read the first line which is not a comment
                switch ( line.charAt( 0 ) )
                {
                    case '#':
                        insideComment = true;
                        break;

                    case ' ':
                        isFirstLine = false;

                        if ( insideComment )
                        {
                            continue;
                        }
                        else if ( sb.length() == 0 )
                        {
                            log.error( "Cannot have an empty continuation line" );
                            throw new NamingException( "Ldif Parsing error" );
                        }
                        else
                        {
                            sb.append( line.substring( 1 ) );
                        }

                        insideComment = false;
                        break;

                    default:
                        isFirstLine = false;

                        // We have found a new entry
                        // First, stores the previous one if any.
                        if ( sb.length() != 0 )
                        {
                            lines.add( sb.toString() );
                        }

                        sb = new StringBuffer( line );
                        insideComment = false;
                        break;
                }
            }
        }
        catch (IOException ioe)
        {
            throw new NamingException( "Error while reading ldif lines" );
        }

        // Stores the current line if necessary.
        if ( sb.length() != 0 )
        {
            lines.add( sb.toString() );
        }

        return;
    }

    /**
     * Parse a ldif file (using the default encoding).
     * 
     * @param fileName
     *            The ldif file
     * @return A list of entries
     * @throws NamingException
     *             If the parsing fails
     */
    public List parseLdifFile( String fileName ) throws NamingException
    {
        return parseLdifFile( fileName, Charset.forName( StringTools.getDefaultCharsetName() ).toString() );
    }

    /**
     * Parse a ldif file, decoding it using the given charset encoding
     * 
     * @param fileName
     *            The ldif file
     * @param encoding
     *            The charset encoding to use
     * @return A list of entries
     * @throws NamingException
     *             If the parsing fails
     */
    public List parseLdifFile( String fileName, String encoding ) throws NamingException
    {
        if ( StringTools.isEmpty( fileName ) )
        {
            log.error( "Cannot parse an empty file name !" );
            throw new NamingException( "Empty filename" );
        }

        File file = new File( fileName );

        if ( file.exists() == false )
        {
            log.error( "Cannot parse the file {}, it does not exist", fileName );
            throw new NamingException( "Filename " + fileName + " not found." );
        }

        // Open the file and then get a channel from the stream
        BufferedReader in;

        try
        {
            in = new BufferedReader( new InputStreamReader( new FileInputStream( file ), Charset.forName( encoding ) ) );
        }
        catch (FileNotFoundException fnfe)
        {
            log.error( "Cannot find file {}", fileName );
            throw new NamingException( "Filename " + fileName + " not found." );
        }

        return parseLdif( in );
    }

    /**
     * A method which parses a ldif string and returns a list of entries.
     * 
     * @param ldif
     *            The ldif string
     * @return A list of entries, or an empty List
     * @throws NamingException
     *             If something went wrong
     */
    public List parseLdif( String ldif ) throws NamingException
    {
        log.debug( "Starts parsing ldif buffer" );

        if ( StringTools.isEmpty( ldif ) )
        {
            return new ArrayList();
        }

        StringReader strIn = new StringReader( ldif );
        BufferedReader in = new BufferedReader( strIn );

        try
        {
            List entries = parseLdif( in );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Parsed {} entries.", ( entries == null ? new Integer( 0 ) : new Integer( entries.size() ) ) );
            }

            return entries;
        }
        catch (NamingException ne)
        {
            log.error( "Cannot parse the ldif buffer : {}", ne.getMessage() );
            throw new NamingException( "Error while parsing the ldif buffer" );
        }
    }

    // ------------------------------------------------------------------------
    // Iterator Methods
    // ------------------------------------------------------------------------

    /**
     * Gets the next LDIF on the channel.
     * 
     * @return the next LDIF as a String.
     */
    public Object next()
    {
        try
        {
            log.debug( "next(): -- called" );

            Entry entry = prefetched;
            readLines();

            try
            {
                prefetched = parseEntry();
            }
            catch (NamingException ne)
            {
                error = ne;
            }

            log.debug( "next(): -- returning ldif {}\n", entry );

            return entry;
        }
        catch (NamingException ne)
        {
            log.error( "Premature termination of LDIF iterator" );
            error = ne;
            return null;
        }
    }

    /**
     * Tests to see if another LDIF is on the input channel.
     * 
     * @return true if another LDIF is available false otherwise.
     */
    public boolean hasNext()
    {
        log.debug( "hasNext(): -- returning {}", ( prefetched != null ) ? Boolean.TRUE : Boolean.FALSE );

        return null != prefetched;
    }

    /**
     * Always throws UnsupportedOperationException!
     * 
     * @see java.util.Iterator#remove()
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return An iterator on the file
     */
    public Iterator iterator()
    {
        return this;
    }

    /**
     * @return True if an error occured during parsing
     */
    public boolean hasError()
    {
        return error != null;
    }

    /**
     * @return The exception that occurs during an entry parsing
     */
    public Exception getError()
    {
        return error;
    }

    /**
     * The main entry point of the LdifParser. It reads a buffer and returns a
     * List of entries.
     * 
     * @param in
     *            The buffer being processed
     * @return A list of entries
     * @throws NamingException
     *             If something went wrong
     */
    public List<Entry> parseLdif( BufferedReader in ) throws NamingException
    {
        // Create a list that will contain the read entries
        List<Entry> entries = new ArrayList<Entry>();

        this.in = in;

        // First get the version - if any -
        version = parseVersion();
        prefetched = parseEntry();

        // When done, get the entries one by one.
        while ( hasNext() )
        {
            Entry entry = (Entry) next();

            if ( error != null )
            {
                throw new NamingException( "Error while parsing ldif : " + error.getMessage() );
            }
            else if ( entry != null )
            {
                entries.add( entry );
            }
        }

        return entries;
    }

    /**
     * @return True if the ldif file contains entries, fals if it contains
     *         changes
     */
    public boolean containsEntries()
    {
        return containsEntries;
    }
}