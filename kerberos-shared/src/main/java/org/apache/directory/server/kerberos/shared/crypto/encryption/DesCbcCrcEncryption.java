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


import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumEngine;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.checksum.Crc32Checksum;


public class DesCbcCrcEncryption extends DesCbcEncryption
{
    public ChecksumEngine getChecksumEngine()
    {
        return new Crc32Checksum();
    }


    public EncryptionType encryptionType()
    {
        return EncryptionType.DES_CBC_CRC;
    }


    public ChecksumType checksumType()
    {
        return ChecksumType.CRC32;
    }


    public CipherType cipherType()
    {
        return CipherType.DES;
    }


    public int confounderSize()
    {
        return 8;
    }


    public int checksumSize()
    {
        return 4;
    }


    public int minimumPadSize()
    {
        return 4;
    }
}
