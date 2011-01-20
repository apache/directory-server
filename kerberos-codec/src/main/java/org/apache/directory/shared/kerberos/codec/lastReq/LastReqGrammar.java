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
package org.apache.directory.shared.kerberos.codec.lastReq;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.lastReq.actions.LastReqInit;
import org.apache.directory.shared.kerberos.codec.lastReq.actions.StoreLrType;
import org.apache.directory.shared.kerberos.codec.lastReq.actions.StoreLrValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the LastReq structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LastReqGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( LastReqGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. LastReqGrammar is a singleton */
    private static Grammar instance = new LastReqGrammar();


    /**
     * Creates a new LastReqGrammar object.
     */
    private LastReqGrammar()
    {
        setName( LastReqGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[LastReqStatesEnum.LAST_LAST_REQ_STATE.ordinal()][256];

        // ============================================================================================
        // LastReq 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from LastReq init to LastReq SEQ OF
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF
        super.transitions[LastReqStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            LastReqStatesEnum.START_STATE, LastReqStatesEnum.LAST_REQ_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new LastReqInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from LastReq SEQ OF to LastReq SEQ OF SEQ
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        super.transitions[LastReqStatesEnum.LAST_REQ_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_SEQ_STATE, LastReqStatesEnum.LAST_REQ_SEQ_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from LastReq SEQ OF SEQ to lr-type tag
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        //         lr-type         [0]
        super.transitions[LastReqStatesEnum.LAST_REQ_SEQ_SEQ_STATE.ordinal()][KerberosConstants.LAST_REQ_LR_TYPE_TAG] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_SEQ_SEQ_STATE, LastReqStatesEnum.LAST_REQ_LR_TYPE_TAG_STATE, KerberosConstants.LAST_REQ_LR_TYPE_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from lr-type tag to lr-type value
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        //         lr-type         [0]  Int32,
        super.transitions[LastReqStatesEnum.LAST_REQ_LR_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_LR_TYPE_TAG_STATE, LastReqStatesEnum.LAST_REQ_LR_TYPE_STATE, UniversalTag.INTEGER.getValue(),
            new StoreLrType() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from lr-type value lr-value tag
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         lr-value        [1]
        super.transitions[LastReqStatesEnum.LAST_REQ_LR_TYPE_STATE.ordinal()][KerberosConstants.LAST_REQ_LR_VALUE_TAG] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_LR_TYPE_STATE, LastReqStatesEnum.LAST_REQ_LR_VALUE_TAG_STATE, KerberosConstants.LAST_REQ_LR_VALUE_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from lr-value tag to lr-value value
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         lr-value        [1] KerberosTime
        super.transitions[LastReqStatesEnum.LAST_REQ_LR_VALUE_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_LR_VALUE_TAG_STATE, LastReqStatesEnum.LAST_REQ_LR_VALUE_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreLrValue() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from lr-value value to SEQ OF SEQ
        // --------------------------------------------------------------------------------------------
        // LastReq   ::= SEQUENCE OF SEQUENCE {
        //         ...
        //         lr-value        [1] KerberosTime
        // }
        super.transitions[LastReqStatesEnum.LAST_REQ_LR_VALUE_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            LastReqStatesEnum.LAST_REQ_LR_VALUE_STATE, LastReqStatesEnum.LAST_REQ_SEQ_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the LastReq Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
