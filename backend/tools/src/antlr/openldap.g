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
}


class antlrOpenLdapSchemaLexer extends Lexer ;

options    {
    k = 4 ;
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

OPEN_PAREN         : '(' 
    ;

CLOSE_PAREN        : ')'
    ;

OPEN_BRACKET       : '{'
    ;

CLOSE_BRACKET      : '}'
    ;

NUMERICOID         :
        ( '0'..'9' )+ ( '.' ( '0'..'9' )+ )*
    ;

LENGTH             :
        OPEN_BRACKET ('0' .. '9')+ CLOSE_BRACKET
    ;

IDENTIFIER options { testLiterals=true; }
    : 
        ( 'a' .. 'z') ( 'a' .. 'z' | '0' .. '9' | '-' | ';' )*
    ;

QIDENTIFIER
    :
        '\'' IDENTIFIER '\''
    ;

    


class antlrOpenLdapSchemaParser extends Parser ;

options    {
    k = 5 ;
    defaultErrorHandler = false ;
}


{
    OpenLdapSchemaParserMonitor monitor = null ;


    public void matchedProduction( String a_msg )
    {
        if ( null == monitor )
        {
            //System.out.println( a_msg ) ;
        }
        else
        {
            monitor.matchedProduction( a_msg ) ;
        }
    }
    

    public void setOpenLdapSchemaParserMonitor( OpenLdapSchemaParserMonitor monitor )
    {
        this.monitor = monitor ;
    }
    
}


attributeType returns [AttributeType type]
{
    matchedProduction( "attributeType()" ) ;
    type = null ;
    UsageEnum usageEnum;
    String[] nameArray;
} 
    :
    "attributetype"
    OPEN_PAREN NUMERICOID
        ( "NAME" nameArray=names )?
        ( "DESC" QDESC )?
        ( "OBSOLETE" )?
        ( "SUP" ( NUMERICOID | IDENTIFIER ) )?
        ( "EQUALITY" ( NUMERICOID | IDENTIFIER ) )?
        ( "ORDERING" ( NUMERICOID | IDENTIFIER ) )?
        ( "SUBSTR"   ( NUMERICOID | IDENTIFIER ) )?
        ( "SINGLE-VALUE" )?
        ( "COLLECTIVE" )?
        ( "NO-USER-MODIFICATION" )?
        ( usageEnum=usage )?

    CLOSE_PAREN ;


names returns [String[] nameArray]
{
    nameArray = null;
    ArrayList list = new ArrayList();
}
    :
    (
        "'" id0:IDENTIFIER "'" { list.add( id0.getText() ); } |
        ( OPEN_PAREN "'" id1:IDENTIFIER { list.add( id1.getText(); } "'"
            ( "'" id2:IDENTIFIER "'" {list.add( id2.getText();} )* CLOSE_PAREN ) 
    );


usage returns [UsageEnum usage]
{
    usage = null;
}
    :
    "USAGE"
    (
        "userApplications" { usage = UsageEnum.USERAPPLICATIONS; } |
        "directoryOperation" { usage = UsageEnum.DIRECTORYOPERATION; } |
        "distributedOperation" { usage = UsageEnum.DISTRIBUTEDOPERATION; } |
        "dSAOperation" { usage = UsageEnum.DSAOPERATION; }
    );
