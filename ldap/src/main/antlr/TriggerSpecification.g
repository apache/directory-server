header
{
/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License" );
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


package org.apache.directory.shared.ldap.trigger;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
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
 * @version $Rev$ $Date$
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
    
    // subordinate parser instances
    private DnParser dnParser;
    
    private boolean isNormalizing = false;
    private NameComponentNormalizer normalizer;
    
    /**
     * Creates a (normalizing) subordinate DnParser for parsing Names.
     * This method MUST be called for each instance while we cannot do
     * constructor overloading for this class.
     *
     * @return the DnParser to be used for parsing Names
     */
    public void init()
    {
        try
        {
            if( isNormalizing )
            {
                dnParser = new DnParser( normalizer );
            }
            else
            {
                dnParser = new DnParser();
            }
        }
        catch ( NamingException e )
        {
            String msg = "Failed to initialize the subordinate DnParser for this AntlrTriggerSpecificationParser";

            // We throw a NPE since this variable cannot be null for proper operation
            // so we can catch the null pointer before the dnParser is even used.

            throw new NullPointerException( "dnParser is null: " + msg );
        }
    }

    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizer(NameComponentNormalizer normalizer)
    {
        this.normalizer = normalizer;
        this.isNormalizing = true;
    }
}


// ----------------------------------------------------------------------------
//  parser productions
// ----------------------------------------------------------------------------

wrapperEntryPoint
{
    log.debug( "entered wrapperEntryPoint()" );
}
    :
    ( SP )* triggerSpecification ( SP )* EOF
    ;

// -----------------------------------------------------------------------------
//  main rules
// -----------------------------------------------------------------------------

triggerSpecification
{
    log.debug( "entered triggerSpecification()" );
}
    :
    actionTime ( SP )+ ldapOperationAndStoredProcedureCall
    ;
    
actionTime
{
    log.debug( "entered actionTime()" );
}
    : ID_BEFORE
    | ID_AFTER
    | ID_INSTEADOF
    ;
    
ldapOperationAndStoredProcedureCall
{
    log.debug( "entered ldapOperationAndStoredProcedureCall()" );
}
    : bindOperationAndStoredProcedureCall
    | unbindOperationAndStoredProcedureCall
    | searchOperationAndStoredProcedureCall
    | modifyOperationAndStoredProcedureCall
    | addOperationAndStoredProcedureCall
    | delOperationAndStoredProcedureCall
    | modDNOperationAndStoredProcedureCall
    | compareOperationAndStoredProcedureCall
    | abandonOperationAndStoredProcedureCall
    | extendedOperationAndStoredProcedureCall
    ;

// -----------------------------------------------------------------------------
//  XXXOperationAndStoredProcedureCall
// -----------------------------------------------------------------------------

bindOperationAndStoredProcedureCall
{
    log.debug( "entered bindOperationAndStoredProcedureCall()" );
}
    :
    ID_bind theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( bindStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

unbindOperationAndStoredProcedureCall
{
    log.debug( "entered unbindOperationAndStoredProcedureCall()" );
}
    :
    ID_unbind theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( unbindStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

searchOperationAndStoredProcedureCall
{
    log.debug( "entered searchOperationAndStoredProcedureCall()" );
}
    :
    ID_search theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( searchStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

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

delOperationAndStoredProcedureCall
{
    log.debug( "entered delOperationAndStoredProcedureCall()" );
}
    :
    ( ID_del | ID_delete ) theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( delStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

modDNOperationAndStoredProcedureCall
{
    log.debug( "entered modDNOperationAndStoredProcedureCall()" );
}
    :
    ID_modDN theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( modDNStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

compareOperationAndStoredProcedureCall
{
    log.debug( "entered compareOperationAndStoredProcedureCall()" );
}
    :
    ID_compare theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( compareStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

abandonOperationAndStoredProcedureCall
{
    log.debug( "entered abandonOperationAndStoredProcedureCall()" );
}
    :
    ID_abandon theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( abandonStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

extendedOperationAndStoredProcedureCall
{
    log.debug( "entered extendedOperationAndStoredProcedureCall()" );
}
    :
    ID_extended theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( extendedStoredProcedureParameterList )?
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
    ( SP )+ ID_CALL ( SP )+ fullyQualifiedStoredProcedureName ( SP )*
        ( genericStoredProcedureOptionList ( SP )* )?
    ;

// -----------------------------------------------------------------------------
//  XXXStoredProcedureParameterList
// -----------------------------------------------------------------------------

bindStoredProcedureParameterList
{
    log.debug( "entered bindStoredProcedureParameterList()" );
}
    :
    bindStoredProcedureParameter ( SP )*
        ( SEP ( SP )* bindStoredProcedureParameter ( SP )* )*
    ;

unbindStoredProcedureParameterList
{
    log.debug( "entered unbindStoredProcedureParameterList()" );
}
    :
    unbindStoredProcedureParameter ( SP )*
        ( SEP ( SP )* unbindStoredProcedureParameter ( SP )* )*
    ;

searchStoredProcedureParameterList
{
    log.debug( "entered searchStoredProcedureParameterList()" );
}
    :
    searchStoredProcedureParameter ( SP )*
        ( SEP ( SP )* searchStoredProcedureParameter ( SP )* )*
    ;

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

delStoredProcedureParameterList
{
    log.debug( "entered delStoredProcedureParameterList()" );
}
    :
    delStoredProcedureParameter ( SP )*
        ( SEP ( SP )* delStoredProcedureParameter ( SP )* )*
    ;

modDNStoredProcedureParameterList
{
    log.debug( "entered modDNStoredProcedureParameterList()" );
}
    :
    modDNStoredProcedureParameter ( SP )*
        ( SEP ( SP )* modDNStoredProcedureParameter ( SP )* )*
    ;

compareStoredProcedureParameterList
{
    log.debug( "entered compareStoredProcedureParameterList()" );
}
    :
    compareStoredProcedureParameter ( SP )*
        ( SEP ( SP )* compareStoredProcedureParameter ( SP )* )*
    ;

abandonStoredProcedureParameterList
{
    log.debug( "entered abandonStoredProcedureParameterList()" );
}
    :
    abandonStoredProcedureParameter ( SP )*
        ( SEP ( SP )* abandonStoredProcedureParameter ( SP )* )*
    ;

extendedStoredProcedureParameterList
{
    log.debug( "entered extendedStoredProcedureParameterList()" );
}
    :
    extendedStoredProcedureParameter ( SP )*
        ( SEP ( SP )* extendedStoredProcedureParameter ( SP )* )*
    ;

// -----------------------------------------------------------------------------
// XXXStoredProcedureParameter
// -----------------------------------------------------------------------------

bindStoredProcedureParameter
{
    log.debug( "entered bindStoredProcedureParameter()" );
}
    : ID_version
    | ID_name
    | ID_authentication
    | genericStoredProcedureParameter
    ;

unbindStoredProcedureParameter
{
    log.debug( "entered unbindStoredProcedureParameter()" );
}
    : genericStoredProcedureParameter
    ;

searchStoredProcedureParameter
{
    log.debug( "entered searchStoredProcedureParameter()" );
}
    : ID_baseObject
    | ID_scope
    | ID_derefAliases
    | ID_sizeLimit
    | ID_timeLimit
    | ID_typesOnly
    | ID_filter
    | ID_attributes
    | genericStoredProcedureParameter
    ;

modifyStoredProcedureParameter
{
    log.debug( "entered modifyStoredProcedureParameter()" );
}
    : ID_object
    | ID_modification
    | ID_oldEntry
    | ID_newEntry
    | genericStoredProcedureParameter
    ;

addStoredProcedureParameter
{
    log.debug( "entered addStoredProcedureParameter()" );
}
    : ID_entry
    | ID_attributes
    | genericStoredProcedureParameter
    ;

delStoredProcedureParameter
{
    log.debug( "entered delStoredProcedureParameter()" );
}
    : ID_name
    | ID_deletedEntry
    | genericStoredProcedureParameter
    ;

modDNStoredProcedureParameter
{
    log.debug( "entered modDNStoredProcedureParameter()" );
}
    : ID_entry
    | ID_newrdn
    | ID_deleteoldrdn
    | ID_newSuperior
    | genericStoredProcedureParameter
    ;

compareStoredProcedureParameter
{
    log.debug( "entered compareStoredProcedureParameter()" );
}
    : ID_entry
    | ID_ava
    | genericStoredProcedureParameter
    ;

abandonStoredProcedureParameter
{
    log.debug( "entered abandonStoredProcedureParameter()" );
}
    : ID_messageId
    | genericStoredProcedureParameter
    ;
    
extendedStoredProcedureParameter
{
    log.debug( "entered extendedStoredProcedureParameter()" );
}
    : ID_requestName
    | ID_requestValue
    | genericStoredProcedureParameter
    ;

// -----------------------------------------------------------------------------

genericStoredProcedureParameter
{
    log.debug( "entered genericStoredProcedureParameter()" );
}
    :
    ID_operationTime | ID_operationPrincipal
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
}
    :
    storedProcedureLanguageOption | storedProcedureSearchContextOption
    ;

storedProcedureLanguageOption
{
    log.debug( "entered storedProcedureLanguageOption()" );
}
    :
    ID_language ( SP )+ languageToken:UTF8String
    ;

storedProcedureSearchContextOption
{
    log.debug( "entered storedProcedureSearchContextOption()" );
}
    :
    ID_searchContext ( SP )+
        ( OPEN_CURLY ( SP )*
            ( ID_search_scope ( SP )+ storedProcedureSearchScope ( SP )* )?
        CLOSE_CURLY ( SP )+ )?
    storedProcedureSearchContext
    ;

storedProcedureSearchScope
{
    log.debug( "entered storedProcedureSearchScope()" );
}
    : ID_scope_base
    | ID_scope_one
    | ID_scope_subtree
    ;

storedProcedureSearchContext
{
    log.debug( "entered storedProcedureSearchContext()" );
}
    : distinguishedName
    ;

// -----------------------------------------------------------------------------

fullyQualifiedStoredProcedureName
{
    log.debug( "entered fullyQualifiedStoredProcedureName()" );
}
    :
    spNameToken:UTF8String
    ;

distinguishedName returns [ Name name ] 
{
    log.debug( "entered distinguishedName()" );
    name = null;
}
    :
    nameToken:UTF8String
    {
        name = dnParser.parse( nameToken.getText() );
    }
    ;
    exception
    catch [Exception e]
    {
        throw new RecognitionException( "name parser failed for " + nameToken.getText() + " " + e.getMessage() );
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
    ID_BEFORE = "before";
    ID_AFTER = "after";
    ID_INSTEADOF = "insteadof";
    
    // operation identifiers
    ID_bind = "bind";
    ID_unbind = "unbind";
    ID_search  = "search";
    ID_modify  = "modify";
    ID_add = "add";
    ID_del = "del";
    ID_delete = "delete";
    ID_modDN = "moddn";
    ID_compare = "compare";
    ID_abandon = "abandon";
    ID_extended = "extended";
    
    // bind specific parameters
    ID_version = "$version";
    ID_name = "$name";
    ID_authentication = "$authentication";
    
    // unbind specific parameters
    // there is non currently
    
    // search specific parameters
    ID_baseObject = "$baseobject";
    ID_scope = "$scope";
    ID_derefAliases = "$derefaliases";
    ID_sizeLimit = "$sizelimit";
    ID_timeLimit = "$timelimit";
    ID_typesOnly = "$typesonly";
    ID_filter = "$filter";
    ID_attributes = "$attributes";
    
    // modify specific parameters
    ID_object = "$object";
    ID_modification = "$modification";
    ID_oldEntry = "$oldentry";
    ID_newEntry = "$newentry";
    
    // add specific parameters
    ID_entry = "$entry";
    // ID_attributes = "$attributes"; // defined before
    
    // del specific parameters
    // ID_name = "$name"; // defined before
    ID_deletedEntry = "$deletedentry";
    
    // modDN specific parameters
    // ID_entry = "$entry"; // defined before
    ID_newrdn = "$newrdn";
    ID_deleteoldrdn = "$deleteoldrdn";
    ID_newSuperior = "$newsuperior";
    
    // compare specific parameters
    // ID_entry = "$entry"; // defined before
    ID_ava = "$ava";
    
    // abandon specific parameters
    ID_messageId = "$messageid";
    
    // extended specific parameters
    ID_requestName = "$requestname";
    ID_requestValue = "$requestvalue";
    
    // generic parameters
    ID_operationTime = "$operationtime";
    ID_operationPrincipal = "$operationprincipal";
    
    ID_CALL = "call";
    
    ID_language = "language";
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