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
 * This class store the EntryChangeControl's grammar constants.
 * It is also used for debugging purposes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlStatesEnum implements IStates
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //=========================================================================
    // Entry change control grammar states 
    //=========================================================================

    /** Sequence Tag */
    public static int EC_SEQUENCE_TAG = 0;

    /** Sequence Value */
    public static int EC_SEQUENCE_VALUE = 1;

    /** changeType Tag */
    public static int CHANGE_TYPE_TAG = 2;

    /** changeType Value */
    public static int CHANGE_TYPE_VALUE = 3;

    /** previousDN Tag */
    public static int CHANGE_NUMBER_OR_PREVIOUS_DN_TAG = 4;

    /** previousDN Value */
    public static int PREVIOUS_DN_VALUE = 5;

    /** changeNumber Tag */
    public static int CHANGE_NUMBER_TAG = 6;

    /** changeNumber Value */
    public static int CHANGE_NUMBER_VALUE = 7;
    
    /** terminal state */
    public static int LAST_EC_STATE = 8;

    //=========================================================================
    // Grammars declaration.
    //=========================================================================
    /** Entry change grammar */
    public static final int EC_GRAMMAR_SWITCH = 0x0100;

    /** Entry change grammar number */
    public static final int EC_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    //=========================================================================
    // Grammar switches debug strings 
    //=========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString = new String[] { "EC_GRAMMAR_SWITCH" };

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] EcString = new String[]
    {
        "EC_SEQUENCE_TAG",
        "EC_SEQUENCE_VALUE",
        "CHANGE_TYPE_TAG",
        "CHANGE_TYPE_VALUE",
        "CHANGE_NUMBER_OR_PREVIOUS_DN_TAG",
        "PREVIOUS_DN_VALUE",
        "CHANGE_NUMBER_TAG",
        "CHANGE_NUMBER_VALUE"
    };

    /** The instance */
    private static EntryChangeControlStatesEnum instance = new EntryChangeControlStatesEnum();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     *
     */
    private EntryChangeControlStatesEnum()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get an instance of this class
     * @return An instance on this class
     */
    public static IStates getInstance()
    {
        return instance;
    }

    /**
     * Get the grammar name
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        switch ( grammar )
        {
            case EC_GRAMMAR: return "EC_GRAMMAR";
            default: return "UNKNOWN";
        }
    }

    /**
     * Get the grammar name
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof EntryChangeControlGrammar )
        {
            return "EC_GRAMMAR";
        }
        
        return "UNKNOWN GRAMMAR";
    }

    /**
     * Get the string representing the state
     * 
     * @param grammar The current grammar being used
     * @param state The state number
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

                case EC_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "EC_END_STATE" : EcString[state] );

                default :
                    return "UNKNOWN";
            }
        }
    }
}
