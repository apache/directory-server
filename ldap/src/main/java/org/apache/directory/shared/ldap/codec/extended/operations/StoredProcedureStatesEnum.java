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
 * Constants for StoredProcedureGrammar
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureStatesEnum implements IStates
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //=========================================================================
    // StoredProcedure
    //=========================================================================
    /** StoredProcedure Tag */
    public static int STORED_PROCEDURE_TAG = 0;

    /** StoredProcedure Value */
    public static int STORED_PROCEDURE_VALUE = 1;

    // Language ---------------------------------------------------------------
    /** Language Tag */
    public static int LANGUAGE_TAG = 2;

    /** Language Value */
    public static int LANGUAGE_VALUE = 3;

    // Procedure --------------------------------------------------------------
    /** Procedure Tag */
    public static int PROCEDURE_TAG = 4;

    /** Procedure Value */
    public static int PROCEDURE_VALUE = 5;

    // Parameters -------------------------------------------------------------
    /** Parameters Tag */
    public static int PARAMETERS_TAG = 6;

    /** Parameters Value */
    public static int PARAMETERS_VALUE = 7;

    // Parameter type ---------------------------------------------------------
    /** Parameter type Tag */
    public static int PARAMETER_TYPE_TAG = 8;

    /** Parameter type Value */
    public static int PARAMETER_TYPE_VALUE = 9;

    // Parameters value -------------------------------------------------------
    /** Parameter value Tag */
    public static int PARAMETER_VALUE_TAG = 10;

    /** Parameter value Value */
    public static int PARAMETER_VALUE_VALUE = 11;

    public static int LAST_STORED_PROCEDURE_STATE = 12;

    //=========================================================================
    // Grammars declaration.
    //=========================================================================
    /** Ldap Message Grammar */
    public static final int STORED_PROCEDURE_GRAMMAR_SWITCH = 0x0100;

    /** LdapMessage grammar number */
    public static final int STORED_PROCEDURE_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    //=========================================================================
    // Grammar switches debug strings 
    //=========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString = new String[]
        { "STORED_PROCEDURE_GRAMMAR_SWITCH", };

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] StoredProcedureString = new String[]
        { "STORED_PROCEDURE_TAG", "STORED_PROCEDURE_VALUE", "LANGUAGE_TAG", "LANGUAGE_VALUE", "PROCEDURE_TAG",
            "PROCEDURE_VALUE", "PARAMETERS_TAG", "PARAMETERS_VALUE", "PARAMETER_TYPE_TAG", "PARAMETER_TYPE_VALUE",
            "PARAMETER_VALUE_TAG", "PARAMETER_VALUE_VALUE" };

    /** The instance */
    private static StoredProcedureStatesEnum instance = new StoredProcedureStatesEnum();


    //~ Constructors -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     *
     */
    private StoredProcedureStatesEnum()
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
     * @param The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        switch ( grammar )
        {
            case STORED_PROCEDURE_GRAMMAR:
                return "STORED_PROCEDURE_GRAMMAR";

            default:
                return "UNKNOWN";
        }
    }


    /**
     * Get the grammar name
     * @param The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof StoredProcedureGrammar )
        {
            return "STORED_PROCEDURE_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
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

                case STORED_PROCEDURE_GRAMMAR:
                    return ( ( state == GRAMMAR_END ) ? "STORED_PROCEDURE_END_STATE" : StoredProcedureString[state] );

                default:
                    return "UNKNOWN";
            }
        }
    }
}
