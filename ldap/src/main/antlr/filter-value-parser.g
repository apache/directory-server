// ----------------------------------------------------------------------------
// file header
// ----------------------------------------------------------------------------

header {
/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.filter;


import antlr.*;
import java.util.*;
}


// ----------------------------------------------------------------------------
// class definition
// ----------------------------------------------------------------------------

/**
 * A filter assertion value encoding parser.  This parser is used by the top
 * filter parser to handle equality, extensible, presence and substring
 * assertion values.  It also participates in multiplexing the underlying
 * token stream by driving filter assertion value lexer.
 *
 * @see <a href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-filter-08.txt">String Representation of Search Filters</a>
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 */
class AntlrFilterValueParser extends Parser;
 

// ----------------------------------------------------------------------------
// parser options
// ----------------------------------------------------------------------------

options
{
    k = 3;
	importVocab = FilterValueLexer;
}


// ----------------------------------------------------------------------------
// parser class members
// ----------------------------------------------------------------------------

{
    /** the monitor used to track the activities of this parser */
    FilterParserMonitor monitor;
    /** the primary lexer used by the filter parser */
    AntlrFilterLexer lexer;
    /** the token stream selector for the filter parser */
    TokenStreamSelector selector;


    /**
     * Sets the token stream select so we can use it to switch to the filter
     * parsers primary lexer.
     *
     * @param selector the token stream selector for the filter parser
     */
    public void setSelector( TokenStreamSelector selector )
    {
        this.selector = selector;
    }


    /**
     * Sets the filter's main lexer so we can switch back to it.
     *
     * @param lexer the primary lexer used by the filter parser
     */
    public void setLexer( AntlrFilterLexer lexer )
    {
        this.lexer = lexer;
    }


    /**
     * Sets the monitor used to track the activities of this parser.
     *
     * @param monitor used to track the activities of this parser
     */
    public void setFilterParserMonitor( FilterParserMonitor monitor )
    {
        this.monitor = monitor;
    }


    /**
     * Monitors FilterParser events where it matches a production.
     *
     * @param production the name of the production matched
     */
    private void matchedProduction( String production )
    {
        if ( this.monitor != null )
        {
            this.monitor.matchedProduction( production );
        }
    }
}



// ----------------------------------------------------------------------------
// parser rules
// ----------------------------------------------------------------------------


/**
 * Parser production that calls either equal, presence or substring productions.
 */
value [String attribute] returns [Object node]
{
    node = null;
}
    : ( node=equal | node=presence[attribute] | node=substring[attribute] );


/**
 * Parser production that parses the equality expression value immediately
 * after the '=' in a filter equality assertion.
 */
equal returns [String retval]
{
    retval = null;
}
    : val:VALUEENCODING
    {
        selector.select( lexer );
        retval = val.getText();
    }
    RPAREN;


/**
 * Parser production that parses the presence expressions immediately after the
 * '=' in a presence assertion.
 */
presence [String attribute] returns [PresenceNode node]
{
    node = null;
}
    : ASTERISK
    {
        selector.select( lexer );
        node = new PresenceNode( attribute.trim() );
    }
    RPAREN;


/**
 * Parser production that parses the substring expression value immediately
 * after the '=' in a substring filter assertion.
 */
substring [String attribute] returns [LeafNode node]
{
    node = null;
    String initial = null;
    String fin = null;
    ArrayList any = new ArrayList();
}
    :
    (
        // A L T E R N A T I V E   1:    initial*
        alt1:VALUEENCODING
        {
            initial = alt1.getText().trim();
        }
        ASTERISK
    |
        // A L T E R N A T I V E   2:    (*any) *final
        ( ASTERISK alt2:VALUEENCODING
        {
            if ( fin != null && fin.length() > 0 )
            {
                any.add( fin );
            }

            fin = alt2.getText().trim();
        }
        )+ ( ASTERISK 
        {
            if ( fin != null && fin.length() > 0 )
            {
                any.add( fin );
            }

            fin = null;
        })?
    |
        // A L T E R N A T I V E   3:    initial (*any) *final
        alt3t0:VALUEENCODING
        {
            initial = alt3t0.getText().trim();
        }
        ( ASTERISK alt3t1:VALUEENCODING
        {
            if ( fin != null && fin.length() > 0 )
            {
                any.add( fin );
            }

            fin = alt3t1.getText().trim();
        }
        )+
    )
    RPAREN
    {
        selector.select( lexer );

        /*
         * Under special circumstances a presence string can appear to be a
         * SubstringNode.  Namely the following too filters are not equal:
         *
         * (ou=*) != (ou=* )
         *
         * The first on the left hand side has no space between the '*' and the
         * closing parenthesis.  The 2nd on the right hand side has a space.  So
         * the question arrises how do we interpret this.  Intuitively both
         * would be considered the same and that's what we shall do until we ask
         * on the LDAPBIS mailing list.  However note that the first filter will
         * be processed by the presence rule while the second will be processed
         * by this rule the substring rule.  That is because according to the
         * parser white space is significant as it should be in the value
         * encoding just not on the periphery.  So as a substring the 2nd filter
         * would be interpretted as a search for all ou values ending in a
         * whitespace.  Again intuitively this does not make sense.  I would
         * imagine it best if this is what was intended that the escape sequence
         * '\20' be used for white space rather than using actual whitespace on
         * the periphery.
         *
         * So what do we do to solve this problem.  Well we need to characterize
         * first the range of errors that are possible.  First any one of these
         * filters are valid presence filters (note whitespace differences):
         *    #1       #2           #3         #4
         * (ou= * ) (ou=  *) (   ou  =  *  ) (ou=* )
         *
         * #1: initial = " ", any is empty, final = " "
         * #2: initial = "  ", any is empty, final = null
         * #3: initial = "  ", any is empty, final = "  "
         * #4: initial = null, any is empty, final = " "
         *
         * To handle this and generate the appropriate node type we check for
         * null, empty strings, and empty any arrays in the condition below:
         */
        if ( any.isEmpty()
             && ( initial == null || initial.trim().length() == 0 )
             && ( fin == null || fin.trim().length() == 0 ) )
        {
            node = new PresenceNode( attribute );
        }
        else
        {
            if ( initial != null && initial.length() == 0 )
            {
                initial = null;
            }

            if ( fin != null && fin.length() == 0 )
            {
                fin = null;
            }

            node = new SubstringNode( any, attribute, initial, fin );
        }
    }
    ;

