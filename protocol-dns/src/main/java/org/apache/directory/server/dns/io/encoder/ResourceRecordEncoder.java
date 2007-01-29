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
import java.nio.ByteBuffer;

import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;


public abstract class ResourceRecordEncoder implements RecordEncoder
{
    public void put( ByteBuffer byteBuffer, ResourceRecord record ) throws IOException
    {
        putDomainName( byteBuffer, record.getDomainName() );
        putRecordType( byteBuffer, record.getRecordType() );
        putRecordClass( byteBuffer, record.getRecordClass() );

        byteBuffer.putInt( record.getTimeToLive() );

        putResourceRecord( byteBuffer, record );
    }


    protected abstract void putResourceRecordData( ByteBuffer byteBuffer, ResourceRecord record );


    protected void putResourceRecord( ByteBuffer byteBuffer, ResourceRecord record )
    {
        int startPosition = prepareForSizedData( byteBuffer );

        putResourceRecordData( byteBuffer, record );

        putDataSize( byteBuffer, startPosition );
    }


    protected int prepareForSizedData( ByteBuffer byteBuffer )
    {
        int startPosition = byteBuffer.position();
        byteBuffer.position( startPosition + 1 );
        return startPosition;
    }


    protected void putDataSize( ByteBuffer byteBuffer, int startPosition )
    {
        byte length = ( byte ) ( byteBuffer.position() - startPosition + 1 );
        byteBuffer.position( startPosition );
        byteBuffer.put( length );
        byteBuffer.position( startPosition + 1 + length );
    }


    /**
     * <domain-name> is a domain name represented as a series of labels, and
     * terminated by a label with zero length.
     * @param byteBuffer the ByteBuffer to encode the domain name into
     * @param domainName the domain name to encode
     */
    protected void putDomainName( ByteBuffer byteBuffer, String domainName )
    {
        int startPosition = prepareForSizedData( byteBuffer );

        putDomainNameData( byteBuffer, domainName );

        putDataSize( byteBuffer, startPosition );
    }


    protected void putDomainNameData( ByteBuffer byteBuffer, String domainName )
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


    protected void putRecordType( ByteBuffer byteBuffer, RecordType recordType )
    {
        byteBuffer.putShort( recordType.convert() );
    }


    protected void putRecordClass( ByteBuffer byteBuffer, RecordClass recordClass )
    {
        byteBuffer.putShort( ( short ) recordClass.convert() );
    }


    /**
     * <character-string> is a single length octet followed by that number
     * of characters.  <character-string> is treated as binary information,
     * and can be up to 256 characters in length (including the length octet).
     * @param characterString the character string to encode
     * @return byte array of the encoded character string
     */
    protected void putCharacterString( ByteBuffer byteBuffer, String characterString )
    {
        byteBuffer.put( ( byte ) characterString.length() );

        char[] characters = characterString.toCharArray();

        for ( int ii = 0; ii < characters.length; ii++ )
        {
            byteBuffer.put( ( byte ) characters[ii] );
        }
    }

}
