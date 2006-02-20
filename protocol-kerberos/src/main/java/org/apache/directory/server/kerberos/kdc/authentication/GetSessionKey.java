/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.kdc.authentication;


import java.security.SecureRandom;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.service.DesStringToKey;
import org.apache.directory.server.protocol.shared.chain.Context;


public class GetSessionKey extends DesStringToKey
{
    private static final SecureRandom random = new SecureRandom();


    public boolean execute( Context context ) throws Exception
    {
        AuthenticationContext authContext = ( AuthenticationContext ) context;
        authContext.setSessionKey( getNewSessionKey() );

        return CONTINUE_CHAIN;
    }


    private EncryptionKey getNewSessionKey()
    {
        byte[] confounder = new byte[8];

        // SecureRandom.nextBytes is already synchronized
        random.nextBytes( confounder );

        byte[] subSessionKey = getKey( new String( confounder ) );

        return new EncryptionKey( EncryptionType.DES_CBC_MD5, subSessionKey );
    }
}
