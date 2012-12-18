/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.shared.kerberos.codec.encKrbCredPart;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.EncKrbCredPartInit;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreNonce;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreRecipientAddress;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreSenderAddress;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreTicketInfo;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreTimestamp;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions.StoreUsec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncKrbCredPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncKrbCredPartGrammar extends AbstractGrammar<EncKrbCredPartContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncKrbCredPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncKrbCredPartGrammar is a singleton */
    private static Grammar<EncKrbCredPartContainer> instance = new EncKrbCredPartGrammar();


    /**
     * Creates a new EncKrbCredPartGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncKrbCredPartGrammar()
    {
        setName( EncKrbCredPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncKrbCredPartStatesEnum.LAST_ENC_KRB_CRED_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncKrbCredPart
        // ============================================================================================
        super.transitions[EncKrbCredPartStatesEnum.START_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.START_STATE, EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_TAG,
                new EncKrbCredPartInit() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TAG_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SEQ_TAG_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SEQ_TAG_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_TICKET_INFO_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SEQ_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_TICKET_INFO_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_TAG_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                UniversalTag.SEQUENCE,
                new StoreTicketInfo() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_NONCE_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_NONCE_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE,
                UniversalTag.INTEGER,
                new StoreNonce() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_TIMESTAMP_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_TIMESTAMP_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreTimestamp() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_STATE,
                UniversalTag.INTEGER,
                new StoreUsec() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG,
                new StoreRecipientAddress() );

        // ---------------------------------- OPTIONAL transitions -----------------------------------------------
        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_TIMESTAMP_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_TIMESTAMP_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TICKET_INFO_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG,
                new StoreRecipientAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG,
                new CheckNotNullLength<EncKrbCredPartContainer>() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_NONCE_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG,
                new StoreRecipientAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_TIMESTAMP_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG,
                new StoreRecipientAddress() );

        super.transitions[EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_STATE.ordinal()][KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbCredPartContainer>(
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_USEC_STATE,
                EncKrbCredPartStatesEnum.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG,
                new StoreRecipientAddress() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncKrbCredPart Grammar
     */
    public static Grammar<EncKrbCredPartContainer> getInstance()
    {
        return instance;
    }
}
