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
package org.apache.directory.shared.asn1.ber.tlv;


import java.math.BigInteger;

import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.util.IntegerDecoder;

import junit.framework.Assert;
import junit.framework.TestCase;


/**
 * This class is used to test the Value class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ValueTest extends TestCase
{

    /**
     * Test the getNbBytes method
     */
    public void testValueGetNbBytes()
    {
        Assert.assertEquals( 1, Value.getNbBytes( 0 ) );
        Assert.assertEquals( 1, Value.getNbBytes( 1 ) );
        Assert.assertEquals( 2, Value.getNbBytes( 255 ) );
        Assert.assertEquals( 2, Value.getNbBytes( 256 ) );
        Assert.assertEquals( 3, Value.getNbBytes( 65535 ) );
        Assert.assertEquals( 3, Value.getNbBytes( 65536 ) );
        Assert.assertEquals( 4, Value.getNbBytes( 16777215 ) );
        Assert.assertEquals( 4, Value.getNbBytes( 16777216 ) );
        Assert.assertEquals( 1, Value.getNbBytes( -1 ) );
    }


    public void testEncodeInt2Bytes()
    {
        byte[] encoded = Value.getBytes( 128 );

        Assert.assertEquals( 0x00, encoded[0] );
        Assert.assertEquals( ( byte ) 0x80, encoded[1] );

        encoded = Value.getBytes( -27066 );

        Assert.assertEquals( ( byte ) 0x96, ( byte ) encoded[0] );
        Assert.assertEquals( 0x46, encoded[1] );

    }


    public void testEncodeInt3Bytes()
    {

        byte[] encoded = Value.getBytes( 32787 );

        Assert.assertEquals( 0x00, encoded[0] );
        Assert.assertEquals( ( byte ) 0x80, encoded[1] );
        Assert.assertEquals( ( byte ) 0x13, encoded[2] );
    }


    public void testEncodeInt()
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { Integer.MIN_VALUE, -2147483647, -16777216, -16777215, -8388608, -8388607, -65536, -65535, -32768, -32767,
                -256, -255, -128, -127, -1, 0, 1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 8388607, 8388608,
                16777215, 16777216, Integer.MAX_VALUE };

        for ( int i = 0; i < testedInt.length; i++ )
        {
            encoded = Value.getBytes( testedInt[i] );

            int value = new BigInteger( encoded ).intValue();

            Assert.assertEquals( testedInt[i], value );
        }
    }


    public void testDecodeInt() throws Exception
    {
        byte[] encoded = null;
        int[] testedInt = new int[]
            { Integer.MIN_VALUE, -2147483647, -16777216, -16777215, -8388608, -8388607, -65536, -65535, -32768, -32767,
                -256, -255, -128, -127, -1, 0, 1, 127, 128, 255, 256, 32767, 32768, 65535, 65536, 8388607, 8388608,
                16777215, 16777216, Integer.MAX_VALUE };

        for ( int i = 0; i < testedInt.length; i++ )
        {
            encoded = new BigInteger( Integer.toString( testedInt[i] ) ).toByteArray();

            int value = IntegerDecoder.parse( new Value( encoded ) );

            Assert.assertEquals( testedInt[i], value );
        }
    }
}
