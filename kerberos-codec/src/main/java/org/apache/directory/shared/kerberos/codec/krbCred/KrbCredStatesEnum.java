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
package org.apache.directory.shared.kerberos.codec.krbCred;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the KrbCred grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KrbCredStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- KRB-ERROR component --------------------------------------
    KRB_CRED_TAG_STATE, // 1

    KRB_CRED_SEQ_TAG_STATE, // 2

    KRB_CRED_PVNO_TAG_STATE, // 3
    KRB_CRED_PVNO_STATE, // 4

    KRB_CRED_MSGTYPE_TAG_STATE, // 5
    KRB_CRED_MSGTYPE_STATE, // 6

    KRB_CRED_TICKETS_TAG_STATE, // 7
    KRB_CRED_TICKETS_STATE, // 8

    KRB_CRED_ENCPART_TAG_STATE, // 9

    // End
    LAST_KRB_CRED_STATE; // 10

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KRB_CRED_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KrbCredContainer> grammar )
    {
        if ( grammar instanceof KrbCredGrammar )
        {
            return "KRB_CRED_GRAMMAR";
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
        return ( ( state == LAST_KRB_CRED_STATE.ordinal() ) ? "LAST_KRB_CRED_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KRB_CRED_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KrbCredStatesEnum getStartState()
    {
        return START_STATE;
    }
}
