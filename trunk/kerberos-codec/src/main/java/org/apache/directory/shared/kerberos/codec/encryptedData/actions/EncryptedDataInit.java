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
package org.apache.directory.shared.kerberos.codec.encryptedData.actions;


import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.grammar.GrammarAction;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.encryptedData.EncryptedDataContainer;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize the EncryptedData object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncryptedDataInit extends GrammarAction<EncryptedDataContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( EncryptedDataInit.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new EncryptedDataInit action.
     */
    public EncryptedDataInit()
    {
        super( "Creates a EncryptedData instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( EncryptedDataContainer encryptedDataContainer ) throws DecoderException
    {
        TLV tlv = encryptedDataContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_744_NULL_PDU_LENGTH ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_744_NULL_PDU_LENGTH ) );
        }

        if ( encryptedDataContainer.getEncryptedData() == null )
        {
            EncryptedData encryptedData = new EncryptedData();
            encryptedDataContainer.setEncryptedData( encryptedData );

            if ( IS_DEBUG )
            {
                LOG.debug( "EncryptedData created" );
            }
        }
    }
}
