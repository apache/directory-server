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
package org.apache.directory.shared.kerberos.codec.padata;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.padata.actions.PaDataInit;
import org.apache.directory.shared.kerberos.codec.padata.actions.StoreDataType;
import org.apache.directory.shared.kerberos.codec.padata.actions.StorePaDataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the PaData structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class PaDataGrammar extends AbstractGrammar<PaDataContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( PaDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. PaDataGrammar is a singleton */
    private static Grammar<PaDataContainer> instance = new PaDataGrammar();


    /**
     * Creates a new PaDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private PaDataGrammar()
    {
        setName( PaDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[PaDataStatesEnum.LAST_PADATA_STATE.ordinal()][256];

        // ============================================================================================
        // PaData
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from PaData init to PaData SEQ OF
        // --------------------------------------------------------------------------------------------
        // PA-DATA         ::= SEQUENCE {
        super.transitions[PaDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<PaDataContainer>(
                PaDataStatesEnum.START_STATE,
                PaDataStatesEnum.PADATA_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new PaDataInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from PaData SEQ to padata-type tag
        // --------------------------------------------------------------------------------------------
        // PA-DATA         ::= SEQUENCE {
        //       padata-type     [1]
        super.transitions[PaDataStatesEnum.PADATA_SEQ_STATE.ordinal()][KerberosConstants.PADATA_TYPE_TAG] =
            new GrammarTransition<PaDataContainer>(
                PaDataStatesEnum.PADATA_SEQ_STATE,
                PaDataStatesEnum.PADATA_TYPE_TAG_STATE,
                KerberosConstants.PADATA_TYPE_TAG,
                new CheckNotNullLength<PaDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from PaData type tag to padata-type
        // --------------------------------------------------------------------------------------------
        // PA-DATA         ::= SEQUENCE {
        //       padata-type     [1] Int32
        super.transitions[PaDataStatesEnum.PADATA_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<PaDataContainer>(
                PaDataStatesEnum.PADATA_TYPE_TAG_STATE,
                PaDataStatesEnum.PADATA_TYPE_STATE,
                UniversalTag.INTEGER,
                new StoreDataType() );

        // --------------------------------------------------------------------------------------------
        // Transition from padata-type to padata-value tag
        // --------------------------------------------------------------------------------------------
        // PA-DATA         ::= SEQUENCE {
        //          padata-value    [2]
        super.transitions[PaDataStatesEnum.PADATA_TYPE_STATE.ordinal()][KerberosConstants.PADATA_VALUE_TAG] =
            new GrammarTransition<PaDataContainer>(
                PaDataStatesEnum.PADATA_TYPE_STATE,
                PaDataStatesEnum.PADATA_VALUE_TAG_STATE,
                KerberosConstants.PADATA_VALUE_TAG,
                new CheckNotNullLength<PaDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from padata-value tag to padata-value
        // --------------------------------------------------------------------------------------------
        // PA-DATA         ::= SEQUENCE {
        //          padata-value    [2] OCTET STRING
        super.transitions[PaDataStatesEnum.PADATA_VALUE_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] =
            new GrammarTransition<PaDataContainer>(
                PaDataStatesEnum.PADATA_VALUE_TAG_STATE,
                PaDataStatesEnum.PADATA_VALUE_STATE,
                UniversalTag.OCTET_STRING,
                new StorePaDataValue() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the PaData Grammar
     */
    public static Grammar<PaDataContainer> getInstance()
    {
        return instance;
    }
}
