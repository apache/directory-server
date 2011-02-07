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
package org.apache.directory.shared.kerberos.codec.etypeInfoEntry;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.actions.ETypeInfoEntryInit;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.actions.StoreEType;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.actions.StoreSalt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ETYPE-INFO-ENTRY structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ETypeInfoEntryGrammar extends AbstractGrammar<ETypeInfoEntryContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ETypeInfoEntryGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ETypeInfoEntryGrammar is a singleton */
    private static Grammar<ETypeInfoEntryContainer> instance = new ETypeInfoEntryGrammar();


    /**
     * Creates a new ETypeInfoEntryGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ETypeInfoEntryGrammar()
    {
        setName( ETypeInfoEntryGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ETypeInfoEntryStatesEnum.LAST_ETYPE_INFO_ENTRY_STATE.ordinal()][256];

        // ============================================================================================
        // ETYPE-INFO-ENTRY
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO-ENTRY init to ETYPE-INFO-ENTRY SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE {
        super.transitions[ETypeInfoEntryStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfoEntryContainer>(
                ETypeInfoEntryStatesEnum.START_STATE,
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new ETypeInfoEntryInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO-ENTRY SEQ to etype tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE {
        //         etype           [0]
        super.transitions[ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SEQ_STATE.ordinal()][KerberosConstants.TRANSITED_ENCODING_TR_TYPE_TAG] =
            new GrammarTransition<ETypeInfoEntryContainer>(
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SEQ_STATE,
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_TAG_STATE,
                KerberosConstants.TRANSITED_ENCODING_TR_TYPE_TAG,
                new CheckNotNullLength<ETypeInfoEntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype tag to etype value
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE {
        //         etype           [0] Int32,
        super.transitions[ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<ETypeInfoEntryContainer>(
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_TAG_STATE,
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreEType() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype value to salt tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE {
        //         ...
        //         salt            [1]
        super.transitions[ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_STATE.ordinal()][KerberosConstants.TRANSITED_ENCODING_CONTENTS_TAG] =
            new GrammarTransition<ETypeInfoEntryContainer>(
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_ETYPE_STATE,
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SALT_TAG_STATE,
                KerberosConstants.TRANSITED_ENCODING_CONTENTS_TAG,
                new CheckNotNullLength<ETypeInfoEntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from salt tag to salt value
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE {
        //         ...
        //         salt            [1] OCTET STRING OPTIONAL
        super.transitions[ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SALT_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<ETypeInfoEntryContainer>(
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SALT_TAG_STATE,
                ETypeInfoEntryStatesEnum.ETYPE_INFO_ENTRY_SALT_STATE,
                UniversalTag.OCTET_STRING.getValue(),
                new StoreSalt() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the ETYPE-INFO-ENTRY Grammar
     */
    public static Grammar<ETypeInfoEntryContainer> getInstance()
    {
        return instance;
    }
}
