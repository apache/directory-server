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
package org.apache.directory.shared.kerberos.codec.etypeInfo;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.codec.etypeInfo.actions.AddETypeInfoEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ETYPE-INFO structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ETypeInfoGrammar extends AbstractGrammar<ETypeInfoContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ETypeInfoGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ETypeInfoGrammar is a singleton */
    private static Grammar<ETypeInfoContainer> instance = new ETypeInfoGrammar();


    /**
     * Creates a new ETypeInfoGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ETypeInfoGrammar()
    {
        setName( ETypeInfoGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ETypeInfoStatesEnum.LAST_ETYPE_INFO_STATE.ordinal()][256];

        // ============================================================================================
        // ETYPE-INFO
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO init to ETYPE-INFO SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE
        super.transitions[ETypeInfoStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfoContainer>(
                ETypeInfoStatesEnum.START_STATE,
                ETypeInfoStatesEnum.ETYPE_INFO_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<ETypeInfoContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO init to ETYPE-INFO SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO-ENTRY         ::= SEQUENCE OF <ETYPE-INFO-ENTRY>
        //
        super.transitions[ETypeInfoStatesEnum.ETYPE_INFO_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfoContainer>(
                ETypeInfoStatesEnum.ETYPE_INFO_SEQ_STATE,
                ETypeInfoStatesEnum.ETYPE_INFO_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new AddETypeInfoEntry() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the ETYPE-INFO Grammar
     */
    public static Grammar<ETypeInfoContainer> getInstance()
    {
        return instance;
    }
}
