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
package org.apache.directory.shared.kerberos.codec.krbError;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.krbError.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.krbError.actions.KrbErrorInit;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreCName;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreCRealm;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreCTime;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreCusec;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreEData;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreEText;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreErrorCode;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StorePvno;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreRealm;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreSName;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreSTime;
import org.apache.directory.shared.kerberos.codec.krbError.actions.StoreSusec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KrbError structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KrbErrorGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KrbErrorGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KrbErrorGrammar is a singleton */
    private static Grammar instance = new KrbErrorGrammar();


    /**
     * Creates a new KrbErrorGrammar object.
     */
    private KrbErrorGrammar()
    {
        setName( KrbErrorGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KrbErrorStatesEnum.LAST_KRB_ERR_STATE.ordinal()][256];

        // ============================================================================================
        // KrbError 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KrbError init to KrbError tag
        // --------------------------------------------------------------------------------------------
        // KRB-ERROR       ::= [APPLICATION 30]
        super.transitions[KrbErrorStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_ERROR_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.START_STATE, KrbErrorStatesEnum.KRB_ERR_TAG, KerberosConstants.KRB_ERROR_TAG,
            new KrbErrorInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from KrbError tag to KrbError SEQ
        // --------------------------------------------------------------------------------------------
        // KRB-ERROR       ::= [APPLICATION 30] SEQUENCE {
        super.transitions[KrbErrorStatesEnum.KRB_ERR_TAG.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_TAG, KrbErrorStatesEnum.KRB_ERR_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from KrbError SEQ to pvno tag
        // --------------------------------------------------------------------------------------------
        // KRB-ERROR         ::= SEQUENCE {
        //         pvno            [0]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_SEQ_STATE.ordinal()][KerberosConstants.KRB_ERROR_PVNO_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_SEQ_STATE, KrbErrorStatesEnum.KRB_ERR_PVNO_TAG_STATE, KerberosConstants.KRB_ERROR_PVNO_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from pvno tag to pvno value
        // --------------------------------------------------------------------------------------------
        // KRB-ERROR         ::= SEQUENCE {
        //         pvno            [0] INTEGER (5) ,
        super.transitions[KrbErrorStatesEnum.KRB_ERR_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_PVNO_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_PVNO_STATE, UniversalTag.INTEGER.getValue(),
            new StorePvno() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from pvno to msg-type tag
        // --------------------------------------------------------------------------------------------
        // msg-type        [1]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_PVNO_STATE.ordinal()][KerberosConstants.KRB_ERROR_MSGTYPE_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_PVNO_STATE, KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_TAG_STATE, KerberosConstants.KRB_ERROR_MSGTYPE_TAG, 
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type tag to msg-type value
        // --------------------------------------------------------------------------------------------
        // msg-type        [1] INTEGER (30)
        super.transitions[KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE, UniversalTag.INTEGER.getValue(), 
            new CheckMsgType() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to cTime tag
        // --------------------------------------------------------------------------------------------
        // ctime        [2]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE.ordinal()][KerberosConstants.KRB_ERROR_CTIME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE, KrbErrorStatesEnum.KRB_ERR_CTIME_TAG_STATE, KerberosConstants.KRB_ERROR_CTIME_TAG, 
            new CheckNotNullLength());

        // --------------------------------------------------------------------------------------------
        // Transition from cTime tag to cTime value
        // --------------------------------------------------------------------------------------------
        // ctime        [2] KerberosTime OPTIONAL
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CTIME_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_CTIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(), 
            new StoreCTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cTime value to cusec tag
        // --------------------------------------------------------------------------------------------
        // cusec           [3]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CTIME_STATE.ordinal()][KerberosConstants.KRB_ERROR_CUSEC_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CTIME_STATE, KrbErrorStatesEnum.KRB_ERR_CUSEC_TAG_STATE, KerberosConstants.KRB_ERROR_CUSEC_TAG, 
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from cusec tag to cusec value
        // --------------------------------------------------------------------------------------------
        // cusec           [3] Microseconds OPTIONAL
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CUSEC_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CUSEC_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_CUSEC_STATE, UniversalTag.INTEGER.getValue(), 
            new StoreCusec() );
    
        // --------------------------------------------------------------------------------------------
        // Transition from cusec value to stime tag
        // --------------------------------------------------------------------------------------------
        // stime           [4]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CUSEC_STATE.ordinal()][KerberosConstants.KRB_ERROR_STIME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CUSEC_STATE, KrbErrorStatesEnum.KRB_ERR_STIME_TAG_STATE, KerberosConstants.KRB_ERROR_STIME_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from stime tag to stime value
        // --------------------------------------------------------------------------------------------
        // stime           [4] KerberosTime
        super.transitions[KrbErrorStatesEnum.KRB_ERR_STIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_STIME_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_STIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(), 
            new StoreSTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from stime value to susec tag
        // --------------------------------------------------------------------------------------------
        // susec           [5]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_STIME_STATE.ordinal()][KerberosConstants.KRB_ERROR_SUSEC_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_STIME_STATE, KrbErrorStatesEnum.KRB_ERR_SUSEC_TAG_STATE, KerberosConstants.KRB_ERROR_SUSEC_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from susec tag to susec value
        // --------------------------------------------------------------------------------------------
        // susec           [5] Microseconds
        super.transitions[KrbErrorStatesEnum.KRB_ERR_SUSEC_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_SUSEC_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_SUSEC_STATE, UniversalTag.INTEGER.getValue(), 
            new StoreSusec() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from susec value to error-code tag
        // --------------------------------------------------------------------------------------------
        // error-code      [6]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_SUSEC_STATE.ordinal()][KerberosConstants.KRB_ERROR_ERROR_CODE_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_SUSEC_STATE, KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_TAG_STATE, KerberosConstants.KRB_ERROR_ERROR_CODE_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from error-code tag to error-code value
        // --------------------------------------------------------------------------------------------
        // error-code      [6] Int32
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE, UniversalTag.INTEGER.getValue(), 
            new StoreErrorCode() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from error-code value to crealm tag
        // --------------------------------------------------------------------------------------------
        // crealm          [7]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE.ordinal()][KerberosConstants.KRB_ERROR_CREALM_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE, KrbErrorStatesEnum.KRB_ERR_CREALM_TAG_STATE, KerberosConstants.KRB_ERROR_CREALM_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from crealm tag to crealm value
        // --------------------------------------------------------------------------------------------
        // crealm          [7] Realm
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CREALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CREALM_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_CREALM_STATE, UniversalTag.GENERAL_STRING.getValue(), 
            new StoreCRealm() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from crealm value to cname
        // --------------------------------------------------------------------------------------------
        // cname           [8] PrincipalName OPTIONAL,
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CREALM_STATE.ordinal()][KerberosConstants.KRB_ERROR_CNAME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CREALM_STATE, KrbErrorStatesEnum.KRB_ERR_CNAME_STATE, KerberosConstants.KRB_ERROR_CNAME_TAG, 
            new StoreCName() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cname value to realm tag
        // --------------------------------------------------------------------------------------------
        // realm           [9]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CNAME_STATE.ordinal()][KerberosConstants.KRB_ERROR_REALM_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CNAME_STATE, KrbErrorStatesEnum.KRB_ERR_REALM_TAG_STATE, KerberosConstants.KRB_ERROR_REALM_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from realm tag to realm value
        // --------------------------------------------------------------------------------------------
        // realm           [9] Realm
        super.transitions[KrbErrorStatesEnum.KRB_ERR_REALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_REALM_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_REALM_STATE, UniversalTag.GENERAL_STRING.getValue(), 
            new StoreRealm() );

        // --------------------------------------------------------------------------------------------
        // Transition from realm value to sname 
        // --------------------------------------------------------------------------------------------
        // sname           [10] PrincipalName,
        super.transitions[KrbErrorStatesEnum.KRB_ERR_REALM_STATE.ordinal()][KerberosConstants.KRB_ERROR_SNAME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_REALM_STATE, KrbErrorStatesEnum.KRB_ERR_SNAME_STATE, KerberosConstants.KRB_ERROR_SNAME_TAG, 
            new StoreSName() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from sname value to etext tag 
        // --------------------------------------------------------------------------------------------
        // e-text          [11]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_SNAME_STATE.ordinal()][KerberosConstants.KRB_ERROR_ETEXT_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_SNAME_STATE, KrbErrorStatesEnum.KRB_ERR_ETEXT_TAG_STATE, KerberosConstants.KRB_ERROR_ETEXT_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from etext tag to etext value 
        // --------------------------------------------------------------------------------------------
        // e-text          [11] KerberosString OPTIONAL
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ETEXT_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ETEXT_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_ETEXT_STATE, UniversalTag.GENERAL_STRING.getValue(), 
            new StoreEText() );

        // --------------------------------------------------------------------------------------------
        // Transition from etext value to edata tag 
        // --------------------------------------------------------------------------------------------
        // e-data          [12]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ETEXT_STATE.ordinal()][KerberosConstants.KRB_ERROR_EDATA_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ETEXT_STATE, KrbErrorStatesEnum.KRB_ERR_EDATA_TAG_STATE, KerberosConstants.KRB_ERROR_EDATA_TAG, 
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from edata tag to edata value 
        // --------------------------------------------------------------------------------------------
        // e-data          [12] OCTET STRING OPTIONAL
        super.transitions[KrbErrorStatesEnum.KRB_ERR_EDATA_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_EDATA_TAG_STATE, KrbErrorStatesEnum.KRB_ERR_EDATA_STATE, UniversalTag.OCTET_STRING.getValue(), 
            new StoreEData() );
        
        
        // ----------------------------------------- OPTIONAL transitions -----------------------------------------
        
        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to cusec tag
        // --------------------------------------------------------------------------------------------
        // cusec           [3]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE.ordinal()][KerberosConstants.KRB_ERROR_CUSEC_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE, KrbErrorStatesEnum.KRB_ERR_CUSEC_TAG_STATE, KerberosConstants.KRB_ERROR_CUSEC_TAG, 
            new CheckNotNullLength());

        // --------------------------------------------------------------------------------------------
        // Transition from msg-type value to stime tag
        // --------------------------------------------------------------------------------------------
        // stime           [4]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE.ordinal()][KerberosConstants.KRB_ERROR_STIME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_MSG_TYPE_STATE, KrbErrorStatesEnum.KRB_ERR_STIME_TAG_STATE, KerberosConstants.KRB_ERROR_STIME_TAG, 
            new CheckNotNullLength());
        
        // --------------------------------------------------------------------------------------------
        // Transition from cTime value to stime tag
        // --------------------------------------------------------------------------------------------
        // stime           [4]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CTIME_STATE.ordinal()][KerberosConstants.KRB_ERROR_STIME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CTIME_STATE, KrbErrorStatesEnum.KRB_ERR_STIME_TAG_STATE, KerberosConstants.KRB_ERROR_STIME_TAG, 
            new CheckNotNullLength() );

        // from erro-code to realm

        // --------------------------------------------------------------------------------------------
        // Transition from error-code value to realm tag
        // --------------------------------------------------------------------------------------------
        // realm           [9]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE.ordinal()][KerberosConstants.KRB_ERROR_REALM_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE, KrbErrorStatesEnum.KRB_ERR_REALM_TAG_STATE, KerberosConstants.KRB_ERROR_REALM_TAG, 
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from error-code value to cname
        // --------------------------------------------------------------------------------------------
        // cname           [8] PrincipalName OPTIONAL,
        super.transitions[KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE.ordinal()][KerberosConstants.KRB_ERROR_CNAME_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_ERROR_CODE_STATE, KrbErrorStatesEnum.KRB_ERR_CNAME_STATE, KerberosConstants.KRB_ERROR_CNAME_TAG, 
            new StoreCName() );

        // --------------------------------------------------------------------------------------------
        // Transition from crealm value to realm tag
        // --------------------------------------------------------------------------------------------
        // realm           [9]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_CREALM_STATE.ordinal()][KerberosConstants.KRB_ERROR_REALM_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_CREALM_STATE, KrbErrorStatesEnum.KRB_ERR_REALM_TAG_STATE, KerberosConstants.KRB_ERROR_REALM_TAG, 
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from sname value to edata tag 
        // --------------------------------------------------------------------------------------------
        // e-data          [12]
        super.transitions[KrbErrorStatesEnum.KRB_ERR_SNAME_STATE.ordinal()][KerberosConstants.KRB_ERROR_EDATA_TAG] = new GrammarTransition(
            KrbErrorStatesEnum.KRB_ERR_SNAME_STATE, KrbErrorStatesEnum.KRB_ERR_EDATA_TAG_STATE, KerberosConstants.KRB_ERROR_EDATA_TAG, 
            new CheckNotNullLength() );
    }

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the KRB-ERROR Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
