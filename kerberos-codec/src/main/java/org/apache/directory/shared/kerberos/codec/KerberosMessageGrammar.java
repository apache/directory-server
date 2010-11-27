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
package org.apache.directory.shared.kerberos.codec;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.actions.CheckNotNullLength;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreRealm;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreSName;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreTktVno;
import org.apache.directory.shared.kerberos.codec.ticket.actions.TicketInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KerberosMessage message. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KerberosMessageGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KerberosMessageGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KerberosMessageGrammar is a singleton */
    private static Grammar instance = new KerberosMessageGrammar();


    /**
     * Creates a new KerberosMessageGrammar object.
     */
    private KerberosMessageGrammar()
    {
        setName( KerberosMessageGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KerberosStatesEnum.LAST_KERBEROS_STATE.ordinal()][256];

        // ============================================================================================
        // Ticket 
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from START to Ticket
        // --------------------------------------------------------------------------------------------
        // This is the starting state :
        // Ticket          ::= [APPLICATION 1] ... 
        super.transitions[KerberosStatesEnum.START_STATE.ordinal()][KerberosConstants.TICKET_TAG] = new GrammarTransition(
            KerberosStatesEnum.START_STATE, KerberosStatesEnum.TICKET_STATE, KerberosConstants.TICKET_TAG,
            new TicketInit() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from Ticket to Ticket-SEQ
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        super.transitions[KerberosStatesEnum.TICKET_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] = new GrammarTransition(
            KerberosStatesEnum.TICKET_STATE, KerberosStatesEnum.TICKET_SEQ_STATE, UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from Ticket-SEQ to tkt-vno tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         tkt-vno         [0] 
        super.transitions[KerberosStatesEnum.TICKET_SEQ_STATE.ordinal()][KerberosConstants.TICKET_TKT_VNO_TAG] = new GrammarTransition(
            KerberosStatesEnum.TICKET_SEQ_STATE, KerberosStatesEnum.TICKET_VNO_TAG_STATE, KerberosConstants.TICKET_TKT_VNO_TAG,
            new CheckNotNullLength() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from tkt-vno tag to tkt-vno 
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         tkt-vno         [0] INTEGER (5),
        super.transitions[KerberosStatesEnum.TICKET_VNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] = new GrammarTransition(
            KerberosStatesEnum.TICKET_VNO_TAG_STATE, KerberosStatesEnum.TICKET_VNO_STATE, UniversalTag.INTEGER.getValue(),
            new StoreTktVno() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from tkt-vno to realm tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         tkt-vno         [0] INTEGER (5), 
        //         realm           [1]
        super.transitions[KerberosStatesEnum.TICKET_VNO_STATE.ordinal()][KerberosConstants.TICKET_REALM_TAG] = new GrammarTransition(
            KerberosStatesEnum.TICKET_VNO_STATE, KerberosStatesEnum.TICKET_REALM_TAG_STATE, KerberosConstants.TICKET_REALM_TAG,
            new CheckNotNullLength() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from realm tag to realm value
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         tkt-vno         [0] INTEGER (5),
        //         realm           [1] Realm,
        super.transitions[KerberosStatesEnum.TICKET_REALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] = new GrammarTransition(
            KerberosStatesEnum.TICKET_REALM_TAG_STATE, KerberosStatesEnum.TICKET_REALM_STATE, UniversalTag.GENERAL_STRING.getValue(),
            new StoreRealm() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from realm value to sname tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         tkt-vno         [0] INTEGER (5),
        //         realm           [1] Realm,
        //         sname           [2] 
        super.transitions[KerberosStatesEnum.TICKET_REALM_STATE.ordinal()][KerberosConstants.TICKET_SNAME_TAG] = new GrammarTransition(
            KerberosStatesEnum.TICKET_REALM_STATE, KerberosStatesEnum.TICKET_SNAME_TAG_STATE, KerberosConstants.TICKET_SNAME_TAG,
            new StoreSName() );


        // --------------------------------------------------------------------------------------------
        // Transition from sname tag to enc-part tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         ...
        //         sname           [2] PrincipalName,
        //         enc-part        [3]
        // 
        super.transitions[KerberosStatesEnum.TICKET_SNAME_TAG_STATE.ordinal()][KerberosConstants.TICKET_ENC_PART_TAG] = new GrammarTransition(
            KerberosStatesEnum.TICKET_SNAME_TAG_STATE, KerberosStatesEnum.TICKET_ENC_PART_TAG_STATE, KerberosConstants.TICKET_ENC_PART_TAG,
            new CheckNotNullLength() );

        
        // --------------------------------------------------------------------------------------------
        // Transition from enc-part tag to enc-part value
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE { 
        //         ...
        //         enc-part        [3] EncryptedData
        // 
        super.transitions[KerberosStatesEnum.TICKET_SNAME_TAG_STATE.ordinal()][KerberosConstants.TICKET_ENC_PART_TAG] = new GrammarTransition(
            KerberosStatesEnum.TICKET_SNAME_TAG_STATE, KerberosStatesEnum.TICKET_ENC_PART_TAG_STATE, KerberosConstants.TICKET_ENC_PART_TAG,
            new StoreEncPart() );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     * 
     * @return An instance on the KerberosMessage Grammar
     */
    public static Grammar getInstance()
    {
        return instance;
    }
}
