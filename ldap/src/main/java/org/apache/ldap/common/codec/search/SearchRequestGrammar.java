/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.ldap.common.codec.search;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.asn1.ber.tlv.Value;
import org.apache.asn1.util.IntegerDecoderException;
import org.apache.asn1.util.IntegerDecoder;
import org.apache.asn1.util.BooleanDecoder;
import org.apache.asn1.util.BooleanDecoderException;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.LdapStatesEnum;
import org.apache.ldap.common.codec.util.LdapString;
import org.apache.ldap.common.codec.util.LdapStringEncodingException;
import org.apache.ldap.common.name.LdapDN;
import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SearchRequest LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( SearchRequestGrammar.class );

    /** The instance of grammar. SearchRequestGrammar is a singleton */
    private static IGrammar instance = new SearchRequestGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SearchRequestGrammar object.
     */
    private SearchRequestGrammar()
    {
        name       = SearchRequestGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_SEARCH_REQUEST_STATE][256];

        //============================================================================================
        // SearchRequest Message
        //============================================================================================
        // LdapMessage   ::= ... SearchRequest ...
        // SearchRequest ::= [APPLICATION 3] SEQUENCE { (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_TAG][LdapConstants.SEARCH_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_TAG, LdapStatesEnum.SEARCH_REQUEST_VALUE, null );

        // LdapMessage   ::= ... SearchRequest ...
        // SearchRequest ::= [APPLICATION 3] SEQUENCE { ... (Value)
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_VALUE][LdapConstants.SEARCH_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_VALUE, LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_TAG,
                new GrammarAction( "Init SearchRequest" )
                {
                    public void action( IAsn1Container container ) 
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // Now, we can allocate the SearchRequest
                        // And we associate it to the ldapMessage Object
                        ldapMessage.setProtocolOP( new SearchRequest() );
                    }
                });

        // SearchRequest ::= [APPLICATION 3] SEQUENCE { 
        //    baseObject      LDAPDN, (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_TAG, LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE { 
        //    baseObject      LDAPDN, (Value)
        //    ...
        // We have a value for the base object, we will store it in the message
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_VALUE][UniversalTag.OCTET_STRING_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_BASE_OBJECT_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_SCOPE_TAG,
                new GrammarAction( "store base object value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to check that this is a correct DN
                        Name baseObject = LdapDN.EMPTY_LDAPDN;
                        
                        // We have to handle the special case of a 0 length base object,
                        // which means that the search is done from the default root.
                        if ( tlv.getLength().getLength() != 0 )
                        {
                            try
                            {
                                baseObject = new LdapDN( tlv.getValue().getData() );
                                baseObject = LdapDN.normalize( baseObject );
                            }
                            catch ( InvalidNameException ine )
                            {
                            	String msg = "The root DN " + baseObject.toString() + " is invalid"; 
                                log.error( "{} : {}", msg, ine.getMessage());
                                throw new DecoderException( msg, ine );
                            }
                            catch ( NamingException ne )
                            {
                            	String msg = "The root DN " + baseObject.toString() + " cannot be modified";
                                log.error( "{} : {}", msg, ne.getMessage() );
                                throw new DecoderException( msg, ne );
                            }
                        }
                        
                        searchRequest.setBaseObject(baseObject);

                        log.debug( "Searching with root DN : {}", baseObject );
                       
                        return;
                    }
                } );

        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    scope           ENUMERATED {
        //        baseObject              (0),
        //        singleLevel             (1),
        //        wholeSubtree            (2) }, (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_SCOPE_TAG][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_SCOPE_TAG, LdapStatesEnum.SEARCH_REQUEST_SCOPE_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE { 
        //    ...
        //    scope           ENUMERATED {
        //        baseObject              (0),
        //        singleLevel             (1),
        //        wholeSubtree            (2) }, (Value)
        //    ...
        // We have a value for the scope, we will store it in the message
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_SCOPE_VALUE][UniversalTag.ENUMERATED_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_SCOPE_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_TAG,
                new GrammarAction( "store scope value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to check that this is a correct scope
                        Value value   = tlv.getValue();
                        int   scope = 0;

                        try
                        {
                            scope = IntegerDecoder.parse( value, LdapConstants.SCOPE_BASE_OBJECT, LdapConstants.SCOPE_WHOLE_SUBTREE );
                        }
                        catch ( IntegerDecoderException ide )
                        {
                            log.error( "The scope is not in [0..2] : {}", value.toString() );
                            throw new DecoderException( "The scope is not in [0..2] : " + value.toString() );
                        }
                        
                        searchRequest.setScope(scope);

                        if ( log.isDebugEnabled() )
                        {
                            switch ( scope )
                            {
                                case LdapConstants.SCOPE_BASE_OBJECT :
                                    log.debug( "Searching within BASE_OBJECT scope " );
                                    break;
                            
                                case LdapConstants.SCOPE_SINGLE_LEVEL :
                                    log.debug( "Searching within SINGLE_LEVEL scope " );
                                    break;

                                case LdapConstants.SCOPE_WHOLE_SUBTREE :
                                    log.debug( "Searching within WHOLE_SUBTREE scope " );
                                    break;
                            }
                        }

                        return;
                    }
                } );

        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    derefAliases    ENUMERATED {
        //        neverDerefAliases       (0),
        //        derefInSearching        (1),
        //        derefFindingBaseObj     (2),
        //        derefAlways             (3) }, (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_TAG][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_TAG, LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    derefAliases    ENUMERATED {
        //        neverDerefAliases       (0),
        //        derefInSearching        (1),
        //        derefFindingBaseObj     (2),
        //        derefAlways             (3) }, (Value)
        //    ...
        // We have a value for the derefAliases, we will store it in the message
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_VALUE][UniversalTag.ENUMERATED_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_DEREF_ALIASES_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_TAG,
                new GrammarAction( "store derefAliases value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to check that this is a correct derefAliases
                        Value value   = tlv.getValue();
                        int   derefAliases = 0;

                        try
                        {
                            derefAliases = IntegerDecoder.parse( value, LdapConstants.NEVER_DEREF_ALIASES, LdapConstants.DEREF_ALWAYS );
                        }
                        catch ( IntegerDecoderException ide )
                        {
                            log.error( "The derefAlias is not in [0..3] : {}", value.toString() );
                            throw new DecoderException( "The derefAlias is not in [0..3] : " + value.toString() );
                        }
                        
                        searchRequest.setDerefAliases( derefAliases );

                        if ( log.isDebugEnabled() )
                        {
                            switch ( derefAliases )
                            {
                                case LdapConstants.NEVER_DEREF_ALIASES :
                                    log.debug( "Handling object strategy : NEVER_DEREF_ALIASES" );
                                    break;
                            
                                case LdapConstants.DEREF_IN_SEARCHING :
                                    log.debug( "Handling object strategy : DEREF_IN_SEARCHING" );
                                    break;

                                case LdapConstants.DEREF_FINDING_BASE_OBJ :
                                    log.debug( "Handling object strategy : DEREF_FINDING_BASE_OBJ" );
                                    break;

                                case LdapConstants.DEREF_ALWAYS :
                                    log.debug( "Handling object strategy : DEREF_ALWAYS" );
                                    break;
                            }
                        }
                        return;
                    }
                } );

        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    sizeLimit  INTEGER (0 .. maxInt), (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_TAG, LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    sizeLimit  INTEGER (0 .. maxInt), (Value)
        //    ...
        // We have a value for the sizeLimit, we will store it in the message
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_VALUE][UniversalTag.INTEGER_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_SIZE_LIMIT_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_TAG,
                new GrammarAction( "store sizeLimit value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // The current TLV should be a integer
                        // We get it and store it in sizeLimit
                        Value value     = tlv.getValue();
                        int   sizeLimit = 0;

                        try
                        {
                            sizeLimit = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );
                        }
                        catch ( IntegerDecoderException ide )
                        {
                            log.error( "The sizeLimit is not a valid Integer: {}", value.toString() );
                            throw new DecoderException( "The sizeLimit is not a valid Integer: " + value.toString() );
                        }

                        searchRequest.setSizeLimit( sizeLimit );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "The sizeLimit value is set to {} objects", new Integer( sizeLimit ) );
                        }

                        return;
                    }
                } );

        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    timeLimit  INTEGER (0 .. maxInt), (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_TAG, LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    timeLimit  INTEGER (0 .. maxInt), (Value)
        //    ...
        // We have a value for the timeLimit, we will store it in the message
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_VALUE][UniversalTag.INTEGER_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_TIME_LIMIT_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_TAG,
                new GrammarAction( "store timeLimit value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // The current TLV should be a integer
                        // We get it and store it in timeLimit
                        Value value     = tlv.getValue();

                        int   timeLimit = 0;

                        try
                        {
                            timeLimit = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );
                        }
                        catch ( IntegerDecoderException ide )
                        {
                            log.error( "The timeLimit is not a valid Integer: {}", value.toString() );
                            throw new DecoderException( "The timeLimit is not a valid Integer: " + value.toString() );
                        }
                        
                        searchRequest.setTimeLimit( timeLimit );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "The timeLimit value is set to {} seconds", new Integer( timeLimit ) );
                        }

                        return;
                    }
                } );

        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    typesOnly       BOOLEAN, (Tag)
        //    ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_TAG][UniversalTag.BOOLEAN_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_TAG, LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_VALUE,
                null);
        
        // SearchRequest ::= [APPLICATION 3] SEQUENCE {
        //    ...
        //    typesOnly       BOOLEAN, (Value)
        //    ...
        // We have a value for the typesOnly, we will store it in the message.
        // The next transition will deal with the Filter. As a filter could contains
        // sub-filter, we will initialize the filter Object right here. 
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_VALUE][UniversalTag.BOOLEAN_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_TYPES_ONLY_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_FILTER,
                new GrammarAction( "store typesOnly value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // We get the value. If it's a 0, it's a FALSE. If it's
                        // a FF, it's a TRUE. Any other value should be an error,
                        // but we could relax this constraint. So if we have something
                        // which is not 0, it will be interpreted as TRUE, but we
                        // will generate a warning.
                        Value value     = tlv.getValue();

                        try
                        {
                            searchRequest.setTypesOnly( BooleanDecoder.parse( value ) );
                        }
                        catch ( BooleanDecoderException bde )
                        {
                            log.error("The types only flag {} is invalid : {}. It should be 0 or 255",
                            		StringTools.dumpBytes( value.getData() ), 
                                    bde.getMessage() );
                        
                            throw new DecoderException( bde.getMessage() );
                        }

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "The search will return {}", ( searchRequest.isTypesOnly() ? 
                                        "only attributs type" : 
                                        "attributes types and values" ) );
                        }
                        return;
                    }
                } );

        //********************************************************************************************
        // Here we are dealing with the Search Request filter. 
        // If the Tag is 0xA0, then it's an and Filter
        // If the Tag is 0xA1, then it's an or Filter
        // If the Tag is 0xA2, then it's a not Filter
        // If the Tag is 0xA3, then it's an equalityMatch Filter
        // If the Tag is 0xA4, then it's a substrings Filter
        // If the Tag is 0xA5, then it's a greaterOrEqual Filter
        // If the Tag is 0xA6, then it's a lessOrEqual Filter
        // If the Tag is 0x87, then it's a present Filter
        // If the Tag is 0xA8, then it's an approxMatch Filter
        // If the Tag is 0xA9, then it's an extensibleMatch Filter
        //********************************************************************************************

        //--------------------------------------------------------------------------------------------
        // And Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   and             [0] SET OF Filter,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.AND_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // Or Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   or             [0] SET OF Filter,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.OR_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // Not Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   not             [2] Filter,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.NOT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // EqualityMatch Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   equalityMatch   [3] AttributeValueAssertion,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.EQUALITY_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // Substrings Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   substrings      [4] SubstringFilter,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.SUBSTRINGS_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // GreaterOrEqual Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   greaterOrEqual  [5] AttributeValueAssertion,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.GREATER_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // LessOrEqual Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   lessOrEqual     [6] AttributeValueAssertion,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.LESS_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // Present Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   present         [7] AttributeDescription,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.PRESENT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ApproxMatch Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   approxMatch     [8] AttributeValueAssertion,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.APPROX_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ExtensibleMatch Filter Message.
        //--------------------------------------------------------------------------------------------
        // Filter ::= CHOICE {
        //   ...
        //   extensibleMatch  [9] MatchingRuleAssertion,
        //   ...
        // We have to switch to the Filter grammar
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.FILTER_GRAMMAR_SWITCH,
                null );


        // ...
        //    attributes      AttributeDescriptionList }
        // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription (Tag)
        //    ...
        // We have to check that the filter has been created.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_FILTER][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_FILTER, LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_VALUE,
                null);
        
        // ...
        //    attributes      AttributeDescriptionList }
        // AttributeDescriptionList ::= SEQUENCE OF AttributeDescription (Length)
        //    ...
        // We have to create an array of elements to store the list of attributes
        // to retrieve. We don't know yet how many attributes we will read so we
        // will allocate an ArrayList. We also can have an empty list.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_VALUE][UniversalTag.SEQUENCE_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_LIST_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG,
                new GrammarAction( "store Attribute Description List value" )
                {
                    public void action( IAsn1Container container )
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();

                        // The attribute list may be null.
                        if ( tlv.getLength().getLength() != 0 )
                        {
                        	searchRequest.initAttributes();
                        }

                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );

                        return;
                    }
                } );
        
        // AttributeDescription ::= LDAPString
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG, LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_VALUE,
                null);
        
        // AttributeDescription ::= LDAPString (Value)
        // Decodes the attribute description and stores it.
        super.transitions[LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_VALUE][UniversalTag.OCTET_STRING_TAG] =
            new GrammarTransition( LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_VALUE,
                LdapStatesEnum.SEARCH_REQUEST_ATTRIBUTE_DESCRIPTION_TAG,
                new GrammarAction( "store Attribute Description value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        SearchRequest     searchRequest =
                            ldapMessageContainer.getLdapMessage().getSearchRequest();

                        TLV                  tlv = ldapMessageContainer.getCurrentTLV();
                        LdapString attributeDescription = null;
                        
                        try
                        {
                            attributeDescription = new LdapString( tlv.getValue().getData() );
                            searchRequest.addAttribute( attributeDescription );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            log.error( "Cannot decode the attribute description : {}", StringTools.dumpBytes( tlv.getValue().getData() ) );
                            throw new DecoderException( "Cannot decode the attribute description" );
                        }
                        
                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Decoded Attribute Description : {}", attributeDescription.getString() );
                        }

                        return;
                    }
                } );
        
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the LdapMessage Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
