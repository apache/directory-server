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
package org.apache.directory.shared.kerberos.codec.etypeInfo2.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.etypeInfo2.ETypeInfo2Container;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.ETypeInfo2EntryContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add an ETypeInfo2Entry object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddETypeInfo2Entry extends GrammarAction<ETypeInfo2Container>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AddETypeInfo2Entry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AddETypeInfo2Entry action.
     */
    public AddETypeInfo2Entry()
    {
        super( "Add an ETypeInfo2Entry instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ETypeInfo2Container eTypeInfo2Container ) throws DecoderException
    {
        TLV tlv = eTypeInfo2Container.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the ETYPE-INFO2-ENTRY
        Asn1Decoder etypeInfo2EntryDecoder = new Asn1Decoder();

        ETypeInfo2EntryContainer etypeInfo2EntryContainer = new ETypeInfo2EntryContainer();
        etypeInfo2EntryContainer.setStream( eTypeInfo2Container.getStream() );

        // Compute the start position in the stream for the ETypeInfoEntry to decode :
        // We have to move back to the ETypeInfoEntry tag
        eTypeInfo2Container.rewind();

        // Decode the ETypeInfo2Entry PDU
        try
        {
            etypeInfo2EntryDecoder.decode( eTypeInfo2Container.getStream(), etypeInfo2EntryContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        eTypeInfo2Container.updateParent();

        // Store the ETypeInfoEntry in the container
        ETypeInfo2Entry etypeInfo2Entry = etypeInfo2EntryContainer.getETypeInfo2Entry();
        eTypeInfo2Container.addEtypeInfo2Entry( etypeInfo2Entry );

        if ( IS_DEBUG )
        {
            LOG.debug( "ETYPE-INFO2-ENTRY added : {}", etypeInfo2Entry );
        }

        eTypeInfo2Container.setGrammarEndAllowed( true );
    }
}
