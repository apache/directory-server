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
package org.apache.directory.shared.kerberos.codec.krbCred;


import org.apache.directory.shared.asn1.actions.CheckNotNullLength;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.krbCred.actions.CheckMsgType;
import org.apache.directory.shared.kerberos.codec.krbCred.actions.KrbCredInit;
import org.apache.directory.shared.kerberos.codec.krbCred.actions.StoreEncPart;
import org.apache.directory.shared.kerberos.codec.krbCred.actions.StorePvno;
import org.apache.directory.shared.kerberos.codec.krbCred.actions.StoreTickets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KrbCred structure. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KrbCredGrammar extends AbstractGrammar<KrbCredContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KrbCredGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KrbCredCredGrammar is a singleton */
    private static Grammar<KrbCredContainer> instance = new KrbCredGrammar();


    /**
     * Creates a new KrbCredGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KrbCredGrammar()
    {
        setName( KrbCredGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KrbCredStatesEnum.LAST_KRB_CRED_STATE.ordinal()][256];

        // ============================================================================================
        // KrbCred
        // ============================================================================================

        super.transitions[KrbCredStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_CRED_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.START_STATE, KrbCredStatesEnum.KRB_CRED_TAG_STATE, KerberosConstants.KRB_CRED_TAG,
                new KrbCredInit() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_TAG_STATE,
                KrbCredStatesEnum.KRB_CRED_SEQ_TAG_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<KrbCredContainer>() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_SEQ_TAG_STATE.ordinal()][KerberosConstants.KRB_CRED_PVNO_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_SEQ_TAG_STATE,
                KrbCredStatesEnum.KRB_CRED_PVNO_TAG_STATE,
                KerberosConstants.KRB_CRED_PVNO_TAG,
                new CheckNotNullLength<KrbCredContainer>() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_PVNO_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_PVNO_TAG_STATE,
                KrbCredStatesEnum.KRB_CRED_PVNO_STATE,
                UniversalTag.INTEGER,
                new StorePvno() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_PVNO_STATE.ordinal()][KerberosConstants.KRB_CRED_MSGTYPE_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_PVNO_STATE,
                KrbCredStatesEnum.KRB_CRED_MSGTYPE_TAG_STATE,
                KerberosConstants.KRB_CRED_MSGTYPE_TAG,
                new CheckNotNullLength<KrbCredContainer>() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_MSGTYPE_TAG_STATE.ordinal()][UniversalTag.INTEGER.getValue()] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_MSGTYPE_TAG_STATE,
                KrbCredStatesEnum.KRB_CRED_MSGTYPE_STATE,
                UniversalTag.INTEGER,
                new CheckMsgType() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_MSGTYPE_STATE.ordinal()][KerberosConstants.KRB_CRED_TICKETS_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_MSGTYPE_STATE,
                KrbCredStatesEnum.KRB_CRED_TICKETS_TAG_STATE,
                KerberosConstants.KRB_CRED_TICKETS_TAG,
                new CheckNotNullLength<KrbCredContainer>() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_TICKETS_TAG_STATE.ordinal()][UniversalTag.SEQUENCE.getValue()] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_TICKETS_TAG_STATE,
                KrbCredStatesEnum.KRB_CRED_TICKETS_STATE,
                UniversalTag.SEQUENCE,
                new CheckNotNullLength<KrbCredContainer>() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_TICKETS_STATE.ordinal()][KerberosConstants.TICKET_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_TICKETS_STATE,
                KrbCredStatesEnum.KRB_CRED_TICKETS_STATE,
                KerberosConstants.TICKET_TAG,
                new StoreTickets() );

        super.transitions[KrbCredStatesEnum.KRB_CRED_TICKETS_STATE.ordinal()][KerberosConstants.KRB_CRED_ENCPART_TAG] =
            new GrammarTransition<KrbCredContainer>(
                KrbCredStatesEnum.KRB_CRED_TICKETS_STATE,
                KrbCredStatesEnum.KRB_CRED_ENCPART_TAG_STATE,
                KerberosConstants.KRB_CRED_ENCPART_TAG,
                new StoreEncPart() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KrbCredInfo Grammar
     */
    public static Grammar<KrbCredContainer> getInstance()
    {
        return instance;
    }
}
