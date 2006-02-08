/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.codec.search;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.BooleanDecoder;
import org.apache.directory.shared.asn1.util.BooleanDecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.codec.util.LdapStringEncodingException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Filter grammar. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MatchingRuleAssertionGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( MatchingRuleAssertionGrammar.class );

    /** The instance of grammar. FilterGrammar is a singleton */
    private static IGrammar instance = new MatchingRuleAssertionGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LdapResultGrammar object.
     */
    private MatchingRuleAssertionGrammar()
    {
        name              = MatchingRuleAssertionGrammar.class.getName();
        statesEnum        = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_FILTER_STATE][256];

        //============================================================================================
        // Here we are dealing with extensible matches 
        //
        // Filter ::= CHOICE {
        //     ...
        //     extensibleMatch [9] MatchingRuleAssertion} (Value)
        //     ...
        //
        // MatchingRuleAssertion ::= SEQUENCE {
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_TAG][LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_VALUE, null );

        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_VALUE][LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_VALUE, LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG, 
                new GrammarAction( "Init extensible match Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        // We can allocate the ExtensibleMatch Filter
                        Filter extensibleMatchFilter = new ExtensibleMatchFilter();
                        
                        // Get the parent, if any
                        Filter currentFilter = searchRequest.getCurrentFilter();
                        
                        if (currentFilter != null)
                        {
                            // Ok, we have a parent. The new Filter will be added to
                            // this parent, then. 
                            ((ConnectorFilter)currentFilter).addFilter(extensibleMatchFilter);
                            extensibleMatchFilter.setParent( currentFilter );
                        }
                        else
                        {
                            // No parent. This Filter will become the root.
                            searchRequest.setFilter(extensibleMatchFilter);
                            extensibleMatchFilter.setParent( searchRequest );
                        }

                        searchRequest.setCurrentFilter(extensibleMatchFilter);

                        // We now have to get back to the nearest filter which is not terminal.
                        unstackFilters( container );
                    }
                } ); 

        // MatchingRuleAssertion ::= SEQUENCE {
        //          matchingRule    [1] MatchingRuleId OPTIONAL, (Tag)
        //          ...
        // Nothing to do
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG][LdapConstants.SEARCH_MATCHING_RULE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_VALUE, null );

        // MatchingRuleAssertion ::= SEQUENCE {
        //          matchingRule    [1] MatchingRuleId OPTIONAL, (Value)
        //          ...
        // tore the matching rule value.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_VALUE][LdapConstants.SEARCH_MATCHING_RULE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_VALUE, LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG, 
                new GrammarAction( "Store matching rule Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                        container;
                        LdapMessage      ldapMessage          =
                        ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();

                        // Store the value.
                        ExtensibleMatchFilter extensibleMatchFilter = (ExtensibleMatchFilter)searchRequest.getCurrentFilter();
                        
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The matching rule is empty" );
                            throw new DecoderException( "Invalid matching rule : it can't be empty" );
                        }
                        else
                        {
                            try
                            {
                                extensibleMatchFilter.setMatchingRule(new LdapString(tlv.getValue().getData()));
                            }
                            catch ( LdapStringEncodingException lsee )
                            {
                                String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                                log.error( "The matching rule ({}) is invalid", msg );
                                throw new DecoderException( "Invalid matching rule " + msg + ", : " + lsee.getMessage() );
                            }
                        }
                    }
                });

        // MatchingRuleAssertion ::= SEQUENCE { 
        //          (void)
        //          type            [2] AttributeDescription OPTIONAL, (Tag)
        //          ...
        // Nothing to do.
        // Nothing to do.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG][LdapConstants.MATCHING_RULE_ASSERTION_TYPE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCHING_RULE_OR_TYPE_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_VALUE, null );

                
        // MatchingRuleAssertion ::= SEQUENCE { 
        //          matchingRule    [1] MatchingRuleId OPTIONAL,
        //          type            [2] AttributeDescription OPTIONAL, (Tag)
        //          ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG][LdapConstants.MATCHING_RULE_ASSERTION_TYPE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_VALUE, null );

        // MatchingRuleAssertion ::= SEQUENCE { 
        //          ...
        //          type            [2] AttributeDescription OPTIONAL, (Value)
        //          ...
        // Store the matching type value.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_VALUE][LdapConstants.MATCHING_RULE_ASSERTION_TYPE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_VALUE, LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_TAG, 
                new GrammarAction( "Store matching type Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                        container;
                        LdapMessage      ldapMessage          =
                        ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();

                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The type cannot be null in a MacthingRuleAssertion" );
                            throw new DecoderException( "The type cannot be null in a MacthingRuleAssertion" );
                        }
                        else
                        {
                            // Store the value.
                            ExtensibleMatchFilter extensibleMatchFilter = (ExtensibleMatchFilter)searchRequest.getCurrentFilter();
                            
                            try
                            {
                                LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                                extensibleMatchFilter.setType( type );
                            }
                            catch ( LdapStringEncodingException lsee )
                            {
                                String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                                log.error( "The match filter ({}) is invalid", msg );
                                throw new DecoderException( "Invalid match filter " + msg + ", : " + lsee.getMessage() );
                            }
                        }
                    }
                });
                
        // MatchingRuleAssertion ::= SEQUENCE { 
        //          matchingRule    [1] MatchingRuleId OPTIONAL,
        //          (void),
        //          matchValue      [3] AssertionValue, (Tag)
        //          ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG][LdapConstants.SEARCH_MATCH_VALUE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_TYPE_OR_MATCH_VALUE_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE, null );

        // MatchingRuleAssertion ::= SEQUENCE { 
        //          ...
        //          type            [2] AttributeDescription OPTIONAL,
        //          matchValue      [3] AssertionValue, (Tag)
        //          ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_TAG][LdapConstants.SEARCH_MATCH_VALUE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE, null );

        // MatchingRuleAssertion ::= SEQUENCE { 
        //          ...
        //          matchValue      [3] AssertionValue, (Value)
        //          ...
        // Store the matching type value.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE][LdapConstants.SEARCH_MATCH_VALUE_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_MATCH_VALUE_VALUE, LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_TAG, 
                new GrammarAction( "Store matching match value Value" )
                {
                    public void action( IAsn1Container container )
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                        container;
                        LdapMessage      ldapMessage          =
                        ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();

                        // Store the value.
                        ExtensibleMatchFilter extensibleMatchFilter = (ExtensibleMatchFilter)searchRequest.getCurrentFilter();
                        extensibleMatchFilter.setMatchValue( StringTools.utf8ToString( tlv.getValue().getData() ) );

                        // We can have a pop transition
                        ldapMessageContainer.grammarPopAllowed( true );
                    }
                });
                
        // MatchingRuleAssertion ::= SEQUENCE { 
        //          ...
        //          dnAttributes    [4] BOOLEAN DEFAULT FALSE } (Tag)
        // Nothing to do.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_TAG][LdapConstants.DN_ATTRIBUTES_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_VALUE, null );

        // MatchingRuleAssertion ::= SEQUENCE { 
        //          ...
        //          dnAttributes    [4] BOOLEAN DEFAULT FALSE } (Length)
        // Store the matching type value.
        super.transitions[LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_VALUE][LdapConstants.DN_ATTRIBUTES_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.MATCHING_RULE_ASSERTION_DN_ATTRIBUTES_VALUE, LdapStatesEnum.END_STATE, 
                new GrammarAction( "Store matching dnAttributes Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                        container;
                        LdapMessage      ldapMessage          =
                        ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();

                        // Store the value.
                        ExtensibleMatchFilter extensibleMatchFilter = (ExtensibleMatchFilter)searchRequest.getCurrentFilter();

                        // We get the value. If it's a 0, it's a FALSE. If it's
                        // a FF, it's a TRUE. Any other value should be an error,
                        // but we could relax this constraint. So if we have something
                        // which is not 0, it will be interpreted as TRUE, but we
                        // will generate a warning.
                        Value value     = tlv.getValue();

                        try
                        {
                            extensibleMatchFilter.setDnAttributes( BooleanDecoder.parse( value ) );
                        }
                        catch ( BooleanDecoderException bde )
                        {
                            log.error("The DN attributes flag {} is invalid : {}. It should be 0 or 255",
                                    StringTools.dumpBytes( value.getData() ), 
                                    bde.getMessage() );
                        
                            throw new DecoderException( bde.getMessage() );
                        }
                            
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "DN Attributes : {}", new Boolean( extensibleMatchFilter.isDnAttributes() ) );
                        }
                        
                        // We can have a pop transition
                        ldapMessageContainer.grammarPopAllowed( true );
                    }
                });
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * This class is a singleton.
     *
     * @return An instance on this grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
    
    /**
     * This method is used to clear the filter's stack for terminated elements. An element
     * is considered as terminated either if :
     *  - it's a final element (ie an element which cannot contains a Filter)
     *  - its current length equals its expected length.
     * 
     * @param container The container being decoded
     */
    private void unstackFilters( IAsn1Container container ) 
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessage      ldapMessage          =   ldapMessageContainer.getLdapMessage();
        SearchRequest searchRequest = ldapMessage.getSearchRequest();
        
        TLV tlv            = ldapMessageContainer.getCurrentTLV();
        
        // Get the parent, if any
        Filter currentFilter = searchRequest.getCurrentFilter();

        // We know have to check if the parent has been completed
	    if (tlv.getParent().getExpectedLength() == 0)
	    {
	        TLV parent = tlv.getParent();
	        
	        //  The parent has been completed, we have to switch it
	        while ( (parent != null) && (parent.getExpectedLength() == 0) )
	        {
	            parent = parent.getParent();
	            
	            if ( ( currentFilter != null ) && ( currentFilter.getParent() instanceof Filter ) )
	            {
	                currentFilter = (Filter)currentFilter.getParent();
	            }
	            else
	            {
	                currentFilter = null;
	                break;
	            }
	        }
	        
	        searchRequest.setCurrentFilter(currentFilter);
	    }
    }
}
