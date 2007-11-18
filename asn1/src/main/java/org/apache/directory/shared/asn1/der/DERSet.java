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


/**
 * A DER encoded set object
 */
public class DERSet implements DEREncodable
{
    protected Vector<DEREncodable> set = new Vector<DEREncodable>();


    public Enumeration<DEREncodable> getObjects()
    {
        return set.elements();
    }


    public DEREncodable getObjectAt( int index )
    {
        return ( DEREncodable ) set.elementAt( index );
    }


    public int size()
    {
        return set.size();
    }


    public void add( DEREncodable obj )
    {
        set.addElement( obj );
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        Enumeration<DEREncodable> e = getObjects();

        while ( e.hasMoreElements() )
        {
            aos.writeObject( e.nextElement() );
        }

        aos.close();

        byte[] bytes = baos.toByteArray();

        out.writeEncoded( DERObject.SET | DERObject.CONSTRUCTED, bytes );
    }
}
