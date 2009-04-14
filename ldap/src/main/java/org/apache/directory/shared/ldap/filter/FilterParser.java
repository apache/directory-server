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
package org.apache.directory.shared.ldap.filter;


import java.text.ParseException;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.Position;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * This class parse a Ldap filter. The grammar is given in RFC 4515
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class FilterParser
{
    /**
     * Creates a filter parser implementation.
     */
    public FilterParser()
    {
    }


    /**
     * Parse an extensible
     * 
     * extensible     = ( attr [":dn"] [':' oid] ":=" assertionvalue )
     *                  / ( [":dn"] ':' oid ":=" assertionvalue )
     * matchingrule   = ":" oid
     */
    private static ExprNode parseExtensible( String attr, String filter, Position pos ) throws ParseException
    {
        ExtensibleNode node = new ExtensibleNode( attr );

        if ( attr != null )
        {
            // First check if we have a ":dn"
            if ( StringTools.areEquals( filter, pos.start, "dn" ) )
            {
                // Set the dnAttributes flag and move forward in the string
                node.setDnAttributes( true );
                pos.start += 2;
            }
            else
            {
                // Push back the ':' 
                pos.start--;
            }

            // Do we have a MatchingRule ?
            if ( StringTools.charAt( filter, pos.start ) == ':' )
            {
                pos.start++;
                int start = pos.start;

                if ( StringTools.charAt( filter, pos.start ) == '=' )
                {
                    pos.start++;

                    // Get the assertionValue
                    node.setValue( parseAssertionValue( filter, pos, true ) );

                    return node;
                }
                else
                {
                    AttributeUtils.parseAttribute( filter, pos, false );

                    node.setMatchingRuleId( filter.substring( start, pos.start ) );

                    if ( StringTools.areEquals( filter, pos.start, ":=" ) )
                    {
                        pos.start += 2;

                        // Get the assertionValue
                        node.setValue( parseAssertionValue( filter, pos, true ) );

                        return node;
                    }
                    else
                    {
                        throw new ParseException( "AssertionValue expected", pos.start );
                    }
                }
            }
            else
            {
                throw new ParseException( "Expected MatchingRule or assertionValue", pos.start );
            }
        }
        else
        {
            boolean oidRequested = false;

            // First check if we have a ":dn"
            if ( StringTools.areEquals( filter, pos.start, ":dn" ) )
            {
                // Set the dnAttributes flag and move forward in the string
                node.setDnAttributes( true );
                pos.start += 3;
            }
            else
            {
                oidRequested = true;
            }

            // Do we have a MatchingRule ?
            if ( StringTools.charAt( filter, pos.start ) == ':' )
            {
                pos.start++;
                int start = pos.start;

                if ( StringTools.charAt( filter, pos.start ) == '=' )
                {
                    if ( oidRequested )
                    {
                        throw new ParseException( "MatchingRule expected", pos.start );
                    }

                    pos.start++;

                    // Get the assertionValue
                    node.setValue( parseAssertionValue( filter, pos, true ) );

                    return node;
                }
                else
                {
                    AttributeUtils.parseAttribute( filter, pos, false );

                    node.setMatchingRuleId( filter.substring( start, pos.start ) );

                    if ( StringTools.areEquals( filter, pos.start, ":=" ) )
                    {
                        pos.start += 2;

                        // Get the assertionValue
                        node.setValue( parseAssertionValue( filter, pos, true ) );

                        return node;
                    }
                    else
                    {
                        throw new ParseException( "AssertionValue expected", pos.start );
                    }
                }
            }
            else
            {
                throw new ParseException( "Expected MatchingRule or assertionValue", pos.start );
            }
        }
    }


    /**
     * An assertion value : 
     * assertionvalue = valueencoding
     * valueencoding  = 0*(normal / escaped)
     * normal         = UTF1SUBSET / UTFMB
     * escaped        = '\' HEX HEX
     * HEX            = '0'-'9' / 'A'-'F' / 'a'-'f'
     * UTF1SUBSET     = %x01-27 / %x2B-5B / %x5D-7F (Everything but '\0', '*', '(', ')' and '\')
     * UTFMB          = UTF2 / UTF3 / UTF4
     * UTF0           = %x80-BF
     * UTF2           = %xC2-DF UTF0
     * UTF3           = %xE0 %xA0-BF UTF0 / %xE1-EC UTF0 UTF0 / %xED %x80-9F UTF0 / %xEE-EF UTF0 UTF0
     * UTF4           = %xF0 %x90-BF UTF0 UTF0 / %xF1-F3 UTF0 UTF0 UTF0 / %xF4 %x80-8F UTF0 UTF0
     * 
     * With the specific constraints (RFC 4515):
     *    "The <valueencoding> rule ensures that the entire filter string is a"
     *    "valid UTF-8 string and provides that the octets that represent the"
     *    "ASCII characters "*" (ASCII 0x2a), "(" (ASCII 0x28), ")" (ASCII"
     *    "0x29), "\" (ASCII 0x5c), and NUL (ASCII 0x00) are represented as a"
     *    "backslash "\" (ASCII 0x5c) followed by the two hexadecimal digits"
     *    "representing the value of the encoded octet."

     * 
     * The incomming String is already transformed from UTF-8 to unicode, so we must assume that the 
     * grammar we have to check is the following :
     * 
     * assertionvalue = valueencoding
     * valueencoding  = 0*(normal / escaped)
     * normal         = unicodeSubset
     * escaped        = '\' HEX HEX
     * HEX            = '0'-'9' / 'A'-'F' / 'a'-'f'
     * unicodeSubset     = %x01-27 / %x2B-5B / %x5D-FFFF
     */
    private static String parseAssertionValue( String filter, Position pos, boolean preserveEscapedChars ) throws ParseException
    {
        int start = pos.start;
        boolean foundHex = false;

        char c = StringTools.charAt( filter, pos.start );

        do
        {
            if ( StringTools.isUnicodeSubset( c ) )
            {
                pos.start++;
            }
            else if ( StringTools.isCharASCII( filter, pos.start, '\\' ) )
            {
                // Maybe an escaped 
                pos.start++;

                // First hex
                if ( StringTools.isHex( filter, pos.start ) )
                {
                    pos.start++;
                }
                else
                {
                    throw new ParseException( "Not a valid escaped value", pos.start );
                }

                // second hex
                if ( StringTools.isHex( filter, pos.start ) )
                {
                    pos.start++;
                    foundHex = true;
                }
                else
                {
                    throw new ParseException( "Not a valid escaped value", pos.start );
                }
            }
            else
            {
                // not a valid char, so let's get out
                break;
            }
        }
        while ( ( c = StringTools.charAt( filter, pos.start ) ) != '\0' );

        String ret = filter.substring( start, pos.start );
        if ( !preserveEscapedChars && foundHex )
        {
            try
            {
                ret = StringTools.decodeEscapedHex( ret );
            }
            catch ( InvalidNameException e )
            {
                throw new ParseException( "Unable to decode escaped hex sequences: '" + ret + "': " + e, pos.start );
            }
        }
        return ret;
    }


    private static String parseAssertionValue( String filter, Position pos ) throws ParseException
    {
        return parseAssertionValue( filter, pos, false );
    }


    /**
     * Parse a substring
     */
    private static ExprNode parseSubstring( String attr, String initial, String filter, Position pos )
        throws ParseException
    {
        if ( StringTools.isCharASCII( filter, pos.start, '*' ) )
        {
            // We have found a '*' : this is a substring
            SubstringNode node = new SubstringNode( attr );

            if ( !StringTools.isEmpty( initial ) )
            {
                // We have a substring starting with a value : val*...
                // Set the initial value
                node.setInitial( initial );
            }

            pos.start++;

            // 
            while ( true )
            {
                String assertionValue = parseAssertionValue( filter, pos );

                // Is there anything else but a ')' after the value ?
                if ( StringTools.isCharASCII( filter, pos.start, ')' ) )
                {
                    // Nope : as we have had [initial] '*' (any '*' ) *,
                    // this is the final
                    if ( !StringTools.isEmpty( assertionValue ) )
                    {
                        node.setFinal( assertionValue );
                    }

                    return node;
                }
                else if ( StringTools.isCharASCII( filter, pos.start, '*' ) )
                {
                    // We have a '*' : it's an any
                    // If the value is empty, that means we have more than 
                    // one consecutive '*' : do nothing in this case.
                    if ( !StringTools.isEmpty( assertionValue ) )
                    {
                        node.addAny( assertionValue );
                    }

                    pos.start++;
                }
                else
                {
                    // This is an error
                    throw new ParseException( "Bad substring", pos.start );
                }
            }
        }
        else
        {
            // This is an error
            throw new ParseException( "Bad substring", pos.start );
        }
    }


    /**
     * Here is the grammar to parse :
     * 
     * simple    ::= '=' assertionValue
     * present   ::= '=' '*'
     * substring ::= '=' [initial] any [final]
     * initial   ::= assertionValue
     * any       ::= '*' ( assertionValue '*')*
     * 
     * As we can see, there is an ambiguity in the grammar : attr=* can be
     * seen as a present or as a substring. As stated in the RFC :
     * 
     * "Note that although both the <substring> and <present> productions in"
     * "the grammar above can produce the "attr=*" construct, this construct"
     * "is used only to denote a presence filter." (RFC 4515, 3)
     * 
     * We have also to consider the difference between a substring and the
     * equality node : this last node does not contain a '*'
     *
     * @param attr
     * @param filter
     * @param pos
     * @return
     */
    private static ExprNode parsePresenceEqOrSubstring( String attr, String filter, Position pos )
        throws ParseException
    {
        if ( StringTools.isCharASCII( filter, pos.start, '*' ) )
        {
            // To be a present node, the next char should be a ')'
            //StringTools.trimLeft( filter, pos );
            pos.start++;

            if ( StringTools.isCharASCII( filter, pos.start, ')' ) )
            {
                // This is a present node
                return new PresenceNode( attr );
            }
            else
            {
                // Definitively a substring with no initial or an error
                // Push back the '*' on the string
                pos.start--;
                return parseSubstring( attr, null, filter, pos );
            }
        }
        else if ( StringTools.isCharASCII( filter, pos.start, ')' ) )
        {
            // An empty equality Node
            return new EqualityNode( attr, new ClientStringValue( "" ) );
        }
        else
        {
            // A substring or an equality node
            String value = parseAssertionValue( filter, pos );

            // Is there anything else but a ')' after the value ?
            if ( StringTools.isCharASCII( filter, pos.start, ')' ) )
            {
                // This is an equality node
                return new EqualityNode( attr, new ClientStringValue( value ) );
            }

            return parseSubstring( attr, value, filter, pos );
        }
    }


    /**
     * Parse the following grammar :
     * item           = simple / present / substring / extensible
     * simple         = attr filtertype assertionvalue
     * filtertype     = '=' / '~=' / '>=' / '<='
     * present        = attr '=' '*'
     * substring      = attr '=' [initial] any [final]
     * extensible     = ( attr [":dn"] [':' oid] ":=" assertionvalue )
     *                  / ( [":dn"] ':' oid ":=" assertionvalue )
     * matchingrule   = ":" oid
     *                  
     * An item starts with an attribute or a colon.
     */
    private static ExprNode parseItem( String filter, Position pos, char c ) throws ParseException
    {
        LeafNode node = null;
        String attr = null;

        // Relax the grammar a bit : we can have spaces 
        // StringTools.trimLeft( filter, pos );

        if ( c == '\0' )
        {
            throw new ParseException( "Bad char", pos.start );
        }

        if ( c == ':' )
        {
            // If we have a colon, then the item is an extensible one
            return parseExtensible( null, filter, pos );
        }
        else
        {
            // We must have an attribute
            attr = AttributeUtils.parseAttribute( filter, pos, true );

            // Relax the grammar a bit : we can have spaces 
            // StringTools.trimLeft( filter, pos );

            // Now, we may have a present, substring, simple or an extensible
            c = StringTools.charAt( filter, pos.start );

            switch ( c )
            {
                case '=':
                    // It can be a presence, an equal or a substring
                    pos.start++;
                    return parsePresenceEqOrSubstring( attr, filter, pos );

                case '~':
                    // Approximate node
                    pos.start++;

                    // Check that we have a '='
                    if ( !StringTools.isCharASCII( filter, pos.start, '=' ) )
                    {
                        throw new ParseException( "Expecting a '=' ", pos.start );
                    }

                    pos.start++;

                    // Parse the value and create the node
                    node = new ApproximateNode( attr, new ClientStringValue( parseAssertionValue( filter, pos ) ) );
                    return node;

                case '>':
                    // Greater or equal node
                    pos.start++;

                    // Check that we have a '='
                    if ( !StringTools.isCharASCII( filter, pos.start, '=' ) )
                    {
                        throw new ParseException( "Expecting a '=' ", pos.start );
                    }

                    pos.start++;

                    // Parse the value and create the node
                    node = new GreaterEqNode( attr, new ClientStringValue( parseAssertionValue( filter, pos ) ) );
                    return node;

                case '<':
                    // Less or equal node
                    pos.start++;

                    // Check that we have a '='
                    if ( !StringTools.isCharASCII( filter, pos.start, '=' ) )
                    {
                        throw new ParseException( "Expecting a '=' ", pos.start );
                    }

                    pos.start++;

                    // Parse the value and create the node
                    node = new LessEqNode( attr, new ClientStringValue( parseAssertionValue( filter, pos ) ) );
                    return node;

                case ':':
                    // An extensible node
                    pos.start++;
                    return parseExtensible( attr, filter, pos );

                default:
                    // This is an error
                    throw new ParseException( "An item is expected", pos.start );
            }
        }
    }


    /**
     * Parse AND, OR and NOT nodes :
     * 
     * and            = '&' filterlist
     * or             = '|' filterlist
     * not            = '!' filter
     * filterlist     = 1*filter
     * 
     * @return
     */
    private static ExprNode parseBranchNode( ExprNode node, String filter, Position pos ) throws ParseException
    {
        BranchNode bNode = ( BranchNode ) node;

        // Relax the grammar a bit : we can have spaces 
        // StringTools.trimLeft( filter, pos );

        // We must have at least one filter
        ExprNode child = parseFilterInternal( filter, pos );

        // Add the child to the node children
        bNode.addNode( child );

        // Relax the grammar a bit : we can have spaces 
        // StringTools.trimLeft( filter, pos );

        // Now, iterate though all the remaining filters, if any
        while ( ( child = parseFilterInternal( filter, pos ) ) != null )
        {
            // Add the child to the node children
            bNode.addNode( child );

            // Relax the grammar a bit : we can have spaces 
            // StringTools.trimLeft( filter, pos );
        }

        // Relax the grammar a bit : we can have spaces 
        // StringTools.trimLeft( filter, pos );

        return node;
    }


    /**
     * filtercomp     = and / or / not / item
     * and            = '&' filterlist
     * or             = '|' filterlist
     * not            = '!' filter
     * item           = simple / present / substring / extensible
     * simple         = attr filtertype assertionvalue
     * present        = attr EQUALS ASTERISK
     * substring      = attr EQUALS [initial] any [final]
     * extensible     = ( attr [dnattrs]
     *                    [matchingrule] COLON EQUALS assertionvalue )
     *                    / ( [dnattrs]
     *                         matchingrule COLON EQUALS assertionvalue )
     */
    private static ExprNode parseFilterComp( String filter, Position pos ) throws ParseException
    {
        ExprNode node = null;

        // Relax the grammar a bit : we can have spaces 
        // StringTools.trimLeft( filter, pos );

        if ( pos.start == pos.length )
        {
            throw new ParseException( "Empty filterComp", pos.start );
        }

        char c = StringTools.charAt( filter, pos.start );

        switch ( c )
        {
            case '&':
                // This is a AND node
                pos.start++;
                node = new AndNode();
                parseBranchNode( node, filter, pos );
                break;

            case '|':
                // This is an OR node
                pos.start++;
                node = new OrNode();
                parseBranchNode( node, filter, pos );
                break;

            case '!':
                // This is a NOT node
                pos.start++;
                node = new NotNode();
                parseBranchNode( node, filter, pos );
                break;

            default:
                // This is an item
                node = parseItem( filter, pos, c );
                break;

        }

        return node;
    }


    /**
     * Pasre the grammar rule :
     * filter ::= '(' filterComp ')'
     */
    private static ExprNode parseFilterInternal( String filter, Position pos ) throws ParseException
    {
        // relax the grammar by allowing spaces
        // StringTools.trimLeft( filter, pos );

        // Check for the left '('
        if ( !StringTools.isCharASCII( filter, pos.start, '(' ) )
        {
            // No more node, get out
            if ( ( pos.start == 0 ) && ( pos.length != 0 ) )
            {
                throw new ParseException( "No '(' at the begining of the filter", 0 );
            }
            else
            {
                return null;
            }
        }

        pos.start++;

        // relax the grammar by allowing spaces
        // StringTools.trimLeft( filter, pos );

        // parse the filter component
        ExprNode node = parseFilterComp( filter, pos );

        if ( node == null )
        {
            throw new ParseException( "Bad filter", pos.start );
        }

        // relax the grammar by allowing spaces
        // StringTools.trimLeft( filter, pos );

        // Check that we have a right ')'
        if ( !StringTools.isCharASCII( filter, pos.start, ')' ) )
        {
            throw new ParseException( "The filter has no right parenthese", pos.start );
        }

        pos.start++;

        return node;
    }


    /**
     * @see FilterParser#parse(String)
     */
    public static ExprNode parse( String filter ) throws ParseException
    {
        // The filter must not be null. This is a defensive test
        if ( StringTools.isEmpty( filter ) )
        {
            throw new ParseException( "Empty filter", 0 );
        }

        Position pos = new Position();
        pos.start = 0;
        pos.end = 0;
        pos.length = filter.length();

        return parseFilterInternal( filter, pos );
    }


    public void setFilterParserMonitor( FilterParserMonitor monitor )
    {
    }
}
