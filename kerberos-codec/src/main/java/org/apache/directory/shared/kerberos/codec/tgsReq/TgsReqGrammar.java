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
package org.apache.directory.shared.kerberos.codec.tgsReq;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.tgsReq.actions.StoreKdcReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the TGS-REQ structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TgsReqGrammar extends AbstractGrammar<TgsReqContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( TgsReqGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. TgsReqGrammar is a singleton */
    private static Grammar<TgsReqContainer> instance = new TgsReqGrammar();


    /**
     * Creates a new TgsReqGrammar object.
     */
    @SuppressWarnings("unchecked")
    private TgsReqGrammar()
    {
        setName( TgsReqGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[TgsReqStatesEnum.LAST_TGS_REQ_STATE.ordinal()][256];

        // ============================================================================================
        // TS-REQ
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from TS-REQ init to KDC-REQ
        // --------------------------------------------------------------------------------------------
        // TGS-REQ          ::= [APPLICATION 12] KDC-REQ
        super.transitions[TgsReqStatesEnum.START_STATE.ordinal()][KerberosConstants.TGS_REQ_TAG] =
            new GrammarTransition<TgsReqContainer>(
                TgsReqStatesEnum.START_STATE,
                TgsReqStatesEnum.TGS_REQ_STATE,
                KerberosConstants.TGS_REQ_TAG,
                new StoreKdcReq() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AS-REQ Grammar
     */
    public static Grammar<TgsReqContainer> getInstance()
    {
        return instance;
    }
}
