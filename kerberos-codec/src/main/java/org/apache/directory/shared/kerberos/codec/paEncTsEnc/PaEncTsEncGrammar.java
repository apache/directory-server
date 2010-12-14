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
package org.apache.directory.shared.kerberos.codec.paEncTsEnc;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.actions.PaEncTsEncInit;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.actions.StorePaTimestamp;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.actions.StorePaUsec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the PaEncTsEnc structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class PaEncTsEncGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( PaEncTsEncGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. PaEncTsEncGrammar is a singleton */
    private static Grammar instance = new PaEncTsEncGrammar();


    /**
     * Creates a new PaEncTsEncrGrammar object.
     */
    private PaEncTsEncGrammar()
    {
        setName( PaEncTsEncGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[PaEncTsEncStatesEnum.LAST_PA_ENC_TS_ENC_STATE.ordinal()][256];

        // ============================================================================================
        // PaEncTsEnc 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from PaEncTsEnc init to PaEncTsEnc SEQ
        // --------------------------------------------------------------------------------------------
        // PA-ENC-TS-ENC           ::= SEQUENCE {
        super.transitions[PaEncTsEncStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            PaEncTsEncStatesEnum.START_STATE, PaEncTsEncStatesEnum.PA_ENC_TS_ENC_STATE, UniversalTag.SEQUENCE.getValue(),
            new PaEncTsEncInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from PaEncTsEnc SEQ to patimestamp tag
        // --------------------------------------------------------------------------------------------
        // PA-ENC-TS-ENC           ::= SEQUENCE {
        //         patimestamp     [0]
        super.transitions[PaEncTsEncStatesEnum.PA_ENC_TS_ENC_STATE.ordinal()][KerberosConstants.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG] = new GrammarTransition(
            PaEncTsEncStatesEnum.PA_ENC_TS_ENC_STATE, PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG_STATE, KerberosConstants.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from patimestamp tag to patimestamp  value
        // --------------------------------------------------------------------------------------------
        // PA-ENC-TS-ENC           ::= SEQUENCE {
        //         patimestamp     [0] KerberosTime -- client's time --,
        super.transitions[PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_TIMESTAMP_TAG_STATE, PaEncTsEncStatesEnum.PA_ENC_TS_PA_TIMESTAMP_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StorePaTimestamp() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from patimestamp value to pausec tag
        // --------------------------------------------------------------------------------------------
        // PA-ENC-TS-ENC           ::= SEQUENCE {
        //         ...
        //         pausec          [1]
        super.transitions[PaEncTsEncStatesEnum.PA_ENC_TS_PA_TIMESTAMP_STATE.ordinal()][KerberosConstants.PA_ENC_TS_ENC_PA_USEC_TAG] = new GrammarTransition(
            PaEncTsEncStatesEnum.PA_ENC_TS_PA_TIMESTAMP_STATE, PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_USEC_TAG_STATE, KerberosConstants.PA_ENC_TS_ENC_PA_USEC_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from pausec tag to pausec value
        // --------------------------------------------------------------------------------------------
        // PA-ENC-TS-ENC           ::= SEQUENCE {
        //         ...
        //         pausec          [1] Microseconds OPTIONAL
        // }
        super.transitions[PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_USEC_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            PaEncTsEncStatesEnum.PA_ENC_TS_ENC_PA_USEC_TAG_STATE, PaEncTsEncStatesEnum.    PA_ENC_TS_ENC_PA_USEC_STATE, UniversalTag.INTEGER.getValue(),
            new StorePaUsec() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the PA-ENC-TS-ENC Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
