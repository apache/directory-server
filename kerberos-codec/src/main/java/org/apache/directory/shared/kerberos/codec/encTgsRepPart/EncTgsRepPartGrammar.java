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
package org.apache.directory.shared.kerberos.codec.encTgsRepPart;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.encTgsRepPart.actions.StoreEncTgsRepPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EncTgsRepPart structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class EncTgsRepPartGrammar extends AbstractGrammar<EncTgsRepPartContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( EncTgsRepPartGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. EncTgsRepPartGrammar is a singleton */
    private static Grammar<EncTgsRepPartContainer> instance = new EncTgsRepPartGrammar();


    /**
     * Creates a new EncTgsRepPartGrammar object.
     */
    @SuppressWarnings("unchecked")
    private EncTgsRepPartGrammar()
    {
        setName( EncTgsRepPartGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[EncTgsRepPartStatesEnum.LAST_ENC_TGS_REP_PART_STATE.ordinal()][256];

        // ============================================================================================
        // EncTgsRepPart
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from EncTgsRepPart init to EncKDCRepPart
        // --------------------------------------------------------------------------------------------
        // EncASRepPart    ::= [APPLICATION 25] EncKDCRepPart
        super.transitions[EncTgsRepPartStatesEnum.START_STATE.ordinal()][KerberosConstants.ENC_TGS_REP_PART_TAG] =
            new GrammarTransition<EncTgsRepPartContainer>(
                EncTgsRepPartStatesEnum.START_STATE,
                EncTgsRepPartStatesEnum.ENC_TGS_REP_PART_STATE,
                KerberosConstants.ENC_TGS_REP_PART_TAG,
                new StoreEncTgsRepPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the EncAsRepPart Grammar
     */
    public static Grammar<EncTgsRepPartContainer> getInstance()
    {
        return instance;
    }
}
