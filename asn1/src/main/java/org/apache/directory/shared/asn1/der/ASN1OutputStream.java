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


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class ASN1OutputStream extends FilterOutputStream
{
    public ASN1OutputStream(OutputStream os)
    {
        super( os );
    }


    public ASN1OutputStream(ByteBuffer out)
    {
        super( newOutputStream( out ) );
    }


    public static OutputStream newOutputStream( final ByteBuffer buf )
    {
        return new OutputStream()
        {
            public synchronized void write( int integer ) throws IOException
            {
                buf.put( ( byte ) integer );
            }


            public synchronized void write( byte[] bytes, int off, int len ) throws IOException
            {
                buf.put( bytes, off, len );
            }
        };
    }


    private void writeLength( int length ) throws IOException
    {
        if ( length > 127 )
        {
            int size = 1;
            int val = length;

            while ( ( val >>>= 8 ) != 0 )
            {
                size++;
            }

            write( ( byte ) ( size | 0x80 ) );

            for ( int i = ( size - 1 ) * 8; i >= 0; i -= 8 )
            {
                write( ( byte ) ( length >> i ) );
            }
        }
        else
        {
            write( ( byte ) length );
        }
    }


    void writeEncoded( int tag, byte[] bytes ) throws IOException
    {
        write( tag );
        writeLength( bytes.length );
        write( bytes );
    }


    public void writeObject( Object obj ) throws IOException
    {
        if ( obj == null )
        {
            writeNull();
        }
        else if ( obj instanceof DEREncodable )
        {
            ( ( DEREncodable ) obj ).encode( this );
        }
        else
        {
            throw new IOException( "Object not DEREncodable." );
        }
    }


    protected void writeNull() throws IOException
    {
        write( DERObject.NULL );
        write( DERObject.TERMINATOR );
    }
}
