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
package org.apache.directory.server.kerberos.shared.keytab;


import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.kerberos.components.EncryptionKey;


/**
 * Encode keytab fields into a {@link ByteBuffer}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeytabEncoder
{
    /**
     * Tells if the keytabCersion is 0x0501 or 0x0502
     */
    private short getKeytabVersion( byte[] version )
    {
        if ( ( version != null ) && ( version.length == 2 ) && ( version[0] == 0x05 ) )
        {
            switch ( version[1] )
            {
                case 0x01:
                    return Keytab.VERSION_0X501;

                case 0x02:
                    return Keytab.VERSION_0X502;

                default:
                    return -1;
            }
        }

        return -1;
    }


    /**
     * Write the keytab version and entries into a {@link ByteBuffer}.
     *
     * @param keytabVersion
     * @param entries
     * @return The ByteBuffer.
     */
    public ByteBuffer write( byte[] keytabVersion, List<KeytabEntry> entries )
    {
        List<ByteBuffer> keytabEntryBuffers = new ArrayList<>();
        short version = getKeytabVersion( keytabVersion );

        int buffersSize = encodeKeytabEntries( keytabEntryBuffers, version, entries );

        ByteBuffer buffer = ByteBuffer.allocate(
            keytabVersion.length + buffersSize );

        // The keytab version (0x0502 or 0x5001)
        buffer.put( keytabVersion );

        for ( ByteBuffer keytabEntryBuffer : keytabEntryBuffers )
        {
            // The buffer
            buffer.put( keytabEntryBuffer );
        }

        buffer.flip();

        return buffer;
    }


    /**
     * Encode the keytab entries. Each entry stores :
     * - the size
     * - the principal name
     * - the type (int, 4 bytes)
     * - the timestamp (int, 4 bytes)
     * - the key version (1 byte)
     * - the key 
     *
     * @param buffer
     * @param entries
     */
    private int encodeKeytabEntries( List<ByteBuffer> buffers, short version, List<KeytabEntry> entries )
    {
        int size = 0;

        for ( KeytabEntry keytabEntry : entries )
        {
            ByteBuffer entryBuffer = encodeKeytabEntry( version, keytabEntry );

            buffers.add( entryBuffer );

            // The buffer size
            size += entryBuffer.limit();
        }

        return size;
    }


    /**
     * Encode a "keytab entry," which consists of a principal name,
     * principal type, key version number, and key material.
     */
    private ByteBuffer encodeKeytabEntry( short version, KeytabEntry entry )
    {
        // Compute the principalName encoding
        ByteBuffer principalNameBuffer = encodePrincipalName( version, entry.getPrincipalName() );

        // Compute the keyblock encoding
        ByteBuffer keyBlockBuffer = encodeKeyBlock( entry.getKey() );

        int bufferSize =
            4 + // size
                principalNameBuffer.limit() + // principalName size
                4 + // timeStamp
                1 + // keyVersion
                keyBlockBuffer.limit(); // keyBlock size

        if ( version == Keytab.VERSION_0X502 )
        {
            bufferSize += 4; // Add the principal NameType only for version 0x502
        }

        ByteBuffer buffer = ByteBuffer.allocate( bufferSize );

        // Store the size
        buffer.putInt( bufferSize - 4 );

        // Store the principalNames
        buffer.put( principalNameBuffer );

        // Store the principal type if version == 0x0502
        if ( version == Keytab.VERSION_0X502 )
        {
            buffer.putInt( ( int ) entry.getPrincipalType() );
        }

        // Store the timeStamp 
        buffer.putInt( ( int ) ( entry.getTimeStamp().getTime() / 1000 ) );

        // Store the key version
        buffer.put( entry.getKeyVersion() );

        // Store the KeyBlock
        buffer.put( keyBlockBuffer );
        buffer.flip();

        return buffer;
    }


    /**
     * Encode a principal name.
     *
     * @param buffer
     * @param principalName
     */
    private ByteBuffer encodePrincipalName( short version, String principalName )
    {
        String[] split = principalName.split( "@" );
        String nameComponentPart = split[0];
        String realm = split[1];

        String[] nameComponents = nameComponentPart.split( "/" );

        // Compute the size of the buffer
        List<byte[]> strings = new ArrayList<>();

        // Initialize the size with the number of components' size
        int size = 2;

        size += encodeCountedString( strings, realm );

        // compute NameComponents
        for ( String nameComponent : nameComponents )
        {
            size += encodeCountedString( strings, nameComponent );
        }

        ByteBuffer buffer = ByteBuffer.allocate( size );

        // Now, write the data into the buffer
        // store the numComponents
        if ( version == Keytab.VERSION_0X501 )
        {
            // increment for version 0x0501
            buffer.putShort( ( short ) ( nameComponents.length + 1 ) );
        }
        else
        {
            // Version = OxO502
            buffer.putShort( ( short ) ( nameComponents.length ) );
        }

        // Store the realm and the nameComponents
        for ( byte[] string : strings )
        {
            buffer.putShort( ( short ) ( string.length ) );
            buffer.put( string );
        }

        buffer.flip();

        return buffer;
    }


    /**
     * Encode a 16-bit encryption type and symmetric key material.
     * 
     * We store the KeyType value ( a short ) and the KeyValue ( a length
     * on a short and the bytes )
     */
    private ByteBuffer encodeKeyBlock( EncryptionKey key )
    {
        byte[] keyBytes = key.getKeyValue();
        int size = 2 + 2 + keyBytes.length; // type, length, data
        ByteBuffer buffer = ByteBuffer.allocate( size );

        // The type
        buffer.putShort( ( short ) key.getKeyType().getValue() );

        // Use a prefixed 16-bit length to encode raw bytes.
        buffer.putShort( ( short ) keyBytes.length );
        buffer.put( keyBytes );

        buffer.flip();

        return buffer;
    }


    /**
     * Use a prefixed 16-bit length to encode a String.  Realm and name
     * components are ASCII encoded text with no zero terminator.
     */
    private short encodeCountedString( List<byte[]> nameComponentBytes, String string )
    {
        try
        {
            byte[] data = string.getBytes( "US-ASCII" );
            nameComponentBytes.add( data );

            return ( short ) ( data.length + 2 );
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new RuntimeException( uee.getMessage(), uee );
        }
    }
}
