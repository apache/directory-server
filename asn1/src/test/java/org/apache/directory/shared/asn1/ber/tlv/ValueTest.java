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
package org.apache.directory.shared.asn1.ber.tlv;


import java.math.BigInteger;
import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.BitString;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.junit.Test;



/**
 * This class is used to test the Value class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ValueTest extends TestCase
{

    /**
     * Test the getNbBytes method for an int value
     */
    @Test
    public void testValueIntGetNbBytes()
    {
        assertEquals( 1, Value.getNbBytes( 0x00000000 ) );
        assertEquals( 1, Value.getNbBytes( 0x00000001 ) );
        assertEquals( 2, Value.getNbBytes( 0x000000FF ) );
        assertEquals( 2, Value.getNbBytes( 0x00000100 ) );
        assertEquals( 3, Value.getNbBytes( 0x0000FFFF ) );
        assertEquals( 3, Value.getNbBytes( 0x00010000 ) );
        assertEquals( 4, Value.getNbBytes( 0x00FFFFFF ) );
        assertEquals( 4, Value.getNbBytes( 0x01000000 ) );
        assertEquals( 1, Value.getNbBytes( -1 ) );
        assertEquals( 4, Value.getNbBytes( 0x7FFFFFFF ) );
        assertEquals( 1, Value.getNbBytes( 0xFFFFFFFF ) );
    }


    /**
     * Test the getNbBytes method for a long value
     */
    @Test
    public void testValueLongGetNbBytes()
    {
        assertEquals( 1, Value.getNbBytes( 0x0000000000000000L ) );
        assertEquals( 1, Value.getNbBytes( 0x0000000000000001L ) );
        assertEquals( 2, Value.getNbBytes( 0x00000000000000FFL ) );
        assertEquals( 2, Value.getNbBytes( 0x0000000000000100L ) );
        assertEquals( 3, Value.getNbBytes( 0x000000000000FFFFL ) );
        assertEquals( 3, Value.getNbBytes( 0x0000000000010000L ) );
        assertEquals( 4, Value.getNbBytes( 0x0000000000FFFFFFL ) );
        assertEquals( 4, Value.getNbBytes( 0x0000000001000000L ) );
        assertEquals( 5, Value.getNbBytes( 0x00000000FFFFFFFFL ) );
        assertEquals( 5, Value.getNbBytes( 0x0000000100000000L ) );
        assertEquals( 6, Value.getNbBytes( 0x000000FFFFFFFFFFL ) );
        assertEquals( 6, Value.getNbBytes( 0x0000010000000000L ) );
        assertEquals( 7, Value.getNbBytes( 0x0000FFFFFFFFFFFFL ) );
        assertEquals( 7, Value.getNbBytes( 0x0001000000000000L ) );
        assertEquals( 8, Value.getNbBytes( 0x00FFFFFFFFFFFFFFL ) );
        assertEquals( 8, Value.getNbBytes( 0x0100000000000000L ) );
        assertEquals( 1, Value.getNbBytes( -1L ) );
        assertEquals( 8, Value.getNbBytes( 0x7FFFFFFFFFFFFFFFL ) );
        assertEquals( 1, Value.getNbBytes( 0xFFFFFFFFFFFFFFFFL ) );
    }

    
    @Test
    public void testGetBytes()
    {
        byte[] bb = Value.getBytes( 0x00000000 );
        assertEquals( 1, bb.length );
        assertEquals( 0, bb[0] );

        bb = Value.getBytes( 0x00000001 );
        assertEquals( 1, bb.length );
        assertEquals( 1, bb[0] );

        bb = Value.getBytes( 0x0000007F );
        assertEquals( 1, bb.length );
        assertEquals( 0x7F, bb[0] );

        bb = Value.getBytes( 0x00000080 );
        assertEquals( 2, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0x80, bb[1] );

        bb = Value.getBytes( 0x000000FF );
        assertEquals( 2, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );

        bb = Value.getBytes( 0x00007FFF );
        assertEquals( 2, bb.length );
        assertEquals( 0x7F, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );

        bb = Value.getBytes( 0x00008000 );
        assertEquals( 3, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0x80, bb[1] );
        assertEquals( 0x00, bb[2] );

        bb = Value.getBytes( 0x0000FFFF );
        assertEquals( 3, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );

        bb = Value.getBytes( 0x00010000 );
        assertEquals( 3, bb.length );
        assertEquals( 0x01, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );

        bb = Value.getBytes( 0x007FFFFF );
        assertEquals( 3, bb.length );
        assertEquals( 0x7F, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );

        bb = Value.getBytes( 0x00800000 );
        assertEquals( 4, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0x80, bb[1] );
        assertEquals( 0x00, bb[2] );
        assertEquals( 0x00, bb[3] );

        bb = Value.getBytes( 0x00FFFFFF );
        assertEquals( 4, bb.length );
        assertEquals( 0x00, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );
        assertEquals( (byte)0xFF, bb[3] );

        bb = Value.getBytes( 0x01000000 );
        assertEquals( 4, bb.length );
        assertEquals( 0x01, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );
        assertEquals( 0x00, bb[3] );

        bb = Value.getBytes( 0x7FFFFFFF );
        assertEquals( 4, bb.length );
        assertEquals( 0x7F, bb[0] );
        assertEquals( (byte)0xFF, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );
        assertEquals( (byte)0xFF, bb[3] );

        bb = Value.getBytes( 0x80000000 );
        assertEquals( 4, bb.length );
        assertEquals( (byte)0x80, bb[0] );
        assertEquals( (byte)0x00, bb[1] );
        assertEquals( (byte)0x00, bb[2] );
        assertEquals( (byte)0x00, bb[3] );
        
        bb = Value.getBytes( 0xFFFFFFFF );
        assertEquals( 1, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        
        bb = Value.getBytes( 0xFFFFFF80 );
        assertEquals( 1, bb.length );
        assertEquals( (byte)0x80, bb[0] );

        bb = Value.getBytes( 0xFFFFFF7F );
        assertEquals( 2, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x7F, bb[1] );

        bb = Value.getBytes( 0xFFFFFF00 );
        assertEquals( 2, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x00, bb[1] );

        bb = Value.getBytes( 0xFFFF8000 );
        assertEquals( 2, bb.length );
        assertEquals( (byte)0x80, bb[0] );
        assertEquals( 0x00, bb[1] );

        bb = Value.getBytes( 0xFFFF7FFF );
        assertEquals( 3, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x7F, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );

        bb = Value.getBytes( 0xFFFF0000 );
        assertEquals( 3, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );

        bb = Value.getBytes( 0xFF800000 );
        assertEquals( 3, bb.length );
        assertEquals( (byte)0x80, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );

        bb = Value.getBytes( 0xFF7FFFFF );
        assertEquals( 4, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x7F, bb[1] );
        assertEquals( (byte)0xFF, bb[2] );
        assertEquals( (byte)0xFF, bb[3] );

        bb = Value.getBytes( 0xFF000000 );
        assertEquals( 4, bb.length );
        assertEquals( (byte)0xFF, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );
        assertEquals( 0x00, bb[3] );

        bb = Value.getBytes( 0x80000000 );
        assertEquals( 4, bb.length );
        assertEquals( (byte)0x80, bb[0] );
        assertEquals( 0x00, bb[1] );
        assertEquals( 0x00, bb[2] );
        assertEquals( 0x00, bb[3] );
    }


    @Test
    public void testEncodeInt2Bytes()
    {
        byte[] encoded = Value.getBytes( 128 );

        Assert.assertEquals( 0x00, encoded[0] );
        Assert.assertEquals( ( byte ) 0x80, encoded[1] );

        encoded = Value.getBytes( -27066 );

        Assert.assertEquals( ( byte ) 0x96, encoded[0] );
        Assert.assertEquals( 0x46, encoded[1] );

    }


    @Test
    public void testEncodeInt3Bytes()
    {

        byte[] encoded = Value.getBytes( 32787 );

        Assert.assertEquals( 0x00, encoded[0] );
        Assert.assertEquals( ( byte ) 0x80, encoded[1] );
        Assert.assertEquals( ( byte ) 0x13, encoded[2] );
    }


    @Test
    public void testEncodeInt()
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { Integer.MIN_VALUE, -2147483647, -16777216, -16777215, -8388608, -8388607, -65536, -65535, -32768, -32767,
                -256, -255, -128, -127, -1, 0, 1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 8388607, 8388608,
                16777215, 16777216, Integer.MAX_VALUE };

        for ( int i:testedInt )
        {
            encoded = Value.getBytes( i );

            int value = new BigInteger( encoded ).intValue();

            Assert.assertEquals( i, value );
        }
    }


    @Test
    public void testDecodeInt() throws Exception
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { Integer.MIN_VALUE, -2147483647, -16777216, -16777215, -8388608, -8388607, -65536, -65535, -32768, -32767,
                -256, -255, -128, -127, -1, 0, 1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 8388607, 8388608,
                16777215, 16777216, Integer.MAX_VALUE };

        for ( int i:testedInt )
        {
            encoded = new BigInteger( Integer.toString( i ) ).toByteArray();

            int value = IntegerDecoder.parse( new Value( encoded ) );

            Assert.assertEquals( i, value );
        }
    }
    

    @Test
    public void testNewByteArrayValue()
    {
        byte[] bb = new byte[]{0x01, (byte)0xFF};
        
        Value v = new Value( bb );
        byte[] vv = v.getData();
        
        assertEquals( 0x01, vv[0] );
        assertEquals( (byte)0xFF, vv[1] );
        
        bb[0] = 0x00;
        assertEquals( 0x01, vv[0] );
    }

    
    @Test
    public void testEncodeBitString()
    {
        BitString bs = new BitString( 10 );
        bs.setBit( 9 );
        
        ByteBuffer buffer = ByteBuffer.allocate( 5 );
        
        try
        {
            Value.encode( buffer, bs );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
        
        assertEquals( "0x03 0x03 0x06 0x80 0x00 ", Asn1StringUtils.dumpBytes( buffer.array() )  );
    }
}

