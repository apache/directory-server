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
