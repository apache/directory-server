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
package org.apache.directory.shared.kerberos.codec.kdcRep.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.codec.padata.PaDataContainer;
import org.apache.directory.shared.kerberos.components.KdcRep;
import org.apache.directory.shared.kerberos.components.PaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add a PaData
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddPaData extends GrammarAction<KdcRepContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AddPaData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AddPaData action.
     */
    public AddPaData()
    {
        super( "KDC-REP Add PA-DATA" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( KdcRepContainer kdcRepContainer ) throws DecoderException
    {
        TLV tlv = kdcRepContainer.getCurrentTLV();

        // The Length can't be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the PA-DATA
        Asn1Decoder paDataDecoder = new Asn1Decoder();

        PaDataContainer paDataContainer = new PaDataContainer();
        paDataContainer.setStream( kdcRepContainer.getStream() );

        // We have to move back to the PA-DATA tag
        kdcRepContainer.rewind();

        // Decode the PA-DATA PDU
        try
        {
            paDataDecoder.decode( kdcRepContainer.getStream(), paDataContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        // Update the parent
        kdcRepContainer.updateParent();

        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Store the PData in the container
        PaData paData = paDataContainer.getPaData();
        KdcRep kdcRep = kdcRepContainer.getKdcRep();
        kdcRep.addPaData( paData );

        if ( IS_DEBUG )
        {
            LOG.debug( "Added PA-DATA:  {}", paData );
        }
    }
}
