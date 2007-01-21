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


import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.ResourceRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


public class BuildReply implements IoHandlerCommand
{
    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        DnsContext dnsContext = (DnsContext) session.getAttribute( getContextKey() );
        ResourceRecords records = dnsContext.getResourceRecords();
        DnsMessage request = (DnsMessage) message;

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
        modifier.setAuthorityRecords( new ResourceRecords() );
        modifier.setAdditionalRecords( new ResourceRecords() );

        dnsContext.setReply( modifier.getDnsMessage() );

        next.execute( session, message );
    }

    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
