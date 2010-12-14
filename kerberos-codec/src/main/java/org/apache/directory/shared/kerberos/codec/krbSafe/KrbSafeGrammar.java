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
package org.apache.directory.shared.kerberos.codec.krbSafe;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.krbSafe.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.krbSafe.actions.KrbSafeInit;
import org.apache.directory.shared.kerberos.codec.krbSafe.actions.StoreChecksum;
import org.apache.directory.shared.kerberos.codec.krbSafe.actions.StorePvno;
import org.apache.directory.shared.kerberos.codec.krbSafe.actions.StoreSafeBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KRB-SAFE structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KrbSafeGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KrbSafeGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KrbSafeGrammar is a singleton */
    private static Grammar instance = new KrbSafeGrammar();


    /**
     * Creates a new KrbSafeGrammar object.
     */
    private KrbSafeGrammar()
    {
        setName( KrbSafeGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KrbSafeStatesEnum.LAST_KRB_SAFE_STATE.ordinal()][256];

        // ============================================================================================
        // KRB-SAFE 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KrbSafe init to KrbSafe tag
        // --------------------------------------------------------------------------------------------
        // KRB-SAFE       ::= [APPLICATION 20]
        super.transitions[KrbSafeStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_SAFE_TAG] = new GrammarTransition(
            KrbSafeStatesEnum.START_STATE, KrbSafeStatesEnum.KRB_SAFE_TAG_STATE, KerberosConstants.KRB_SAFE_TAG,
            new KrbSafeInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from KrbSafe tag to KrbSafe SEQ
        // --------------------------------------------------------------------------------------------
        // KRB-SAFE       ::= [APPLICATION 20] SEQUENCE {
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_TAG_STATE, KrbSafeStatesEnum.KRB_SAFE_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from KrbSafe SEQ to pvno tag
        // --------------------------------------------------------------------------------------------
        // KRB-SAFE         ::= SEQUENCE {
        //         pvno            [0]
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_SEQ_STATE.ordinal()][KerberosConstants.KRB_SAFE_PVNO_TAG] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_SEQ_STATE, KrbSafeStatesEnum.KRB_SAFE_PVNO_TAG_STATE, KerberosConstants.KRB_SAFE_PVNO_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // KRB-SAFE         ::= SEQUENCE {
        //         pvno            [0] INTEGER (5) ,
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_PVNO_TAG_STATE, KrbSafeStatesEnum.KRB_SAFE_PVNO_STATE, UniversalTag.INTEGER.getValue(),
            new StorePvno() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from pvno to msg-type tag
        // --------------------------------------------------------------------------------------------
        // msg-type        [1]
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_PVNO_STATE.ordinal()][KerberosConstants.KRB_SAFE_MSGTYPE_TAG] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_PVNO_STATE, KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_TAG_STATE, KerberosConstants.KRB_SAFE_MSGTYPE_TAG, 
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // msg-type        [1] INTEGER (30)
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_TAG_STATE, KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_STATE, UniversalTag.INTEGER.getValue(), 
            new CheckMsgType() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to safe-body tag
        // --------------------------------------------------------------------------------------------
        // safe-body       [2] KRB-SAFE-BODY
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_STATE.ordinal()][KerberosConstants.KRB_SAFE_SAFE_BODY_TAG] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_MSGTYPE_STATE, KrbSafeStatesEnum.KRB_SAFE_SAFE_BODY_TAG_STATE, KerberosConstants.KRB_SAFE_SAFE_BODY_TAG, 
            new StoreSafeBody() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from safe-body tag to cksum tag
        // --------------------------------------------------------------------------------------------
        // cksum           [3] Checksum
        super.transitions[KrbSafeStatesEnum.KRB_SAFE_SAFE_BODY_TAG_STATE.ordinal()][KerberosConstants.KRB_SAFE_CKSUM_TAG] = new GrammarTransition(
            KrbSafeStatesEnum.KRB_SAFE_SAFE_BODY_TAG_STATE, KrbSafeStatesEnum.KRB_SAFE_CKSUM_TAG_STATE, KerberosConstants.KRB_SAFE_CKSUM_TAG, 
            new StoreChecksum() );
    }

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the KRB-SAFE Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
