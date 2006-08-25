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
package org.apache.directory.shared.asn1.ber.grammar;


import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;


/**
 * Define a transition between two states of a grammar. It stores the next
 * state, and the action to execute while transiting.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GrammarTransition
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The next state in the grammar */
    private int nextState;

    /** The action associated to the transition */
    private GrammarAction action;

    /** The current state */
    private int currentState;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new GrammarTransition object.
     * 
     * @param currentState
     *            The current transition
     * @param nextState
     *            The target state
     * @param action
     *            The action to execute. It could be null.
     */
    public GrammarTransition(int currentState, int nextState, GrammarAction action)
    {
        this.currentState = currentState;
        this.nextState = nextState;
        this.action = action;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * @return Returns the target state.
     */
    public int getNextState()
    {
        return nextState;
    }


    /**
     * Tells if the transition has an associated action.
     * 
     * @return <code>true</code> if an action has been asociated to the
     *         transition
     */
    public boolean hasAction()
    {
        return action != null;
    }


    /**
     * @return Returns the action associated with the transition
     */
    public GrammarAction getAction()
    {
        return action;
    }


    /**
     * @param grammar
     *            The grammar which state we want a String from
     * @return A representation of the transition as a string.
     */
    public String toString( int grammar, IStates statesEnum )
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "Transition from <" ).append( statesEnum.getState( grammar, currentState ) ).append( "> to <" )
            .append( statesEnum.getState( grammar, nextState ) ).append( ">, action : " ).append(
                ( ( action == null ) ? "no action" : action.toString() ) ).append( ">" );

        return sb.toString();
    }
}
