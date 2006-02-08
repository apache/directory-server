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

import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;


/**
 * Test the Primitives
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrimitivesTest extends TestCase
{
    /**
     * Test the Integer Primitive
     */
    public void testIntegerPrimitive() throws IntegerDecoderException
    {
        Value value = new Value();

        value.init( 1 );
        value.setData( new byte[]
            { 0x00 } ); // res = 0
        Assert.assertEquals( 0, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 1 );
        value.setData( new byte[]
            { 0x01 } ); // res = 1
        Assert.assertEquals( 1, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 1 );
        value.setData( new byte[]
            { ( byte ) 0xFF } ); // res = 255
        Assert.assertEquals( -1, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 2 );
        value.setData( new byte[]
            { 0x00, 0x01 } ); // res = 1
        Assert.assertEquals( 1, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 2 );
        value.setData( new byte[]
            { 0x01, 0x00 } ); // res = 256
        Assert.assertEquals( 256, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 2 );
        value.setData( new byte[]
            { 0x01, 0x01 } ); // res = 257
        Assert.assertEquals( 257, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 2 );
        value.setData( new byte[]
            { 0x01, ( byte ) 0xFF } ); // res = 511
        Assert.assertEquals( 511, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 2 );
        value.setData( new byte[]
            { 0x02, 0x00 } ); // res = 512
        Assert.assertEquals( 512, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 3 );
        value.setData( new byte[]
            { 0x00, ( byte ) 0xFF, ( byte ) 0xFF } ); // res = 65535
        Assert.assertEquals( 65535, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 4 );
        value.setData( new byte[]
            { ( byte ) 0x7F, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF } ); // res
                                                                                // =
                                                                                // 2^31
                                                                                // - 1
                                                                                // =
                                                                                // MaxInt
        Assert.assertEquals( Integer.MAX_VALUE, IntegerDecoder.parse( value ) );
        value.reset();

        value.init( 4 );
        value.setData( new byte[]
            { ( byte ) 0x80, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00 } ); // res
                                                                                // =
                                                                                // 2^31
                                                                                // =
                                                                                // MinInt
        Assert.assertEquals( Integer.MIN_VALUE, IntegerDecoder.parse( value ) );
        value.reset();
    }
} // end class TLVTagDecoderTest
