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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.krbSafeBody.KrbSafeBodyContainer;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.KrbSafeBody;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;


/**
 * Test cases for KrbSafeBody codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbSafeBodyDecoderTest
{
    @Test
    public void testDecodeKrbSafeBody() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x47;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x45,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                ( byte ) 0xA1,
                0x11, // timestamp
                0x18,
                0xF,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '9',
                '0',
                '8',
                '0',
                '0',
                '4',
                '3',
                'Z',
                ( byte ) 0xA2,
                0x03, // usec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA3,
                0x03, // seq-number
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0xF, // s-address
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
                ( byte ) 0xA5,
                0xF, // r-adress
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
                1
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        String time = "20101119080043Z";
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertEquals( time, body.getTimestamp().getDate() );
        assertEquals( 1, body.getUsec() );
        assertEquals( 1, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertEquals( ad, body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeBodyWithoutTimestamp() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x34;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x32,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                // NO timestamp
                ( byte ) 0xA2,
                0x03, // usec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA3,
                0x03, // seq-number
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0xF, // s-address
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
                ( byte ) 0xA5,
                0xF, // r-adress
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
                1
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertNull( body.getTimestamp() );
        assertEquals( 1, body.getUsec() );
        assertEquals( 1, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertEquals( ad, body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeBodyWithoutTimestampAndUsec() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x2F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x2D,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                // NO timestamp and usec
                ( byte ) 0xA3,
                0x03, // seq-number
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0xF, // s-address
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
                ( byte ) 0xA5,
                0xF, // r-adress
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
                1
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertNull( body.getTimestamp() );
        assertEquals( 0, body.getUsec() );
        assertEquals( 1, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertEquals( ad, body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeBodyWithoutTimestampUsecAndSeqNumber() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x2A;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x28,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                // NO timestamp, usec and seq-number
                ( byte ) 0xA4,
                0xF, // s-address
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
                ( byte ) 0xA5,
                0xF, // r-adress
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
                1
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertNull( body.getTimestamp() );
        assertEquals( 0, body.getUsec() );
        assertEquals( 0, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertEquals( ad, body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeBodyWithoutSequenceNumber() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x42;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x40,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                ( byte ) 0xA1,
                0x11, // timestamp
                0x18,
                0xF,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '9',
                '0',
                '8',
                '0',
                '0',
                '4',
                '3',
                'Z',
                ( byte ) 0xA2,
                0x03, // usec
                0x02,
                0x01,
                0x01,
                // NO seq-number
                ( byte ) 0xA4,
                0xF, // s-address
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
                ( byte ) 0xA5,
                0xF, // r-adress
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
                1
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        String time = "20101119080043Z";
        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertEquals( time, body.getTimestamp().getDate() );
        assertEquals( 1, body.getUsec() );
        assertEquals( 0, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertEquals( ad, body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbSafeBodyWithoutOptionalValues() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x19;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x30, 0x17,
                ( byte ) 0xA0, 0x4, // user-data
                0x04,
                0x02,
                0x00,
                0x01,
                // NO timestamp, usec and seq-number
                ( byte ) 0xA4,
                0xF, // s-address
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
            // NO r-address
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbSafeBodyContainer container = new KrbSafeBodyContainer();
        container.setStream( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            e.printStackTrace();
            fail();
        }

        KrbSafeBody body = container.getKrbSafeBody();

        HostAddress ad = new HostAddress( InetAddress.getByName( "127.0.0.1" ) );

        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, body.getUserData() ) );
        assertNull( body.getTimestamp() );
        assertEquals( 0, body.getUsec() );
        assertEquals( 0, body.getSeqNumber() );
        assertEquals( ad, body.getSenderAddress() );
        assertNull( body.getRecipientAddress() );

        int computedLen = body.computeLength();

        assertEquals( streamLen, computedLen );

        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );

            body.encode( bb );

            String encoded = Strings.dumpBytes( bb.array() );
            assertEquals( decoded, encoded );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }

}
