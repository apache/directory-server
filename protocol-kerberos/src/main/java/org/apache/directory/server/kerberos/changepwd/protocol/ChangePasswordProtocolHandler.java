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

package org.apache.directory.server.kerberos.changepwd.protocol;


import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.changepwd.ChangePasswordServer;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordError;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;
import org.apache.directory.server.kerberos.changepwd.service.ChangePasswordContext;
import org.apache.directory.server.kerberos.changepwd.service.ChangePasswordService;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordProtocolHandler extends IoHandlerAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( ChangePasswordProtocolHandler.class );

    private ChangePasswordServer server;
    private PrincipalStore store;
    private String contextKey = "context";


    /**
     * Creates a new instance of ChangePasswordProtocolHandler.
     *
     * @param config The ChangePassword server configuration
     * @param store The Principal store
     */
    public ChangePasswordProtocolHandler( ChangePasswordServer config, PrincipalStore store )
    {
        this.server = config;
        this.store = store;
    }


    @Override
    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata() );
        }

        session.getFilterChain().addFirst( "codec",
            new ProtocolCodecFilter( ChangePasswordProtocolCodecFactory.getInstance() ) );
    }


    @Override
    public void sessionOpened( IoSession session )
    {
        LOG.debug( "{} OPENED", session.getRemoteAddress() );
    }


    @Override
    public void sessionClosed( IoSession session )
    {
        LOG.debug( "{} CLOSED", session.getRemoteAddress() );
    }


    @Override
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        LOG.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
    }


    @Override
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        LOG.debug( session.getRemoteAddress() + " EXCEPTION", cause );
        session.closeNow();
    }


    @Override
    public void messageReceived( IoSession session, Object message )
    {
        LOG.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );

        InetAddress clientAddress = ( ( InetSocketAddress ) session.getRemoteAddress() ).getAddress();
        ChangePasswordRequest request = ( ChangePasswordRequest ) message;

        try
        {
            ChangePasswordContext changepwContext = new ChangePasswordContext();
            changepwContext.setConfig( server.getConfig() );
            changepwContext.setStore( store );
            changepwContext.setClientAddress( clientAddress );
            changepwContext.setRequest( request );
            changepwContext.setReplayCache( server.getReplayCache() );
            session.setAttribute( getContextKey(), changepwContext );

            ChangePasswordService.execute( session, changepwContext );

            session.write( changepwContext.getReply() );
        }
        catch ( KerberosException ke )
        {
            if ( LOG.isDebugEnabled() )
            {
                LOG.warn( ke.getLocalizedMessage(), ke );
            }
            else
            {
                LOG.warn( ke.getLocalizedMessage() );
            }

            KrbError errorMessage = getErrorMessage( server.getConfig().getServicePrincipal(), ke );

            session.write( new ChangePasswordError( request.getVersionNumber(), errorMessage ) );
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_152, e.getLocalizedMessage() ), e );

            KrbError error = getErrorMessage( server.getConfig().getServicePrincipal(), new ChangePasswordException(
                ChangePasswdErrorType.KRB5_KPASSWD_UNKNOWN_ERROR ) );
            session.write( new ChangePasswordError( request.getVersionNumber(), error ) );
        }
    }


    @Override
    public void messageSent( IoSession session, Object message )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
        }
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }


    private KrbError getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        KrbError krbError = new KrbError();

        KerberosTime now = new KerberosTime();

        //FIXME not sure if this is the correct error to set for KrbError instance
        // the correct change password protocol related error code is set in e-data anyway
        krbError.setErrorCode( ErrorType.KRB_ERR_GENERIC );
        krbError.setEText( exception.getLocalizedMessage() );
        krbError.setSName( new PrincipalName( principal ) );
        krbError.setSTime( now );
        krbError.setSusec( 0 );
        krbError.setRealm( principal.getRealm() );
        krbError.setEData( buildExplanatoryData( exception ) );

        return krbError;
    }


    private byte[] buildExplanatoryData( KerberosException exception )
    {
        short resultCode = ( short ) exception.getErrorCode();

        byte[] resultString =
            { ( byte ) 0x00 };

        if ( exception.getExplanatoryData() == null || exception.getExplanatoryData().length == 0 )
        {
            try
            {
                resultString = exception.getLocalizedMessage().getBytes( "UTF-8" );
            }
            catch ( UnsupportedEncodingException uee )
            {
                LOG.error( uee.getLocalizedMessage() );
            }
        }
        else
        {
            resultString = exception.getExplanatoryData();
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate( 2 + resultString.length );
        byteBuffer.putShort( resultCode );
        byteBuffer.put( resultString );

        return byteBuffer.array();
    }

    
    @Override
    public void inputClosed( IoSession session )
    {
    }
}
