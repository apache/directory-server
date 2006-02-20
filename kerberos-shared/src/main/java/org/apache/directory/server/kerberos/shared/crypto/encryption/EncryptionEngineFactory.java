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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


public class EncryptionEngineFactory
{
    public static EncryptionEngine getEncryptionEngineFor( EncryptionKey key ) throws KerberosException
    {
        int type = key.getKeyType().getOrdinal();

        switch ( type )
        {
            case 0:
                return new NullEncryption();
            case 1:
                return new DesCbcCrcEncryption();
            case 2:
                return new DesCbcMd4Encryption();
            case 3:
                return new DesCbcMd5Encryption();
            case 5:
                return new Des3CbcMd5Encryption();
            case 7:
                return new Des3CbcSha1Encryption();
        }

        throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
    }
}
