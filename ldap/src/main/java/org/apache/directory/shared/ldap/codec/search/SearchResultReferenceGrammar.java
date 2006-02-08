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
package org.apache.directory.shared.ldap.codec.search;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SearchResultReference LDAP message. All the actions
 * are declared in this class. As it is a singleton, these declaration are only
 * done once. If an action is to be added or modified, this is where the work is
 * to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultReferenceGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( SearchResultReferenceGrammar.class );

    /** The instance of grammar. SearchResultReferenceGrammar is a singleton */
    private static IGrammar instance = new SearchResultReferenceGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new SearchResultReferenceGrammar object.
     */
    private SearchResultReferenceGrammar()
    {
        name = SearchResultReferenceGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_SEARCH_RESULT_REFERENCE_STATE_STATE][256];

        // ============================================================================================
        // SearchResultReference Message
        // ============================================================================================
        // SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.SEARCH_RESULT_REFERENCE_TAG][LdapConstants.SEARCH_RESULT_REFERENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_TAG, LdapStatesEnum.SEARCH_RESULT_REFERENCE_VALUE, null );

        // SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL
        // (Value)
        // We won't have a value. The next Tag will be the LDAPUrl Tag (0x04)
        super.transitions[LdapStatesEnum.SEARCH_RESULT_REFERENCE_VALUE][LdapConstants.SEARCH_RESULT_REFERENCE_TAG] = new GrammarTransition(
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_VALUE, LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_TAG,
            new GrammarAction( "Init SearchResultReference" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // Now, we can allocate the BindRequest Object
                    SearchResultReference searchResultReference = new SearchResultReference();

                    // As this is a new Constructed object, we have to init its
                    // length
                    searchResultReference.setParent( ldapMessage );

                    // And we associate it to the ldapMessage Object
                    ldapMessage.setProtocolOP( searchResultReference );
                }
            } );

        // LDAPURL (Tag)
        super.transitions[LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_TAG, LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE,
            null );

        // LDAPURL loop (Tag)
        super.transitions[LdapStatesEnum.SEARCH_RESULT_REFERENCE_LOOP_OR_END_TAG][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_LOOP_OR_END_TAG,
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE, null );

        // LDAPURL (Value)
        super.transitions[LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_LDAP_URL_VALUE,
            LdapStatesEnum.SEARCH_RESULT_REFERENCE_LOOP_OR_END_TAG, new GrammarAction( "Store ldapUrl value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;

                    SearchResultReference searchResultReference = ldapMessageContainer.getLdapMessage()
                        .getSearchResultReference();

                    // Get the Value and store it in the BindRequest
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    // We have to handle the special case of a 0 length server
                    // sasl credentials
                    LdapURL url = LdapURL.EMPTY_URL;

                    if ( tlv.getLength().getLength() == 0 )
                    {
                        searchResultReference.addSearchResultReference( url );
                    }
                    else
                    {
                        try
                        {
                            url = new LdapURL( tlv.getValue().getData() );
                            searchResultReference.addSearchResultReference( url );
                        }
                        catch ( LdapURLEncodingException luee )
                        {
                            String badUrl = new String( tlv.getValue().getData() );
                            log.error( "The URL {} is not valid : {}", badUrl, luee.getMessage() );
                            throw new DecoderException( "Invalid URL : " + luee.getMessage() );
                        }
                    }

                    log.debug( "Search reference URL found : {}", url );

                    // We can have an END transition
                    ldapMessageContainer.grammarEndAllowed( true );

                    // We can have a Pop transition
                    ldapMessageContainer.grammarPopAllowed( true );

                    return;
                }
            } );

    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

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
