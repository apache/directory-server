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
package org.apache.directory.shared.ldap.codec;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestGrammar;
import org.apache.directory.shared.ldap.codec.add.AddRequestGrammar;
import org.apache.directory.shared.ldap.codec.add.AddResponseGrammar;
import org.apache.directory.shared.ldap.codec.bind.BindRequestGrammar;
import org.apache.directory.shared.ldap.codec.bind.BindResponseGrammar;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestGrammar;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseGrammar;
import org.apache.directory.shared.ldap.codec.del.DelRequestGrammar;
import org.apache.directory.shared.ldap.codec.del.DelResponseGrammar;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestGrammar;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseGrammar;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestGrammar;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseGrammar;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestGrammar;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseGrammar;
import org.apache.directory.shared.ldap.codec.search.FilterGrammar;
import org.apache.directory.shared.ldap.codec.search.MatchingRuleAssertionGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchRequestGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryGrammar;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceGrammar;
import org.apache.directory.shared.ldap.codec.search.SubstringFilterGrammar;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestGrammar;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * The LdapMessage container stores all the messages decoded by the Asn1Decoder.
 * When dealing whith an incoding PDU, we will obtain a LdapMessage in the
 * ILdapContainer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapMessageContainer extends AbstractContainer implements IAsn1Container // extends
                                                                                        // AbstractLdapContainer
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The ldap message */
    private LdapMessage ldapMessage;

    /** A HashSet which contaons the binary attributes */
    private Set binaries;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessageContainer object. We will store ten grammars,
     * it's enough ...
     */
    public LdapMessageContainer()
    {
        this( new HashSet() );
    }


    /**
     * Creates a new LdapMessageContainer object. We will store ten grammars,
     * it's enough ...
     */
    public LdapMessageContainer(Set binaries)
    {
        super();
        currentGrammar = 0;
        grammars = new IGrammar[LdapStatesEnum.NB_GRAMMARS];
        grammarStack = new IGrammar[10];
        stateStack = new int[10];
        popAllowedStack = new boolean[10];
        nbGrammars = 0;

        grammars[LdapStatesEnum.LDAP_MESSAGE_GRAMMAR] = LdapMessageGrammar.getInstance();
        grammars[LdapStatesEnum.LDAP_CONTROL_GRAMMAR] = LdapControlGrammar.getInstance();
        grammars[LdapStatesEnum.BIND_REQUEST_GRAMMAR] = BindRequestGrammar.getInstance();
        grammars[LdapStatesEnum.LDAP_RESULT_GRAMMAR] = LdapResultGrammar.getInstance();
        grammars[LdapStatesEnum.BIND_RESPONSE_GRAMMAR] = BindResponseGrammar.getInstance();
        grammars[LdapStatesEnum.UNBIND_REQUEST_GRAMMAR] = UnBindRequestGrammar.getInstance();
        grammars[LdapStatesEnum.ABANDON_REQUEST_GRAMMAR] = AbandonRequestGrammar.getInstance();
        grammars[LdapStatesEnum.ADD_RESPONSE_GRAMMAR] = AddResponseGrammar.getInstance();
        grammars[LdapStatesEnum.COMPARE_RESPONSE_GRAMMAR] = CompareResponseGrammar.getInstance();
        grammars[LdapStatesEnum.DEL_RESPONSE_GRAMMAR] = DelResponseGrammar.getInstance();
        grammars[LdapStatesEnum.MODIFY_RESPONSE_GRAMMAR] = ModifyResponseGrammar.getInstance();
        grammars[LdapStatesEnum.MODIFY_DN_RESPONSE_GRAMMAR] = ModifyDNResponseGrammar.getInstance();
        grammars[LdapStatesEnum.SEARCH_RESULT_DONE_GRAMMAR] = SearchResultDoneGrammar.getInstance();
        grammars[LdapStatesEnum.SEARCH_REQUEST_GRAMMAR] = SearchRequestGrammar.getInstance();
        grammars[LdapStatesEnum.FILTER_GRAMMAR] = FilterGrammar.getInstance();
        grammars[LdapStatesEnum.SEARCH_RESULT_ENTRY_GRAMMAR] = SearchResultEntryGrammar.getInstance();
        grammars[LdapStatesEnum.MODIFY_REQUEST_GRAMMAR] = ModifyRequestGrammar.getInstance();
        grammars[LdapStatesEnum.SEARCH_RESULT_REFERENCE_GRAMMAR] = SearchResultReferenceGrammar.getInstance();
        grammars[LdapStatesEnum.ADD_REQUEST_GRAMMAR] = AddRequestGrammar.getInstance();
        grammars[LdapStatesEnum.MODIFY_DN_REQUEST_GRAMMAR] = ModifyDNRequestGrammar.getInstance();
        grammars[LdapStatesEnum.DEL_REQUEST_GRAMMAR] = DelRequestGrammar.getInstance();
        grammars[LdapStatesEnum.COMPARE_REQUEST_GRAMMAR] = CompareRequestGrammar.getInstance();
        grammars[LdapStatesEnum.EXTENDED_REQUEST_GRAMMAR] = ExtendedRequestGrammar.getInstance();
        grammars[LdapStatesEnum.EXTENDED_RESPONSE_GRAMMAR] = ExtendedResponseGrammar.getInstance();
        grammars[LdapStatesEnum.SUBSTRING_FILTER_GRAMMAR] = SubstringFilterGrammar.getInstance();
        grammars[LdapStatesEnum.MATCHING_RULE_ASSERTION_GRAMMAR] = MatchingRuleAssertionGrammar.getInstance();

        grammarStack[currentGrammar] = grammars[LdapStatesEnum.LDAP_MESSAGE_GRAMMAR];

        states = LdapStatesEnum.getInstance();

        this.binaries = binaries;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * @return Returns the ldapMessage.
     */
    public LdapMessage getLdapMessage()
    {

        return ldapMessage;
    }


    /**
     * Set a ldapMessage Object into the container. It will be completed by the
     * ldapDecoder .
     * 
     * @param ldapMessage
     *            The ldapMessage to set.
     */
    public void setLdapMessage( LdapMessage ldapMessage )
    {
        this.ldapMessage = ldapMessage;
    }


    public void clean()
    {
        super.clean();

        ldapMessage = null;
    }


    /**
     * @return Returns true if the attribute is binary.
     */
    public boolean isBinary( String id )
    {
        return binaries.contains( StringTools.lowerCase( StringTools.trim( id ) ) );
    }
}
