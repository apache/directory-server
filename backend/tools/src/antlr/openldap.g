// ============================================================================
//
//
//                    OpenLDAP Schema Parser
//
//
// ============================================================================
// $Rev$
// ============================================================================


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
/*
 * Keep the semicolon right next to the package name or else there will be a
 * bug that comes into the foreground in the new antlr release.
 */
package org.apache.eve.tools.schema;

import java.util.* ;
import org.apache.ldap.common.schema.*;

}


class antlrOpenLdapSchemaLexer extends Lexer ;

options    {
    k = 7 ;
    exportVocab=antlrOpenLdapSchema ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = false ;
    //testLiterals = true ;
    defaultErrorHandler = false ;
}


WS  :   (   '#' (~'\n')* '\n' { newline(); }
        |    ' '
        |   '\t'
        |   '\r' '\n' { newline(); }
        |   '\n'      { newline(); }
        |   '\r'      { newline(); }
        )
        {$setType(Token.SKIP);} //ignore this token
    ;

QUOTE              : '\''
    ;

DIGIT              : '0' .. '9'
    ;

OPEN_PAREN         : '('
    ;

CLOSE_PAREN        : ')'
    ;

OPEN_BRACKET       : '{'
    ;

CLOSE_BRACKET      : '}'
    ;

protected NUMERIC_STRING : ('0' .. '9')+
    ;

NUMERICOID         :
        NUMERIC_STRING ( '.' NUMERIC_STRING )+
    ;

IDENTIFIER options { testLiterals=true; }
    : 
        ( 'a' .. 'z') ( 'a' .. 'z' | '0' .. '9' | '-' | ';' )*
    ;

DESC
    :
        "desc" WS QUOTE ( ~'\'' )+ QUOTE
    ;

SYNTAX
    :
        "syntax" WS NUMERICOID OPEN_BRACKET ( DIGIT )+ CLOSE_BRACKET
    ;

class antlrOpenLdapSchemaParser extends Parser ;

options    {
    k = 5 ;
    defaultErrorHandler = false ;
}


{
    public static final String[] EMPTY = new String[0];

    private Map attributeTypes = new HashMap();
    private Map objectClasses = new HashMap();
    private ParserMonitor monitor = null;


    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------


    public Map getAttributeTypes()
    {
        return Collections.unmodifiableMap( attributeTypes );
    }


    public Map getObjectClasses()
    {
        return Collections.unmodifiableMap( objectClasses );
    }


    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }


    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------


    private void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
}


parseSchema
{
}
    :
    ( attributeType )*
    ;

attributeType
{
    matchedProduction( "attributeType()" );
    AttributeTypeLiteral type = null;
    UsageEnum usageEnum;
}
    :
    "attributetype"
    OPEN_PAREN oid:NUMERICOID
    {
        type = new AttributeTypeLiteral( oid.getText() );
    }
        ( names[type] )?
        ( desc[type] )?
        ( "OBSOLETE" { type.setObsolete( true ); } )?
        ( superior[type] )?
        ( equality[type] )?
        ( ordering[type] )?
        ( substr[type] )?
        ( syntax[type] )?
        ( "SINGLE-VALUE" { type.setSingleValue( true ); } )?
        ( "COLLECTIVE" { type.setCollective( true ); } )?
        ( "NO-USER-MODIFICATION" { type.setNoUserModification( true ); } )?
        ( usage[type] )?

    CLOSE_PAREN
    {
        attributeTypes.put( type.getOid(), type );
    }
    ;


desc [AttributeTypeLiteral type]
{
}
    : d:DESC
    {
        type.setDescription( d.getText().split( "'" )[1] );
    }
    ;


superior [AttributeTypeLiteral type]
{
    matchedProduction( "superior()" ) ;
}
    : "SUP"
    (
        oid:NUMERICOID
        {
            type.setSuperior( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setSuperior( id.getText() );
        }
    );


equality [AttributeTypeLiteral type]
{
    matchedProduction( "equality()" ) ;
}
    : "EQUALITY"
    (
        oid:NUMERICOID
        {
            type.setEquality( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setEquality( id.getText() );
        }
    );


substr [AttributeTypeLiteral type]
{
    matchedProduction( "substr()" ) ;
}
    : "SUBSTR"
    (
        oid:NUMERICOID
        {
            type.setSubstr( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setSubstr( id.getText() );
        }
    );


ordering [AttributeTypeLiteral type]
{
    matchedProduction( "ordering()" ) ;
}
    : "ORDERING"
    (
        oid:NUMERICOID
        {
            type.setOrdering( oid.getText() );
        }
        |
        id:IDENTIFIER
        {
            type.setOrdering( id.getText() );
        }
    );


names [AttributeTypeLiteral type]
{
    matchedProduction( "names()" ) ;
    ArrayList list = new ArrayList();
}
    :
    (
        "NAME" QUOTE id0:IDENTIFIER QUOTE
        {
            list.add( id0.getText() );
        }
        |
        ( OPEN_PAREN QUOTE id1:IDENTIFIER
        {
            list.add( id1.getText() );
        } QUOTE
        ( QUOTE id2:IDENTIFIER QUOTE
        {
            list.add( id2.getText() );
        } )* CLOSE_PAREN )
    )
    {
        type.setNames( ( String[] ) list.toArray( EMPTY ) );
    }
    ;


syntax [AttributeTypeLiteral type]
{
    matchedProduction( "syntax()" ) ;
}
    : token:SYNTAX
    {
        String[] comps = token.getText().split( " " );

        int index = comps[1].indexOf( "{" );
        if ( index == -1 )
        {
            type.setSyntax( comps[1] );
            return;
        }

        String oid = comps[1].substring( 0, index );
        String length = comps[1].substring( index + 1, comps[1].length() - 1 );

        type.setSyntax( oid );
        type.setLength( Integer.parseInt( length ) );
    }
    ;


usage [AttributeTypeLiteral type]
{
    matchedProduction( "usage()" ) ;
}
    :
    "USAGE"
    (
        "userApplications" { type.setUsage( UsageEnum.USERAPPLICATIONS ); } |
        "directoryOperation" { type.setUsage( UsageEnum.DIRECTORYOPERATION ); } |
        "distributedOperation" { type.setUsage( UsageEnum.DISTRIBUTEDOPERATION ); } |
        "dSAOperation" { type.setUsage( UsageEnum.DSAOPERATION ); }
    );
