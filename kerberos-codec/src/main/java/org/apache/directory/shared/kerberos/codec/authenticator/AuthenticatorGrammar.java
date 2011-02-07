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
package org.apache.directory.shared.kerberos.codec.authenticator;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.AuthenticatorInit;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreAuthenticatorVno;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreAuthorizationData;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreCName;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreCRealm;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreCTime;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreChecksum;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreCusec;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreSeqNumber;
import org.apache.directory.shared.kerberos.codec.authenticator.actions.StoreSubKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Authenticator structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AuthenticatorGrammar extends AbstractGrammar<AuthenticatorContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AuthenticatorGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AuthenticatorGrammar is a singleton */
    private static Grammar<AuthenticatorContainer> instance = new AuthenticatorGrammar();


    /**
     * Creates a new AuthenticatorGrammar object.
     */
    @SuppressWarnings("unchecked")
    private AuthenticatorGrammar()
    {
        setName( AuthenticatorGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AuthenticatorStatesEnum.LAST_AUTHENTICATOR_STATE.ordinal()][256];

        // ============================================================================================
        // Authenticator
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from START to Authenticator init
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2]
        super.transitions[AuthenticatorStatesEnum.START_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.START_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_STATE,
                KerberosConstants.AUTHENTICATOR_TAG,
                new AuthenticatorInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from Authenticator init to Authenticator SEQ
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from Authenticator SEQ to authenticator-vno tag
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         authenticator-vno       [0]
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from authenticator-vno tag to authenticator-vno value
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         authenticator-vno       [0] INTEGER (5),
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreAuthenticatorVno() );

        // --------------------------------------------------------------------------------------------
        // Transition from authenticator-vno value to crealm tag
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         crealm                  [1]
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CREALM_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHENTICATOR_VNO_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_CREALM_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from crealm tag to crealm value
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         crealm                  [1] Realm,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_TAG_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_STATE,
                UniversalTag.GENERAL_STRING.getValue(),
                new StoreCRealm() );

        // --------------------------------------------------------------------------------------------
        // Transition from crealm value cname
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         cname                   [2] PrincipalName,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CNAME_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CREALM_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CNAME_STATE,
                KerberosConstants.AUTHENTICATOR_CNAME_TAG,
                new StoreCName() );

        // --------------------------------------------------------------------------------------------
        // Transition from cname to cksum
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         cksum                   [3] Checksum OPTIONAL,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CNAME_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CKSUM_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CNAME_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CKSUM_STATE,
                KerberosConstants.AUTHENTICATOR_CKSUM_TAG,
                new StoreChecksum() );

        // --------------------------------------------------------------------------------------------
        // Transition from cname to cusec tag
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         cusec                   [4]
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CNAME_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CUSEC_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CNAME_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_CUSEC_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from cksum to cusec tag
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         cusec                   [4]
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CKSUM_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CUSEC_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CKSUM_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_CUSEC_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from cusec tag to cusec value
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         cusec                   [4] Microseconds,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_TAG_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreCusec() );

        // --------------------------------------------------------------------------------------------
        // Transition from cusec value to ctime tag
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         ctime                   [5]
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_CTIME_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CUSEC_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_CTIME_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ctime tag to ctime value
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         ctime                   [5] KerberosTime,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_TAG_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE,
                UniversalTag.GENERALIZED_TIME.getValue(),
                new StoreCTime() );

        // --------------------------------------------------------------------------------------------
        // Transition from ctime value to subkey
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         subkey                  [6] EncryptionKe> OPTIONAL,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_SUBKEY_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_SUBKEY_STATE,
                KerberosConstants.AUTHENTICATOR_SUBKEY_TAG,
                new StoreSubKey() );

        // --------------------------------------------------------------------------------------------
        // Transition from ctime value to seq-number
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         seq-number              [7] UInt32 OPTIONAL,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_SEQ_NUMBER_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_SEQ_NUMBER_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ctime value to authorization-data
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         authorization-data      [8] AuthorizationData OPTIONAL
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_CTIME_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHORIZATION_DATA_STATE,
                KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );

        // --------------------------------------------------------------------------------------------
        // Transition from subkey to seq-number
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         seq-number              [7] UInt32 OPTIONAL,
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_SUBKEY_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_SEQ_NUMBER_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_SUBKEY_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_TAG_STATE,
                KerberosConstants.AUTHENTICATOR_SEQ_NUMBER_TAG,
                new CheckNotNullLength<AuthenticatorContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from subkey to authorization-data
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         authorization-data      [8] AuthorizationData OPTIONAL
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_SUBKEY_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_SUBKEY_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHORIZATION_DATA_STATE,
                KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );

        // --------------------------------------------------------------------------------------------
        // Transition from seq-number tag to seq-number value
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         authorization-data      [8] AuthorizationData OPTIONAL
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_TAG_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreSeqNumber() );

        // --------------------------------------------------------------------------------------------
        // Transition from seq-number value to authorization-data
        // --------------------------------------------------------------------------------------------
        // Authenticator    ::= [APPLICATION 2] SEQUENCE {
        //         ...
        //         authorization-data      [8] AuthorizationData OPTIONAL
        // }
        super.transitions[AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_STATE.ordinal()][KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<AuthenticatorContainer>(
                AuthenticatorStatesEnum.AUTHENTICATOR_SEQ_NUMBER_STATE,
                AuthenticatorStatesEnum.AUTHENTICATOR_AUTHORIZATION_DATA_STATE,
                KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );
    }

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the Authenticator Grammar
     */
    public static Grammar<AuthenticatorContainer> getInstance()
    {
        return instance;
    }
}
