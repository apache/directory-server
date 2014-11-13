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
package org.apache.directory.shared.kerberos.codec.kdcRep;


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * This class store the KDC-REP grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KdcRepStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- KDC-REP component --------------------------------------
    KDC_REP_SEQ_STATE, // 1

    KDC_REP_PVNO_TAG_STATE, // 2
    KDC_REP_PVNO_STATE, // 3

    KDC_REP_MSG_TYPE_TAG_STATE, // 4
    KDC_REP_MSG_TYPE_STATE, // 5

    KDC_REP_PA_DATA_TAG_STATE, // 6
    KDC_REP_PA_DATA_STATE, // 7

    KDC_REP_CREALM_TAG_STATE, // 8
    KDC_REP_CREALM_STATE, // 9

    KDC_REP_CNAME_STATE, // 10

    KDC_REP_TICKET_STATE, // 11

    KDC_REP_ENC_PART_STATE, // 12

    // End
    LAST_KDC_REP_STATE; // 13

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KDC_REP_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KdcRepContainer> grammar )
    {
        if ( grammar instanceof KdcRepGrammar )
        {
            return "KDC_REP_GRAMMAR";
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
        return ( ( state == LAST_KDC_REP_STATE.ordinal() ) ? "KDC_REP_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KDC_REP_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KdcRepStatesEnum getStartState()
    {
        return START_STATE;
    }
}
