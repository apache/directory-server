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
package org.apache.directory.shared.kerberos.codec.kdcRep;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.AddPaData;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.StoreCName;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.StoreCRealm;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.StorePvno;
import org.apache.directory.shared.kerberos.codec.kdcRep.actions.StoreTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KdcReq structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KdcRepGrammar extends AbstractGrammar<KdcRepContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KdcRepGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KdcReqGrammar is a singleton */
    private static Grammar<KdcRepContainer> instance = new KdcRepGrammar();


    /**
     * Creates a new KdcRepGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KdcRepGrammar()
    {
        setName( KdcRepGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KdcRepStatesEnum.LAST_KDC_REP_STATE.ordinal()][256];

        // ============================================================================================
        // KdcReq
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KdcRep init to KdcRep SEQ
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        super.transitions[KdcRepStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.START_STATE,
                KdcRepStatesEnum.KDC_REP_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from KdcRep SEQ to pvno tag
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         pvno            [0]
        super.transitions[KdcRepStatesEnum.KDC_REP_SEQ_STATE.ordinal()][KerberosConstants.KDC_REP_PVNO_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_SEQ_STATE,
                KdcRepStatesEnum.KDC_REP_PVNO_TAG_STATE,
                KerberosConstants.KDC_REP_PVNO_TAG,
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         pvno            [0] INTEGER (5)
        super.transitions[KdcRepStatesEnum.KDC_REP_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_PVNO_TAG_STATE,
                KdcRepStatesEnum.KDC_REP_PVNO_STATE,
                UniversalTag.INTEGER.getValue(),
                new StorePvno() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno value to msg-type tag
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         msg-type        [1]
        super.transitions[KdcRepStatesEnum.KDC_REP_PVNO_STATE.ordinal()][KerberosConstants.KDC_REP_MSG_TYPE_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_PVNO_STATE,
                KdcRepStatesEnum.KDC_REP_MSG_TYPE_TAG_STATE,
                KerberosConstants.KDC_REP_MSG_TYPE_TAG,
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         msg-type        [1] INTEGER (11 -- AS -- | 13 -- TGS --),
        super.transitions[KdcRepStatesEnum.KDC_REP_MSG_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_MSG_TYPE_TAG_STATE,
                KdcRepStatesEnum.KDC_REP_MSG_TYPE_STATE,
                UniversalTag.INTEGER.getValue(),
                new CheckMsgType() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value pa-data tag
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         padata          [2]
        super.transitions[KdcRepStatesEnum.KDC_REP_MSG_TYPE_STATE.ordinal()][KerberosConstants.KDC_REP_PA_DATA_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_MSG_TYPE_STATE,
                KdcRepStatesEnum.KDC_REP_PA_DATA_TAG_STATE,
                KerberosConstants.KDC_REP_PA_DATA_TAG,
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pa-data tag to pa-data sequence
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         padata          [2] SEQUENCE OF
        super.transitions[KdcRepStatesEnum.KDC_REP_PA_DATA_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_PA_DATA_TAG_STATE,
                KdcRepStatesEnum.KDC_REP_PA_DATA_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pa-data sequence to PA-DATA
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         padata          [2] SEQUENCE OF PA-DATA
        super.transitions[KdcRepStatesEnum.KDC_REP_PA_DATA_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_PA_DATA_STATE,
                KdcRepStatesEnum.KDC_REP_PA_DATA_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new AddPaData() );

        // --------------------------------------------------------------------------------------------
        // Transition from PA-DATA to crealm tag
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         crealm          [3]
        super.transitions[KdcRepStatesEnum.KDC_REP_PA_DATA_STATE.ordinal()][KerberosConstants.KDC_REP_CREALM_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_PA_DATA_STATE,
                KdcRepStatesEnum.KDC_REP_CREALM_TAG_STATE,
                KerberosConstants.KDC_REP_CREALM_TAG,
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to crealm tag (pa-data is empty)
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         crealm          [3]
        super.transitions[KdcRepStatesEnum.KDC_REP_MSG_TYPE_STATE.ordinal()][KerberosConstants.KDC_REP_CREALM_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_MSG_TYPE_STATE,
                KdcRepStatesEnum.KDC_REP_CREALM_TAG_STATE,
                KerberosConstants.KDC_REP_CREALM_TAG,
                new CheckNotNullLength<KdcRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from crealm tag to crealm value
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         crealm          [3] Realm,
        super.transitions[KdcRepStatesEnum.KDC_REP_CREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_CREALM_TAG_STATE,
                KdcRepStatesEnum.KDC_REP_CREALM_STATE,
                UniversalTag.GENERAL_STRING.getValue(),
                new StoreCRealm() );

        // --------------------------------------------------------------------------------------------
        // Transition from crealm value to cname
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         cname           [4] PrincipalName,
        super.transitions[KdcRepStatesEnum.KDC_REP_CREALM_STATE.ordinal()][KerberosConstants.KDC_REP_CNAME_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_CREALM_STATE,
                KdcRepStatesEnum.KDC_REP_CNAME_STATE,
                KerberosConstants.KDC_REP_CNAME_TAG,
                new StoreCName() );

        // --------------------------------------------------------------------------------------------
        // Transition from cname to ticket
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         ticket          [5] Ticket,
        super.transitions[KdcRepStatesEnum.KDC_REP_CNAME_STATE.ordinal()][KerberosConstants.KDC_REP_TICKET_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_CNAME_STATE,
                KdcRepStatesEnum.KDC_REP_TICKET_STATE,
                KerberosConstants.KDC_REP_TICKET_TAG,
                new StoreTicket() );

        // --------------------------------------------------------------------------------------------
        // Transition from ticket to enc-part
        // --------------------------------------------------------------------------------------------
        // KDC-REP         ::= SEQUENCE {
        //         ...
        //         enc-part        [6] EncryptedData
        super.transitions[KdcRepStatesEnum.KDC_REP_TICKET_STATE.ordinal()][KerberosConstants.KDC_REP_ENC_PART_TAG] =
            new GrammarTransition<KdcRepContainer>(
                KdcRepStatesEnum.KDC_REP_TICKET_STATE,
                KdcRepStatesEnum.KDC_REP_ENC_PART_STATE,
                KerberosConstants.KDC_REP_ENC_PART_TAG,
                new StoreEncPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KDC-REQ Grammar
     */
    public static Grammar<KdcRepContainer> getInstance()
    {
        return instance;
    }
}
