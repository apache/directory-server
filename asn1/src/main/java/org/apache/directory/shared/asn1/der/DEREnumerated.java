/*
 * Copyright (c) 2000 - 2006 The Legion Of The Bouncy Castle (http://www.bouncycastle.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */

package org.apache.directory.shared.asn1.der;


/**
 * DER Enumerated object.
 */
public class DEREnumerated extends DERObject
{
    /**
     * Basic DERObject constructor.
     */
    public DEREnumerated(byte[] value)
    {
        super( ENUMERATED, value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DEREnumerated valueOf( int integer )
    {
        return new DEREnumerated( intToOctet( integer ) );
    }


    /**
     * Lazy accessor
     * 
     * @return integer value
     */
    public int intValue()
    {
        return octetToInt( value );
    }


    private static int octetToInt( byte[] bytes )
    {
        int result = 0;

        for ( int ii = 0; ii < Math.min( 4, bytes.length ); ii++ )
        {
            result += bytes[ii] * ( 16 ^ ii );
        }
        return result;
    }


    private static byte[] intToOctet( int integer )
    {
        byte[] result = new byte[4];

        for ( int ii = 0, shift = 24; ii < 4; ii++, shift -= 8 )
        {
            result[ii] = ( byte ) ( 0xFF & ( integer >> shift ) );
        }
        return result;
    }
}
