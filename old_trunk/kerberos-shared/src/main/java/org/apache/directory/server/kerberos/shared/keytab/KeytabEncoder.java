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


import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.mina.common.ByteBuffer;


/**
 * Encode keytab fields into a {@link ByteBuffer}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class KeytabEncoder
{
    /**
     * Write the keytab version and entries into a {@link ByteBuffer}.
     *
     * @param keytabVersion
     * @param entries
     * @return The ByteBuffer.
     */
    ByteBuffer write( byte[] keytabVersion, List<KeytabEntry> entries )
    {
        ByteBuffer buffer = ByteBuffer.allocate( 512 );
        putKeytabVersion( buffer, keytabVersion );
        putKeytabEntries( buffer, entries );
        buffer.flip();

        return buffer;
    }


    /**
     * Encode the 16-bit file format version.  This
     * keytab reader currently only support verision 5.2.
     */
    private void putKeytabVersion( ByteBuffer buffer, byte[] version )
    {
        buffer.put( version );
    }


    /**
     * Encode the keytab entries.
     *
     * @param buffer
     * @param entries
     */
    private void putKeytabEntries( ByteBuffer buffer, List<KeytabEntry> entries )
    {
        Iterator<KeytabEntry> iterator = entries.iterator();

        while ( iterator.hasNext() )
        {
            ByteBuffer entryBuffer = putKeytabEntry( iterator.next() );
            int size = entryBuffer.position();

            entryBuffer.flip();

            buffer.putInt( size );
            buffer.put( entryBuffer );
        }
    }


    /**
     * Encode a "keytab entry," which consists of a principal name,
     * principal type, key version number, and key material.
     */
    private ByteBuffer putKeytabEntry( KeytabEntry entry )
    {
        ByteBuffer buffer = ByteBuffer.allocate( 100 );

        putPrincipalName( buffer, entry.getPrincipalName() );

        buffer.putInt( ( int ) entry.getPrincipalType() );

        buffer.putInt( ( int ) ( entry.getTimeStamp().getTime() / 1000 ) );

        buffer.put( entry.getKeyVersion() );

        putKeyBlock( buffer, entry.getKey() );

        return buffer;
    }


    /**
     * Encode a principal name.
     *
     * @param buffer
     * @param principalName
     */
    private void putPrincipalName( ByteBuffer buffer, String principalName )
    {
        String[] split = principalName.split( "@" );
        String nameComponent = split[0];
        String realm = split[1];

        String[] nameComponents = nameComponent.split( "/" );

        // increment for v1
        buffer.putShort( ( short ) nameComponents.length );

        putCountedString( buffer, realm );
        // write components

        for ( int ii = 0; ii < nameComponents.length; ii++ )
        {
            putCountedString( buffer, nameComponents[ii] );
        }
    }


    /**
     * Encode a 16-bit encryption type and symmetric key material.
     */
    private void putKeyBlock( ByteBuffer buffer, EncryptionKey key )
    {
        buffer.putShort( ( short ) key.getKeyType().getOrdinal() );
        putCountedBytes( buffer, key.getKeyValue() );
    }


    /**
     * Use a prefixed 16-bit length to encode a String.  Realm and name
     * components are ASCII encoded text with no zero terminator.
     */
    private void putCountedString( ByteBuffer buffer, String string )
    {
        byte[] data = string.getBytes();
        buffer.putShort( ( short ) data.length );
        buffer.put( data );
    }


    /**
     * Use a prefixed 16-bit length to encode raw bytes.
     */
    private void putCountedBytes( ByteBuffer buffer, byte[] data )
    {
        buffer.putShort( ( short ) data.length );
        buffer.put( data );
    }
}
