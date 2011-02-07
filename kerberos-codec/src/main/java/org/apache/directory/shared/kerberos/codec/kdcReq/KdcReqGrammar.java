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
package org.apache.directory.shared.kerberos.codec.kdcReq;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.kdcReq.actions.AddPaData;
import org.apache.directory.shared.kerberos.codec.kdcReq.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.kdcReq.actions.StoreKdcReqBody;
import org.apache.directory.shared.kerberos.codec.kdcReq.actions.StorePvno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KdcReq structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KdcReqGrammar extends AbstractGrammar<KdcReqContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KdcReqGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KdcReqGrammar is a singleton */
    private static Grammar<KdcReqContainer> instance = new KdcReqGrammar();


    /**
     * Creates a new KdcReqGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KdcReqGrammar()
    {
        setName( KdcReqGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KdcReqStatesEnum.LAST_KDC_REQ_STATE.ordinal()][256];

        // ============================================================================================
        // KdcReq
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KdcReq init to KdcReq SEQ
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        super.transitions[KdcReqStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.START_STATE,
                KdcReqStatesEnum.KDC_REQ_PVNO_TAG_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<KdcReqContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from KdcReq SEQ to pvno tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         pvno            [1]
        super.transitions[KdcReqStatesEnum.KDC_REQ_PVNO_TAG_STATE.ordinal()][KerberosConstants.KDC_REQ_PVNO_TAG] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PVNO_TAG_STATE,
                KdcReqStatesEnum.KDC_REQ_PVNO_STATE,
                KerberosConstants.KDC_REQ_PVNO_TAG,
                new CheckNotNullLength<KdcReqContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         pvno            [1] INTEGER (5) ,
        super.transitions[KdcReqStatesEnum.KDC_REQ_PVNO_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PVNO_STATE,
                KdcReqStatesEnum.KDC_REQ_MSG_TYPE_TAG_STATE,
                UniversalTag.INTEGER.getValue(),
                new StorePvno() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno value to msg-type tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         msg-type        [2]
        super.transitions[KdcReqStatesEnum.KDC_REQ_MSG_TYPE_TAG_STATE.ordinal()][KerberosConstants.KDC_REQ_MSG_TYPE_TAG] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_MSG_TYPE_TAG_STATE,
                KdcReqStatesEnum.KDC_REQ_MSG_TYPE_STATE,
                KerberosConstants.KDC_REQ_MSG_TYPE_TAG,
                new CheckNotNullLength<KdcReqContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         msg-type        [2] INTEGER (10 -- AS -- | 12 -- TGS --),
        super.transitions[KdcReqStatesEnum.KDC_REQ_MSG_TYPE_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_MSG_TYPE_STATE,
                KdcReqStatesEnum.KDC_REQ_PA_DATA_OR_REQ_BODY_STATE,
                UniversalTag.INTEGER.getValue(),
                new CheckMsgType() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to padata tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         padata          [3]
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_OR_REQ_BODY_STATE.ordinal()][KerberosConstants.KDC_REQ_PA_DATA_TAG] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_OR_REQ_BODY_STATE,
                KdcReqStatesEnum.KDC_REQ_PA_DATA_TAG_STATE,
                KerberosConstants.KDC_REQ_PA_DATA_TAG,
                new CheckNotNullLength<KdcReqContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to KDC-REQ-BODY tag (pa-data is missing)
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         req-body        [4]
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_OR_REQ_BODY_STATE.ordinal()][KerberosConstants.KDC_REQ_KDC_REQ_BODY_TAG] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_OR_REQ_BODY_STATE,
                KdcReqStatesEnum.KDC_REQ_KDC_REQ_BODY_STATE,
                KerberosConstants.KDC_REQ_KDC_REQ_BODY_TAG,
                new StoreKdcReqBody() );

        // --------------------------------------------------------------------------------------------
        // Transition from padata tag to pa-data SEQ
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         padata          [3] SEQUENCE OF
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_TAG_STATE,
                KdcReqStatesEnum.KDC_REQ_PA_DATA_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new CheckNotNullLength<KdcReqContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pa-data SEQ to pa-data
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         padata          [3] SEQUENCE OF <PA-DATA>
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_SEQ_STATE,
                KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new AddPaData() );

        // --------------------------------------------------------------------------------------------
        // Transition from pa-data to pa-data
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         padata          [3] SEQUENCE OF <PA-DATA>
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE,
                KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new AddPaData() );

        // --------------------------------------------------------------------------------------------
        // Transition from pa-data to KDC-REQ-BODY tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ         ::= SEQUENCE {
        //         ...
        //         req-body        [4]
        super.transitions[KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE.ordinal()][KerberosConstants.KDC_REQ_KDC_REQ_BODY_TAG] =
            new GrammarTransition<KdcReqContainer>(
                KdcReqStatesEnum.KDC_REQ_PA_DATA_STATE,
                KdcReqStatesEnum.KDC_REQ_KDC_REQ_BODY_STATE,
                KerberosConstants.KDC_REQ_KDC_REQ_BODY_TAG,
                new StoreKdcReqBody() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KDC-REQ Grammar
     */
    public static Grammar<KdcReqContainer> getInstance()
    {
        return instance;
    }
}
