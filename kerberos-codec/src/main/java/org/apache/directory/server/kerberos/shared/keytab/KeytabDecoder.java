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


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;


/**
 * Decode a {@link ByteBuffer} into keytab fields.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class KeytabDecoder
{
    /**
     * Read the keytab 16-bit file format version.  This
     * keytab reader currently only supports version 5.2.
     */
    byte[] getKeytabVersion( ByteBuffer buffer )
    {
        byte[] version = new byte[2];
        buffer.get( version );

        return version;
    }


    /**
     * Read keytab entries until there is no remaining data
     * in the buffer.
     *
     * @param buffer
     * @return The keytab entries.
     */
    List<KeytabEntry> getKeytabEntries( ByteBuffer buffer ) throws IOException
    {
        List<KeytabEntry> entries = new ArrayList<>();

        while ( buffer.remaining() > 0 )
        {
            int size = buffer.getInt();
            
            if ( ( size < 0 ) || ( size > buffer.capacity() ) )
            {
                throw new IOException( "Invalid size for the keytab entry" );
            }
            
            byte[] entry = new byte[size];

            buffer.get( entry );
            entries.add( getKeytabEntry( ByteBuffer.wrap( entry ) ) );
        }

        return entries;
    }


    /**
     * Reads off a "keytab entry," which consists of a principal name,
     * principal type, key version number, and key material.
     */
    private KeytabEntry getKeytabEntry( ByteBuffer buffer ) throws IOException
    {
        String principalName = getPrincipalName( buffer );

        int principalType = buffer.getInt();

        long time = buffer.getInt();
        KerberosTime timeStamp = new KerberosTime( time * 1000 );

        byte keyVersion = buffer.get();

        EncryptionKey key = getKeyBlock( buffer );

        return new KeytabEntry( principalName, principalType, timeStamp, keyVersion, key );
    }


    /**
     * Reads off a principal name.
     *
     * @param buffer
     * @return The principal name.
     */
    private String getPrincipalName( ByteBuffer buffer ) throws IOException
    {
        int count = buffer.getShort();

        // decrement for v1
        String realm = getCountedString( buffer );

        StringBuilder principalNameBuffer = new StringBuilder();

        for ( int i = 0; i < count; i++ )
        {
            String nameComponent = getCountedString( buffer );

            principalNameBuffer.append( nameComponent );

            if ( i < count - 1 )
            {
                principalNameBuffer.append( "/" );
            }
        }

        principalNameBuffer.append( "@" ).append( realm );

        return principalNameBuffer.toString();
    }


    /**
     * Read off a 16-bit encryption type and symmetric key material.
     */
    private EncryptionKey getKeyBlock( ByteBuffer buffer ) throws IOException
    {
        int type = buffer.getShort();
        byte[] keyblock = getCountedBytes( buffer );

        EncryptionType encryptionType = EncryptionType.getTypeByValue( type );

        return new EncryptionKey( encryptionType, keyblock );
    }


    /**
     * Use a prefixed 16-bit length to read off a String.  Realm and name
     * components are ASCII encoded text with no zero terminator.
     */
    private String getCountedString( ByteBuffer buffer ) throws IOException
    {
        int length = buffer.getShort();
        
        if ( ( length < 0 ) || ( length > buffer.capacity() ) )
        {
            throw new IOException( "Invalid size for the keytab entry" );
        }

        byte[] data = new byte[length];
        buffer.get( data );

        try
        {
            return new String( data, "ASCII" );
        }
        catch ( UnsupportedEncodingException uee )
        {
            // Should never happen for ASCII
            return "";
        }
    }


    /**
     * Use a prefixed 16-bit length to read off raw bytes.
     */
    private byte[] getCountedBytes( ByteBuffer buffer ) throws IOException
    {
        int length = buffer.getShort();
        
        if ( ( length < 0 ) || ( length > buffer.capacity() ) )
        {
            throw new IOException( "Invalid size for the keytab entry" );
        }

        byte[] data = new byte[length];
        buffer.get( data );

        return data;
    }
}
