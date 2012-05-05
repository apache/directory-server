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
package org.apache.directory.shared.kerberos.codec.encAsRepPart.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.EncKdcRepPartContainer;
import org.apache.directory.shared.kerberos.codec.encAsRepPart.EncAsRepPartContainer;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add a EncAsRepPart object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreEncAsRepPart extends GrammarAction<EncAsRepPartContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreEncAsRepPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new EncAsRepPart action.
     */
    public StoreEncAsRepPart()
    {
        super( "Add an EncAsRepPart instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( EncAsRepPartContainer encAsRepPartContainer ) throws DecoderException
    {
        TLV tlv = encAsRepPartContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the EncKdcRepPart
        Asn1Decoder encKdcRepPartDecoder = new Asn1Decoder();

        EncKdcRepPartContainer encKdcRepPartContainer = new EncKdcRepPartContainer( encAsRepPartContainer.getStream() );

        // Decode the EncKdcRepPart PDU
        try
        {
            encKdcRepPartDecoder.decode( encAsRepPartContainer.getStream(), encKdcRepPartContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        encAsRepPartContainer.updateParent();

        EncKdcRepPart encKdcRepPart = encKdcRepPartContainer.getEncKdcRepPart();

        encAsRepPartContainer.getEncAsRepPart().setEncKdcRepPart( encKdcRepPart );

        if ( IS_DEBUG )
        {
            LOG.debug( "EncAsRepPart : {}", encKdcRepPart );
        }

        encAsRepPartContainer.setGrammarEndAllowed( true );
    }
}
