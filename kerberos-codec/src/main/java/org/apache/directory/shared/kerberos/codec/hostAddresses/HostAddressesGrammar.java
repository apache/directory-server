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
package org.apache.directory.shared.kerberos.codec.hostAddresses;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.codec.hostAddresses.actions.AddHostAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the HostAddresses structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class HostAddressesGrammar extends AbstractGrammar<HostAddressesContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( HostAddressesGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. HostAddressesGrammar is a singleton */
    private static Grammar<HostAddressesContainer> instance = new HostAddressesGrammar();


    /**
     * Creates a new HostAddressGrammar object.
     */
    @SuppressWarnings("unchecked")
    private HostAddressesGrammar()
    {
        setName( HostAddressesGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[HostAddressesStatesEnum.LAST_HOST_ADDRESSES_STATE.ordinal()][256];

        // ============================================================================================
        // HostAddresses
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from HostAddresses init to HostAddresses SEQUENCE OF
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE OF
        super.transitions[HostAddressesStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<HostAddressesContainer>(
                HostAddressesStatesEnum.START_STATE,
                HostAddressesStatesEnum.HOST_ADDRESSES_ADDRESS_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<HostAddressesContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from HostAddresses SEQ to HostAddress
        // --------------------------------------------------------------------------------------------
        // HostAddresses   ::= SEQUENCE OF
        //         HostAddress
        super.transitions[HostAddressesStatesEnum.HOST_ADDRESSES_ADDRESS_STATE.ordinal()][UniversalTag.SEQUENCE
            .getValue()] =
            new GrammarTransition<HostAddressesContainer>(
                HostAddressesStatesEnum.HOST_ADDRESSES_ADDRESS_STATE,
                HostAddressesStatesEnum.HOST_ADDRESSES_ADDRESS_STATE,
                UniversalTag.SEQUENCE,
                new AddHostAddress() );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the instance of this grammar
     *
     * @return An instance on the HostAddresses Grammar
     */
    public static Grammar<HostAddressesContainer> getInstance()
    {
        return instance;
    }
}
