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
package org.apache.directory.shared.kerberos.codec.methodData;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.codec.methodData.actions.AddPaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the METHOD-DATA structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class MethodDataGrammar extends AbstractGrammar<MethodDataContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( MethodDataGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. MethodDataGrammar is a singleton */
    private static Grammar<MethodDataContainer> instance = new MethodDataGrammar();


    /**
     * Creates a new MethodDataGrammar object.
     */
    @SuppressWarnings("unchecked")
    private MethodDataGrammar()
    {
        setName( MethodDataGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[MethodDataStatesEnum.LAST_METHOD_DATA_STATE.ordinal()][256];

        // ============================================================================================
        // METHOD-DATA
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from METHOD-DATA init to METHOD-DATA SEQ
        // --------------------------------------------------------------------------------------------
        // METHOD-DATA         ::= SEQUENCE
        super.transitions[MethodDataStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<MethodDataContainer>(
            MethodDataStatesEnum.START_STATE,
            MethodDataStatesEnum.METHOD_DATA_SEQ_STATE,
            UniversalTag.SEQUENCE.getValue(),
            new CheckNotNullLength<MethodDataContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from METHOD-DATA SEQ to PA-DATA
        // --------------------------------------------------------------------------------------------
        // METHOD-DATA         ::= SEQUENCE OF <PA-DATA>
        //
        super.transitions[MethodDataStatesEnum.METHOD_DATA_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<MethodDataContainer>(
            MethodDataStatesEnum.METHOD_DATA_SEQ_STATE,
            MethodDataStatesEnum.METHOD_DATA_SEQ_STATE,
            UniversalTag.SEQUENCE.getValue(),
            new AddPaData() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the METHOD-DATA Grammar
     */
    public static Grammar<MethodDataContainer> getInstance()
    {
        return instance;
    }
}
