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
package org.apache.directory.shared.kerberos.codec.apRep;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.apRep.actions.ApRepInit;
import org.apache.directory.shared.kerberos.codec.apRep.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.apRep.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.apRep.actions.StorePvno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AP-REP structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ApRepGrammar extends AbstractGrammar<ApRepContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ApRepGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ApRepGrammar is a singleton */
    private static Grammar<ApRepContainer> instance = new ApRepGrammar();


    /**
     * Creates a new ApRepGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ApRepGrammar()
    {
        setName( ApRepGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ApRepStatesEnum.LAST_AP_REP_STATE.ordinal()][256];

        // ============================================================================================
        // AP-REP
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AP-REP init to AP-REP tag
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15]
        super.transitions[ApRepStatesEnum.START_STATE.ordinal()][KerberosConstants.AP_REP_TAG] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.START_STATE,
                ApRepStatesEnum.AP_REP_STATE,
                KerberosConstants.AP_REP_TAG,
                new ApRepInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from AP-REP tag to AP-REP SEQUENCE
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        super.transitions[ApRepStatesEnum.AP_REP_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_STATE,
                ApRepStatesEnum.AP_REP_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<ApRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from AP-REP SEQUENCE to pvno tag
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        //        pvno            [0]
        super.transitions[ApRepStatesEnum.AP_REP_SEQ_STATE.ordinal()][KerberosConstants.AP_REP_PVNO_TAG] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_SEQ_STATE,
                ApRepStatesEnum.AP_REP_PVNO_TAG_STATE,
                KerberosConstants.AP_REP_PVNO_TAG,
                new CheckNotNullLength<ApRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        //        pvno            [0] INTEGER (5),
        super.transitions[ApRepStatesEnum.AP_REP_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_PVNO_TAG_STATE,
                ApRepStatesEnum.AP_REP_PVNO_STATE,
                UniversalTag.INTEGER,
                new StorePvno() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno value to msg-type tag
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        //        ...
        //        msg-type        [1]
        super.transitions[ApRepStatesEnum.AP_REP_PVNO_STATE.ordinal()][KerberosConstants.AP_REP_MSG_TYPE_TAG] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_PVNO_STATE,
                ApRepStatesEnum.AP_REP_MSG_TYPE_TAG_STATE,
                KerberosConstants.AP_REP_MSG_TYPE_TAG,
                new CheckNotNullLength<ApRepContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        //        ...
        //        msg-type        [1] INTEGER (15),
        super.transitions[ApRepStatesEnum.AP_REP_MSG_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_MSG_TYPE_TAG_STATE,
                ApRepStatesEnum.AP_REP_MSG_TYPE_STATE,
                UniversalTag.INTEGER,
                new CheckMsgType() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to enc-part
        // --------------------------------------------------------------------------------------------
        // AP-REP          ::= [APPLICATION 15] SEQUENCE OF {
        //        ...
        //        enc-part        [2] EncryptedData -- EncAPRepPart
        // }
        super.transitions[ApRepStatesEnum.AP_REP_MSG_TYPE_STATE.ordinal()][KerberosConstants.AP_REP_ENC_PART_TAG] =
            new GrammarTransition<ApRepContainer>(
                ApRepStatesEnum.AP_REP_MSG_TYPE_STATE,
                ApRepStatesEnum.AP_REP_ENC_PART_STATE,
                KerberosConstants.AP_REP_ENC_PART_TAG,
                new StoreEncPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AP-REP Grammar
     */
    public static Grammar<ApRepContainer> getInstance()
    {
        return instance;
    }
}
