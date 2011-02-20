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
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoder;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to read the PVNO
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadPvno<E extends Asn1Container> extends GrammarAction<E>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadPvno.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Creates a new instance of AbstractReadPvno.
     *
     * @param name the name of the action
     */
    public AbstractReadPvno( String name )
    {
        super( name );
    }


    /**
     * sets the pvno on the ASN.1 object present in the container
     *
     * @param pvno the protocol version number received
     * @param container the ASN.1 object's container
     */
    protected abstract void setPvno( int pvno, E container );


    /**
     * {@inheritDoc}
     */
    public final void action( E container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

        // The Length should not be null and should be 1
        if ( tlv.getLength() != 1 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        Value value = tlv.getValue();

        try
        {
            int pvno = IntegerDecoder.parse(value);

            if ( pvno != 5 )
            {
                LOG.error( I18n.err( I18n.ERR_04070, Strings.dumpBytes(value.getData()), "The PVNO should be 5" ) );

                // This will generate a PROTOCOL_ERROR
                throw new DecoderException( "The PVNO should be 5" );
            }

            if ( IS_DEBUG )
            {
                LOG.debug( "pvno : {}", pvno );
            }

            setPvno( pvno, container );
        }
        catch ( IntegerDecoderException ide )
        {
            LOG.error( I18n.err( I18n.ERR_04070, Strings.dumpBytes(value.getData()), ide
                .getLocalizedMessage() ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( ide.getMessage() );
        }
    }
}
