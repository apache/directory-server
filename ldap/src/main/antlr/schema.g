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

import java.io.* ;
import java.util.* ;

}

/**
 * An antlr generated schema main lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaLexer extends Lexer;

options    {
    k = 5 ;
    exportVocab=AntlrSchema ;
    charVocabulary = '\u0000'..'\uFFFE'; 
    caseSensitive = true ;
    defaultErrorHandler = false ;
}

WHSP : (options{greedy=true;}: ' ' )+ {$setType(Token.SKIP);} ;

LPAR : '(' ;
RPAR : ')' ;
QUOTE : '\'' ;
DOLLAR : '$' ;
LBRACKET : '{' ;
RBRACKET : '}' ;

LEN : LBRACKET ('0'..'9')+ RBRACKET ;

USERAPPLICATIONS : "userApplications" ;
DIRECTORYOPERATION : "directoryOperation" ;
DISTRIBUTEDOPERATION : "distributedOperation" ;
DSAOPERATION : "dSAOperation" ;

SINGLE_VALUE : ( "SINGLE-VALUE" (WHSP)? ) ;
COLLECTIVE : ( "COLLECTIVE" (WHSP)? ) ;
NO_USER_MODIFICATION : ( "NO-USER-MODIFICATION" (WHSP)? ) ;

OBSOLETE : ( "OBSOLETE" (WHSP)? ) ;
ABSTRACT : ( "ABSTRACT" (WHSP)? ) ;
STRUCTURAL : ( "STRUCTURAL" (WHSP)? ) ;
AUXILIARY : ( "AUXILIARY" (WHSP)? ) ;

STARTNUMERICOID : ( LPAR ( numericoid:VALUE ) ) { setText(numericoid.getText().trim()); } ;
NAME : ( "NAME" WHSP qdstrings:VALUES ) { setText(qdstrings.getText().trim()); } ;
DESC : ( "DESC" WHSP qdstring:VALUES ) { setText(qdstring.getText().trim()); } ;
SUP : ( "SUP" WHSP sup:VALUES ) { setText(sup.getText().trim()); } ;
MUST : ( "MUST" WHSP must:VALUES ) { setText(must.getText().trim()); } ;
MAY : ( "MAY" WHSP may:VALUES ) { setText(may.getText()); } ;
EQUALITY : ( "EQUALITY" WHSP equality:VALUES ) { setText(equality.getText().trim()); } ;
ORDERING : ( "ORDERING" WHSP ordering:VALUES ) { setText(ordering.getText().trim()); } ;
SUBSTR : ( "SUBSTR" WHSP substr:VALUES ) { setText(substr.getText().trim()); } ;
SYNTAX : ( "SYNTAX" WHSP syntax:VALUES (len:LEN)? ) { setText(syntax.getText().trim() + (len!=null?len.getText().trim():"")); } ;
USAGE : ( "USAGE" WHSP op:VALUES ) { setText(op.getText().trim()); } ;
APPLIES : ( "APPLIES" WHSP applies:VALUES ) { setText(applies.getText().trim()); } ;
EXTENSION : x:( "X-" ( 'a'..'z' | 'A'..'Z' | '-' | '_' )+ WHSP VALUES ) ; 

protected VALUES : ( VALUE | LPAR  VALUE ( (DOLLAR)? VALUE )* RPAR ) ;
protected VALUE : (WHSP)? ( QUOTED_STRING | UNQUOTED_STRING ) (options {greedy=true;}: WHSP)? ;
protected UNQUOTED_STRING : (options{greedy=true;}: 'a'..'z' | 'A'..'Z' | '0'..'9' | '-' | ';' | '.' )+ ;
protected QUOTED_STRING : ( QUOTE (~'\'')* QUOTE ) ;


/**
 * An antlr generated schema main parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class AntlrSchemaParser extends Parser;
options    {
    k = 3 ;
    defaultErrorHandler = false ;
    //buildAST=true ;
}

{
	static class Extension
	{
	
	    private String key;
	    
	    private List<String> values;
	    
	    public Extension()
	    {
	        this.key = "";
	        this.values = new ArrayList<String>();
	    }
	
	    public String getKey()
	    {
	        return key;
	    }
	
	    public void setKey( String key )
	    {
	        this.key = key;
	    }
	
	    public List<String> getValues()
	    {
	        return values;
	    }
	
	    public void setValues( List<String> values )
	    {
	        this.values = values;
	    }
	    
	    
	    public void addValue( String value )
	    {
	        this.values.add( value );
	    }
	    
	}
}


    /**
     * Production for matching object class descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * ObjectClassDescription = LPAREN WSP
     *     numericoid                 ; object identifier
     *     [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]  ; description
     *     [ SP "OBSOLETE" ]          ; not active
     *     [ SP "SUP" SP oids ]       ; superior object classes
     *     [ SP kind ]                ; kind of class
     *     [ SP "MUST" SP oids ]      ; attribute types
     *     [ SP "MAY" SP oids ]       ; attribute types
     *     extensions WSP RPAREN
     *
     * kind = "ABSTRACT" / "STRUCTURAL" / "AUXILIARY"
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
    */
objectClassDescription returns [ObjectClassDescription ocd = new ObjectClassDescription()]
    :
    ( oid:STARTNUMERICOID { ocd.setOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { ocd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { ocd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( OBSOLETE { ocd.setObsolete( true ); } )
	    |
	    ( sup:SUP { ocd.setSuperiorObjectClasses(oids(sup.getText())); } )
	    |
	    ( ABSTRACT { ocd.setKind( ObjectClassDescription.Kind.ABSTRACT ); }
	      |
	      STRUCTURAL { ocd.setKind( ObjectClassDescription.Kind.STRUCTURAL ); }
	      |
	      AUXILIARY { ocd.setKind( ObjectClassDescription.Kind.AUXILIARY ); } 
	    )
	    |
	    ( must:MUST { ocd.setMustAttributeTypes(oids(must.getText())); } )
	    |
	    ( may:MAY { ocd.setMayAttributeTypes(oids(may.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        ocd.addExtension(ex.getKey(), ex.getValues()); 
	     } )
	)*    
    RPAR
    ;


extension [String s] returns [Extension extension]
    {
        extension = new Extension();
        AntlrSchemaExtensionLexer lexer = new AntlrSchemaExtensionLexer(new StringReader(s));
        AntlrSchemaExtensionParser parser = new AntlrSchemaExtensionParser(lexer);
        extension = parser.extension();
    }
    :
    ;


numericoid [String s] returns [String numericoid]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        numericoid = parser.numericoid();
    }
    :
    ;

oid [String s] returns [String oid]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        oid = parser.oid();
    }
    :
    ;

oids [String s] returns [List<String> oids]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        oids = parser.oids();
    }
    :
    ;

qdescr [String s] returns [String qdescr]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        qdescr = parser.qdescr();
    }
    :
    ;

qdescrs [String s] returns [List<String> qdescrs]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        qdescrs = parser.qdescrs();
    }
    :
    ;

qdstring [String s] returns [String qdstring]
    {
    	AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
        AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
        qdstring = parser.qdstring();
    }
    :
    ;

qdstrings [String s] returns [List<String> qdstrings]
    {
    	AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
        AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
        qdstrings = parser.qdstrings();
    }
    :
    ;

    

    