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
package org.apache.directory.shared.asn1.codec.stateful.examples;


import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.binary.Hex;
import org.apache.directory.shared.asn1.codec.stateful.DecoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;


/**
 * Document me.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class HexDecoderTest implements DecoderCallback
{
    private HexDecoder decoder = null;

    byte[] encoded = null;

    byte[] decoded = null;

    byte[] original = null;


    @Before
    public void setUp() throws Exception
    {
        decoder = new HexDecoder();
        decoder.setCallback( this );
    }


    @After
    public void tearDown() throws Exception
    {
        decoder = null;
        encoded = null;
        decoded = null;
        original = null;
    }


    private void generateData( int amount )
    {
        Random rand = new Random( System.currentTimeMillis() );
        original = new byte[amount / 2];
        rand.nextBytes( original );
        char[] chars = Hex.encodeHex( original );
        encoded = new byte[amount];
        for ( int ii = 0; ii < amount; ii++ )
        {
            encoded[ii] = ( byte ) chars[ii];
        }
    }


    public void decodeOccurred( StatefulDecoder decoder, Object obj )
    {
        ByteBuffer decodedBuf = ( ByteBuffer ) obj;

        if ( decoded == null )
        {
            decoded = new byte[decodedBuf.remaining()];
            decodedBuf.get( decoded );
        }
        else
        {
            byte[] temp = decoded;
            decoded = new byte[decodedBuf.remaining() + temp.length];
            System.arraycopy( temp, 0, decoded, 0, temp.length );
            decodedBuf.get( decoded, temp.length, decodedBuf.remaining() );
        }
    }


    @Test
    public void testDecode0() throws DecoderException
    {
        generateData( 0 );
        decoder.decode( ByteBuffer.wrap( encoded ) );

        if ( decoded == null )
        {
            decoded = new byte[0];
        }

        assertDecoded();
    }


    @Test
    public void testDecode2() throws DecoderException
    {
        generateData( 2 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testDecode26() throws DecoderException
    {
        generateData( 26 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testDecode254() throws DecoderException
    {
        generateData( 254 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testDecode256() throws DecoderException
    {
        generateData( 256 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testDecode258() throws DecoderException
    {
        generateData( 258 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testDecode2048() throws DecoderException
    {
        generateData( 2048 );
        decoder.decode( ByteBuffer.wrap( encoded ) );
        assertDecoded();
    }


    @Test
    public void testPartialDecode2() throws DecoderException
    {
        generateData( 2 );

        decoder.decode( ByteBuffer.wrap( encoded, 0, 1 ) );

        try
        {
            assertDecoded();
            fail( "should not get here" );
        }
        catch ( NullPointerException e )
        {
        }

        decoder.decode( ByteBuffer.wrap( encoded, 1, 1 ) );

        assertDecoded();
    }


    @Test
    public void testPartialDecode30() throws DecoderException
    {
        generateData( 30 );

        for ( int ii = 0; ii < 30; ii += 3 )
        {
            decoder.decode( ByteBuffer.wrap( encoded, ii, 3 ) );
        }

        assertDecoded();
    }


    @Test
    public void testPartialDecode300() throws DecoderException
    {
        generateData( 300 );

        for ( int ii = 0; ii < 300; ii += 5 )
        {
            decoder.decode( ByteBuffer.wrap( encoded, ii, 5 ) );
        }

        assertDecoded();
    }


    private void assertDecoded()
    {
        if ( decoded.length != original.length )
        {
            fail( "decoded length of " + decoded.length + " did not match expected original data length of "
                + original.length );
        }

        for ( int ii = 0; ii < decoded.length; ii++ )
        {
            if ( decoded[ii] != original[ii] )
            {
                fail( "decode failed - decoded array does not match" );
            }
        }
    }
}
