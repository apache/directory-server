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

package org.apache.directory.shared.kerberos.codec.encKrbCredPart.actions;


import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.codec.encKrbCredPart.EncKrbCredPartContainer;
import org.apache.directory.shared.kerberos.codec.krbCredInfo.KrbCredInfoContainer;
import org.apache.directory.shared.kerberos.components.KrbCredInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store EncKrbCredInfo ticket-info.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreTicketInfo extends GrammarAction<EncKrbCredPartContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreTicketInfo.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * {@inheritDoc}
     */
    public void action( EncKrbCredPartContainer encKrbCredPartContainer ) throws DecoderException
    {
        TLV tlv = encKrbCredPartContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // decoder for KrbCredInfo
        Asn1Decoder decoder = new Asn1Decoder();

        // KrbCredInfo container
        KrbCredInfoContainer ticketInfoContainer = new KrbCredInfoContainer();
        ticketInfoContainer.setStream( encKrbCredPartContainer.getStream() );

        encKrbCredPartContainer.rewind();

        try
        {
            // decode KrbCredInfo
            decoder.decode( encKrbCredPartContainer.getStream(), ticketInfoContainer );
        }
        catch ( DecoderException e )
        {
            throw e;
        }

        KrbCredInfo ticketInfo = ticketInfoContainer.getKrbCredInfo();
        // add KrbCredInfo to the list of ticket-info
        encKrbCredPartContainer.getEncKrbCredPart().addTicketInfo( ticketInfo );

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        encKrbCredPartContainer.updateParent();

        encKrbCredPartContainer.setGrammarEndAllowed( true );

        if ( IS_DEBUG )
        {
            LOG.debug( "KrbCredInfo : {}", ticketInfo );
        }
    }
}
