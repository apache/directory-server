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
package org.apache.directory.shared.kerberos.codec.typedData;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.typedData.actions.StoreDataValue;
import org.apache.directory.shared.kerberos.codec.typedData.actions.StoreTdType;
import org.apache.directory.shared.kerberos.codec.typedData.actions.TypedDataInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the TypedData structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TypedDataGrammar extends AbstractGrammar<TypedDataContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( TypedDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. TypedDataGrammar is a singleton */
    private static Grammar<TypedDataContainer> instance = new TypedDataGrammar();


    /**
     * Creates a new TypedDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private TypedDataGrammar()
    {
        setName( TypedDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[TypedDataStatesEnum.LAST_TYPED_DATA_STATE.ordinal()][256];

        // ============================================================================================
        // TypedData
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from TypedData init to TypedData SEQ OF
        // --------------------------------------------------------------------------------------------
        // TypedData   ::= SEQUENCE OF
        super.transitions[TypedDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.START_STATE,
                TypedDataStatesEnum.TYPED_DATA_SEQ_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new TypedDataInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from TypedData SEQ OF to SEQ
        // --------------------------------------------------------------------------------------------
        // TypedData  ::= SEQUENCE OF SEQUENCE {
        super.transitions[TypedDataStatesEnum.TYPED_DATA_SEQ_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_SEQ_SEQ_STATE,
                TypedDataStatesEnum.TYPED_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<TypedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from TypedData SEQ OF to tdType tag
        // --------------------------------------------------------------------------------------------
        // TypedData  ::= SEQUENCE OF SEQUENCE {
        //         data-type     [0]
        super.transitions[TypedDataStatesEnum.TYPED_DATA_SEQ_STATE.ordinal()][KerberosConstants.TYPED_DATA_TDTYPE_TAG] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_SEQ_STATE,
                TypedDataStatesEnum.TYPED_DATA_TDTYPE_TAG_STATE,
                KerberosConstants.TYPED_DATA_TDTYPE_TAG,
                new CheckNotNullLength<TypedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from adtype tag to tdtype value
        // --------------------------------------------------------------------------------------------
        // TypedData  ::= SEQUENCE OF SEQUENCE {
        //         data-type     [0] Int32,
        super.transitions[TypedDataStatesEnum.TYPED_DATA_TDTYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_TDTYPE_TAG_STATE,
                TypedDataStatesEnum.TYPED_DATA_TDTYPE_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreTdType() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-type value to ad-data tag
        // --------------------------------------------------------------------------------------------
        // TypedData   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         data-value     [1]
        super.transitions[TypedDataStatesEnum.TYPED_DATA_TDTYPE_STATE.ordinal()][KerberosConstants.TYPED_DATA_TDDATA_TAG] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_TDTYPE_STATE,
                TypedDataStatesEnum.TYPED_DATA_TDDATA_TAG_STATE,
                KerberosConstants.TYPED_DATA_TDDATA_TAG,
                new CheckNotNullLength<TypedDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-data tag to ad-data value
        // --------------------------------------------------------------------------------------------
        // TypedData   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         data-value     [1] (OCTET STRING)
        super.transitions[TypedDataStatesEnum.TYPED_DATA_TDDATA_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_TDDATA_TAG_STATE,
                TypedDataStatesEnum.TYPED_DATA_TDDATA_STATE,
                UniversalTag.OCTET_STRING.getValue(),
                new StoreDataValue() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-data value to SEQUENCE
        // --------------------------------------------------------------------------------------------
        // TypedData   ::= SEQUENCE {
        //         ...
        //         data-value     [1] (OCTET STRING)
        super.transitions[TypedDataStatesEnum.TYPED_DATA_TDDATA_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<TypedDataContainer>(
                TypedDataStatesEnum.TYPED_DATA_TDDATA_STATE,
                TypedDataStatesEnum.TYPED_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<TypedDataContainer>() );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the TypedData Grammar
     */
    public static Grammar<TypedDataContainer> getInstance()
    {
        return instance;
    }
}
