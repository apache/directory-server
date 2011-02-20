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
package org.apache.directory.shared.kerberos.codec;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.Grammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.apRep.ApRepContainer;
import org.apache.directory.shared.kerberos.codec.apReq.ApReqContainer;
import org.apache.directory.shared.kerberos.codec.asRep.AsRepContainer;
import org.apache.directory.shared.kerberos.codec.asReq.AsReqContainer;
import org.apache.directory.shared.kerberos.codec.krbCred.KrbCredContainer;
import org.apache.directory.shared.kerberos.codec.krbError.KrbErrorContainer;
import org.apache.directory.shared.kerberos.codec.krbPriv.KrbPrivContainer;
import org.apache.directory.shared.kerberos.codec.krbSafe.KrbSafeContainer;
import org.apache.directory.shared.kerberos.codec.tgsRep.TgsRepContainer;
import org.apache.directory.shared.kerberos.codec.tgsReq.TgsReqContainer;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the KerberosMessage message. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once. If
 * an action is to be added or modified, this is where the work is to be done !
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class KerberosMessageGrammar extends AbstractGrammar<KerberosMessageContainer>
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( KerberosMessageGrammar.class );

    /** A speedup for logger */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. KerberosMessageGrammar is a singleton */
    private static Grammar<KerberosMessageContainer> instance = new KerberosMessageGrammar();

    Asn1Decoder kerberosDecoder = new Asn1Decoder();

    private class DecodeKerberosMessage extends GrammarAction<KerberosMessageContainer>
    {
        public void action( KerberosMessageContainer kerberosMessageContainer ) throws DecoderException
        {
            ByteBuffer stream = kerberosMessageContainer.getStream();
            stream.rewind();

            TLV tlv = kerberosMessageContainer.getCurrentTLV();
            kerberosMessageContainer.setGrammarEndAllowed( true );

            // Now, depending on the T, call the inner decoder
            switch ( tlv.getTag() )
            {
                case KerberosConstants.AS_REQ_TAG:
                    AsReqContainer asReqContainer = new AsReqContainer( stream );

                    // Decode the AS_REQ PDU
                    try
                    {
                        kerberosDecoder.decode( stream, asReqContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage asReq = asReqContainer.getAsReq();
                    kerberosMessageContainer.setMessage( asReq );

                    break;

                case KerberosConstants.AS_REP_TAG:
                    AsRepContainer asRepContainer = new AsRepContainer( stream );

                    // Decode the AS-REP PDU
                    try
                    {
                        kerberosDecoder.decode( stream, asRepContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage asRep = asRepContainer.getAsRep();
                    kerberosMessageContainer.setMessage( asRep );

                    break;

                case KerberosConstants.TGS_REQ_TAG:
                    TgsReqContainer tgsReqContainer = new TgsReqContainer( stream );

                    // Decode the TGS-REQ PDU
                    try
                    {
                        kerberosDecoder.decode( stream, tgsReqContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage tgsReq = tgsReqContainer.getTgsReq();
                    kerberosMessageContainer.setMessage( tgsReq );

                    break;

                case KerberosConstants.TGS_REP_TAG:
                    TgsRepContainer tgsRepContainer = new TgsRepContainer( stream );

                    // Decode the TGS-REP PDU
                    try
                    {
                        kerberosDecoder.decode( stream, tgsRepContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage tgsRep = tgsRepContainer.getTgsRep();
                    kerberosMessageContainer.setMessage( tgsRep );

                    break;

                case KerberosConstants.AP_REQ_TAG:
                    ApReqContainer apReqContainer = new ApReqContainer( stream );

                    // Decode the AP-REQ PDU
                    try
                    {
                        kerberosDecoder.decode( stream, apReqContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage apReq = apReqContainer.getApReq();
                    kerberosMessageContainer.setMessage( apReq );
                    break;

                case KerberosConstants.AP_REP_TAG:
                    ApRepContainer apRepContainer = new ApRepContainer( stream );

                    // Decode the AP-REP PDU
                    try
                    {
                        kerberosDecoder.decode( stream, apRepContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage apRep = apRepContainer.getApRep();
                    kerberosMessageContainer.setMessage( apRep );
                    break;

                case KerberosConstants.KRB_SAFE_TAG:
                    KrbSafeContainer krbSafeContainer = new KrbSafeContainer( stream );

                    // Decode the KRB-SAFE PDU
                    try
                    {
                        kerberosDecoder.decode( stream, krbSafeContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage krbSafe = krbSafeContainer.getKrbSafe();
                    kerberosMessageContainer.setMessage( krbSafe );
                    break;

                case KerberosConstants.KRB_PRIV_TAG:
                    KrbPrivContainer krbPrivContainer = new KrbPrivContainer( stream );

                    // Decode the KRB-PRIV PDU
                    try
                    {
                        kerberosDecoder.decode( stream, krbPrivContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage krbPriv = krbPrivContainer.getKrbPriv();
                    kerberosMessageContainer.setMessage( krbPriv );
                    break;

                case KerberosConstants.KRB_CRED_TAG:
                    KrbCredContainer krbCredContainer = new KrbCredContainer( stream );

                    // Decode the KRB-CRED PDU
                    try
                    {
                        kerberosDecoder.decode( stream, krbCredContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage krbCred = krbCredContainer.getKrbCred();
                    kerberosMessageContainer.setMessage( krbCred );
                    break;

                case KerberosConstants.KRB_ERROR_TAG:
                    KrbErrorContainer krbErrorContainer = new KrbErrorContainer( stream );

                    // Decode the KRB-ERROR PDU
                    try
                    {
                        kerberosDecoder.decode( stream, krbErrorContainer );
                    }
                    catch ( DecoderException de )
                    {
                        throw de;
                    }

                    KerberosMessage krbError = krbErrorContainer.getKrbError();
                    kerberosMessageContainer.setMessage( krbError );
                    break;
            }

            // We are done, get out
            if ( IS_DEBUG )
            {
                LOG.debug( "Decoded KerberosMessage {}", kerberosMessageContainer.getMessage() );
            }
        }
    }


    /**
     * Creates a new KerberosMessageGrammar object.
     */
    @SuppressWarnings("unchecked")
    private KerberosMessageGrammar()
    {
        setName( KerberosMessageGrammar.class.getName() );

        // Create the transitions table
        super.transitions = new GrammarTransition[KerberosMessageStatesEnum.LAST_KERBEROS_MESSAGE_STATE.ordinal()][256];

        // ============================================================================================
        // Ticket
        // ============================================================================================
        // --------------------------------------------------------------------------------------------
        // Transition from START to Ticket
        // --------------------------------------------------------------------------------------------
        // This is the starting state :
        // Ticket          ::= [APPLICATION 1] ...
        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.AS_REQ_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.AS_REQ_STATE,
                KerberosConstants.AS_REQ_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.AS_REP_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.AS_REP_TAG_STATE,
                KerberosConstants.AS_REP_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.TGS_REQ_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.TGS_REQ_TAG_STATE,
                KerberosConstants.TGS_REQ_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.TGS_REP_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.TGS_REP_TAG_STATE,
                KerberosConstants.TGS_REP_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.AP_REQ_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.AP_REQ_TAG_STATE,
                KerberosConstants.AP_REQ_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.AP_REP_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.AP_REP_TAG_STATE,
                KerberosConstants.AP_REP_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_SAFE_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.KRB_SAFE_STATE,
                KerberosConstants.KRB_SAFE_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_PRIV_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.KRB_PRIV_STATE,
                KerberosConstants.KRB_PRIV_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_CRED_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.KRB_CRED_STATE,
                KerberosConstants.KRB_CRED_TAG,
                new DecodeKerberosMessage() );

        super.transitions[KerberosMessageStatesEnum.START_STATE.ordinal()][KerberosConstants.KRB_ERROR_TAG] =
            new GrammarTransition<KerberosMessageContainer>(
                KerberosMessageStatesEnum.START_STATE, KerberosMessageStatesEnum.KRB_ERROR_STATE,
                KerberosConstants.KRB_ERROR_TAG,
                new DecodeKerberosMessage() );
    }


    /**
     * Get the instance of this grammar
     *
     * @return An instance on the KerberosMessage Grammar
     */
    public static Grammar<KerberosMessageContainer> getInstance()
    {
        return instance;
    }
}
