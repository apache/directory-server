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
package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Graceful Disconnect. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once.
 * The grammar is :
 * 
 * <pre>
 *  GracefulDisconnect ::= SEQUENCE {
 *                         timeOffline INTEGER (0..720) DEFAULT 0,
 *                         delay [0] INTEGER (0..86400) DEFAULT 0,
 *                         replicatedContexts Referral OPTIONAL
 *              }
 *  
 *  Referral ::= SEQUENCE OF LDAPURL
 *  
 *  LDAPURL ::= LDAPString -- limited to characters permitted in URLs
 *  
 *  LDAPString ::= OCTET STRING
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulDisconnectGrammar extends AbstractGrammar implements IGrammar
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( GracefulDisconnectGrammar.class );

    /** The instance of grammar. GracefulDisconnectnGrammar is a singleton */
    private static IGrammar instance = new GracefulDisconnectGrammar();


    /**
     * Creates a new GracefulDisconnectGrammar object.
     */
    private GracefulDisconnectGrammar()
    {
        name = GracefulDisconnectGrammar.class.getName();
        statesEnum = GracefulDisconnectStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[GracefulDisconnectStatesEnum.LAST_GRACEFUL_DISCONNECT_STATE][256];

        /**
         * GracefulDisconnect ::= SEQUENCE { (Tag) ... Nothing to do...
         */
        super.transitions[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_TAG,
            GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { (Value) ... Creates the
         * GracefulDisconnect object
         */
        super.transitions[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_VALUE,
            GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG, new GrammarAction(
                "Init Graceful Disconnect" )
            {
                public void action( IAsn1Container container )
                {
                    GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
                    GracefulDisconnect gracefulDisconnect = new GracefulDisconnect();
                    gracefulDisconnectContainer.setGracefulDisconnect( gracefulDisconnect );
                    gracefulDisconnectContainer.grammarEndAllowed( true );
                }
            } );

        /**
         * GracefulDisconnect ::= SEQUENCE { timeOffline INTEGER (0..720)
         * DEFAULT 0, (Tag) ... Nothing to do
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG,
            GracefulDisconnectStatesEnum.TIME_OFFLINE_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... delay [0] INTEGER (0..86400)
         * DEFAULT 0, (Tag) ... We have no TimeOffline. Nothing to do.
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG][GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG,
            GracefulDisconnectStatesEnum.DELAY_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... replicatedContexts Referral
         * OPTIONAL } Referral ::= SEQUENCE OF LDAPURL (Tag) We have no
         * TimeOffline nor delay, just a replicatedContexts. Nothing to do.
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG,
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { timeOffline INTEGER (0..720)
         * DEFAULT 0, (Value) ... Set the time offline value into the
         * GracefulDisconnect object.
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_VALUE][UniversalTag.INTEGER_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.TIME_OFFLINE_VALUE,
            GracefulDisconnectStatesEnum.DELAY_OR_REPLICATED_OR_END_TAG, new GrammarAction(
                "Set Graceful Disconnect time offline" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
                    Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();

                    try
                    {
                        int timeOffline = IntegerDecoder.parse( value, 0, 720 );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Time Offline = " + timeOffline );
                        }

                        gracefulDisconnectContainer.getGracefulDisconnect().setTimeOffline( timeOffline );
                        gracefulDisconnectContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = "failed to decode the timeOffline, the value should be between 0 and 720 minutes, it is '"
                            + StringTools.dumpBytes( value.getData() ) + "'";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... delay [0] INTEGER (0..86400)
         * DEFAULT 0, (Tag) ... We have had a TimeOffline, and now we are
         * reading the delay. Nothing to do.
         */
        super.transitions[GracefulDisconnectStatesEnum.DELAY_OR_REPLICATED_OR_END_TAG][GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.DELAY_OR_REPLICATED_OR_END_TAG, GracefulDisconnectStatesEnum.DELAY_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... delay [0] INTEGER (0..86400)
         * DEFAULT 0, (Value) ... Set the delay value into the
         * GracefulDisconnect object.
         */
        super.transitions[GracefulDisconnectStatesEnum.DELAY_VALUE][GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.DELAY_VALUE, GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_OR_END_TAG,
            new GrammarAction( "Set Graceful Disconnect Delay" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
                    Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();

                    try
                    {
                        int delay = IntegerDecoder.parse( value, 0, 86400 );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Delay = " + delay );
                        }

                        gracefulDisconnectContainer.getGracefulDisconnect().setDelay( delay );
                        gracefulDisconnectContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = "failed to decode the delay, the value should be between 0 and 86400 seconds, it is '"
                            + StringTools.dumpBytes( value.getData() ) + "'";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... replicatedContexts Referral
         * OPTIONAL } Referral ::= SEQUENCE OF LDAPURL (Tag) We have a referral
         * sequence. Nothing to do.
         */
        super.transitions[GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_OR_END_TAG][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_OR_END_TAG,
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... replicatedContexts Referral
         * OPTIONAL } Referral ::= SEQUENCE OF LDAPURL (Value) We have a
         * sequence, so we will have Octet String following. Nothing to do.
         */
        super.transitions[GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_VALUE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_VALUE,
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_OR_END_TAG, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... replicatedContexts Referral
         * OPTIONAL } Referral ::= SEQUENCE OF LDAPURL LDAPURL ::= LDAPString
         * (Tag) We have a referral. It can be the first one, or one of the
         * following. Nothing to do
         */
        super.transitions[GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_OR_END_TAG][UniversalTag.OCTET_STRING] = new GrammarTransition(
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_OR_END_TAG,
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_VALUE, null );

        /**
         * GracefulDisconnect ::= SEQUENCE { ... replicatedContexts Referral
         * OPTIONAL } Referral ::= SEQUENCE OF LDAPURL (Tag) LDAPURL ::=
         * LDAPString (Tag) Read and store a referral url.
         */
        super.transitions[GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_VALUE][UniversalTag.OCTET_STRING] = new GrammarTransition(
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_VALUE,
            GracefulDisconnectStatesEnum.REPLICATED_CONTEXT_OR_END_TAG, new GrammarAction( "Replicated context URL" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
                    Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();

                    try
                    {
                        LdapURL url = new LdapURL( value.getData() );
                        gracefulDisconnectContainer.getGracefulDisconnect().addReplicatedContexts( url );
                        gracefulDisconnectContainer.grammarEndAllowed( true );
                    }
                    catch ( LdapURLEncodingException e )
                    {
                        String msg = "failed to decode the URL '" + StringTools.dumpBytes( value.getData() ) + "'";
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );
    }


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
