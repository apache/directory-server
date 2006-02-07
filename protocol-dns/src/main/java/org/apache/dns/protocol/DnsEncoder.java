/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.dns.protocol;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.dns.io.encoder.AddressRecordEncoder;
import org.apache.dns.io.encoder.CanonicalNameRecordEncoder;
import org.apache.dns.io.encoder.MailExchangeRecordEncoder;
import org.apache.dns.io.encoder.NameServerRecordEncoder;
import org.apache.dns.io.encoder.PointerRecordEncoder;
import org.apache.dns.io.encoder.QuestionRecordEncoder;
import org.apache.dns.io.encoder.RecordEncoder;
import org.apache.dns.io.encoder.ServerSelectionRecordEncoder;
import org.apache.dns.io.encoder.StartOfAuthorityRecordEncoder;
import org.apache.dns.io.encoder.TextRecordEncoder;
import org.apache.dns.messages.DnsMessage;
import org.apache.dns.messages.MessageType;
import org.apache.dns.messages.OpCode;
import org.apache.dns.messages.QuestionRecord;
import org.apache.dns.messages.QuestionRecords;
import org.apache.dns.messages.RecordType;
import org.apache.dns.messages.ResourceRecord;
import org.apache.dns.messages.ResourceRecords;
import org.apache.dns.messages.ResponseCode;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsEncoder implements ProtocolEncoder
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DnsEncoder.class );

    /**
     * A Hashed Adapter mapping record types to their encoders.
     */
    private static final Map DEFAULT_ENCODERS;

    static
    {
        Map map = new HashMap();

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

    public void encode( IoSession session, Object message, ProtocolEncoderOutput out )
    {
        ByteBuffer buf = ByteBuffer.allocate( 1024 );
        encode( buf, (DnsMessage) message );

        buf.flip();

        out.write( buf );
    }

    public void encode( ByteBuffer byteBuffer, DnsMessage message )
    {
        byteBuffer.putShort( message.getTransactionId() );

        byte header = (byte) 0x00;
        header = encodeMessageType( message.getMessageType(), header );
        header = encodeOpCode( message.getOpCode(), header );
        header = encodeAuthoritativeAnswer( message.isAuthoritativeAnswer(), header );
        header = encodeTruncated( message.isTruncated(), header );
        header = encodeRecursionDesired( message.isRecursionDesired(), header );
        byteBuffer.put( header );

        header = (byte) 0x00;
        header = encodeRecursionAvailable( message.isRecursionAvailable(), header );
        header = encodeResponseCode( message.getResponseCode(), header );
        byteBuffer.put( header );

        byteBuffer.putShort( (short) message.getQuestionRecords().size() );
        byteBuffer.putShort( (short) message.getAnswerRecords().size() );
        byteBuffer.putShort( (short) message.getAuthorityRecords().size() );
        byteBuffer.putShort( (short) message.getAdditionalRecords().size() );

        encodeRecords( message.getQuestionRecords(), byteBuffer );
        encodeRecords( message.getAnswerRecords(), byteBuffer );
        encodeRecords( message.getAuthorityRecords(), byteBuffer );
        encodeRecords( message.getAdditionalRecords(), byteBuffer );
    }

    private void encodeRecords( QuestionRecords questions, ByteBuffer byteBuffer )
    {
        QuestionRecordEncoder encoder = new QuestionRecordEncoder();

        Iterator it = questions.iterator();

        while ( it.hasNext() )
        {
            QuestionRecord question = (QuestionRecord) it.next();
            encoder.encode( byteBuffer, question );
        }
    }

    private void encodeRecords( ResourceRecords records, ByteBuffer byteBuffer )
    {
        Iterator it = records.iterator();

        while ( it.hasNext() )
        {
            ResourceRecord record = (ResourceRecord) it.next();

            try
            {
                encode( byteBuffer, record );
            }
            catch ( IOException ioe )
            {
                log.error( ioe.getMessage(), ioe );
            }
        }
    }

    private void encode( ByteBuffer out, ResourceRecord record ) throws IOException
    {
        RecordType type = record.getRecordType();

        RecordEncoder encoder = (RecordEncoder) DEFAULT_ENCODERS.get( type );

        if ( encoder == null )
        {
            throw new IOException( "Encoder unavailable for " + type );
        }

        encoder.encode( out, record );
    }

    private byte encodeMessageType( MessageType messageType, byte header )
    {
        byte oneBit = (byte) ( messageType.getOrdinal() & 0x01 );
        return (byte) ( ( oneBit << 7 ) | header );
    }

    private byte encodeOpCode( OpCode opCode, byte header )
    {
        byte fourBits = (byte) ( opCode.getOrdinal() & 0x0F );
        return (byte) ( ( fourBits << 3 ) | header );
    }

    private byte encodeAuthoritativeAnswer( boolean authoritative, byte header )
    {
        if ( authoritative )
        {
            header = (byte) ( ( (byte) 0x01 << 2 ) | header );
        }
        return header;
    }

    private byte encodeTruncated( boolean truncated, byte header )
    {
        if ( truncated )
        {
            header = (byte) ( ( (byte) 0x01 << 1 ) | header );
        }
        return header;
    }

    private byte encodeRecursionDesired( boolean recursionDesired, byte header )
    {
        if ( recursionDesired )
        {
            header = (byte) ( ( (byte) 0x01 ) | header );
        }
        return header;
    }

    private byte encodeRecursionAvailable( boolean recursionAvailable, byte header )
    {
        if ( recursionAvailable )
        {
            header = (byte) ( ( (byte) 0x01 << 7 ) | header );
        }
        return header;
    }

    private byte encodeResponseCode( ResponseCode responseCode, byte header )
    {
        byte fourBits = (byte) ( responseCode.getOrdinal() & 0x0F );
        return (byte) ( fourBits | header );
    }

    public void dispose( IoSession arg0 ) throws Exception
    {
    }
}
