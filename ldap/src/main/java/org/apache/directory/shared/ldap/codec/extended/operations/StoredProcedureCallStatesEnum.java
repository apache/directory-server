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

package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * Constants for StoredProcedureCallGrammar.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureCallStatesEnum implements IStates
{
    // ~ Static fields/initializers -------------------------------------------

    //=========================================================================
    // Tags and values
    //=========================================================================
    /** StoredProcedureCall Tag */
    public static int STORED_PROCEDURE_CALL_TAG = 0;

    /** StoredProcedureCall Value */
    public static int STORED_PROCEDURE_CALL_VALUE = 1;

    // Name -------------------------------------------------------------------
    /** Name Tag */
    public static int NAME_TAG = 2;

    /** Name Value */
    public static int NAME_VALUE = 3;

    // Options ----------------------------------------------------------------
    /** Options Tag */
    public static int OPTIONS_TAG = 4;

    /** Options Value */
    public static int OPTIONS_VALUE = 5;

    // --- Language Scheme ----------------------------------------------------
    /** Language Scheme Tag */
    public static int LANGUAGE_SCHEME_TAG = 6;

    /** Language Scheme Value */
    public static int LANGUAGE_SCHEME_VALUE = 7;

    // --- Search Context -----------------------------------------------------
    /** Search Context Tag */
    public static int SEARCH_CONTEXT_TAG = 8;

    /** Search Context Value */
    public static int SEARCH_CONTEXT_VALUE = 9;

    // ------ Context ---------------------------------------------------------
    /** Context Tag */
    public static int CONTEXT_TAG = 10;

    /** Context Value */
    public static int CONTEXT_VALUE = 11;

    // ------ Scope -----------------------------------------------------------
    /** Scope Tag */
    public static int SCOPE_TAG = 12;

    /** Scope Value */
    public static int SCOPE_VALUE = 13;

    // Parameters -------------------------------------------------------------
    /** Parameters Tag */
    public static int PARAMETERS_TAG = 14;

    /** Parameters Value */
    public static int PARAMETERS_VALUE = 15;

    // --- Parameter ----------------------------------------------------------
    /** Parameter Tag */
    public static int PARAMETER_TAG = 16;

    /** Parameter Value */
    public static int PARAMETER_VALUE = 17;

    // ------ Parameter type --------------------------------------------------
    /** Parameter type Tag */
    public static int PARAMETER_TYPE_TAG = 18;

    /** Parameter type Value */
    public static int PARAMETER_TYPE_VALUE = 19;

    // ------ Parameter value -------------------------------------------------
    /** Parameter value Tag */
    public static int PARAMETER_VALUE_TAG = 20;

    /** Parameter value Value */
    public static int PARAMETER_VALUE_VALUE = 21;

    public static int LAST_STORED_PROCEDURE_CALL_STATE = 22;

    //=========================================================================
    // Grammar declarations
    //=========================================================================
    /** Ldap Message grammar */
    public static final int STORED_PROCEDURE_CALL_GRAMMAR_SWITCH = 0x0100;

    /** Ldap Message grammar number */
    public static final int STORED_PROCEDURE_CALL_GRAMMAR = 0;

    /** The total number of grammars used */
    public static final int NB_GRAMMARS = 1;

    //=========================================================================
    // Grammar switches debug strings 
    //=========================================================================
    /** A string representation of grammars */
    private static String[] GrammarSwitchString = new String[]
        { "STORED_PROCEDURE_CALL_GRAMMAR_SWITCH", };

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] StoredProcedureCallString = new String[]
        { "STORED_PROCEDURE_CALL_TAG", "STORED_PROCEDURE_CALL_VALUE",
          "NAME_TAG", "NAME_VALUE",
          "OPTIONS_TAG", "OPTIONS_VALUE",
          "LANGUAGE_SCHEME_TAG", "LANGUAGE_SCHEME_VALUE",
          "SEARCH_CONTEXT_TAG", "SEARCH_CONTEXT_VALUE",
          "CONTEXT_TAG", "CONTEXT_VALUE",
          "SCOPE_TAG", "SCOPE_VALUE",
          "PARAMETERS_TAG", "PARAMETERS_VALUE",
          "PARAMETER_TYPE_TAG", "PARAMETER_TYPE_VALUE",
          "PARAMETER_VALUE_TAG", "PARAMETER_VALUE_VALUE" };

    /** The instance */
    private static StoredProcedureCallStatesEnum instance = new StoredProcedureCallStatesEnum();


    // ~ Constructors ---------------------------------------------------------

    private StoredProcedureCallStatesEnum()
    {
    }


    // ~ Methods --------------------------------------------------------------

    public static IStates getInstance()
    {
        return instance;
    }


    public String getGrammarName( int grammar )
    {
        switch ( grammar )
        {
            case STORED_PROCEDURE_CALL_GRAMMAR:
                return "STORED_PROCEDURE_CALL_GRAMMAR";

            default:
                return "UNKNOWN";
        }
    }


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


    public String getState( int grammar, int state )
    {

        if ( ( state & GRAMMAR_SWITCH_MASK ) != 0 )
        {
            return ( state == END_STATE )
                ? "END_STATE"
                : GrammarSwitchString[( ( state & GRAMMAR_SWITCH_MASK ) >> 8 ) - 1];
        }
        else
        {
            switch ( grammar )
            {

                case STORED_PROCEDURE_CALL_GRAMMAR:
                    return( ( state == GRAMMAR_END )
                        ? "STORED_PROCEDURE_CALL_END_STATE"
                        : StoredProcedureCallString[state] );

                default:
                    return "UNKNOWN";
            }
        }
    }
}
