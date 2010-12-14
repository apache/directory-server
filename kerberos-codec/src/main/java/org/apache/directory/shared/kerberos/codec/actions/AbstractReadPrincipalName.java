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
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to set the ticket SName
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractReadPrincipalName extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReadPrincipalName.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new AbstractReadPrincipalName action.
     */
    public AbstractReadPrincipalName( String name )
    {
        super( name );
    }


    /**
     * set the PrincipalName on the ASN.1 object of the container
     *   
     * @param principalName the principal
     * @param container container holding the ASN.1 object
     */
    protected abstract void setPrincipalName( PrincipalName principalName, Asn1Container container );


    /**
     * {@inheritDoc}
     */
    public final void action( Asn1Container container ) throws DecoderException
    {
        TLV tlv = container.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        // Now, let's decode the PrincipalName
        Asn1Decoder principalNameDecoder = new Asn1Decoder();

        PrincipalNameContainer principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        try
        {
            principalNameDecoder.decode( container.getStream(), principalNameContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }

        PrincipalName principalName = principalNameContainer.getPrincipalName();

        if ( IS_DEBUG )
        {
            LOG.debug( "PrincipalName : " + principalName );
        }

        setPrincipalName( principalName, container );
        
        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        container.updateParent();
    }
}
