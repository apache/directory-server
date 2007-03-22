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
import java.util.Arrays;


/**
 * DER object.
 */
public abstract class DERObject implements DEREncodable
{
    static final int TERMINATOR = 0x00;

    static final int BOOLEAN = 0x01;

    static final int INTEGER = 0x02;

    static final int BIT_STRING = 0x03;

    static final int OCTET_STRING = 0x04;

    static final int NULL = 0x05;

    static final int OBJECT_IDENTIFIER = 0x06;

    static final int EXTERNAL = 0x08;

    static final int ENUMERATED = 0x0a;

    static final int SEQUENCE = 0x10;

    static final int SET = 0x11;

    static final int NUMERIC_STRING = 0x12;

    static final int PRINTABLE_STRING = 0x13;

    static final int T61_STRING = 0x14;

    static final int VIDEOTEX_STRING = 0x15;

    static final int IA5_STRING = 0x16;

    static final int UTC_TIME = 0x17;

    static final int GENERALIZED_TIME = 0x18;

    static final int GRAPHIC_STRING = 0x19;

    static final int VISIBLE_STRING = 0x1a;

    static final int GENERAL_STRING = 0x1b;

    static final int UNIVERSAL_STRING = 0x1c;

    static final int BMP_STRING = 0x1e;

    static final int UTF8_STRING = 0x0c;

    static final int CONSTRUCTED = 0x20;

    static final int APPLICATION = 0x40;

    static final int TAGGED = 0x80;

    protected int tag;

    protected byte[] value;


    /**
     * Basic DERObject constructor.
     */
    protected DERObject(int tag, byte[] value)
    {
        this.tag = tag;
        this.value = value;
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        out.writeEncoded( tag, value );
    }


    byte[] getOctets()
    {
        return value;
    }


    /**
     * Fast rotate left and XOR hashcode generator.
     * 
     * @return a hash code for the byte array backing this object.
     */
    public int hashCode()
    {
        int hash = 0;
        int len = value.length;

        for ( int i = 0; i < len; i++ )
        {
            // rotate left and xor
            hash <<= 1;
            if ( hash < 0 )
            {
                hash |= 1;
            }
            hash ^= value[i];
        }

        return hash;
    }


    /**
     * Two DERObjects are equal if their underlying byte arrays are equal.
     * 
     * @return true if the two DERObject underlying byte arrays are equal.
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof DERObject ) )
        {
            return false;
        }

        DERObject that = ( DERObject ) o;

        return Arrays.equals( this.value, that.value );
    }
}
