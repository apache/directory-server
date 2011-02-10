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
package org.apache.directory.shared.kerberos.codec.krbPriv;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.krbPriv.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.krbPriv.actions.KrbPrivInit;
import org.apache.directory.shared.kerberos.codec.krbPriv.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.krbPriv.actions.StorePvno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KRB-PRIV structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KrbPrivGrammar extends AbstractGrammar<KrbPrivContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KrbPrivGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KrbPrivGrammar is a singleton */
    private static Grammar<KrbPrivContainer> instance = new KrbPrivGrammar();


    /**
     * Creates a new KrbPrivGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KrbPrivGrammar()
    {
        setName( KrbPrivGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KrbPrivStatesEnum.LAST_KRB_PRIV_STATE.ordinal()][256];

        // ============================================================================================
        // KRB_PRIV
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KrbPriv init to KrbPriv tag
        // --------------------------------------------------------------------------------------------
        // KRB_PRIV       ::= [APPLICATION 21]
        super.transitions[KrbPrivStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_PRIV_TAG] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.START_STATE,
                KrbPrivStatesEnum.KRB_PRIV_TAG_STATE,
                KerberosConstants.KRB_PRIV_TAG,
                new KrbPrivInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from KrbPriv tag to KrbPriv SEQ
        // --------------------------------------------------------------------------------------------
        // KRB_PRIV       ::= [APPLICATION 21] SEQUENCE {
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_TAG_STATE,
                KrbPrivStatesEnum.KRB_PRIV_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<KrbPrivContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from KrbPriv SEQ to pvno tag
        // --------------------------------------------------------------------------------------------
        // KRB_PRIV         ::= SEQUENCE {
        //         pvno            [0]
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_SEQ_STATE.ordinal()][KerberosConstants.KRB_PRIV_PVNO_TAG] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_SEQ_STATE,
                KrbPrivStatesEnum.KRB_PRIV_PVNO_TAG_STATE,
                KerberosConstants.KRB_PRIV_PVNO_TAG,
                new CheckNotNullLength<KrbPrivContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // KRB_PRIV         ::= SEQUENCE {
        //         pvno            [0] INTEGER (5) ,
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_PVNO_TAG_STATE,
                KrbPrivStatesEnum.KRB_PRIV_PVNO_STATE,
                UniversalTag.INTEGER,
                new StorePvno() );

        // --------------------------------------------------------------------------------------------
        // Transition from pvno to msg-type tag
        // --------------------------------------------------------------------------------------------
        // msg-type        [1]
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_PVNO_STATE.ordinal()][KerberosConstants.KRB_PRIV_MSGTYPE_TAG] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_PVNO_STATE,
                KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_TAG_STATE,
                KerberosConstants.KRB_PRIV_MSGTYPE_TAG,
                new CheckNotNullLength<KrbPrivContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // msg-type        [1] INTEGER (30)
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_TAG_STATE,
                KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_STATE,
                UniversalTag.INTEGER,
                new CheckMsgType() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to enc-part tag
        // --------------------------------------------------------------------------------------------
        // enc-part       [3] [3] EncryptedData -- EncKrbPrivPart
        super.transitions[KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_STATE.ordinal()][KerberosConstants.KRB_PRIV_ENC_PART_TAG] =
            new GrammarTransition<KrbPrivContainer>(
                KrbPrivStatesEnum.KRB_PRIV_MSGTYPE_STATE,
                KrbPrivStatesEnum.KRB_PRIV_EN_PART_TAG_STATE,
                KerberosConstants.KRB_PRIV_ENC_PART_TAG,
                new StoreEncPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KRB_PRIV Grammar
     */
    public static Grammar<KrbPrivContainer> getInstance()
    {
        return instance;
    }
}
