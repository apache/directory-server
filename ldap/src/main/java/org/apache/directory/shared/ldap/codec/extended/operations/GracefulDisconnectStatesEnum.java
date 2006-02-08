/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the GracefulDisconnect's grammar constants. It is also used
 * for debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulDisconnectStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // GracefulDisconnect grammar states
    // =========================================================================

    /** Sequence Tag */
    public static int GRACEFUL_DISCONNECT_SEQUENCE_TAG = 0;

    /** Sequence Value */
    public static int GRACEFUL_DISCONNECT_SEQUENCE_VALUE = 1;

    /** Time offline Tag */
    public static int TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG = 2;

    /** Time offline Value */
    public static int TIME_OFFLINE_VALUE = 3;

    /** Delay Tag */
    public static int DELAY_OR_REPLICATED_OR_END_TAG = 4;

    /** Delay Value */
    public static int DELAY_VALUE = 5;

    /** Replicated contexts Tag */
    public static int REPLICATED_CONTEXTS_OR_END_TAG = 6;

    /** Replicated contexts Value */
    public static int REPLICATED_CONTEXTS_VALUE = 7;

    /** Replicated contexts Tag */
    public static int REPLICATED_CONTEXT_OR_END_TAG = 8;

    /** Replicated contexts Value */
    public static int REPLICATED_CONTEXT_VALUE = 9;

    /** terminal state */
    public static int LAST_GRACEFUL_DISCONNECT_STATE = 10;

    // =========================================================================
    // Grammars declaration.
    // =========================================================================
    /** GracefulDisconnect grammar */
    public static final int GRACEFUL_DISCONNECT_GRAMMAR_SWITCH = 0x0100;

    /** GracefulDisconnect grammar number */
    public static final int GRACEFUL_DISCONNECT_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    // =========================================================================
    // Grammar switches debug strings
    // =========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString = new String[]
        { "GRACEFUL_DISCONNECT_GRAMMAR_SWITCH" };

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] GracefulDisconnectString = new String[]
        { "GRACEFUL_DISCONNECT_SEQUENCE_TAG", "GRACEFUL_DISCONNECT_SEQUENCE_VALUE",
            "TIME_OFFLINE_OR_DELAY_OR_REPLICATED_OR_END_TAG", "TIME_OFFLINE_VALUE", "DELAY_OR_REPLICATED_OR_END_TAG",
            "DELAY_VALUE", "REPLICATED_CONTEXTS_OR_END_TAG", "REPLICATED_CONTEXTS_VALUE",
            "REPLICATED_CONTEXT_OR_END_TAG", "REPLICATED_CONTEXT_VALUE" };

    /** The instance */
    private static GracefulDisconnectStatesEnum instance = new GracefulDisconnectStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private GracefulDisconnectStatesEnum()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get an instance of this class
     * 
     * @return An instance on this class
     */
    public static IStates getInstance()
    {
        return instance;
    }


    /**
     * Get the grammar name
     * 
     * @param grammar
     *            The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        switch ( grammar )
        {
            case GRACEFUL_DISCONNECT_GRAMMAR:
                return "GRACEFUL_DISCONNECT_GRAMMAR";
            default:
                return "UNKNOWN";
        }
    }


    /**
     * Get the grammar name
     * 
     * @param grammar
     *            The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof GracefulDisconnectGrammar )
        {
            return "GRACEFUL_DISCONNECT_GRAMMAR";
        }

        return "UNKNOWN GRAMMAR";
    }


    /**
     * Get the string representing the state
     * 
     * @param grammar
     *            The current grammar being used
     * @param state
     *            The state number
     * @return The String representing the state
     */
    public String getState( int grammar, int state )
    {

        if ( ( state & GRAMMAR_SWITCH_MASK ) != 0 )
        {
            return ( state == END_STATE ) ? "END_STATE"
                : GrammarSwitchString[( ( state & GRAMMAR_SWITCH_MASK ) >> 8 ) - 1];
        }
        else
        {

            switch ( grammar )
            {

                case GRACEFUL_DISCONNECT_GRAMMAR:
                    return ( ( state == GRAMMAR_END ) ? "GRACEFUL_DISCONNECT_END_STATE"
                        : GracefulDisconnectString[state] );

                default:
                    return "UNKNOWN";
            }
        }
    }
}
