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
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


public class BuildReply extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        DnsContext dnsContext = ( DnsContext ) context;
        ResourceRecords records = dnsContext.getResourceRecords();
        DnsMessage request = dnsContext.getRequest();

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

        return CONTINUE_CHAIN;
    }
}
