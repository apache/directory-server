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

import org.apache.directory.shared.ldap.schema.parser.ParserMonitor;
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
SUBSTR : ( "substr" (options {greedy=true;} : WHSP)? substr:VALUES ) { setText(substr.getText().trim()); } ;
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
        AttributeTypeDescription atd = null;
        ObjectClassDescription ocd = null;
        OpenLdapObjectIdentifierMacro oloid = null;
    }
    :
    (
        oloid = openLdapObjectIdentifier { list.add( oloid ); }
        |
        atd = openLdapAttributeType { list.add( atd ); }
        |
        ocd = openLdapObjectClass { list.add( ocd ); }
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
    

openLdapObjectClass returns [ObjectClassDescription ocd]
    {
        matchedProduction( "openLdapObjectClass()" );
    }
    :
    (
        OBJECTCLASS
        ( ocd=objectClassDescription )
    )
    ;
    
    
openLdapAttributeType returns [AttributeTypeDescription atd]
    {
        matchedProduction( "openLdapAttributeType()" );
    }
    :
    (
        ATTRIBUTETYPE
        ( atd=attributeTypeDescription )
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
objectClassDescription returns [ObjectClassDescription ocd = new ObjectClassDescription()]
    {
        matchedProduction( "objectClassDescription()" );
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
        matchedProduction( "attributeTypeDescription()" );
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
        if( !isQuirksModeEnabled )
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
        matchedProduction( "ldapSyntaxDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { lsd.setNumericOid(numericoid(oid.getText())); } )
    (
        ( name:NAME { et.track("NAME", name); lsd.setNames(qdescrs(name.getText())); } )
        |
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
        matchedProduction( "matchingRuleDescription()" );
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
matchingRuleUseDescription returns [MatchingRuleUseDescription mrud = new MatchingRuleUseDescription()]
    {
        matchedProduction( "matchingRuleUseDescription()" );
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
ditContentRuleDescription returns [DITContentRuleDescription dcrd = new DITContentRuleDescription()]
    {
        matchedProduction( "ditContentRuleDescription()" );
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
        matchedProduction( "ditStructureRuleDescription()" );
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
nameFormDescription returns [NameFormDescription nfd = new NameFormDescription()]
    {
        matchedProduction( "nameFormDescription()" );
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
     * ComparatorDescription = LPAREN WSP
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
comparatorDescription returns [ComparatorDescription cd = new ComparatorDescription()]
    {
        matchedProduction( "comparatorDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { cd.setNumericOid(numericoid(oid.getText())); } )
    (
        ( desc:DESC { et.track("DESC", desc); cd.setDescription(qdstring(desc.getText())); } )
        |
        ( fqcn:FQCN { et.track("FQCN", fqcn); cd.setFqcn(fqcn.getText()); } )
        |
        ( bytecode:BYTECODE { et.track("BYTECODE", bytecode); cd.setBytecode(bytecode.getText()); } )
        |
        ( extension:EXTENSION { 
            Extension ex = extension(extension.getText());
            et.track(ex.key, extension); 
            cd.addExtension(ex.key, ex.values); 
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
            if( cd.getBytecode() != null && ( cd.getBytecode().length() % 4 != 0 ) ) {
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
normalizerDescription returns [NormalizerDescription nd = new NormalizerDescription()]
    {
        matchedProduction( "normalizerDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { nd.setNumericOid(numericoid(oid.getText())); } )
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
syntaxCheckerDescription returns [SyntaxCheckerDescription scd = new SyntaxCheckerDescription()]
    {
        matchedProduction( "syntaxCheckerDescription()" );
        ElementTracker et = new ElementTracker();
    }
    :
    ( oid:STARTNUMERICOID { scd.setNumericOid(numericoid(oid.getText())); } )
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


    