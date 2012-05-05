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
package org.apache.directory.shared.kerberos.codec.etypeInfo2;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.codec.etypeInfo2.actions.AddETypeInfo2Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the ETYPE-INFO2 structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ETypeInfo2Grammar extends AbstractGrammar<ETypeInfo2Container>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ETypeInfo2Grammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ETypeInfo2Grammar is a singleton */
    private static Grammar<ETypeInfo2Container> instance = new ETypeInfo2Grammar();


    /**
     * Creates a new ETypeInfo2Grammar object.
     */
    @SuppressWarnings("unchecked")
    private ETypeInfo2Grammar()
    {
        setName( ETypeInfo2Grammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ETypeInfo2StatesEnum.LAST_ETYPE_INFO2_STATE.ordinal()][256];

        // ============================================================================================
        // ETYPE-INFO2
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO2 init to ETYPE-INFO2 SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE
        super.transitions[ETypeInfo2StatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfo2Container>(
                ETypeInfo2StatesEnum.START_STATE,
                ETypeInfo2StatesEnum.ETYPE_INFO2_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<ETypeInfo2Container>() );

        // --------------------------------------------------------------------------------------------
        // Transition from ETYPE-INFO2 init to ETYPE-INFO2 SEQ
        // --------------------------------------------------------------------------------------------
        // ETYPE-INFO2-ENTRY         ::= SEQUENCE OF <ETYPE-INFO2-ENTRY>
        //
        super.transitions[ETypeInfo2StatesEnum.ETYPE_INFO2_SEQ_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ETypeInfo2Container>(
                ETypeInfo2StatesEnum.ETYPE_INFO2_SEQ_STATE,
                ETypeInfo2StatesEnum.ETYPE_INFO2_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new AddETypeInfo2Entry() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the ETYPE-INFO Grammar
     */
    public static Grammar<ETypeInfo2Container> getInstance()
    {
        return instance;
    }
}
