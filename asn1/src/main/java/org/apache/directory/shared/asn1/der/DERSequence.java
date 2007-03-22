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
import java.util.Enumeration;
import java.util.Vector;


public class DERSequence implements DEREncodable
{
    private Vector v = new Vector();


    public void add( DEREncodable obj )
    {
        v.addElement( obj );
    }


    public Enumeration getObjects()
    {
        return v.elements();
    }


    public DEREncodable get( int i )
    {
        return ( DEREncodable ) v.elementAt( i );
    }


    public int size()
    {
        return v.size();
    }


    /**
     * As DER requires the constructed, definite-length model to be used for
     * structured types, this varies slightly from the ASN.1 descriptions given.
     * Rather than just outputing SEQUENCE, we also have to specify CONSTRUCTED,
     * and the objects length.
     */
    public void encode( ASN1OutputStream out ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        Enumeration e = getObjects();

        while ( e.hasMoreElements() )
        {
            aos.writeObject( e.nextElement() );
        }

        aos.close();

        byte[] bytes = baos.toByteArray();

        out.writeEncoded( DERObject.SEQUENCE | DERObject.CONSTRUCTED, bytes );
    }
}
