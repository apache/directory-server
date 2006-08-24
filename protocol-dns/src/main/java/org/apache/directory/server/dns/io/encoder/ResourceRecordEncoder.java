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

package org.apache.directory.server.dns.io.encoder;


import java.io.IOException;

import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.mina.common.ByteBuffer;


public abstract class ResourceRecordEncoder implements RecordEncoder
{
    protected abstract byte[] encodeResourceData( ResourceRecord record );


    public void encode( ByteBuffer out, ResourceRecord record ) throws IOException
    {
        encodeDomainName( out, record.getDomainName() );
        encodeRecordType( out, record.getRecordType() );
        encodeRecordClass( out, record.getRecordClass() );

        out.putInt( record.getTimeToLive() );

        byte[] resourceData = encodeResourceData( record );

        out.putShort( ( short ) resourceData.length );
        out.put( resourceData );
    }


    /**
     * <domain-name> is a domain name represented as a series of labels, and
     * terminated by a label with zero length.
     * @param domainName the domain name to encode
     * @return byte array of the encoded domain name
     */
    protected byte[] encodeDomainName( String domainName )
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );

        encodeDomainName( byteBuffer, domainName );

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get( bytes, 0, bytes.length );

        return bytes;
    }


    /**
     * <domain-name> is a domain name represented as a series of labels, and
     * terminated by a label with zero length.
     * @param byteBuffer the ByteBuffer to encode the domain name into
     * @param domainName the domain name to encode
     */
    protected void encodeDomainName( ByteBuffer byteBuffer, String domainName )
    {
        String[] labels = domainName.split( "\\." );

        for ( int ii = 0; ii < labels.length; ii++ )
        {
            byteBuffer.put( ( byte ) labels[ii].length() );

            char[] characters = labels[ii].toCharArray();
            for ( int jj = 0; jj < characters.length; jj++ )
            {
                byteBuffer.put( ( byte ) characters[jj] );
            }
        }

        byteBuffer.put( ( byte ) 0x00 );
    }


    protected void encodeRecordType( ByteBuffer byteBuffer, RecordType recordType )
    {
        byteBuffer.putShort( ( short ) recordType.getOrdinal() );
    }


    protected void encodeRecordClass( ByteBuffer byteBuffer, RecordClass recordClass )
    {
        byteBuffer.putShort( ( short ) recordClass.getOrdinal() );
    }


    /**
     * <character-string> is a single length octet followed by that number
     * of characters.  <character-string> is treated as binary information,
     * and can be up to 256 characters in length (including the length octet).
     * @param characterString the character string to encode
     * @return byte array of the encoded character string
     */
    protected byte[] encodeCharacterString( String characterString )
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );

        byteBuffer.put( ( byte ) characterString.length() );

        char[] characters = characterString.toCharArray();

        for ( int ii = 0; ii < characters.length; ii++ )
        {
            byteBuffer.put( ( byte ) characters[ii] );
        }

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get( bytes, 0, bytes.length );

        return bytes;
    }


    protected void putUnsignedByte( ByteBuffer byteBuffer, int value )
    {
        byteBuffer.put( ( byte ) ( value & 0xff ) );
    }


    protected void putUnsignedByte( ByteBuffer byteBuffer, int position, int value )
    {
        byteBuffer.put( position, ( byte ) ( value & 0xff ) );
    }


    protected void putUnsignedShort( ByteBuffer byteBuffer, int value )
    {
        byteBuffer.putShort( ( short ) ( value & 0xffff ) );
    }


    protected void putUnsignedShort( ByteBuffer byteBuffer, int position, int value )
    {
        byteBuffer.putShort( position, ( short ) ( value & 0xffff ) );
    }


    protected void putUnsignedInt( ByteBuffer byteBuffer, long value )
    {
        byteBuffer.putInt( ( int ) ( value & 0xffffffffL ) );
    }


    protected void putUnsignedInt( ByteBuffer byteBuffer, int position, long value )
    {
        byteBuffer.putInt( position, ( int ) ( value & 0xffffffffL ) );
    }
}
