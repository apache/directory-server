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
package org.apache.directory.shared.kerberos.codec.authenticator;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the Authenticator grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum AuthenticatorStatesEnum implements States
{
    // Start
    START_STATE,                                // 0

    // ----- AUTHENTICATOR message --------------------------------------
    AUTHENTICATOR_STATE,                        // 1
    AUTHENTICATOR_SEQ_STATE,                    // 2

    AUTHENTICATOR_AUTHENTICATOR_VNO_TAG_STATE,  // 3
    AUTHENTICATOR_AUTHENTICATOR_VNO_STATE,      // 4

    AUTHENTICATOR_CREALM_TAG_STATE,             // 5
    AUTHENTICATOR_CREALM_STATE,                 // 6

    AUTHENTICATOR_CNAME_STATE,                  // 7

    AUTHENTICATOR_CKSUM_STATE,                  // 8

    AUTHENTICATOR_CUSEC_TAG_STATE,              // 9
    AUTHENTICATOR_CUSEC_STATE,                  // 10

    AUTHENTICATOR_CTIME_TAG_STATE,              // 11
    AUTHENTICATOR_CTIME_STATE,                  // 12

    AUTHENTICATOR_SUBKEY_STATE,                 // 13

    AUTHENTICATOR_SEQ_NUMBER_TAG_STATE,         // 14
    AUTHENTICATOR_SEQ_NUMBER_STATE,             // 15

    AUTHENTICATOR_AUTHORIZATION_DATA_STATE,     // 16

    // End
    LAST_AUTHENTICATOR_STATE;                   // 17


    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "AUTHENTICATOR_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<AuthenticatorContainer> grammar )
    {
        if ( grammar instanceof AuthenticatorGrammar )
        {
            return "AUTHENTICATOR_GRAMMAR";
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
        return ( ( state == LAST_AUTHENTICATOR_STATE.ordinal() ) ? "AUTHENTICATOR_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_AUTHENTICATOR_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public AuthenticatorStatesEnum getStartState()
    {
        return START_STATE;
    }
}
