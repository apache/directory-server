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
package org.apache.directory.shared.kerberos.codec.ticket;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreRealm;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreSName;
import org.apache.directory.shared.kerberos.codec.ticket.actions.StoreTktVno;
import org.apache.directory.shared.kerberos.codec.ticket.actions.TicketInit;


/**
 * This class implements the Ticket message. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TicketGrammar extends AbstractGrammar<TicketContainer>
{
    /** The instance of grammar. TicketGrammar is a singleton */
    private static Grammar<TicketContainer> instance = new TicketGrammar();


    /**
     * Creates a new TicketGrammar object.
     */
    @SuppressWarnings("unchecked")
    private TicketGrammar()
    {
        setName( TicketGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[TicketStatesEnum.LAST_TICKET_STATE.ordinal()][256];

        // ============================================================================================
        // Ticket
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from START to Ticket
        // --------------------------------------------------------------------------------------------
        // This is the starting state :
        // Ticket          ::= [APPLICATION 1] ...
        super.transitions[TicketStatesEnum.START_STATE.ordinal()][KerberosConstants.TICKET_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.START_STATE,
                TicketStatesEnum.TICKET_STATE,
                KerberosConstants.TICKET_TAG,
                new TicketInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from Ticket to Ticket-SEQ
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        super.transitions[TicketStatesEnum.TICKET_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_STATE,
                TicketStatesEnum.TICKET_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<TicketContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from Ticket-SEQ to tkt-vno tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         tkt-vno         [0]
        super.transitions[TicketStatesEnum.TICKET_SEQ_STATE.ordinal()][KerberosConstants.TICKET_TKT_VNO_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_SEQ_STATE,
                TicketStatesEnum.TICKET_VNO_TAG_STATE,
                KerberosConstants.TICKET_TKT_VNO_TAG,
                new CheckNotNullLength<TicketContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from tkt-vno tag to tkt-vno
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         tkt-vno         [0] INTEGER (5),
        super.transitions[TicketStatesEnum.TICKET_VNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_VNO_TAG_STATE,
                TicketStatesEnum.TICKET_VNO_STATE,
                UniversalTag.INTEGER,
                new StoreTktVno() );

        // --------------------------------------------------------------------------------------------
        // Transition from tkt-vno to realm tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         tkt-vno         [0] INTEGER (5),
        //         realm           [1]
        super.transitions[TicketStatesEnum.TICKET_VNO_STATE.ordinal()][KerberosConstants.TICKET_REALM_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_VNO_STATE,
                TicketStatesEnum.TICKET_REALM_TAG_STATE,
                KerberosConstants.TICKET_REALM_TAG,
                new CheckNotNullLength<TicketContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from realm tag to realm value
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         tkt-vno         [0] INTEGER (5),
        //         realm           [1] Realm,
        super.transitions[TicketStatesEnum.TICKET_REALM_TAG_STATE.ordinal()][UniversalTag.GENERAL_STRING.getValue()] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_REALM_TAG_STATE,
                TicketStatesEnum.TICKET_REALM_STATE,
                UniversalTag.GENERAL_STRING,
                new StoreRealm() );

        // --------------------------------------------------------------------------------------------
        // Transition from realm value to sname tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         tkt-vno         [0] INTEGER (5),
        //         realm           [1] Realm,
        //         sname           [2]
        super.transitions[TicketStatesEnum.TICKET_REALM_STATE.ordinal()][KerberosConstants.TICKET_SNAME_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_REALM_STATE,
                TicketStatesEnum.TICKET_SNAME_TAG_STATE,
                KerberosConstants.TICKET_SNAME_TAG,
                new StoreSName() );

        // --------------------------------------------------------------------------------------------
        // Transition from sname tag to enc-part tag
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         ...
        //         sname           [2] PrincipalName,
        //         enc-part        [3]
        //
        super.transitions[TicketStatesEnum.TICKET_SNAME_TAG_STATE.ordinal()][KerberosConstants.TICKET_ENC_PART_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_SNAME_TAG_STATE,
                TicketStatesEnum.TICKET_ENC_PART_TAG_STATE,
                KerberosConstants.TICKET_ENC_PART_TAG,
                new CheckNotNullLength<TicketContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from enc-part tag to enc-part value
        // --------------------------------------------------------------------------------------------
        // Ticket          ::= [APPLICATION 1] SEQUENCE {
        //         ...
        //         enc-part        [3] EncryptedData
        //
        super.transitions[TicketStatesEnum.TICKET_SNAME_TAG_STATE.ordinal()][KerberosConstants.TICKET_ENC_PART_TAG] =
            new GrammarTransition<TicketContainer>(
                TicketStatesEnum.TICKET_SNAME_TAG_STATE,
                TicketStatesEnum.TICKET_ENC_PART_TAG_STATE,
                KerberosConstants.TICKET_ENC_PART_TAG,
                new StoreEncPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the Ticket Grammar
     */
    public static Grammar<TicketContainer> getInstance()
    {
        return instance;
    }
}
