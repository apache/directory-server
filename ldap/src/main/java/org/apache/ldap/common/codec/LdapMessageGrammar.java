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
package org.apache.ldap.common.codec;

import org.apache.asn1.codec.DecoderException;
import org.apache.asn1.ber.grammar.AbstractGrammar;
import org.apache.asn1.ber.grammar.IGrammar;
import org.apache.asn1.ber.grammar.GrammarTransition;
import org.apache.asn1.ber.grammar.GrammarAction;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.Value;
import org.apache.asn1.ber.tlv.TLV;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.util.IntegerDecoderException;
import org.apache.asn1.util.IntegerDecoder;
import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the LdapMessage message. All the actions are declared in this
 * class. As it is a singleton, these declaration are only done once.
 * 
 * If an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapMessageGrammar extends AbstractGrammar implements IGrammar
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( LdapMessageGrammar.class );

    /** The instance of grammar. LdapMessageGrammar is a singleton */
    private static IGrammar instance = new LdapMessageGrammar();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessageGrammar object.
     */
    private LdapMessageGrammar()
    {

        name       = LdapMessageGrammar.class.getName();
        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_LDAP_MESSAGE_STATE][256];

        //============================================================================================
        // LdapMessage
        //============================================================================================
        // LDAPMessage --> SEQUENCE { ... (Tag)
        // We have a LDAPMessage, and the tag must be 0x30
        super.transitions[LdapStatesEnum.LDAP_MESSAGE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
                LdapStatesEnum.LDAP_MESSAGE_TAG, LdapStatesEnum.LDAP_MESSAGE_VALUE, null );

         // LDAPMessage --> SEQUENCE { ... (Value)
        // Nothing to do, it's a constructed TLV. It's just a phantom transition ...
        super.transitions[LdapStatesEnum.LDAP_MESSAGE_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
                LdapStatesEnum.LDAP_MESSAGE_VALUE, LdapStatesEnum.LDAP_MESSAGE_ID_TAG, 
                new GrammarAction( "LdapMessage initialization" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;

                        TLV   tlv       = ldapMessageContainer.getCurrentTLV();

                        // The Length should not be null
                        if ( tlv.getLength().getLength() == 0 )
                        {
                        	log.error( "The LdapMessage has a zero length. This is not allowed" );
                        	throw new DecoderException( "The LdapMessage should not be empty" );
                        }

                        // First, create a empty LdapMessage Object
                        LdapMessage ldapMessage = new LdapMessage();

                        // Then stores it into the container
                        ldapMessageContainer.setLdapMessage( ldapMessage );
                        ldapMessageContainer.grammarEndAllowed( false );

                        return;
                    }
                } );

        //--------------------------------------------------------------------------------------------
        // LdapMessage Message ID
        //--------------------------------------------------------------------------------------------
        // LDAPMessage --> ... MessageId ...(Tag)
        // The tag must be 0x02. Nothing special to do.
        super.transitions[LdapStatesEnum.LDAP_MESSAGE_ID_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
                LdapStatesEnum.LDAP_MESSAGE_ID_TAG, LdapStatesEnum.LDAP_MESSAGE_ID_VALUE, null );

        // LDAPMessage --> ... MessageId ...(Value)
        // Checks that MessageId is in [0 .. 2147483647] and store the value in the LdapMessage Object
        // (2147483647 = Integer.MAX_VALUE)
        super.transitions[LdapStatesEnum.LDAP_MESSAGE_ID_VALUE][UniversalTag.INTEGER_TAG] = new GrammarTransition(
                LdapStatesEnum.LDAP_MESSAGE_ID_VALUE, LdapStatesEnum.PROTOCOL_OP_TAG,
                new GrammarAction( "Store MessageId" )
                {
                    public void action( IAsn1Container container ) throws DecoderException
                    {

                        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer )
                            container;
                        LdapMessage      ldapMessage          =
                            ldapMessageContainer.getLdapMessage();

                        // The current TLV should be a integer
                        // We get it and store it in MessageId
                        TLV   tlv       = ldapMessageContainer.getCurrentTLV();
                        
                        // The Length should not be null
                        if ( tlv.getLength().getLength() == 0 )
                        {
                        	log.error( "The messageId has a zero length. This is not allowed" );
                        	throw new DecoderException( "The messageId should not be null" );
                        }

                        Value value     = tlv.getValue();

                        try
                        {
                            int   messageId = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );
                            
                            ldapMessage.setMessageId( messageId );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Ldap Message Id has been decoded : " + messageId );
                            }
                        }
                        catch ( IntegerDecoderException ide )
                        {
                            log.error("The Message Id " + StringTools.dumpBytes( value.getData() ) + 
                                    " is invalid : " + ide.getMessage() + ". The message ID must be between (0 .. 2 147 483 647)" );
                        
                            throw new DecoderException( ide.getMessage() );
                        }

                        return;
                    }
                } );

        //********************************************************************************************
        // If the Tag is 0x42, then it's an UnBindRequest.
        // If the Tag is 0x4A, then it's a DelRequest.
        // If the Tag is 0x50, then it's an AbandonRequest.
        // If the Tag is 0x60, then it's a BindRequest.
        // If the Tag is 0x61, then it's a BindResponse.
        // If the Tag is 0x63, then it's a SearchRequest.
        // If the Tag is 0x64, then it's a SearchResultEntry.
        // If the Tag is 0x65, then it's a SearchResultDone
        // If the Tag is 0x66, then it's a ModifyRequest
        // If the Tag is 0x67, then it's a ModifyResponse.
        // If the Tag is 0x68, then it's an AddRequest.
        // If the Tag is 0x69, then it's an AddResponse.
        // If the Tag is 0x6B, then it's a DelResponse.
        // If the Tag is 0x6C, then it's a ModifyDNRequest.
        // If the Tag is 0x6D, then it's a ModifyDNResponse.
        // If the Tag is 0x6E, then it's a CompareRequest
        // If the Tag is 0x6F, then it's a CompareResponse.
        // If the Tag is 0x73, then it's a SearchResultReference.
        // If the Tag is 0x77, then it's an ExtendedRequest.
        // If the Tag is 0x78, then it's an ExtendedResponse.
        //********************************************************************************************

        //--------------------------------------------------------------------------------------------
        // UnBindRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... UnBindRequest ...
        // unbindRequest ::= [APPLICATION 2] NULL (Tag)
        // We have to switch to the UnBindRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.UNBIND_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.UNBIND_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // DelRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... DelRequest ...
        // delRequest ::= [APPLICATION 10] LDAPDN (Tag)
        // We have to switch to the DelRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.DEL_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.DEL_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // AbandonRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... AbandonRequest ...
        // AbandonRequest ::= [APPLICATION 16] MessageID (Tag)
        // We have to switch to the AbandonRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.ABANDON_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.ABANDON_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // BindRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... BindRequest ...
        // BindRequest ::= [APPLICATION 0] SEQUENCE { ... (Tag)
        // Nothing to do while the length is not checked.
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.BIND_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.BIND_REQUEST_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // BindResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... BindResponse ...
        // BindResponse ::= [APPLICATION 1] SEQUENCE { ... (Tag)
        // We have to switch to the BindResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.BIND_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.BIND_RESPONSE_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // SearchRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... SearchRequest ...
        // SearchRequest ::= [APPLICATION 3] SEQUENCE { ... (Tag)
        // Nothing to do while the length is not checked.
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.SEARCH_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.SEARCH_REQUEST_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // SearchResultEntry Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... SearchResultEntry ...
        // SearchResultEntry ::= [APPLICATION 4] SEQUENCE { ... (Tag)
        // Nothing to do while the length is not checked.
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.SEARCH_RESULT_ENTRY_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.SEARCH_RESULT_ENTRY_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // SearchResultDone Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... SearchResultDone ...
        // SearchResultDone ::= [APPLICATION 5] SEQUENCE { ... (Tag)
        // We have to switch to the SearchResultDone grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.SEARCH_RESULT_DONE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.SEARCH_RESULT_DONE_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ModifyRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ModifyRequest ...
        // ModifyRequest ::= [APPLICATION 6] SEQUENCE { ... (Tag)
        // We have to switch to the ModifyRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.MODIFY_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.MODIFY_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ModifydResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ModifyResponse ...
        // ModifyResponse ::= [APPLICATION 7] SEQUENCE { ... (Tag)
        // We have to switch to the ModifyResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.MODIFY_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.MODIFY_RESPONSE_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // AddRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... AddRequest ...
        // AddRequest ::= [APPLICATION 8] SEQUENCE { ... (Tag)
        // We have to switch to the AddRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.ADD_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.ADD_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // AddResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... AddResponse ...
        // AddResponse ::= [APPLICATION 9] LDAPResult (Tag)
        // We have to switch to the AddResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.ADD_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.ADD_RESPONSE_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // DelResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... DelResponse ...
        // DelResponse ::= [APPLICATION 11] LDAPResult (Tag)
        // We have to switch to the DelResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.DEL_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.DEL_RESPONSE_GRAMMAR_SWITCH, null );

        //--------------------------------------------------------------------------------------------
        // ModifydDNRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ModifyDNRequest ...
        // ModifyDNRequest ::= [APPLICATION 12] SEQUENCE { ... (Tag)
        // We have to switch to the ModifyDNRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.MODIFY_DN_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.MODIFY_DN_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ModifydResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ModifyDNResponse ...
        // ModifyDNResponse ::= [APPLICATION 13] SEQUENCE { ... (Tag)
        // We have to switch to the ModifyDNResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.MODIFY_DN_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.MODIFY_DN_RESPONSE_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // CompareResquest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... CompareRequest ...
        // CompareRequest ::= [APPLICATION 14] SEQUENCE {
        // We have to switch to the CompareRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.COMPARE_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.COMPARE_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // CompareResponse Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... CompareResponse ...
        // CompareResponse ::= [APPLICATION 15] LDAPResult (Tag)
        // We have to switch to the CompareResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.COMPARE_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.COMPARE_RESPONSE_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // SearchResultReference Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... SearchResultReference ...
        // SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL (Tag)
        // We have to switch to the SearchResultReference grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.SEARCH_RESULT_REFERENCE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.SEARCH_RESULT_REFERENCE_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ExtendedRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ExtendedRequest ...
        // ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
        // We have to switch to the ExtendedRequest grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.EXTENDED_REQUEST_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.EXTENDED_REQUEST_GRAMMAR_SWITCH,
                null );

        //--------------------------------------------------------------------------------------------
        // ExtendedRequest Message.
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... ExtendedResponse ...
        // ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
        // We have to switch to the ExtendedResponse grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.EXTENDED_RESPONSE_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.EXTENDED_RESPONSE_GRAMMAR_SWITCH,
                null );
        
        //--------------------------------------------------------------------------------------------
        // Controls
        //--------------------------------------------------------------------------------------------
        // LdapMessage ::= ... extendedResp    ExtendedResponse },
        //                 controls [0] Controls OPTIONAL }
        // 
        // We have to switch to the Controls grammar
        super.transitions[LdapStatesEnum.PROTOCOL_OP_TAG][LdapConstants.CONTROLS_TAG] = new GrammarTransition(
                LdapStatesEnum.PROTOCOL_OP_TAG, LdapStatesEnum.LDAP_CONTROL_GRAMMAR_SWITCH,
                null );
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
