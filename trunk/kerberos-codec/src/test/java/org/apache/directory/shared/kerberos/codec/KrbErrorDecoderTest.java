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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.krbError.KrbErrorContainer;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.junit.Test;


/**
 * Test cases for KrbError codec
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbErrorDecoderTest
{
    @Test
    public void testDecodeKrbError()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x8F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x81, ( byte ) 0x8C,
                0x30, ( byte ) 0x81, ( byte ) 0x89,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        String time = "20101119080043Z";
        assertEquals( 5, krbError.getProtocolVersionNumber() );
        assertEquals( KerberosMessageType.KRB_ERROR, krbError.getMessageType() );
        assertEquals( time, krbError.getCTime().getDate() );
        assertEquals( 1, krbError.getCusec() );
        assertEquals( time, krbError.getSTime().getDate() );
        assertEquals( 2, krbError.getSusec() );
        assertEquals( ErrorType.KDC_ERR_NONE, krbError.getErrorCode() );
        assertEquals( "crealm", krbError.getCRealm() );
        assertEquals( "cname", krbError.getCName().getNameString() );
        assertEquals( "realm", krbError.getRealm() );
        assertEquals( "sname", krbError.getSName().getNameString() );
        assertEquals( "etext", krbError.getEText() );
        assertTrue( Arrays.equals( new byte[]
            { 0, 1 }, krbError.getEData() ) );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCtime()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x7A;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x78,
                0x30, ( byte ) 0x76,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                //
                // NO ctime
                //
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCTime() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCusec()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x8A;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x81, ( byte ) 0x87,
                0x30, ( byte ) 0x81, ( byte ) 0x84,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                // NO cuses
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertEquals( 0, krbError.getCusec() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCtimeAndCusec()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x75;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x73,
                0x30, ( byte ) 0x71,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                //
                // NO ctime, cusec
                //
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCTime() );
        assertEquals( 0, krbError.getCusec() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCrealm()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x84;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x81, ( byte ) 0x81,
                0x30, ( byte ) 0x7F,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                // NO crealm
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCRealm() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCname()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x79;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x77,
                0x30, ( byte ) 0x75,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                // NO cname
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCName() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutCrealmAndCname()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x6F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x6D,
                0x30, ( byte ) 0x6B,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                // NO crealm and cname
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't',
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCRealm() );
        assertNull( krbError.getCName() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutEtext()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x86;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x81, ( byte ) 0x83,
                0x30, ( byte ) 0x81, ( byte ) 0x80,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                // NO etext
                ( byte ) 0xAC,
                0x04, // e-data
                0x04,
                0x02,
                0x00,
                0x01
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getEText() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutEdata()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x89;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x81, ( byte ) 0x86,
                0x30, ( byte ) 0x81, ( byte ) 0x83,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xAB,
                0x07, // e-text
                0x1B,
                0x5,
                'e',
                't',
                'e',
                'x',
                't'
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getEData() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutEtextAndEdata()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x7E;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x7C,
                0x30, ( byte ) 0x7A,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA2,
                0x11, // ctime
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
                ( byte ) 0xA3,
                0x03, // cusec
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA7,
                0x08, // crealm
                0x1B,
                0x06,
                'c',
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xA8,
                0x12, // cname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                'c',
                'n',
                'a',
                'm',
                'e',
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
            // NO etext and edata
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getEText() );
        assertNull( krbError.getEData() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }


    @Test
    public void testDecodeKrbErrorWithoutOptionalFields()
    {
        Asn1Decoder decoder = new Asn1Decoder();

        int streamLen = 0x48;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
            {
                0x7E, ( byte ) 0x46,
                0x30, ( byte ) 0x44,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x1E,
                ( byte ) 0xA4,
                0x11, // stime
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
                ( byte ) 0xA5,
                0x03, // susec
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA6,
                0x03, // error-code
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA9,
                0x07, // realm
                0x1B,
                0x05,
                'r',
                'e',
                'a',
                'l',
                'm',
                ( byte ) 0xAA,
                0x12, // sname
                0x30,
                0x10,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x00,
                ( byte ) 0xA1,
                0x09,
                0x30,
                0x07,
                0x1B,
                0x05,
                's',
                'n',
                'a',
                'm',
                'e',
        } );

        String decoded = Strings.dumpBytes( stream.array() );
        stream.flip();

        KrbErrorContainer container = new KrbErrorContainer( stream );

        try
        {
            decoder.decode( stream, container );
        }
        catch ( DecoderException e )
        {
            fail();
        }

        KrbError krbError = container.getKrbError();

        assertNull( krbError.getCTime() );
        assertEquals( 0, krbError.getCusec() );
        assertNull( krbError.getCRealm() );
        assertNull( krbError.getCName() );
        assertNull( krbError.getEText() );
        assertNull( krbError.getEData() );

        int encodedLen = krbError.computeLength();

        assertEquals( streamLen, encodedLen );

        ByteBuffer buffer = ByteBuffer.allocate( streamLen );
        try
        {
            buffer = krbError.encode( buffer );

            assertEquals( decoded, Strings.dumpBytes( buffer.array() ) );
        }
        catch ( EncoderException e )
        {
            fail();
        }
    }
}
