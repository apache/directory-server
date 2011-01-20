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
package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.paEncTimestamp.PaEncTimestampContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.PaEncTimestamp;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the PaEncTimestamp decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class PaEncTimestampDecoderTest
{
    /**
     * Test the decoding of a PaEncTimestamp
     */
    @Test
    public void testPaEncTimestamp()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x16 );
        
        stream.put( new byte[]
            { 
              0x30, 0x14,
                (byte)0xA0, 0x03,                 // etype
                  0x02, 0x01, 0x12,               //
                (byte)0xA1, 0x03,                 // kvno
                  0x02, 0x01, 0x05,               //
                (byte)0xA2, 0x08,                 // cipher
                  0x04, 0x06, 'a', 'b', 'c', 'd', 'e', 'f'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a PaEncTimestamp Container
        Asn1Container paEncTimestampContainer = new PaEncTimestampContainer();

        // Decode the PaEncTimestamp PDU
        try
        {
            kerberosDecoder.decode( stream, paEncTimestampContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded PaEncTimestamp
        PaEncTimestamp paEncTimestamp = ( ( PaEncTimestampContainer ) paEncTimestampContainer ).getPaEncTimestamp();

        assertEquals( EncryptionType.AES256_CTS_HMAC_SHA1_96, paEncTimestamp.getEType() );
        assertEquals( 5, paEncTimestamp.getKvno() );
        assertTrue( Arrays.equals( StringTools.getBytesUtf8( "abcdef" ), paEncTimestamp.getCipher() ) );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( paEncTimestamp.computeLength() );
        
        try
        {
            bb = paEncTimestamp.encode( bb );
    
            // Check the length
            assertEquals( 0x16, bb.limit() );
    
            String encodedPdu = StringTools.dumpBytes( bb.array() );
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
}
