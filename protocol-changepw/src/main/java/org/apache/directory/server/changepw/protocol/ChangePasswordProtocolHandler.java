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

package org.apache.directory.server.changepw.protocol;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.changepw.exceptions.ChangePasswordException;
import org.apache.directory.server.changepw.messages.ChangePasswordErrorModifier;
import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.service.ChangePasswordChain;
import org.apache.directory.server.changepw.service.ChangePasswordContext;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessageModifier;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChangePasswordProtocolHandler implements IoHandler
{
    private static final Logger log = LoggerFactory.getLogger( ChangePasswordProtocolHandler.class );

    private ChangePasswordConfiguration config;
    private PrincipalStore store;
    private IoHandlerCommand changepwService;
    private String contextKey = "context";


    public ChangePasswordProtocolHandler(ChangePasswordConfiguration config, PrincipalStore store)
    {
        this.config = config;
        this.store = store;

        changepwService = new ChangePasswordChain();
    }


    public void sessionCreated( IoSession session ) throws Exception
    {
        log.debug( "{} CREATED", session.getRemoteAddress() );

        session.getFilterChain().addFirst( "codec",
            new ProtocolCodecFilter( ChangePasswordProtocolCodecFactory.getInstance() ) );
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
        log.debug( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close();
    }


    public void messageReceived( IoSession session, Object message )
    {
        log.debug( "{} RCVD: {}", session.getRemoteAddress(), message );

        InetAddress clientAddress = ( ( InetSocketAddress ) session.getRemoteAddress() ).getAddress();
        ChangePasswordRequest request = ( ChangePasswordRequest ) message;

        try
        {
            ChangePasswordContext changepwContext = new ChangePasswordContext();
            changepwContext.setConfig( config );
            changepwContext.setStore( store );
            changepwContext.setClientAddress( clientAddress );
            changepwContext.setRequest( request );
            session.setAttribute( getContextKey(), changepwContext );

            changepwService.execute( null, session, message );

            session.write( changepwContext.getReply() );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );

            ChangePasswordException cpe = ( ChangePasswordException ) e;

            ErrorMessage errorMessage = getErrorMessage( config.getChangepwPrincipal(), cpe );

            ChangePasswordErrorModifier modifier = new ChangePasswordErrorModifier();
            modifier.setErrorMessage( errorMessage );

            session.write( modifier.getChangePasswordError() );
        }
    }


    public void messageSent( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} SENT: {}", session.getRemoteAddress(), message );
        }
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }


    private ErrorMessage getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        ErrorMessageModifier modifier = new ErrorMessageModifier();

        KerberosTime now = new KerberosTime();

        modifier.setErrorCode( exception.getErrorCode() );
        modifier.setExplanatoryText( exception.getMessage() );
        modifier.setServerPrincipal( principal );
        modifier.setServerTime( now );
        modifier.setServerMicroSecond( 0 );
        modifier.setExplanatoryData( buildExplanatoryData( exception ) );

        return modifier.getErrorMessage();
    }


    private byte[] buildExplanatoryData( KerberosException exception )
    {
        short resultCode = ( short ) exception.getErrorCode();
        byte[] resultString = exception.getExplanatoryData();

        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );
        byteBuffer.putShort( resultCode );
        byteBuffer.put( resultString );

        byteBuffer.flip();
        byte[] explanatoryData = new byte[byteBuffer.remaining()];
        byteBuffer.get( explanatoryData, 0, explanatoryData.length );

        return explanatoryData;
    }
}
