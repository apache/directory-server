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


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * This class store the Kerberos grammar's constants. It is also used for debugging
 * purpose. We will decode all the Kerberos messages :
 * <ul>
 * <li>0x6A : AS-REQ</li>
 * <li>0x6B : AS-REP</li>
 * <li>0x6C : TGS-REQ</li>
 * <li>0x6D : TGS-REP</li>
 * <li>0x6E : AP-REQ</li>
 * <li>0x6F : AP-REP</li>
 * <li>0x74 : KRB-SAFE</li>
 * <li>0x75 : KRB-PRIV</li>
 * <li>0x76 : KRB-CRED</li>
 * <li>0x7E : KRB-ERROR</li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KerberosMessageStatesEnum implements States
{
    // Start
    START_STATE,

    // ----- Kerberos message --------------------------------------------

    AS_REQ_STATE, // 0x6A
    AS_REP_TAG_STATE, // 0x6B
    TGS_REQ_TAG_STATE, // 0x6C
    TGS_REP_TAG_STATE, // 0x6D
    AP_REQ_TAG_STATE, // 0x6E
    AP_REP_TAG_STATE, // 0x6F
    KRB_SAFE_STATE, // 0x74
    KRB_PRIV_STATE, // 0x75
    KRB_CRED_STATE, // 0x76
    KRB_ERROR_STATE, // 0x7E

    // End
    LAST_KERBEROS_MESSAGE_STATE;

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KERBEROS_MESSAGE_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KerberosMessageContainer> grammar )
    {
        if ( grammar instanceof KerberosMessageGrammar )
        {
            return "KERBEROS_MESSAGE_GRAMMAR";
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
        return ( ( state == LAST_KERBEROS_MESSAGE_STATE.ordinal() ) ? "KERBEROS_MESSAGE_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KERBEROS_MESSAGE_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KerberosMessageStatesEnum getStartState()
    {
        return START_STATE;
    }
}
