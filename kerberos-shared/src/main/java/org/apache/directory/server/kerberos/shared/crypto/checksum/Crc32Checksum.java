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


import java.util.zip.CRC32;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Crc32Checksum implements ChecksumEngine
{
    public ChecksumType checksumType()
    {
        return ChecksumType.CRC32;
    }


    public CipherType keyType()
    {
        return CipherType.NULL;
    }


    public byte[] calculateChecksum( byte[] data, byte[] key, KeyUsage usage )
    {
        CRC32 crc32 = new CRC32();
        crc32.update( data );

        return int2octet( ( int ) crc32.getValue() );
    }


    private byte[] int2octet( int value )
    {
        byte[] bytes = new byte[4];
        int i, shift;

        for ( i = 0, shift = 24; i < 4; i++, shift -= 8 )
        {
            bytes[i] = ( byte ) ( 0xFF & ( value >> shift ) );
        }

        return bytes;
    }
}
