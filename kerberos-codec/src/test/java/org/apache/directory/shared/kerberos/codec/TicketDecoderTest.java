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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionType;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.components.PrincipalNameType;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class TicketDecoderTest
{
    /**
     * Test the decoding of a Ticket message
     */
    @Test
    public void testDecodeTicket()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x46 );
        byte LL = 0;
        
        stream.put( new byte[]
            { 0x61, 0x44,                               // Ticket
                0x30, 0x42,
                  (byte)0xA0, 0x03,                     // tkt-vno
                    0x02, 0x01, 0x05,
                  (byte)0xA1, 0x0D,                     // realm
                    0x1B, 0x0B, 'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                  (byte)0xA2, 0x14,                     // sname
                    0x30, 0x12,
                      (byte)0xA0, 0x03,                 // name-type
                        0x02, 0x01, 0x01,
                      (byte)0xA1, 0x0B,                 // name-string
                        0x30, 0x09,
                          0x1B, 0x07, 'h', 'n', 'e', 'l', 's', 'o', 'n',
                  (byte)0xA3, 0x16,                     // enc-part
                    0x030, 0x14,
                      (byte)0xA0, 0x03,                 // etype
                        0x02, 0x01, 0x12,
                      (byte)0xA1, 0x03,                 // kvno
                        0x02, 0x01, 0x05,
                      (byte)0xA2, 0x08,                 // cipher
                        0x04, 0x06,
                          'a', 'b', 'c', 'd', 'e', 'f'
            } );

        String decodedPdu = StringTools.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a KerberosMessage Container
        Asn1Container kerberosMessageContainer = new KerberosMessageContainer();
        kerberosMessageContainer.setStream( stream );

        // Decode the Ticket PDU
        try
        {
            kerberosDecoder.decode( stream, kerberosMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
            fail( de.getMessage() );
        }

        // Check the decoded BindRequest
        Ticket ticket = ( ( KerberosMessageContainer ) kerberosMessageContainer ).getTicket();

        assertEquals( 5, ticket.getTktVno() );
        assertEquals( "EXAMPLE.COM", ticket.getRealm() );
        
        PrincipalName principalName = ticket.getSName();

        assertNotNull( principalName );
        assertEquals( PrincipalNameType.KRB_NT_PRINCIPAL, principalName.getNameType() );
        assertTrue( principalName.getNames().contains( "hnelson" ) );
        
        EncryptedData encryptedData = ticket.getEncPart();
        
        assertNotNull( encryptedData );
        assertEquals( EncryptionType.AES256_CTS_HMAC_SHA1_96, encryptedData.getEType() );
        assertEquals( 5, encryptedData.getKvno() );
        assertTrue( Arrays.equals( StringTools.getBytesUtf8( "abcdef" ), encryptedData.getCipher() ) );

        // Check the encoding
        try
        {
            ByteBuffer bb = ticket.encode();

            // Check the length
            assertEquals( 0x46, bb.limit() );

            String encodedPdu = StringTools.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }
}
