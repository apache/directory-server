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
package org.apache.directory.shared.kerberos.codec.encryptedData;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;


/**
 * This class store the EncryptedData grammar's constants. It is also used for debugging
 * purpose
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum EncryptedDataStatesEnum implements States
{
    // Start
    START_STATE, // 0

    // ----- EncryptedData message --------------------------------------
    ENCRYPTED_DATA_SEQ_STATE, // 1

    ENCRYPTED_DATA_ETYPE_TAG_STATE, // 2
    ENCRYPTED_DATA_ETYPE_STATE, // 3

    ENCRYPTED_DATA_KVNO_TAG_STATE, // 4
    ENCRYPTED_DATA_KVNO_STATE, // 5

    ENCRYPTED_DATA_CIPHER_TAG_STATE, // 6
    ENCRYPTED_DATA_CIPHER_STATE, // 7

    // End
    LAST_ENCRYPTED_DATA_STATE; // 8

    /**
     * Get the grammar name
     *
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "ENCRYPTED_DATA_GRAMMAR";
    }


    /**
     * Get the grammar name
     *
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar<EncryptedDataContainer> grammar )
    {
        if ( grammar instanceof EncryptedDataGrammar )
        {
            return "ENCRYPTED_DATA_GRAMMAR";
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
        return ( ( state == LAST_ENCRYPTED_DATA_STATE.ordinal() ) ? "ENCRYPTED_DATA_END_STATE" : name() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_ENCRYPTED_DATA_STATE;
    }


    /**
     * {@inheritDoc}
     */
    public EncryptedDataStatesEnum getStartState()
    {
        return START_STATE;
    }
}
