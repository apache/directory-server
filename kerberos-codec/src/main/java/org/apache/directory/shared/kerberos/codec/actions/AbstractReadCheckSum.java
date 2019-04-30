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


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.checksum.ChecksumContainer;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to set the Checksum
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadCheckSum<E extends Asn1Container> extends GrammarAction<E>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadCheckSum.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AbstractReadChecksum action.
     */
    public AbstractReadCheckSum( String name )
    {
        super( name );
    }


    /**
     * set the Checksum on the ASN.1 object of the container
     *
     * @param checksum the Checksum object
     * @param container container holding the ASN.1 object
     */
    protected abstract void setChecksum( Checksum checksum, E container );


    /**
     * {@inheritDoc}
     */
    public final void action( E container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_01308_ZERO_LENGTH_TLV ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_01309_EMPTY_TLV ) );
        }

        // Now, let's decode the Checksum
        ChecksumContainer checksumContainer = new ChecksumContainer();

        // Decode the Checksum PDU
        Asn1Decoder.decode( container.getStream(), checksumContainer );

        Checksum checksum = checksumContainer.getChecksum();

        if ( IS_DEBUG )
        {
            LOG.debug( "Checksum : {}", checksum );
        }

        setChecksum( checksum, container );

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        container.updateParent();
    }
}
