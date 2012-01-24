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
package org.apache.directory.shared.kerberos.codec.authorizationData;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.authorizationData.actions.AuthorizationDataInit;
import org.apache.directory.shared.kerberos.codec.authorizationData.actions.StoreAdData;
import org.apache.directory.shared.kerberos.codec.authorizationData.actions.StoreAdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AuthorizationData structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AuthorizationDataGrammar extends AbstractGrammar<AuthorizationDataContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AuthorizationDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AuthorizationDataGrammar is a singleton */
    private static Grammar<AuthorizationDataContainer> instance = new AuthorizationDataGrammar();


    /**
     * Creates a new AuthorizationDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private AuthorizationDataGrammar()
    {
        setName( AuthorizationDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AuthorizationDataStatesEnum.LAST_AUTHORIZATION_DATA_STATE.ordinal()][256];

        // ============================================================================================
        // AuthorizationData
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AuthorizationData init to AuthorizationData SEQ OF
        // --------------------------------------------------------------------------------------------
        // AuthorizationData   ::= SEQUENCE OF
        super.transitions[AuthorizationDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.START_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new AuthorizationDataInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from AuthorizationData SEQ OF to SEQ
        // --------------------------------------------------------------------------------------------
        // AuthorizationData  ::= SEQUENCE OF SEQUENCE {
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_SEQ_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<AuthorizationDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from AuthorizationData SEQ OF to adType tag
        // --------------------------------------------------------------------------------------------
        // AuthorizationData  ::= SEQUENCE OF SEQUENCE {
        //         ad-type     [0]
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_STATE.ordinal()][KerberosConstants.AUTHORIZATION_DATA_ADTYPE_TAG] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_TAG_STATE,
                KerberosConstants.AUTHORIZATION_DATA_ADTYPE_TAG,
                new CheckNotNullLength<AuthorizationDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from adtype tag to adtype value
        // --------------------------------------------------------------------------------------------
        // AuthorizationData  ::= SEQUENCE OF SEQUENCE {
        //         ad-type     [0] Int32,
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_TAG_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_STATE,
                UniversalTag.INTEGER,
                new StoreAdType() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-type value to ad-data tag
        // --------------------------------------------------------------------------------------------
        // AuthorizationData   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         ad-data     [1]
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_STATE.ordinal()][KerberosConstants.AUTHORIZATION_DATA_ADDATA_TAG] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADTYPE_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_TAG_STATE,
                KerberosConstants.AUTHORIZATION_DATA_ADDATA_TAG,
                new CheckNotNullLength<AuthorizationDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-data tag to ad-data value
        // --------------------------------------------------------------------------------------------
        // AuthorizationData   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         ad-data     [1] (OCTET STRING)
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .getValue()] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_TAG_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_STATE,
                UniversalTag.OCTET_STRING,
                new StoreAdData() );

        // --------------------------------------------------------------------------------------------
        // Transition from ad-data value to SEQUENCE
        // --------------------------------------------------------------------------------------------
        // AuthorizationData   ::= SEQUENCE {
        //         ...
        //         ad-data     [1] (OCTET STRING)
        super.transitions[AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<AuthorizationDataContainer>(
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_ADDATA_STATE,
                AuthorizationDataStatesEnum.AUTHORIZATION_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<AuthorizationDataContainer>() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AuthorizationData Grammar
     */
    public static Grammar<AuthorizationDataContainer> getInstance()
    {
        return instance;
    }
}
