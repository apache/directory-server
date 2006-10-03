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
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;


/**
 * This class is the abstract container used to store the current state of a PDU
 * being decoded. It also stores the grammars used to decode the PDU, and zll
 * the informations needed to decode a PDU.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbstractContainer implements IAsn1Container
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** All the possible grammars */
    protected IGrammar grammar;

    /** Store a stack of the current states used when switching grammars */
    protected int[] stateStack;

    /** The current state of the decoding */
    protected int state;

    /** The current transition */
    protected int transition;

    /** The current TLV */
    protected TLV tlv;

    /** Store the different states for debug purpose */
    protected IStates states;

    /** The parent TLV */
    protected TLV parentTLV;

    /** The grammar end transition flag */
    protected boolean grammarEndAllowed;

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the current grammar
     * 
     * @return Returns the grammar used to decode a LdapMessage.
     */
    public IGrammar getGrammar()
    {
        return grammar;
    }


    /**
     * Get the current grammar state
     * 
     * @return Returns the current grammar state
     */
    public int getState()
    {
        return state;
    }


    /**
     * Set the new current state
     * 
     * @param state The new state
     */
    public void setState( int state )
    {
        this.state = state;
    }


    /**
     * Check that we can have a end state after this transition
     * 
     * @return true if this can be the last transition
     */
    public boolean isGrammarEndAllowed()
    {
        return grammarEndAllowed;
    }


    /**
     * Set the flag to allow a end transition
     * 
     * @param endAllowed true or false, depending on the next transition 
     * being an end or not.
     */
    public void grammarEndAllowed( boolean grammarEndAllowed )
    {
        this.grammarEndAllowed = grammarEndAllowed;
    }


    /**
     * Get the transition
     * 
     * @return Returns the transition from the previous state to the new state
     */
    public int getTransition()
    {
        return transition;
    }


    /**
     * Update the transition from a state to another
     * 
     * @param transition The transition to set
     */
    public void setTransition( int transition )
    {
        this.transition = transition;
    }


    /**
     * Set the current TLV
     * 
     * @param tlv The current TLV
     */
    public void setCurrentTLV( TLV tlv )
    {
        this.tlv = tlv;
    }


    /**
     * Get the current TLV
     * 
     * @return Returns the current TLV being decoded
     */
    public TLV getCurrentTLV()
    {
        return this.tlv;
    }


    /**
     * Get the states for this container's grammars
     * 
     * @return Returns the states.
     */
    public IStates getStates()
    {
        return states;
    }


    /**
     * Get the parent TLV;
     * 
     * @return Returns the parent TLV, if any.
     */
    public TLV getParentTLV()
    {
        return parentTLV;
    }


    /**
     * Set the parent TLV.
     * 
     * @param The parent TLV to set.
     */
    public void setParentTLV( TLV parentTLV )
    {
        this.parentTLV = parentTLV;
    }


    /**
     * Clean the container for the next usage.
     */
    public void clean()
    {
        tlv = null;
        parentTLV = null;
        transition = 0;
        state = TLVStateEnum.TAG_STATE_START;
    }
}
