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
 * DER Boolean object.
 */
public class DERBoolean extends DERObject
{
    private static final byte[] trueArray =
        { ( byte ) 0xff };

    private static final byte[] falseArray =
        { ( byte ) 0x00 };

    public static final DERBoolean TRUE = new DERBoolean( trueArray );

    public static final DERBoolean FALSE = new DERBoolean( falseArray );


    /**
     * Basic DERObject constructor.
     */
    public DERBoolean(byte[] value)
    {
        super( BOOLEAN, value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DERBoolean valueOf( boolean value )
    {
        return ( value ? TRUE : FALSE );
    }


    /**
     * Lazy accessor
     * 
     * @return boolean value
     */
    public boolean isTrue()
    {
        return value[0] == ( byte ) 0xff;
    }
}
