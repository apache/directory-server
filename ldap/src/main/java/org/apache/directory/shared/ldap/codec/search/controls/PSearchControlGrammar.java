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


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.BooleanDecoder;
import org.apache.directory.shared.asn1.util.BooleanDecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the PSearchControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * The decoded grammar is the following :
 * 
 * PersistenceSearch ::= SEQUENCE {
 *     changeTypes  INTEGER,  -- an OR combinaison of 0, 1, 2 and 4 --
 *     changeOnly   BOOLEAN,
 *     returnECs    BOOLEAN
 * }
 * 
 * The changeTypes field is the logical OR of one or more of these values:
 * add    (1), 
 * delete (2), 
 * modify (4), 
 * modDN  (8).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControlGrammar extends AbstractGrammar implements IGrammar
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( PSearchControlGrammar.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. PSearchControlGrammar is a singleton */
    private static IGrammar instance = new PSearchControlGrammar();


    /**
     * Creates a new ModifyDNRequestGrammar object.
     */
    private PSearchControlGrammar()
    {
        name = PSearchControlGrammar.class.getName();
        statesEnum = PSearchControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[PSearchControlStatesEnum.LAST_PSEARCH_STATE][256];

        /** 
         * Transition from initial state to Psearch sequence
         * PSearch ::= SEQUENCE OF {
         *     ...
         *     
         * Initialize the persistence search object
         */
        super.transitions[PSearchControlStatesEnum.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( PSearchControlStatesEnum.INIT_GRAMMAR_STATE, 
                                    PSearchControlStatesEnum.PSEARCH_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG, 
                new GrammarAction( "Init PSearchControl" )
            {
                public void action( IAsn1Container container )
                {
                    PSearchControlContainer psearchContainer = ( PSearchControlContainer ) container;
                    PSearchControl control = new PSearchControl();
                    psearchContainer.setPSearchControl( control );
                }
            } );


        /** 
         * Transition from Psearch sequence to Change types
         * PSearch ::= SEQUENCE OF {
         *     changeTypes  INTEGER,  -- an OR combinaison of 0, 1, 2 and 4 --
         *     ...
         *     
         * Stores the change types value
         */
        super.transitions[PSearchControlStatesEnum.PSEARCH_SEQUENCE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( PSearchControlStatesEnum.PSEARCH_SEQUENCE_STATE, PSearchControlStatesEnum.CHANGE_TYPES_STATE, UniversalTag.INTEGER_TAG,
                new GrammarAction( "Set PSearchControl changeTypes" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    PSearchControlContainer psearchContainer = ( PSearchControlContainer ) container;
                    Value value = psearchContainer.getCurrentTLV().getValue();

                    try
                    {
                        // Check that the value is into the allowed interval
                        int changeTypes = IntegerDecoder.parse( value, PSearchControl.CHANGE_TYPES_MIN, PSearchControl.CHANGE_TYPES_MAX );
                        
                        if ( IS_DEBUG )
                        {
                            log.debug( "changeTypes = " + changeTypes );
                        }

                        psearchContainer.getPSearchControl().setChangeTypes( changeTypes );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = "failed to decode the changeTypes for PSearchControl";
                        log.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /** 
         * Transition from Change types to Changes only
         * PSearch ::= SEQUENCE OF {
         *     ...
         *     changeOnly   BOOLEAN,
         *     ...
         *     
         * Stores the change only flag
         */
        super.transitions[PSearchControlStatesEnum.CHANGE_TYPES_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( PSearchControlStatesEnum.CHANGE_TYPES_STATE, PSearchControlStatesEnum.CHANGES_ONLY_STATE, UniversalTag.BOOLEAN_TAG,
                new GrammarAction( "Set PSearchControl changesOnly" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    PSearchControlContainer psearchContainer = ( PSearchControlContainer ) container;
                    Value value = psearchContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean changesOnly = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            log.debug( "changesOnly = " + changesOnly );
                        }

                        psearchContainer.getPSearchControl().setChangesOnly( changesOnly );
                    }
                    catch ( BooleanDecoderException e )
                    {
                        String msg = "failed to decode the changesOnly for PSearchControl";
                        log.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /** 
         * Transition from Change types to Changes only
         * PSearch ::= SEQUENCE OF {
         *     ...
         *     returnECs    BOOLEAN 
         * }
         *     
         * Stores the return ECs flag 
         */
        super.transitions[PSearchControlStatesEnum.CHANGES_ONLY_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( PSearchControlStatesEnum.CHANGES_ONLY_STATE, PSearchControlStatesEnum.RETURN_ECS_STATE, UniversalTag.BOOLEAN_TAG,
                new GrammarAction( "Set PSearchControl returnECs" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    PSearchControlContainer psearchContainer = ( PSearchControlContainer ) container;
                    Value value = psearchContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean returnECs = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            log.debug( "returnECs = " + returnECs );
                        }

                        psearchContainer.getPSearchControl().setReturnECs( returnECs );

                        // We can have an END transition
                        psearchContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException e )
                    {
                        String msg = "failed to decode the returnECs for PSearchControl";
                        log.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );
    }


    /**
     * This class is a singleton.
     * 
     * @return An instance on this grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
