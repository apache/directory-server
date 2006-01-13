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

import java.util.zip.CRC32;

import org.apache.kerberos.crypto.encryption.CipherType;
import org.bouncycastle.crypto.Digest;

public class Crc32Checksum extends ChecksumEngine
{
    public Digest getDigest()
    {
        return new CRC32Digest();
    }

    public ChecksumType checksumType()
    {
        return ChecksumType.CRC32;
    }

    public CipherType keyType()
    {
        return CipherType.NULL;
    }

    public int checksumSize()
    {
        return 4;
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

    private class CRC32Digest implements Digest
    {
        private CRC32 crc32 = new CRC32();

        public String getAlgorithmName()
        {
            return "CRC-32";
        }

        public int getDigestSize()
        {
            return 4;
        }

        public void reset()
        {
            crc32.reset();
        }

        public void update( byte in )
        {
            crc32.update( in );
        }

        public void update( byte[] in, int inOff, int len )
        {
            crc32.update( in, inOff, len );
        }

        public int doFinal( byte[] out, int outOff )
        {
            out = int2octet( (int) crc32.getValue() );

            return 0;
        }

        private byte[] int2octet( int value )
        {
            byte[] bytes = new byte[ 4 ];
            int i, shift;

            for ( i = 0, shift = 24; i < 4; i++, shift -= 8 )
            {
                bytes[ i ] = (byte) ( 0xFF & ( value >> shift ) );
            }

            return bytes;
        }
    }
}
