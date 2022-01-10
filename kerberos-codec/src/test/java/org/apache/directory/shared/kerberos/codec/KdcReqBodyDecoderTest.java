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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.kdcReqBody.KdcReqBodyContainer;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



/**
 * Test the decoder for a KdcReqBody
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcReqBodyDecoderTest
{
    /**
     * Test the decoding of a KdcReqBody message
     */
    @Test
    public void testDecodeFullKdcReqBody() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x15B );

        stream.put( new byte[]
            {
                0x30, ( byte ) 0x82, 0x01, 0x57,
                ( byte ) 0xA0, 0x07,
                0x03, 0x05,
                0x00, 0x01, 0x04, 0x00, 0x32,
                ( byte ) 0xA1, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                'c', 'l', 'i', 'e', 'n', 't',
                ( byte ) 0xA2, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA3, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                's', 'e', 'r', 'v', 'e', 'r',
                ( byte ) 0xA4, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA5, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA6, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA7, 0x04,
                0x02, 0x02,
                0x30, 0x39,
                ( byte ) 0xA8, 0x0B,
                0x30, 0x09,
                0x02, 0x01, 0x06,
                0x02, 0x01, 0x11,
                0x02, 0x01, 0x12,
                ( byte ) 0xA9, 0x2E,
                0x30, 0x2C,
                0x30, 0x14,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x02,
                ( byte ) 0xA1, 0x0D,
                0x04, 0x0B,
                '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '1',
                0x30, 0x14,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x02,
                ( byte ) 0xA1, 0x0D,
                0x04, 0x0B,
                '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '2',
                ( byte ) 0xAA, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f',
                ( byte ) 0xAB, ( byte ) 0x81, ( byte ) 0x83,
                0x30, ( byte ) 0x81, ( byte ) 0x80,
                0x61, 0x3E,
                0x30, 0x3C,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x01,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                'c', 'l', 'i', 'e', 'n', 't',
                ( byte ) 0xA3, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f',
                0x61, 0x3E,
                0x30, 0x3C,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x01,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                's', 'e', 'r', 'v', 'e', 'r',
                ( byte ) 0xA3, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f'
        } );

        stream.flip();

        // Allocate a KdcReqBody Container
        Asn1Container kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KdcReqBody PDU
        try
        {
            Asn1Decoder.decode( stream, kdcReqBodyContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        KdcReqBody body = new KdcReqBody();

        body.setKdcOptions( new KdcOptions( new byte[]
            { 0x00, 0x01, 0x04, 0x00, 0x32 } ) );
        body.setCName( new PrincipalName( "client", PrincipalNameType.KRB_NT_ENTERPRISE ) );
        body.setRealm( "EXAMPLE.COM" );
        body.setSName( new PrincipalName( "server", PrincipalNameType.KRB_NT_ENTERPRISE ) );

        body.setFrom( new KerberosTime( System.currentTimeMillis() ) );
        body.setTill( new KerberosTime( System.currentTimeMillis() ) );
        body.setRtime( new KerberosTime( System.currentTimeMillis() ) );
        body.setNonce( 12345 );

        body.addEType( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
        body.addEType( EncryptionType.DES3_CBC_MD5 );
        body.addEType( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        HostAddresses addresses = new HostAddresses();
        addresses.addHostAddress(
            new HostAddress( HostAddrType.ADDRTYPE_INET, Strings.getBytesUtf8( "192.168.0.1" ) ) );
        addresses.addHostAddress(
            new HostAddress( HostAddrType.ADDRTYPE_INET, Strings.getBytesUtf8( "192.168.0.2" ) ) );
        body.setAddresses( addresses );

        EncryptedData encAuthorizationData = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96,
            Strings.getBytesUtf8( "abcdef" ) );
        body.setEncAuthorizationData( encAuthorizationData );

        Ticket ticket1 = new Ticket();
        ticket1.setTktVno( 5 );
        ticket1.setRealm( "EXAMPLE.COM" );
        ticket1.setSName( new PrincipalName( "client", PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket1.setEncPart(
            new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, Strings.getBytesUtf8( "abcdef" ) ) );

        body.addAdditionalTicket( ticket1 );

        Ticket ticket2 = new Ticket();
        ticket2.setTktVno( 5 );
        ticket2.setRealm( "EXAMPLE.COM" );
        ticket2.setSName( new PrincipalName( "server", PrincipalNameType.KRB_NT_PRINCIPAL ) );
        ticket2.setEncPart(
            new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, Strings.getBytesUtf8( "abcdef" ) ) );

        body.addAdditionalTicket( ticket2 );

        // Check the encoding
        int length = body.computeLength();

        // Check the length
        assertEquals( 0x15B, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = body.encode( encodedPdu );

            // Check the length
            assertEquals( 0x15B, encodedPdu.limit() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a KDC-REQ-BODY with nothing in it
     */
    @Test
    public void testKdcReqBodyEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a KDC-REQ-BODY Container
        Asn1Container kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KDC-REQ-BODY PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, kdcReqBodyContainer);
        } );
    }


    /**
     * Test the decoding of a KDC-REQ-BODY with empty options tag
     */
    @Test
    public void testKdcReqBodyEmptyOptionTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        // Allocate a KDC-REQ-BODY Container
        Asn1Container kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KDC-REQ-BODY PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, kdcReqBodyContainer);
        } );
    }


    /**
     * Test the decoding of a KDC-REQ-BODY with empty options value
     */
    @Test
    public void testKdcReqBodyEmptyOptionValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        // Allocate a KDC-REQ-BODY Container
        Asn1Container kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KDC-REQ-BODY PDU
        Assertions.assertThrows( DecoderException.class, () -> {
                    Asn1Decoder.decode(stream, kdcReqBodyContainer);
                } );
    }


    /**
     * Test the decoding of a KDC-REQ-BODY with no options
     */
    @Test
    public void testKdcReqBodyNoOptions() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x152 );

        stream.put( new byte[]
            {
                0x30, ( byte ) 0x82, 0x01, 0x4E,
                ( byte ) 0xA1, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                'c', 'l', 'i', 'e', 'n', 't',
                ( byte ) 0xA2, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA3, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x0A,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                's', 'e', 'r', 'v', 'e', 'r',
                ( byte ) 0xA4, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA5, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA6, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA7, 0x04,
                0x02, 0x02,
                0x30, 0x39,
                ( byte ) 0xA8, 0x0B,
                0x30, 0x09,
                0x02, 0x01, 0x06,
                0x02, 0x01, 0x11,
                0x02, 0x01, 0x12,
                ( byte ) 0xA9, 0x2E,
                0x30, 0x2C,
                0x30, 0x14,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x02,
                ( byte ) 0xA1, 0x0D,
                0x04, 0x0B,
                '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '1',
                0x30, 0x14,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x02,
                ( byte ) 0xA1, 0x0D,
                0x04, 0x0B,
                '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '2',
                ( byte ) 0xAA, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f',
                ( byte ) 0xAB, ( byte ) 0x81, ( byte ) 0x83,
                0x30, ( byte ) 0x81, ( byte ) 0x80,
                0x61, 0x3E,
                0x30, 0x3C,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x01,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                'c', 'l', 'i', 'e', 'n', 't',
                ( byte ) 0xA3, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f',
                0x61, 0x3E,
                0x30, 0x3C,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x05,
                ( byte ) 0xA1, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA2, 0x13,
                0x30, 0x11,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x01,
                ( byte ) 0xA1, 0x0A,
                0x30, 0x08,
                0x1B, 0x06,
                's', 'e', 'r', 'v', 'e', 'r',
                ( byte ) 0xA3, 0x11,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x11,
                ( byte ) 0xA2, 0x08,
                0x04, 0x06,
                'a', 'b', 'c', 'd', 'e', 'f'
        } );

        stream.flip();

        // Allocate a KDC-REQ-BODY Container
        Asn1Container kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KDC-REQ-BODY PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, kdcReqBodyContainer);
        } );
    }


    /**
     * Test the decoding of a KdcReqBody message with no optional value
     * ( we only have options, realm, till, nonce and etype )
     */
    @Test
    public void testDecodeKdcReqBodyNoOptionalValue() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x40 );

        stream.put( new byte[]
            {
                0x30, ( byte ) 0x3E,
                ( byte ) 0xA0, 0x07,
                0x03, 0x05,
                0x00, 0x01, 0x04, 0x00, 0x32,
                ( byte ) 0xA2, 0x0D,
                0x1B, 0x0B,
                'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M',
                ( byte ) 0xA5, 0x11,
                0x18, 0x0F,
                '2', '0', '1', '0', '1', '1', '1', '0', '1', '5', '4', '5', '2', '5', 'Z',
                ( byte ) 0xA7, 0x04,
                0x02, 0x02,
                0x30, 0x39,
                ( byte ) 0xA8, 0x0B,
                0x30, 0x09,
                0x02, 0x01, 0x06,
                0x02, 0x01, 0x11,
                0x02, 0x01, 0x12
        } );

        Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a KdcReqBody Container
        KdcReqBodyContainer kdcReqBodyContainer = new KdcReqBodyContainer( stream );

        // Decode the KdcReqBody PDU
        try
        {
            Asn1Decoder.decode( stream, kdcReqBodyContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        KdcReqBody body = kdcReqBodyContainer.getKdcReqBody();

        assertNotNull( body );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( body.computeLength() );

        try
        {
            bb = body.encode( bb );

            // Check the length
            assertEquals( 0x40, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            //assertEquals( decodedPdu, encodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
}
