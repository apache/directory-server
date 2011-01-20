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
package org.apache.directory.shared.kerberos.codec.EncKdcRepPart;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.EncKdcRepPartInit;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreAuthTime;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreCAddr;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreEndTime;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreFlags;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreKey;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreKeyExpiration;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreLastReq;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreNonce;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreRenewTill;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreSName;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreSRealm;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.actions.StoreStartTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncKdcRepPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncKdcRepPartGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncKdcRepPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncKdcRepPartGrammar is a singleton */
    private static Grammar instance = new EncKdcRepPartGrammar();


    /**
     * Creates a new EncKdcRepPartGrammar object.
     */
    private EncKdcRepPartGrammar()
    {
        setName( EncKdcRepPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncKdcRepPartStatesEnum.LAST_ENC_KDC_REP_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncKdcRepPart 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncKdcRepPart init to EncKdcRepPart tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        super.transitions[EncKdcRepPartStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.START_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SEQ_TAG_STATE, UniversalTag.SEQUENCE.getValue(),
            new EncKdcRepPartInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from EncKdcRepPart tag to key
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         key             [0] EncryptionKey,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SEQ_TAG_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_KEY_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SEQ_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_KEY_TAG,
            new StoreKey() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from key to last-req
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         last-req        [1] LastReq,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_TAG_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_LAST_REQ_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_LAST_REQ_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_LAST_REQ_TAG,
            new StoreLastReq() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from last-req to nonce tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         nonce           [2]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_LAST_REQ_TAG_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_NONCE_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_LAST_REQ_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_NONCE_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from nonce tag to nonce value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         nonce           [2] UInt32,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_STATE, UniversalTag.INTEGER.getValue(),
            new StoreNonce() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from nonce value to key-expiration tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         key-expiration  [3]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from nonce value to flags tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         flags           [4]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_FLAGS_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_NONCE_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_FLAGS_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from key-expiration tag to key-expiration value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         key-expiration  [3] KerberosTime OPTIONAL,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreKeyExpiration() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from key-expiration value to flags tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         flags           [4]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_FLAGS_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_KEY_EXPIRATION_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_FLAGS_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from flags tag to flags value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         flags           [4] TicketFlags,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_TAG_STATE.ordinal()][UniversalTag.BIT_STRING.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_STATE, UniversalTag.BIT_STRING.getValue(),
            new StoreFlags() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from flags value to authtime tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         authtime        [5]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_AUTH_TIME_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_FLAGS_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_AUTH_TIME_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from authtime tag to authtime value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         authtime        [5] KerberosTime,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreAuthTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from authtime value to starttime tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         starttime       [6]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_START_TIME_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_START_TIME_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from authtime value to endtime tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         endtime         [7] 
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_END_TIME_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_AUTH_TIME_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_END_TIME_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from starttime tag to starttime value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         starttime       [6] KerberosTime OPTIONAL,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreStartTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from starttime value to endtime tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         endtime         [7] 
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_END_TIME_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_START_TIME_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_END_TIME_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from endtime tag to endtime value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         endtime         [7] KerberosTime,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreEndTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from endtime value to renew-till tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         renew-till      [8] 
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_RENEW_TILL_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_RENEW_TILL_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from endtime value to srealm tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         srealm          [9]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_SREALM_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_END_TIME_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_SREALM_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from renew-till tag to renew-till value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         renew-till      [8] KerberosTime OPTIONAL,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreRenewTill() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from renew-till value to srealm tag
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         srealm          [9]
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_SREALM_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_RENEW_TILL_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_SREALM_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from srealm tag to srealm value
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         srealm          [9] Realm,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_STATE, UniversalTag.GENERAL_STRING.getValue(),
            new StoreSRealm() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from srealm value to sname
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         sname           [10] PrincipalName,
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_SNAME_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SREALM_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SNAME_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_SNAME_TAG,
            new StoreSName() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from sname to caddr
        // --------------------------------------------------------------------------------------------
        // EncKDCRepPart   ::= SEQUENCE {
        //         ...
        //         caddr           [11] <HostAddresses> OPTIONAL
        // }
        super.transitions[EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SNAME_TAG_STATE.ordinal()][KerberosConstants.ENC_KDC_REP_PART_CADDR_TAG] = new GrammarTransition(
            EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_SNAME_TAG_STATE, EncKdcRepPartStatesEnum.ENC_KDC_REP_PART_CADDR_TAG_STATE, KerberosConstants.ENC_KDC_REP_PART_CADDR_TAG,
            new StoreCAddr() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the EncKdcRepPart Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
