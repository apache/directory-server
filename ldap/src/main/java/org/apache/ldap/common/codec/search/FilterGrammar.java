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

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.ldap.common.codec.AttributeValueAssertion;
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
 * This class implements the Filter grammar. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FilterGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( FilterGrammar.class );

    /** The instance of grammar. FilterGrammar is a singleton */
    private static IGrammar instance = new FilterGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LdapResultGrammar object.
     */
    private FilterGrammar()
    {
        name              = FilterGrammar.class.getName();
        statesEnum        = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_FILTER_STATE][256];

        //============================================================================================
        // Search Request And Filter
        // This is quite complicated, because we have a tree structure to build,
        // and we may have many elements on each node. For instance, considering the 
        // search filter :
        // (& (| (a = b) (c = d)) (! (e = f)) (attr =* h))
        // We will have to create an And filter with three children :
        //  - an Or child,
        //  - a Not child
        //  - and a Present child.
        // The Or child will also have two children.
        //
        // We know when we have a children while decoding the PDU, because the length
        // of its parent has not yet reached its expected length.
        //
        // This search filter :
        // (&(|(objectclass=top)(ou=contacts))(!(objectclass=ttt))(objectclass=*top))
        // is encoded like this :
        //                              +----------------+---------------+
        //                              | ExpectedLength | CurrentLength |
        //+-----------------------------+----------------+---------------+
        //|A0 52                        | 82             | 0             | new level 1
        //|   A1 24                     | 82 36          | 0 0           | new level 2
        //|      A3 12                  | 82 36 18       | 0 0 0         | new level 3
        //|         04 0B 'objectclass' | 82 36 18       | 0 0 13        |
        //|         04 03 'top'         | 82 36 18       | 0 20 18       | 
        //|                             |       ^               ^        |
        //|                             |       |               |        |
        //|                             |       +---------------+        |
        //+-----------------------------* end level 3 -------------------*
        //|      A3 0E                  | 82 36 14       | 0 0 0         | new level 3
        //|         04 02 'ou'          | 82 36 14       | 0 0 4         |
        //|         04 08 'contacts'    | 82 36 14       | 38 36 14      | 
        //|                             |    ^  ^             ^  ^       |
        //|                             |    |  |             |  |       |
        //|                             |    |  +-------------|--+       |
        //|                             |    +----------------+          |
        //+-----------------------------* end level 3, end level 2 ------*
        //|   A2 14                     | 82 20          | 38 0          | new level 2
        //|      A3 12                  | 82 20 18       | 38 0 0        | new level 3
        //|         04 0B 'objectclass' | 82 20 18       | 38 0 13       | 
        //|         04 03 'ttt'         | 82 20 18       | 60 20 18      |
        //|                             |    ^  ^             ^  ^       |
        //|                             |    |  |             |  |       |
        //|                             |    |  +-------------|--+       |
        //|                             |    +----------------+          |
        //+-----------------------------* end level 3, end level 2 ------*
        //|   A4 14                     | 82 20          | 60 0          | new level 2
        //|      04 0B 'objectclass'    | 82 20          | 60 13         |
        //|      30 05                  | 82 20          | 60 13         |
        //|         82 03 'top'         | 82 20          | 82 20         | 
        //|                             | ^  ^             ^  ^          |
        //|                             | |  |             |  |          |
        //|                             | |  +-------------|--+          |
        //|                             | +----------------+             |
        //+-----------------------------* end level 2, end level 1 ------*
        //+-----------------------------+----------------+---------------+
        //
        // When the current length equals the expected length of the parent PDU,
        // then we are able to 'close' the parent : it has all its children. This
        // is propagated through all the tree, until either there are no more
        // parents, or the expected length of the parent is different from the
        // current length.
        //                              
        //============================================================================================
        // Filter ::= CHOICE {
        //     and             [0] SET OF Filter, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.AND_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_AND_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     or              [1] SET OF Filter, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.OR_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_OR_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     not             [2] Filter, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.NOT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_NOT_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     equalityMatch   [3] AttributeValueAssertion, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.EQUALITY_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_EQUALITY_MATCH_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     substrings      [4] SubstringFilter, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.SUBSTRINGS_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.SUBSTRING_FILTER_GRAMMAR_SWITCH, 
                new GrammarAction( "Allow pop" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        container.grammarPopAllowed( true );
                    }
                });

        // Filter ::= CHOICE {
        //     ...
        //     greaterOrEqual  [5] AttributeValueAssertion, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.GREATER_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_GREATER_OR_EQUAL_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     lessOrEqual     [6] AttributeValueAssertion, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.LESS_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_LESS_OR_EQUAL_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     present         [7] AttributeDescription, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.PRESENT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_PRESENT_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     approxMatch     [8] AttributeValueAssertion, (Tag)
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.APPROX_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.FILTER_APPROX_MATCH_VALUE, null );

        // Filter ::= CHOICE {
        //     ...
        //     extensibleMatch [9] ExtensibleMatchFilter } (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.FILTER_TAG][LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_TAG, LdapStatesEnum.MATCHING_RULE_ASSERTION_GRAMMAR_SWITCH,                 
                new GrammarAction( "Allow pop" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        container.grammarPopAllowed( true );
                    }
                });

        // Filter ::= CHOICE {
        //     and             [0] SET OF Filter, (Value)
        //     ...
        // We just have to switch to the initial state of Filter, because this is what
        // we will get !
        super.transitions[LdapStatesEnum.FILTER_AND_VALUE][LdapConstants.AND_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_AND_VALUE, LdapStatesEnum.FILTER_TAG, 
                new GrammarAction( "Init And Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();
                        
                        TLV tlv = ldapMessageContainer.getCurrentTLV();
                        
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The And filter PDU must not be empty" );
                            throw new DecoderException( "The And filter PDU must not be empty" );
                        }
                        
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        // We can allocate the SearchRequest
                        Filter andFilter = new AndFilter();
                        
                        // Get the parent, if any
                        Filter currentFilter = searchRequest.getCurrentFilter();
                        
                        if (currentFilter != null)
                        {
                            // Ok, we have a parent. The new Filter will be added to
                            // this parent, then. 
                            ((ConnectorFilter)currentFilter).addFilter(andFilter);
                            andFilter.setParent( currentFilter );
                        }
                        else
                        {
                            // No parent. This Filter will become the root.
                            searchRequest.setFilter(andFilter);
                            andFilter.setParent( searchRequest );
                        }

                        searchRequest.setCurrentFilter(andFilter);
                    }
                });

        // Filter ::= CHOICE {
        //     ...
        //     or              [1] SET OF Filter, (Value)
        //     ...
        // We just have to switch to the initial state of Filter, because this is what
        // we will get !
        super.transitions[LdapStatesEnum.FILTER_OR_VALUE][LdapConstants.OR_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_OR_VALUE, LdapStatesEnum.FILTER_TAG, 
                new GrammarAction( "Init Or Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        TLV tlv = ldapMessageContainer.getCurrentTLV();
                        
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The Or filter PDU must not be empty" );
                            throw new DecoderException( "The Or filter PDU must not be empty" );
                        }
                        
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        // We can allocate the SearchRequest
                        Filter orFilter = new OrFilter();
                        
                        // Get the parent, if any
                        Filter currentFilter = searchRequest.getCurrentFilter();
                        
                        if (currentFilter != null)
                        {
                            // Ok, we have a parent. The new Filter will be added to
                            // this parent, then. 
                            ((ConnectorFilter)currentFilter).addFilter(orFilter);
                            orFilter.setParent( currentFilter );
                        }
                        else
                        {
                            // No parent. This Filter will become the root.
                            searchRequest.setFilter(orFilter);
                            orFilter.setParent( searchRequest );
                        }

                        searchRequest.setCurrentFilter(orFilter);
                    }
                });

        // Filter ::= CHOICE {
        //     ...
        //     not             [2] Filter, (Value)
        //     ...
        // We just have to switch to the initial state of Filter, because this is what
        // we will get !
        super.transitions[LdapStatesEnum.FILTER_NOT_VALUE][LdapConstants.NOT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_NOT_VALUE, LdapStatesEnum.FILTER_TAG, 
                new GrammarAction( "Init Not Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        TLV tlv = ldapMessageContainer.getCurrentTLV();
                        
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The Not filter PDU must not be empty" );
                            throw new DecoderException( "The Not filter PDU must not be empty" );
                        }
                        
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        // We can allocate the SearchRequest
                        Filter notFilter = new NotFilter();
                        
                        // Get the parent, if any
                        Filter currentFilter = searchRequest.getCurrentFilter();
                        
                        if (currentFilter != null)
                        {
                            // Ok, we have a parent. The new Filter will be added to
                            // this parent, then. 
                            ((ConnectorFilter)currentFilter).addFilter(notFilter);
                            notFilter.setParent( currentFilter );
                        }
                        else
                        {
                            // No parent. This Filter will become the root.
                            searchRequest.setFilter(notFilter);
                            notFilter.setParent( searchRequest );
                        }

                        searchRequest.setCurrentFilter(notFilter);
                    }
                });

        // Filter ::= CHOICE {
        //     ...
        //     equalityMatch   [3] AttributeValueAssertion, (Value)
        //     ...
        // We will create the filter container (as this is an equalityMatch filter,
        // we will create an AttributeValueAssertionFilter).
        super.transitions[LdapStatesEnum.FILTER_EQUALITY_MATCH_VALUE][LdapConstants.EQUALITY_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_EQUALITY_MATCH_VALUE, LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG, 
                new GrammarAction( "Init Equality Match Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        compareFilterAction(container, LdapConstants.EQUALITY_MATCH_FILTER);
                    }
                }); 
                
        // Filter ::= CHOICE {
        //     ...
        //     greaterOrEqual  [5] AttributeValueAssertion, (Value)
        //     ...
        // We will create the filter container (as this is an GreaterOrEqual filter,
        // we will create an AttributeValueAssertionFilter).
        super.transitions[LdapStatesEnum.FILTER_GREATER_OR_EQUAL_VALUE][LdapConstants.GREATER_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_GREATER_OR_EQUAL_VALUE, LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG, 
                new GrammarAction( "Init Greater Or Equal Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        compareFilterAction(container, LdapConstants.GREATER_OR_EQUAL_FILTER);
                    }
                } ); 
                
        // Filter ::= CHOICE {
        //     ...
        //     lessOrEqual    [6] AttributeValueAssertion, (Value)
        //     ...
        // We will create the filter container (as this is an lessOrEqual filter,
        // we will create an AttributeValueAssertionFilter).
        super.transitions[LdapStatesEnum.FILTER_LESS_OR_EQUAL_VALUE][LdapConstants.LESS_OR_EQUAL_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_LESS_OR_EQUAL_VALUE, LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG, 
                new GrammarAction( "Init Less Or Equal Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        compareFilterAction(container, LdapConstants.LESS_OR_EQUAL_FILTER );
                    }
                } ); 
                
        // Filter ::= CHOICE {
        //     ...
        //     approxMatch    [8] AttributeValueAssertion, (Value)
        //     ...
        // We will create the filter container (as this is an approxMatch filter,
        // we will create an AttributeValueAssertionFilter).
        super.transitions[LdapStatesEnum.FILTER_APPROX_MATCH_VALUE][LdapConstants.APPROX_MATCH_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_APPROX_MATCH_VALUE, LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG, 
                new GrammarAction( "Init ApproxMatch Filter" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        compareFilterAction(container, LdapConstants.APPROX_MATCH_FILTER );
                    }
                } ); 
                
        // AttributeValueAssertion ::= SEQUENCE {
        //    attributeDesc   AttributeDescription, (TAG)
        //     ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ATTRIBUTE_DESC_TAG, LdapStatesEnum.FILTER_ATTRIBUTE_DESC_VALUE, null);

        // AttributeValueAssertion ::= SEQUENCE {
        //    attributeDesc   AttributeDescription, (VALUE)
        //     ...
        // We have to set the attribute description in the current filter.
        // It could be an equalityMatch, greaterOrEqual, lessOrEqual or an
        // approxMatch filter.
        super.transitions[LdapStatesEnum.FILTER_ATTRIBUTE_DESC_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ATTRIBUTE_DESC_VALUE, LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG, 
                new GrammarAction( "Init attributeDesc Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();
                        
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        
                        try
                        {
                        	LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                            assertion.setAttributeDesc( type );
                        }
                        catch ( LdapStringEncodingException lsee )
                        {
                            String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                            log.error( "The assertion description ({}) is invalid", msg );
                            throw new DecoderException( "Invalid assertion description " + msg + ", : " + lsee.getMessage() );
                        }
                        
                        AttributeValueAssertionFilter currentFilter = (AttributeValueAssertionFilter)searchRequest.getCurrentFilter();
                        currentFilter.setAssertion(assertion);
                    }
                });

        // AttributeValueAssertion ::= SEQUENCE {
        //     ...
        //    assertionValue  AssertionValue } (TAG)
        // Nothing to do.
        super.transitions[LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG, LdapStatesEnum.FILTER_ASSERTION_VALUE_VALUE, null);

        // AttributeValueAssertion ::= SEQUENCE {
        //     ...
        //    assertionValue  AssertionValue } (VALUE)
        // We have to set the attribute description in the current filter.
        // It could be an equalityMatch, greaterOrEqual, lessOrEqual or an
        // approxMatch filter.
        // When finished, we will transit to the first state.
        super.transitions[LdapStatesEnum.FILTER_ASSERTION_VALUE_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ASSERTION_VALUE_VALUE, LdapStatesEnum.FILTER_TAG, 
                new GrammarAction( "Init AssertionValue Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();
                        
                        // The value can be null.
                        Object assertionValue = StringTools.EMPTY_BYTES;

                        if ( tlv.getLength().getLength() != 0 )
                        {
                            assertionValue = tlv.getValue().getData();
                        }
                        
                        AttributeValueAssertionFilter currentFilter = (AttributeValueAssertionFilter)searchRequest.getCurrentFilter();
                        AttributeValueAssertion assertion = currentFilter.getAssertion();
                        
                        if ( ldapMessageContainer.isBinary( assertion.getAttributeDesc() ) )
                        {
                            assertion.setAssertionValue( assertionValue );
                        }
                        else
                        {
                            assertion.setAssertionValue( StringTools.utf8ToString( (byte[])assertionValue ) );
                        }
                        
                        // We now have to get back to the nearest filter which is not terminal.
                        unstackFilters( container );
                        container.grammarPopAllowed( true );
                    }
                });

        // AttributeValueAssertion ::= SEQUENCE {
        //    attributeDesc   AttributeDescription, (VALUE)
        //     ...
        // We have to set the attribute description in the current filter.
        // It could be an equalityMatch, greaterOrEqual, lessOrEqual or an
        // approxMatch filter.
        super.transitions[LdapStatesEnum.FILTER_ATTRIBUTE_DESC_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ATTRIBUTE_DESC_VALUE, LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG, 
                new GrammarAction( "Init attributeDesc Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();
                        
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            log.error( "The attribute description is empty " );
                            throw new DecoderException( "The type can't be null" );
                        }
                        else
                        {
                            try
                            {
                            	LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                                assertion.setAttributeDesc( type );
                            }
                            catch ( LdapStringEncodingException lsee )
                            {
                                String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                                log.error( "The assertion value ({}) is invalid", msg );
                                throw new DecoderException( "Invalid assertion value " + msg + ", : " + lsee.getMessage() );
                            }
                            
                            AttributeValueAssertionFilter currentFilter = (AttributeValueAssertionFilter)searchRequest.getCurrentFilter();
                            currentFilter.setAssertion(assertion);
                        }
                    }
                });

        // AttributeValueAssertion ::= SEQUENCE {
        //     ...
        //    assertionValue  AssertionValue } (TAG)
        // Nothing to do.
        super.transitions[LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_ASSERTION_VALUE_TAG, LdapStatesEnum.FILTER_ASSERTION_VALUE_VALUE, null);

        // Filter ::= CHOICE {
        //     ...
        //     present    [7] AttributeDescription, (Value)
        //     ...
        super.transitions[LdapStatesEnum.FILTER_PRESENT_VALUE][LdapConstants.PRESENT_FILTER_TAG] = new GrammarTransition(
                LdapStatesEnum.FILTER_PRESENT_VALUE, LdapStatesEnum.FILTER_TAG, 
                new GrammarAction( "Init present filter Value" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                        container;
                        LdapMessage      ldapMessage          =
                        ldapMessageContainer.getLdapMessage();
                        SearchRequest searchRequest = ldapMessage.getSearchRequest();

                        TLV tlv            = ldapMessageContainer.getCurrentTLV();

                        // We can allocate the Attribute Value Assertion
                        PresentFilter presentFilter = new PresentFilter();
                        
                        // Get the parent, if any
                        Filter currentFilter = searchRequest.getCurrentFilter();
                        
                        if (currentFilter != null)
                        {
                            // Ok, we have a parent. The new Filter will be added to
                            // this parent, then. 
                            ((ConnectorFilter)currentFilter).addFilter(presentFilter);
                            presentFilter.setParent( currentFilter );
                        }
                        else
                        {
                            // No parent. This Filter will become the root.
                            //searchRequest.setCurrentFilter(presentFilter);
                            presentFilter.setParent( searchRequest );
                            searchRequest.setFilter( presentFilter );
                        }

                        String value = StringTools.utf8ToString( tlv.getValue().getData() );
                        
                        if ( StringTools.isEmpty( value ) )
                        {
                            presentFilter.setAttributeDescription( LdapString.EMPTY_STRING );
                        }
                        else
                        {
                            // Store the value.
                            try
                            {
                            	LdapString type = LdapDN.normalizeAttribute( tlv.getValue().getData() );
                                presentFilter.setAttributeDescription( type );
                            }
                            catch ( LdapStringEncodingException lsee )
                            {
                                String msg = StringTools.dumpBytes( tlv.getValue().getData() );
                                log.error( "Present filter attribute description ({}) is invalid", msg );
                                throw new DecoderException( "Invalid present filter attribute description " + msg + ", : " + lsee.getMessage() );
                            }
                        }
                            
                        // We now have to get back to the nearest filter which is not terminal.
                        unstackFilters( container );
                        container.grammarPopAllowed( true );
                    }
                } );

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

        // We now have to check if the parent has been completed
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
    
    /**
     * This method is used by each comparaison filters (=, <=, >= or ~=).
     * 
     * @param container The LdapContainer
     * @throws DecoderException If any error occurs.
     */
    private void compareFilterAction( IAsn1Container container , int filterType ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
            container;
        LdapMessage      ldapMessage          =
            ldapMessageContainer.getLdapMessage();
        SearchRequest searchRequest = ldapMessage.getSearchRequest();

        // We can allocate the Attribute Value Assertion
        Filter filter = new AttributeValueAssertionFilter( filterType );
        
        // Get the parent, if any
        Filter currentFilter = searchRequest.getCurrentFilter();
        
        if (currentFilter != null)
        {
            // Ok, we have a parent. The new Filter will be added to
            // this parent, then. 
            ((ConnectorFilter)currentFilter).addFilter(filter);
            filter.setParent( currentFilter );
        }
        else
        {
            // No parent. This Filter will become the root.
            filter.setParent( searchRequest );
            searchRequest.setFilter(filter);
        }

        searchRequest.setCurrentFilter(filter);
    }
}
