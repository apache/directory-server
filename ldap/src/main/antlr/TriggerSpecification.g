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
import java.util.ArrayList;
import java.util.List;

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
    
    // subordinate parser instances
    private DnParser dnParser;
    
    private boolean isNormalizing = false;
    private NameComponentNormalizer normalizer;
    
    private LdapOperationTokenListener caller = null;
    
    private ActionTime triggerActionTime;
    
    private LdapOperation triggerLdapOperation;
    
    private String triggerStoredProcedureName;
    
    private List triggerStoredProcedureOptions = new ArrayList();
    
    private List triggerStoredProcedureParameters = new ArrayList();
    
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
    
	public void registerLdapOperationTokenListener( LdapOperationTokenListener listener )
	{
		this.caller = listener;
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

wrapperEntryPoint returns [ TriggerSpecification triggerSpec ]
{
    log.debug( "entered wrapperEntryPoint()" );
    triggerSpec = null;
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
    : ID_BEFORE { triggerActionTime = ActionTime.BEFORE; }
    | ID_AFTER { triggerActionTime = ActionTime.AFTER; }
    | ID_INSTEADOF { triggerActionTime = ActionTime.INSTEADOF; }
    ;
    
ldapOperationAndStoredProcedureCall
{
    log.debug( "entered ldapOperationAndStoredProcedureCall()" );
}
    : bindOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.BIND; }
    | unbindOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.UNBIND; }
    | searchOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.SEARCH; }
    | modifyOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.MODIFY; }
    | addOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.ADD; }
    | delOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.DEL; }
    | modDNOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.MODDN; }
    | compareOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.COMPARE; }
    | abandonOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.ABANDON; }
    | extendedOperationAndStoredProcedureCall { triggerLdapOperation = LdapOperation.EXTENDED; }
    ;

// -----------------------------------------------------------------------------
//  XXXOperationAndStoredProcedureCall
// -----------------------------------------------------------------------------

bindOperationAndStoredProcedureCall
{
    log.debug( "entered bindOperationAndStoredProcedureCall()" );
}
    :
    ID_bind
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.BIND ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.BIND  );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( bindStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

unbindOperationAndStoredProcedureCall
{
    log.debug( "entered unbindOperationAndStoredProcedureCall()" );
}
    :
    ID_unbind
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.UNBIND ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.UNBIND );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( unbindStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

searchOperationAndStoredProcedureCall
{
    log.debug( "entered searchOperationAndStoredProcedureCall()" );
}
    :
    ID_search
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.SEARCH ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.SEARCH );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( searchStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

modifyOperationAndStoredProcedureCall
{
    log.debug( "entered modifyOperationAndStoredProcedureCall()" );
}
    :
    ID_modify
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.MODIFY ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.MODIFY );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( modifyStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

addOperationAndStoredProcedureCall
{
    log.debug( "entered addOperationAndStoredProcedureCall()" );
}
    :
    ID_add
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.ADD ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.ADD );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( addStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

delOperationAndStoredProcedureCall
{
    log.debug( "entered delOperationAndStoredProcedureCall()" );
}
    :
    ( ID_del | ID_delete )
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.DEL ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.DEL );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( delStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

modDNOperationAndStoredProcedureCall
{
    log.debug( "entered modDNOperationAndStoredProcedureCall()" );
}
    :
    ID_modDN
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.MODDN ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.MODDN );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( modDNStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

compareOperationAndStoredProcedureCall
{
    log.debug( "entered compareOperationAndStoredProcedureCall()" );
}
    :
    ID_compare
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.COMPARE ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.COMPARE );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( compareStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

abandonOperationAndStoredProcedureCall
{
    log.debug( "entered abandonOperationAndStoredProcedureCall()" );
}
    :
    ID_abandon
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.ABANDON ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.ABANDON );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
    OPEN_PARAN ( SP )*
        ( abandonStoredProcedureParameterList )?
    CLOSE_PARAN
    ;

extendedOperationAndStoredProcedureCall
{
    log.debug( "entered extendedOperationAndStoredProcedureCall()" );
}
    :
    ID_extended
    {
    	if ( caller.ldapOperationTokenRead( LdapOperation.EXTENDED ) == false )
    	{
    		throw new ConditionalParserFailureBasedOnCallerFeedback( LdapOperation.EXTENDED );
    	}
    }
    theCompositeRuleForCallAndSPNameAndSPOptionList
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
    ( SP )+ ID_CALL ( SP )+ triggerStoredProcedureName=fullyQualifiedStoredProcedureName ( SP )*
        ( genericStoredProcedureOptionList ( SP )* )?
    {  }
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
    : ID_version { triggerStoredProcedureParameters.add( StoredProcedureParameter.BindStoredProcedureParameter.VERSION ); }
    | ID_name { triggerStoredProcedureParameters.add( StoredProcedureParameter.BindStoredProcedureParameter.NAME ); }
    | ID_authentication { triggerStoredProcedureParameters.add( StoredProcedureParameter.BindStoredProcedureParameter.AUTHENTICATION ); }
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
    : ID_baseObject { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.BASE_OBJECT ); }
    | ID_scope { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.SCOPE ); }
    | ID_derefAliases { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.DEREF_ALIASES ); }
    | ID_sizeLimit { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.SIZE_LIMIT ); }
    | ID_timeLimit { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.TIME_LIMIT ); }
    | ID_typesOnly { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.TYPES_ONLY ); }
    | ID_filter { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.FILTER ); }
    | ID_attributes { triggerStoredProcedureParameters.add( StoredProcedureParameter.SearchStoredProcedureParameter.ATTRIBUTES ); }
    | genericStoredProcedureParameter
    ;

modifyStoredProcedureParameter
{
    log.debug( "entered modifyStoredProcedureParameter()" );
}
    : ID_object { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyStoredProcedureParameter.OBJECT ); }
    | ID_modification { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyStoredProcedureParameter.MODIFICATION ); }
    | ID_oldEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyStoredProcedureParameter.OLD_ENTRY ); }
    | ID_newEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyStoredProcedureParameter.NEW_ENTRY ); }
    | genericStoredProcedureParameter
    ;

addStoredProcedureParameter
{
    log.debug( "entered addStoredProcedureParameter()" );
}
    : ID_entry { triggerStoredProcedureParameters.add( StoredProcedureParameter.AddStoredProcedureParameter.ENTRY ); }
    | ID_attributes { triggerStoredProcedureParameters.add( StoredProcedureParameter.AddStoredProcedureParameter.ATTRIBUTES ); }
    | genericStoredProcedureParameter
    ;

delStoredProcedureParameter
{
    log.debug( "entered delStoredProcedureParameter()" );
}
    : ID_name { triggerStoredProcedureParameters.add( StoredProcedureParameter.DelStoredProcedureParameter.NAME ); }
    | ID_deletedEntry { triggerStoredProcedureParameters.add( StoredProcedureParameter.DelStoredProcedureParameter.DELETED_ENTRY ); }
    | genericStoredProcedureParameter
    ;

modDNStoredProcedureParameter
{
    log.debug( "entered modDNStoredProcedureParameter()" );
}
    : ID_entry { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModDNStoredProcedureParameter.ENTRY ); }
    | ID_newrdn { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModDNStoredProcedureParameter.NEW_RDN ); }
    | ID_deleteoldrdn { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModDNStoredProcedureParameter.DELETE_OLD_RDN ); }
    | ID_newSuperior { triggerStoredProcedureParameters.add( StoredProcedureParameter.ModDNStoredProcedureParameter.NEW_SUPERIOR ); }
    | genericStoredProcedureParameter
    ;

compareStoredProcedureParameter
{
    log.debug( "entered compareStoredProcedureParameter()" );
}
    : ID_entry { triggerStoredProcedureParameters.add( StoredProcedureParameter.CompareStoredProcedureParameter.ENTRY ); }
    | ID_ava { triggerStoredProcedureParameters.add( StoredProcedureParameter.CompareStoredProcedureParameter.AVA ); }
    | genericStoredProcedureParameter
    ;

abandonStoredProcedureParameter
{
    log.debug( "entered abandonStoredProcedureParameter()" );
}
    : ID_messageId { triggerStoredProcedureParameters.add( StoredProcedureParameter.AbandonStoredProcedureParameter.MESSAGE_ID ); }
    | genericStoredProcedureParameter
    ;
    
extendedStoredProcedureParameter
{
    log.debug( "entered extendedStoredProcedureParameter()" );
}
    : ID_requestName { triggerStoredProcedureParameters.add( StoredProcedureParameter.ExtendedStoredProcedureParameter.REQUEST_NAME ); }
    | ID_requestValue { triggerStoredProcedureParameters.add( StoredProcedureParameter.ExtendedStoredProcedureParameter.REQUEST_VALUE ); }
    | genericStoredProcedureParameter
    ;

// -----------------------------------------------------------------------------

genericStoredProcedureParameter
{
    log.debug( "entered genericStoredProcedureParameter()" );
}
    : ID_operationTime { triggerStoredProcedureParameters.add( StoredProcedureParameter.OPERATION_TIME ); }
    | ID_operationPrincipal { triggerStoredProcedureParameters.add( StoredProcedureParameter.OPERATION_PRINCIPAL ); }
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
    : ( spOption=storedProcedureLanguageOption | spOption=storedProcedureSearchContextOption )
    { triggerStoredProcedureOptions.add( spOption ); }
    ;

storedProcedureLanguageOption returns [ StoredProcedureLanguageOption spLanguageOption ]
{
    log.debug( "entered storedProcedureLanguageOption()" );
    spLanguageOption = null;
}
    : ID_language ( SP )+ languageToken:UTF8String
    { spLanguageOption = new StoredProcedureLanguageOption( languageToken.getText() ); }
    ;

storedProcedureSearchContextOption returns [ StoredProcedureSearchContextOption spSearchContextOption ]
{
    log.debug( "entered storedProcedureSearchContextOption()" );
    spSearchContextOption = null;
    SearchScope searchScope = SearchScope.BASE; // default scope
    Name spSearchContext = null;
}
    :
    ID_searchContext ( SP )+
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

storedProcedureSearchContext returns [ Name spSearchContext ]
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

distinguishedName returns [ Name name ] 
{
    log.debug( "entered distinguishedName()" );
    name = null;
}
    : nameToken:UTF8String
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