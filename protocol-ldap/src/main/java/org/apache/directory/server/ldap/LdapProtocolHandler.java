/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap;


import org.apache.directory.api.ldap.codec.api.LdapDecoder;
import org.apache.directory.api.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.api.ldap.codec.api.SchemaBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.exception.ResponseCarryingMessageException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.Message;
import org.apache.directory.api.ldap.model.message.Request;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.ResultResponse;
import org.apache.directory.api.ldap.model.message.ResultResponseRequest;
import org.apache.directory.api.ldap.model.message.extended.NoticeOfDisconnect;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;
import org.apache.mina.filter.ssl.SslEvent;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The MINA IoHandler implementation extending {@link DemuxingIoHandler} for
 * the LDAP protocol.  THe {@link LdapServer} creates this multiplexing
 * {@link IoHandler} handler and populates it with subordinate handlers for
 * the various kinds of LDAP {@link Request} messages.  This is done in the
 * setXxxHandler() methods of the LdapServer where Xxxx is Add, Modify, etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class LdapProtocolHandler extends DemuxingIoHandler
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdapProtocolHandler.class );

    /** the {@link LdapServer} this handler is associated with */
    private final LdapServer ldapServer;


    /**
     * Creates a new instance of LdapProtocolHandler.
     *
     * @param ldapServer The LDAP server instance
     */
    LdapProtocolHandler( LdapServer ldapServer )
    {
        this.ldapServer = ldapServer;
    }


    /**
     * This method is called when a new session is created. We will store some
     * informations that the session will need to process incoming requests.
     * 
     * @param session the newly created session
     */
    @Override
    public void sessionCreated( IoSession session ) throws Exception
    {
        // First, create a new LdapSession and store it i the manager
        LdapSession ldapSession = new LdapSession( session );
        ldapServer.getLdapSessionManager().addLdapSession( ldapSession );

        // Now, we have to store the DirectoryService instance into the session
        session.setAttribute( LdapDecoder.MAX_PDU_SIZE_ATTR, ldapServer.getDirectoryService().getMaxPDUSize() );

        // Last, store the message container
        LdapMessageContainer<Message> ldapMessageContainer =
            new LdapMessageContainer<>(
                ldapServer.getDirectoryService().getLdapCodecService(),
                new SchemaBinaryAttributeDetector(
                    ldapServer.getDirectoryService().getSchemaManager() ) );

        session.setAttribute( LdapDecoder.MESSAGE_CONTAINER_ATTR, ldapMessageContainer );
    }


    /**
     * This method is called when a session is closed. If we have some
     * cleanup to do, it's done there.
     * 
     * @param session the closing session
     */
    @Override
    public void sessionClosed( IoSession session )
    {
        // Get the associated LdapSession
        LdapSession ldapSession = ldapServer.getLdapSessionManager().removeLdapSession( session );

        // Clean it up !
        cleanUpSession( ldapSession );
    }


    /**
     * Explicitly handles {@link LdapSession} and {@link IoSession} cleanup tasks.
     *
     * @param ldapSession the LdapSession to cleanup after being removed from
     * the {@link LdapSessionManager}
     */
    private void cleanUpSession( LdapSession ldapSession )
    {
        if ( ldapSession == null )
        {
            LOG.debug( "Null LdapSession given to cleanUpSession." );
            return;
        }

        LOG.debug( "Cleaning the {} session", ldapSession );

        // Abandon all the requests
        ldapSession.abandonAllOutstandingRequests();

        if ( !ldapSession.getIoSession().isClosing() || ldapSession.getIoSession().isConnected() )
        {
            try
            {
                ldapSession.getIoSession().closeNow();
            }
            catch ( Throwable t )
            {
                LOG.warn( "Failed to close IoSession for LdapSession." );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent( IoSession session, Object message ) throws Exception
    {
        // Do nothing : we have to ignore this message, otherwise we get an exception,
        // thanks to the way MINA 2 works ...
        if ( message instanceof IoBuffer )
        {
            // Nothing to do in this case
            return;
        }

        super.messageSent( session, message );
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void event( IoSession session, FilterEvent event ) throws Exception 
    {
        if ( event instanceof SslEvent )
        {
            if ( ( ( SslEvent ) event ) == SslEvent.SECURED ) 
            {
                LdapSession ldapSession = ldapServer.getLdapSessionManager().getLdapSession( session );
                LOG.debug( "Session {} secured", ldapSession ); 
            }
            else
            {
                LdapSession ldapSession = ldapServer.getLdapSessionManager().getLdapSession( session );
                LOG.debug( "Session {} not secured", ldapSession ); 
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        // Translate SSLFilter messages into LDAP extended request
        // defined in RFC #2830, 'Lightweight Directory Access Protocol (v3):
        // Extension for Transport Layer Security'.
        //
        // The RFC specifies the payload should be empty, but we use
        // it to notify the TLS state changes.  This hack should be
        // OK from the viewpointd of security because StartTLS
        // handler should react to only SESSION_UNSECURED message
        // and degrade authentication level to 'anonymous' as specified
        // in the RFC, and this is no threat.
        if ( ( ( Request ) message ).getControls().size() > 0
            && message instanceof ResultResponseRequest )
        {
            ResultResponseRequest req = ( ResultResponseRequest ) message;

            for ( Control control : req.getControls().values() )
            {
                if ( control.isCritical() && !ldapServer.getSupportedControls().contains( control.getOid() ) )
                {
                    ResultResponse resp = req.getResultResponse();
                    resp.getLdapResult().setDiagnosticMessage( "Unsupport critical control: " + control.getOid() );
                    resp.getLdapResult().setResultCode( ResultCodeEnum.UNAVAILABLE_CRITICAL_EXTENSION );
                    session.write( resp );

                    return;
                }
            }
        }

        super.messageReceived( session, message );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        if ( cause.getCause() instanceof ResponseCarryingMessageException )
        {
            ResponseCarryingMessageException rcme = ( ResponseCarryingMessageException ) cause.getCause();

            if ( rcme.getResponse() != null )
            {
                session.write( rcme.getResponse() );
                return;
            }
        }

        LOG.warn( "Unexpected exception forcing session to close: sending disconnect notice to client.", cause );

        session.write( NoticeOfDisconnect.PROTOCOLERROR );
        session.closeOnFlush();
        //LdapSession ldapSession = this.ldapServer.getLdapSessionManager().removeLdapSession( session );
        //cleanUpSession( ldapSession );
    }
}
