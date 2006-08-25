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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Vector;


/**
 * General purpose ASN.1 decoder.
 */
public class ASN1InputStream extends FilterInputStream
{
    private boolean EOF_FOUND = false;

    private DERObject END_OF_STREAM = new DERObject( 0, null )
    {
        public void encode( ASN1OutputStream out ) throws IOException
        {
            throw new IOException( "End of stream." );
        }


        public int hashCode()
        {
            return 0;
        }


        public boolean equals( Object o )
        {
            return o == this;
        }
    };


    public ASN1InputStream(ByteBuffer in)
    {
        super( newInputStream( in ) );
    }


    public ASN1InputStream(byte[] input)
    {
        super( new ByteArrayInputStream( input ) );
    }


    private static InputStream newInputStream( final ByteBuffer buf )
    {
        return new InputStream()
        {
            public synchronized int read() throws IOException
            {
                if ( !buf.hasRemaining() )
                {
                    return -1;
                }

                int result = buf.get() & 0x000000FF;

                return result;
            }


            public synchronized int read( byte[] bytes, int off, int len ) throws IOException
            {
                // Read only what's left
                len = Math.min( len, buf.remaining() );
                buf.get( bytes, off, len );
                return len;
            }
        };
    }


    protected int readLength() throws IOException
    {
        int length = read();
        if ( length < 0 )
        {
            throw new IOException( "EOF found when length expected." );
        }

        // Indefinite-length encoding.
        if ( length == 0x80 )
        {
            return -1;
        }

        if ( length > 127 )
        {
            int size = length & 0x7f;

            if ( size > 4 )
            {
                throw new IOException( "DER length more than 4 bytes." );
            }

            length = 0;
            for ( int i = 0; i < size; i++ )
            {
                int next = read();

                if ( next < 0 )
                {
                    throw new IOException( "EOF found reading length." );
                }

                length = ( length << 8 ) + next;
            }

            if ( length < 0 )
            {
                throw new IOException( "Corrupted steam - negative length found." );
            }
        }

        return length;
    }


    protected void readFully( byte[] bytes ) throws IOException
    {
        int left = bytes.length;
        int len;

        if ( left == 0 )
        {
            return;
        }

        while ( ( len = read( bytes, bytes.length - left, left ) ) > 0 )
        {
            if ( ( left -= len ) == 0 )
            {
                return;
            }
        }

        if ( left != 0 )
        {
            throw new EOFException( "EOF encountered in middle of object." );
        }
    }


    /**
     * Build an object given its tag and a byte stream.
     */
    protected DEREncodable buildObject( int tag, byte[] bytes ) throws IOException
    {
        if ( ( tag & DERObject.APPLICATION ) != 0 )
        {
            return new DERApplicationSpecific( tag, bytes );
        }

        switch ( tag )
        {
            case DERObject.NULL:
                return new DERNull();
            case DERObject.SEQUENCE | DERObject.CONSTRUCTED:
                ASN1InputStream ais = new ASN1InputStream( bytes );

                DERSequence sequence = new DERSequence();

                DEREncodable obj = ais.readObject();

                while ( obj != null )
                {
                    sequence.add( obj );
                    obj = ais.readObject();
                }

                return sequence;
            case DERObject.SET | DERObject.CONSTRUCTED:
                ais = new ASN1InputStream( bytes );
                DERSet set = new DERSet();

                obj = ais.readObject();

                while ( obj != null )
                {
                    set.add( obj );
                    obj = ais.readObject();
                }

                return set;
            case DERObject.BOOLEAN:
                return new DERBoolean( bytes );
            case DERObject.INTEGER:
                return new DERInteger( bytes );
            case DERObject.ENUMERATED:
                return new DEREnumerated( bytes );
            case DERObject.OBJECT_IDENTIFIER:
                return new DERObjectIdentifier( bytes );
            case DERObject.BIT_STRING:
                return new DERBitString( bytes );
            case DERObject.NUMERIC_STRING:
                return new DERNumericString( bytes );
            case DERObject.UTF8_STRING:
                return new DERUTF8String( bytes );
            case DERObject.PRINTABLE_STRING:
                return new DERPrintableString( bytes );
            case DERObject.IA5_STRING:
                return new DERIA5String( bytes );
            case DERObject.T61_STRING:
                return new DERTeletexString( bytes );
            case DERObject.VISIBLE_STRING:
                return new DERVisibleString( bytes );
            case DERObject.GENERAL_STRING:
                return new DERGeneralString( bytes );
            case DERObject.UNIVERSAL_STRING:
                return new DERUniversalString( bytes );
            case DERObject.BMP_STRING:
                return new DERBMPString( bytes );
            case DERObject.OCTET_STRING:
                return new DEROctetString( bytes );
            case DERObject.UTC_TIME:
                return new DERUTCTime( bytes );
            case DERObject.GENERALIZED_TIME:
                return new DERGeneralizedTime( bytes );
            default:
                // Tag number is bottom 5 bits.
                if ( ( tag & DERObject.TAGGED ) != 0 )
                {
                    int tagNo = tag & 0x1f;

                    if ( tagNo == 0x1f )
                    {
                        int idx = 0;

                        tagNo = 0;

                        while ( ( bytes[idx] & 0x80 ) != 0 )
                        {
                            tagNo |= ( bytes[idx++] & 0x7f );
                            tagNo <<= 7;
                        }

                        tagNo |= ( bytes[idx] & 0x7f );

                        byte[] tmp = bytes;

                        bytes = new byte[tmp.length - ( idx + 1 )];
                        System.arraycopy( tmp, idx + 1, bytes, 0, bytes.length );
                    }

                    // Empty tag.
                    if ( bytes.length == 0 )
                    {
                        if ( ( tag & DERObject.CONSTRUCTED ) == 0 )
                        {
                            return new DERTaggedObject( tagNo, new DERNull() );
                        }

                        return new DERTaggedObject( false, tagNo, new DERSequence() );
                    }

                    // Simple type - implicit, return an octet string.
                    if ( ( tag & DERObject.CONSTRUCTED ) == 0 )
                    {
                        return new DERTaggedObject( false, tagNo, new DEROctetString( bytes ) );
                    }

                    ais = new ASN1InputStream( bytes );

                    DEREncodable encodable = ais.readObject();

                    // Explicitly tagged - if it isn't we'd have to tell from
                    // the context.
                    if ( ais.available() == 0 )
                    {
                        return new DERTaggedObject( true, tagNo, encodable, bytes );
                    }

                    // Another implicit object, create a sequence.
                    DERSequence derSequence = new DERSequence();

                    while ( encodable != null )
                    {
                        derSequence.add( encodable );
                        encodable = ais.readObject();
                    }

                    return new DERTaggedObject( false, tagNo, derSequence );
                }

                return new DERUnknownTag( tag, bytes );
        }
    }


    /**
     * Read a string of bytes representing an indefinite length object.
     */
    private byte[] readIndefiniteLengthFully() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b, b1;

        b1 = read();

        while ( ( b = read() ) >= 0 )
        {
            if ( b1 == 0 && b == 0 )
            {
                break;
            }

            baos.write( b1 );
            b1 = b;
        }

        return baos.toByteArray();
    }


    private BERConstructedOctetString buildConstructedOctetString() throws IOException
    {
        Vector octets = new Vector();

        for ( ;; )
        {
            DEREncodable encodable = readObject();

            if ( encodable == END_OF_STREAM )
            {
                break;
            }

            octets.addElement( encodable );
        }

        return new BERConstructedOctetString( octets );
    }


    public DEREncodable readObject() throws IOException
    {
        int tag = read();
        if ( tag == -1 )
        {
            if ( EOF_FOUND )
            {
                throw new EOFException( "Attempt to read past end of file." );
            }

            EOF_FOUND = true;

            return null;
        }

        int length = readLength();

        // Indefinite length method.
        if ( length < 0 )
        {
            switch ( tag )
            {
                case DERObject.NULL:
                    return new BERNull();
                case DERObject.SEQUENCE | DERObject.CONSTRUCTED:
                    BERSequence sequence = new BERSequence();

                    for ( ;; )
                    {
                        DEREncodable obj = readObject();

                        if ( obj == END_OF_STREAM )
                        {
                            break;
                        }

                        sequence.add( obj );
                    }
                    return sequence;
                case DERObject.SET | DERObject.CONSTRUCTED:
                    BERSet set = new BERSet();

                    for ( ;; )
                    {
                        DEREncodable obj = readObject();

                        if ( obj == END_OF_STREAM )
                        {
                            break;
                        }

                        set.add( obj );
                    }
                    return set;
                case DERObject.OCTET_STRING | DERObject.CONSTRUCTED:
                    return buildConstructedOctetString();
                default:
                    // Tag number is bottom 5 bits.
                    if ( ( tag & DERObject.TAGGED ) != 0 )
                    {
                        int tagNo = tag & 0x1f;

                        if ( tagNo == 0x1f )
                        {
                            int b = read();

                            tagNo = 0;

                            while ( ( b >= 0 ) && ( ( b & 0x80 ) != 0 ) )
                            {
                                tagNo |= ( b & 0x7f );
                                tagNo <<= 7;
                                b = read();
                            }

                            tagNo |= ( b & 0x7f );
                        }

                        // Simple type - implicit, return an octet string.
                        if ( ( tag & DERObject.CONSTRUCTED ) == 0 )
                        {
                            byte[] bytes = readIndefiniteLengthFully();

                            return new BERTaggedObject( false, tagNo, new DEROctetString( bytes ) );
                        }

                        // Either constructed or explicitly tagged
                        DEREncodable dObj = readObject();

                        // Empty tag!
                        if ( dObj == END_OF_STREAM )
                        {
                            return new DERTaggedObject( tagNo );
                        }

                        DEREncodable next = readObject();

                        // Explicitly tagged.
                        if ( next == END_OF_STREAM )
                        {
                            return new BERTaggedObject( tagNo, dObj );
                        }

                        // Another implicit object, create a sequence.
                        BERSequence berSequence = new BERSequence();

                        berSequence.add( dObj );

                        do
                        {
                            berSequence.add( next );
                            next = readObject();
                        }
                        while ( next != END_OF_STREAM );

                        return new BERTaggedObject( false, tagNo, berSequence );
                    }

                    throw new IOException( "Unknown BER object encountered." );
            }
        }

        // End of contents marker.
        if ( tag == 0 && length == 0 )
        {
            return END_OF_STREAM;
        }

        byte[] bytes = new byte[length];

        readFully( bytes );

        return buildObject( tag, bytes );
    }
}
