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


import java.io.IOException;


/**
 * DER Bit String object.
 */
public class DERBitString extends DERObject
{
    /**
     * Basic DERObject constructor.
     */
    public DERBitString(byte[] value)
    {
        super( BIT_STRING, value );
    }


    public byte[] getOctets()
    {
        return value;
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        byte[] bytes = new byte[value.length + 1];

        bytes[0] = ( byte ) 0x00;
        System.arraycopy( value, 0, bytes, 1, bytes.length - 1 );

        out.writeEncoded( BIT_STRING, bytes );
    }
}
