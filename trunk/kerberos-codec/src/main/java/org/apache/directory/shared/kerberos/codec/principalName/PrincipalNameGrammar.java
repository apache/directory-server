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
package org.apache.directory.shared.kerberos.codec.principalName;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.principalName.actions.PrincipalNameInit;
import org.apache.directory.shared.kerberos.codec.principalName.actions.StoreNameString;
import org.apache.directory.shared.kerberos.codec.principalName.actions.StoreNameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the PrincipalName. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class PrincipalNameGrammar extends AbstractGrammar<PrincipalNameContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( PrincipalNameGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. PrincipalNameGrammar is a singleton */
    private static Grammar<PrincipalNameContainer> instance = new PrincipalNameGrammar();


    /**
     * Creates a new PrincipalNameGrammar object.
     */
    @SuppressWarnings("unchecked")
    private PrincipalNameGrammar()
    {
        setName( PrincipalNameGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[PrincipalNameStatesEnum.LAST_PRINCIPAL_NAME_STATE.ordinal()][256];

        // ============================================================================================
        // PrincipalName
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from PrincipalName init to PrincipalName SEQ
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE
        super.transitions[PrincipalNameStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.START_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new PrincipalNameInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from PrincipalName SEQ to name-type tag
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE {
        //         name-type       [0] Int32,
        super.transitions[PrincipalNameStatesEnum.PRINCIPAL_NAME_SEQ_STATE.ordinal()][KerberosConstants.PRINCIPAL_NAME_NAME_TYPE_TAG] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.PRINCIPAL_NAME_SEQ_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_TAG_STATE,
                KerberosConstants.PRINCIPAL_NAME_NAME_TYPE_TAG,
                new CheckNotNullLength<PrincipalNameContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from name-type tag to name-type value
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE {
        //         name-type       [0] Int32,
        super.transitions[PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_TAG_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_STATE,
                UniversalTag.INTEGER,
                new StoreNameType() );

        // --------------------------------------------------------------------------------------------
        // Transition from name-type value to name-string tag
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE {
        //         name-type       [0] Int32,
        //         name-string     [1]
        super.transitions[PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_STATE.ordinal()][KerberosConstants.PRINCIPAL_NAME_NAME_STRING_TAG] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_TYPE_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_TAG_STATE,
                KerberosConstants.PRINCIPAL_NAME_NAME_STRING_TAG,
                new CheckNotNullLength<PrincipalNameContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from name-string tag to name-string SEQ
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE {
        //         name-type       [0] Int32,
        //         name-string     [1] SEQUENCE OF
        super.transitions[PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_TAG_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_TAG_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<PrincipalNameContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from name-string SEQ to name-string value
        // --------------------------------------------------------------------------------------------
        // PrincipalName   ::= SEQUENCE {
        //         name-type       [0] Int32,
        //         name-string     [1] SEQUENCE OF KerberosString
        super.transitions[PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_SEQ_STATE.ordinal()][UniversalTag.GENERAL_STRING
            .getValue()] =
            new GrammarTransition<PrincipalNameContainer>(
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_SEQ_STATE,
                PrincipalNameStatesEnum.PRINCIPAL_NAME_NAME_STRING_SEQ_STATE,
                UniversalTag.GENERAL_STRING,
                new StoreNameString() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the PrincipalName Grammar
     */
    public static Grammar<PrincipalNameContainer> getInstance()
    {
        return instance;
    }
}
