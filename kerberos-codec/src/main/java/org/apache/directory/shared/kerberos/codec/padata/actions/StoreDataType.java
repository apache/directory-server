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

package org.apache.directory.shared.kerberos.codec.padata.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoder;
import org.apache.directory.shared.asn1.ber.tlv.IntegerDecoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.padata.PaDataContainer;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Sets the PaData's data type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreDataType extends GrammarAction<PaDataContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreDataType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Creates a new instance of PaDataDataType.
     */
    public StoreDataType()
    {
        super( "PaDataData's padata-type" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( PaDataContainer paDataContainer ) throws DecoderException
    {
        TLV tlv = paDataContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        PaData paData = paDataContainer.getPaData();
        // The PaData's type is an integer
        Value value = tlv.getValue();

        try
        {
            int paDataType = IntegerDecoder.parse( value );

            paData.setPaDataType( PaDataType.getTypeByValue( paDataType ) );

            if ( IS_DEBUG )
            {
                LOG.debug( "padata-type : {}", paDataType );
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
