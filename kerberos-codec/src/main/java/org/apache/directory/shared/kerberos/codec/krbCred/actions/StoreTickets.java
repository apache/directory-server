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

package org.apache.directory.shared.kerberos.codec.krbCred.actions;


import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.codec.krbCred.KrbCredContainer;
import org.apache.directory.shared.kerberos.codec.ticket.TicketContainer;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store  tickets.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreTickets extends GrammarAction<KrbCredContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreTickets.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * {@inheritDoc}
     */
    public void action( KrbCredContainer krbCredContainer ) throws DecoderException
    {
        TLV tlv = krbCredContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // decoder for Ticket
        Asn1Decoder decoder = new Asn1Decoder();

        // Ticket container
        TicketContainer ticketContainer = new TicketContainer( krbCredContainer.getStream() );

        krbCredContainer.rewind();

        try
        {
            // decode Ticket
            decoder.decode( krbCredContainer.getStream(), ticketContainer );
        }
        catch ( DecoderException e )
        {
            throw e;
        }

        Ticket ticket = ticketContainer.getTicket();
        // add Ticket to the list of tickets
        krbCredContainer.getKrbCred().addTicket( ticket );

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        krbCredContainer.updateParent();

        if ( IS_DEBUG )
        {
            LOG.debug( "Ticket : {}", ticket );
        }
    }
}
