header
{
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


package org.apache.directory.shared.ldap.subtree;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
}


// ----------------------------------------------------------------------------
// parser class definition
// ----------------------------------------------------------------------------

/**
 * The antlr generated subtree specification parser.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSubtreeSpecificationChecker extends Parser;


// ----------------------------------------------------------------------------
// parser options
// ----------------------------------------------------------------------------

options
{
    k = 1;
    defaultErrorHandler = false;
}


// ----------------------------------------------------------------------------
// parser initialization
// ----------------------------------------------------------------------------

{
    private static final Logger log = LoggerFactory.getLogger( AntlrSubtreeSpecificationChecker.class );
    
    private ComponentsMonitor subtreeSpecificationComponentsMonitor = null;

    /**
     * Does nothing.
     */
    public void init()
    {
    }
    

    private int token2Integer( Token token ) throws RecognitionException
    {
        int i = 0;
        
        try
        {
            i = Integer.parseInt( token.getText());
        }
        catch ( NumberFormatException e )
        {
            throw new RecognitionException( "Value of INTEGER token " +
                                            token.getText() +
                                            " cannot be converted to an Integer" );
        }
        
        return i;
    }
}


// ----------------------------------------------------------------------------
// parser productions
// ----------------------------------------------------------------------------

wrapperEntryPoint
{
    log.debug( "entered wrapperEntryPoint()" );
} :
    subtreeSpecification "end"
    ;

subtreeSpecification
{
    log.debug( "entered subtreeSpecification()" );
    subtreeSpecificationComponentsMonitor = new OptionalComponentsMonitor( 
            new String [] { "base", "specificExclusions", "minimum", "maximum", "specificationFilter" } );
}
    :
    OPEN_CURLY ( SP )*
        ( subtreeSpecificationComponent ( SP )*
            ( SEP ( SP )* subtreeSpecificationComponent ( SP )* )* )?
    CLOSE_CURLY
    ;

subtreeSpecificationComponent
{
    log.debug( "entered subtreeSpecification()" );
}
    :
    ss_base
    {
        subtreeSpecificationComponentsMonitor.useComponent( "base" );
    }
    | ss_specificExclusions
    {
        subtreeSpecificationComponentsMonitor.useComponent( "specificExclusions" );
    }
    | ss_minimum
    {
        subtreeSpecificationComponentsMonitor.useComponent( "minimum" );
    }
    | ss_maximum
    {
        subtreeSpecificationComponentsMonitor.useComponent( "maximum" );
    }
    | ss_specificationFilter
    {
        subtreeSpecificationComponentsMonitor.useComponent( "specificationFilter" );
    }
    ;
    exception
    catch [IllegalArgumentException e]
    {
        throw new RecognitionException( e.getMessage() );
    }

ss_base
{
    log.debug( "entered ss_base()" );
}
    :
    ID_base ( SP )+ distinguishedName
    ;

ss_specificExclusions
{
    log.debug( "entered ss_specificExclusions()" );
}
    :
    ID_specificExclusions ( SP )+ specificExclusions
    ;

specificExclusions
{
    log.debug( "entered specificExclusions()" );
}
    :
    OPEN_CURLY ( SP )*
        ( specificExclusion ( SP )*
            ( SEP ( SP )* specificExclusion ( SP )* )*
        )?
    CLOSE_CURLY
    ;

specificExclusion
{
    log.debug( "entered specificExclusion()" );
}
    :
    chopBefore | chopAfter
    ;

chopBefore
{
    log.debug( "entered chopBefore()" );
}
    :
    ID_chopBefore ( SP )* COLON ( SP )* distinguishedName
    ;

chopAfter
{
    log.debug( "entered chopAfter()" );
}
    :
    ID_chopAfter ( SP )* COLON ( SP )* distinguishedName
    ;

ss_minimum
{
    log.debug( "entered ss_minimum()" );
}
    :
    ID_minimum ( SP )+ baseDistance
    ;

ss_maximum
{
    log.debug( "entered ss_maximum()" );
}
    :
    ID_maximum ( SP )+ baseDistance
    ;

ss_specificationFilter
{
    log.debug( "entered ss_specificationFilter()" );
}
    :
    ID_specificationFilter ( SP )+ refinement
    ;
    
distinguishedName
{
    log.debug( "entered distinguishedName()" );
}
    :
    token:SAFEUTF8STRING
    {
        new LdapDN( token.getText() );
        log.debug( "recognized a DistinguishedName: " + token.getText() );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "dnParser failed for " + token.getText() + " " + e.getMessage() );
    }

baseDistance
{
    log.debug( "entered baseDistance()" );
}
    :
    token:INTEGER
    {
        token2Integer(token);
    }
    ;

oid
{
    log.debug( "entered oid()" );
     Token token = null;
}
    :
    { token = LT( 1 ); } // an interesting trick goes here ;-)
    ( DESCR | NUMERICOID )
    {
        log.debug( "recognized an oid: " + token.getText() );
    }
    ;

refinement
{
    log.debug( "entered refinement()" );
}
    :
    item | and | or | not
    ;

item
{
    log.debug( "entered item()" );
}
    :
    ID_item ( SP )* COLON ( SP )* oid
    ;

and
{
    log.debug( "entered and()" );
}
    :
    ID_and ( SP )* COLON ( SP )* refinements
    ;

or
{
    log.debug( "entered or()" );
}
    :
    ID_or ( SP )* COLON ( SP )* refinements
    ;

not
{
    log.debug( "entered not()" );
}
    :
    ID_not ( SP )* COLON ( SP )* refinements
    ;

refinements
{
    log.debug( "entered refinements()" );
}
    :
    OPEN_CURLY ( SP )*
    (
        refinement ( SP )*
            ( SEP ( SP )* refinement ( SP )* )*
    )? CLOSE_CURLY
    ;


// ----------------------------------------------------------------------------
// lexer class definition
// ----------------------------------------------------------------------------

/**
 * The parser's primary lexer.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSubtreeSpecificationCheckerLexer extends Lexer;


// ----------------------------------------------------------------------------
// lexer options
// ----------------------------------------------------------------------------

options
{
    k = 2;

    charVocabulary = '\u0001'..'\u0127';
}

tokens
{
    ID_base = "base";
    ID_specificExclusions = "specificExclusions";
    ID_chopBefore = "chopBefore";
    ID_chopAfter = "chopAfter";
    ID_minimum = "minimum";
    ID_maximum = "maximum";
    ID_specificationFilter = "specificationFilter";
    ID_item = "item";
    ID_and = "and";
    ID_or = "or";
    ID_not = "not";
}


//----------------------------------------------------------------------------
// lexer initialization
//----------------------------------------------------------------------------

{
    private static final Logger log = LoggerFactory.getLogger( AntlrSubtreeSpecificationLexer.class );
}


// ----------------------------------------------------------------------------
// attribute description lexer rules from models
// ----------------------------------------------------------------------------

SP : ' ';

COLON : ':' { log.debug( "matched COLON(':')" ); } ;

OPEN_CURLY : '{' { log.debug( "matched LBRACKET('{')" ); } ;

CLOSE_CURLY : '}' { log.debug( "matched RBRACKET('}')" ); } ;

SEP : ',' { log.debug( "matched SEP(',')" ); } ;

SAFEUTF8STRING : '"'! ( SAFEUTF8CHAR )* '"'! { log.debug( "matched SAFEUTF8CHAR: \"" + getText() + "\"" ); } ;

DESCR : ALPHA ( ALPHA | DIGIT | '-' )* { log.debug( "matched DESCR" ); } ;

INTEGER_OR_NUMERICOID
    :
    ( INTEGER DOT ) => NUMERICOID
    {
        $setType( NUMERICOID );
    }
    |
    INTEGER
    {
        $setType( INTEGER );
    }
    ;

protected INTEGER: DIGIT | ( LDIGIT ( DIGIT )+ ) { log.debug( "matched INTEGER: " + getText() ); } ;

protected NUMERICOID: INTEGER ( DOT INTEGER )+ { log.debug( "matched NUMERICOID: " + getText() ); } ;

protected DOT: '.' ;

protected DIGIT: '0' | LDIGIT ;

protected LDIGIT: '1'..'9' ;

protected ALPHA: 'A'..'Z' | 'a'..'z' ;

// This is all messed up - could not figure out how to get antlr to represent
// the safe UTF-8 character set from RFC 3642 for production SafeUTF8Character

protected SAFEUTF8CHAR:
    '\u0001'..'\u0021' |
    '\u0023'..'\u007F' |
    '\u00c0'..'\u00d6' |
    '\u00d8'..'\u00f6' |
    '\u00f8'..'\u00ff' |
    '\u0100'..'\u1fff' |
    '\u3040'..'\u318f' |
    '\u3300'..'\u337f' |
    '\u3400'..'\u3d2d' |
    '\u4e00'..'\u9fff' |
    '\uf900'..'\ufaff' ;
