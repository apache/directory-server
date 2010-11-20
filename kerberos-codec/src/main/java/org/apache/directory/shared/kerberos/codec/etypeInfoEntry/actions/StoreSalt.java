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
package org.apache.directory.shared.kerberos.codec.etypeInfoEntry.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.ETypeInfoEntryContainer;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store the ETYPE-INFO-ENTRY cipher
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreSalt extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreSalt.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new StoreSalt action.
     */
    public StoreSalt()
    {
        super( "ETYPE-INFO-ENTRY salt" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( Asn1Container container ) throws DecoderException
    {
        ETypeInfoEntryContainer etypeInfoEntryContainer = ( ETypeInfoEntryContainer ) container;

        TLV tlv = etypeInfoEntryContainer.getCurrentTLV();
        ETypeInfoEntry etypeInfoEntry = etypeInfoEntryContainer.getETypeInfoEntry();

        // The Length may be null
        if ( tlv.getLength() != 0 ) 
        {
            Value value = tlv.getValue();
            
            // The encrypted data may be null
            if ( value.getData() == null ) 
            {
                etypeInfoEntry.setSalt( value.getData() );
            }
        }
        
        if ( IS_DEBUG )
        {
            LOG.debug( "salt : {}", StringTools.dumpBytes( etypeInfoEntry.getSalt() ) );
        }
        
        // We can end here
        etypeInfoEntryContainer.setGrammarEndAllowed( true );
    }
}
