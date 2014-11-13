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
package org.apache.directory.shared.kerberos.codec.etypeInfo.actions;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.etypeInfo.ETypeInfoContainer;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.ETypeInfoEntryContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add an ETypeInfoEntry object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddETypeInfoEntry extends GrammarAction<ETypeInfoContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AddETypeInfoEntry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AddETypeInfoEntry action.
     */
    public AddETypeInfoEntry()
    {
        super( "Add an ETypeInfoEntry instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ETypeInfoContainer eTypeInfoContainer ) throws DecoderException
    {
        TLV tlv = eTypeInfoContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the ETYPE-INFO-ENTRY
        Asn1Decoder etypeInfoEntryDecoder = new Asn1Decoder();

        ETypeInfoEntryContainer etypeInfoEntryContainer = new ETypeInfoEntryContainer();
        etypeInfoEntryContainer.setStream( eTypeInfoContainer.getStream() );

        // Compute the start position in the stream for the ETypeInfoEntry to decode :
        // We have to move back to the ETypeInfoEntry tag
        eTypeInfoContainer.rewind();

        // Decode the ETypeInfoEntry PDU
        try
        {
            etypeInfoEntryDecoder.decode( eTypeInfoContainer.getStream(), etypeInfoEntryContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        eTypeInfoContainer.updateParent();

        // Store the ETypeInfoEntry in the container
        ETypeInfoEntry etypeInfoEntry = etypeInfoEntryContainer.getETypeInfoEntry();
        eTypeInfoContainer.addEtypeInfoEntry( etypeInfoEntry );

        if ( IS_DEBUG )
        {
            LOG.debug( "ETYPE-INFO-ENTRY added : {}", etypeInfoEntry );
        }

        eTypeInfoContainer.setGrammarEndAllowed( true );
    }
}
