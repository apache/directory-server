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
package org.apache.directory.server.dns.service;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.RecordStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Domain Name Service (DNS) Protocol (RFC 1034, 1035)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DomainNameService
{
    /** the log for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DomainNameService.class );


    /**
     * Creates a new instance of DomainNameService.
     */
    public static void execute( DnsContext dnsContext, DnsMessage request ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            monitorRequest( request );
        }

        getResourceRecords( dnsContext, request );

        if ( LOG.isDebugEnabled() )
        {
            monitorContext( dnsContext );
        }

        buildReply( dnsContext, request );

        if ( LOG.isDebugEnabled() )
        {
            monitorReply( dnsContext );
        }
    }
    
    private static void monitorRequest( DnsMessage request ) throws Exception
    {
        try
        {
            LOG.debug( monitorMessage( request, "request" ) );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in request monitor", e );
        }
    }
    

    private static void getResourceRecords( DnsContext dnsContext, DnsMessage request ) throws Exception
    {
        RecordStore store = dnsContext.getStore();

        List<QuestionRecord> questions = request.getQuestionRecords();

        Iterator<QuestionRecord> it = questions.iterator();

        while ( it.hasNext() )
        {
            dnsContext.addResourceRecords( getEntry( store, it.next() ) );
        }
    }
    
    
    /**
     * Returns a set of {@link ResourceRecord}s from a {@link RecordStore}, given a DNS {@link QuestionRecord}.
     *
     * @param store
     * @param question
     * @return The set of {@link ResourceRecord}s.
     * @throws DNSException
     */
    private static Set<ResourceRecord> getEntry( RecordStore store, QuestionRecord question ) throws DnsException
    {
        Set<ResourceRecord> records = null;

        records = store.getRecords( question );

        if ( records == null || records.isEmpty() )
        {
            LOG.debug( "The domain name referenced in the query does not exist." );

            throw new DnsException( ResponseCode.NAME_ERROR );
        }

        return records;
    }
    
    
    private static void monitorContext( DnsContext dnsContext ) throws Exception
    {
        try
        {
            RecordStore store = dnsContext.getStore();
            List<ResourceRecord> records = dnsContext.getResourceRecords();

            StringBuffer sb = new StringBuffer();
            sb.append( "Monitoring context:" );
            sb.append( "\n\t" + "store:                     " + store );
            sb.append( "\n\t" + "records:                   " + records );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in context monitor", e );
        }
    }
    
    
    private static void buildReply( DnsContext dnsContext, DnsMessage request ) throws Exception
    {
        List<ResourceRecord> records = dnsContext.getResourceRecords();

        DnsMessageModifier modifier = new DnsMessageModifier();

        modifier.setTransactionId( request.getTransactionId() );
        modifier.setMessageType( MessageType.RESPONSE );
        modifier.setOpCode( OpCode.QUERY );
        modifier.setAuthoritativeAnswer( false );
        modifier.setTruncated( false );
        modifier.setRecursionDesired( request.isRecursionDesired() );
        modifier.setRecursionAvailable( false );
        modifier.setReserved( false );
        modifier.setAcceptNonAuthenticatedData( false );
        modifier.setResponseCode( ResponseCode.NO_ERROR );
        modifier.setQuestionRecords( request.getQuestionRecords() );

        modifier.setAnswerRecords( records );
        modifier.setAuthorityRecords( new ArrayList<ResourceRecord>() );
        modifier.setAdditionalRecords( new ArrayList<ResourceRecord>() );

        dnsContext.setReply( modifier.getDnsMessage() );
    }
 
    
    private static void monitorReply( DnsContext dnsContext ) throws Exception
    {
        try
        {
            DnsMessage reply = dnsContext.getReply();

            LOG.debug( monitorMessage( reply, "reply" ) );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in reply monitor", e );
        }
    }
    
    
    private static String monitorMessage( DnsMessage message, String direction )
    {
        MessageType messageType = message.getMessageType();
        OpCode opCode = message.getOpCode();
        ResponseCode responseCode = message.getResponseCode();
        int transactionId = message.getTransactionId();

        StringBuffer sb = new StringBuffer();
        sb.append( "Monitoring " + direction + ":" );
        sb.append( "\n\t" + "messageType                " + messageType );
        sb.append( "\n\t" + "opCode                     " + opCode );
        sb.append( "\n\t" + "responseCode               " + responseCode );
        sb.append( "\n\t" + "transactionId              " + transactionId );

        sb.append( "\n\t" + "authoritativeAnswer        " + message.isAuthoritativeAnswer() );
        sb.append( "\n\t" + "truncated                  " + message.isTruncated() );
        sb.append( "\n\t" + "recursionDesired           " + message.isRecursionDesired() );
        sb.append( "\n\t" + "recursionAvailable         " + message.isRecursionAvailable() );
        sb.append( "\n\t" + "reserved                   " + message.isReserved() );
        sb.append( "\n\t" + "acceptNonAuthenticatedData " + message.isAcceptNonAuthenticatedData() );

        List<QuestionRecord> questions = message.getQuestionRecords();

        sb.append( "\n\t" + "questions:                 " + questions );

        return sb.toString();
    }
}
