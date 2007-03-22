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


public class BERConstructedOctetString extends DEROctetString
{
    private Vector octets;


    /**
     * @param string
     *            the octets making up the octet string.
     */
    public BERConstructedOctetString(byte[] string)
    {
        super( string );
    }


    public BERConstructedOctetString(Vector octets)
    {
        super( toBytes( octets ) );

        this.octets = octets;
    }


    /**
     * Convert a vector of octet strings into a single byte string.
     */
    static private byte[] toBytes( Vector octs )
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for ( int i = 0; i != octs.size(); i++ )
        {
            try
            {
                DEROctetString o = ( DEROctetString ) octs.elementAt( i );

                baos.write( o.getOctets() );
            }
            catch ( ClassCastException e )
            {
                throw new IllegalArgumentException( octs.elementAt( i ).getClass().getName()
                    + " found in input should only contain DEROctetString." );
            }
            catch ( IOException e )
            {
                throw new IllegalArgumentException( "Exception converting octets " + e.toString() );
            }
        }

        return baos.toByteArray();
    }


    /**
     * @return Enumeration the DER octets that make up this string.
     */
    public Enumeration getObjects()
    {
        if ( octets == null )
        {
            return generateOcts().elements();
        }

        return octets.elements();
    }


    private Vector generateOcts()
    {
        int start = 0;
        int end = 0;
        Vector vector = new Vector();

        while ( ( end + 1 ) < value.length )
        {
            if ( value[end] == 0 && value[end + 1] == 0 )
            {
                byte[] nStr = new byte[end - start + 1];

                System.arraycopy( value, start, nStr, 0, nStr.length );

                vector.addElement( new DEROctetString( nStr ) );
                start = end + 1;
            }
            end++;
        }

        byte[] nStr = new byte[value.length - start];

        System.arraycopy( value, start, nStr, 0, nStr.length );

        vector.addElement( new DEROctetString( nStr ) );

        return vector;
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        out.write( CONSTRUCTED | OCTET_STRING );

        out.write( DERObject.TAGGED );

        if ( octets != null )
        {
            for ( int i = 0; i != octets.size(); i++ )
            {
                out.writeObject( octets.elementAt( i ) );
            }
        }
        else
        {
            int start = 0;
            int end = 0;

            while ( ( end + 1 ) < value.length )
            {
                if ( value[end] == 0 && value[end + 1] == 0 )
                {
                    byte[] newString = new byte[end - start + 1];

                    System.arraycopy( value, start, newString, 0, newString.length );

                    out.writeObject( new DEROctetString( newString ) );
                    start = end + 1;
                }
                end++;
            }

            byte[] newString = new byte[value.length - start];

            System.arraycopy( value, start, newString, 0, newString.length );

            out.writeObject( new DEROctetString( newString ) );
        }

        out.write( TERMINATOR );
        out.write( TERMINATOR );
    }
}
