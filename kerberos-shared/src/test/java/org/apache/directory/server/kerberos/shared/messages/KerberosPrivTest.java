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
package org.apache.directory.server.kerberos.shared.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;

/**
 * Test the KRB-PRIV encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosPrivTest extends TestCase
{
    public void testKrbPrivBase() throws Exception
    {
        KerberosPriv kp = new KerberosPriv();
        
        EncryptedData ed = new EncryptedData( 
            EncryptionType.AES128_CTS_HMAC_SHA1_96, 1, 
            new byte[] { 0x01, 0x02, 0x03, 0x04 } );
        
        kp.setEncPart( ed );
        
        ByteBuffer encoded = ByteBuffer.allocate( kp.computeLength() );
        
        kp.encode( encoded );
        
        byte[] expectedResult = new byte[]
            {
              0x75, 0x22,
                0x30, 0x20,
                  (byte)0xA0, 0x03,
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x03,
                    0x02, 0x01, 0x15,
                  (byte)0xA3, 0x14,
                    0x30, 0x12, 
                    (byte)0xA0, 0x03, 
                      0x02, 0x01, 0x11, 
                    (byte)0xA1, 0x03, 
                      0x02, 0x01, 0x01, 
                    (byte)0xA2, 0x06, 
                      0x04, 0x04, 0x01, 0x02, 0x03, 0x04 
            };

        assertEquals( StringTools.dumpBytes( expectedResult ), StringTools.dumpBytes( encoded.array() ) );
        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    public void testKrbNoEncryptedData() throws Exception
    {
        KerberosPriv kp = new KerberosPriv();
        
        try
        {
            kp.encode( null );
            fail(); // We should not reach this point : null enc-part is not allowed
        }
        catch ( EncoderException ee )
        {
            assertTrue( true );
        }
    }
}
