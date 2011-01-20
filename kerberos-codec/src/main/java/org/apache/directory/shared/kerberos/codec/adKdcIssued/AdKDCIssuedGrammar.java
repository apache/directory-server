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
package org.apache.directory.shared.kerberos.codec.adKdcIssued;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.AdKdcIssuedInit;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.StoreChecksum;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.StoreElements;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.StoreIRealm;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.StoreISName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AdKdcIssued structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AdKDCIssuedGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AdKDCIssuedGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AdKdcIssuedGrammar is a singleton */
    private static Grammar instance = new AdKDCIssuedGrammar();


    /**
     * Creates a new AdKdcIssuedGrammar object.
     */
    private AdKDCIssuedGrammar()
    {
        setName( AdKDCIssuedGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AdKDCIssuedStatesEnum.LAST_AD_KDC_ISSUED_STATE.ordinal()][256];

        // ============================================================================================
        // AdKdcIssued 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AdKdcIssued init to SEQ
        // --------------------------------------------------------------------------------------------
        // AD-KDCIssued            ::= SEQUENCE {
        super.transitions[AdKDCIssuedStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            AdKDCIssuedStatesEnum.START_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new AdKdcIssuedInit() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_SEQ_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_AD_CHECKSUM_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_SEQ_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_AD_CHECKSUM_TAG,
            new StoreChecksum() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_I_REALM_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_I_REALM_TAG,
            new CheckNotNullLength() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_I_SNAME_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_SNAME_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_I_SNAME_TAG,
            new StoreISName() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_AD_CHECKSUM_TAG_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_ELEMENTS_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG,
            new StoreElements() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_TAG_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_STATE, UniversalTag.GENERAL_STRING.getValue(),
            new StoreIRealm() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_I_SNAME_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_SNAME_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_I_SNAME_TAG,
            new StoreISName() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_REALM_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_ELEMENTS_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG,
            new StoreElements() );
        
        super.transitions[AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_SNAME_TAG_STATE.ordinal()][KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG] = new GrammarTransition(
            AdKDCIssuedStatesEnum.AD_KDC_ISSUED_I_SNAME_TAG_STATE, AdKDCIssuedStatesEnum.AD_KDC_ISSUED_ELEMENTS_TAG_STATE, KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG,
            new StoreElements() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the AdKdcIssued Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
