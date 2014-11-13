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
package org.apache.directory.shared.kerberos.codec.authenticator;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.AbstractContainer;
import org.apache.directory.shared.kerberos.messages.Authenticator;


/**
 * The Authenticator container stores the Authenticator decoded by the Asn1Decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticatorContainer extends AbstractContainer
{
    /** An Authenticator container */
    private Authenticator authenticator;


    /**
     * Creates a new AuthenticatorContainer object.
     * @param stream The stream containing the data to decode
     */
    public AuthenticatorContainer( ByteBuffer stream )
    {
        super( stream );
        this.grammar = AuthenticatorGrammar.getInstance();
        setTransition( AuthenticatorStatesEnum.START_STATE );
    }


    /**
     * @return Returns the Authenticator.
     */
    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    /**
     * Set a Authenticator Object into the container. It will be completed by the
     * KerberosDecoder.
     * 
     * @param authenticator The Authenticator to set.
     */
    public void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }
}
