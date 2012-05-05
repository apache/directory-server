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
package org.apache.directory.shared.kerberos.codec.encKrbPrivPart;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.EncKrbPrivPartInit;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreRecipientAddress;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreSenderAddress;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreSeqNumber;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreTimestamp;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreUsec;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.actions.StoreUserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncKrbPrivPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncKrbPrivPartGrammar extends AbstractGrammar<EncKrbPrivPartContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncKrbPrivPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncKrbPrivPartGrammar is a singleton */
    private static Grammar<EncKrbPrivPartContainer> instance = new EncKrbPrivPartGrammar();


    /**
     * Creates a new EncKrbPrivPartGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncKrbPrivPartGrammar()
    {
        setName( EncKrbPrivPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncKrbPrivPartStatesEnum.LAST_ENC_KRB_PRIV_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncKrbPrivPart
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncKrbPrivPart init to EncKrbPrivPart tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart  ::= [APPLICATION 28] EncKrbPrivPart   ::= SEQUENCE {
        super.transitions[EncKrbPrivPartStatesEnum.START_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.START_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_TAG,
                new EncKrbPrivPartInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from EncKrbPrivPart tag to EncKrbPrivPart seq
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart  ::= [APPLICATION 28] SEQUENCE {
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TAG_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_TAG_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from EncKrbPrivPart seq to user-data tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // user-data       [0]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_TAG_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_USER_DATA_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_USER_DATA_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from user-data tag to user-data value
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // user-data       [0] OCTET STRING
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE,
                UniversalTag.OCTET_STRING,
                new StoreUserData() );

        // --------------------------------------------------------------------------------------------
        // Transition from user-data value to timestamp tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // timestamp       [1]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_TIMESTAMP_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_TIMESTAMP_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from timestamp tag to timestamp value
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // timestamp       [1] KerberosTime OPTIONAL
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreTimestamp() );

        // --------------------------------------------------------------------------------------------
        // Transition from timestamp value to usec tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // usec            [2]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_USEC_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_TIMESTAMP_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_USEC_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from usec tag to usec value
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // usec            [2] Microseconds OPTIONAL
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_STATE,
                UniversalTag.INTEGER,
                new StoreUsec() );

        // --------------------------------------------------------------------------------------------
        // Transition from usec value to seq-number tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // seq-number      [3]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from seq-number tag to seq-number value
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // seq-number      [3] UInt32 OPTIONAL
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_STATE,
                UniversalTag.INTEGER,
                new StoreSeqNumber() );

        // --------------------------------------------------------------------------------------------
        // Transition from seq-number to s-address tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // s-address       [4] HostAddress
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        // --------------------------------------------------------------------------------------------
        // Transition from s-address tag to r-address tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // r-address       [5] HostAddress
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_RECIPIENT_ADDRESS_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_RECIPIENT_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_RECIPIENT_ADDRESS_TAG,
                new StoreRecipientAddress() );

        //----------------------------- OPTIONAL transitions ---------------------------

        // --------------------------------------------------------------------------------------------
        // Transition from user-data value to usec tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // usec       [2]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_USEC_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_USEC_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from user-data value to seq-number tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // seq-number       [3]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_SEQ_NUMBER_TAG,
                new CheckNotNullLength<EncKrbPrivPartContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from user-data value to s-address tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // s-address       [4]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USER_DATA_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );

        // --------------------------------------------------------------------------------------------
        // Transition from usec value to s-address tag
        // --------------------------------------------------------------------------------------------
        // EncKrbPrivPart   ::= SEQUENCE {
        // s-address       [4]
        super.transitions[EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_STATE.ordinal()][KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG] =
            new GrammarTransition<EncKrbPrivPartContainer>(
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_USEC_STATE,
                EncKrbPrivPartStatesEnum.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG_STATE,
                KerberosConstants.ENC_KRB_PRIV_PART_SENDER_ADDRESS_TAG,
                new StoreSenderAddress() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncKrbPrivPart Grammar
     */
    public static Grammar<EncKrbPrivPartContainer> getInstance()
    {
        return instance;
    }
}
