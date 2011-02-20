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
package org.apache.directory.shared.kerberos.codec.transitedEncoding;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the TransitedEncoding grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum TransitedEncodingStatesEnum implements States
{
    // Start
    START_STATE,                                // 0

    // ----- TransitedEncoding message --------------------------------------
    TRANSITED_ENCODING_SEQ_STATE,               // 1

    TRANSITED_ENCODING_TR_TYPE_TAG_STATE,       // 2
    TRANSITED_ENCODING_TR_TYPE_STATE,           // 3

    TRANSITED_ENCODING_CONTENTS_TAG_STATE,      // 4
    TRANSITED_ENCODING_CONTENTS_STATE,          // 5

    // End
    LAST_TRANSITED_ENCODING_STATE;              // 6


    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "TRANSITED_ENCODING_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<TransitedEncodingContainer> grammar )
    {
        if ( grammar instanceof TransitedEncodingGrammar )
        {
            return "TRANSITED_ENCODING_GRAMMAR";
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
        return ( ( state == LAST_TRANSITED_ENCODING_STATE.ordinal() ) ? "TRANSITED_ENCODING_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_TRANSITED_ENCODING_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public TransitedEncodingStatesEnum getStartState()
    {
        return START_STATE;
    }
}
