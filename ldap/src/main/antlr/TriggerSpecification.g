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


package org.apache.directory.shared.ldap.trigger;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

}


// ----------------------------------------------------------------------------
// parser class definition
// ----------------------------------------------------------------------------

/**
 * The ANTLR generated TriggerSpecification parser.
 * 
 * @see http://docs.safehaus.org/display/APACHEDS/Grammar+for+Triggers
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
class AntlrTriggerSpecificationParser extends Parser;


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
    private static final Logger log = LoggerFactory.getLogger( AntlrTriggerSpecificationParser.class );
    
    private NormalizerMappingResolver resolver;
    private boolean isNormalizing = false;
    
    private ActionTime triggerActionTime;
    
    private LdapOperation triggerLdapOperation;
    
    private String triggerStoredProcedureName;
    
    private List triggerStoredProcedureOptions;
    
    private List triggerStoredProcedureParameters;
    
    
    public void init()
    {
    }


    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizerMappingResolver( NormalizerMappingResolver resolver )
    {
        this.resolver = resolver;
        this.isNormalizing = true;
    }
}


// ----------------------------------------------------------------------------
//  parser productions
// ----------------------------------------------------------------------------

wrapperEntryPoint returns [ TriggerSpecification triggerSpec ]
{
    log.debug( "entered wrapperEntryPoint()" );
    triggerSpec = null;
    triggerStoredProcedureOptions = new ArrayList();
    triggerStoredProcedureParameters = new ArrayList();
}
    :
    ( SP )* triggerSpec=triggerSpecification ( SP )* EOF
    ;

// -----------------------------------------------------------------------------
//  main rules
// -----------------------------------------------------------------------------

triggerSpecification returns [ TriggerSpecification triggerSpec ]
{
    log.debug( "entered triggerSpecification()" );
    triggerSpec = null;
}
    :
    actionTime ( SP )+ ldapOperationAndStoredProcedureCall
    { triggerSpec = new TriggerSpecification( triggerLdapOperation,
                                              triggerActionTime,
                                              triggerStoredProcedureName,
                                              triggerStoredProcedureOptions,
                                              triggerStoredProcedureParameters
                                            );
    }
    ;
    
actionTime
{
    log.debug( "entered actionTime()" );
}
    : ID_AFTER { triggerActionTime = ActionTime.AFTER; }
    ;
    
ldapOperationAndStoredProcedureCall
{
    log.debug( "entered ldapOperationAndStoredProcedureCall()" );
}
    : modifyOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.MODIFY; }
    | addOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.ADD; }
    | deleteOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.DELETE; }
    | modifyDNOperationAndStoredProcedureCall
    ;

// -----------------------------------------------------------------------------
//  XXXOperationAndStoredProcedureCall
// -----------------------------------------------------------------------------

modifyOperationAndStoredProcedureCall
{
    log.debug( "entered modifyOperationAndStoredProcedureCall()" );
}
    :
    ID_modify theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( modifyStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

addOperationAndStoredProcedureCall
{
    log.debug( "entered addOperationAndStoredProcedureCall()" );
}
    :
    ID_add theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( addStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

deleteOperationAndStoredProcedureCall
{
    log.debug( "entered deleteOperationAndStoredProcedureCall()" );
}
    :
    ID_delete theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( deleteStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

modifyDNOperationAndStoredProcedureCall
{
    log.debug( "entered modifyDNOperationAndStoredProcedureCall()" );
}
    :
    ID_modifyDN DOT
    ( ID_modifyDNRename { triggerLdapOperation = LdapOperation.MODIFYDN_RENAME; }
    | ID_modifyDNExport { triggerLdapOperation = LdapOperation.MODIFYDN_EXPORT; }
    | ID_modifyDNImport { triggerLdapOperation = LdapOperation.MODIFYDN_IMPORT; } )
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( modifyDNStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

// -----------------------------------------------------------------------------
// The following rule does not make any sense semantically. Just placed for
// reducing repetition. All OperationAndStoredProcedureCall type are heavily
// context sensitive where their StoredProcedureParameterList depends on their
// Operation type. Other elements that sit between these two dependend elements
// are repeated for all OperationAndStoredProcedureCall type rules. So the
// the fallowing rule is for the part falling between those two dependend parts.
// -----------------------------------------------------------------------------

theCompositeRuleForCallAndSPNameAndSPOptionList
{
    log.debug( "entered theCompositeRuleForCallAndSPNameAndSPOptionList()" );
}
    :
    ( SP )+ ID_CALL ( SP )+ triggerStoredProcedureName=fullyQualifiedStoredProcedureName ( SP )*
        ( genericStoredProcedureOptionList ( SP )* )?
    {  }
    ;

// -----------------------------------------------------------------------------
//  XXXStoredProcedureParameterList
// -----------------------------------------------------------------------------

modifyStoredProcedureParameterList
{
    log.debug( "entered modifyStoredProcedureParameterList()" );
}
    :
    modifyStoredProcedureParameter ( SP )*
        ( SEP ( SP )* modifyStoredProcedureParameter ( SP )* )*
    ;

addStoredProcedureParameterList
{
    log.debug( "entered addStoredProcedureParameterList()" );
}
    :
    addStoredProcedureParameter ( SP )*
        ( SEP ( SP )* addStoredProcedureParameter ( SP )* )*
    ;

deleteStoredProcedureParameterList
{
    log.debug( "entered deleteStoredProcedureParameterList()" );
}
    :
    deleteStoredProcedureParameter ( SP )*
        ( SEP ( SP )* deleteStoredProcedureParameter ( SP )* )*
    ;

modifyDNStoredProcedureParameterList
{
    log.debug( "entered modifyDNStoredProcedureParameterList()" );
}
    :
    modifyDNStoredProcedureParameter ( SP )*
        ( SEP ( SP )* modifyDNStoredProcedureParameter ( SP )* )*
    ;

// -----------------------------------------------------------------------------
// XXXStoredProcedureParameter
// -----------------------------------------------------------------------------

modifyStoredProcedureParameter
{
    log.debug( "entered modifyStoredProcedureParameter()" );
}
    : ID_object { triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_OBJECT.instance() ); }
    | ID_modification { triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_MODIFICATION.instance() ); }
    | ID_oldEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_OLD_ENTRY.instance() ); }
    | ID_newEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_NEW_ENTRY.instance() ); }
    | genericStoredProcedureParameter
    ;

addStoredProcedureParameter
{
    log.debug( "entered addStoredProcedureParameter()" );
}
    : ID_entry { triggerStoredProcedureParameters.add( StoredProcedureParameter.Add_ENTRY.instance() ); }
    | ID_attributes { triggerStoredProcedureParameters.add( StoredProcedureParameter.Add_ATTRIBUTES.instance() ); }
    | genericStoredProcedureParameter
    ;

deleteStoredProcedureParameter
{
    log.debug( "entered deleteStoredProcedureParameter()" );
}
    : ID_name { triggerStoredProcedureParameters.add( StoredProcedureParameter.Delete_NAME.instance() ); }
    | ID_deletedEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.Delete_DELETED_ENTRY.instance() ); }
    | genericStoredProcedureParameter
    ;

modifyDNStoredProcedureParameter
{
    log.debug( "entered modifyDNStoredProcedureParameter()" );
}
    : ID_entry { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_ENTRY.instance() ); }
    | ID_newrdn { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_NEW_RDN.instance() ); }
    | ID_deleteoldrdn { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_DELETE_OLD_RDN.instance() ); }
    | ID_newSuperior { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_NEW_SUPERIOR.instance() ); }
    | genericStoredProcedureParameter
    ;

// -----------------------------------------------------------------------------

genericStoredProcedureParameter
{
    log.debug( "entered genericStoredProcedureParameter()" );
}
    : ldapContextStoredProcedureParameter
    | ID_operationPrincipal { triggerStoredProcedureParameters.add( StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.instance() ); }
    ;

ldapContextStoredProcedureParameter
{
    log.debug( "entered ldapContextStoredProcedureParameter()" );
    LdapDN ldapContext = null;
}
    : ID_ldapContext ( SP )+ ldapContext=distinguishedName
    { triggerStoredProcedureParameters.add( StoredProcedureParameter.Generic_LDAP_CONTEXT.instance( ldapContext ) ); }
	;

// -----------------------------------------------------------------------------

genericStoredProcedureOptionList
{
    log.debug( "entered genericStoredProcedureOptionList()" );
}
    :
    OPEN_CURLY ( SP )* ( genericStoredProcedureOption ( SP )*
        ( SEP ( SP )* genericStoredProcedureOption ( SP )* )* )* CLOSE_CURLY
    ;

genericStoredProcedureOption
{
    log.debug( "entered genericStoredProcedureOption()" );
    StoredProcedureOption spOption = null;
}
    : ( spOption=storedProcedureLanguageSchemeOption | spOption=storedProcedureSearchContextOption )
    { triggerStoredProcedureOptions.add( spOption ); }
    ;

storedProcedureLanguageSchemeOption returns [ StoredProcedureLanguageSchemeOption spLanguageSchemeOption ]
{
    log.debug( "entered storedProcedureLanguageSchemeOption()" );
    spLanguageSchemeOption = null;
}
    : ID_languageScheme ( SP )+ languageToken:UTF8String
    { spLanguageSchemeOption = new StoredProcedureLanguageSchemeOption( languageToken.getText() ); }
    ;

storedProcedureSearchContextOption returns [ StoredProcedureSearchContextOption spSearchContextOption ]
{
    log.debug( "entered storedProcedureSearchContextOption()" );
    spSearchContextOption = null;
    SearchScope searchScope = SearchScope.BASE; // default scope
    LdapDN spSearchContext = null;
}
    :
    ID_searchContext ( SP )+ // FIXME: SP should not be mandatory if an OPEN_CURLY follows
        ( OPEN_CURLY ( SP )*
            ( ID_search_scope ( SP )+ searchScope=storedProcedureSearchScope ( SP )* )?
        CLOSE_CURLY ( SP )+ )?
    spSearchContext=storedProcedureSearchContext
    { spSearchContextOption = new StoredProcedureSearchContextOption( spSearchContext, searchScope ); }
    ;

storedProcedureSearchScope returns [ SearchScope scope ]
{
    log.debug( "entered storedProcedureSearchScope()" );
    scope = null;
}
    : ID_scope_base { scope = SearchScope.BASE; }
    | ID_scope_one { scope = SearchScope.ONE; }
    | ID_scope_subtree { scope = SearchScope.SUBTREE; }
    ;

storedProcedureSearchContext returns [ LdapDN spSearchContext ]
{
    log.debug( "entered storedProcedureSearchContext()" );
    spSearchContext = null;
}
    : spSearchContext=distinguishedName
    ;

// -----------------------------------------------------------------------------

fullyQualifiedStoredProcedureName returns [ String spName ] 
{
    log.debug( "entered fullyQualifiedStoredProcedureName()" );
    spName = null;
}
    : spNameToken:UTF8String
    { spName = spNameToken.getText(); }
    ;

distinguishedName returns [ LdapDN name ] 
{
    log.debug( "entered distinguishedName()" );
    name = null;
}
    : nameToken:UTF8String
    {
        name = new LdapDN( nameToken.getText() );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "name parse failed for " + nameToken.getText() + " " + e.getMessage() );
    }

// -----------------------------------------------------------------------------
//  lexer class definition
// -----------------------------------------------------------------------------

/**
  * The parser's primary lexer.
  *
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
class AntlrTriggerSpecificationLexer extends Lexer;


// -----------------------------------------------------------------------------
//  lexer options
// -----------------------------------------------------------------------------

options
{
    k = 1;
    defaultErrorHandler = false;
    charVocabulary = '\3'..'\377';
    // the grammar is case-insensitive
    caseSensitive = false;
    caseSensitiveLiterals = false;
}


// -----------------------------------------------------------------------------
//  tokens
// -----------------------------------------------------------------------------

tokens
{
    // action time identifiers
    ID_AFTER = "after";
    
    // operation identifiers
    ID_modify  = "modify";
    ID_add = "add";
    ID_delete = "delete";
    ID_modifyDN = "modifydn";
    ID_modifyDNRename = "rename";
    ID_modifyDNExport = "export";
    ID_modifyDNImport = "import";
    
    // modify specific parameters
    ID_object = "$object";
    ID_modification = "$modification";
    ID_oldEntry = "$oldentry";
    ID_newEntry = "$newentry";
    
    // add specific parameters
    ID_entry = "$entry";
    ID_attributes = "$attributes";
    
    // delete specific parameters
    ID_name = "$name";
    ID_deletedEntry = "$deletedentry";
    
    // modifyDN specific parameters
    // ID_entry = "$entry"; // defined before
    ID_newrdn = "$newrdn";
    ID_deleteoldrdn = "$deleteoldrdn";
    ID_newSuperior = "$newsuperior";
    
    // generic parameters
    ID_ldapContext = "$ldapcontext";
    ID_operationPrincipal = "$operationprincipal";
    
    ID_CALL = "call";
    
    ID_languageScheme = "languagescheme";
    ID_searchContext = "searchcontext";
    ID_search_scope = "scope";
    ID_scope_base = "base";
    ID_scope_one = "one";
    ID_scope_subtree = "subtree";
}


// -----------------------------------------------------------------------------
//  lexer initialization
// -----------------------------------------------------------------------------

{
    private static final Logger log = LoggerFactory.getLogger( AntlrTriggerSpecificationLexer.class );
}


// -----------------------------------------------------------------------------
//  attribute description lexer rules from models
// -----------------------------------------------------------------------------

OPEN_PARAN : '(' ;

CLOSE_PARAN : ')' ;

OPEN_CURLY : '{' ;

CLOSE_CURLY : '}' ;

SEP : ',' ;

SP
    : ' '
    | '\t'
    | '\n' { newline(); }
    | '\r' ('\n')? { newline(); }
    ;

DOT : '.' ;

UTF8String : '"'! ( SAFEUTF8CHAR )* '"'! ;

//  This is all messed up - could not figure out how to get antlr to represent
//  the safe UTF-8 character set from RFC 3642 for production SafeUTF8Character

protected SAFEUTF8CHAR
    : '\u0001'..'\u0021'
    | '\u0023'..'\u007F'
    | '\u00c0'..'\u00d6'
    | '\u00d8'..'\u00f6'
    | '\u00f8'..'\u00ff'
    | '\u0100'..'\u1fff'
    | '\u3040'..'\u318f'
    | '\u3300'..'\u337f'
    | '\u3400'..'\u3d2d'
    | '\u4e00'..'\u9fff'
    | '\uf900'..'\ufaff'
    ;

COMMENT
    : '#'
    (~('\n'|'\r'))* (('\n'|'\r'('\n')?){newline();})?
    {$setType(Token.SKIP);}
    ;

IDENTIFIER : ALPHA ( ALPHA )* ; // A MUST HAVE although we do not use explicitly

protected ALPHA : 'a'..'z' | '$' ;