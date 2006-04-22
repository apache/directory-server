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

package org.apache.directory.server.dns.protocol;


import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.ResourceRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.service.DnsContext;
import org.apache.directory.server.dns.service.DomainNameServiceChain;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DnsProtocolHandler implements IoHandler
{
    private static final Logger log = LoggerFactory.getLogger( DnsProtocolHandler.class );

    private DnsConfiguration config;
    private RecordStore store;
    private IoHandlerCommand dnsService;
    private String contextKey = "context";


    public DnsProtocolHandler(DnsConfiguration config, RecordStore store)
    {
        this.config = config;
        this.store = store;

        dnsService = new DomainNameServiceChain();
    }


    public void sessionCreated( IoSession session ) throws Exception
    {
        log.debug( "{} CREATED", session.getRemoteAddress() );

        session.getFilterChain().addFirst( "codec", new ProtocolCodecFilter( DnsProtocolCodecFactory.getInstance() ) );
    }


    public void sessionOpened( IoSession session )
    {
        log.debug( "{} OPENED", session.getRemoteAddress() );
    }


    public void sessionClosed( IoSession session )
    {
        log.debug( "{} CLOSED", session.getRemoteAddress() );
    }


    public void sessionIdle( IoSession session, IdleStatus status )
    {
        log.debug( "{} IDLE({})", session.getRemoteAddress(), status );
    }


    public void exceptionCaught( IoSession session, Throwable cause )
    {
        log.error( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close();
    }


    public void messageReceived( IoSession session, Object message )
    {
        log.debug( "{} RCVD: {}", session.getRemoteAddress(), message );

        try
        {
            DnsContext dnsContext = new DnsContext();
            dnsContext.setConfig( config );
            dnsContext.setStore( store );
            session.setAttribute( getContextKey(), dnsContext );

            dnsService.execute( null, session, message );

            DnsMessage response = dnsContext.getReply();

            session.write( response );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );

            DnsMessage request = (DnsMessage) message;
            DnsException de = (DnsException) e;

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
            modifier.setResponseCode( ResponseCode.getTypeByOrdinal( de.getResponseCode() ) );
            modifier.setQuestionRecords( request.getQuestionRecords() );
            modifier.setAnswerRecords( new ResourceRecords() );
            modifier.setAuthorityRecords( new ResourceRecords() );
            modifier.setAdditionalRecords( new ResourceRecords() );

            session.write( modifier.getDnsMessage() );
        }
    }


    public void messageSent( IoSession session, Object message )
    {
        log.debug( "{} SENT: {}", session.getRemoteAddress(), message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
