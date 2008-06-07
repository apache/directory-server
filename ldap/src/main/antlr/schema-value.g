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
package org.apache.directory.shared.ldap.schema.syntax;

import java.util.List;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.schema.parser.ParserMonitor;

}


/**
 * An antlr generated schema lexer. This is a sub-lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaValueLexer extends Lexer;

options    {
    k = 3 ;
    exportVocab=AntlrSchemaValue ;
    charVocabulary = '\3'..'\377' ;
    caseSensitive = false ;
    defaultErrorHandler = false ;
}

WHSP
    :
    ( options {greedy=true;} :
    ' '
    |
    '\t'
    |
    '\r' (options {greedy=true;} : '\n')? { newline(); } 
    |
    '\n' { newline(); }
    |
    '#' (~'\n')* '\n' { newline(); }
    )+
    { setText(" "); }
    //{$setType(Token.SKIP);} //ignore this token
    ;

LPAR : '(' ;
RPAR : ')' ;
protected CHAR : 'a'..'z' ;
protected LDIGIT : '1'..'9' ;
protected DIGIT : '0'..'9' ; 
protected NUMBER : DIGIT | ( LDIGIT (DIGIT)+ ) ;
protected NUMBER2 : (DIGIT)+ ;
protected NUMERICOID : NUMBER ( '.' NUMBER )+ ;
protected HYPEN : '-';
protected OTHER : '_' | ';' | '.';
protected DESCR: CHAR ( CHAR | DIGIT | HYPEN )* ;
protected QUIRKS_DESCR: ( CHAR | DIGIT | HYPEN | OTHER )+ ;

QUOTE : '\'' ;
DOLLAR : '$' ;
LCURLY : '{' ;
RCURLY : '}' ;
LEN : LCURLY n:NUMBER2 RCURLY { setText(n.getText()); } ;


DESCR_OR_QUIRKS_DESCR :
    ( NUMERICOID QUIRKS_DESCR ) => QUIRKS_DESCR { $setType( QUIRKS_DESCR ); }
    |
    ( NUMBER QUIRKS_DESCR ) => QUIRKS_DESCR { $setType( QUIRKS_DESCR ); }
    |
    ( HYPEN QUIRKS_DESCR ) => QUIRKS_DESCR { $setType( QUIRKS_DESCR ); }
    |
    ( OTHER QUIRKS_DESCR ) => QUIRKS_DESCR { $setType( QUIRKS_DESCR ); }
    |
    ( DESCR QUIRKS_DESCR ) => QUIRKS_DESCR { $setType( QUIRKS_DESCR ); }
    |
    ( DESCR ) { $setType( DESCR ); }
    |
    ( NUMBER '.' ) => NUMERICOID { $setType( NUMERICOID ); }
    |
    ( NUMBER ) { $setType( NUMBER ); }
    ;


/**
 * An antlr generated schema parser. This is a sub-parser used to parse
 * numericoid, oid, oids, qdescr, qdescrs according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaValueParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}

{
    private ParserMonitor monitor = null;
    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }
    private void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
}

    /**
     * noidlen = numericoid [ LCURLY len RCURLY ]
     * len = number
     */
noidlen returns [AntlrSchemaParser.NoidLen noidlen = new AntlrSchemaParser.NoidLen()]
    {
        matchedProduction( "AntlrSchemaValueParser.noidlen()" );
    }
    :
    ( 
        (LPAR)?
        (WHSP)?
        (QUOTE)?
        (
            ( d4:DESCR { noidlen.noid = d4.getText(); } )
            |
            ( n2:NUMERICOID { noidlen.noid = n2.getText(); } )
        )
        (QUOTE)?
        (WHSP)?
        (RPAR)?
        (
            l:LEN { noidlen.len = Integer.parseInt(l.getText()); }
            (QUOTE)?
            (WHSP)?
            (RPAR)?
        )?
    )
    ;


    /**
     * noidlen = numericoid [ LCURLY len RCURLY ]
     * len = number
     */
quirksNoidlen returns [AntlrSchemaParser.NoidLen noidlen = new AntlrSchemaParser.NoidLen()]
    {
        matchedProduction( "AntlrSchemaValueParser.quirksNoidlen()" );
    }
    :
    (
        (LPAR)?
        (WHSP)?
        (QUOTE)?
        (
            ( q2:QUIRKS_DESCR { noidlen.noid = q2.getText(); } )
            |
            ( d4:DESCR { noidlen.noid = d4.getText(); } )
            |
            ( n2:NUMERICOID { noidlen.noid = n2.getText(); } )
        )
        (QUOTE)?
        (WHSP)?
        (RPAR)?
        (
            l:LEN { noidlen.len = Integer.parseInt(l.getText()); }
            (QUOTE)?
            (WHSP)?
            (RPAR)?
        )?    
    )
    ;


    /**
     * numericoid = number 1*( DOT number )
     */
numericoid returns [String numericoid=null]
    {
        matchedProduction( "AntlrSchemaValueParser.numericoid()" );
    }
    : 
    (
        (WHSP)?
        (LPAR (WHSP)? )?
        (
            ( QUOTE n1:NUMERICOID { numericoid = n1.getText(); } QUOTE )
            |
            ( n2:NUMERICOID { numericoid = n2.getText(); } )
        )
        (
        (WHSP)?
        (RPAR)?
        )
    )
    ;


    /**
     * oid = descr / numericoid
     * numericoid = number 1*( DOT number )
     * descr = keystring
     * keystring = leadkeychar *keychar
     * leadkeychar = ALPHA
     * keychar = ALPHA / DIGIT / HYPHEN
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
oid returns [String oid=null]
    {
        matchedProduction( "AntlrSchemaValueParser.oid()" );
    }
    : 
    (
        (WHSP)?
        (
            ( QUOTE n1:NUMERICOID { oid = n1.getText(); } QUOTE  )
            |
            ( n2:NUMERICOID { oid = n2.getText(); } )
            |
            ( QUOTE d1:DESCR { oid = d1.getText(); } QUOTE )
            |
            ( d2:DESCR { oid = d2.getText(); } )
        )
        (options {greedy=true;} : WHSP)?
    )
    ;


    /**
     * oids = oid / ( LPAREN WSP oidlist WSP RPAREN )
     * oidlist = oid *( WSP DOLLAR WSP oid )
     */
oids returns [List<String> oids]
    {
        matchedProduction( "AntlrSchemaValueParser.oids()" );
        oids = new ArrayList<String>();
        String oid = null;
    }
    :
    (
        ( 
            oid=oid { oids.add(oid); } 
        )
    |
        (
            LPAR
            oid=oid { oids.add(oid); } 
            ( 
                (DOLLAR)? 
                oid=oid { oids.add(oid); } 
            )* 
            RPAR
        )
    )
    ;


    /**
     * qdescr = SQUOTE descr SQUOTE
     */
qdescr returns [String qdescr=null]
    {
        matchedProduction( "AntlrSchemaValueParser.qdescr()" );
    }
    : 
    ( 
        (WHSP)?
        (
            ( QUOTE d1:DESCR { qdescr = d1.getText(); } QUOTE )
            |
            ( d2:DESCR { qdescr = d2.getText(); } )
        )
    )
    ; 


    /**
     * qdescrs = qdescr / ( LPAREN WSP qdescrlist WSP RPAREN )
     * qdescrlist = [ qdescr *( SP qdescr ) ]
     */
qdescrs returns [List<String> qdescrs]
    {
        matchedProduction( "AntlrSchemaValueParser.qdescrs()" );
        qdescrs = new ArrayList<String>();
        String qdescr = null;
    }
    :
    (
        ( 
            qdescr=qdescr { qdescrs.add(qdescr); } 
        )
    |
        (             
        
            LPAR 
            qdescr=qdescr { qdescrs.add(qdescr); } 
            (options {greedy=true;} : WHSP)?
            (DOLLAR)?
            (options {greedy=true;} : WHSP)?
            (
                qdescr=qdescr { qdescrs.add(qdescr); } 
                (options {greedy=true;} : WHSP)?
                (DOLLAR)?
                (options {greedy=true;} : WHSP)?
            )*
            RPAR 
        )
    )
    ;
    
    
    
    /**
     * qdescr = SQUOTE descr SQUOTE
     */
quirksQdescr returns [String qdescr=null]
    {
        matchedProduction( "AntlrSchemaValueParser.qdescr()" );
    }
    : 
    ( 
        (WHSP)?
        (
            ( QUOTE d1:QUIRKS_DESCR { qdescr = d1.getText(); } QUOTE )
            |
            ( d2:QUIRKS_DESCR { qdescr = d2.getText(); } )
            |
            ( QUOTE d3:DESCR { qdescr = d3.getText(); } QUOTE )
            |
            ( d4:DESCR { qdescr = d4.getText(); } )
            |
            ( QUOTE n1:NUMERICOID { qdescr = n1.getText(); } QUOTE  )
            |
            ( n2:NUMERICOID { qdescr = n2.getText(); } )
        )
        (options {greedy=true;} : WHSP)?
    )
    ; 


    /**
     * qdescrs = qdescr / ( LPAREN WSP qdescrlist WSP RPAREN )
     * qdescrlist = [ qdescr *( SP qdescr ) ]
     */
quirksQdescrs returns [List<String> qdescrs]
    {
        matchedProduction( "AntlrSchemaValueParser.qdescrs()" );
        qdescrs = new ArrayList<String>();
        String qdescr = null;
    }
    :
    (
        ( 
            qdescr=quirksQdescr { qdescrs.add(qdescr); } 
        )
    |
        ( 
            LPAR 
            qdescr=quirksQdescr { qdescrs.add(qdescr); } 
            (options {greedy=true;} : WHSP)?
            (DOLLAR)?
            (options {greedy=true;} : WHSP)?
            (
                qdescr=quirksQdescr { qdescrs.add(qdescr); } 
                (options {greedy=true;} : WHSP)?
                (DOLLAR)?
                (options {greedy=true;} : WHSP)?
            )*
            RPAR 
        )
    )
    ;
    
    
    
    
    /**
     * ruleid = number
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
ruleid returns [Integer ruleid=null]
    {
        matchedProduction( "AntlrSchemaValueParser.ruleid()" );
    }
    : 
    (
        (WHSP)? 
        n:NUMBER { ruleid = Integer.parseInt(n.getText()); }
    )
    ;


    /**
     * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
     * ruleidlist = ruleid *( SP ruleid )
     */
ruleids returns [List<Integer> ruleids]
    {
        matchedProduction( "AntlrSchemaValueParser.ruleids()" );
        ruleids = new ArrayList<Integer>();
        Integer ruleid = null;
    }
    :
    (
        ( 
            ruleid=ruleid { ruleids.add(ruleid); } 
        )
    |
        ( 
            LPAR 
            ruleid=ruleid { ruleids.add(ruleid); } 
            ( 
                WHSP
                ruleid=ruleid { ruleids.add(ruleid); } 
            )* 
            (WHSP)?
            RPAR 
        )
    )
    ;
    