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
package org.apache.directory.shared.kerberos.codec.encryptedData;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encryptedData.actions.EncryptedDataInit;
import org.apache.directory.shared.kerberos.codec.encryptedData.actions.StoreCipher;
import org.apache.directory.shared.kerberos.codec.encryptedData.actions.StoreEType;
import org.apache.directory.shared.kerberos.codec.encryptedData.actions.StoreKvno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncryptedData structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncryptedDataGrammar extends AbstractGrammar<EncryptedDataContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncryptedDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncryptedDataGrammar is a singleton */
    private static Grammar<EncryptedDataContainer> instance = new EncryptedDataGrammar();


    /**
     * Creates a new EncryptedDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncryptedDataGrammar()
    {
        setName( EncryptedDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncryptedDataStatesEnum.LAST_ENCRYPTED_DATA_STATE.ordinal()][256];

        // ============================================================================================
        // EncryptedData
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncryptedData init to EncryptedData SEQ
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE
        super.transitions[EncryptedDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.START_STATE, EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new EncryptedDataInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from EncryptedData SEQ to etype tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         etype       [0]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_ETYPE_TAG] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_SEQ_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_ETYPE_TAG,
                new CheckNotNullLength<EncryptedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype tag to etype value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         etype       [0] Int32,
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_TAG_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE, UniversalTag.INTEGER.getValue(),
                new StoreEType() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype value to kvno tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         kvno     [1]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_KVNO_TAG] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_KVNO_TAG,
                new CheckNotNullLength<EncryptedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype value to cipher tag (kvno is missing)
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_ETYPE_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG,
                new CheckNotNullLength<EncryptedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from kvno tag to kvno value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         kvno     [1] UInt32
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_TAG_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE, UniversalTag.INTEGER.getValue(),
                new StoreKvno() );

        // --------------------------------------------------------------------------------------------
        // Transition from kvno value value to cipher tag
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2]
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE.ordinal()][KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_KVNO_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE, KerberosConstants.ENCRYPTED_DATA_CIPHER_TAG,
                new CheckNotNullLength<EncryptedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from cipher tag to cipher value
        // --------------------------------------------------------------------------------------------
        // EncryptedData   ::= SEQUENCE {
        //         ...
        //         cipher     [2] OCTET STRING
        super.transitions[EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<EncryptedDataContainer>(
                EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_TAG_STATE,
                EncryptedDataStatesEnum.ENCRYPTED_DATA_CIPHER_STATE, UniversalTag.OCTET_STRING.getValue(),
                new StoreCipher() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncryptedData Grammar
     */
    public static Grammar<EncryptedDataContainer> getInstance()
    {
        return instance;
    }
}
