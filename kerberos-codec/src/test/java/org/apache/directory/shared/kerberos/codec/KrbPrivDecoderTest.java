/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.krbPriv.KrbPrivContainer;
import org.apache.directory.shared.kerberos.messages.KrbPriv;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;

/**
 * Test cases for KrbPriv codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbPrivDecoderTest
{
    @Test
    public void testKrbPrivDecoderTest() throws Exception
    {
        byte[] data = new byte[]{
            0x75, 0x1B,
              0x30, 0x19,
               (byte)0xA0, 0x03,        // pvno
                      0x02, 0x01, 0x05,
               (byte)0xA1, 0x03,        // msg-type
                      0x02, 0x01, 0x15,
               (byte)0xA3, 0x0D,
                      0x30, 0x0B,
                       (byte)0xA0, 0x03,
                              0x02, 0x01, 0x03,
                       (byte)0xA2, 0x04,
                              0x04, 0x02, 0x00, 0x01 
        };

        int streamLen = data.length;
        ByteBuffer stream = ByteBuffer.wrap( data );
        
        String decoded = Strings.dumpBytes(stream.array());
        
        Asn1Decoder decoder = new Asn1Decoder();
        
        KrbPrivContainer container = new  KrbPrivContainer( stream );
        
        try
        {
            decoder.decode( stream, container );
        }
        catch( DecoderException e )
        {
            fail();
        }
        
        KrbPriv krbPriv = container.getKrbPriv();
        
        assertEquals( 5, krbPriv.getProtocolVersionNumber() );
        assertEquals( KerberosMessageType.KRB_PRIV, krbPriv.getMessageType() );
        assertNotNull( krbPriv.getEncPart() );
        
        int encodedLen = krbPriv.computeLength();
        assertEquals( streamLen, encodedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( encodedLen );
            krbPriv.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }
}
