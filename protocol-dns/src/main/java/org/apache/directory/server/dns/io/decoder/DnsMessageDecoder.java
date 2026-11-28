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

package org.apache.directory.server.dns.io.decoder;


import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordImpl;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A decoder for DNS messages.  The primary usage of the DnsMessageDecoder is by
 * calling the <code>decode(ByteBuffer)</code> method which will read the 
 * message from the incoming ByteBuffer and build a <code>DnsMessage</code>
 * from it according to the DnsMessage encoding in RFC-1035.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsMessageDecoder
{

    private final Logger logger = LoggerFactory.getLogger( DnsMessageDecoder.class );

    /** The minimum size of a DNS header in bytes (12 bytes). */
    private static final int DNS_HEADER_SIZE = 12;

    /** Maximum recursion depth for domain name parsing to prevent StackOverflowError. */
    private static final int MAX_DOMAIN_NAME_RECURSION = 100;

    /** Maximum label length in DNS (63 bytes). */
    private static final int MAX_LABEL_LENGTH = 63;

    /**
     * A Hashed Adapter mapping record types to their encoders.
     */
    private static final Map<RecordType, RecordDecoder> DEFAULT_DECODERS;

    static
    {
        EnumMap<RecordType, RecordDecoder> map = new EnumMap<>( RecordType.class );

        map.put( RecordType.A, new AddressRecordDecoder() );
        map.put( RecordType.NS, new NameServerRecordDecoder() );
        map.put( RecordType.MX, new MailExchangeRecordDecoder() );
        map.put( RecordType.AAAA, new IPv6RecordDecoder() );

        DEFAULT_DECODERS = Collections.unmodifiableMap( map );
    }


    /**
     * Decode the {@link IoBuffer} into a {@link DnsMessage}.
     *
     * @param in
     * @return The {@link DnsMessage}.
     * @throws IOException
     * @throws BufferUnderflowException if the buffer doesn't contain enough data
     */
    public DnsMessage decode( IoBuffer in ) throws IOException
    {
        if ( in.remaining() < DNS_HEADER_SIZE )
        {
            throw new BufferUnderflowException();
        }

        DnsMessageModifier modifier = new DnsMessageModifier();

        modifier.setTransactionId( in.getUnsignedShort() );

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

        // Validate record counts are non-negative
        if ( questionCount < 0 || answerCount < 0 || authorityCount < 0 || additionalCount < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_37012_NEGATIVE_RECORD_COUNT ) );
        }

        logger.debug( "decoding {} question records", questionCount );
        modifier.setQuestionRecords( getQuestions( in, questionCount ) );

        logger.debug( "decoding {} answer records", answerCount );
        modifier.setAnswerRecords( getRecords( in, answerCount ) );

        logger.debug( "decoding {} authority records", authorityCount );
        modifier.setAuthorityRecords( getRecords( in, authorityCount ) );

        logger.debug( "decoding {} additional records", additionalCount );
        modifier.setAdditionalRecords( getRecords( in, additionalCount ) );

        return modifier.getDnsMessage();
    }


    private List<ResourceRecord> getRecords( IoBuffer byteBuffer, short recordCount ) throws IOException
    {
        List<ResourceRecord> records = new ArrayList<>( recordCount );

        for ( int ii = 0; ii < recordCount; ii++ )
        {
            if ( byteBuffer.remaining() < 10 ) // Minimum: domain name (1 byte) + type (2) + class (2) + ttl (4) + length (2)
            {
                throw new BufferUnderflowException();
            }
            
            String domainName = getDomainName( byteBuffer );
            
            if ( byteBuffer.remaining() < 8 ) // type (2) + class (2) + ttl (4) + length (2)
            {
                throw new BufferUnderflowException();
            }
            
            RecordType recordType = RecordType.convert( byteBuffer.getShort() );
            RecordClass recordClass = RecordClass.convert( byteBuffer.getShort() );

            int timeToLive = byteBuffer.getInt();
            short dataLength = byteBuffer.getShort();

            if ( dataLength < 0 )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_37012_NEGATIVE_RECORD_COUNT ) );
            }
            
            if ( byteBuffer.remaining() < dataLength )
            {
                throw new BufferUnderflowException();
            }
            
            Map<String, Object> attributes = decode( byteBuffer, recordType, dataLength );
            records.add( new ResourceRecordImpl( domainName, recordType, recordClass, timeToLive, attributes ) );
        }

        return records;
    }


    private Map<String, Object> decode( IoBuffer byteBuffer, RecordType type, short length ) throws IOException
    {
        RecordDecoder recordDecoder = DEFAULT_DECODERS.get( type );

        if ( recordDecoder == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_37006_DECODER_UNAVAILABLE, type ) );
        }

        return recordDecoder.decode( byteBuffer, length );
    }


    private List<QuestionRecord> getQuestions( IoBuffer byteBuffer, short questionCount )
    {
        List<QuestionRecord> questions = new ArrayList<>( questionCount );

        for ( int ii = 0; ii < questionCount; ii++ )
        {
            if ( byteBuffer.remaining() < 1 ) // Minimum: domain name (1 byte for empty label)
            {
                throw new BufferUnderflowException();
            }
            
            String domainName = getDomainName( byteBuffer );

            if ( byteBuffer.remaining() < 4 ) // type (2) + class (2)
            {
                throw new BufferUnderflowException();
            }
            
            RecordType recordType = RecordType.convert( byteBuffer.getShort() );
            RecordClass recordClass = RecordClass.convert( byteBuffer.getShort() );

            questions.add( new QuestionRecord( domainName, recordType, recordClass ) );
        }

        return questions;
    }


    static String getDomainName( IoBuffer byteBuffer )
    {
        StringBuilder domainName = new StringBuilder();
        recurseDomainName( byteBuffer, domainName, 0, new java.util.HashSet<>() );

        return domainName.toString();
    }


    static void recurseDomainName( IoBuffer byteBuffer, StringBuilder domainName, int recursionDepth, java.util.Set<Integer> visitedPositions )
    {
        // Prevent infinite recursion / StackOverflowError
        if ( recursionDepth > MAX_DOMAIN_NAME_RECURSION )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_37013_DOMAIN_NAME_TOO_DEEP ) );
        }
        
        if ( !byteBuffer.hasRemaining() )
        {
            throw new BufferUnderflowException();
        }
        
        int currentPosition = byteBuffer.position();
        
        // Check for circular reference
        if ( visitedPositions.contains( currentPosition ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_37016_CIRCULAR_DOMAIN_NAME_REFERENCE ) );
        }
        
        int length = byteBuffer.getUnsigned();

        if ( isOffset( length ) )
        {
            if ( !byteBuffer.hasRemaining() )
            {
                throw new BufferUnderflowException();
            }
            
            int position = byteBuffer.getUnsigned();
            int offset = ( length & ~( 0xc0 ) ) << 8;
            int originalPosition = byteBuffer.position();
            
            // Validate offset is within bounds
            int targetPosition = position + offset;
            if ( targetPosition < 0 || targetPosition > byteBuffer.limit() )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_37014_INVALID_DOMAIN_NAME_OFFSET ) );
            }
            
            // Add current position to visited set before jumping
            visitedPositions.add( currentPosition );
            
            byteBuffer.position( targetPosition );

            recurseDomainName( byteBuffer, domainName, recursionDepth + 1, visitedPositions );

            byteBuffer.position( originalPosition );
        }
        else if ( isLabel( length ) )
        {
            // Validate label length (DNS max is 63 bytes)
            if ( length > MAX_LABEL_LENGTH )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_37015_INVALID_LABEL_LENGTH, length ) );
            }
            
            if ( byteBuffer.remaining() < length )
            {
                throw new BufferUnderflowException();
            }
            
            int labelLength = length;
            getLabel( byteBuffer, domainName, labelLength );
            
            // Check if we have at least one more byte for the next label or null terminator
            if ( !byteBuffer.hasRemaining() )
            {
                throw new BufferUnderflowException();
            }
            
            recurseDomainName( byteBuffer, domainName, recursionDepth + 1, visitedPositions );
        }
    }


    static boolean isOffset( int length )
    {
        return ( ( length & 0xc0 ) == 0xc0 );
    }


    static boolean isLabel( int length )
    {
        return ( length != 0 && ( length & 0xc0 ) == 0 );
    }


    static void getLabel( IoBuffer byteBuffer, StringBuilder domainName, int labelLength )
    {
        for ( int jj = 0; jj < labelLength; jj++ )
        {
            char character = ( char ) byteBuffer.get();
            domainName.append( character );
        }

        if ( byteBuffer.hasRemaining() && byteBuffer.get( byteBuffer.position() ) != 0 )
        {
            domainName.append( "." );
        }
    }


    private MessageType decodeMessageType( byte header )
    {
        return MessageType.convert( ( byte ) ( ( header & 0x80 ) >>> 7 ) );
    }


    private OpCode decodeOpCode( byte header )
    {
        return OpCode.convert( ( byte ) ( ( header & 0x78 ) >>> 3 ) );
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
        return ( header & 0x01 ) == 1;
    }


    private boolean decodeRecursionAvailable( byte header )
    {
        return ( ( header & 0x80 ) >>> 7 ) == 1;
    }


    private ResponseCode decodeResponseCode( byte header )
    {
        return ResponseCode.convert( ( byte ) ( header & 0x0F ) );
    }
}
