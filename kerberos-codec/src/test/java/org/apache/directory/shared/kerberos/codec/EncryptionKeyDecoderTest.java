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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.encryptionKey.EncryptionKeyContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for EncryptionKey codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncryptionKeyDecoderTest
{
    @Test
    public void testDecodeFullEncryptionKey()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x0F,
                  ( byte ) 0xA0, 0x03, // keytype
                  0x02, 0x01, 0x02,
                  ( byte ) 0xA1, 0x08, // keyvalue
                    0x04, 0x06,
                      'k', 'e', 'y', 'v', 'a', 'l'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        EncryptionKey encKey = container.getEncryptionKey();

        assertEquals( EncryptionType.getTypeByValue( 2 ), encKey.getKeyType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "keyval" ), encKey.getKeyValue() ) );

        ByteBuffer bb = ByteBuffer.allocate( encKey.computeLength() );

        try
        {
            bb = encKey.encode( bb );

            // Check the length
            assertEquals( 0x11, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    @Test
    public void testDecodeEncryptionKeyWithEmptySeq() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            {
                0x30, 0x00
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeEncryptionKeyEmptyKeyTypeTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                  ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeEncryptionKeyEmptyKeyTypeValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                  ( byte ) 0xA0, 0x02,
                    0x02, 0x00
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeEncryptionKeyWithoutType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0C );

        stream.put( new byte[]
            {
                0x30, 0x0A,
                  ( byte ) 0xA1, 0x08, // keyvalue
                    0x04, 0x06,
                      'k', 'e', 'y', 'v', 'a', 'l'
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeChecksumWithoutEncryptionKeyValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                  ( byte ) 0xA0, 0x03, // keytype
                    0x02, 0x01, 0x02
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeChecksumWitEmptyEncryptionKeyTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x30, 0x07,
                  ( byte ) 0xA0, 0x03, // keytype
                    0x02, 0x01, 0x02,
                    ( byte ) 0xA1, 0x00
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testDecodeChecksumWitEmptyEncryptionKeyValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            {
                0x30, 0x09,
                  ( byte ) 0xA0, 0x03, // keytype
                    0x02, 0x01, 0x02,
                    ( byte ) 0xA1, 0x02,
                      0x04, 0x00
        } );

        stream.flip();

        EncryptionKeyContainer container = new EncryptionKeyContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }
}
