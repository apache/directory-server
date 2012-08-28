/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec.checksum.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoder;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.checksum.ChecksumContainer;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Sets the Checksum's type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreCksumType extends GrammarAction<ChecksumContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreCksumType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Creates a new instance of ChecksumReadType.
     */
    public StoreCksumType()
    {
        super( "Checksum cksumtype" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( ChecksumContainer checksumContainer ) throws DecoderException
    {
        TLV tlv = checksumContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        Checksum checksum = checksumContainer.getChecksum();
        // The Checksum's type is an integer
        Value value = tlv.getValue();

        try
        {
            int cksumType = IntegerDecoder.parse( value );

            checksum.setChecksumType( ChecksumType.getTypeByValue( cksumType ) );

            if ( IS_DEBUG )
            {
                LOG.debug( "cksumType : " + cksumType );
            }
        }
        catch ( IntegerDecoderException ide )
        {
            LOG.error( I18n.err( I18n.ERR_04070, Strings.dumpBytes( value.getData() ), ide
                .getLocalizedMessage() ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( ide.getMessage() );
        }

    }

}
