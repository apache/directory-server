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
 * DER TaggedObject
 */
public class DERTaggedObject implements DEREncodable
{
    protected int tag;

    protected boolean empty = false;

    protected boolean explicit = true;

    protected DEREncodable obj;

    private byte[] bytes;


    /**
     * create an implicitly tagged object that contains a zero length sequence.
     */
    public DERTaggedObject(int tag)
    {
        this( false, tag, new DERSequence() );
    }


    /**
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public DERTaggedObject(int tag, DEREncodable obj)
    {
        this.explicit = true;
        this.tag = tag;
        this.obj = obj;
    }


    /**
     * @param explicit
     *            true if an explicitly tagged object.
     * @param tag
     *            the tag number for this object.
     * @param obj
     *            the tagged object.
     */
    public DERTaggedObject(boolean explicit, int tag, DEREncodable obj)
    {
        this.explicit = explicit;
        this.tag = tag;
        this.obj = obj;
    }


    public DERTaggedObject(boolean explicit, int tag, DEREncodable obj, byte[] bytes)
    {
        this.explicit = explicit;
        this.tag = tag;
        this.obj = obj;

        // Copy the byte array
        this.bytes = new byte[bytes.length];
        System.arraycopy( bytes, 0, this.bytes, 0, bytes.length );
    }


    public byte[] getOctets()
    {
        return bytes;
    }


    public int getTagNo()
    {
        return tag;
    }


    /**
     * return whatever was following the tag.
     * <p>
     * Note: tagged objects are generally context dependent if you're trying to
     * extract a tagged object you should be going via the appropriate
     * getInstance method.
     */
    public DEREncodable getObject()
    {
        if ( obj != null )
        {
            return obj;
        }

        return null;
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        if ( !empty )
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASN1OutputStream aos = new ASN1OutputStream( baos );

            aos.writeObject( obj );
            aos.close();

            byte[] bytes = baos.toByteArray();

            if ( explicit )
            {
                out.writeEncoded( DERObject.CONSTRUCTED | DERObject.TAGGED | tag, bytes );
            }
            else
            {
                // need to mark constructed types
                if ( ( bytes[0] & DERObject.CONSTRUCTED ) != 0 )
                {
                    bytes[0] = ( byte ) ( DERObject.CONSTRUCTED | DERObject.TAGGED | tag );
                }
                else
                {
                    bytes[0] = ( byte ) ( DERObject.TAGGED | tag );
                }

                out.write( bytes );
            }
        }
        else
        {
            out.writeEncoded( DERObject.CONSTRUCTED | DERObject.TAGGED | tag, new byte[0] );
        }
    }
}
