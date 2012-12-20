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
package org.apache.directory.shared.kerberos.codec.tgsRep;


import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.tgsRep.actions.StoreKdcRep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the TGS-REP structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TgsRepGrammar extends AbstractGrammar<TgsRepContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( TgsRepGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. TgsRepGrammar is a singleton */
    private static Grammar<TgsRepContainer> instance = new TgsRepGrammar();


    /**
     * Creates a new TgsRepGrammar object.
     */
    @SuppressWarnings("unchecked")
    private TgsRepGrammar()
    {
        setName( TgsRepGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[TgsRepStatesEnum.LAST_TGS_REP_STATE.ordinal()][256];

        // ============================================================================================
        // TS-REP
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from TS-REP init to KDC-REP
        // --------------------------------------------------------------------------------------------
        // TGS-REP          ::= [APPLICATION 13] KDC-REP
        super.transitions[TgsRepStatesEnum.START_STATE.ordinal()][KerberosConstants.TGS_REP_TAG] =
            new GrammarTransition<TgsRepContainer>(
                TgsRepStatesEnum.START_STATE,
                TgsRepStatesEnum.TGS_REP_STATE,
                KerberosConstants.TGS_REP_TAG,
                new StoreKdcRep() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AS-REP Grammar
     */
    public static Grammar<TgsRepContainer> getInstance()
    {
        return instance;
    }
}
