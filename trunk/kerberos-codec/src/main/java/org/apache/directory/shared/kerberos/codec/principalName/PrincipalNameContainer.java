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
package org.apache.directory.shared.kerberos.codec.principalName;


import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * The PrincipalName container stores the PrincipalName decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalNameContainer extends AbstractContainer
{
    /** A PrincipalName container */
    private PrincipalName principalName;


    /**
     * Creates a new PrincipalNameContainer object.
     */
    public PrincipalNameContainer()
    {
        super();
        this.grammar = PrincipalNameGrammar.getInstance();
        setTransition( PrincipalNameStatesEnum.START_STATE );
    }


    /**
     * @return Returns the PrincipalName.
     */
    public PrincipalName getPrincipalName()
    {
        return principalName;
    }


    /**
     * Set a PrincipalName Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param principalName The principalName to set.
     */
    public void setPrincipalName( PrincipalName principalName )
    {
        this.principalName = principalName;
    }
}
