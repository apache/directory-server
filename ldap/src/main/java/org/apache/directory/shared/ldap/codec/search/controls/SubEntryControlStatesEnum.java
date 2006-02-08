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
package org.apache.directory.shared.ldap.codec.search.controls;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the SubEntryControl's grammar constants. It is also used for
 * debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubEntryControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // Sub entry control grammar states
    // =========================================================================

    /** Visibility Tag */
    public static int SUB_ENTRY_VISIBILITY_TAG = 0;

    /** Visibility Value */
    public static int SUB_ENTRY_VISIBILITY_VALUE = 1;

    /** terminal state */
    public static int LAST_SUB_ENTRY_STATE = 2;

    // =========================================================================
    // Grammars declaration.
    // =========================================================================
    /** PSsearch grammar */
    public static final int SUB_ENTRY_GRAMMAR_SWITCH = 0x0100;

    /** PSearch grammar number */
    public static final int SUB_ENTRY_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    // =========================================================================
    // Grammar switches debug strings
    // =========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString = new String[]
        { "SUB_ENTRY_GRAMMAR_SWITCH" };

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] SubEntryString = new String[]
        { "SUB_ENTRY_VISIBILITY_TAG", "SUB_ENTRY_VISIBILITY_VALUE", };

    /** The instance */
    private static SubEntryControlStatesEnum instance = new SubEntryControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private SubEntryControlStatesEnum()
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
            case SUB_ENTRY_GRAMMAR:
                return "SUB_ENTRY_GRAMMAR";
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
        if ( grammar instanceof SubEntryControlGrammar )
        {
            return "SUB_ENTRY_GRAMMAR";
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

                case SUB_ENTRY_GRAMMAR:
                    return ( ( state == GRAMMAR_END ) ? "SUB_ENTRY_END_STATE" : SubEntryString[state] );

                default:
                    return "UNKNOWN";
            }
        }
    }
}
