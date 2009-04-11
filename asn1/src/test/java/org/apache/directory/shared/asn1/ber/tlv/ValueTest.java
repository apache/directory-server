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
import java.util.Arrays;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.BitString;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.LongDecoder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;



/**
 * This class is used to test the Value class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ValueTest
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


    /**
     * Test the generation of an Integer Value
     *
     */
    @Test
    public void testGetBytesInt()
    {
        int[] positiveValues = new int[]
             {
                 0x00, 0x01, 0x7F,
                 0x0080, 0x0081, 0x7FFF,
                 0x008000, 0x008001, 0x7FFFFF,
                 0x00800000, 0x00800001, 0x7FFFFFFF
             };

         byte[][] expectedPositiveBytes = new byte[][]
             {
                 // 1 byte
                 { 0x00 }, 
                 { 0x01 }, 
                 { 0x7F },
                 
                 // 2 bytes
                 { 0x00, (byte)0x80 }, 
                 { 0x00, (byte)0x81 }, 
                 { 0x7F, (byte)0xFF },

                 // 3 bytes
                 { 0x00, (byte)0x80, 0x00 },
                 { 0x00, (byte)0x80, 0x01 },
                 { 0x7F, (byte)0xFF, (byte)0xFF },

                 // 4 bytes
                 { 0x00, (byte)0x80, 0x00, 0x00 },
                 { 0x00, (byte)0x80, 0x00, 0x01 },
                 { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF },
             };

         int[] negativeValues = new int[]
             {
                 // 1 byte
                 -1, -127, -128,  
                             
                 // 2 bytes
                 -129, -255, -256, -257, -32767, -32768,

                 // 3 bytes
                 -32769, -65535, -65536, -65537, -8388607, -8388608,

                 // 4 bytes
                 -8388609, -16777215, -16777216, -16777217, -2147483647, -2147483648,
             };

         byte[][] expectedNegativeBytes = new byte[][]
             {
                 // 1 byte
                 { (byte)0xFF },
                 { (byte)0x81 }, 
                 { (byte)0x80 }, 
     
                 // 2 bytes
                 { (byte)0xFF, 0x7F }, 
                 { (byte)0xFF, 0x01 }, 
                 { (byte)0xFF, 0x00 }, 
                 { (byte)0xFE, (byte)0xFF }, 
                 { (byte)0x80, 0x01 }, 
                 { (byte)0x80, 0x00 }, 
     
                 // 3 bytes
                 { (byte)0xFF, 0x7F, (byte)0xFF }, 
                 { (byte)0xFF, 0x00, 0x01 },
                 { (byte)0xFF, 0x00, 0x00 },
                 { (byte)0xFE, (byte)0xFF, (byte)0xFF },
                 { (byte)0x80, 0x00, 0x01 },
                 { (byte)0x80, 0x00, 0x00 },
     
                 // 4 bytes
                 { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF },
                 { (byte)0xFF, 0x00, 0x00, 0x01 },
                 { (byte)0xFF, 0x00, 0x00, 0x00 },
                 { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                 { (byte)0x80, 0x00, 0x00, 0x01 },
                 { (byte)0x80, 0x00, 0x00, 0x00 },
             };

         int i = 0;
         
         for ( int value:positiveValues )
         {
             byte[] bb = Value.getBytes( value );
             assertEquals( expectedPositiveBytes[i].length, bb.length );
             assertTrue( Arrays.equals( expectedPositiveBytes[i], bb ) );
             i++;
         }
         
         i=0;
         
         for ( int value:negativeValues )
         {
             byte[] bb = Value.getBytes( value );
             assertEquals( expectedNegativeBytes[i].length, bb.length );
             assertTrue( Arrays.equals( expectedNegativeBytes[i], bb ) );
             i++;
         }

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


    /**
     * Test the generation of a Long Value
     *
     */
    @Test
    public void testGetBytesLong()
    {
        long[] positiveValues = new long[]
            {
                0x00L, 0x01L, 0x7FL,
                0x0080L, 0x0081L, 0x7FFFL,
                0x008000L, 0x008001L, 0x7FFFFFL,
                0x00800000L, 0x00800001L, 0x7FFFFFFFL,
                0x0080000000L, 0x0080000001L, 0x7FFFFFFFFFL,
                0x008000000000L, 0x008000000001L, 0x7FFFFFFFFFFFL,
                0x00800000000000L, 0x00800000000001L, 0x7FFFFFFFFFFFFFL,
                0x0080000000000000L, 0x0080000000000001L, 0x7FFFFFFFFFFFFFFFL
            };
        
        byte[][] expectedPositiveBytes = new byte[][]
            {
                // 1 byte
                { 0x00 }, 
                { 0x01 }, 
                { 0x7F },
                
                // 2 bytes
                { 0x00, (byte)0x80 }, 
                { 0x00, (byte)0x81 }, 
                { 0x7F, (byte)0xFF },

                // 3 bytes
                { 0x00, (byte)0x80, 0x00 },
                { 0x00, (byte)0x80, 0x01 },
                { 0x7F, (byte)0xFF, (byte)0xFF },

                // 4 bytes
                { 0x00, (byte)0x80, 0x00, 0x00 },
                { 0x00, (byte)0x80, 0x00, 0x01 },
                { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF },

                // 5 bytes
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00 },
                { 0x00, (byte)0x80, 0x00, 0x00, 0x01 },
                { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },

                // 6 bytes
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00 },
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x01 },
                { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },

                // 7 bytes
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00 },
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },

                // 8 bytes
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }, 
                { 0x00, (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 }, 
                { 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF }
            };

        long[] negativeValues = new long[]
            {
                // 1 byte
                -1L, -127L, -128L,  
                            
                // 2 bytes
                -129L, -255L, -256L, -257L, -32767L, -32768L,

                // 3 bytes
                -32769L, -65535L, -65536L, -65537L, -8388607L, -8388608L,

                // 4 bytes
                -8388609L, -16777215L, -16777216L, -16777217L, -2147483647L, -2147483648L,

                // 5 bytes
                -2147483649L, -4294967295L, -4294967296L, -4294967297L, -549755813887L, -549755813888L,

                // 6 bytes
                -549755813889L, -1099511627775L, -1099511627776L, 
                -1099511627777L, -140737488355327L, -140737488355328L,

                // 7 bytes
                -140737488355329L, -281474976710655L, -281474976710656L,
                -281474976710657L, -36028797018963967L, -36028797018963968L,
                
                // 8 bytes
                -36028797018963969L, -72057594037927935L, -72057594037927936L,
                -72057594037927937L, -9223372036854775807L, -9223372036854775808L
            };
        
        byte[][] expectedNegativeBytes = new byte[][]
            {
                // 1 byte
                { (byte)0xFF },
                { (byte)0x81 }, 
                { (byte)0x80 }, 
    
                // 2 bytes
                { (byte)0xFF, 0x7F }, 
                { (byte)0xFF, 0x01 }, 
                { (byte)0xFF, 0x00 }, 
                { (byte)0xFE, (byte)0xFF }, 
                { (byte)0x80, 0x01 }, 
                { (byte)0x80, 0x00 }, 
    
                // 3 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF }, 
                { (byte)0xFF, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00 },
    
                // 4 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF },
                { (byte)0xFF, 0x00, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00, 0x00 },
    
                // 5 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00 },
    
                // 6 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00 },
    
                // 7 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
    
                // 8 bytes
                { (byte)0xFF, 0x7F, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
                { (byte)0xFE, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 },
                { (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }
            };
                                                    
        int i = 0;
        
        for ( long value:positiveValues )
        {
            byte[] bb = Value.getBytes( value );
            assertEquals( expectedPositiveBytes[i].length, bb.length );
            assertTrue( Arrays.equals( expectedPositiveBytes[i], bb ) );
            i++;
        }
        
        i=0;
        
        for ( long value:negativeValues )
        {
            byte[] bb = Value.getBytes( value );
            assertEquals( expectedNegativeBytes[i].length, bb.length );
            assertTrue( Arrays.equals( expectedNegativeBytes[i], bb ) );
            i++;
        }
    }


    @Test
    public void testEncodeInt2Bytes()
    {
        byte[] encoded = Value.getBytes( 128 );

        assertEquals( 0x00, encoded[0] );
        assertEquals( ( byte ) 0x80, encoded[1] );

        encoded = Value.getBytes( -27066 );

        assertEquals( ( byte ) 0x96, encoded[0] );
        assertEquals( 0x46, encoded[1] );

    }


    @Test
    public void testEncodeInt3Bytes()
    {

        byte[] encoded = Value.getBytes( 32787 );

        assertEquals( 0x00, encoded[0] );
        assertEquals( ( byte ) 0x80, encoded[1] );
        assertEquals( ( byte ) 0x13, encoded[2] );
    }


    @Test
    public void testEncodeInt()
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { 
                Integer.MIN_VALUE, 
                -2147483647, 
                -16777216, 
                -16777215, 
                -8388608, 
                -8388607, 
                -65536, 
                -65535, 
                -32768, 
                -32767,
                -256, 
                -255, 
                -128, 
                -127, 
                -1, 
                0, 
                1, 
                127, 
                128, 
                255, 
                256, 
                32767, 
                32768, 
                65535, 
                65536, 
                8388607, 
                8388608,
                16777215, 
                16777216, 
                Integer.MAX_VALUE };

        for ( int i:testedInt )
        {
            encoded = Value.getBytes( i );

            int value = new BigInteger( encoded ).intValue();

            assertEquals( i, value );
        }
    }


    /**
     * Test the decoding of integer values
     */
    @Test
    public void testDecodeInt() throws Exception
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { 
                Integer.MIN_VALUE, 
                -2147483647, 
                -16777216, 
                -16777215, 
                -8388608, 
                -8388607, 
                -65536, 
                -65535, 
                -32768, 
                -32767,
                -256, 
                -255, 
                -128, 
                -127, 
                -1, 0, 
                1, 
                127, 
                128, 
                255, 
                256, 
                32767, 
                32768, 
                65535, 
                65536, 
                8388607, 
                8388608,
                16777215, 
                16777216, 
                Integer.MAX_VALUE };

        for ( int i:testedInt )
        {
            encoded = new BigInteger( Integer.toString( i ) ).toByteArray();

            int value = IntegerDecoder.parse( new Value( encoded ) );

            assertEquals( i, value );
        }
    }
    


    /**
     * Test the decoding of long values
     */
    @Test
    public void testDecodeLong() throws Exception
    {
        byte[] encoded = null;
        long[] testedLong = new long[]
            { 
                Long.MIN_VALUE, 
                -9223372036854775808L,
                -9223372036854775807L,
                -72057594037927937L,
                -72057594037927936L,
                -72057594037927935L,
                -36028797018963969L,
                -36028797018963968L,
                -36028797018963967L,
                -281474976710657L,
                -281474976710656L,
                -281474976710655L,
                -140737488355329L,
                -140737488355328L,
                -140737488355327L,
                -1099511627777L,
                -1099511627776L,
                -1099511627775L,
                -549755813889L,
                -549755813888L,
                -549755813887L,
                -4294967297L,
                -4294967296L,
                -4294967295L,
                -2147483649L,
                -2147483648L,
                -2147483647L, 
                -16777216L, 
                -16777215L, 
                -8388608L, 
                -8388607L, 
                -65536L, 
                -65535L, 
                -32769L,
                -32768L, 
                -32767L,
                -257L,
                -256L, 
                -255L, 
                -129L,
                -128L, 
                -127L, 
                -1L, 
                0L, 
                1L, 
                127L, 
                128L, 
                255L, 
                256L, 
                32767L, 
                32768L, 
                32769L, 
                65535L, 
                65536L, 
                8388607L, 
                8388608L,
                8388609L,
                2147483647L,
                2147483648L,
                2147483649L,
                549755813887L,
                549755813888L,
                549755813889L,
                140737488355327L,
                140737488355328L,
                140737488355329L,
                36028797018963967L,
                36028797018963967L,
                36028797018963967L,
                Long.MAX_VALUE };

        for ( long i:testedLong )
        {
            encoded = new BigInteger( Long.toString( i ) ).toByteArray();

            long value = LongDecoder.parse( new Value( encoded ) );

            assertEquals( i, value );
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

