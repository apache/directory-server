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
    /** starting state */
    public static int START_STATE = 0;

    /** StoredProcedureCall */
    public static int STORED_PROCEDURE_CALL_STATE = 1;

    // Name -------------------------------------------------------------------
    /** Name */
    public static int NAME_STATE = 2;

    // Options ----------------------------------------------------------------
    /** Options */
    public static int OPTIONS_STATE = 3;

    // --- Language Scheme ----------------------------------------------------
    /** Language Scheme */
    public static int LANGUAGE_SCHEME_STATE = 4;

    // --- Search Context -----------------------------------------------------
    /** Search Context */
    public static int SEARCH_CONTEXT_STATE = 5;

    // ------ Context ---------------------------------------------------------
    /** Context */
    public static int CONTEXT_STATE = 6;

    // ------ Scope -----------------------------------------------------------
    /** Scope */
    public static int SCOPE_STATE = 7;

    // Parameters -------------------------------------------------------------
    /** Parameters */
    public static int PARAMETERS_STATE = 8;

    // --- Parameter ----------------------------------------------------------
    /** Parameter */
    public static int PARAMETER_STATE = 9;

    // ------ Parameter type --------------------------------------------------
    /** Parameter type */
    public static int PARAMETER_TYPE_STATE = 10;

    // ------ Parameter value -------------------------------------------------
    /** Parameter value */
    public static int PARAMETER_VALUE_STATE = 11;

    public static int LAST_STORED_PROCEDURE_CALL_STATE = 12;

    //=========================================================================
    // States debug strings 
    //=========================================================================
    /** A string representation of all the states */
    private static String[] StoredProcedureCallString = new String[]
        { 
        "START_STATE",
        "STORED_PROCEDURE_CALL_STATE", 
        "NAME_STATE",
        "OPTIONS_STATE",
        "LANGUAGE_SCHEME_STATE",
        "SEARCH_CONTEXT_STATE",
        "CONTEXT_STATE",
        "SCOPE_STATE",
        "PARAMETERS_STATE",
        "PARAMETER_STATE",
        "PARAMETER_TYPE_STATE",
        "PARAMETER_VALUE_STATE" 
        };
    
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
        return "STORED_PROCEDURE_CALL_GRAMMAR";
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


    public String getState( int state )
    {
        return( ( state == GRAMMAR_END )
                        ? "STORED_PROCEDURE_CALL_END_STATE"
                        : StoredProcedureCallString[state] );
    }
}
