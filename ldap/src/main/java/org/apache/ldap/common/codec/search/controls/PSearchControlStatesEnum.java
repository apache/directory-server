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
package org.apache.ldap.common.codec.search.controls;


import org.apache.asn1.ber.grammar.IStates;
import org.apache.asn1.ber.grammar.IGrammar;


/**
 * This class store the PSearchControl's grammar constants.
 * It is also used for debugging purposes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControlStatesEnum implements IStates
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //=========================================================================
    // Persistent search control grammar states 
    //=========================================================================

    /** Sequence Tag */
    public static int PSEARCH_SEQUENCE_TAG = 0;

    /** Sequence Value */
    public static int PSEARCH_SEQUENCE_VALUE = 1;

    /** changeTypes Tag */
    public static int CHANGE_TYPES_TAG = 2;

    /** changeTypes Value */
    public static int CHANGE_TYPES_VALUE = 3;

    /** changesOnly Tag */
    public static int CHANGES_ONLY_TAG = 4;

    /** changesOnly Value */
    public static int CHANGES_ONLY_VALUE = 5;

    /** returnECs Tag */
    public static int RETURN_ECS_TAG = 6;

    /** returnECs Value */
    public static int RETURN_ECS_VALUE = 7;
    
    /** terminal state */
    public static int LAST_PSEARCH_STATE = 8;

    //=========================================================================
    // Grammars declaration.
    //=========================================================================
    /** PSsearch grammar */
    public static final int PSEARCH_GRAMMAR_SWITCH = 0x0100;

    /** PSearch grammar number */
    public static final int PSEARCH_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    //=========================================================================
    // Grammar switches debug strings 
    //=========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString =
        new String[]
        {
            "PSEARCH_GRAMMAR_SWITCH"
        };

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] PSearchString = new String[]
    {
        "PSEARCH_SEQUENCE_TAG",
        "PSEARCH_SEQUENCE_VALUE",
        "CHANGE_TYPES_TAG",
        "CHANGE_TYPES_VALUE",
        "CHANGES_ONLY_TAG",
        "CHANGES_ONLY_VALUE",
        "RETURN_ECS_TAG",
        "RETURN_ECS_VALUE"
    };

    /** The instance */
    private static PSearchControlStatesEnum instance = new PSearchControlStatesEnum();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     *
     */
    private PSearchControlStatesEnum()
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
            case PSEARCH_GRAMMAR: return "PSEARCH_GRAMMAR";
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
        if ( grammar instanceof PSearchControlGrammar )
        {
            return "PSEARCH_GRAMMAR";
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

                case PSEARCH_GRAMMAR :
                    return ( ( state == GRAMMAR_END ) ? "PSEARCH_END_STATE" : PSearchString[state] );

                default :
                    return "UNKNOWN";
            }
        }
    }
}
