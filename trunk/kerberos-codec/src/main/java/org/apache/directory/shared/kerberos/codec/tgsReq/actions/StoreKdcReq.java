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
package org.apache.directory.shared.kerberos.codec.tgsReq.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.kdcReq.KdcReqContainer;
import org.apache.directory.shared.kerberos.codec.tgsReq.TgsReqContainer;
import org.apache.directory.shared.kerberos.messages.TgsReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add a KDC-REQ object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreKdcReq extends GrammarAction<TgsReqContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreKdcReq.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new KDC-REQ action.
     */
    public StoreKdcReq()
    {
        super( "Add an KDC-REQ instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( TgsReqContainer tgsReqContainer ) throws DecoderException
    {
        TLV tlv = tgsReqContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the KDC-REQ
        Asn1Decoder kdcReqDecoder = new Asn1Decoder();

        KdcReqContainer kdcReqContainer = new KdcReqContainer( tgsReqContainer.getStream() );

        // Store the created TGS-REQ object into the KDC-REQ container
        TgsReq tgsReq = new TgsReq();
        kdcReqContainer.setKdcReq( tgsReq );

        // Decode the KDC_REQ PDU
        try
        {
            kdcReqDecoder.decode( tgsReqContainer.getStream(), kdcReqContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        tgsReqContainer.updateParent();

        if ( tgsReq.getMessageType() != KerberosMessageType.TGS_REQ )
        {
            throw new DecoderException( "Bad message type" );
        }

        tgsReqContainer.setTgsReq( tgsReq );

        if ( IS_DEBUG )
        {
            LOG.debug( "TGS-REQ : {}", tgsReq );
        }

        tgsReqContainer.setGrammarEndAllowed( true );
    }
}
