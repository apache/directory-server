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
package org.apache.directory.shared.kerberos.codec.asRep;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.asRep.actions.StoreKdcRep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AS-REP structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AsRepGrammar extends AbstractGrammar<AsRepContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AsRepGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AsRepGrammar is a singleton */
    private static Grammar<AsRepContainer> instance = new AsRepGrammar();


    /**
     * Creates a new AsRepGrammar object.
     */
    @SuppressWarnings("unchecked")
    private AsRepGrammar()
    {
        setName( AsRepGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AsRepStatesEnum.LAST_AS_REP_STATE.ordinal()][256];

        // ============================================================================================
        // AS-REP
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AS-REP init to KDC-REP
        // --------------------------------------------------------------------------------------------
        // AS-REP          ::= [APPLICATION 11] KDC-REP
        super.transitions[AsRepStatesEnum.START_STATE.ordinal()][KerberosConstants.AS_REP_TAG] =
            new GrammarTransition<AsRepContainer>(
                AsRepStatesEnum.START_STATE,
                AsRepStatesEnum.AS_REP_STATE,
                KerberosConstants.AS_REP_TAG,
                new StoreKdcRep() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AS-REP Grammar
     */
    public static Grammar<AsRepContainer> getInstance()
    {
        return instance;
    }
}
