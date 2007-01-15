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
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.UsageEnum;

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
AUX : ( "AUX" WHSP aux:VALUES ) { setText(aux.getText()); } ;
NOT : ( "NOT" WHSP not:VALUES ) { setText(not.getText()); } ;
FORM : ( "FORM" WHSP form:VALUES ) { setText(form.getText()); } ;
OC : ( "OC" WHSP oc:VALUES ) { setText(oc.getText()); } ;
EQUALITY : ( "EQUALITY" WHSP equality:VALUES ) { setText(equality.getText().trim()); } ;
ORDERING : ( "ORDERING" WHSP ordering:VALUES ) { setText(ordering.getText().trim()); } ;
SUBSTR : ( "SUBSTR" WHSP substr:VALUES ) { setText(substr.getText().trim()); } ;
SYNTAX : ( "SYNTAX" WHSP syntax:VALUES (len:LEN)? ) { setText(syntax.getText().trim() + (len!=null?len.getText().trim():"")); } ;
APPLIES : ( "APPLIES" WHSP applies:VALUES ) { setText(applies.getText().trim()); } ;
EXTENSION : x:( "X-" ( 'a'..'z' | 'A'..'Z' | '-' | '_' )+ WHSP VALUES ) ; 

protected VALUES : ( VALUE | LPAR  VALUE ( (DOLLAR)? VALUE )* RPAR ) ;
protected VALUE : (WHSP)? ( QUOTED_STRING | UNQUOTED_STRING ) (options {greedy=true;}: WHSP)? ;
protected UNQUOTED_STRING : (options{greedy=true;}: 'a'..'z' | 'A'..'Z' | '0'..'9' | '-' | ';' | '.' )+ ;
protected QUOTED_STRING : ( QUOTE (~'\'')* QUOTE ) ;

USAGE : ( "USAGE" (WHSP)? ) ;
USER_APPLICATIONS : ( "userApplications" (WHSP)? ) ;
DIRECTORY_OPERATION : ( "directoryOperation" (WHSP)? ) ;
DISTRIBUTED_OPERATION : ( "distributedOperation" (WHSP)? ) ;
DSA_OPERATION : ( "dSAOperation" (WHSP)? ) ;

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
	    String key = "";
	    List<String> values = new ArrayList<String>();
	    
	    public void addValue( String value )
	    {
	        this.values.add( value );
	    }
	}
	static class NoidLen
	{
	    String noid = "";
	    int len = 0;
	}
    static class ElementTracker
	{
	    Map<String, Integer> elementMap = new HashMap<String, Integer>();
	    void track(String element, Token token) throws SemanticException 
	    {
	        if(elementMap.containsKey(element))
	        {
	            throw new SemanticException( element + " appears twice.", token.getFilename(), token.getLine() , token.getColumn() );
	        }
	        elementMap.put(element, new Integer(1));
	    }
	    boolean contains(String element) 
	    {
	        return elementMap.containsKey(element);
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
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { ocd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); ocd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); ocd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); ocd.setObsolete( true ); } )
	    |
	    ( sup:SUP { et.track("SUP", sup); ocd.setSuperiorObjectClasses(oids(sup.getText())); } )
	    |
	    ( kind1:ABSTRACT { et.track("KIND", kind1); ocd.setKind( ObjectClassTypeEnum.ABSTRACT ); }
	      |
	      kind2:STRUCTURAL { et.track("KIND", kind2); ocd.setKind( ObjectClassTypeEnum.STRUCTURAL ); }
	      |
	      kind3:AUXILIARY { et.track("KIND", kind3); ocd.setKind( ObjectClassTypeEnum.AUXILIARY ); } 
	    )
	    |
	    ( must:MUST { et.track("MUST", must); ocd.setMustAttributeTypes(oids(must.getText())); } )
	    |
	    ( may:MAY { et.track("MAY", may); ocd.setMayAttributeTypes(oids(may.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        ocd.addExtension(ex.key, ex.values); 
	     } )
	)*    
    RPAR
    ;


    /**
     * Production for matching attribute type descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * AttributeTypeDescription = LPAREN WSP
     *     numericoid                    ; object identifier
     *     [ SP "NAME" SP qdescrs ]      ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]     ; description
     *     [ SP "OBSOLETE" ]             ; not active
     *     [ SP "SUP" SP oid ]           ; supertype
     *     [ SP "EQUALITY" SP oid ]      ; equality matching rule
     *     [ SP "ORDERING" SP oid ]      ; ordering matching rule
     *     [ SP "SUBSTR" SP oid ]        ; substrings matching rule
     *     [ SP "SYNTAX" SP noidlen ]    ; value syntax
     *     [ SP "SINGLE-VALUE" ]         ; single-value
     *     [ SP "COLLECTIVE" ]           ; collective
     *     [ SP "NO-USER-MODIFICATION" ] ; not user modifiable
     *     [ SP "USAGE" SP usage ]       ; usage
     *     extensions WSP RPAREN         ; extensions
     * 
     * usage = "userApplications"     /  ; user
     *         "directoryOperation"   /  ; directory operational
     *         "distributedOperation" /  ; DSA-shared operational
     *         "dSAOperation"            ; DSA-specific operational     
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
    */
attributeTypeDescription returns [AttributeTypeDescription atd = new AttributeTypeDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { atd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); atd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); atd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); atd.setObsolete( true ); } )
	    |
	    ( sup:SUP { et.track("SUP", sup); atd.setSuperType(oid(sup.getText())); } )
	    |
        ( equality:EQUALITY { et.track("EQUALITY", equality); atd.setEqualityMatchingRule(oid(equality.getText())); } )
        |
        ( ordering:ORDERING { et.track("ORDERING", ordering); atd.setOrderingMatchingRule(oid(ordering.getText())); } )
        |
        ( substr:SUBSTR { et.track("SUBSTR", substr); atd.setSubstringsMatchingRule(oid(substr.getText())); } )
        |
        ( syntax:SYNTAX { 
           et.track("SYNTAX", syntax); 
            NoidLen noidlen = noidlen(syntax.getText());
            atd.setSyntax(noidlen.noid); 
            atd.setSyntaxLength(noidlen.len);
          } )
        |
        ( singleValue:SINGLE_VALUE { et.track("SINGLE_VALUE", singleValue); atd.setSingleValued( true ); } )
        |
        ( collective:COLLECTIVE { et.track("COLLECTIVE", collective); atd.setCollective( true ); } )
        |
        ( noUserModification:NO_USER_MODIFICATION { et.track("NO_USER_MODIFICATION", noUserModification); atd.setUserModifiable( false ); } )
        |
	    ( usage1:USAGE (WHSP)* USER_APPLICATIONS { et.track("USAGE", usage1); atd.setUsage( UsageEnum.USER_APPLICATIONS ); }
	      |
	      usage2:USAGE DIRECTORY_OPERATION { et.track("USAGE", usage2); atd.setUsage( UsageEnum.DIRECTORY_OPERATION ); }
	      |
	      usage3:USAGE DISTRIBUTED_OPERATION { et.track("USAGE", usage3); atd.setUsage( UsageEnum.DISTRIBUTED_OPERATION ); } 
	      |
	      usage4:USAGE DSA_OPERATION { et.track("USAGE", usage4); atd.setUsage( UsageEnum.DSA_OPERATION ); } 
	    )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        atd.addExtension(ex.key, ex.values); 
	     } )
	)*    
    RPAR
    {
        // semantic check: required elements
        if( !et.contains("SYNTAX") && !et.contains("SUP") ) 
        {
            throw new SemanticException( "One of SYNTAX or SUP is required", null, 0, 0 );
        }
        
        // COLLECTIVE requires USAGE userApplications
        if ( atd.isCollective() && ( atd.getUsage() != UsageEnum.USER_APPLICATIONS ) )
        {
            throw new SemanticException( "COLLECTIVE requires USAGE userApplications", null, 0, 0 );
        }
        
        // NO-USER-MODIFICATION requires an operational USAGE.
        if ( !atd.isUserModifiable() && ( atd.getUsage() == UsageEnum.USER_APPLICATIONS ) )
        {
            throw new SemanticException( "NO-USER-MODIFICATION requires an operational USAGE", null, 0, 0 );
        }
    }
    ;


    /**
     * Production for matching ldap syntax descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * SyntaxDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "DESC" SP qdstring ]  ; description
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
ldapSyntaxDescription returns [LdapSyntaxDescription lsd = new LdapSyntaxDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { lsd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( desc:DESC { et.track("DESC", desc); lsd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        lsd.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    ;



    /**
     * Production for matching rule descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * MatchingRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "SYNTAX" SP numericoid  ; assertion syntax
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
matchingRuleDescription returns [MatchingRuleDescription mrd = new MatchingRuleDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { mrd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); mrd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); mrd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); mrd.setObsolete( true ); } )
	    |
        ( syntax:SYNTAX { et.track("SYNTAX", syntax); mrd.setSyntax(numericoid(syntax.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        mrd.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    {
        // semantic check: required elements
        if( !et.contains("SYNTAX") ) {
            throw new SemanticException( "SYNTAX is required", null, 0, 0 );
        }
    }
    ;


    /**
     * Production for matching rule use descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * MatchingRuleUseDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "APPLIES" SP oids       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
matchingRuleUseDescription returns [MatchingRuleUseDescription mrud = new MatchingRuleUseDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { mrud.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); mrud.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); mrud.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); mrud.setObsolete( true ); } )
	    |
        ( applies:APPLIES { et.track("APPLIES", applies); mrud.setApplicableAttributes(oids(applies.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        mrud.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    {
        // semantic check: required elements
        if( !et.contains("APPLIES") ) {
            throw new SemanticException( "APPLIES is required", null, 0, 0 );
        }
    }
    ;


    /**
     * Production for DIT content rule descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * DITContentRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    [ SP "AUX" SP oids ]       ; auxiliary object classes
     *    [ SP "MUST" SP oids ]      ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    [ SP "NOT" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
ditContentRuleDescription returns [DITContentRuleDescription dcrd = new DITContentRuleDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { dcrd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); dcrd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); dcrd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); dcrd.setObsolete( true ); } )
	    |
	    ( aux:AUX { et.track("AUX", aux); dcrd.setAuxiliaryObjectClasses(oids(aux.getText())); } )
	    |
	    ( must:MUST { et.track("MUST", must); dcrd.setMustAttributeTypes(oids(must.getText())); } )
	    |
	    ( may:MAY { et.track("MAY", may); dcrd.setMayAttributeTypes(oids(may.getText())); } )
	    |
	    ( not:NOT { et.track("NOT", not); dcrd.setNotAttributeTypes(oids(not.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        dcrd.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    ;


    /**
     * Production for DIT structure rules descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * DITStructureRuleDescription = LPAREN WSP
     *   ruleid                     ; rule identifier
     *   [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *   [ SP "DESC" SP qdstring ]  ; description
     *   [ SP "OBSOLETE" ]          ; not active
     *   SP "FORM" SP oid           ; NameForm
     *   [ SP "SUP" ruleids ]       ; superior rules
     *   extensions WSP RPAREN      ; extensions
     *
     * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
     * ruleidlist = ruleid *( SP ruleid )
     * ruleid = number
     * </pre>
    */
ditStructureRuleDescription returns [DITStructureRuleDescription dsrd = new DITStructureRuleDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( ruleid:STARTNUMERICOID { dsrd.setRuleId(ruleid(ruleid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); dsrd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); dsrd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); dsrd.setObsolete( true ); } )
	    |
	    ( form:FORM { et.track("FORM", form); dsrd.setForm(oid(form.getText())); } )
	    |
	    ( sup:SUP { et.track("SUP", sup); dsrd.setSuperRules(ruleids(sup.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        dsrd.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    {
        // semantic check: required elements
        if( !et.contains("FORM") ) {
            throw new SemanticException( "FORM is required", null, 0, 0 );
        }
    }
    ;


    /**
     * Production for name form descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * NameFormDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "OC" SP oid             ; structural object class
     *    SP "MUST" SP oids          ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
nameFormDescription returns [NameFormDescription nfd = new NameFormDescription()]
    {
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { nfd.setNumericOid(numericoid(oid.getText())); } )
    (
	    ( name:NAME { et.track("NAME", name); nfd.setNames(qdescrs(name.getText())); } )
	    |
	    ( desc:DESC { et.track("DESC", desc); nfd.setDescription(qdstring(desc.getText())); } )
	    |
	    ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); nfd.setObsolete( true ); } )
	    |
	    ( oc:OC { et.track("OC", oc); nfd.setStructuralObjectClass(oid(oc.getText())); } )
	    |
	    ( must:MUST { et.track("MUST", must); nfd.setMustAttributeTypes(oids(must.getText())); } )
	    |
	    ( may:MAY { et.track("MAY", may); nfd.setMayAttributeTypes(oids(may.getText())); } )
	    |
	    ( extension:EXTENSION { 
	        Extension ex = extension(extension.getText());
	        et.track(ex.key, extension); 
	        nfd.addExtension(ex.key, ex.values); 
	     } )
    )*
    RPAR
    {
        // semantic check: required elements
        if( !et.contains("MUST") ) {
            throw new SemanticException( "MUST is required", null, 0, 0 );
        }
        if( !et.contains("OC") ) {
            throw new SemanticException( "OC is required", null, 0, 0 );
        }
        
        // semantic check: MUST and MAY must be disjoint
        //List<String> aList = new ArrayList<String>( nfd.getMustAttributeTypes() );
        //aList.retainAll( nfd.getMayAttributeTypes() );
        //if( !aList.isEmpty() ) 
        //{
        //    throw new SemanticException( "MUST and MAY must be disjoint, "+aList.get( 0 )+" appears in both", null, 0, 0 );
        //}
    }
    ;
    



noidlen [String s] returns [NoidLen noidlen]
    {
        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        noidlen = parser.noidlen();
    }
    :
    ;


extension [String s] returns [Extension extension]
    {
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

ruleid [String s] returns [Integer ruleid]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        ruleid = parser.ruleid();
    }
    :
    ;

ruleids [String s] returns [List<Integer> ruleids]
    {
    	AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        ruleids = parser.ruleids();
    }
    :
    ;


    