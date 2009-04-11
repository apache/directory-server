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
package org.apache.directory.shared.asn1.primitives;


import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;


/**
 * Test the Bit String primitive
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BitStringTest
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test a null BitString
     */
    @Test
    public void testBitStringNull()
    {

        BitString bitString = new BitString( 1 );

        bitString.setData( null );

        try
        {
            bitString.getBit( 0 );
            fail( "Should not reach this point ..." );
        }
        catch ( IndexOutOfBoundsException ioobe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test an empty BitString
     */
    @Test
    public void testBitStringEmpty()
    {

        BitString bitString = new BitString( 1 );

        bitString.setData( new byte[]
            {} );

        try
        {
            bitString.getBit( 0 );
            fail( "Should not reach this point ..." );
        }
        catch ( IndexOutOfBoundsException ioobe )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a single bit BitString
     */
    @Test
    public void testSingleBitBitString() throws DecoderException
    {

        BitString bitString = new BitString( 1 );

        bitString.setData( new byte[]
            { 0x07, ( byte ) 0x80 } );

        assertEquals( true, bitString.getBit( 0 ) );
    }


    /**
     * Test a 32 bits BitString
     */
    @Test
    public void test32BitsBitString() throws DecoderException
    {

        BitString bitString = new BitString( 32 );

        bitString.setData( new byte[]
            { 0x00, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF } );

        for ( int i = 0; i < 32; i++ )
        {
            assertEquals( true, bitString.getBit( i ) );
        }
    }


    /**
     * Test a 33 bits BitString
     */
    @Test
    public void test33BitsBitString() throws DecoderException
    {

        BitString bitString = new BitString( 33 );

        bitString.setData( new byte[]
            { 0x07, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0x80 } );

        for ( int i = 0; i < 33; i++ )
        {
            assertEquals( true, bitString.getBit( i ) );
        }

        assertEquals( true, bitString.getBit( 32 ) );
    }


    /**
     * Test all bits from 0 to 128 BitString
     */
    @Test
    public void test0to128BitString() throws DecoderException
    {

        // bit number 14
        BitString bitString14 = new BitString( 14 );

        bitString14.setData( new byte[]
            { 0x02, ( byte ) 0xFF, ( byte ) 0xFC } );

        for ( int i = 0; i < 14; i++ )
        {
            assertEquals( true, bitString14.getBit( i ) );
        }

        // bit number 31
        BitString bitString31 = new BitString( 31 );

        bitString31.setData( new byte[]
            { 0x01, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFE } );

        for ( int i = 0; i < 31; i++ )
        {
            assertEquals( true, bitString31.getBit( i ) );
        }

        // bit number 128
        BitString bitString128 = new BitString( 128 );

        bitString128.setData( new byte[]
            { 0x00, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
                ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
                ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF } );

        for ( int i = 0; i < 128; i++ )
        {
            assertEquals( true, bitString128.getBit( i ) );
        }
    }
    
    @Test
    public void testBitStringSet()
    {
        BitString bitString = new BitString( 32 );

        byte[] bytes = new byte[]
            { (byte)0xAA, 0x11, (byte)0x88, (byte)0xFE };
        
        int[] bits = new int[]
            {
                1, 0, 1, 0 ,   1, 0, 1, 0,
                0, 0, 0, 1,    0, 0, 0, 1,
                1, 0, 0, 0,    1, 0, 0, 0,
                1, 1, 1, 1,    1, 1, 1, 0
            };

        for ( int i = 0; i < bits.length; i++ )
        {
            if ( bits[i] == 1 )
            {
                bitString.setBit( bits.length - i - 1 );
            }
        }
        
        assertEquals( Asn1StringUtils.dumpBytes( bytes ), Asn1StringUtils.dumpBytes( bitString.getData() ) );
    }

    @Test
    public void testBitStringSetBit()
    {
        BitString bitString = new BitString( 32 );

        int[] bits = new int[]
            {
                1, 0, 1, 0 ,   1, 0, 1, 0,
                0, 0, 0, 1,    0, 0, 0, 1,
                1, 0, 0, 0,    1, 0, 0, 0,  // After modification, will become 8A
                1, 1, 1, 1,    1, 1, 1, 0
            };

        for ( int i = 0; i < bits.length; i++ )
        {
            if ( bits[i] == 1 )
            {
                bitString.setBit( bits.length - i - 1 );
            }
        }
        
        bitString.setBit( 9 );
        byte[] bytesModified = new byte[]
            { (byte)0xAA, 0x11, (byte)0x8A, (byte)0xFE };
                            
        assertEquals( Asn1StringUtils.dumpBytes( bytesModified ), Asn1StringUtils.dumpBytes( bitString.getData() ) );
    }

    @Test
    public void testBitStringClearBit()
    {
        BitString bitString = new BitString( 32 );

        int[] bits = new int[]
            {
                1, 0, 1, 0 ,   1, 0, 1, 0,
                0, 0, 0, 1,    0, 0, 0, 1,
                1, 0, 0, 0,    1, 0, 0, 0,
                1, 1, 1, 1,    1, 1, 1, 0
            };

        for ( int i = 0; i < bits.length; i++ )
        {
            if ( bits[i] == 1 )
            {
                bitString.setBit( bits.length - i - 1 );
            }
        }
        
        bitString.clearBit( 11 );
        byte[] bytesModified = new byte[]
            { (byte)0xAA, 0x11, (byte)0x80, (byte)0xFE };
                            
        assertEquals( Asn1StringUtils.dumpBytes( bytesModified ), Asn1StringUtils.dumpBytes( bitString.getData() ) );
    }
}
