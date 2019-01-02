/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec.encTicketPart.actions;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.encTicketPart.EncTicketPartContainer;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.TransitedEncodingContainer;
import org.apache.directory.shared.kerberos.components.TransitedEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store the transited value of EncTicketPart.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreTransited extends GrammarAction<EncTicketPartContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreTransited.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Creates a new instance of StoreTransited.
     */
    public StoreTransited()
    {
        super( "EncTicketPart transited" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( EncTicketPartContainer encTicketPartContainer ) throws DecoderException
    {
        TLV tlv = encTicketPartContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_01308_ZERO_LENGTH_TLV ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_01309_EMPTY_TLV ) );
        }

        TransitedEncodingContainer transitedContainer = new TransitedEncodingContainer();

        // Now, let's decode the TransitedEncoding
        Asn1Decoder.decode( encTicketPartContainer.getStream(), transitedContainer );

        TransitedEncoding te = transitedContainer.getTransitedEncoding();

        if ( IS_DEBUG )
        {
            LOG.debug( "TransitedEncoding {}", te );
        }

        encTicketPartContainer.getEncTicketPart().setTransited( te );

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        encTicketPartContainer.updateParent();
    }
}
