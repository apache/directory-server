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
package org.apache.directory.shared.kerberos.codec.ticket;


import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.States;
import org.apache.directory.shared.kerberos.codec.KerberosMessageGrammar;


/**
 * This class store the Ticket grammar's constants. It is also used for debugging
 * purpose
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum TicketStatesEnum implements States
{
    // Start
    START_STATE,
    
    // ----- Ticket message --------------------------------------------
    TICKET_STATE,
    TICKET_SEQ_STATE,
    
    TICKET_VNO_TAG_STATE,
    TICKET_VNO_STATE,
    
    TICKET_REALM_TAG_STATE,
    TICKET_REALM_STATE,
    
    TICKET_SNAME_TAG_STATE,

    TICKET_ENC_PART_TAG_STATE,
    
    // End
    LAST_TICKET_STATE;

    
    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "TICKET_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( Grammar grammar )
    {
        if ( grammar instanceof KerberosMessageGrammar )
        {
            return "TICKET_GRAMMAR";
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
        return ( ( state == LAST_TICKET_STATE.ordinal() ) ? "TICKET_END_STATE" : name() );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isEndState()
    {
        return this == LAST_TICKET_STATE;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public TicketStatesEnum getStartState()
    {
        return START_STATE;
    }
}
