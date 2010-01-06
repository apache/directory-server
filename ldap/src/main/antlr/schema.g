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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OpenLdapObjectIdentifierMacro;
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
    k = 8 ;
    exportVocab=AntlrSchema ;
    charVocabulary = '\u0000'..'\uFFFE';
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
    {$setType(Token.SKIP);} //ignore this token
    ;

LPAR : '(' ;
RPAR : ')' ;
QUOTE : '\'' ;
DOLLAR : '$' ;
LBRACKET : '{' ;
RBRACKET : '}' ;

LEN : LBRACKET ('0'..'9')+ RBRACKET ;

SINGLE_VALUE : ( "single-value" (WHSP)? ) ;
COLLECTIVE : ( "collective" (WHSP)? ) ;
NO_USER_MODIFICATION : ( "no-user-modification" (WHSP)? ) ;

OBSOLETE : ( "obsolete" (WHSP)? ) ;
ABSTRACT : ( "abstract" (WHSP)? ) ;
STRUCTURAL : ( "structural" (WHSP)? ) ;
protected AUXILIARY : ( "auxiliary" (WHSP)? ) ;

OBJECTIDENTIFIER : 
    ( "objectidentifier" 
      WHSP
      ( oiName:UNQUOTED_STRING ) 
      WHSP
      ( oiValue:UNQUOTED_STRING ) 
    ) 
    { setText( oiName.getText() + " " + oiValue.getText() ); }
    ;

OBJECTCLASS : ( "objectclass" (WHSP)? ) ;
ATTRIBUTETYPE : ( "attributetype" (WHSP)? ) ;

STARTNUMERICOID : ( LPAR (options {greedy=true;} : WHSP)? ( numericoid:VALUES ) ) { setText(numericoid.getText()); } ;
NAME : ( "name" (options {greedy=true;} : WHSP)? qdstrings:VALUES ) { setText(qdstrings.getText().trim()); } ;
DESC : ( "desc" (options {greedy=true;} : WHSP)? qdstring:VALUES ) { setText(qdstring.getText().trim()); } ;
SUP : ( "sup" (options {greedy=true;} : WHSP)? sup:VALUES ) { setText(sup.getText().trim()); } ;
MUST : ( "must" (options {greedy=true;} : WHSP)? must:VALUES ) { setText(must.getText().trim()); } ;
MAY : ( "may" (options {greedy=true;} : WHSP)? may:VALUES ) { setText(may.getText()); } ;
protected AUX : ( "aux" (options {greedy=true;} : WHSP)? aux:VALUES ) { setText(aux.getText()); } ;
NOT : ( "not" (options {greedy=true;} : WHSP)? not:VALUES ) { setText(not.getText()); } ;
FORM : ( "form" (options {greedy=true;} : WHSP)? form:VALUES ) { setText(form.getText()); } ;
OC : ( "oc" (options {greedy=true;} : WHSP)? oc:VALUES ) { setText(oc.getText()); } ;
EQUALITY : ( "equality" (options {greedy=true;} : WHSP)? equality:VALUES ) { setText(equality.getText().trim()); } ;
ORDERING : ( "ordering" (options {greedy=true;} : WHSP)? ordering:VALUES ) { setText(ordering.getText().trim()); } ;
SUBSTR : ( "substr" (options {greedy=true;} : WHSP)? substring:VALUES ) { setText(substring.getText().trim()); } ;
SYNTAX : ( "syntax" (options {greedy=true;} : WHSP)? syntax:VALUES (len:LEN)? ) { setText(syntax.getText().trim() + (len!=null?len.getText().trim():"")); } ;
APPLIES : ( "applies" (options {greedy=true;} : WHSP)? applies:VALUES ) { setText(applies.getText().trim()); } ;
EXTENSION : x:( "x-" ( options {greedy=true;} : 'a'..'z' | '-' | '_' )+ (options {greedy=true;} : WHSP)? VALUES ) ; 
FQCN : ( "fqcn" (options {greedy=true;} : WHSP)? fqcn:FQCN_VALUE ) { setText(fqcn.getText().trim()); } ;
BYTECODE : ( "bytecode" (options {greedy=true;} : WHSP)? bytecode:BYTECODE_VALUE ) { setText(bytecode.getText().trim()); } ;

AUX_OR_AUXILIARY :
    ( AUXILIARY ) => AUXILIARY { $setType( AUXILIARY ); }
    |
    ( AUX ) { $setType( AUX ); }
    ;

protected VALUES : ( VALUE | LPAR  VALUE ( (DOLLAR)? VALUE )* RPAR ) ;
protected VALUE : (WHSP)? ( QUOTED_STRING | UNQUOTED_STRING ) (options {greedy=true;}: WHSP)? ;
protected UNQUOTED_STRING : (options{greedy=true;}: 'a'..'z' | '0'..'9' | '-' | '_' | ';' | '.' | ':' )+ ;
protected QUOTED_STRING : ( QUOTE (~'\'')* QUOTE ) ;
protected FQCN_VALUE : ( FQCN_IDENTIFIER ( '.' FQCN_IDENTIFIER )* ) ;
protected FQCN_IDENTIFIER : ( FQCN_LETTER ( FQCN_LETTERORDIGIT )* ) ;
protected FQCN_LETTER : 
       '\u0024' |
       '\u005f' |
       '\u0061'..'\u007a' |
       '\u00c0'..'\u00d6' |
       '\u00d8'..'\u00f6' |
       '\u00f8'..'\u00ff' |
       '\u0100'..'\u1fff' |
       '\u3040'..'\u318f' |
       '\u3300'..'\u337f' |
       '\u3400'..'\u3d2d' |
       '\u4e00'..'\u9fff' |
       '\uf900'..'\ufaff' ;
protected FQCN_LETTERORDIGIT : 
       '\u0024' |
       '\u005f' |
       '\u0061'..'\u007a' |
       '\u00c0'..'\u00d6' |
       '\u00d8'..'\u00f6' |
       '\u00f8'..'\u00ff' |
       '\u0100'..'\u1fff' |
       '\u3040'..'\u318f' |
       '\u3300'..'\u337f' |
       '\u3400'..'\u3d2d' |
       '\u4e00'..'\u9fff' |
       '\uf900'..'\ufaff' |
       '\u0030'..'\u0039' ;
protected BYTECODE_VALUE : ( 'a'..'z' | '0'..'9' | '+' | '/' | '=' )+ ;


USAGE : ( "usage" (WHSP)? ) ;
USER_APPLICATIONS : ( "userapplications" (WHSP)? ) ;
DIRECTORY_OPERATION : ( "directoryoperation" (WHSP)? ) ;
DISTRIBUTED_OPERATION : ( "distributedoperation" (WHSP)? ) ;
DSA_OPERATION : ( "dsaoperation" (WHSP)? ) ;

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
    private ParserMonitor monitor = null;
    private boolean isQuirksModeEnabled = false;
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
    public void setQuirksMode( boolean enabled )
    {
        this.isQuirksModeEnabled = enabled;
    }
    public boolean isQuirksMode()
    {
        return this.isQuirksModeEnabled;
    }
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

openLdapSchema returns [List<Object> list = new ArrayList<Object>()]
    {
        AttributeType attributeType = null;
        ObjectClass objectClass = null;
        OpenLdapObjectIdentifierMacro oloid = null;
    }
    :
    (
        oloid = openLdapObjectIdentifier { list.add( oloid ); }
        |
        attributeType = openLdapAttributeType { list.add( attributeType ); }
        |
        objectClass = openLdapObjectClass { list.add( objectClass ); }
    )*
    ;

openLdapObjectIdentifier returns [OpenLdapObjectIdentifierMacro oloid]
    {
        matchedProduction( "openLdapObjectIdentifier()" );
    }
    :
    (
        oi:OBJECTIDENTIFIER 
        {
            String[] nameAndValue = oi.getText().split( " " );
            oloid = new OpenLdapObjectIdentifierMacro();
            oloid.setName( nameAndValue[0] );
            oloid.setRawOidOrNameSuffix( nameAndValue[1] );
        }
    )
    ;
    

openLdapObjectClass returns [ObjectClass objectClass]
    {
        matchedProduction( "openLdapObjectClass()" );
    }
    :
    (
        OBJECTCLASS
        ( objectClass=objectClassDescription )
    )
    ;
    
    
openLdapAttributeType returns [AttributeType attributeType]
    {
        matchedProduction( "openLdapAttributeType()" );
    }
    :
    (
        ATTRIBUTETYPE
        ( attributeType=attributeTypeDescription )
    )
    ;


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
objectClassDescription returns [ObjectClass objectClass]
    {
        matchedProduction( "objectClassDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { objectClass = new ObjectClass(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); objectClass.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); objectClass.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); objectClass.setObsolete( true ); } )
        |
        ( sup:SUP { et.track("SUP", sup); objectClass.setSuperiorOids(oids(sup.getText())); } )
        |
        ( kind1:ABSTRACT { et.track("KIND", kind1); objectClass.setType( ObjectClassTypeEnum.ABSTRACT ); }
          |
          kind2:STRUCTURAL { et.track("KIND", kind2); objectClass.setType( ObjectClassTypeEnum.STRUCTURAL ); }
          |
          kind3:AUXILIARY { et.track("KIND", kind3); objectClass.setType( ObjectClassTypeEnum.AUXILIARY ); } 
        )
        |
        ( must:MUST { et.track("MUST", must); objectClass.setMustAttributeTypeOids(oids(must.getText())); } )
        |
        ( may:MAY { et.track("MAY", may); objectClass.setMayAttributeTypeOids(oids(may.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            objectClass.addExtension(ex.key, ex.values); 
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
attributeTypeDescription returns [AttributeType attributeType]
    {
        matchedProduction( "attributeTypeDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { attributeType = new AttributeType(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); attributeType.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); attributeType.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); attributeType.setObsolete( true ); } )
        |
        ( superior:SUP { et.track("SUP", superior); attributeType.setSuperiorOid(oid(superior.getText())); } )
        |
        ( equality:EQUALITY { et.track("EQUALITY", equality); attributeType.setEqualityOid(oid(equality.getText())); } )
        |
        ( ordering:ORDERING { et.track("ORDERING", ordering); attributeType.setOrderingOid(oid(ordering.getText())); } )
        |
        ( substring:SUBSTR { et.track("SUBSTR", substring); attributeType.setSubstringOid(oid(substring.getText())); } )
        |
        ( syntax:SYNTAX { 
           et.track("SYNTAX", syntax); 
            NoidLen noidlen = noidlen(syntax.getText());
            attributeType.setSyntaxOid(noidlen.noid); 
            attributeType.setSyntaxLength(noidlen.len);
          } )
        |
        ( singleValued:SINGLE_VALUE { et.track("SINGLE_VALUE", singleValued); attributeType.setSingleValued( true ); } )
        |
        ( collective:COLLECTIVE { et.track("COLLECTIVE", collective); attributeType.setCollective( true ); } )
        |
        ( noUserModification:NO_USER_MODIFICATION { et.track("NO_USER_MODIFICATION", noUserModification); attributeType.setUserModifiable( false ); } )
        |
        ( usage1:USAGE (WHSP)* USER_APPLICATIONS { et.track("USAGE", usage1); attributeType.setUsage( UsageEnum.USER_APPLICATIONS ); }
          |
          usage2:USAGE DIRECTORY_OPERATION { et.track("USAGE", usage2); attributeType.setUsage( UsageEnum.DIRECTORY_OPERATION ); }
          |
          usage3:USAGE DISTRIBUTED_OPERATION { et.track("USAGE", usage3); attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION ); } 
          |
          usage4:USAGE DSA_OPERATION { et.track("USAGE", usage4); attributeType.setUsage( UsageEnum.DSA_OPERATION ); } 
        )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            attributeType.addExtension(ex.key, ex.values); 
         } )
    )*    
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("SYNTAX") && !et.contains("SUP") ) 
            {
                throw new SemanticException( "One of SYNTAX or SUP is required", null, 0, 0 );
            }
        
            // COLLECTIVE requires USAGE userApplications
            if ( attributeType.isCollective() && ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) )
            {
                throw new SemanticException( "COLLECTIVE requires USAGE userApplications", null, 0, 0 );
            }
        
            // NO-USER-MODIFICATION requires an operational USAGE.
            if ( !attributeType.isUserModifiable() && ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) )
            {
                throw new SemanticException( "NO-USER-MODIFICATION requires an operational USAGE", null, 0, 0 );
            }
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
ldapSyntaxDescription returns [LdapSyntax ldapSyntax]
    {
        matchedProduction( "ldapSyntaxDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { ldapSyntax = new LdapSyntax(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); ldapSyntax.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); ldapSyntax.setDescription(qdstring(desc.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            ldapSyntax.addExtension(ex.key, ex.values); 
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
matchingRuleDescription returns [MatchingRule matchingRule]
    {
        matchedProduction( "matchingRuleDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { matchingRule = new MatchingRule(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); matchingRule.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); matchingRule.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); matchingRule.setObsolete( true ); } )
        |
        ( syntax:SYNTAX { et.track("SYNTAX", syntax); matchingRule.setSyntaxOid(numericoid(syntax.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            matchingRule.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {    
            // semantic check: required elements
            if( !et.contains("SYNTAX") ) {
                throw new SemanticException( "SYNTAX is required", null, 0, 0 );
            }
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
matchingRuleUseDescription returns [MatchingRuleUse matchingRuleUse]
    {
        matchedProduction( "matchingRuleUseDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { matchingRuleUse = new MatchingRuleUse(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); matchingRuleUse.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); matchingRuleUse.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); matchingRuleUse.setObsolete( true ); } )
        |
        ( applies:APPLIES { et.track("APPLIES", applies); matchingRuleUse.setApplicableAttributeOids(oids(applies.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            matchingRuleUse.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("APPLIES") ) {
                throw new SemanticException( "APPLIES is required", null, 0, 0 );
            }
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
ditContentRuleDescription returns [DITContentRule ditContentRule]
    {
        matchedProduction( "ditContentRuleDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { ditContentRule = new DITContentRule(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); ditContentRule.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); ditContentRule.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); ditContentRule.setObsolete( true ); } )
        |
        ( aux:AUX { et.track("AUX", aux); ditContentRule.setAuxObjectClassOids(oids(aux.getText())); } )
        |
        ( must:MUST { et.track("MUST", must); ditContentRule.setMustAttributeTypeOids(oids(must.getText())); } )
        |
        ( may:MAY { et.track("MAY", may); ditContentRule.setMayAttributeTypeOids(oids(may.getText())); } )
        |
        ( not:NOT { et.track("NOT", not); ditContentRule.setNotAttributeTypeOids(oids(not.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            ditContentRule.addExtension(ex.key, ex.values); 
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
ditStructureRuleDescription returns [DITStructureRule ditStructureRule]
    {
        matchedProduction( "ditStructureRuleDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( ruleid:STARTNUMERICOID { ditStructureRule = new DITStructureRule(ruleid(ruleid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); ditStructureRule.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); ditStructureRule.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); ditStructureRule.setObsolete( true ); } )
        |
        ( form:FORM { et.track("FORM", form); ditStructureRule.setForm(oid(form.getText())); } )
        |
        ( sup:SUP { et.track("SUP", sup); ditStructureRule.setSuperRules(ruleids(sup.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            ditStructureRule.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("FORM") ) {
                throw new SemanticException( "FORM is required", null, 0, 0 );
            }
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
nameFormDescription returns [NameForm nameForm]
    {
        matchedProduction( "nameFormDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { nameForm = new NameForm(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); nameForm.setNames(qdescrs(name.getText())); } )
        |
        ( desc:DESC { et.track("DESC", desc); nameForm.setDescription(qdstring(desc.getText())); } )
        |
        ( obsolete:OBSOLETE { et.track("OBSOLETE", obsolete); nameForm.setObsolete( true ); } )
        |
        ( oc:OC { et.track("OC", oc); nameForm.setStructuralObjectClassOid(oid(oc.getText())); } )
        |
        ( must:MUST { et.track("MUST", must); nameForm.setMustAttributeTypeOids(oids(must.getText())); } )
        |
        ( may:MAY { et.track("MAY", may); nameForm.setMayAttributeTypeOids(oids(may.getText())); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            nameForm.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
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
    }
    ;
    

    /**
     * Production for comparator descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * LdapComparator = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
ldapComparator returns [LdapComparatorDescription lcd]
    {
        matchedProduction( "ldapComparator()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { lcd = new LdapComparatorDescription(numericoid(oid.getText())); } )
    (
        ( desc:DESC { et.track("DESC", desc); lcd.setDescription(qdstring(desc.getText())); } )
        |
        ( fqcn:FQCN { et.track("FQCN", fqcn); lcd.setFqcn(fqcn.getText()); } )
        |
        ( bytecode:BYTECODE { et.track("BYTECODE", bytecode); lcd.setBytecode(bytecode.getText()); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            lcd.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("FQCN") ) {
                throw new SemanticException( "FQCN is required", null, 0, 0 );
            }
        
            // semantic check: length should be divisible by 4
            if( ( lcd.getBytecode() != null ) && ( lcd.getBytecode().length() % 4 != 0 ) ) {
                throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
            }
        }
    }
    ;
    

    /**
     * Production for normalizer descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * NormalizerDescription = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
normalizerDescription returns [NormalizerDescription nd]
    {
        matchedProduction( "normalizerDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { nd = new NormalizerDescription(numericoid(oid.getText())); } )
    (
        ( desc:DESC { et.track("DESC", desc); nd.setDescription(qdstring(desc.getText())); } )
        |
        ( fqcn:FQCN { et.track("FQCN", fqcn); nd.setFqcn(fqcn.getText()); } )
        |
        ( bytecode:BYTECODE { et.track("BYTECODE", bytecode); nd.setBytecode(bytecode.getText()); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            nd.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("FQCN") ) {
                throw new SemanticException( "FQCN is required", null, 0, 0 );
            }
        
            // semantic check: length should be divisible by 4
            if( nd.getBytecode() != null && ( nd.getBytecode().length() % 4 != 0 ) ) {
                throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
            }     
        }   
    }
    ;
    

    /**
     * Production for syntax checker descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * SyntaxCheckerDescription = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
syntaxCheckerDescription returns [SyntaxCheckerDescription scd]
    {
        matchedProduction( "syntaxCheckerDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { scd = new SyntaxCheckerDescription(numericoid(oid.getText())); } )
    (
        ( desc:DESC { et.track("DESC", desc); scd.setDescription(qdstring(desc.getText())); } )
        |
        ( fqcn:FQCN { et.track("FQCN", fqcn); scd.setFqcn(fqcn.getText()); } )
        |
        ( bytecode:BYTECODE { et.track("BYTECODE", bytecode); scd.setBytecode(bytecode.getText()); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            scd.addExtension(ex.key, ex.values); 
         } )
    )*
    RPAR
    {
        if( !isQuirksModeEnabled )
        {
            // semantic check: required elements
            if( !et.contains("FQCN") ) {
                throw new SemanticException( "FQCN is required", null, 0, 0 );
            }
        
            // semantic check: length should be divisible by 4
            if( scd.getBytecode() != null && ( scd.getBytecode().length() % 4 != 0 ) ) {
                throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
            }  
        }      
    }
    ;
    



noidlen [String s] returns [NoidLen noidlen]
    {
        matchedProduction( "noidlen()" );
        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        parser.setParserMonitor(monitor);
        noidlen = isQuirksModeEnabled ? parser.quirksNoidlen() : parser.noidlen();
    }
    :
    ;


extension [String s] returns [Extension extension]
    {
        matchedProduction( "extension()" );
        AntlrSchemaExtensionLexer lexer = new AntlrSchemaExtensionLexer(new StringReader(s));
        AntlrSchemaExtensionParser parser = new AntlrSchemaExtensionParser(lexer);
        extension = parser.extension();
    }
    :
    ;


numericoid [String s] returns [String numericoid]
    {
        matchedProduction( "numericoid()");
        if(isQuirksModeEnabled)
        {
             numericoid = oid(s);
        }
        else
        {
	        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
	        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
	        parser.setParserMonitor(monitor);
	        numericoid = parser.numericoid();
        }
    }
    :
    ;

oid [String s] returns [String oid]
    {
        matchedProduction( "oid()" );
        List<String> oids = oids(s);
        if( oids.size() != 1 ) 
        {
            throw new SemanticException( "Exactly one OID expected", null, 0, 0 );
        }
        oid = oids.get(0);
    }
    :
    ;

oids [String s] returns [List<String> oids]
    {
        matchedProduction( "oids()" );
        if(isQuirksModeEnabled)
        {
             oids = qdescrs(s);
        }
        else
        {
	        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
	        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
	        parser.setParserMonitor(monitor);
	        oids = parser.oids();
	    }
    }
    :
    ;

qdescr [String s] returns [String qdescr]
    {
        matchedProduction( "qdescr()" );
        List<String> qdescrs = qdescrs(s);
        if( qdescrs.size() != 1 ) 
        {
            throw new SemanticException( "Exactly one qdescrs expected", null, 0, 0 );
        }
        qdescr = qdescrs.get(0);
    }
    :
    ;

qdescrs [String s] returns [List<String> qdescrs]
    {
        matchedProduction( "qdescrs()" );
        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        parser.setParserMonitor(monitor);
        qdescrs = isQuirksModeEnabled ? parser.quirksQdescrs() : parser.qdescrs();
    }
    :
    ;

qdstring [String s] returns [String qdstring]
    {
        matchedProduction( "qdstring()" );
        List<String> qdstrings = qdstrings(s);
        if( qdstrings.size() != 1 ) 
        {
            throw new SemanticException( "Exactly one qdstrings expected", null, 0, 0 );
        }
        qdstring = qdstrings.get(0);
    }
    :
    ;

qdstrings [String s] returns [List<String> qdstrings]
    {
        matchedProduction( "qdstrings()" );
        AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
        AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
        parser.setParserMonitor(monitor);
        qdstrings = parser.qdstrings();
    }
    :
    ;

ruleid [String s] returns [Integer ruleid]
    {
        matchedProduction( "ruleid()" );
        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        parser.setParserMonitor(monitor);
        ruleid = parser.ruleid();
    }
    :
    ;

ruleids [String s] returns [List<Integer> ruleids]
    {
        matchedProduction( "ruleids()" );
        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
        parser.setParserMonitor(monitor);
        ruleids = parser.ruleids();
    }
    :
    ;


    
