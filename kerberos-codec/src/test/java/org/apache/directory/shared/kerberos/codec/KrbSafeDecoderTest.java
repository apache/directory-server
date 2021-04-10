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

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.krbSafe.KrbSafeContainer;
import org.apache.directory.shared.kerberos.messages.KrbSafe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for KrbSafe codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbSafeDecoderTest
{

    @Test
    public void testDecodeKrbSafe()
    {
        byte[] data = new byte[]
            {
                0x74, 0x36,
                0x30, 0x34,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x14,
                ( byte ) 0xA2,
                0x19, // safe-body
                0x30,
                0x17,
                ( byte ) 0xA0,
                0x04,
                0x04,
                0x02,
                0x00,
                0x01,
                ( byte ) 0xA4,
                0x0F,
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x06,
                0x04,
                0x04,
                127,
                0,
                0,
                1,
                ( byte ) 0xA3,
                0x0D, // cksum
                0x30,
                0x0B,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x04,
                0x04,
                0x02,
                0x00,
                0x01
        };

        String decoded = Strings.dumpBytes( data );
        int streamLen = data.length;
        ByteBuffer stream = ByteBuffer.wrap( data );

        KrbSafeContainer container = new KrbSafeContainer( stream );

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }

        KrbSafe krbSafe = container.getKrbSafe();

        assertEquals( 5, krbSafe.getProtocolVersionNumber() );
        assertEquals( KerberosMessageType.KRB_SAFE, krbSafe.getMessageType() );
        assertNotNull( krbSafe.getChecksum() );
        assertNotNull( krbSafe.getSafeBody() );

        int encodedLen = krbSafe.computeLength();
        assertEquals( streamLen, encodedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( encodedLen );
            krbSafe.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeWithIncorrectPdu() throws DecoderException
    {
        byte[] data = new byte[]
            {
                0x74, 0xC,
                0x30, 0xA,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x14,
        };

        ByteBuffer stream = ByteBuffer.wrap( data );

        KrbSafeContainer container = new KrbSafeContainer( stream );

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }
}
