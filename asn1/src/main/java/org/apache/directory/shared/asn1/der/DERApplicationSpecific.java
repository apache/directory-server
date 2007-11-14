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


import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * DER Application Specific object.
 */
public class DERApplicationSpecific extends DERObject
{
    private int tag;


    /**
     * Basic DERObject constructor.
     */
    public DERApplicationSpecific(int tag, byte[] value)
    {
        super( tag, value );
        this.tag = tag;
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DERApplicationSpecific valueOf( int tag, DEREncodable object ) throws IOException
    {
        tag = tag | CONSTRUCTED;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        aos.writeObject( object );

        return new DERApplicationSpecific( tag, baos.toByteArray() );
    }


    public int getApplicationTag()
    {
        return tag & 0x1F;
    }


    public DEREncodable getObject() throws IOException
    {   
        final ASN1InputStream ais = new ASN1InputStream( getOctets() );
        try
        {
            return ais.readObject();
        }
        finally
        {
            ais.close();
        }
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        out.writeEncoded( APPLICATION | tag, value );
    }
}
