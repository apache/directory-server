/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
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
