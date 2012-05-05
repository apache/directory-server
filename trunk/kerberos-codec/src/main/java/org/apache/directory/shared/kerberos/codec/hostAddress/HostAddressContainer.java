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
package org.apache.directory.shared.kerberos.codec.hostAddress;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.HostAddress;


/**
 * The HostAddress container stores the HostAddress decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HostAddressContainer extends AbstractContainer
{
    /** An HostAddress container */
    private HostAddress hostAddress;


    /**
     * Creates a new HostAddressContainer object.
     */
    public HostAddressContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = HostAddressGrammar.getInstance();
        setTransition( HostAddressStatesEnum.START_STATE );
    }


    /**
     * @return Returns the HostAddress.
     */
    public HostAddress getHostAddress()
    {
        return hostAddress;
    }


    /**
     * Set a HostAddress Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param hostAddress The HostAddress to set.
     */
    public void setHostAddress( HostAddress hostAddress )
    {
        this.hostAddress = hostAddress;
    }
}
