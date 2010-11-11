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
package org.apache.directory.shared.kerberos.codec.kdcReqBody;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.actions.KdcReqBodyInit;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.actions.StoreCName;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.actions.StoreKdcOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KdcReqBody structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KdcReqBodyGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KdcReqBodyGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KdcReqBodyGrammar is a singleton */
    private static Grammar instance = new KdcReqBodyGrammar();


    /**
     * Creates a new KdcReqBodyGrammar object.
     */
    private KdcReqBodyGrammar()
    {
        setName( KdcReqBodyGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KdcReqBodyStatesEnum.LAST_KRB_REQ_BODY_STATE.ordinal()][256];

        // ============================================================================================
        // KdcReqBody 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from KdcReqBody init to KdcReqBody SEQ
        // --------------------------------------------------------------------------------------------
        // KDC-REQ-BODY    ::= SEQUENCE {
        super.transitions[KdcReqBodyStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            KdcReqBodyStatesEnum.START_STATE, KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_TAG_STATE, UniversalTag.SEQUENCE.getValue(),
            new KdcReqBodyInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from KdcReqBody SEQ to kdc-options tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ-BODY    ::= SEQUENCE {
        //         kdc-options             [0]
        super.transitions[KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_TAG_STATE.ordinal()][KerberosConstants.KDC_REQ_BODY_KDC_OPTIONS_TAG] = new GrammarTransition(
            KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_TAG_STATE, KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_STATE, KerberosConstants.KDC_REQ_BODY_KDC_OPTIONS_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from kdc-options tag to kdc-options value
        // --------------------------------------------------------------------------------------------
        // KDC-REQ-BODY    ::= SEQUENCE {
        //         kdc-options             [0] KDCOptions
        super.transitions[KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_STATE.ordinal()][UniversalTag.BIT_STRING.getValue()] = new GrammarTransition(
            KdcReqBodyStatesEnum.KRB_REQ_BODY_KDC_OPTIONS_STATE, KdcReqBodyStatesEnum.KRB_REQ_BODY_CNAME_TAG_STATE, UniversalTag.BIT_STRING.getValue(),
            new StoreKdcOptions() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from kdc-options value to cname tag
        // --------------------------------------------------------------------------------------------
        // KDC-REQ-BODY    ::= SEQUENCE {
        //         ...
        //         cname                   [1]
        super.transitions[KdcReqBodyStatesEnum.KRB_REQ_BODY_CNAME_TAG_STATE.ordinal()][KerberosConstants.KDC_REQ_BODY_CNAME_TAG] = new GrammarTransition(
            KdcReqBodyStatesEnum.KRB_REQ_BODY_CNAME_TAG_STATE, KdcReqBodyStatesEnum.KRB_REQ_BODY_REALM_TAG_STATE, KerberosConstants.KDC_REQ_BODY_CNAME_TAG,
            new StoreCName() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from kdc-options value to realm tag (cname is empty)
        // --------------------------------------------------------------------------------------------
        // KDC-REQ-BODY    ::= SEQUENCE {
        //         ...
        //         realm                   [2]
        super.transitions[KdcReqBodyStatesEnum.KRB_REQ_BODY_CNAME_TAG_STATE.ordinal()][KerberosConstants.KDC_REQ_BODY_REALM_TAG] = new GrammarTransition(
            KdcReqBodyStatesEnum.KRB_REQ_BODY_CNAME_TAG_STATE, KdcReqBodyStatesEnum.KRB_REQ_BODY_REALM_TAG_STATE, KerberosConstants.KDC_REQ_BODY_REALM_TAG,
            new CheckNotNullLength() );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the KDC-REQ-BODY Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
