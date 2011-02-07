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
package org.apache.directory.shared.kerberos.codec.krbPriv.actions;


import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.krbPriv.KrbPrivContainer;
import org.apache.directory.shared.kerberos.messages.KrbPriv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize the KrbPriv object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbPrivInit extends GrammarAction<KrbPrivContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( KrbPrivInit.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new KrbPrivInit action.
     */
    public KrbPrivInit()
    {
        super( "Creates a KrbPriv instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( KrbPrivContainer krbPrivContainer ) throws DecoderException
    {
        TLV tlv = krbPrivContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }

        KrbPriv krbPriv = new KrbPriv();
        krbPrivContainer.setKrbPriv( krbPriv );

        if ( IS_DEBUG )
        {
            LOG.debug( "KrbPriv created" );
        }
    }
}
