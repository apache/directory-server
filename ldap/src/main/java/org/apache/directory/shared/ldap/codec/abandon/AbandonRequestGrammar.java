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
package org.apache.directory.shared.ldap.codec.abandon;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapStatesEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AbandonRequest LDAP message. All the actions are
 * declared in this class. As it is a singleton, these declaration are only done
 * once. If an action is to be added or modified, this is where the work is to
 * be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbandonRequestGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AbandonRequestGrammar.class );

    /** The instance of grammar. AbandonRequestGrammar is a singleton */
    private static IGrammar instance = new AbandonRequestGrammar();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AbandonRequestGrammar object.
     */
    private AbandonRequestGrammar()
    {

        name = AbandonRequestGrammar.class.getName();

        statesEnum = LdapStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[LdapStatesEnum.LAST_ABANDON_REQUEST_STATE][256];

        // --------------------------------------------------------------------------------------------
        // AbandonRequest Message ID
        // --------------------------------------------------------------------------------------------
        // AbandonRequest ::= [APPLICATION 16] MessageID (Tag)
        // The tag must be 0x50. It codes for an integer, as we use
        // IMPLICIT tags (we won't have a 0x02 tag to code the integer).
        // Nothing special to do.
        super.transitions[LdapStatesEnum.ABANDON_REQUEST_MESSAGE_ID_TAG][LdapConstants.ABANDON_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.ABANDON_REQUEST_MESSAGE_ID_TAG, LdapStatesEnum.ABANDON_REQUEST_MESSAGE_ID_VALUE, null );

        // AbandonRequest ::= [APPLICATION 16] MessageID (Value)
        // (2147483647 = Integer.MAX_VALUE)
        // Checks that MessageId is in [0 .. 2147483647] and store the value in
        // the AbandonRequest Object
        // This is the last state.
        super.transitions[LdapStatesEnum.ABANDON_REQUEST_MESSAGE_ID_VALUE][LdapConstants.ABANDON_REQUEST_TAG] = new GrammarTransition(
            LdapStatesEnum.ABANDON_REQUEST_MESSAGE_ID_VALUE, LdapStatesEnum.GRAMMAR_END, new GrammarAction(
                "Store MessageId" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {

                    LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
                    LdapMessage ldapMessage = ldapMessageContainer.getLdapMessage();

                    // The current TLV should be a integer
                    // We get it and store it in MessageId
                    TLV tlv = ldapMessageContainer.getCurrentTLV();

                    Value value = tlv.getValue();

                    if ( ( value == null ) || ( value.getData() == null ) )
                    {
                        String msg = "The AbandonRequest messageId must not be null";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }

                    try
                    {
                        int abandonnedMessageId = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );

                        // Ok, the Message ID is correct. We have to store it
                        // in the AbandonRequest object
                        // Object in the LDAPMessage
                        AbandonRequest abandonRequest = new AbandonRequest();
                        abandonRequest.setAbandonedMessageId( abandonnedMessageId );
                        ldapMessage.setProtocolOP( abandonRequest );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "AbandonMessage Id has been decoded : {}", new Integer( abandonnedMessageId ) );
                        }

                        ldapMessageContainer.grammarEndAllowed( true );
                        ldapMessageContainer.grammarPopAllowed( true );
                        return;
                    }
                    catch ( IntegerDecoderException ide )
                    {
                        log
                            .error(
                                "The Abandonned Message Id {} is invalid : {}. The message ID must be between (0 .. 2 147 483 647)",
                                StringTools.dumpBytes( value.getData() ), ide.getMessage() );

                        throw new DecoderException( ide.getMessage() );
                    }
                }
            } );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the AbandonRequest Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
