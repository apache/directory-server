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
package org.apache.directory.shared.kerberos.codec.hostAddress.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.actions.AbstractReadOctetString;
import org.apache.directory.shared.kerberos.codec.hostAddress.HostAddressContainer;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add the HostAddress address value
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreAddress extends AbstractReadOctetString
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreAddress.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * Instantiates a new HostAddressAddress action.
     */
    public StoreAddress()
    {
        super( "Store the HostAddress' address" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setOctetString( byte[] data, Asn1Container container )
    {
        HostAddressContainer hostAddressContainer = ( HostAddressContainer ) container;
        hostAddressContainer.getHostAddress().setAddress( data );
        container.setGrammarEndAllowed( true );
        
        if ( IS_DEBUG )
        {
            LOG.debug( "Address : {}", Strings.utf8ToString(data) );
        }
    }
}
