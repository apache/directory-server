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
package org.apache.directory.shared.kerberos.codec.asReq;


import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.asReq.actions.StoreKdcReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the AS-REQ structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class AsReqGrammar extends AbstractGrammar<AsReqContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( AsReqGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. AsReqGrammar is a singleton */
    private static Grammar<AsReqContainer> instance = new AsReqGrammar();


    /**
     * Creates a new AsReqGrammar object.
     */
    @SuppressWarnings("unchecked")
    private AsReqGrammar()
    {
        setName( AsReqGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[AsReqStatesEnum.LAST_AS_REQ_STATE.ordinal()][256];

        // ============================================================================================
        // AS-REQ
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from AS-REQ init to KDC-REQ
        // --------------------------------------------------------------------------------------------
        // AS-REQ          ::= [APPLICATION 10] KDC-REQ
        super.transitions[AsReqStatesEnum.START_STATE.ordinal()][KerberosConstants.AS_REQ_TAG] =
            new GrammarTransition<AsReqContainer>(
                AsReqStatesEnum.START_STATE,
                AsReqStatesEnum.AS_REQ_STATE,
                KerberosConstants.AS_REQ_TAG,
                new StoreKdcReq() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the AS-REQ Grammar
     */
    public static Grammar<AsReqContainer> getInstance()
    {
        return instance;
    }
}
