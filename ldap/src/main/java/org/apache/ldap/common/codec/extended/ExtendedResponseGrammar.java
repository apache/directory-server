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
package org.apache.ldap.common.codec.extended;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.asn1.primitives.OID;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.LdapMessageContainer;
import org.apache.ldap.common.codec.LdapStatesEnum;
import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ExtendedResponse LDAP message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedResponseGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ExtendedResponseGrammar.class );

    /** The instance of grammar. ExtendedResponseGrammar is a singleton */
    private static IGrammar instance = new ExtendedResponseGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ExtendedResponseGrammar object.
     */
    private ExtendedResponseGrammar()
    {
        name              = ExtendedResponseGrammar.class.getName();
        statesEnum        = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_EXTENDED_RESPONSE_STATE][256];

        //============================================================================================
        // ExtendedResponse
        //============================================================================================
        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE { (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_TAG][LdapConstants.EXTENDED_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_TAG,
                LdapStatesEnum.EXTENDED_RESPONSE_VALUE, null );

        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE { (Value)
        // Initialize the compare request pojo
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_VALUE][LdapConstants.EXTENDED_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_VALUE, LdapStatesEnum.EXTENDED_RESPONSE_LDAP_RESULT,
                new GrammarAction( "Init Extended Reponse" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage          ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We can allocate the ExtendedResponse Object
                        ldapMessage.setProtocolOP( new ExtendedResponse() );
                    }
                } );

        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        //     COMPONENTS OF LDAPResult, (Tag)
        //     ...
        // The Tag will be the LDAPResult Tag (0x0A). So we have to switch the grammar.
        // The current state will be stored.
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_LDAP_RESULT][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_LDAP_RESULT,
                LdapStatesEnum.LDAP_RESULT_GRAMMAR_SWITCH, 
                new GrammarAction( "Pop allowed" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {
                        container.grammarPopAllowed( true );
                    }
                });

        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        //     ...
        //     responseName     [10] LDAPOID OPTIONAL, (Tag)
        //     ...
        // Nothing to do.
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_LDAP_RESULT][LdapConstants.EXTENDED_RESPONSE_RESPONSE_NAME_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_LDAP_RESULT, LdapStatesEnum.EXTENDED_RESPONSE_NAME_VALUE, null
                 );
        
        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        //     ...
        //     responseName     [10] LDAPOID OPTIONAL, (Value)
        //     ...
        // Store the response name.
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_NAME_VALUE][LdapConstants.EXTENDED_RESPONSE_RESPONSE_NAME_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_NAME_VALUE, LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_TAG,
			        new GrammarAction( "Store name" )
			        {
			            public void action( IAsn1Container container ) throws DecoderException
			            {
			
			                LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
			                    container;
			                LdapMessage          ldapMessage          =
			                    ldapMessageContainer.getLdapMessage();
			
			                // We can allocate the ExtendedResponse Object
			                ExtendedResponse extendedResponse = ldapMessage.getExtendedResponse();
			
			                // Get the Value and store it in the ExtendedResponse
			                TLV tlv = ldapMessageContainer.getCurrentTLV();
			
			                // We have to handle the special case of a 0 length matched OID
			                if ( tlv.getLength().getLength() == 0 )
			                {
                                log.error( "The name must not be null" );
			                    throw new DecoderException( "The name must not be null" );
			                }
			                else
			                {
			                    extendedResponse.setResponseName( new OID( StringTools.utf8ToString( tlv.getValue().getData() ) ) );
			                }
                            
	                        // We can have an END transition
	                        ldapMessageContainer.grammarEndAllowed( true );

                            // We can have a Pop transition
                            ldapMessageContainer.grammarPopAllowed( true );

	                        if ( log.isDebugEnabled() )
                            {
                                log.debug( "OID read : {}", extendedResponse.getResponseName() );
                            }
			            }
			        } );

        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        //     ...
        //     response         [11] OCTET STRING OPTIONAL } (Tag)
        // Nothing to do
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_TAG][LdapConstants.EXTENDED_RESPONSE_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_TAG,
                LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_VALUE, null );

        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        //     ...
        //     response         [11] OCTET STRING OPTIONAL } (Value)
        // Store the response
        super.transitions[LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_VALUE][LdapConstants.EXTENDED_RESPONSE_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.EXTENDED_RESPONSE_RESPONSE_VALUE, LdapStatesEnum.END_STATE,
                new GrammarAction( "Store response" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage          ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // We can allocate the ExtendedResponse Object
                        ExtendedResponse extendedResponse = ldapMessage.getExtendedResponse();

                        // Get the Value and store it in the ExtendedResponse
                        TLV tlv = ldapMessageContainer.getCurrentTLV();

                        // We have to handle the special case of a 0 length matched OID
                        if ( tlv.getLength().getLength() == 0 )
                        {
                            extendedResponse.setResponse( new byte[]{} );
                        }
                        else
                        {
                            extendedResponse.setResponse( tlv.getValue().getData() );
                        }
                        
                        // We can have an END transition
                        ldapMessageContainer.grammarEndAllowed( true );

                        // We can have a Pop transition
                        ldapMessageContainer.grammarPopAllowed( true );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Extended value : {}", extendedResponse.getResponse() );
                        }
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
}
