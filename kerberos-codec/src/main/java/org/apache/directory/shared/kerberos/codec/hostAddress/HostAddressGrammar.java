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
package org.apache.directory.shared.kerberos.codec.hostAddress;


import org.apache.directory.api.asn1.actions.CheckNotNullLength;
import org.apache.directory.api.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.api.asn1.ber.grammar.Grammar;
import org.apache.directory.api.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.hostAddress.actions.HostAddressInit;
import org.apache.directory.shared.kerberos.codec.hostAddress.actions.StoreAddrType;
import org.apache.directory.shared.kerberos.codec.hostAddress.actions.StoreAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the HostAddress structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class HostAddressGrammar extends AbstractGrammar<HostAddressContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( HostAddressGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. HostAddressGrammar is a singleton */
    private static Grammar<HostAddressContainer> instance = new HostAddressGrammar();


    /**
     * Creates a new HostAddressGrammar object.
     */
    @SuppressWarnings("unchecked")
    private HostAddressGrammar()
    {
        setName( HostAddressGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[HostAddressStatesEnum.LAST_HOST_ADDRESS_STATE.ordinal()][256];

        // ============================================================================================
        // HostAddress
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from HostAddress init to HostAddress SEQ
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE
        super.transitions[HostAddressStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<HostAddressContainer>(
                HostAddressStatesEnum.START_STATE,
                HostAddressStatesEnum.HOST_ADDRESS_SEQ_STATE,
                UniversalTag.SEQUENCE,
                new HostAddressInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from HostAddress SEQ to addr-type tag
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE
        //         addr-type       [0]
        super.transitions[HostAddressStatesEnum.HOST_ADDRESS_SEQ_STATE.ordinal()][KerberosConstants.HOST_ADDRESS_ADDR_TYPE_TAG] =
            new GrammarTransition<HostAddressContainer>(
                HostAddressStatesEnum.HOST_ADDRESS_SEQ_STATE,
                HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_TAG_STATE,
                KerberosConstants.HOST_ADDRESS_ADDR_TYPE_TAG,
                new CheckNotNullLength<HostAddressContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from addr-type tag to addr-type value
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE
        //         addr-type       [0] Int32,
        super.transitions[HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER
            .getValue()] =
            new GrammarTransition<HostAddressContainer>(
                HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_TAG_STATE,
                HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_STATE,
                UniversalTag.INTEGER,
                new StoreAddrType() );

        // --------------------------------------------------------------------------------------------
        // Transition from addr-type value to address tag
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE
        //         ...
        //         address         [1]
        super.transitions[HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_STATE.ordinal()][KerberosConstants.HOST_ADDRESS_ADDRESS_TAG] =
            new GrammarTransition<HostAddressContainer>(
                HostAddressStatesEnum.HOST_ADDRESS_ADDR_TYPE_STATE,
                HostAddressStatesEnum.HOST_ADDRESS_ADDRESS_TAG_STATE,
                KerberosConstants.HOST_ADDRESS_ADDRESS_TAG,
                new CheckNotNullLength<HostAddressContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from address tag to address value
        // --------------------------------------------------------------------------------------------
        // HostAddress   ::= SEQUENCE
        //         ...
        //         address         [1] OCTET STRING
        super.transitions[HostAddressStatesEnum.HOST_ADDRESS_ADDRESS_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING
            .ordinal()] =
            new GrammarTransition<HostAddressContainer>(
                HostAddressStatesEnum.HOST_ADDRESS_ADDRESS_TAG_STATE,
                HostAddressStatesEnum.HOST_ADDRESS_ADDRESS_STATE,
                UniversalTag.OCTET_STRING,
                new StoreAddress() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the HostAddress Grammar
     */
    public static Grammar<HostAddressContainer> getInstance()
    {
        return instance;
    }
}
