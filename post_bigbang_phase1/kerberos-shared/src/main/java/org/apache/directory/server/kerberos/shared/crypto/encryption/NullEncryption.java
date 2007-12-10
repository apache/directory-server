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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class NullEncryption extends EncryptionEngine
{
    public EncryptionType getEncryptionType()
    {
        return EncryptionType.NULL;
    }


    public int getChecksumLength()
    {
        return 0;
    }


    public int getConfounderLength()
    {
        return 0;
    }


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data, KeyUsage usage ) throws KerberosException
    {
        return data.getCipher();
    }


    public EncryptedData getEncryptedData( EncryptionKey key, byte[] plainText, KeyUsage usage )
    {
        return new EncryptedData( getEncryptionType(), key.getKeyVersion(), plainText );
    }


    public byte[] encrypt( byte[] plainText, byte[] keyBytes )
    {
        return plainText;
    }


    public byte[] decrypt( byte[] cipherText, byte[] keyBytes )
    {
        return cipherText;
    }


    public byte[] calculateIntegrity( byte[] plainText, byte[] key, KeyUsage usage )
    {
        return null;
    }
}
