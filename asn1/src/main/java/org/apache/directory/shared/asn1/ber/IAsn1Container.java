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
package org.apache.directory.shared.asn1.ber;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.TLV;


/**
 * Every ASN1 container must implement this interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface IAsn1Container
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    // State accessors
    /**
     * Get the current grammar state
     * 
     * @return Returns the current grammar state
     */
    int getState();


    /**
     * Set the new current state
     * 
     * @param state The new state
     */
    void setState( int state );


    /**
     * Set the current TLV
     * 
     * @param tlv The current TLV
     */
    void setCurrentTLV( TLV tlv );


    /**
     * Get the currentTLV
     * 
     * @return Returns the current TLV being decoded
     */
    TLV getCurrentTLV();


    /**
     * Get the grammar
     * 
     * @return Returns the grammar used to decode a LdapMessage.
     */
    IGrammar getGrammar();


    /**
     * Get the transition
     * 
     * @return Returns the transition from the previous state to the new state
     */
    int getTransition();


    /**
     * Update the transition from a state to another
     * 
     * @param transition The transition to set
     */
    void setTransition( int transition );

    /**
     * @return Returns the states.
     */
    IStates getStates();


    /**
     * @return get the parent TLV.
     */
    TLV getParentTLV();


    /**
     * Set the parent TLV
     * 
     * @param parentTLV The new parent TLV
     */
    void setParentTLV( TLV parentTLV );


    /**
     * Check that we can have a end state after this transition
     * 
     * @return true if this can be the last transition
     */
    boolean isGrammarEndAllowed();


    /**
     * Set the flag to allow a end transition
     * 
     * @param grammarEndAllowed true or false, depending on the next transition
     * being an end or not.
     */
    void grammarEndAllowed( boolean grammarEndAllowed );
    
    /**
     * Get a new TLV id
     * @return a unique value representing the current TLV id
     */
    int getNewTlvId();

    /**
     * Get the current TLV id
     * @return a unique value representing the current TLV id
     */
    int getTlvId();
}
