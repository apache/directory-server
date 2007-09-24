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

import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;


/**
 * Compute a checksum using a CRC32 algorithm
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class Crc32Checksum implements ChecksumEngine
{
    /**
     * Return the checksum type. Here, CRC32.
     */
    public ChecksumType checksumType()
    {
        return ChecksumType.CRC32;
    }

    /**
     * Compute the checksum
     * 
     * @param data the data for which the checksum is computed
     * @param key Not used
     * @param usage Not used
     * 
     * @return the data checksum, as a four bytes array.
     */
    public byte[] calculateChecksum( byte[] data, byte[] key, KeyUsage usage )
    {
        CRC32 crc32 = new CRC32();
        crc32.update( data );
        int value = ( int ) crc32.getValue();

        byte[] bytes = new byte[4];
        
        bytes[0] = ( byte ) ( ( value >> 24 ) & 0x00FF );
        bytes[1] = ( byte ) ( ( value >> 16 ) & 0x00FF );
        bytes[2] = ( byte ) ( ( value >> 8 ) & 0x00FF );
        bytes[3] = ( byte ) ( value & 0x00FF );
        
        return bytes;
    }
}