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
import java.io.OutputStream;


public class DERObjectIdentifier extends DERObject
{
    String identifier;


    DERObjectIdentifier(byte[] bytes)
    {
        super( OBJECT_IDENTIFIER, bytes );

        StringBuffer objId = new StringBuffer();
        long value = 0;
        boolean first = true;

        for ( int i = 0; i != bytes.length; i++ )
        {
            int b = bytes[i] & 0xff;

            value = value * 128 + ( b & 0x7f );
            if ( ( b & 0x80 ) == 0 ) // end of number reached
            {
                if ( first )
                {
                    switch ( ( int ) value / 40 )
                    {
                        case 0:
                            objId.append( '0' );
                            break;
                        case 1:
                            objId.append( '1' );
                            value -= 40;
                            break;
                        default:
                            objId.append( '2' );
                            value -= 80;
                    }
                    first = false;
                }

                objId.append( '.' );
                objId.append( Long.toString( value ) );
                value = 0;
            }
        }

        this.identifier = objId.toString();
    }


    private void writeField( OutputStream out, long fieldValue ) throws IOException
    {
        if ( fieldValue >= ( 1 << 7 ) )
        {
            if ( fieldValue >= ( 1 << 14 ) )
            {
                if ( fieldValue >= ( 1 << 21 ) )
                {
                    if ( fieldValue >= ( 1 << 28 ) )
                    {
                        if ( fieldValue >= ( 1 << 35 ) )
                        {
                            if ( fieldValue >= ( 1 << 42 ) )
                            {
                                if ( fieldValue >= ( 1 << 49 ) )
                                {
                                    if ( fieldValue >= ( 1 << 56 ) )
                                    {
                                        out.write( ( int ) ( fieldValue >> 56 ) | 0x80 );
                                    }
                                    out.write( ( int ) ( fieldValue >> 49 ) | 0x80 );
                                }
                                out.write( ( int ) ( fieldValue >> 42 ) | 0x80 );
                            }
                            out.write( ( int ) ( fieldValue >> 35 ) | 0x80 );
                        }
                        out.write( ( int ) ( fieldValue >> 28 ) | 0x80 );
                    }
                    out.write( ( int ) ( fieldValue >> 21 ) | 0x80 );
                }
                out.write( ( int ) ( fieldValue >> 14 ) | 0x80 );
            }
            out.write( ( int ) ( fieldValue >> 7 ) | 0x80 );
        }
        out.write( ( int ) fieldValue & 0x7f );
    }


    public void encode( ASN1OutputStream out ) throws IOException
    {
        OIDTokenizer tok = new OIDTokenizer( identifier );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ASN1OutputStream aos = new ASN1OutputStream( baos );

        writeField( baos, Integer.parseInt( tok.nextToken() ) * 40 + Integer.parseInt( tok.nextToken() ) );

        while ( tok.hasMoreTokens() )
        {
            writeField( baos, Long.parseLong( tok.nextToken() ) );
        }

        aos.close();

        byte[] bytes = baos.toByteArray();

        out.writeEncoded( OBJECT_IDENTIFIER, bytes );
    }
}
