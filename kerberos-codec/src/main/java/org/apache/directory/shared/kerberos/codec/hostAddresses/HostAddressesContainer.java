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
package org.apache.directory.shared.kerberos.codec.hostAddresses;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;


/**
 * The HostAddresses container stores the HostAddresses decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HostAddressesContainer extends AbstractContainer
{
    /** The list of HostAddress */
    private HostAddresses hostAddresses = new HostAddresses();


    /**
     * Creates a new HostAddressesContainer object.
     */
    public HostAddressesContainer()
    {
        super();
        this.grammar = HostAddressesGrammar.getInstance();
        setTransition( HostAddressesStatesEnum.START_STATE );
    }


    /**
     * @return Returns the HostAddresses.
     */
    public HostAddresses getHostAddresses()
    {
        return hostAddresses;
    }


    /**
     * Add a HostAddress Object into the list. It will be completed by the
     * KerberosDecoder.
     * 
     * @param hostAddress The HostAddress to add.
     */
    public void addHostAddress( HostAddress hostAddress )
    {
        this.hostAddresses.addHostAddress( hostAddress );
    }
}
