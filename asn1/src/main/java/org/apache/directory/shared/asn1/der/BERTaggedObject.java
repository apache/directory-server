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
import java.util.Enumeration;


/**
 * BER TaggedObject
 */
public class BERTaggedObject extends DERTaggedObject
{
    /**
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public BERTaggedObject(int tag, DEREncodable obj)
    {
        super( tag, obj );
    }


    /**
     * @param explicit true
     *            if an explicitly tagged object.
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public BERTaggedObject(boolean explicit, int tag, DEREncodable obj)
    {
        super( explicit, tag, obj );
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        out.write( DERObject.CONSTRUCTED | DERObject.TAGGED | tag );
        out.write( DERObject.TAGGED );

        if ( !empty )
        {
            if ( !explicit )
            {
                if ( obj instanceof DEROctetString )
                {
                    Enumeration<DEREncodable> e;

                    if ( obj instanceof BERConstructedOctetString )
                    {
                        e = ( ( BERConstructedOctetString ) obj ).getObjects();
                    }
                    else
                    {
                        DEROctetString octs = ( DEROctetString ) obj;
                        BERConstructedOctetString berO = new BERConstructedOctetString( octs.getOctets() );

                        e = berO.getObjects();
                    }

                    while ( e.hasMoreElements() )
                    {
                        out.writeObject( e.nextElement() );
                    }
                }
                else if ( obj instanceof DERSequence )
                {
                    Enumeration<DEREncodable> e = ( ( DERSequence ) obj ).getObjects();

                    while ( e.hasMoreElements() )
                    {
                        out.writeObject( e.nextElement() );
                    }
                }
                else
                {
                    throw new RuntimeException( "Not implemented: " + obj.getClass().getName() );
                }
            }
            else
            {
                out.writeObject( obj );
            }
        }

        out.write( DERObject.TERMINATOR );
        out.write( DERObject.TERMINATOR );
    }
}
