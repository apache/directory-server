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

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ResourceRecordEncoder implements RecordEncoder
{
    public void put( IoBuffer byteBuffer, ResourceRecord record ) throws IOException
    {
        putDomainName( byteBuffer, record.getDomainName() );
        putRecordType( byteBuffer, record.getRecordType() );
        putRecordClass( byteBuffer, record.getRecordClass() );

        byteBuffer.putInt( record.getTimeToLive() );

        putResourceRecord( byteBuffer, record );
    }


    protected abstract void putResourceRecordData( IoBuffer byteBuffer, ResourceRecord record );


    protected void putResourceRecord( IoBuffer byteBuffer, ResourceRecord record )
    {
        int startPosition = byteBuffer.position();
        byteBuffer.position( startPosition + 2 );

        putResourceRecordData( byteBuffer, record );

        putDataSize( byteBuffer, startPosition );
    }


    protected void putDataSize( IoBuffer byteBuffer, int startPosition )
    {
        int endPosition = byteBuffer.position();
        short length = ( short ) ( endPosition - startPosition - 2 );

        byteBuffer.position( startPosition );
        byteBuffer.putShort( length );
        byteBuffer.position( endPosition );
    }


    /**
     * <domain-name> is a domain name represented as a series of labels, and
     * terminated by a label with zero length.
     * 
     * @param byteBuffer the ByteBuffer to encode the domain name into
     * @param domainName the domain name to encode
     */
    protected void putDomainName( IoBuffer byteBuffer, String domainName )
    {
        if ( !Strings.isEmpty( domainName ) )
        {
            String[] labels = domainName.split( "\\." );
        

            for ( String label : labels )
            {
                byteBuffer.put( ( byte ) label.length() );
    
                char[] characters = label.toCharArray();
                
                for ( char c : characters )
                {
                    byteBuffer.put( ( byte ) c );
                }
            }
        }

        byteBuffer.put( ( byte ) 0x00 );
    }


    protected void putRecordType( IoBuffer byteBuffer, RecordType recordType )
    {
        byteBuffer.putShort( recordType.convert() );
    }


    protected void putRecordClass( IoBuffer byteBuffer, RecordClass recordClass )
    {
        byteBuffer.putShort( recordClass.convert() );
    }


    /**
     * <character-string> is a single length octet followed by that number
     * of characters.  <character-string> is treated as binary information,
     * and can be up to 256 characters in length (including the length octet).
     * 
     * @param byteBuffer The byte buffer to encode the character string into.
     * @param characterString the character string to encode
     */
    protected void putCharacterString( IoBuffer byteBuffer, String characterString )
    {
        byteBuffer.put( ( byte ) characterString.length() );

        char[] characters = characterString.toCharArray();

        for ( int ii = 0; ii < characters.length; ii++ )
        {
            byteBuffer.put( ( byte ) characters[ii] );
        }
    }
}
