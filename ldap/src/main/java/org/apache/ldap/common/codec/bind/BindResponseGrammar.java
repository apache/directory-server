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
package org.apache.ldap.common.codec.bind;

import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.LdapStatesEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the BindResponse LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindResponseGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( BindResponseGrammar.class );

    /** The instance of grammar. BindResponseGrammar is a singleton */
    private static IGrammar instance = new BindResponseGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BindResponseGrammar object.
     */
    private BindResponseGrammar()
    {
        name       = BindResponseGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_BIND_RESPONSE_STATE][256];

        //============================================================================================
        // BindResponse Message
        //============================================================================================
        // BindResponse ::= [APPLICATION 1] SEQUENCE {
        //     COMPONENTS OF LDAPResult,
        //     ...
        // Nothing to do
        super.transitions[LdapStatesEnum.BIND_RESPONSE_TAG][LdapConstants.BIND_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.BIND_RESPONSE_TAG, LdapStatesEnum.BIND_RESPONSE_VALUE, null );

        // LdapMessage ::= ... BindResponse ...
        // BindResponse ::= [APPLICATION 1] SEQUENCE { ... (Value)
        // We won't have a value. The next Tag will be the LDAPResult Tag (0x0A)
        super.transitions[LdapStatesEnum.BIND_RESPONSE_VALUE][LdapConstants.BIND_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.BIND_RESPONSE_VALUE, LdapStatesEnum.BIND_RESPONSE_LDAP_RESULT,
                new GrammarAction( "Init BindReponse" )
                {
                    public void action( IAsn1Container container ) 
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // Now, we can allocate the BindRequest Object
                        BindResponse bindResponse = new BindResponse();

                        // As this is a new Constructed object, we have to init its length
                        bindResponse.setParent( ldapMessage );

                        // And we associate it to the ldapMessage Object
                        ldapMessage.setProtocolOP( bindResponse );
                    }
                }  );

        // LdapMessage ::= ... BindResponse ...
        // BindResponse ::= [APPLICATION 1] SEQUENCE { ... (Value)
        //    COMPONENTS OF LDAPResult, ...
        // The Tag will be the LDAPResult Tag (0x0A). So we have to switch the grammar.
        // The current state will be stored.
        super.transitions[LdapStatesEnum.BIND_RESPONSE_LDAP_RESULT][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.BIND_RESPONSE_LDAP_RESULT, LdapStatesEnum.LDAP_RESULT_GRAMMAR_SWITCH,
                null );

        // LdapMessage ::= ... BindResponse ...
        // BindResponse ::= [APPLICATION 1] SEQUENCE {
        //       COMPONENTS OF LDAPResult,
        //    serverSaslCreds    [7] OCTET STRING OPTIONAL }
        // If there is a sasl credential, we will decode it here. We must control that we had a LdapResult
        //
        super.transitions[LdapStatesEnum.BIND_RESPONSE_LDAP_RESULT][LdapConstants.SERVER_SASL_CREDENTIAL_TAG] = new GrammarTransition(
                LdapStatesEnum.BIND_RESPONSE_LDAP_RESULT,
                LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_VALUE, null );

        // BindResponse ::= APPLICATION 1] SEQUENCE {
        //     ...
        //    serverSaslCreds   [7] OCTET STRING OPTIONAL } (Tag)
        // It's a server sasl credential if the tag is 0x87
        // Otherwise, if the tag is 0x90, it's a control
        super.transitions[LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_TAG][LdapConstants.SERVER_SASL_CREDENTIAL_TAG] =
            new GrammarTransition( LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_TAG,
                LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_VALUE, null );

        // BindResponse ::= APPLICATION 1] SEQUENCE {
        //     ...
        //    serverSaslCreds   [7] OCTET STRING OPTIONAL } (Length)
        //
        // We have to get the server sasl Credentials and store it in the BindResponse Object.
        // Two different following states are possible :
        // - a Control tag (0x90)
        // - nothing at all (end of the BindResponse).
        // We just have to transit to the first case, which will accept or not the transition.
        // This is done by transiting to the GRAMMAR_END state
        super.transitions[LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_VALUE][LdapConstants.SERVER_SASL_CREDENTIAL_TAG] =
            new GrammarTransition( LdapStatesEnum.BIND_RESPONSE_SERVER_SASL_CREDS_VALUE,
            		LdapStatesEnum.GRAMMAR_END,
                new GrammarAction( "Store server sasl credentials value" )
                {
                    public void action( IAsn1Container container )
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        BindResponse     bindResponseMessage =
                            ldapMessageContainer.getLdapMessage().getBindResponse();

                        // Get the Value and store it in the BindRequest
                        TLV tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to handle the special case of a 0 length server sasl credentials
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            bindResponseMessage.setServerSaslCreds( new byte[]{} );
                        }
                        else
                        {
                            bindResponseMessage.setServerSaslCreds( tlv.getValue().getData() );
                        }
                        
                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );
                        
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "The SASL credentials value is : {}", bindResponseMessage.getServerSaslCreds().toString() );
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
