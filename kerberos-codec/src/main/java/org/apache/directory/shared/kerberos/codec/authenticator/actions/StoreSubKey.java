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
package org.apache.directory.shared.kerberos.codec.authenticator.actions;


import org.apache.directory.shared.kerberos.codec.actions.AbstractReadEncryptionKey;
import org.apache.directory.shared.kerberos.codec.authenticator.AuthenticatorContainer;
import org.apache.directory.shared.kerberos.components.EncryptionKey;


/**
 * The action used to set the Authenticator subkey
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreSubKey extends AbstractReadEncryptionKey<AuthenticatorContainer>
{
    /**
     * Instantiates a new StoreEncryptionKey action.
     */
    public StoreSubKey()
    {
        super( "Authenticator subkey" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEncryptionKey( EncryptionKey encryptionKey, AuthenticatorContainer authenticatorContainer )
    {
        authenticatorContainer.getAuthenticator().setSubKey( encryptionKey );
        authenticatorContainer.setGrammarEndAllowed( true );
    }
}
