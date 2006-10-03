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
package org.apache.directory.shared.ldap.codec.search.controls;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the PSearchControl's grammar constants. It is also used for
 * debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // Persistent search control grammar states
    // =========================================================================
    /** Initial state */
    public static final int START_STATE = 0;

    /** Sequence Value */
    public static int PSEARCH_SEQUENCE_STATE = 1;

    /** changeTypes Value */
    public static int CHANGE_TYPES_STATE = 3;
    
    /** changesOnly Value */
    public static int CHANGES_ONLY_STATE = 5;

    /** returnECs Value */
    public static int RETURN_ECS_STATE = 7;

    /** terminal state */
    public static int LAST_PSEARCH_STATE = 8;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] PSearchString = new String[]
        { 
        "START_STATE", 
        "PSEARCH_SEQUENCE_VALUE", 
        "CHANGE_TYPES_STATE",
        "CHANGES_ONLY_STATE", 
        "RETURN_ECS_STATE" 
        };

    /** The instance */
    private static PSearchControlStatesEnum instance = new PSearchControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private PSearchControlStatesEnum()
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
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "PSEARCH_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
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
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == GRAMMAR_END ) ? "PSEARCH_END_STATE" : PSearchString[state] );
    }
}
