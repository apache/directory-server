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
package org.apache.directory.shared.kerberos.codec.encApRepPart;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encApRepPart.actions.EncApRepPartInit;
import org.apache.directory.shared.kerberos.codec.encApRepPart.actions.StoreCTime;
import org.apache.directory.shared.kerberos.codec.encApRepPart.actions.StoreCusec;
import org.apache.directory.shared.kerberos.codec.encApRepPart.actions.StoreSeqNumber;
import org.apache.directory.shared.kerberos.codec.encApRepPart.actions.StoreSubKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncApRepPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncApRepPartGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncApRepPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncApRepPartGrammar is a singleton */
    private static Grammar instance = new EncApRepPartGrammar();


    /**
     * Creates a new EncApRepPartGrammar object.
     */
    private EncApRepPartGrammar()
    {
        setName( EncApRepPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncApRepPartStatesEnum.LAST_ENC_AP_REP_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncApRepPart 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncApRepPart init to APPLICATION tag
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27]
        super.transitions[EncApRepPartStatesEnum.START_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.START_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_STATE, KerberosConstants.ENC_AP_REP_PART_TAG,
            new EncApRepPartInit() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from EncApRepPart APPLICATION to EncApRepPart SEQ
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from EncApRepPart SEQ to ctime tag
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ctime           [0]
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_CTIME_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_TAG_STATE, KerberosConstants.ENC_AP_REP_PART_CTIME_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from ctime tag to ctime value
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ctime           [0] KerberosTime,
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_TAG_STATE.ordinal()][UniversalTag.GENERALIZED_TIME.getValue()] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_TAG_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_STATE, UniversalTag.GENERALIZED_TIME.getValue(),
            new StoreCTime() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from ctime value to cusec tag
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         cusec           [1]
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_CUSEC_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_CTIME_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_TAG_STATE, KerberosConstants.ENC_AP_REP_PART_CUSEC_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cusec tag to cusec value
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         cusec           [1] Microseconds,
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_TAG_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_STATE, UniversalTag.INTEGER.getValue(),
            new StoreCusec() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cusec value to subkey
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         subkey          [2] <EncryptionKey> OPTIONAL,
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_SUB_KEY_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_SUBKEY_STATE, KerberosConstants.ENC_AP_REP_PART_SUB_KEY_TAG,
            new StoreSubKey() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from cusec value to seq-number tag
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         seq-number      [3] UInt32 OPTIONAL
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_SEQ_NUMBER_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_CUSEC_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_NUMBER_TAG_STATE, KerberosConstants.ENC_AP_REP_PART_SEQ_NUMBER_TAG,
            new CheckNotNullLength() );
        
        // --------------------------------------------------------------------------------------------
        // Transition from subkey to seq-number tag
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         seq-number      [3] UInt32 OPTIONAL
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_SUBKEY_STATE.ordinal()][KerberosConstants.ENC_AP_REP_PART_SEQ_NUMBER_TAG] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_SUBKEY_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_NUMBER_TAG_STATE, KerberosConstants.ENC_AP_REP_PART_SEQ_NUMBER_TAG,
            new CheckNotNullLength() );

        // --------------------------------------------------------------------------------------------
        // Transition from seq-number tag to seq-number value
        // --------------------------------------------------------------------------------------------
        // EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
        //         ...
        //         seq-number      [3] UInt32 OPTIONAL
        // }
        super.transitions[EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_NUMBER_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_NUMBER_TAG_STATE, EncApRepPartStatesEnum.ENC_AP_REP_PART_SEQ_NUMBER_STATE, UniversalTag.INTEGER.getValue(),
            new StoreSeqNumber() );
    }


    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the EncApRepPart Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
