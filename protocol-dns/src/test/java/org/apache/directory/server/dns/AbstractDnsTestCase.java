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
package org.apache.directory.server.dns;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.DnsAttribute;


public abstract class AbstractDnsTestCase extends TestCase
{
    protected static final int MINIMUM_DNS_DATAGRAM_SIZE = 576;


    protected ByteBuffer getTestQueryByteBuffer() throws IOException
    {
        return getByteBufferFromFile( "DNS-QUERY.pdu" );
    }
    
    
    protected ByteBuffer getTestResponseByteBuffer() throws IOException
    {
        return getByteBufferFromFile( "DNS-RESPONSE.pdu" );
    }
    
    
    protected ByteBuffer getTestMxQueryByteBuffer() throws IOException
    {
        return getByteBufferFromFile( "MX-QUERY.pdu" );
    }


    protected ByteBuffer getTestMxResponseByteBuffer() throws IOException
    {
        return getByteBufferFromFile( "MX-RESPONSE.pdu" );
    }

    
    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
    {
        InputStream is = getClass().getResourceAsStream( file );

        byte[] bytes = new byte[MINIMUM_DNS_DATAGRAM_SIZE];

        int offset = 0;
        int numRead = 0;
        while ( offset < bytes.length && ( numRead = is.read( bytes, offset, bytes.length - offset ) ) >= 0 )
        {
            offset += numRead;
        }

        is.close();

        return ByteBuffer.wrap( bytes );
    }
    
    
    protected DnsMessage getTestQuery() 
    {
        DnsMessageModifier modifier = new DnsMessageModifier();
        modifier.setTransactionId( ( short ) 27799 );
        modifier.setMessageType( MessageType.QUERY );
        modifier.setOpCode( OpCode.QUERY );
        modifier.setRecursionDesired( true );
        modifier.setQuestionRecords( Collections.singletonList( getTestQuestionRecord() ) );
        modifier.setResponseCode( ResponseCode.NO_ERROR );
        modifier.setAnswerRecords( new ArrayList<ResourceRecord>() );
        modifier.setAuthorityRecords( new ArrayList<ResourceRecord>() );
        modifier.setAdditionalRecords( new ArrayList<ResourceRecord>() );
        return modifier.getDnsMessage();
    }

    
    protected QuestionRecord getTestQuestionRecord() 
    {
        return new QuestionRecord( "www.example.com", RecordType.A, RecordClass.IN );
    }
    
    
    protected DnsMessage getTestMxQuery()
    {
        DnsMessageModifier modifier = new DnsMessageModifier();
        modifier.setTransactionId( 51511 );
        modifier.setMessageType( MessageType.QUERY );
        modifier.setOpCode( OpCode.QUERY );
        modifier.setRecursionDesired( true );
        modifier.setQuestionRecords( Collections.singletonList( getTestMxQuestionRecord() ) );
        modifier.setResponseCode( ResponseCode.NO_ERROR );
        modifier.setAnswerRecords( new ArrayList<ResourceRecord>() );
        modifier.setAuthorityRecords( new ArrayList<ResourceRecord>() );
        modifier.setAdditionalRecords( new ArrayList<ResourceRecord>() );
        return modifier.getDnsMessage();
    }
    

    protected QuestionRecord getTestMxQuestionRecord() 
    {
        return new QuestionRecord( "apache.org", RecordType.MX, RecordClass.IN );
    }
    
    protected DnsMessage getTestMxResponse() throws UnknownHostException
    {
        DnsMessageModifier modifier = new DnsMessageModifier();
        modifier.setTransactionId( 51511 );
        modifier.setMessageType( MessageType.RESPONSE );
        modifier.setOpCode( OpCode.QUERY );
        modifier.setRecursionDesired( true );
        modifier.setRecursionAvailable( true );
        modifier.setQuestionRecords( Collections.singletonList( getTestMxQuestionRecord() ) );
        modifier.setResponseCode( ResponseCode.NO_ERROR );
        modifier.setAnswerRecords( getTestMxAnswerRecords() );
        modifier.setAuthorityRecords( getTestMxAuthorityRecords() );
        modifier.setAdditionalRecords( getTestMxAdditionalRecords() );
        return modifier.getDnsMessage();
    }
    
    
    protected List<ResourceRecord> getTestMxAnswerRecords()
    {
        List<ResourceRecord> records = new ArrayList<ResourceRecord>();
        
        ResourceRecordModifier modifier = new ResourceRecordModifier();
        modifier.setDnsName ("apache.org");
        modifier.setDnsType (RecordType.MX);
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsTtl (267);
        modifier.put (DnsAttribute.MX_PREFERENCE, "10");
        modifier.put (DnsAttribute.DOMAIN_NAME, "herse.apache.org");
        records.add (modifier.getEntry ());
        
        modifier = new ResourceRecordModifier();
        modifier.setDnsName ("apache.org");
        modifier.setDnsType (RecordType.MX);
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsTtl (267);
        modifier.put (DnsAttribute.MX_PREFERENCE, "20");
        modifier.put (DnsAttribute.DOMAIN_NAME, "mail.apache.org");
        records.add (modifier.getEntry ());

        return records;
    }
    

    protected List<ResourceRecord> getTestMxAuthorityRecords()
    {
        List<ResourceRecord> records = new ArrayList<ResourceRecord>();

        ResourceRecordModifier modifier = new ResourceRecordModifier();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("apache.org");
        modifier.setDnsTtl (1932);
        modifier.setDnsType (RecordType.NS);
        modifier.put (DnsAttribute.DOMAIN_NAME, "ns.hyperreal.org");
        records.add (modifier.getEntry());

        modifier = new ResourceRecordModifier();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("apache.org");
        modifier.setDnsTtl (1932);
        modifier.setDnsType (RecordType.NS);
        modifier.put (DnsAttribute.DOMAIN_NAME, "ns1.eu.bitnames.com");
        records.add (modifier.getEntry());
        
        modifier = new ResourceRecordModifier();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("apache.org");
        modifier.setDnsTtl (1932);
        modifier.setDnsType (RecordType.NS);
        modifier.put (DnsAttribute.DOMAIN_NAME, "ns1.us.bitnames.com");
        records.add (modifier.getEntry());

        modifier = new ResourceRecordModifier();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("apache.org");
        modifier.setDnsTtl (1932);
        modifier.setDnsType (RecordType.NS);
        modifier.put (DnsAttribute.DOMAIN_NAME, "ns2.surfnet.nl");
        records.add (modifier.getEntry());

        return records;
    }


    protected List<ResourceRecord> getTestMxAdditionalRecords() throws UnknownHostException
    {
        List<ResourceRecord> records = new ArrayList<ResourceRecord>();
        
        ResourceRecordModifier modifier = new ResourceRecordModifier();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("herse.apache.org");
        modifier.setDnsTtl (3313);
        modifier.setDnsType (RecordType.A);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("140.211.11.133").toString ());
        records.add (modifier.getEntry());

        modifier = new ResourceRecordModifier ();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("mail.apache.org");
        modifier.setDnsTtl (3313);
        modifier.setDnsType (RecordType.A);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("140.211.11.2").toString ());
        records.add (modifier.getEntry ());
        
        modifier = new ResourceRecordModifier ();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("ns1.eu.bitnames.com");
        modifier.setDnsTtl (156234);
        modifier.setDnsType (RecordType.A);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("82.195.149.118").toString ());
        records.add (modifier.getEntry ());
        
        modifier = new ResourceRecordModifier ();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("ns1.us.bitnames.com");
        modifier.setDnsTtl (156236);
        modifier.setDnsType (RecordType.A);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("216.52.237.236").toString ());
        records.add (modifier.getEntry ());
        
        modifier = new ResourceRecordModifier ();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("ns2.surfnet.nl");
        modifier.setDnsTtl (77100);
        modifier.setDnsType (RecordType.A);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("192.87.36.2").toString ());
        records.add (modifier.getEntry ());

        modifier = new ResourceRecordModifier ();
        modifier.setDnsClass (RecordClass.IN);
        modifier.setDnsName ("ns2.surfnet.nl");
        modifier.setDnsTtl (77100);
        modifier.setDnsType (RecordType.AAAA);
        modifier.put (DnsAttribute.IP_ADDRESS, InetAddress.getByName ("2001:610:3:200a:192:87:36:2").toString ());
        records.add (modifier.getEntry ());

        return records;
    }

}
