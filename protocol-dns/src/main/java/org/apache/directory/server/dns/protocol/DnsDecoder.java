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

package org.apache.directory.server.dns.protocol;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dns.io.decoder.Decoder;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.QuestionRecords;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecordImpl;
import org.apache.directory.server.dns.messages.ResourceRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DnsDecoder implements ProtocolDecoder
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DnsDecoder.class );

    /**
     * A Hashed Adapter mapping record types to their encoders.
     */
    private static final Map DEFAULT_DECODERS;

    static
    {
        Map map = new HashMap();

        // map.put( RecordType.SOA, new StartOfAuthorityRecordEncoder() );

        DEFAULT_DECODERS = Collections.unmodifiableMap( map );
    }


    public void decode( IoSession session, ByteBuffer in, ProtocolDecoderOutput out )
    {
        out.write( decode( in ) );
    }


    DnsMessage decode( ByteBuffer in )
    {
        DnsMessageModifier modifier = new DnsMessageModifier();

        modifier.setTransactionId( in.getShort() );

        byte header = in.get();
        modifier.setMessageType( decodeMessageType( header ) );
        modifier.setOpCode( decodeOpCode( header ) );
        modifier.setAuthoritativeAnswer( decodeAuthoritativeAnswer( header ) );
        modifier.setTruncated( decodeTruncated( header ) );
        modifier.setRecursionDesired( decodeRecursionDesired( header ) );

        header = in.get();
        modifier.setRecursionAvailable( decodeRecursionAvailable( header ) );
        modifier.setResponseCode( decodeResponseCode( header ) );

        short questionCount = in.getShort();
        short answerCount = in.getShort();
        short authorityCount = in.getShort();
        short additionalCount = in.getShort();

        modifier.setQuestionRecords( decodeQuestions( questionCount, in ) );
        modifier.setAnswerRecords( decodeRecords( answerCount, in ) );
        modifier.setAuthorityRecords( decodeRecords( authorityCount, in ) );
        modifier.setAdditionalRecords( decodeRecords( additionalCount, in ) );

        return modifier.getDnsMessage();
    }


    private ResourceRecords decodeRecords( short recordCount, ByteBuffer byteBuffer )
    {
        ResourceRecords records = new ResourceRecords( recordCount );

        for ( int ii = 0; ii < recordCount; ii++ )
        {
            String domainName = decodeDomainName( byteBuffer );

            RecordType recordType = RecordType.getTypeByOrdinal( byteBuffer.getShort() );
            RecordClass recordClass = RecordClass.getTypeByOrdinal( byteBuffer.getShort() );

            int timeToLive = byteBuffer.getInt();
            short dataLength = byteBuffer.getShort();

            byte[] data = new byte[dataLength];
            byteBuffer.get( data );

            try
            {
                Map attributes = decode( recordType, data );
                records.add( new ResourceRecordImpl( domainName, recordType, recordClass, timeToLive, attributes ) );
            }
            catch ( IOException ioe )
            {
                log.error( ioe.getMessage(), ioe );
            }
        }

        return records;
    }


    private Map decode( RecordType type, byte[] resourceData ) throws IOException
    {
        Decoder decoder = ( Decoder ) DEFAULT_DECODERS.get( type );

        if ( decoder == null )
        {
            throw new IOException( "Decoder unavailable for " + type );
        }

        return decoder.decode( resourceData );
    }


    private QuestionRecords decodeQuestions( short questionCount, ByteBuffer byteBuffer )
    {
        QuestionRecords questions = new QuestionRecords( questionCount );

        for ( int ii = 0; ii < questionCount; ii++ )
        {
            String domainName = decodeDomainName( byteBuffer );

            RecordType recordType = RecordType.getTypeByOrdinal( byteBuffer.getShort() );
            RecordClass recordClass = RecordClass.getTypeByOrdinal( byteBuffer.getShort() );

            questions.add( new QuestionRecord( domainName, recordType, recordClass ) );
        }

        return questions;
    }


    private String decodeDomainName( ByteBuffer byteBuffer )
    {
        StringBuffer domainName = new StringBuffer();
        recurseDomainName( domainName, byteBuffer );

        return domainName.toString();
    }


    private void recurseDomainName( StringBuffer domainName, ByteBuffer byteBuffer )
    {
        byte currentByte = byteBuffer.get();

        boolean isCompressed = ( ( currentByte & ( byte ) 0xc0 ) == ( byte ) 0xc0 );
        boolean isLabelLength = ( ( currentByte != 0 ) && !isCompressed );

        if ( isCompressed )
        {
            int position = byteBuffer.get();
            int originalPosition = byteBuffer.position();
            byteBuffer.position( position );

            int labelLength = byteBuffer.get();
            getLabel( labelLength, byteBuffer, domainName );
            recurseDomainName( domainName, byteBuffer );

            byteBuffer.position( originalPosition );
        }

        if ( isLabelLength )
        {
            int labelLength = currentByte;
            getLabel( labelLength, byteBuffer, domainName );
            recurseDomainName( domainName, byteBuffer );
        }
    }


    private void getLabel( int labelLength, ByteBuffer byteBuffer, StringBuffer domainName )
    {
        for ( int jj = 0; jj < labelLength; jj++ )
        {
            char character = ( char ) byteBuffer.get();
            domainName.append( character );
        }

        if ( byteBuffer.get( byteBuffer.position() ) != 0 )
        {
            domainName.append( "." );
        }
    }


    private MessageType decodeMessageType( byte header )
    {
        return MessageType.getTypeByOrdinal( ( header & 0x80 ) >>> 7 );
    }


    private OpCode decodeOpCode( byte header )
    {
        return OpCode.getTypeByOrdinal( ( header & 0x78 ) >>> 3 );
    }


    private boolean decodeAuthoritativeAnswer( byte header )
    {
        return ( ( header & 0x04 ) >>> 2 ) == 1;
    }


    private boolean decodeTruncated( byte header )
    {
        return ( ( header & 0x02 ) >>> 1 ) == 1;
    }


    private boolean decodeRecursionDesired( byte header )
    {
        return ( ( header & 0x01 ) ) == 1;
    }


    private boolean decodeRecursionAvailable( byte header )
    {
        return ( ( header & 0x80 ) >>> 7 ) == 1;
    }


    private ResponseCode decodeResponseCode( byte header )
    {
        return ResponseCode.getTypeByOrdinal( header & 0x0F );
    }


    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}
