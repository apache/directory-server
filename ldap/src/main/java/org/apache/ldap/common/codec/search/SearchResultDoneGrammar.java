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

import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.LdapStatesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SearchResultDone LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultDoneGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( SearchResultDoneGrammar.class );

    /** The instance of grammar. SearchResultDoneGrammar is a singleton */
    private static IGrammar instance = new SearchResultDoneGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SearchResultDoneGrammar object.
     */
    private SearchResultDoneGrammar()
    {
        name = SearchResultDoneGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_SEARCH_RESULT_DONE_STATE][256];

        //============================================================================================
        // DelResponse Message
        //============================================================================================
        // LdapMessage ::= ... SearchResultDone ...
        // SearchResultDone ::= [APPLICATION 5] LDAPResult (Tag)
        // Nothing to do.
        super.transitions[LdapStatesEnum.SEARCH_RESULT_DONE_TAG][LdapConstants.SEARCH_RESULT_DONE_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_RESULT_DONE_TAG, LdapStatesEnum.SEARCH_RESULT_DONE_VALUE, null );

        // LdapMessage ::= ... SearchResultDone ...
        // SearchResultDone ::= [APPLICATION 5] LDAPResult (Value)
        // The next Tag will be the LDAPResult Tag (0x0A).
        // We will switch the grammar then.
        super.transitions[LdapStatesEnum.SEARCH_RESULT_DONE_VALUE][LdapConstants.SEARCH_RESULT_DONE_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_RESULT_DONE_VALUE, LdapStatesEnum.SEARCH_RESULT_DONE_LDAP_RESULT, 
                new GrammarAction( "Init search Result Done" )
                {
                    public void action( IAsn1Container container )
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // Now, we can allocate the SearchResultDone Object
                        ldapMessage.setProtocolOP( new SearchResultDone() );
                        
                        log.debug( "Search Result Done found" );
                    }
                } );

        // LdapMessage ::= ... SearchResultDone ...
        // SearchResultDone ::= [APPLICATION 5] LDAPResult (Value)
        // Ok, we have a LDAPResult Tag (0x0A). So we have to switch the grammar.
        super.transitions[LdapStatesEnum.SEARCH_RESULT_DONE_LDAP_RESULT][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.SEARCH_RESULT_DONE_LDAP_RESULT, LdapStatesEnum.LDAP_RESULT_GRAMMAR_SWITCH, null );
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
