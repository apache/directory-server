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


import org.apache.directory.api.asn1.actions.AbstractReadInteger;
import org.apache.directory.shared.kerberos.codec.hostAddress.HostAddressContainer;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize the HostAddress object
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreAddrType extends AbstractReadInteger<HostAddressContainer>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( StoreAddrType.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new HostAddressInit action.
     */
    public StoreAddrType()
    {
        super( "Creates a HostAddress instance", 0, Integer.MAX_VALUE );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setIntegerValue( int value, HostAddressContainer hostAddressContainer )
    {
        HostAddress hostAddressData = hostAddressContainer.getHostAddress();

        HostAddrType hostAddrType = HostAddrType.getTypeByOrdinal( value );
        hostAddressData.setAddrType( hostAddrType );

        if ( IS_DEBUG )
        {
            LOG.debug( "addr-type : {}", hostAddrType );
        }
    }
}
