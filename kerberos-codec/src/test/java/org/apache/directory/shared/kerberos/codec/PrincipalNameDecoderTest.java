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
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test the PrincipalName decoder
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalNameDecoderTest
{
    /**
     * Test the decoding of a PrincipalName
     */
    @Test
    public void testPrincipalName()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x29 );

        stream.put( new byte[]
            { 0x30, 0x27,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x20, // name-string
                0x30,
                0x1E,
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '1',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '2',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '3',
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        try
        {
            Asn1Decoder.decode( stream, principalNameContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded PrincipalName
        PrincipalName principalName = ( ( PrincipalNameContainer ) principalNameContainer ).getPrincipalName();

        assertEquals( PrincipalNameType.KRB_NT_PRINCIPAL, principalName.getNameType() );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( principalName.computeLength() );

        try
        {
            bb = principalName.encode( bb );

            // Check the length
            assertEquals( 0x29, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a PrincipalName with nothing in it
     */
    @Test
    public void testPrincipalNameEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with no type
     */
    @Test
    public void testPrincipalNameNoType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            { 0x30, 0x02,
                ( byte ) 0xA0, 0x00 // name-type
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with an empty type
     */
    @Test
    public void testPrincipalNameEmptyType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            { 0x30, 0x04,
                ( byte ) 0xA0, 0x02, // name-type
                0x02,
                0x00 // NT-PRINCIPAL
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with a wrong type
     */
    @Test
    public void testPrincipalNameBadType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            { 0x30, 0x09,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x7F, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x02, // name-string
                0x30,
                0x00
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with an empty name
     */
    @Test
    public void testPrincipalNameEmptyName() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x07,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x00 // name-string
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with no name
     */
    @Test
    public void testPrincipalNameNoName() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            { 0x30, 0x09,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x02, // name-string
                0x30,
                0x00
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName
     */
    @Test
    public void testPrincipalNameBadName() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0D );

        stream.put( new byte[]
            { 0x30, 0x0B,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x04, // name-string
                0x30,
                0x02,
                0x1B,
                0x00
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName
     */
    @Test
    public void testPrincipalNameBadName2() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x29 );

        stream.put( new byte[]
            { 0x30, 0x27,
                ( byte ) 0xA0, 0x03, // name-type
                0x02,
                0x01,
                0x01, // NT-PRINCIPAL
                ( byte ) 0xA1,
                0x20, // name-string
                0x30,
                0x1E,
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '1',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                '\r',
                's',
                'o',
                'n',
                '2',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '3',
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }


    /**
     * Test the decoding of a PrincipalName with no name-type
     */
    @Test
    public void testPrincipalNameNoNameType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x24 );

        stream.put( new byte[]
            { 0x30, 0x22,
                ( byte ) 0xA1, 0x20, // name-string
                0x30,
                0x1E,
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '1',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                '\r',
                's',
                'o',
                'n',
                '2',
                0x1B,
                0x08,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                '3',
        } );

        stream.flip();

        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, principalNameContainer);
        } );
    }
}
