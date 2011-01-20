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
package org.apache.directory.shared.kerberos.codec.adAndOr;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.adAndOr.actions.AdAndOrInit;
import org.apache.directory.shared.kerberos.codec.adAndOr.actions.StoreConditionCount;
import org.apache.directory.shared.kerberos.codec.adAndOr.actions.StoreElements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AdAndOr structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AdAndOrGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AdAndOrGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AdAndOrGrammar is a singleton */
    private static Grammar instance = new AdAndOrGrammar();


    /**
     * Creates a new AdAndOrGrammar object.
     */
    private AdAndOrGrammar()
    {
        setName( AdAndOrGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AdAndOrStatesEnum.LAST_AD_AND_OR_STATE.ordinal()][256];

        // ============================================================================================
        // AdAndOr 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AdAndOr init to AdAndOr SEQ
        // --------------------------------------------------------------------------------------------
        // AD-AND-OR               ::= SEQUENCE {
        super.transitions[AdAndOrStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            AdAndOrStatesEnum.START_STATE, AdAndOrStatesEnum.AD_AND_OR_STATE, UniversalTag.SEQUENCE.getValue(),
            new AdAndOrInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from AdAndOr SEQ to condition-count tag
        // --------------------------------------------------------------------------------------------
        // AD-AND-OR               ::= SEQUENCE {
        //         condition-count [0]
        super.transitions[AdAndOrStatesEnum.AD_AND_OR_STATE.ordinal()][KerberosConstants.AD_AND_OR_CONDITION_COUNT_TAG] = new GrammarTransition(
            AdAndOrStatesEnum.AD_AND_OR_STATE, AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_TAG_STATE, KerberosConstants.AD_AND_OR_CONDITION_COUNT_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from condition-count tag to condition-count value
        // --------------------------------------------------------------------------------------------
        // AD-AND-OR               ::= SEQUENCE {
        //         condition-count [0] Int32,
        super.transitions[AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_TAG_STATE, AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_STATE, UniversalTag.INTEGER.getValue(),
            new StoreConditionCount() );

        // --------------------------------------------------------------------------------------------
        // Transition from condition-countvalue to elements
        // --------------------------------------------------------------------------------------------
        // AD-AND-OR               ::= SEQUENCE {
        //         ...
        //         elements        [1] AuthorizationData
        // }
        super.transitions[AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_STATE.ordinal()][KerberosConstants.AD_AND_OR_ELEMENTS_TAG] = new GrammarTransition(
            AdAndOrStatesEnum.AD_AND_OR_CONDITION_COUNT_STATE, AdAndOrStatesEnum.AD_AND_OR_ELEMENTS_TAG_STATE, KerberosConstants.AD_AND_OR_ELEMENTS_TAG,
            new StoreElements() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the AD-AND-OR Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
