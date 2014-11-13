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
package org.apache.directory.shared.kerberos.codec.transitedEncoding;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.actions.StoreContents;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.actions.StoreTrType;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.actions.TransitedEncodingInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the TransitedEncoding structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TransitedEncodingGrammar extends AbstractGrammar<TransitedEncodingContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( TransitedEncodingGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. TransitedEncodingGrammar is a singleton */
    private static Grammar<TransitedEncodingContainer> instance = new TransitedEncodingGrammar();


    /**
     * Creates a new TransitedEncodingGrammar object.
     */
    @SuppressWarnings("unchecked")
    private TransitedEncodingGrammar()
    {
        setName( TransitedEncodingGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[TransitedEncodingStatesEnum.LAST_TRANSITED_ENCODING_STATE.ordinal()][256];

        // ============================================================================================
        // TransitedEncoding
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from TransitedEncoding init to TransitedEncoding SEQ
        // --------------------------------------------------------------------------------------------
        // TransitedEncoding   ::= SEQUENCE {
        super.transitions[TransitedEncodingStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<TransitedEncodingContainer>(
                TransitedEncodingStatesEnum.START_STATE,
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new TransitedEncodingInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from TransitedEncoding SEQ to tr-type tag
        // --------------------------------------------------------------------------------------------
        // TransitedEncoding   ::= SEQUENCE {
        //         tr-type         [0]
        super.transitions[TransitedEncodingStatesEnum.TRANSITED_ENCODING_SEQ_STATE.ordinal()][KerberosConstants.TRANSITED_ENCODING_TR_TYPE_TAG] =
            new GrammarTransition<TransitedEncodingContainer>(
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_SEQ_STATE,
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_TAG_STATE,
                KerberosConstants.TRANSITED_ENCODING_TR_TYPE_TAG,
                new CheckNotNullLength<TransitedEncodingContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from tr-type tag to tr-type value
        // --------------------------------------------------------------------------------------------
        // TransitedEncoding   ::= SEQUENCE {
        //         tr-type         [0] Int32 -- must be registered --,
        super.transitions[TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<TransitedEncodingContainer>(
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_TAG_STATE,
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_STATE,
                UniversalTag.INTEGER,
                new StoreTrType() );

        // --------------------------------------------------------------------------------------------
        // Transition from tr-type value to contents tag
        // --------------------------------------------------------------------------------------------
        // TransitedEncoding   ::= SEQUENCE {
        //         ...
        //         contents        [1]
        super.transitions[TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_STATE.ordinal()][KerberosConstants.TRANSITED_ENCODING_CONTENTS_TAG] =
            new GrammarTransition<TransitedEncodingContainer>(
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_TR_TYPE_STATE,
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_CONTENTS_TAG_STATE,
                KerberosConstants.TRANSITED_ENCODING_CONTENTS_TAG,
                new CheckNotNullLength<TransitedEncodingContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from contents tag to contents value
        // --------------------------------------------------------------------------------------------
        // TransitedEncoding   ::= SEQUENCE {
        //         ...
        //         contents        [1] OCTET STRING
        super.transitions[TransitedEncodingStatesEnum.TRANSITED_ENCODING_CONTENTS_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<TransitedEncodingContainer>(
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_CONTENTS_TAG_STATE,
                TransitedEncodingStatesEnum.TRANSITED_ENCODING_CONTENTS_STATE,
                UniversalTag.OCTET_STRING,
                new StoreContents() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the TransitedEncoding Grammar
     */
    public static Grammar<TransitedEncodingContainer> getInstance()
    {
        return instance;
    }
}
