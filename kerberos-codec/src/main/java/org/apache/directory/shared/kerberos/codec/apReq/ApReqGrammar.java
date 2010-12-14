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
package org.apache.directory.shared.kerberos.codec.apReq;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.apReq.actions.ApReqInit;
import org.apache.directory.shared.kerberos.codec.apReq.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.apReq.actions.StoreApOptions;
import org.apache.directory.shared.kerberos.codec.apReq.actions.StoreAuthenticator;
import org.apache.directory.shared.kerberos.codec.apReq.actions.StorePvno;
import org.apache.directory.shared.kerberos.codec.apReq.actions.StoreTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AP-REQ structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ApReqGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ApReqGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ApReqGrammar is a singleton */
    private static Grammar instance = new ApReqGrammar();


    /**
     * Creates a new ApReqGrammar object.
     */
    private ApReqGrammar()
    {
        setName( ApReqGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ApReqStatesEnum.LAST_AP_REQ_STATE.ordinal()][256];

        // ============================================================================================
        // AP-REQ 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AP-REQ init to AP-REQ tag
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14]
        super.transitions[ApReqStatesEnum.START_STATE.ordinal()][KerberosConstants.AP_REQ_TAG] = new GrammarTransition(
            ApReqStatesEnum.START_STATE, ApReqStatesEnum.AP_REQ_STATE, KerberosConstants.AP_REQ_TAG,
            new ApReqInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from AP-REQ tag to AP-REQ SEQ {
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        super.transitions[ApReqStatesEnum.AP_REQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_STATE, ApReqStatesEnum.AP_REQ_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from AP-REQ SEQ to PVNO tag
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         pvno            [0]
        super.transitions[ApReqStatesEnum.AP_REQ_SEQ_STATE.ordinal()][KerberosConstants.AP_REQ_PVNO_TAG] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_SEQ_STATE, ApReqStatesEnum.AP_REQ_PVNO_TAG_STATE, KerberosConstants.AP_REQ_PVNO_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from PVNO tag to PVNO value
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         pvno            [0] INTEGER (5),
        super.transitions[ApReqStatesEnum.AP_REQ_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_PVNO_TAG_STATE, ApReqStatesEnum.AP_REQ_PVNO_STATE, UniversalTag.INTEGER.getValue(),
            new StorePvno() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from PVNO value to msg-type tag
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         msg-type        [1]
        super.transitions[ApReqStatesEnum.AP_REQ_PVNO_STATE.ordinal()][KerberosConstants.AP_REQ_MSG_TYPE_TAG] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_PVNO_STATE, ApReqStatesEnum.AP_REQ_MSG_TYPE_TAG_STATE, KerberosConstants.AP_REQ_MSG_TYPE_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         msg-type        [1] INTEGER (14),
        super.transitions[ApReqStatesEnum.AP_REQ_MSG_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_MSG_TYPE_TAG_STATE, ApReqStatesEnum.AP_REQ_MSG_TYPE_STATE, UniversalTag.INTEGER.getValue(),
            new CheckMsgType() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to ap-options tag
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         ap-options      [2]
        super.transitions[ApReqStatesEnum.AP_REQ_MSG_TYPE_STATE.ordinal()][KerberosConstants.AP_REQ_AP_OPTIONS_TAG] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_MSG_TYPE_STATE, ApReqStatesEnum.AP_REQ_AP_OPTIONS_TAG_STATE, KerberosConstants.AP_REQ_AP_OPTIONS_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from ap-options tag to ap-options value
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         ap-options      [2] APOptions,
        super.transitions[ApReqStatesEnum.AP_REQ_AP_OPTIONS_TAG_STATE.ordinal()][UniversalTag.BIT_STRING.getValue()] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_AP_OPTIONS_TAG_STATE, ApReqStatesEnum.AP_REQ_AP_OPTIONS_STATE, UniversalTag.BIT_STRING.getValue(),
            new StoreApOptions() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from ap-options value to ticket
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         ticket          [3] Ticket,
        super.transitions[ApReqStatesEnum.AP_REQ_AP_OPTIONS_STATE.ordinal()][KerberosConstants.AP_REQ_TICKET_TAG] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_AP_OPTIONS_STATE, ApReqStatesEnum.AP_REQ_TICKET_STATE, KerberosConstants.AP_REQ_TICKET_TAG,
            new StoreTicket() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from ticket to authenticator
        // --------------------------------------------------------------------------------------------
        // AP-REQ          ::= [APPLICATION 14] SEQUENCE
        //         ...
        //         authenticator   [4] <EncryptedData> -- Authenticator
        // }
        super.transitions[ApReqStatesEnum.AP_REQ_TICKET_STATE.ordinal()][KerberosConstants.AP_REQ_AUTHENTICATOR_TAG] = new GrammarTransition(
            ApReqStatesEnum.AP_REQ_TICKET_STATE, ApReqStatesEnum.LAST_AP_REQ_STATE, KerberosConstants.AP_REQ_AUTHENTICATOR_TAG,
            new StoreAuthenticator() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the AP-REQ Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
