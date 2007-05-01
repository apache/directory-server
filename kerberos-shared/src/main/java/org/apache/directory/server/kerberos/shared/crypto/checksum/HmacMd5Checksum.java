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


import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HmacMd5Checksum extends ChecksumEngine
{
    HmacMd5Checksum()
    {
        // Package-scoped constructor; use ChecksumHandler. 
    }


    public ChecksumType checksumType()
    {
        return ChecksumType.HMAC_SHA1_DES3_KD;
    }


    public CipherType keyType()
    {
        return CipherType.DES3;
    }


    public byte[] calculateChecksum( byte[] data, byte[] key )
    {
        try
        {
            SecretKey sk = new SecretKeySpec( key, "DESede" );

            Mac mac = Mac.getInstance( "HmacSHA1" );
            mac.init( sk );

            return mac.doFinal( data );
        }
        catch ( GeneralSecurityException nsae )
        {
            nsae.printStackTrace();
            return null;
        }
    }
}
