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
package org.apache.directory.server.kerberos.shared.crypto.checksum;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RsaMd5Checksum extends ChecksumEngine
{
    public MessageDigest getDigest() throws NoSuchAlgorithmException
    {
        return MessageDigest.getInstance( "MD5" );
    }


    public ChecksumType checksumType()
    {
        return ChecksumType.RSA_MD5;
    }


    public CipherType keyType()
    {
        return CipherType.NULL;
    }


    public int checksumSize()
    {
        return 16;
    }


    public int keySize()
    {
        return 0;
    }


    public int confounderSize()
    {
        return 0;
    }


    public boolean isSafe()
    {
        return false;
    }


    public byte[] calculateKeyedChecksum( byte[] data, byte[] key )
    {
        return null;
    }


    public boolean verifyKeyedChecksum( byte[] data, byte[] key, byte[] checksum )
    {
        return false;
    }
}
