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


import java.math.BigInteger;


/**
 * DER Integer object.
 */
public class DERInteger extends DERObject
{
    /**
     * Basic DERObject constructor.
     */
    DERInteger(byte[] value)
    {
        super( INTEGER, value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DERInteger valueOf( int integer )
    {
        return new DERInteger( intToOctet( integer ) );
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


    /**
     * Lazy accessor
     * 
     * @return BigInteger value
     */
    public BigInteger bigIntValue() 
    {
        return new BigInteger( value );
    }
    
    
    private static int octetToInt( byte[] bytes )
    {
        return new BigInteger( bytes ).intValue();
    }


    private static byte[] intToOctet( int integer )
    {
        return BigInteger.valueOf( integer ).toByteArray();
    }
}
