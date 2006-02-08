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
package org.apache.directory.server.dns.service;

import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.ResourceRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.Filter;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsExceptionHandler extends CommandBase implements Filter
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DnsExceptionHandler.class );

    public boolean execute( Context context ) throws Exception
    {
        return CONTINUE_CHAIN;
    }

    public boolean postprocess( Context context, Exception exception )
    {
        if ( exception == null )
        {
            return CONTINUE_CHAIN;
        }

        DnsContext dnsContext = (DnsContext) context;
        DnsException de = (DnsException) exception;
        DnsMessage message = dnsContext.getRequest();

        if ( log.isDebugEnabled() )
        {
            log.debug( de.getMessage(), de );
        }
        else
        {
            log.info( de.getMessage() );
        }

        DnsMessageModifier modifier = new DnsMessageModifier();

        modifier.setTransactionId( message.getTransactionId() );
        modifier.setMessageType( MessageType.RESPONSE );
        modifier.setOpCode( OpCode.QUERY );
        modifier.setAuthoritativeAnswer( false );
        modifier.setTruncated( false );
        modifier.setRecursionDesired( message.isRecursionDesired() );
        modifier.setRecursionAvailable( false );
        modifier.setReserved( false );
        modifier.setAcceptNonAuthenticatedData( false );
        modifier.setResponseCode( ResponseCode.getTypeByOrdinal( de.getResponseCode() ) );
        modifier.setQuestionRecords( message.getQuestionRecords() );
        modifier.setAnswerRecords( new ResourceRecords() );
        modifier.setAuthorityRecords( new ResourceRecords() );
        modifier.setAdditionalRecords( new ResourceRecords() );

        dnsContext.setReply( modifier.getDnsMessage() );

        return STOP_CHAIN;
    }
}
