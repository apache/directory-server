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
package org.apache.directory.server.kerberos.protocol;


import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationContext;
import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationServiceChain;
import org.apache.directory.server.kerberos.kdc.ticketgrant.TicketGrantingContext;
import org.apache.directory.server.kerberos.kdc.ticketgrant.TicketGrantingServiceChain;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.KerberosError;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Kerberos protocol handler for MINA which handles requests for the authentication
 * service and the ticket granting service of the KDC.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosProtocolHandler implements IoHandler
{
    private static final Logger log = LoggerFactory.getLogger( KerberosProtocolHandler.class );

    private KdcConfiguration config;
    private PrincipalStore store;
    private IoHandlerCommand authService;
    private IoHandlerCommand tgsService;
    private String contextKey = "context";


    /**
     * Creates a new instance of KerberosProtocolHandler.
     *
     * @param config
     * @param store
     */
    public KerberosProtocolHandler( KdcConfiguration config, PrincipalStore store )
    {
        this.config = config;
        this.store = store;

        authService = new AuthenticationServiceChain();
        tgsService = new TicketGrantingServiceChain();
    }


    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportType() );
        }

        if ( session.getTransportType() == TransportType.DATAGRAM )
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( KerberosUdpProtocolCodecFactory.getInstance() ) );
        }
        else
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( KerberosTcpProtocolCodecFactory.getInstance() ) );
        }
    }


    public void sessionOpened( IoSession session )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} OPENED", session.getRemoteAddress() );
        }
    }


    public void sessionClosed( IoSession session )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} CLOSED", session.getRemoteAddress() );
        }
    }


    public void sessionIdle( IoSession session, IdleStatus status )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
        }
    }


    public void exceptionCaught( IoSession session, Throwable cause )
    {
        log.error( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close();
    }


    public void messageReceived( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );
        }

        InetAddress clientAddress = ( ( InetSocketAddress ) session.getRemoteAddress() ).getAddress();
        KdcRequest request = ( KdcRequest ) message;

        int messageType = request.getMessageType().getOrdinal();

        try
        {
            switch ( messageType )
            {
                case 10:
                    AuthenticationContext authContext = new AuthenticationContext();
                    authContext.setConfig( config );
                    authContext.setStore( store );
                    authContext.setClientAddress( clientAddress );
                    authContext.setRequest( request );
                    session.setAttribute( getContextKey(), authContext );

                    authService.execute( null, session, message );

                    session.write( authContext.getReply() );
                    break;

                case 12:
                    TicketGrantingContext tgsContext = new TicketGrantingContext();
                    tgsContext.setConfig( config );
                    tgsContext.setStore( store );
                    tgsContext.setClientAddress( clientAddress );
                    tgsContext.setRequest( request );
                    session.setAttribute( getContextKey(), tgsContext );

                    tgsService.execute( null, session, message );

                    session.write( tgsContext.getReply() );
                    break;

                case 11:
                case 13:
                    throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADDIRECTION );

                default:
                    throw new KerberosException( KerberosErrorType.KRB_AP_ERR_MSG_TYPE );
            }
        }
        catch ( KerberosException ke )
        {
            if ( log.isDebugEnabled() )
            {
                log.warn( ke.getMessage(), ke );
            }
            else
            {
                log.warn( ke.getMessage() );
            }

            session.write( getErrorMessage( config.getServicePrincipal(), ke ) );
        }
        catch ( Exception e )
        {
            log.error( "Unexpected exception:  " + e.getMessage(), e );

            session.write( getErrorMessage( config.getServicePrincipal(), new KerberosException(
                KerberosErrorType.KDC_ERR_SVC_UNAVAILABLE ) ) );
        }
    }


    public void messageSent( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
        }
    }


    protected KerberosError getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        KerberosError kerberosError = new KerberosError();

        KerberosTime now = new KerberosTime();

        kerberosError.setErrorCode( KerberosErrorType.getTypeByOrdinal( exception.getErrorCode() ) );
        kerberosError.setExplanatoryText( exception.getMessage() );
        kerberosError.setServerPrincipal( principal );
        kerberosError.setServerTime( now );
        kerberosError.setServerMicroseconds( 0 );
        kerberosError.setExplanatoryData( exception.getExplanatoryData() );

        return kerberosError;
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
