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

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationContext;
import org.apache.directory.server.kerberos.kdc.authentication.AuthenticationService;
import org.apache.directory.server.kerberos.kdc.ticketgrant.TicketGrantingContext;
import org.apache.directory.server.kerberos.kdc.ticketgrant.TicketGrantingService;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Kerberos protocol handler for MINA which handles requests for the authentication
 * service and the ticket granting service of the KDC.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosProtocolHandler implements IoHandler
{
    /** The loggers for this class */
    private static final Logger LOG = LoggerFactory.getLogger( KerberosProtocolHandler.class );
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );

    /** The KDC server */
    private KdcServer kdcServer;

    /** The principal Name store */
    private PrincipalStore store;

    private static final String CONTEXT_KEY = "context";


    /**
     * Creates a new instance of KerberosProtocolHandler.
     *
     * @param kdcServer
     * @param store
     */
    public KerberosProtocolHandler( KdcServer kdcServer, PrincipalStore store )
    {
        this.kdcServer = kdcServer;
        this.store = store;
    }


    /**
     * {@inheritDoc}
     */
    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata() );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void sessionOpened( IoSession session )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} OPENED", session.getRemoteAddress() );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} OPENED", session.getRemoteAddress() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void sessionClosed( IoSession session )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} CLOSED", session.getRemoteAddress() );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} CLOSED", session.getRemoteAddress() );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        LOG.error( "{} EXCEPTION", session.getRemoteAddress(), cause );
        LOG_KRB.error( "{} EXCEPTION", session.getRemoteAddress(), cause );
        session.close( true );
    }


    /**
     * {@inheritDoc}
     */
    public void messageReceived( IoSession session, Object message )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} RCVD: {}", session.getRemoteAddress(), message );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} RCVD: {}", session.getRemoteAddress(), message );
        }

        InetAddress clientAddress = ( ( InetSocketAddress ) session.getRemoteAddress() ).getAddress();

        if ( !( message instanceof KdcReq ) )
        {
            LOG.error( I18n.err( I18n.ERR_152, ErrorType.KRB_AP_ERR_BADDIRECTION ) );
            LOG_KRB.error( I18n.err( I18n.ERR_152, ErrorType.KRB_AP_ERR_BADDIRECTION ) );

            session.write( getErrorMessage( kdcServer.getConfig().getServicePrincipal(), new KerberosException(
                ErrorType.KRB_AP_ERR_BADDIRECTION ) ) );
            return;
        }

        KdcReq request = ( KdcReq ) message;

        KerberosMessageType messageType = request.getMessageType();

        try
        {
            switch ( messageType )
            {
                case AS_REQ:
                    AuthenticationContext authContext = new AuthenticationContext();
                    authContext.setConfig( kdcServer.getConfig() );
                    authContext.setStore( store );
                    authContext.setClientAddress( clientAddress );
                    authContext.setRequest( request );
                    session.setAttribute( CONTEXT_KEY, authContext );

                    AuthenticationService.execute( authContext );

                    LOG_KRB.debug( "AuthenticationContext for AS_REQ : \n{}", authContext );

                    session.write( authContext.getReply() );
                    break;

                case TGS_REQ:
                    TicketGrantingContext tgsContext = new TicketGrantingContext();
                    tgsContext.setConfig( kdcServer.getConfig() );
                    tgsContext.setReplayCache( kdcServer.getReplayCache() );
                    tgsContext.setStore( store );
                    tgsContext.setClientAddress( clientAddress );
                    tgsContext.setRequest( request );
                    session.setAttribute( CONTEXT_KEY, tgsContext );

                    TicketGrantingService.execute( tgsContext );

                    LOG_KRB.debug( "TGSContext for TGS_REQ : \n {}", tgsContext );

                    session.write( tgsContext.getReply() );
                    break;

                case AS_REP:
                case TGS_REP:
                    throw new KerberosException( ErrorType.KRB_AP_ERR_BADDIRECTION );

                default:
                    throw new KerberosException( ErrorType.KRB_AP_ERR_MSG_TYPE );
            }
        }
        catch ( KerberosException ke )
        {
            String messageText = ke.getLocalizedMessage() + " (" + ke.getErrorCode() + ")";

            LOG.warn( messageText, ke );
            LOG_KRB.warn( messageText, ke );

            KrbError error = getErrorMessage( kdcServer.getConfig().getServicePrincipal(), ke );

            logErrorMessage( error );

            session.write( error );
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_152, e.getLocalizedMessage() ), e );
            LOG_KRB.error( I18n.err( I18n.ERR_152, e.getLocalizedMessage() ), e );

            session.write( getErrorMessage( kdcServer.getConfig().getServicePrincipal(), new KerberosException(
                ErrorType.KDC_ERR_SVC_UNAVAILABLE ) ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void messageSent( IoSession session, Object message )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            LOG_KRB.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
        }
    }


    /**
     * Construct an error message given some conditions
     * 
     * @param principal The Kerberos Principal
     * @param exception The Exception we've got
     * @return The resulting KrbError
     */
    protected KrbError getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        KrbError krbError = new KrbError();

        KerberosTime now = new KerberosTime();

        krbError.setErrorCode( ErrorType.getTypeByValue( exception.getErrorCode() ) );
        krbError.setEText( exception.getLocalizedMessage() );
        krbError.setSName( new PrincipalName( principal ) );
        krbError.setRealm( principal.getRealm() );
        krbError.setSTime( now );
        krbError.setSusec( 0 );
        krbError.setEData( exception.getExplanatoryData() );

        return krbError;
    }


    /**
     * Creates an explicit error message
     * The error we've get 
     * @param error
     */
    protected void logErrorMessage( KrbError error )
    {
        try
        {
            StringBuilder sb = new StringBuilder();

            sb.append( "Responding to request with error:" );
            sb.append( "\n\t" + "explanatory text:      " + error.getEText() );
            sb.append( "\n\t" + "error code:            " + error.getErrorCode() );
            sb.append( "\n\t" + "clientPrincipal:       " + error.getCName() ).append( "@" ).append( error.getCRealm() );
            sb.append( "\n\t" + "client time:           " + error.getCTime() );
            sb.append( "\n\t" + "serverPrincipal:       " + error.getSName() ).append( "@" ).append( error.getRealm() );
            sb.append( "\n\t" + "server time:           " + error.getSTime() );

            String message = sb.toString();

            LOG.debug( message );
            LOG_KRB.debug( message );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( I18n.err( I18n.ERR_155 ), e );
            LOG_KRB.error( I18n.err( I18n.ERR_155 ), e );
        }
    }
}
