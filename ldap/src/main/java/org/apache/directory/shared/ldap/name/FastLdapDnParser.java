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


import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.util.Position;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A fast LDAP DN parser that handles only simple DNs. If the DN contains
 * any special character an {@link TooComplexException} is thrown.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 08:28:06 +0200 (Sa, 07 Jun 2008) $
 */
public enum FastLdapDnParser implements NameParser
{
    INSTANCE;

    /**
     * Gets the name parser singleton instance.
     * 
     * @return the name parser
     */
    public static NameParser getNameParser()
    {
        return INSTANCE;
    }


    /* (non-Javadoc)
     * @see javax.naming.NameParser#parse(java.lang.String)
     */
    public Name parse( String name ) throws NamingException
    {
        LdapDN dn = new LdapDN();
        parseDn( name, dn );
        return dn;
    }


    /**
     * Parses the given name string and fills the given LdapDN object.
     * 
     * @param name the name to parse
     * @param dn the LdapDN to fill
     * 
     * @throws InvalidNameException the invalid name exception
     */
    public void parseDn( String name, LdapDN dn ) throws InvalidNameException
    {
        parseDn(name, dn.rdns);
        dn.setUpName( name );
        dn.normalizeInternal();
    }
    
    void parseDn( String name, List<Rdn> rdns ) throws InvalidNameException
    {
        if ( name == null || name.trim().length() == 0 )
        {
            // We have an empty DN, just get out of the function.
            return;
        }

        Position pos = new Position();
        pos.start = 0;
        pos.length = name.length();

        while ( true )
        {
            Rdn rdn = new Rdn();
            parseRdnInternal( name, pos, rdn );
            rdns.add( rdn );

            if ( !hasMoreChars( pos ) )
            {
                // end of line reached
                break;
            }
            char c = nextChar( name, pos, true );
            switch ( c )
            {
                case ',':
                case ';':
                    // another RDN to parse
                    break;

                default:
                    throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                        + ". Excpected ',' or ';'." );
            }
        }
    }


    /**
     * Parses the given name string and fills the given Rdn object.
     * 
     * @param name the name to parse
     * @param rdn the RDN to fill
     * 
     * @throws InvalidNameException the invalid name exception
     */
    public void parseRdn( String name, Rdn rdn ) throws InvalidNameException
    {
        if ( name == null || name.length() == 0 )
        {
            throw new InvalidNameException( "RDN must not be empty" );
        }

        Position pos = new Position();
        pos.start = 0;
        pos.length = name.length();

        parseRdnInternal( name, pos, rdn );

        if ( !hasMoreChars( pos ) )
        {
            throw new InvalidNameException( "Expected no more characters at position " + pos.start );
        }
    }


    private void parseRdnInternal( String name, Position pos, Rdn rdn ) throws InvalidNameException
    {
        int rdnStart = pos.start;

        // SPACE*
        matchSpaces( name, pos );

        // attributeType: ALPHA (ALPHA|DIGIT|HYPEN) | NUMERICOID
        String type = matchAttributeType( name, pos );

        // SPACE*
        matchSpaces( name, pos );

        // EQUALS
        matchEquals( name, pos );

        // SPACE*
        matchSpaces( name, pos );

        // here we only match "simple" values
        // stops at \ + # " -> Too Complex Exception
        String upValue = matchValue( name, pos );
        String value = StringTools.trimRight( upValue );
        // TODO: trim, normalize, etc

        // SPACE*
        matchSpaces( name, pos );

        rdn.addAttributeTypeAndValue( type, type, upValue, value );

        rdn.setUpName( name.substring( rdnStart, pos.start ) );
        rdn.normalize();

    }


    /**
     * Matches and forgets optional spaces.
     * 
     * @param name the name
     * @param pos the pos
     * @throws InvalidNameException 
     */
    private void matchSpaces( String name, Position pos ) throws InvalidNameException
    {
        while ( hasMoreChars( pos ) )
        {
            char c = nextChar( name, pos, true );
            if ( c != ' ' )
            {
                pos.start--;
                break;
            }
        }
    }


    /**
     * Matches attribute type.
     * 
     * @param name the name
     * @param pos the pos
     * 
     * @return the matched attribute type
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private String matchAttributeType( String name, Position pos ) throws InvalidNameException
    {
        char c = nextChar( name, pos, false );
        switch ( c )
        {
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                // descr
                return matchAttributeTypeDescr( name, pos );

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                // numericoid
                return matchAttributeTypeNumericOid( name, pos );

            default:
                // error
                throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                    + ". Excpected start of attributeType." );
        }
    }


    /**
     * Matches attribute type descr.
     * 
     * @param name the name
     * @param pos the pos
     * 
     * @return the attribute type descr
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private String matchAttributeTypeDescr( String name, Position pos ) throws InvalidNameException
    {
        StringBuilder descr = new StringBuilder();
        while ( hasMoreChars( pos ) )
        {
            char c = nextChar( name, pos, true );
            switch ( c )
            {
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '-':
                    descr.append( c );
                    break;

                case ' ':
                case '=':
                    pos.start--;
                    return descr.toString();

                case '.':
                    // occurs for RDNs of form "oid.1.2.3=test"
                    throw new TooComplexException();

                default:
                    // error
                    throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                        + ". Excpected start of attributeType descr." );
            }
        }
        return descr.toString();
    }


    /**
     * Matches attribute type numeric OID.
     * 
     * @param name the name
     * @param pos the pos
     * 
     * @return the attribute type OID
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private String matchAttributeTypeNumericOid( String name, Position pos ) throws InvalidNameException
    {
        StringBuilder numericOid = new StringBuilder();
        int dotCount = 0;
        while ( true )
        {
            char c = nextChar( name, pos, true );
            switch ( c )
            {
                case '0':
                    // leading '0', no other digit may follow!
                    numericOid.append( c );
                    c = nextChar( name, pos, true );
                    switch ( c )
                    {
                        case '.':
                            numericOid.append( c );
                            dotCount++;
                            break;
                        case ' ':
                        case '=':
                            pos.start--;
                            break;
                        default:
                            throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                                + ". Excpected numericoid." );
                    }
                    break;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    numericOid.append( c );
                    boolean inInnerLoop = true;
                    while ( inInnerLoop )
                    {
                        c = nextChar( name, pos, true );
                        switch ( c )
                        {
                            case ' ':
                            case '=':
                                inInnerLoop = false;
                                pos.start--;
                                break;
                            case '.':
                                inInnerLoop = false;
                                dotCount++;
                                // no break!
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                numericOid.append( c );
                                break;
                            default:
                                throw new InvalidNameException( "Unexpected character '" + c + "' at position "
                                    + pos.start + ". Excpected numericoid." );
                        }
                    }
                    break;
                case ' ':
                case '=':
                    pos.start--;
                    if ( dotCount > 0 )
                    {
                        return numericOid.toString();
                    }
                    else
                    {
                        throw new InvalidNameException( "Numeric OID must contain at least one dot." );
                    }
                default:
                    throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                        + ". Excpected start of attributeType numericoid." );
            }
        }
    }


    /**
     * Matches the equals character.
     * 
     * @param name the name
     * @param pos the pos
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private void matchEquals( String name, Position pos ) throws InvalidNameException
    {
        char c = nextChar( name, pos, true );
        if ( c != '=' )
        {
            throw new InvalidNameException( "Unexpected character '" + c + "' at position " + pos.start
                + ". Excpected EQUALS '='." );
        }
    }


    /**
     * Matches the assertion value. This method only handles simple values.
     * If we find any special character (BACKSLASH, PLUS, SHARP or DQUOTE),
     * a TooComplexException will be thrown.
     * 
     * @param name the name
     * @param pos the pos
     * 
     * @return the string
     * 
     * @throws InvalidNameException the invalid name exception
     */
    private String matchValue( String name, Position pos ) throws InvalidNameException
    {
        StringBuilder value = new StringBuilder();
        int numTrailingSpaces = 0;
        while ( true )
        {
            if ( !hasMoreChars( pos ) )
            {
                pos.start -= numTrailingSpaces;
                return value.substring( 0, value.length() - numTrailingSpaces );
            }
            char c = nextChar( name, pos, true );
            switch ( c )
            {
                case '\\':
                case '+':
                case '#':
                case '"':
                    throw new TooComplexException();
                case ',':
                case ';':
                    pos.start--;
                    pos.start -= numTrailingSpaces;
                    return value.substring( 0, value.length() - numTrailingSpaces );
                case ' ':
                    numTrailingSpaces++;
                    value.append( c );
                    break;
                default:
                    numTrailingSpaces = 0;
                    value.append( c );
            }
        }
    }


    /**
     * Gets the next character.
     * 
     * @param name the name
     * @param pos the pos
     * @param increment true to increment the position
     * 
     * @return the character
     * @throws InvalidNameException If no more characters are available
     */
    private char nextChar( String name, Position pos, boolean increment ) throws InvalidNameException
    {
        if ( !hasMoreChars( pos ) )
        {
            throw new InvalidNameException( "No more characters available at position " + pos.start );
        }
        char c = name.charAt( pos.start );
        if ( increment )
        {
            pos.start++;
        }
        return c;
    }


    /**
     * Checks if there are more characters.
     * 
     * @param pos the pos
     * 
     * @return true, if more characters are available
     */
    private boolean hasMoreChars( Position pos )
    {
        return pos.start < pos.length;
    }
}
