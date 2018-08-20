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
package org.apache.directory.shared.kerberos.codec.kdcReq.actions;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.kdcReq.KdcReqContainer;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.KdcReqBodyContainer;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the KDC_REQ-BODY
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreKdcReqBody extends GrammarAction<KdcReqContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreKdcReqBody.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreKdcReqBody action.
     */
    public StoreKdcReqBody()
    {
        super( "Stores the KDC-REQ-BODY" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( KdcReqContainer kdcReqContainer ) throws DecoderException
    {
        TLV tlv = kdcReqContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_01308_ZERO_LENGTH_TLV ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_01309_EMPTY_TLV ) );
        }

        // Now, let's decode the KDC-REQ-BODY
        Asn1Decoder kdcReqBodyDecoder = new Asn1Decoder();

        KdcReqBodyContainer kdcReqBodyContainer = new KdcReqBodyContainer( kdcReqContainer.getStream() );

        // Decode the KDC-REQ-BODY PDU
        kdcReqBodyDecoder.decode( kdcReqContainer.getStream(), kdcReqBodyContainer );

        // Store the KDC-REQ-BODY in the container
        KdcReqBody kdcReqBody = kdcReqBodyContainer.getKdcReqBody();
        KdcReq kdcReq = kdcReqContainer.getKdcReq();
        kdcReq.setKdcReqBody( kdcReqBody );

        // Update the parent
        kdcReqContainer.updateParent();

        kdcReqContainer.setGrammarEndAllowed( true );

        if ( IS_DEBUG )
        {
            LOG.debug( "KDC-REQ-BODY : {}", kdcReqBody );
        }
    }
}
