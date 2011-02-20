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
package org.apache.directory.shared.kerberos.codec.etypeInfo2Entry;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions.ETypeInfo2EntryInit;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions.StoreEType;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions.StoreS2KParams;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions.StoreSalt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ETYPE-INFO2-ENTRY structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ETypeInfo2EntryGrammar extends AbstractGrammar<ETypeInfo2EntryContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ETypeInfo2EntryGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ETypeInfo2EntryGrammar is a singleton */
    private static Grammar<ETypeInfo2EntryContainer> instance = new ETypeInfo2EntryGrammar();


    /**
     * Creates a new ETypeInfoEntryGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ETypeInfo2EntryGrammar()
    {
        setName( ETypeInfo2EntryGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ETypeInfo2EntryStatesEnum.LAST_ETYPE_INFO2_ENTRY_STATE.ordinal()][256];

        // ============================================================================================
        // ETYPE-INFO2-ENTRY
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO2-ENTRY init to ETYPE-INFO2-ENTRY SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        super.transitions[ETypeInfo2EntryStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.START_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new ETypeInfo2EntryInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO2-ENTRY SEQ to etype tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //         etype           [0]
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SEQ_STATE.ordinal()][KerberosConstants.ETYPE_INFO2_ENTRY_ETYPE_TAG] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SEQ_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_TAG_STATE,
                KerberosConstants.ETYPE_INFO2_ENTRY_ETYPE_TAG,
                new CheckNotNullLength<ETypeInfo2EntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype tag to etype value
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //         etype           [0] Int32,
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_TAG_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_STATE,
                UniversalTag.INTEGER,
                new StoreEType() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype value to salt tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //        ...
        //         salt            [1]
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_STATE.ordinal()][KerberosConstants.ETYPE_INFO2_ENTRY_SALT_TAG] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_TAG_STATE,
                KerberosConstants.ETYPE_INFO2_ENTRY_SALT_TAG,
                new CheckNotNullLength<ETypeInfo2EntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from etype value to s2kparams tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //        ...
        //         s2kparams       [2]
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_STATE.ordinal()][KerberosConstants.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_ETYPE_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG_STATE,
                KerberosConstants.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG,
                new CheckNotNullLength<ETypeInfo2EntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from salt tag to salt value
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //        ...
        //         salt            [1] KerberosString OPTIONAL,
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_TAG_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_STATE,
                UniversalTag.GENERAL_STRING,
                new StoreSalt() );

        // --------------------------------------------------------------------------------------------
        // Transition from salt value to s2kparams tag
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //        ...
        //         s2kparams       [2]
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_STATE.ordinal()][KerberosConstants.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_SALT_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG_STATE,
                KerberosConstants.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG,
                new CheckNotNullLength<ETypeInfo2EntryContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from s2kparams tag to s2kparams value
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE {
        //        ...
        //         s2kparams       [2] OCTET STRING OPTIONAL
        super.transitions[ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] =
            new GrammarTransition<ETypeInfo2EntryContainer>(
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG_STATE,
                ETypeInfo2EntryStatesEnum.ETYPE_INFO2_ENTRY_S2KPARAMS_STATE,
                UniversalTag.OCTET_STRING,
                new StoreS2KParams() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the ETYPE-INFO2-ENTRY Grammar
     */
    public static Grammar<ETypeInfo2EntryContainer> getInstance()
    {
        return instance;
    }
}
