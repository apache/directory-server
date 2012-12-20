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
package org.apache.directory.shared.kerberos.codec.kdcReqBody;


import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.States;


/**
 * This class store the KDC-REQ-BODY grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum KdcReqBodyStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- KDC-REQ-BODY message --------------------------------------
    KDC_REQ_BODY_SEQ_STATE, // 1

    KDC_REQ_BODY_KDC_OPTIONS_TAG_STATE, // 2
    KDC_REQ_BODY_KDC_OPTIONS_STATE, // 3

    KDC_REQ_BODY_CNAME_OR_REALM_TAG_STATE, // 4
    KDC_REQ_BODY_CNAME_STATE, // 5

    KDC_REQ_BODY_REALM_TAG_STATE, // 6

    KDC_REQ_BODY_SNAME_OR_FROM_OR_TILL_TAG_STATE, // 7
    KDC_REQ_BODY_SNAME_STATE, // 8

    KDC_REQ_BODY_FROM_STATE, // 9

    KDC_REQ_BODY_TILL_TAG_STATE, // 10
    KDC_REQ_BODY_TILL_STATE, // 11

    KDC_REQ_BODY_RTIME_OR_NONCE_TAG_STATE, // 12
    KDC_REQ_BODY_RTIME_STATE, // 13

    KDC_REQ_BODY_NONCE_TAG_STATE, // 14
    KDC_REQ_BODY_NONCE_STATE, // 15

    KDC_REQ_BODY_ETYPE_TAG_STATE, // 16
    KDC_REQ_BODY_ETYPE_SEQ_STATE, // 17
    KDC_REQ_BODY_ETYPE_STATE, // 18

    KDC_REQ_BODY_ADDRESSES_STATE, // 19

    KDC_REQ_BODY_ENC_AUTH_DATA_STATE, // 20

    KDC_REQ_BODY_ADDITIONAL_TICKETS_TAG_STATE, // 21
    KDC_REQ_BODY_ADDITIONAL_TICKETS_SEQ_STATE, // 22
    KDC_REQ_BODY_ADDITIONAL_TICKETS_STATE, // 23

    // End
    LAST_KDC_REQ_BODY_STATE; // 24

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "KDC_REQ_BODY_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<KdcReqBodyContainer> grammar )
    {
        if ( grammar instanceof KdcReqBodyGrammar )
        {
            return "KDC_REQ_BODY_GRAMMAR";
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
        return ( ( state == LAST_KDC_REQ_BODY_STATE.ordinal() ) ? "KDC_REQ_BODY_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_KDC_REQ_BODY_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public KdcReqBodyStatesEnum getStartState()
    {
        return START_STATE;
    }
}
