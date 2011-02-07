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
package org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoder;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.ETypeInfo2EntryContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the ETYPE-INFO2-ENTRY etype
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreEType extends GrammarAction<ETypeInfo2EntryContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreEType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreEType action.
     */
    public StoreEType()
    {
        super( "ETYPE-INFO2-ENTRY etype" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ETypeInfo2EntryContainer eTypeInfo2EntryContainer ) throws DecoderException
    {
        TLV tlv = eTypeInfo2EntryContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // The etype is an integer
        Value value = tlv.getValue();

        EncryptionType etype = null;
        ETypeInfo2Entry etypeInfo2Entry = eTypeInfo2EntryContainer.getETypeInfo2Entry();

        try
        {
            int eType = IntegerDecoder.parse( value );
            etype = EncryptionType.getTypeByValue( eType );

            etypeInfo2Entry.setEType( etype );

            if ( IS_DEBUG )
            {
                LOG.debug( "etype : " + etype );
            }

            // The next tag is optional, we can end here
            eTypeInfo2EntryContainer.setGrammarEndAllowed( true );
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
