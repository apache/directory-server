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
package org.apache.directory.shared.kerberos.codec.encryptionKey;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encryptionKey.actions.EncryptionKeyInit;
import org.apache.directory.shared.kerberos.codec.encryptionKey.actions.StoreKeyType;
import org.apache.directory.shared.kerberos.codec.encryptionKey.actions.StoreKeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncryptionKey structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncryptionKeyGrammar extends AbstractGrammar<EncryptionKeyContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncryptionKeyGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncryptionKeyGrammar is a singleton */
    private static Grammar<EncryptionKeyContainer> instance = new EncryptionKeyGrammar();


    /**
     * Creates a new EncryptionKeyGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncryptionKeyGrammar()
    {
        setName( EncryptionKeyGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncryptionKeyStatesEnum.LAST_ENCRYPTION_KEY_STATE.ordinal()][256];

        // ============================================================================================
        // EncryptionKey
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncryptionKey init to EncryptionKey SEQ OF
        // --------------------------------------------------------------------------------------------
        // EncryptionKey         ::= SEQUENCE {
        super.transitions[EncryptionKeyStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<EncryptionKeyContainer>(
                EncryptionKeyStatesEnum.START_STATE,
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new EncryptionKeyInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from EncryptionKey SEQ to key-type tag
        // --------------------------------------------------------------------------------------------
        // EncryptionKey         ::= SEQUENCE {
        //       keytype     [0]
        super.transitions[EncryptionKeyStatesEnum.ENCRYPTION_KEY_SEQ_STATE.ordinal()][KerberosConstants.ENCRYPTION_KEY_TYPE_TAG] =
            new GrammarTransition<EncryptionKeyContainer>(
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_SEQ_STATE,
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_TAG_STATE,
                KerberosConstants.ENCRYPTION_KEY_TYPE_TAG,
                new CheckNotNullLength<EncryptionKeyContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from EncryptionKey type tag to key-type value
        // --------------------------------------------------------------------------------------------
        // EncryptionKey         ::= SEQUENCE {
        //       keytype     [0] Int32
        super.transitions[EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<EncryptionKeyContainer>(
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_TAG_STATE,
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_STATE,
                UniversalTag.INTEGER,
                new StoreKeyType() );

        // --------------------------------------------------------------------------------------------
        // Transition from key-type to key-value tag
        // --------------------------------------------------------------------------------------------
        // EncryptionKey         ::= SEQUENCE {
        //          ...
        //          keyvalue    [2]
        super.transitions[EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_STATE.ordinal()][KerberosConstants.ENCRYPTION_KEY_VALUE_TAG] =
            new GrammarTransition<EncryptionKeyContainer>(
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_TYPE_STATE,
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_VALUE_TAG_STATE,
                KerberosConstants.ENCRYPTION_KEY_VALUE_TAG,
                new CheckNotNullLength<EncryptionKeyContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from key-value tag to key-value value
        // --------------------------------------------------------------------------------------------
        // EncryptionKey         ::= SEQUENCE {
        //          keyvalue    [2] OCTET STRING
        super.transitions[EncryptionKeyStatesEnum.ENCRYPTION_KEY_VALUE_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<EncryptionKeyContainer>(
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_VALUE_TAG_STATE,
                EncryptionKeyStatesEnum.ENCRYPTION_KEY_VALUE_STATE,
                UniversalTag.OCTET_STRING,
                new StoreKeyValue() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncryptionKey Grammar
     */
    public static Grammar<EncryptionKeyContainer> getInstance()
    {
        return instance;
    }
}
