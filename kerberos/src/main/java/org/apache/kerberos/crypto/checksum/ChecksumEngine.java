/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.kerberos.crypto.checksum;

import org.apache.kerberos.crypto.encryption.CipherType;
import org.bouncycastle.crypto.Digest;

public abstract class ChecksumEngine
{
    public abstract Digest getDigest();
    public abstract ChecksumType checksumType();
    public abstract CipherType keyType();
    public abstract int checksumSize();
    public abstract int keySize();
    public abstract int confounderSize();
    public abstract boolean isSafe();
    public abstract byte[] calculateKeyedChecksum( byte[] data, byte[] key );
    public abstract boolean verifyKeyedChecksum( byte[] data, byte[] key, byte[] checksum );

    public byte[] calculateChecksum( byte[] data )
    {
        Digest digester = getDigest();

        digester.reset();
        digester.update( data, 0, data.length );
        byte[] returnValue = new byte[ digester.getDigestSize() ];
        digester.doFinal( returnValue, 0 );
        return returnValue;
    }
}
