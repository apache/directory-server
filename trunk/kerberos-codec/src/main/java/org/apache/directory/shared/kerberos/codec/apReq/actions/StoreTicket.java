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
package org.apache.directory.shared.kerberos.codec.apReq.actions;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.apReq.ApReqContainer;
import org.apache.directory.shared.kerberos.codec.ticket.TicketContainer;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the Ticket
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreTicket extends GrammarAction<ApReqContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreTicket.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreTicket action.
     */
    public StoreTicket()
    {
        super( "AP-REQ Store Ticket" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ApReqContainer apReqContainer ) throws DecoderException
    {
        TLV tlv = apReqContainer.getCurrentTLV();

        // The Length can't be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the Ticket
        Asn1Decoder ticketDecoder = new Asn1Decoder();

        TicketContainer ticketContainer = new TicketContainer( apReqContainer.getStream() );

        // Decode the Ticket PDU
        try
        {
            ticketDecoder.decode( apReqContainer.getStream(), ticketContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        apReqContainer.updateParent();

        // Store the Ticket in the container
        Ticket ticket = ticketContainer.getTicket();
        ApReq apReq = apReqContainer.getApReq();
        apReq.setTicket( ticket );

        if ( IS_DEBUG )
        {
            LOG.debug( "Stored ticket:  {}", ticket );
        }
    }
}
