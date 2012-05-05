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
package org.apache.directory.shared.kerberos.codec.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.hostAddress.HostAddressContainer;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to read the HostAddress
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadHostAddress<E extends Asn1Container> extends GrammarAction<E>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadHostAddress.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AbstractReadAddress action.
     */
    public AbstractReadHostAddress( String name )
    {
        super( name );
    }


    protected abstract void setAddress( HostAddress hostAddress, E container );


    /**
     * {@inheritDoc}
     */
    public void action( E container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the HostAddress
        Asn1Decoder hostAddressDecoder = new Asn1Decoder();

        HostAddressContainer hostAddressContainer = new HostAddressContainer();

        // Passes the Stream to the decoder
        hostAddressContainer.setStream( container.getStream() );

        // Decode the HostAddress PDU
        try
        {
            hostAddressDecoder.decode( container.getStream(), hostAddressContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Store the HostAddress in the container
        HostAddress hostAddress = hostAddressContainer.getHostAddress();

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        container.updateParent();

        setAddress( hostAddress, container );

        if ( IS_DEBUG )
        {
            LOG.debug( "HostAddress : {}", hostAddress );
        }
    }
}
