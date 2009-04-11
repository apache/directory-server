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

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.codec.binary.Hex;
import org.apache.directory.shared.asn1.codec.stateful.EncoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.StatefulEncoder;
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
public class HexEncoderTest implements EncoderCallback
{
    HexEncoder encoder = null;

    byte[] encoded = null;

    byte[] data = null;


    @Before
    public void setUp() throws Exception
    {
        encoder = new HexEncoder();
        encoder.setCallback( this );
    }


    @After
    public void tearDown() throws Exception
    {
        data = null;
        encoder = null;
        encoded = null;
    }


    private void generateData( int amount )
    {
        Random rand = new Random( System.currentTimeMillis() );
        data = new byte[amount];
        rand.nextBytes( data );
    }


    public void encodeOccurred( StatefulEncoder encoder, Object encodedObj )
    {
        ByteBuffer encodedBuf = ( ByteBuffer ) encodedObj;

        if ( encoded == null )
        {
            encoded = new byte[encodedBuf.remaining()];
            encodedBuf.get( encoded );
        }
        else
        {
            byte[] temp = encoded;
            encoded = new byte[encodedBuf.remaining() + temp.length];
            System.arraycopy( temp, 0, encoded, 0, temp.length );
            encodedBuf.get( encoded, temp.length, encodedBuf.remaining() );
        }
    }


    @Test
    public void testEncode0() throws EncoderException
    {
        generateData( 0 );
        encoder.encode( ByteBuffer.wrap( data ) );
        if ( encoded == null )
        {
            encoded = new byte[0];
        }
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode1() throws EncoderException
    {
        generateData( 1 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode25() throws EncoderException
    {
        generateData( 25 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode63() throws EncoderException
    {
        generateData( 63 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode64() throws EncoderException
    {
        generateData( 64 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode65() throws EncoderException
    {
        generateData( 65 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode66() throws EncoderException
    {
        generateData( 66 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    @Test
    public void testEncode512() throws EncoderException
    {
        generateData( 512 );
        encoder.encode( ByteBuffer.wrap( data ) );
        assertEncoded( encoded, Hex.encodeHex( data ) );
    }


    private void assertEncoded( byte[] encoded, char[] hex )
    {
        if ( encoded.length != hex.length )
        {
            fail( "encoded length of " + encoded.length + " did not match expected hex char length of " + hex.length );
        }

        for ( int ii = 0; ii < encoded.length; ii++ )
        {
            if ( encoded[ii] != hex[ii] )
            {
                fail( "encoding failed - encoded array does not match" );
            }
        }
    }
}
