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
package org.apache.directory.shared.kerberos.codec.krbSafeBody;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class stores the KRB-SAFE-BODY grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KrbSafeBodyStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- KRB-ERROR component --------------------------------------
    KRB_SAFE_BODY_SEQ_TAG_STATE, // 1

    KRB_SAFE_BODY_USER_DATA_TAG_STATE, // 2
    KRB_SAFE_BODY_USER_DATA_STATE, // 3

    KRB_SAFE_BODY_TIMESTAMP_TAG_STATE, // 4
    KRB_SAFE_BODY_TIMESTAMP_STATE, // 5

    KRB_SAFE_BODY_USEC_TAG_STATE, // 6
    KRB_SAFE_BODY_USEC_STATE, // 7

    KRB_SAFE_BODY_SEQ_NUMBER_TAG_STATE, // 8
    KRB_SAFE_BODY_SEQ_NUMBER_STATE, // 9

    KRB_SAFE_BODY_SENDER_ADDRESS_TAG_STATE, // 10

    KRB_SAFE_BODY_RECIPIENT_ADDRESS_TAG_STATE, // 11

    // End
    LAST_KRB_SAFE_BODY_STATE; // 12

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KRB_SAFE_BODY_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KrbSafeBodyContainer> grammar )
    {
        if ( grammar instanceof KrbSafeBodyGrammar )
        {
            return "KRB_SAFE_BODY_GRAMMAR";
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
        return ( ( state == LAST_KRB_SAFE_BODY_STATE.ordinal() ) ? "LAST_KRB_SAFE_BODY_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KRB_SAFE_BODY_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KrbSafeBodyStatesEnum getStartState()
    {
        return START_STATE;
    }
}
