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
package org.apache.directory.shared.kerberos.codec.encKrbCredPart;


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * This class stores the EncKrbCredPart grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum EncKrbCredPartStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- EncKrbPrivPart component --------------------------------------

    ENC_KRB_CRED_PART_TAG_STATE, // 1

    ENC_KRB_CRED_PART_SEQ_TAG_STATE, // 2

    ENC_KRB_CRED_PART_TICKET_INFO_TAG_STATE, // 3
    ENC_KRB_CRED_PART_TICKET_INFO_STATE, // 4

    ENC_KRB_CRED_PART_NONCE_TAG_STATE, // 5
    ENC_KRB_CRED_PART_NONCE_STATE, // 6

    ENC_KRB_CRED_PART_TIMESTAMP_TAG_STATE, // 7
    ENC_KRB_CRED_PART_TIMESTAMP_STATE, // 8

    ENC_KRB_CRED_PART_USEC_TAG_STATE, // 9
    ENC_KRB_CRED_PART_USEC_STATE, // 10

    ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG_STATE, // 11

    ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG_STATE, // 12

    // End
    LAST_ENC_KRB_CRED_PART_STATE; // 13

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "ENC_KRB_CRED_PART_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<EncKrbCredPartContainer> grammar )
    {
        if ( grammar instanceof EncKrbCredPartGrammar )
        {
            return "ENC_KRB_CRED_PART_GRAMMAR";
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
        return ( ( state == LAST_ENC_KRB_CRED_PART_STATE.ordinal() ) ? "LAST_ENC_KRB_CRED_PART_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_ENC_KRB_CRED_PART_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public EncKrbCredPartStatesEnum getStartState()
    {
        return START_STATE;
    }
}
