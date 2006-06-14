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
package org.apache.directory.shared.asn1.ber.grammar;


import javax.naming.NamingException;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Tag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;


/**
 * The abstract IGrammar which is the Mother of all the grammars. It contains
 * the transitions table.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AbstractGrammar.class );

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /**
     * Table of transitions. It's a two dimension array, the first dimension
     * indice the states, the second dimension indices the Tag value, so it is
     * 256 wide.
     */
    protected GrammarTransition[][] transitions;

    /** The grammar name */
    protected String name;

    /** The grammar's states */
    protected IStates statesEnum;


    public AbstractGrammar()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Return the grammar's name
     * 
     * @return The grammar name
     */
    public String getName()
    {
        return name;
    }


    /**
     * Set the grammar's name
     * 
     * @param name
     *            DOCUMENT ME!
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * Checks the Length. If the current TLV length is above the expected length
     * of the PDU, an exception is thrown. The current Object contains the sum
     * of all included Objects and element, which is compared with the PDU's
     * expected length (the Length part of the PDU containing the Object).
     * 
     * @param object
     *            The Object that is being decoded.
     * @param tlv
     *            The current TLV
     * @throws DecoderException
     *             Thrown if the expected length is lower than the sum of all
     *             the included elements.
     */
    protected void checkLength( Asn1Object object, TLV tlv ) throws DecoderException
    {

        // Create a new expected Length
        int expectedLength = tlv.getLength().getLength();

        int tlvLength = tlv.getSize();

        if ( log.isDebugEnabled() )
        {
            log.debug( "Expected Length = " + ( ( Asn1Object ) object ).getExpectedLength() + ", current length = "
                + ( ( Asn1Object ) object ).getCurrentLength() + ", added length = " + expectedLength
                + ", tlv length = " + tlvLength );
        }

        // We already are at the top level.
        // An exception will be thrown if the current length exceed the expected
        // length
        ( ( Asn1Object ) object ).addLength( tlvLength );
    }


    /**
     * Get the transition associated with the state and tag
     * 
     * @param state
     *            The current state
     * @param tag
     *            The current tag
     * @return A valid transition if any, or null.
     */
    public GrammarTransition getTransition( int state, int tag )
    {
        return transitions[state][tag & IStates.STATES_SWITCH_MASK];
    }


    /**
     * The main function. This is where an action is executed. If the action is
     * null, nothing is done.
     * 
     * @param container
     *            The Asn1Container
     * @throws DecoderException
     *             Thrown if anything went wrong
     */
    public void executeAction( IAsn1Container container ) throws DecoderException, NamingException
    {

        int currentState = container.getTransition();
        IGrammar currentGrammar = container.getGrammar();

        // We have to deal with the special case of a GRAMMAR_END state
        if ( currentState == IStates.END_STATE )
        {
            currentState = container.restoreGrammar();

            if ( currentState == IStates.END_STATE )
            {
                return;
            }
        }

        Tag tag = container.getCurrentTLV().getTag();
        byte tagByte = tag.getTagByte();

        // We will loop until no more actions are to be executed
        while ( true )
        {

            GrammarTransition transition = ( ( AbstractGrammar ) container.getGrammar() ).getTransition( currentState,
                tagByte & IStates.STATES_SWITCH_MASK );

            if ( transition == null )
            {

                if ( container.getCurrentGrammar() == 0 )
                {
                    String errorMessage = "Bad transition from state "
                        + currentGrammar.getStatesEnum().getState( container.getCurrentGrammarType(), currentState )
                        + ", tag " + Asn1StringUtils.dumpByte( tag.getTagByte() );

                    log.error( errorMessage );

                    // If we have no more grammar on the stack, then this is an
                    // error
                    throw new DecoderException( "Bad transition !" );
                }
                else
                {

                    // We have finished with the current grammar, so we have to
                    // continue with the
                    // previous one, only if allowed

                    if ( container.isGrammarPopAllowed() )
                    {
                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Pop grammar {}, state = {}", container.getStates().getGrammarName(
                                currentGrammar ), currentGrammar.getStatesEnum().getState(
                                container.getCurrentGrammarType(), currentState ) );
                        }

                        currentState = container.restoreGrammar();
                        continue;
                    }
                    else
                    {
                        String msg = "Cannot pop the grammar " + container.getStates().getGrammarName( currentGrammar )
                            + " for state "
                            + currentGrammar.getStatesEnum().getState( container.getCurrentGrammarType(), currentState );
                        // We can't pop the grammar
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( transition.toString( container.getCurrentGrammarType(), currentGrammar.getStatesEnum() ) );
            }

            int nextState = transition.getNextState();

            if ( ( ( nextState & IStates.GRAMMAR_SWITCH_MASK ) != 0 ) && ( nextState != IStates.END_STATE ) )
            {

                if ( transition.hasAction() )
                {
                    transition.getAction().action( container );
                }

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Switching from grammar {} to grammar {}", container.getStates().getGrammarName(
                        currentGrammar ), container.getStates().getGrammarName( ( nextState >> 8 ) - 1 ) );
                }

                // We have a grammar switch, so we change the current state to
                // the initial
                // state in the new grammar and loop.
                container.switchGrammar( currentState, nextState & IStates.GRAMMAR_SWITCH_MASK );
                currentState = IStates.INIT_GRAMMAR_STATE;
            }
            else
            {

                // This is not a grammar switch, so we execute the
                // action if we have one, and we quit the loop.
                container.setTransition( nextState );

                if ( transition.hasAction() )
                {
                    transition.getAction().action( container );
                }

                break;
            }
        }
    }


    /**
     * Get the states of the current grammar
     * 
     * @return Returns the statesEnum.
     */
    public IStates getStatesEnum()
    {
        return statesEnum;
    }


    /**
     * Set the states for this grammar
     * 
     * @param statesEnum
     *            The statesEnum to set.
     */
    public void setStatesEnum( IStates statesEnum )
    {
        this.statesEnum = statesEnum;
    }
}
