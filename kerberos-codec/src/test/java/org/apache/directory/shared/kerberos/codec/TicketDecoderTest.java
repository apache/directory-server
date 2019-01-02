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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.ticket.TicketContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TicketDecoderTest
{
    /**
     * Test the decoding of a Ticket message
     */
    @Test
    public void testDecodeFullTicket()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x46 );

        stream.put( new byte[]
            { 0x61, 0x44, // Ticket
                0x30,
                0x42,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA3,
                0x16, // enc-part
                0x030,
                0x14,
                ( byte ) 0xA0,
                0x03, // etype
                0x02,
                0x01,
                0x12,
                ( byte ) 0xA1,
                0x03, // kvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA2,
                0x08, // cipher
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        try
        {
            Asn1Decoder.decode( stream, ticketContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded BindRequest
        Ticket ticket = ( ( TicketContainer ) ticketContainer ).getTicket();

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
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "abcdef" ), encryptedData.getCipher() ) );

        // Check the encoding
        try
        {
            ByteBuffer bb = ticket.encode( null );

            // Check the length
            assertEquals( 0x46, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            ee.printStackTrace();
            fail( ee.getMessage() );
        }
    }


    /**
     * Test the decoding of an empty Ticket message
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmpty() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x61, 0x00 } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an empty Ticket sequence
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptySEQ() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x61, 0x02,
                0x30, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an empty tktvno tag
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyTktVnoTag() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x61, 0x04,
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an empty tktvno value
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyTktVnoValue() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );

        stream.put( new byte[]
            {
                0x61, 0x06,
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an bad tktvno value
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketBadTktVnoValue() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x61, 0x07,
                0x30, 0x05,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x02
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with no realm
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketNoRealm() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x61, 0x07,
                0x30, 0x05,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an empty realm tag
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyRealmTag() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            {
                0x61, 0x09,
                0x30, 0x07,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of an empty realm value
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyRealmValue() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0D );

        stream.put( new byte[]
            {
                0x61, 0x0B,
                0x30, 0x09,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x02,
                0x1B, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with no sname
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketNoSname() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x18 );

        stream.put( new byte[]
            {
                0x61, 0x16,
                0x30, 0x14,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B, 'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with an empty sname tag
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptySnameTag() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x1A );

        stream.put( new byte[]
            {
                0x61, 0x18,
                0x30, 0x16,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B, 'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with an empty sname value
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptySnameValue() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x1C );

        stream.put( new byte[]
            {
                0x61, 0x1A,
                0x30, 0x18,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B, 'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x02,
                0x30, 0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with a bad principalName
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketBadSName() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x20 );

        stream.put( new byte[]
            {
                0x61, 0x1E, // Ticket
                0x30,
                0x1C,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x06, // sname
                0x30,
                0x04,
                ( byte ) 0xA0,
                0x02, // name-type
                0x02,
                0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with no enc-part
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketNoEncPart() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x2E );

        stream.put( new byte[]
            {
                0x61, 0x2C, // Ticket
                0x30,
                0x2A,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with an empty enc-part tag
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyEncPartTag() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x30 );

        stream.put( new byte[]
            {
                0x61, 0x2E, // Ticket
                0x30,
                0x2C,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA3,
                0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with an empty enc-part
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketEmptyEncPart() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x32 );

        stream.put( new byte[]
            {
                0x61, 0x30, // Ticket
                0x30,
                0x2E,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA3,
                0x02,
                0x30,
                0x00
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }


    /**
     * Test the decoding of a ticket with a bad enc-part
     */
    @Test(expected = DecoderException.class)
    public void testDecodeTicketBadEncPart() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x34 );

        stream.put( new byte[]
            {
                0x61, 0x32, // Ticket
                0x30,
                0x30,
                ( byte ) 0xA0,
                0x03, // tkt-vno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D, // realm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x14, // sname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA3,
                0x04,
                0x30,
                0x02,
                0x01,
                0x02
        } );

        stream.flip();

        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        Asn1Decoder.decode( stream, ticketContainer );
    }
}
