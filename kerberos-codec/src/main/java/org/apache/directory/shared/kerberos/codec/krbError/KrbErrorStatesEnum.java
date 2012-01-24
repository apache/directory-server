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
package org.apache.directory.shared.kerberos.codec.krbError;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the KRB-ERROR grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KrbErrorStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- KRB-ERROR component --------------------------------------
    KRB_ERR_TAG, // 1

    KRB_ERR_SEQ_STATE, // 2

    KRB_ERR_PVNO_TAG_STATE, // 3
    KRB_ERR_PVNO_STATE, // 4

    KRB_ERR_MSG_TYPE_TAG_STATE, // 5
    KRB_ERR_MSG_TYPE_STATE, // 6

    KRB_ERR_CTIME_TAG_STATE, // 7
    KRB_ERR_CTIME_STATE, // 8

    KRB_ERR_CUSEC_TAG_STATE, // 9
    KRB_ERR_CUSEC_STATE, // 10

    KRB_ERR_STIME_TAG_STATE, // 11
    KRB_ERR_STIME_STATE, // 12

    KRB_ERR_SUSEC_TAG_STATE, // 13
    KRB_ERR_SUSEC_STATE, // 14

    KRB_ERR_ERROR_CODE_TAG_STATE, // 15
    KRB_ERR_ERROR_CODE_STATE, // 16

    KRB_ERR_CREALM_TAG_STATE, // 17
    KRB_ERR_CREALM_STATE, // 18

    KRB_ERR_CNAME_STATE, // 19

    KRB_ERR_REALM_TAG_STATE, // 20
    KRB_ERR_REALM_STATE, // 21

    KRB_ERR_SNAME_STATE, // 22

    KRB_ERR_ETEXT_TAG_STATE, // 23
    KRB_ERR_ETEXT_STATE, // 24

    KRB_ERR_EDATA_TAG_STATE, // 25
    KRB_ERR_EDATA_STATE, // 26

    // End
    LAST_KRB_ERR_STATE; // 27

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KRB_ERR_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KrbErrorContainer> grammar )
    {
        if ( grammar instanceof KrbErrorGrammar )
        {
            return "KRB_ERR_GRAMMAR";
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
        return ( ( state == LAST_KRB_ERR_STATE.ordinal() ) ? "LAST_KRB_ERR_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KRB_ERR_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KrbErrorStatesEnum getStartState()
    {
        return START_STATE;
    }
}
