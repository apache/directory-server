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
package org.apache.directory.shared.kerberos.codec.adKdcIssued.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.AdKdcIssuedContainer;
import org.apache.directory.shared.kerberos.codec.authorizationData.AuthorizationDataContainer;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the AD-KDCIssued 'elements' value
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreElements extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreElements.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreElements action.
     */
    public StoreElements()
    {
        super( "AdKdcIssued 'elements' value" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( Asn1Container container ) throws DecoderException
    {
        AdKdcIssuedContainer adKdcIssuedContainer = ( AdKdcIssuedContainer ) container;

        TLV tlv = adKdcIssuedContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }
        
        // Now, let's decode the AuthorizationData
        Asn1Decoder authorizationDataDecoder = new Asn1Decoder();
        
        AuthorizationDataContainer authorizationDataContainer = new AuthorizationDataContainer();
        authorizationDataContainer.setStream( container.getStream() );
        
        // Decode the AuthorizationData PDU
        try
        {
            authorizationDataDecoder.decode( container.getStream(), authorizationDataContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }
        
        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        container.updateParent();

        // Store the AuthorizationData in the container
        AuthorizationData elements = authorizationDataContainer.getAuthorizationData();
        adKdcIssuedContainer.getAdKdcIssued().setElements( elements );
        

        if ( IS_DEBUG )
        {
            LOG.debug( "Elements added : {}", elements );
        }
        
        container.setGrammarEndAllowed( true );
    }
}
