/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
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

    /**
     * The grammars that are used. It's a stack as we can switch grammars
     */
    protected IGrammar[] grammarStack;

    /** All the possible grammars */
    protected IGrammar[] grammars;

    /** Store a stack of the current states used when switching grammars */
    protected int[] stateStack;

    /** Store a stack of allowed pop */
    protected boolean[] popAllowedStack;

    /** The number of stored grammars */
    protected int nbGrammars;

    /** The current grammar */
    protected int currentGrammar;

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

    /** The grammar pop transition flag */
    protected boolean grammarPopAllowed;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the current grammar
     * 
     * @return Returns the grammar used to decode a LdapMessage.
     */
    public IGrammar getGrammar()
    {
        return grammarStack[currentGrammar];
    }


    /**
     * Add a IGrammar to use
     * 
     * @param grammar
     *            The grammar to add.
     */
    public void addGrammar( IGrammar grammar )
    {
        grammars[nbGrammars++] = grammar;
    }


    /**
     * Switch to another grammar
     * 
     * @param currentState
     *            The current state in the current grammar
     * @param grammar
     *            The grammar to add.
     */
    public void switchGrammar( int currentState, int grammar )
    {
        stateStack[currentGrammar] = currentState;
        currentGrammar++;
        popAllowedStack[currentGrammar] = false;
        grammarStack[currentGrammar] = grammars[( grammar >> 8 ) - 1];
    }


    /**
     * restore the previous grammar (the one before a switch has occured)
     * 
     * @return The previous current state, if any.
     */
    public int restoreGrammar()
    {
        grammarStack[currentGrammar] = null;
        popAllowedStack[currentGrammar] = false;
        currentGrammar--;

        if ( currentGrammar >= 0 )
        {
            return stateStack[currentGrammar];
        }
        else
        {
            return -1;
        }

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
     * @param state
     *            The new state
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
     * @param endAllowed
     *            true or false, depending on the next transition being an end
     *            or not.
     */
    public void grammarEndAllowed( boolean grammarEndAllowed )
    {
        this.grammarEndAllowed = grammarEndAllowed;
    }


    /**
     * Check that we can have a pop after this transition
     * 
     * @return true if this can be the last transition before a pop
     */
    public boolean isGrammarPopAllowed()
    {
        return popAllowedStack[currentGrammar];
    }


    /**
     * Set the flag to allow a pop transition
     * 
     * @param popAllowed
     *            true or false, depending on the next transition allows a pop
     *            or not.
     */
    public void grammarPopAllowed( boolean grammarPopAllowed )
    {
        popAllowedStack[currentGrammar] = grammarPopAllowed;
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
     * @param transition
     *            The transition to set
     */
    public void setTransition( int transition )
    {
        this.transition = transition;
    }


    /**
     * Gert the current grammar number
     * 
     * @return Returns the currentGrammar.
     */
    public int getCurrentGrammar()
    {
        return currentGrammar;
    }


    /**
     * Get the current grammar type.
     * 
     * @return Returns the current Grammar type, or -1 if not found.
     */
    public int getCurrentGrammarType()
    {

        for ( int i = 0; i < grammars.length; i++ )
        {

            if ( grammars[i] == grammarStack[currentGrammar] )
            {
                return i;
            }
        }

        return -1;
    }


    /**
     * Initialize the grammar stack
     * 
     * @param grammar
     *            Set the initial grammar
     */
    public void setInitGrammar( int grammar )
    {
        currentGrammar++;
        grammarStack[currentGrammar] = grammars[grammar];
        stateStack[currentGrammar] = 0;
    }


    /**
     * Set the current TLV
     * 
     * @param tlv
     *            The current TLV
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
     * @param The
     *            parent TLV to set.
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
        currentGrammar = 0;
        tlv = null;
        parentTLV = null;
        transition = 0;
        state = TLVStateEnum.TAG_STATE_START;
    }
}
