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
package org.apache.directory.shared.kerberos.codec.paEncTsEnc;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the PA-ENC-TS-ENC grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum PaEncTsEncStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- PA-ENC-TS-ENC message --------------------------------------
    PA_ENC_TS_ENC_STATE, // 1

    PA_ENC_TS_ENC_PA_TIMESTAMP_TAG_STATE, // 2
    PA_ENC_TS_PA_TIMESTAMP_STATE, // 3

    PA_ENC_TS_ENC_PA_USEC_TAG_STATE, // 4
    PA_ENC_TS_ENC_PA_USEC_STATE, // 4

    // End
    LAST_PA_ENC_TS_ENC_STATE; // 5

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "PA_ENC_TS_ENC_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<PaEncTsEncContainer> grammar )
    {
        if ( grammar instanceof PaEncTsEncGrammar )
        {
            return "PA_ENC_TS_ENC_GRAMMAR";
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
        return ( ( state == LAST_PA_ENC_TS_ENC_STATE.ordinal() ) ? "PA_ENC_TS_ENC_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_PA_ENC_TS_ENC_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public PaEncTsEncStatesEnum getStartState()
    {
        return START_STATE;
    }
}
