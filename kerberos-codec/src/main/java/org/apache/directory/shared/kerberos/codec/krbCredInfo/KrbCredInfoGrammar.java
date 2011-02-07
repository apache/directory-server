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
package org.apache.directory.shared.kerberos.codec.krbCredInfo;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.KrbCredInfoInit;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreAuthTime;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreCaddr;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreEndTime;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreFlags;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreKey;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StorePName;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StorePRealm;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreRenewtill;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreSName;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreSRealm;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.actions.StoreStartTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KrbCredInfo structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KrbCredInfoGrammar extends AbstractGrammar<KrbCredInfoContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KrbCredInfoGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KrbCredInfoGrammar is a singleton */
    private static Grammar<KrbCredInfoContainer> instance = new KrbCredInfoGrammar();


    /**
     * Creates a new KrbCredInfoGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KrbCredInfoGrammar()
    {
        setName( KrbCredInfoGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KrbCredInfoStatesEnum.LAST_KRB_CRED_INFO_STATE.ordinal()][256];

        // ============================================================================================
        // KrbCredInfo
        // ============================================================================================

        super.transitions[KrbCredInfoStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.START_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SEQ_TAG_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new KrbCredInfoInit() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_SEQ_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_KEY_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SEQ_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_KEY_TAG,
                new StoreKey() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_PREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_PREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE,
                UniversalTag.GENERAL_STRING.getValue(),
                new StorePRealm() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_PNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_PNAME_TAG,
                new StorePName() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_FLAGS_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_TAG_STATE, KerberosConstants.KRB_CRED_INFO_FLAGS_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_TAG_STATE.ordinal()][UniversalTag.BIT_STRING
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE,
                UniversalTag.BIT_STRING.getValue(),
                new StoreFlags() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
                new StoreAuthTime() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
                new StoreStartTime() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
                new StoreEndTime() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
                new StoreRenewtill() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING
            .getValue()] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_STATE,
                UniversalTag.GENERAL_STRING.getValue(),
                new StoreSRealm() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE, KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // ---------------------------- OPTIONAL transitions ------------------------

        // transition from key to pname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_PNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_PNAME_TAG,
                new StorePName() );

        // transition from key to flags
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_FLAGS_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_FLAGS_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to authtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to starttime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to endtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from key to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from key to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_KEY_TAG_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from prealm to flags
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_FLAGS_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_FLAGS_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to authtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to starttime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to endtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from prealm to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from prealm to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from pname to authtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from pname to starttime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from pname to endtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from pname to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from pname to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from pname to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from pname to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_PNAME_TAG_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE, KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from flags to starttime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from flags to endtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from flags to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from flags to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from flags to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from flags to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_FLAGS_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from authtime to endtime
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from authtime to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from authtime to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from authtime to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from authtime to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_AUTHTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE, KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from starttime to renewtill
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_TAG_STATE, KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from starttime to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from starttime to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from starttime to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_STARTTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE, KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from endtime to srealm
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SREALM_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SREALM_TAG,
                new CheckNotNullLength<KrbCredInfoContainer>() );

        // transition from endtime to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from endtime to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_ENDTIME_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from renewtill to sname
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_SNAME_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SNAME_TAG_STATE, KerberosConstants.KRB_CRED_INFO_SNAME_TAG,
                new StoreSName() );

        // transition from renewtill to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_RENEWTILL_STATE,
                KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE, KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );

        // transition from srealm to caddr
        super.transitions[KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_STATE.ordinal()][KerberosConstants.KRB_CRED_INFO_CADDR_TAG] =
            new GrammarTransition<KrbCredInfoContainer>(
                KrbCredInfoStatesEnum.KRB_CRED_INFO_SREALM_STATE, KrbCredInfoStatesEnum.KRB_CRED_INFO_CADDR_TAG_STATE,
                KerberosConstants.KRB_CRED_INFO_CADDR_TAG,
                new StoreCaddr() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KrbCredInfo Grammar
     */
    public static Grammar<KrbCredInfoContainer> getInstance()
    {
        return instance;
    }
}
