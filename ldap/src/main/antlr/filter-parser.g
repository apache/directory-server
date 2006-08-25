// ----------------------------------------------------------------------------
// file header
// ----------------------------------------------------------------------------

header {
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


import antlr.*;
import java.util.ArrayList;
}

// ----------------------------------------------------------------------------
// class definition
// ----------------------------------------------------------------------------

/**
 * An LDAP filter parser.
 *
 * @see <a href="http://ietf.org/internet-drafts/draft-ietf-ldapbis-filter-08.txt">String Representation of Search Filters</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class AntlrFilterParser extends Parser;
 

// ----------------------------------------------------------------------------
// parser options
// ----------------------------------------------------------------------------

options
{
    k = 5;
	importVocab = FilterLexer;
}


// ----------------------------------------------------------------------------
// parser class members
// ----------------------------------------------------------------------------

{
    /** the monitor used to track the activities of this parser */
    FilterParserMonitor monitor;
    /** the token stream selector used for multiplexing the underlying stream */
    TokenStreamSelector selector;
    /** the filter value encoding lexer */
    AntlrFilterValueLexer valueLexer;
    /** the value parser pulling tokens from the value lexer */
    AntlrFilterValueParser valueParser;


    /**
     * Sets the token stream selector used for multiplexing the underlying stream.
     *
     * @param selector the token stream selector used for multiplexing
     */
    public void setSelector( TokenStreamSelector selector )
    {
        this.selector = selector;
    }


    /**
     * Sets the filter value encoding lexer.
     *
     * @param valueLexer the filter value encoding lexer
     */
    public void setValueLexer( AntlrFilterValueLexer valueLexer )
    {
        this.valueLexer = valueLexer;
    }


    /**
     * Sets the value parser pulling tokens from the value lexer.
     *
     * @param valueParser value parser pulling tokens from the value lexer
     */
    public void setValueParser( AntlrFilterValueParser valueParser )
    {
        this.valueParser = valueParser;
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
// ldap-filters lexer rules
// ----------------------------------------------------------------------------


/**
 * The top level production for matching a filter expression.
 */
filter returns [ExprNode node]
{
    node = null;
}
    /*
     * The right parenthesis is left off because it is consumed as a termination
     * token for the VALUEENCODING token of the value lexer.  When non terminal
     * nodes ( those other than filters with AND, OR and NOT'd expressions ) are
     * encountered we match the right parenthesis in filtercomp().
     */
    : LPAREN node=filtercomp;


/**
 * A production for matching composite filter expressions.
 */
filtercomp returns [ExprNode node]
{
    node = null;
}
    /*
     * Match the right parenthesis for any non-terminal production not directly
     * using the value parser and value lexer to suck up values.
     */
    : node=and RPAREN | node=or RPAREN | node=not RPAREN | node=item;


/**
 * A recursive production for matching AND'd filter expressions.
 */
and returns [BranchNode node]
{
    node = null;
    ExprNode child;
    ArrayList children = new ArrayList();
}
    :
    AMPERSTAND child=filter
    {
        children.add( child );
    }
    ( child=filter
      {
          children.add( child );
      }
    )+
    {
        node = new BranchNode( AbstractExprNode.AND, children );
    }
    ;


/**
 * A recursive production for matching OR'd filter expressions.
 */
or returns [BranchNode node]
{
    node = null;
    ExprNode child;
    ArrayList children = new ArrayList();
}
    :
    VERTBAR child=filter
    {
        children.add( child );
    }
    ( child=filter
      {
          children.add( child );
      }
    )+
    {
        node = new BranchNode( AbstractExprNode.OR, children );
    }
    ;


/**
 * A recursive production for matching negated filter expressions.
 */
not returns [BranchNode node]
{
    node = null;
    ExprNode child;
}
    : EXCLAMATION child=filter
    {
        node = new BranchNode( AbstractExprNode.NOT );
        node.addNode( child );
    }
    ;


/**
 * A production for matching all non-terminal assertions.  This includes
 * extensible, presence, substring, greaterorequal, lessorequal, and equality
 * filter assertions.
 */
item returns [LeafNode node]
{
    node = null;
}
    : node=simple | node=extensible |
    ( COLONEQUALS
        {
            selector.select( valueLexer );
            String value = ( ( String ) valueParser.value( null ) ).trim();
            node = new ExtensibleNode( null, value, null, false );
        }
    )
    ;


/**
 * General filter assertion matching production for approximate, greater or
 * equal, less or equal, equals, substring, and presence simple items,
 */
simple returns [LeafNode node]
{
    String attribute = null;
    node = null;
    int type = -1;
}
    /*
     * Reads the attribute description, the assertion operator, and then
     * invokes the filter value parser.  Note that this rule switches the
     * lexical state via the selector.  The invoked value parser automatically
     * switches state back to the filter lexer.
     */
    : token:ATTRIBUTEDESCRIPTION
    {
        attribute = token.getText();
    }
    (
      APPROX
        {
            type = AbstractExprNode.APPROXIMATE;
        }
    | GREATEROREQUAL
        {
            type = AbstractExprNode.GREATEREQ;
        }
    | LESSOREQUAL
        {
            type = AbstractExprNode.LESSEQ;
        }
    | EQUALS
        {
            type = AbstractExprNode.EQUALITY;
        }
    )
      {
        selector.select( valueLexer );
        Object value = valueParser.value( attribute );

        switch( type )
        {
            case( AbstractExprNode.APPROXIMATE ):
            case( AbstractExprNode.GREATEREQ ):
            case( AbstractExprNode.LESSEQ ):
                node = new SimpleNode( attribute, ( ( String ) value).trim(), type );
                break;
            case( AbstractExprNode.EQUALITY ):
                if ( value instanceof String )
                {
                    node = new SimpleNode( attribute, ( ( String ) value ).trim(), type );
                }
                else
                {
                    node = ( LeafNode ) value;
                }
                break;
            default:
                throw new IllegalStateException( "Expecting one of 4 types" );
        }
      }
    ;


/**
 * Extensible filter assertion matching production.
 */
extensible returns [ExtensibleNode node]
{
    node = null;
    boolean dnAttrs = false;
    String attribute = null;
    String matchingRule = null;
    String value = null;
}
    :
    (
        // A L T E R N A T I V E   1 :
        attributeAlt1:ATTRIBUTEDESCRIPTION
        {
            attribute = attributeAlt1.getText().trim();
        }
        ( DN { dnAttrs = true; } )?
        ( COLON matchingRuleAlt1:ATTRIBUTEDESCRIPTION
            {
                matchingRule = matchingRuleAlt1.getText().trim();
                int idx = matchingRule.indexOf( ';' );
                if ( idx != -1 )
                {
                    String msg = "matchingRule OIDs cannot have options: ";
                    msg += matchingRule;
                    throw new RecognitionException( msg, matchingRule, 0, idx );
                }
            }
        )? COLONEQUALS
        {
            selector.select( valueLexer );
            value = ( ( String ) valueParser.value( attribute ) ).trim();
        }

    |
        // A L T E R N A T I V E   2 :
        ( DN { dnAttrs = true; })? COLON matchingRuleAlt2:ATTRIBUTEDESCRIPTION
            {
                matchingRule = matchingRuleAlt2.getText().trim();
                int idx = matchingRule.indexOf( ';' );
                if ( idx != -1 )
                {
                    String msg = "matchingRule OIDs cannot have options: ";
                    msg += matchingRule;
                    throw new RecognitionException( msg, matchingRule, 0, idx );
                }
            }
        COLONEQUALS
        {
            selector.select( valueLexer );
            value = ( ( String ) valueParser.value( attribute ) ).trim();
        }
    )
    {
        node = new ExtensibleNode( attribute, value, matchingRule, dnAttrs );
    }
    ;

