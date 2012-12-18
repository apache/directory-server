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
package org.apache.directory.shared.kerberos.codec.krbSafe.actions;


import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.codec.krbSafe.KrbSafeContainer;
import org.apache.directory.shared.kerberos.codec.krbSafeBody.KrbSafeBodyContainer;
import org.apache.directory.shared.kerberos.components.KrbSafeBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to read the KrbSafeBody
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreSafeBody extends GrammarAction<KrbSafeContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreSafeBody.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreSafeBody action.
     */
    public StoreSafeBody()
    {
        super( "KRB-SAFE safe-body" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( KrbSafeContainer krbSafeContainer ) throws DecoderException
    {
        TLV tlv = krbSafeContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the HostAddress
        Asn1Decoder krbSafeBodyDecoder = new Asn1Decoder();

        KrbSafeBodyContainer krbSafeBodyContainer = new KrbSafeBodyContainer();

        // Passes the Stream to the decoder
        krbSafeBodyContainer.setStream( krbSafeContainer.getStream() );

        // Decode the KrbSafeBody PDU
        try
        {
            krbSafeBodyDecoder.decode( krbSafeContainer.getStream(), krbSafeBodyContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Store the KrbSafeBody in the container
        KrbSafeBody krbSafeBody = krbSafeBodyContainer.getKrbSafeBody();

        if ( IS_DEBUG )
        {
            LOG.debug( "KrbSafeBody : {}", krbSafeBody );
        }

        krbSafeContainer.getKrbSafe().setSafeBody( krbSafeBody );

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        krbSafeContainer.updateParent();
    }
}
