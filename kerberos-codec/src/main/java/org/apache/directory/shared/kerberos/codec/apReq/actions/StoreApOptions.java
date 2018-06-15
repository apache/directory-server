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
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.apReq.ApReqContainer;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the ApOptions
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreApOptions extends GrammarAction<ApReqContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreApOptions.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreApOptions action.
     */
    public StoreApOptions()
    {
        super( "Stores the ApOptions" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ApReqContainer apReqContainer ) throws DecoderException
    {
        TLV tlv = apReqContainer.getCurrentTLV();

        // The Length should not be null, and should be 5
        if ( tlv.getLength() != 5 )
        {
            LOG.error( I18n.err( I18n.ERR_01308_ZERO_LENGTH_TLV ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_01309_EMPTY_TLV ) );
        }

        ApReq apReq = apReqContainer.getApReq();
        ApOptions apOptions = new ApOptions( tlv.getValue().getData() );

        apReq.setApOptions( apOptions );

        if ( IS_DEBUG )
        {
            LOG.debug( "APOptions : {}", apOptions );
        }
    }
}
