/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.asn1.primitives;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.BitString;


/**
 * Test the Bit String primitive
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BitStringTest extends TestCase
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Test a null BitString
     */
    public void testBitStringNull()
    {

        BitString bitString = new BitString();

        bitString.setData( null );

        try
        {
            bitString.getBit( 0 );
            Assert.fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }
    }


    /**
     * Test an empty BitString
     */
    public void testBitStringEmpty()
    {

        BitString bitString = new BitString();

        bitString.setData( new byte[]
            {} );

        try
        {
            bitString.getBit( 0 );
            Assert.fail( "Should not reach this point ..." );
        }
        catch ( DecoderException de )
        {
            Assert.assertTrue( true );
        }
    }


    /**
     * Test a single bit BitString BitString
     */
    public void testSingleBitBitString() throws DecoderException
    {

        BitString bitString = new BitString( 1 );

        bitString.setData( new byte[]
            { 0x07, ( byte ) 0x80 } );

        Assert.assertEquals( true, bitString.getBit( 0 ) );
    }


    /**
     * Test a 32 bits BitString
     */
    public void test32BitsBitString() throws DecoderException
    {

        BitString bitString = new BitString( 32 );

        bitString.setData( new byte[]
            { 0x00, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF } );

        for ( int i = 0; i < 32; i++ )
        {
            Assert.assertEquals( true, bitString.getBit( i ) );
        }
    }


    /**
     * Test a 33 bits BitString
     */
    public void test33BitsBitString() throws DecoderException
    {

        BitString bitString = new BitString( 33 );

        bitString.setData( new byte[]
            { 0x07, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0x80 } );

        for ( int i = 0; i < 33; i++ )
        {
            Assert.assertEquals( true, bitString.getBit( i ) );
        }

        Assert.assertEquals( true, bitString.getBit( 32 ) );
    }


    /**
     * Test all bits from 0 to 128 BitString
     */
    public void test0to128BitString() throws DecoderException
    {

        // bit number 14
        BitString bitString14 = new BitString( 14 );

        bitString14.setData( new byte[]
            { 0x02, ( byte ) 0xFF, ( byte ) 0xFC } );

        for ( int i = 0; i < 14; i++ )
        {
            Assert.assertEquals( true, bitString14.getBit( i ) );
        }

        // bit number 31
        BitString bitString31 = new BitString( 31 );

        bitString31.setData( new byte[]
            { 0x01, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFE } );

        for ( int i = 0; i < 31; i++ )
        {
            Assert.assertEquals( true, bitString31.getBit( i ) );
        }

        // bit number 128
        BitString bitString128 = new BitString( 128 );

        bitString128.setData( new byte[]
            { 0x00, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
                ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
                ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF } );

        for ( int i = 0; i < 128; i++ )
        {
            Assert.assertEquals( true, bitString128.getBit( i ) );
        }
    }
}
