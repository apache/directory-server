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
package org.apache.directory.shared.kerberos.codec.encTicketPart;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the EncTicketPart grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum EncTicketPartStatesEnum implements States
{
    // Start
    START_STATE,                        // 0

    ENC_TICKET_PART_TAG_STATE,          // 1

    ENC_TICKET_PART_SEQ_STATE,          // 2

    ENC_TICKET_PART_FLAGS_TAG_STATE,    // 3
    ENC_TICKET_PART_FLAGS_STATE,        // 4

    ENC_TICKET_PART_KEY_TAG_STATE,      // 5

    ENC_TICKET_PART_CREALM_TAG_STATE,   // 6
    ENC_TICKET_PART_CREALM_STATE,       // 7

    ENC_TICKET_PART_CNAME_TAG_STATE,    // 8

    ENC_TICKET_PART_TRANSITED_TAG_STATE,// 9

    ENC_TICKET_PART_AUTHTIME_TAG_STATE, // 10
    ENC_TICKET_PART_AUTHTIME_STATE,     // 11

    ENC_TICKET_PART_STARTTIME_TAG_STATE,// 12
    ENC_TICKET_PART_STARTTIME_STATE,    // 13

    ENC_TICKET_PART_ENDTIME_TAG_STATE,  // 14
    ENC_TICKET_PART_ENDTIME_STATE,      // 15

    ENC_TICKET_PART_RENEWTILL_TAG_STATE,// 16
    ENC_TICKET_PART_RENEWTILL_STATE,    // 17

    ENC_TICKET_PART_CADDR_TAG_STATE,    // 18

    ENC_TICKET_PART_AUTHZ_DATA_TAG_STATE,// 19

    // End
    LAST_ENC_TICKET_PART_STATE;         // 20


    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "ENC_TICKET_PART_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<EncTicketPartContainer> grammar )
    {
        if ( grammar instanceof EncTicketPartGrammar )
        {
            return "ENC_TICKET_PART_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
    }


    /**
     * Get the string representing the state
     *
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == LAST_ENC_TICKET_PART_STATE.ordinal() ) ? "LAST_ENC_TICKET_PART_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_ENC_TICKET_PART_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public EncTicketPartStatesEnum getStartState()
    {
        return START_STATE;
    }
}
