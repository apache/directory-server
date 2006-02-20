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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.DESedeEngine;


public abstract class Des3CbcEncryption extends EncryptionEngine
{
    public BlockCipher getBlockCipher()
    {
        return new DESedeEngine();
    }


    public CipherType keyType()
    {
        return CipherType.DES3;
    }


    public int blockSize()
    {
        return 8;
    }


    public int keySize()
    {
        return 24;
    }
}
