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
 * DER UniversalString object.
 */
public class DERUniversalString extends DERString
{
    private static final char[] table =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };


    /**
     * Basic DERObject constructor.
     */
    public DERUniversalString(byte[] value)
    {
        super( UNIVERSAL_STRING, value );
    }


    public String getString()
    {
        StringBuffer buf = new StringBuffer( "#" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        try
        {
            aos.writeObject( this );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Internal error encoding BitString." );
        }

        byte[] string = baos.toByteArray();

        for ( int i = 0; i < string.length; i++ )
        {
            buf.append( table[( string[i] >>> 4 ) % 0xf] );
            buf.append( table[string[i] & 0xf] );
        }

        return buf.toString();
    }
}
