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


import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class QuestionRecordEncoder
{
    /**
     * Encodes the {@link QuestionRecord} into the {@link IoBuffer}.
     *
     * @param out
     * @param question
     */
    public void put( IoBuffer out, QuestionRecord question )
    {
        encodeDomainName( out, question.getDomainName() );
        encodeRecordType( out, question.getRecordType() );
        encodeRecordClass( out, question.getRecordClass() );
    }


    private void encodeDomainName( IoBuffer byteBuffer, String domainName )
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


    private void encodeRecordType( IoBuffer byteBuffer, RecordType recordType )
    {
        byteBuffer.putShort( recordType.convert() );
    }


    private void encodeRecordClass( IoBuffer byteBuffer, RecordClass recordClass )
    {
        byteBuffer.putShort( recordClass.convert() );
    }
}
