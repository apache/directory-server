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
package org.apache.directory.shared.kerberos.codec.checksum;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.checksum.actions.ChecksumInit;
import org.apache.directory.shared.kerberos.codec.checksum.actions.StoreChecksum;
import org.apache.directory.shared.kerberos.codec.checksum.actions.StoreCksumType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Checksum structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ChecksumGrammar extends AbstractGrammar<ChecksumContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( ChecksumGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. ChecksumGrammar is a singleton */
    private static Grammar<ChecksumContainer> instance = new ChecksumGrammar();


    /**
     * Creates a new ChecksumGrammar object.
     */
    @SuppressWarnings("unchecked")
    private ChecksumGrammar()
    {
        setName( ChecksumGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[ChecksumStatesEnum.LAST_CHECKSUM_STATE.ordinal()][256];

        // ============================================================================================
        // Checksum
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from Checksum init to Checksum SEQ OF
        // --------------------------------------------------------------------------------------------
        // Checksum   ::= SEQUENCE OF
        super.transitions[ChecksumStatesEnum.START_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<ChecksumContainer>(
                ChecksumStatesEnum.START_STATE,
                ChecksumStatesEnum.CHECKSUM_SEQ_STATE,
                UniversalTag.SEQUENCE.getValue(),
                new ChecksumInit() );

        // --------------------------------------------------------------------------------------------
        // Transition from Checksum SEQ to checksumtype
        // --------------------------------------------------------------------------------------------
        // Checksum      ::= SEQUENCE {
        //        cksumtype       [0]
        super.transitions[ChecksumStatesEnum.CHECKSUM_SEQ_STATE.ordinal()][KerberosConstants.CHECKSUM_TYPE_TAG] =
            new GrammarTransition<ChecksumContainer>(
                ChecksumStatesEnum.CHECKSUM_SEQ_STATE,
                ChecksumStatesEnum.CHECKSUM_TYPE_TAG_STATE,
                KerberosConstants.CHECKSUM_TYPE_TAG,
                new CheckNotNullLength<ChecksumContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from checksumtype tag to checksumtype
        // --------------------------------------------------------------------------------------------
        // Checksum      ::= SEQUENCE {
        //          cksumtype       [0] Int32
        super.transitions[ChecksumStatesEnum.CHECKSUM_TYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<ChecksumContainer>(
                ChecksumStatesEnum.CHECKSUM_TYPE_TAG_STATE,
                ChecksumStatesEnum.CHECKSUM_TYPE_STATE,
                UniversalTag.INTEGER.getValue(),
                new StoreCksumType() );

        // --------------------------------------------------------------------------------------------
        // Transition from checksumtype to checksum tag
        // --------------------------------------------------------------------------------------------
        // Checksum      ::= SEQUENCE {
        //          checksum        [1]
        super.transitions[ChecksumStatesEnum.CHECKSUM_TYPE_STATE.ordinal()][KerberosConstants.CHECKSUM_CHECKSUM_TAG] =
            new GrammarTransition<ChecksumContainer>(
                ChecksumStatesEnum.CHECKSUM_TYPE_STATE,
                ChecksumStatesEnum.CHECKSUM_CHECKSUM_TAG_STATE,
                KerberosConstants.CHECKSUM_CHECKSUM_TAG,
                new CheckNotNullLength<ChecksumContainer>() );

        // --------------------------------------------------------------------------------------------
        // Transition from checksum tag to checksum value
        // --------------------------------------------------------------------------------------------
        // Checksum      ::= SEQUENCE {
        //          checksum        [1] OCTET STRING
        super.transitions[ChecksumStatesEnum.CHECKSUM_CHECKSUM_TAG_STATE.ordinal()][UniversalTag.OCTET_STRING.getValue()] =
            new GrammarTransition<ChecksumContainer>(
                ChecksumStatesEnum.CHECKSUM_CHECKSUM_TAG_STATE,
                ChecksumStatesEnum.CHECKSUM_CHECKSUM_STATE,
                UniversalTag.OCTET_STRING.getValue(),
                new StoreChecksum() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the Checksum Grammar
     */
    public static Grammar<ChecksumContainer> getInstance()
    {
        return instance;
    }
}
