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
package org.apache.directory.shared.kerberos.codec.authorizationData;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.AuthorizationData;


/**
 * The AuthorizationData container stores the AuthorizationData decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationDataContainer extends AbstractContainer
{
    /** An AuthorizationData container */
    private AuthorizationData authorizationData;


    /**
     * Creates a new AuthorizationDataContainer object.
     */
    public AuthorizationDataContainer()
    {
        super();
        this.stateStack = new int[1];
        this.grammar = AuthorizationDataGrammar.getInstance();
        setTransition( AuthorizationDataStatesEnum.START_STATE );
    }


    /**
     * @return Returns the AuthorizationData.
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * Set a AuthorizationData Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param authorizationData The AuthorizationData to set.
     */
    public void setAuthorizationData( AuthorizationData authorizationData )
    {
        this.authorizationData = authorizationData;
    }
}
