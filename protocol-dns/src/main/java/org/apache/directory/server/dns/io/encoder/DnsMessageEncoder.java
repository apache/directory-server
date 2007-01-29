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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.protocol.DnsEncoder;
import org.apache.directory.server.dns.util.ByteBufferUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DnsMessageEncoder
{

    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DnsEncoder.class );

    /**
     * A Hashed Adapter mapping record types to their encoders.
     */
    private static final Map DEFAULT_ENCODERS;

    static
    {
        Map<RecordType, RecordEncoder> map = new HashMap<RecordType, RecordEncoder>();

        map.put( RecordType.SOA, new StartOfAuthorityRecordEncoder() );
        map.put( RecordType.A, new AddressRecordEncoder() );
        map.put( RecordType.NS, new NameServerRecordEncoder() );
        map.put( RecordType.CNAME, new CanonicalNameRecordEncoder() );
        map.put( RecordType.PTR, new PointerRecordEncoder() );
        map.put( RecordType.MX, new MailExchangeRecordEncoder() );
        map.put( RecordType.SRV, new ServerSelectionRecordEncoder() );
        map.put( RecordType.TXT, new TextRecordEncoder() );

        DEFAULT_ENCODERS = Collections.unmodifiableMap( map );
    }


    public void encode( ByteBuffer byteBuffer, DnsMessage message )
    {
        ByteBufferUtil.putUnsignedShort( byteBuffer, message.getTransactionId() );

        byte header = ( byte ) 0x00;
        header |= encodeMessageType( message.getMessageType() );
        header |= encodeOpCode( message.getOpCode() );
        header |= encodeAuthoritativeAnswer( message.isAuthoritativeAnswer() );
        header |= encodeTruncated( message.isTruncated() );
        header |= encodeRecursionDesired( message.isRecursionDesired() );
        byteBuffer.put( header );

        header = ( byte ) 0x00;
        header |= encodeRecursionAvailable( message.isRecursionAvailable() );
        header |= encodeResponseCode( message.getResponseCode() );
        byteBuffer.put( header );

        byteBuffer
            .putShort( ( short ) ( message.getQuestionRecords() != null ? message.getQuestionRecords().size() : 0 ) );
        byteBuffer.putShort( ( short ) ( message.getAnswerRecords() != null ? message.getAnswerRecords().size() : 0 ) );
        byteBuffer.putShort( ( short ) ( message.getAuthorityRecords() != null ? message.getAuthorityRecords().size()
            : 0 ) );
        byteBuffer.putShort( ( short ) ( message.getAdditionalRecords() != null ? message.getAdditionalRecords().size()
            : 0 ) );

        putQuestionRecords( byteBuffer, message.getQuestionRecords() );
        putResourceRecords( byteBuffer, message.getAnswerRecords() );
        putResourceRecords( byteBuffer, message.getAuthorityRecords() );
        putResourceRecords( byteBuffer, message.getAdditionalRecords() );
    }


    private void putQuestionRecords( ByteBuffer byteBuffer, List<QuestionRecord> questions )
    {
        if ( questions == null )
        {
            return;
        }

        QuestionRecordEncoder encoder = new QuestionRecordEncoder();

        Iterator it = questions.iterator();

        while ( it.hasNext() )
        {
            QuestionRecord question = ( QuestionRecord ) it.next();
            encoder.put( byteBuffer, question );
        }
    }


    private void putResourceRecords( ByteBuffer byteBuffer, List<ResourceRecord> records )
    {
        if ( records == null )
        {
            return;
        }

        Iterator it = records.iterator();

        while ( it.hasNext() )
        {
            ResourceRecord record = ( ResourceRecord ) it.next();

            try
            {
                put( byteBuffer, record );
            }
            catch ( IOException ioe )
            {
                log.error( ioe.getMessage(), ioe );
            }
        }
    }


    private void put( ByteBuffer byteBuffer, ResourceRecord record ) throws IOException
    {
        RecordType type = record.getRecordType();

        RecordEncoder encoder = ( RecordEncoder ) DEFAULT_ENCODERS.get( type );

        if ( encoder == null )
        {
            throw new IOException( "Encoder unavailable for " + type );
        }

        encoder.put( byteBuffer, record );
    }


    private byte encodeMessageType( MessageType messageType )
    {
        byte oneBit = ( byte ) ( messageType.convert() & 0x01 );
        return ( byte ) ( oneBit << 7 );
    }


    private byte encodeOpCode( OpCode opCode )
    {
        byte fourBits = ( byte ) ( opCode.convert() & 0x0F );
        return ( byte ) ( fourBits << 3 );
    }


    private byte encodeAuthoritativeAnswer( boolean authoritative )
    {
        if ( authoritative )
        {
            return ( byte ) ( ( byte ) 0x01 << 2 );
        }
        return ( byte ) 0;
    }


    private byte encodeTruncated( boolean truncated )
    {
        if ( truncated )
        {
            return ( byte ) ( ( byte ) 0x01 << 1 );
        }
        return 0;
    }


    private byte encodeRecursionDesired( boolean recursionDesired )
    {
        if ( recursionDesired )
        {
            return ( byte ) ( ( byte ) 0x01 );
        }
        return 0;
    }


    private byte encodeRecursionAvailable( boolean recursionAvailable )
    {
        if ( recursionAvailable )
        {
            return ( byte ) ( ( byte ) 0x01 << 7 );
        }
        return 0;
    }


    private byte encodeResponseCode( ResponseCode responseCode )
    {
        return ( byte ) ( responseCode.convert() & 0x0F );
    }

}
