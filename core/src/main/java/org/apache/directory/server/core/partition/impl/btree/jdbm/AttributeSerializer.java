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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.shared.ldap.message.AttributeImpl;

import jdbm.helper.Serializer;


/**
 * Serializes a attributes object using a custom serialization mechanism
 * so we do not have to rely on Java Serialization which is much more 
 * costly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeSerializer implements Serializer
{
    private static final long serialVersionUID = -3756830073760754086L;
  
    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /** value for type parameter for string (non-binary) attributes */
    static final byte STRING_TYPE = 0x00;
    /** value for type parameter for byte array (binary) attributes */
    static final byte BYTE_ARRAY_TYPE = 0x01;
    

    // -----------------------------------------------------------------------
    // Methods for deserialization
    // -----------------------------------------------------------------------
    
    
    /**
     * Deserializes an attribute from the custom serialization structure.
     * 
     * @see jdbm.helper.Serializer#deserialize(byte[])
     */
    public final Object deserialize( byte[] buf ) throws IOException
    {
        String id = readString( buf );
        AttributeImpl attr = new AttributeImpl( id );
        int pos = ( id.length() << 1 ) + 4;
        
        // read the type of the objects stored in this attribute
        if ( buf[pos] == STRING_TYPE )
        {
            pos++;
            while ( pos < buf.length )
            {
                String value = readString( buf, pos );
                pos += ( value.length() << 1 ) + 4;
                attr.add( value );
            }
        }
        else
        {
            pos++;
            while ( pos < buf.length )
            {
                byte[] value = readBytes( buf, pos );
                pos += value.length + 4;
                attr.add( value );
            }
        }
        
        return attr;
    }

    
    /**
     * Deserializes an attribute from the custom serialization structure.
     * 
     * @see jdbm.helper.Serializer#deserialize(byte[])
     */
    public static final DeserializedAttribute deserialize( byte[] buf, int offset ) throws IOException
    {
        final String id = readString( buf, offset );
        final AttributeImpl attr = new AttributeImpl( id );
        int pos = ( id.length() << 1 ) + 4 + offset;
        
        // read the type of the objects stored in this attribute
        if ( buf[pos] == STRING_TYPE )
        {
            pos++;
            while ( pos < buf.length )
            {
                String value = readString( buf, pos );
                pos += ( value.length() << 1 ) + 4;
                attr.add( value );
            }
        }
        else
        {
            pos++;
            while ( pos < buf.length )
            {
                byte[] value = readBytes( buf, pos );
                pos += value.length + 4;
                attr.add( value );
            }
        }
        
        return new DeserializedAttribute( attr, pos );
    }

    
    final static class DeserializedAttribute 
    {
        private final int pos;
        private final Attribute attr;
        
        private DeserializedAttribute( Attribute attr, int pos )
        {
            this.pos = pos;
            this.attr = attr;
        }

        public int getPos()
        {
            return pos;
        }

        public Attribute getAttr()
        {
            return attr;
        }
    }
    
    
    /**
     * Reads a String and it's length bytes from a buffer starting at 
     * position 0.
     * 
     * @param buf the buffer to read the length and character bytes from
     * @return the String contained at the start of the buffer
     */
    static final String readString( byte[] buf )
    {
        int length = getLength( buf );
        
        if ( length == 0 )
        {
            return "";
        }

        // create the new char buffer
        char[] strchars = new char[length>>1];
        
        int ch = 0;
        for ( int ii = 0, jj = 0; ii < strchars.length; ii++ )
        {
            jj = ( ii << 1 ) + 4;
            ch = buf[jj] << 8 & 0x0000FF00;
            ch |= buf[jj+1] & 0x000000FF;
            strchars[ii] = ( char ) ch;
        }

        return new String( strchars );
    }
    
    
    /**
     * Reads a String and it's length bytes from a buffer starting at 
     * a specific offset.
     * 
     * @param buf the buffer to read the length and character bytes from
     * @param offset the offset into the buffer to start reading from
     * @return the String contained at the offset in the buffer
     */
    static final String readString( byte[] buf, int offset )
    {
        int length = getLength( buf, offset );
        
        if ( length == 0 )
        {
            return "";
        }

        // create the new char buffer
        char[] strchars = new char[length>>1];
        
        int ch = 0;
        for ( int ii = 0, jj = 0; ii < strchars.length; ii++ )
        {
            jj = ( ii << 1 ) + 4 + offset;
            ch = buf[jj] << 8 & 0x0000FF00;
            ch |= buf[jj+1] & 0x000000FF;
            strchars[ii] = ( char ) ch;
        }

        return new String( strchars );
    }
    
    
    /**
     * Reads a byte array from a buffer including its length starting
     * from an offset in the buffer.
     * 
     * @param buf the buffer to read the byte array from
     * @param offset the offset to start reading from starting with 4-byte length
     * @return the byte array contained in the buffer
     */
    static final byte[] readBytes( byte[] buf, int offset )
    {
        int length = getLength( buf, offset );
        
        if ( length == 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] bites = new byte[length];
        System.arraycopy( buf, offset+4, bites, 0, length );
        return bites;
    }

    
    // -----------------------------------------------------------------------
    // Methods for serialization
    // -----------------------------------------------------------------------
    
    
    /**
     * Serializes an attribute using the following structure:
     * <code>
     *   [id-length][id-bytes][is-binary][length0][value0]...[lengthN][valueN]
     * </code>
     * 
     * Here the id-length is the 4 byte int value of the length of bytes
     * for the id string bytes.  The id-bytes are the bytes for the id string.
     * The is-binary byte is a true or false for whether or not the values 
     * are byte[] or String types.  Following this is an array of length-value 
     * tuples for the values of the Attributes.  
     * 
     */
    public byte[] serialize( Object obj ) throws IOException
    {
        Attribute attr = ( Attribute ) obj;
        
        // calculate the size of the entire byte[] and allocate
        byte[] buf = new byte[calculateSize( attr )];
        
        // write the length of the id and it's value
        int pos = write( buf, attr.getID() );
        
        try
        {
            // write the type or is-binary field
            Object first = attr.get();
            if ( first instanceof String )
            {
                buf[pos] = STRING_TYPE;
                pos++;

                // write out each value to the buffer whatever type it may be
                for ( NamingEnumeration ii = attr.getAll(); ii.hasMore(); /**/ )
                {
                    String value = ( String ) ii.next();
                    pos = write( buf, value, pos );
                }
            }
            else
            {
                buf[pos] = BYTE_ARRAY_TYPE;
                pos++;

                // write out each value to the buffer whatever type it may be
                for ( NamingEnumeration ii = attr.getAll(); ii.hasMore(); /**/ )
                {
                    byte[] value = ( byte[] ) ii.next();
                    pos = write( buf, value, pos );
                }
            }

        }
        catch ( NamingException e )
        {
            IOException ioe = new IOException( "Failed while accesssing attribute values." );
            ioe.initCause( e );
            throw ioe;
        }
        
        return buf;
    }
    
    
    static final int calculateSize( Attribute attr ) throws IOException
    {
        int size = 4; // start with first length for attribute id
        size += attr.getID().length() << 1; // the length of id * 2 added
        size++; // add one for the type
        
        try
        {
            for ( NamingEnumeration ii = attr.getAll(); ii.hasMore(); /**/ )
            {
                Object value = ii.next();
                if ( value instanceof String )
                {
                    size += ( ( String ) value ).length() << 1; // length of sting * 2
                }
                else 
                {
                    size += ( ( byte [] ) value ).length; // no need to multiply byte[]s
                }
                
                size += 4; // add 4 bytes for a length 
            }
        }
        catch ( NamingException e )
        {
            IOException ioe = new IOException( "Failed while accesssing attribute values." );
            ioe.initCause( e );
            throw ioe;
        }
        
        return size;
    }
    
    
    static final byte[] getLengthBytes( String str )
    {
        return getLengthBytes( str.length() << 1 );
    }
    
    
    static final byte[] getLengthBytes( byte[] bites )
    {
        return getLengthBytes( bites.length );
    }

    
    static final byte[] getLengthBytes( int length )
    {
        byte[] lengthBytes = new byte[4];

        lengthBytes[0] = ( byte ) ( length >> 24 & 0x000000FF );
        lengthBytes[1] = ( byte ) ( length >> 16 & 0x000000FF );
        lengthBytes[2] = ( byte ) ( length >> 8 & 0x000000FF );
        lengthBytes[3] = ( byte ) ( length & 0x000000FF );
        
        return lengthBytes;
    }

    
    static final int getLength( byte[] bites )
    {
        int length  = bites[0] << 24 & 0xFF000000;
        length     |= bites[1] << 16 & 0x00FF0000;
        length     |= bites[2] <<  8 & 0x0000FF00;
        length     |= bites[3]       & 0x000000FF;
        
        return length;
    }

    
    static final int getLength( byte[] bites, int offset )
    {
        int length  = bites[offset]   << 24 & 0xFF000000;
        length     |= bites[offset+1] << 16 & 0x00FF0000;
        length     |= bites[offset+2] <<  8 & 0x0000FF00;
        length     |= bites[offset+3]       & 0x000000FF;
        
        return length;
    }

    
    static final int write( byte[] buf, String value )
    {
        int pos = writeLengthBytes( buf, value.length() << 1 );
        return writeValueBytes( buf, value, pos );
    }
    

    static final int write( byte[] buf, byte[] value )
    {
        int pos = writeLengthBytes( buf, value.length );
        return writeValueBytes( buf, value, pos );
    }
    

    static final int write( byte[] buf, String value, int offset )
    {
        offset = writeLengthBytes( buf, value.length() << 1, offset );
        return writeValueBytes( buf, value, offset );
    }
    

    static final int write( byte[] buf, byte[] value, int offset )
    {
        offset = writeLengthBytes( buf, value.length, offset );
        return writeValueBytes( buf, value, offset );
    }
    

    static final int writeValueBytes( byte[] buf, String value )
    {
        if ( ( ( String ) value ).length() == 0 )
        {
            return 0;
        }
        
        char[] strchars = ( ( String ) value ).toCharArray();
        int jj = 0;
        for ( int ii = 0; ii < strchars.length; ii++, jj = ii << 1 )
        {
            buf[jj] = ( byte ) ( strchars[ii] >> 8 & 0x00FF );
            buf[jj+1] = ( byte ) ( strchars[ii] & 0x00FF );
        }
        return jj+2;
    }

    
    static final int writeValueBytes( byte[] buf, String value, int offset )
    {
        if ( ( ( String ) value ).length() == 0 )
        {
            return offset;
        }
        
        char[] strchars = ( ( String ) value ).toCharArray();
        int jj = 0;
        for ( int ii = 0; ii < strchars.length; ii++, jj = ii << 1 )
        {
            buf[jj+offset] = ( byte ) ( strchars[ii] >> 8 & 0x00FF );
            buf[jj+offset+1] = ( byte ) ( strchars[ii] & 0x00FF );
        }
        return jj+offset;
    }

    
    static final int writeValueBytes( byte[] buf, byte[] value, int offset )
    {
        if ( value.length == 0 )
        {
            return offset;
        }

        System.arraycopy( value, 0, buf, offset, value.length );
        return offset + value.length;
    }

    
    static final int writeLengthBytes( byte[] buf, int length )
    {
        buf[0] = ( byte ) ( length >> 24 & 0x000000FF );
        buf[1] = ( byte ) ( length >> 16 & 0x000000FF );
        buf[2] = ( byte ) ( length >> 8 & 0x000000FF );
        buf[3] = ( byte ) ( length & 0x000000FF );
        return 4;
    }
    
    
    static final int writeLengthBytes( byte[] buf, int length, int offset )
    {
        buf[0+offset] = ( byte ) ( length >> 24 & 0x000000FF );
        buf[1+offset] = ( byte ) ( length >> 16 & 0x000000FF );
        buf[2+offset] = ( byte ) ( length >> 8 & 0x000000FF );
        buf[3+offset] = ( byte ) ( length & 0x000000FF );
        return offset+4;
    }
}
