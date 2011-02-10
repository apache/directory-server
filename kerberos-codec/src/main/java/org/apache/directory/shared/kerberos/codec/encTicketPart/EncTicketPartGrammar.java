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
package org.apache.directory.shared.kerberos.codec.encTicketPart;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.EncTicketPartInit;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreAuthTime;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreAuthorizationData;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreCName;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreCRealm;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreCaddr;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreEndTime;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreFlags;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreKey;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreRenewtill;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreStartTime;
import org.apache.directory.shared.kerberos.codec.encTicketPart.actions.StoreTransited;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncTicketPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncTicketPartGrammar extends AbstractGrammar<EncTicketPartContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncTicketPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncTicketPartGrammar is a singleton */
    private static Grammar<EncTicketPartContainer> instance = new EncTicketPartGrammar();


    /**
     * Creates a new EncTicketPartGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncTicketPartGrammar()
    {
        setName( EncTicketPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncTicketPartStatesEnum.LAST_ENC_TICKET_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncTicketPart
        // ============================================================================================
        super.transitions[EncTicketPartStatesEnum.START_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.START_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_TAG,
                new EncTicketPartInit() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_SEQ_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_FLAGS_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_SEQ_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_FLAGS_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_TAG_STATE.ordinal()][UniversalTag.BIT_STRING
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_STATE,
                UniversalTag.BIT_STRING,
                new StoreFlags() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_KEY_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_FLAGS_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_KEY_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_KEY_TAG,
                new StoreKey() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_KEY_TAG_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_CREALM_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_KEY_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_CREALM_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_STATE,
                UniversalTag.GENERAL_STRING,
                new StoreCRealm() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_CNAME_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_CREALM_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_CNAME_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_CNAME_TAG,
                new StoreCName() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_CNAME_TAG_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_TRANSITED_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_CNAME_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_TRANSITED_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_TRANSITED_TAG,
                new StoreTransited() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_TRANSITED_TAG_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_AUTHTIME_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_TRANSITED_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_AUTHTIME_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreAuthTime() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_STARTTIME_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_STARTTIME_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreStartTime() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_ENDTIME_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_STARTTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_ENDTIME_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreEndTime() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_RENEWTILL_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_RENEWTILL_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME
            .getValue()] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_STATE,
                UniversalTag.GENERALIZED_TIME,
                new StoreRenewtill() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_CADDR_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_CADDR_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_CADDR_TAG,
                new StoreCaddr() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_CADDR_TAG_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_CADDR_TAG_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHZ_DATA_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_ENDTIME_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_ENDTIME_TAG,
                new CheckNotNullLength<EncTicketPartContainer>() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_CADDR_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_CADDR_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_CADDR_TAG,
                new StoreCaddr() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_ENDTIME_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHZ_DATA_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );

        super.transitions[EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_STATE.ordinal()][KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG] =
            new GrammarTransition<EncTicketPartContainer>(
                EncTicketPartStatesEnum.ENC_TICKET_PART_RENEWTILL_STATE,
                EncTicketPartStatesEnum.ENC_TICKET_PART_AUTHZ_DATA_TAG_STATE,
                KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG,
                new StoreAuthorizationData() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncTicketPart Grammar
     */
    public static Grammar<EncTicketPartContainer> getInstance()
    {
        return instance;
    }
}
