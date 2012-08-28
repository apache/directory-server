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
package org.apache.directory.server.ldap.handlers.extended;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.extras.extended.GracefulDisconnectResponseImpl;
import org.apache.directory.shared.ldap.extras.extended.GracefulDisconnectResponse;
import org.apache.directory.shared.ldap.extras.extended.GracefulShutdownResponseImpl;
import org.apache.directory.shared.ldap.extras.extended.GracefulShutdownRequest;
import org.apache.directory.shared.ldap.extras.extended.GracefulShutdownResponse;
import org.apache.directory.shared.ldap.model.message.extended.NoticeOfDisconnect;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @todo : missing Javadoc
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GracefulShutdownHandler implements
    ExtendedOperationHandler<GracefulShutdownRequest, GracefulShutdownResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( GracefulShutdownHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 3 );
        set.add( GracefulShutdownRequest.EXTENSION_OID );
        set.add( GracefulShutdownResponse.EXTENSION_OID );
        set.add( GracefulDisconnectResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    public String getOid()
    {
        return GracefulShutdownRequest.EXTENSION_OID;
    }


    public void handleExtendedOperation( LdapSession requestor, GracefulShutdownRequest req ) throws Exception
    {
        // make sue only the administrator can issue this shutdown request if 
        // not we respond to the requestor with with insufficientAccessRights(50)
        if ( !requestor.getCoreSession().isAnAdministrator() )
        {
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Rejected with insufficientAccessRights to attempt for server shutdown by "
                    + requestor.getCoreSession().getEffectivePrincipal().getName() );
            }

            requestor.getIoSession().write( new GracefulShutdownResponseImpl(
                req.getMessageId(), ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS ) );
            return;
        }

        // -------------------------------------------------------------------
        // handle the body of this operation below here
        // -------------------------------------------------------------------

        IoAcceptor acceptor = ( IoAcceptor ) requestor.getIoSession().getService();
        List<IoSession> sessions = new ArrayList<IoSession>(
            acceptor.getManagedSessions().values() );

        // build the graceful disconnect message with replicationContexts
        GracefulDisconnectResponse notice = getGracefulDisconnect( req.getTimeOffline(), req.getDelay() );

        // send (synch) the GracefulDisconnect to each client before unbinding
        sendGracefulDisconnect( sessions, notice, requestor.getIoSession() );

        // wait for the specified delay before we unbind the service 
        waitForDelay( req.getDelay() );

        // -------------------------------------------------------------------
        // unbind the server socket for the LDAP service here so no new 
        // connections are accepted while we process this shutdown request
        // note that the following must be issued before binding the ldap
        // service in order to prevent client disconnects on service unbind:
        // 
        // minaRegistry.getAcceptor( service.getTransportType() )
        //                       .setDisconnectClientsOnUnbind( false );
        // -------------------------------------------------------------------
        // This might not work, either.
        acceptor.unbind( requestor.getIoSession().getServiceAddress() );

        // -------------------------------------------------------------------
        // synchronously send a NoD to clients that are not aware of this resp
        // after sending the NoD the client is disconnected if still connected
        // -------------------------------------------------------------------
        sendNoticeOfDisconnect( sessions, requestor.getIoSession() );

        // -------------------------------------------------------------------
        // respond back to the client that requested the graceful shutdown w/
        // a success resultCode which confirms all clients have been notified
        // via the graceful disconnect or NoD and the service has been unbound
        // preventing new connections; after recieving this response the 
        // requestor should disconnect and stop using the connection
        // -------------------------------------------------------------------
        sendShutdownResponse( requestor.getIoSession(), req.getMessageId() );
    }


    /**
     * Sends a successful response.
     * 
     * @param requestor the session of the requestor
     * @param messageId the message id associaed with this shutdown request
     */
    public static void sendShutdownResponse( IoSession requestor, int messageId )
    {
        GracefulShutdownResponse msg = new GracefulShutdownResponseImpl( messageId, ResultCodeEnum.SUCCESS );
        WriteFuture future = requestor.write( msg );
        future.awaitUninterruptibly();
        if ( future.isWritten() )
        {
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Sent GracefulShutdownResponse to client: " + requestor.getRemoteAddress() );
            }
        }
        else
        {
            LOG.error( I18n.err( I18n.ERR_159, requestor.getRemoteAddress() ) );
        }
        requestor.close( true );
    }


    /**
     * Blocks to synchronously send the same GracefulDisconnect message to all 
     * managed sessions except for the requestor of the GracefulShutdown.
     * 
     * @param msg the graceful disconnec extended request to send
     * @param requestor the session of the graceful shutdown requestor
     * @param sessions the IoSessions to send disconnect message to
     */
    public static void sendGracefulDisconnect( List<IoSession> sessions, GracefulDisconnectResponse msg,
        IoSession requestor )
    {
        List<WriteFuture> writeFutures = new ArrayList<WriteFuture>();

        // asynchronously send GracefulDisconnection messages to all connected
        // clients giving time for the message to arrive before we block 
        // waiting for message delivery to the client in the loop below

        if ( sessions != null )
        {
            for ( IoSession session : sessions )
            {
                // make sure we do not send the disconnect mesasge to the
                // client which sent the initiating GracefulShutdown request
                if ( session.equals( requestor ) )
                {
                    continue;
                }

                try
                {
                    writeFutures.add( session.write( msg ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to write GracefulDisconnect to client session: " + session, e );
                }
            }
        }

        // wait for GracefulDisconnect messages to be sent before returning
        for ( WriteFuture future : writeFutures )
        {
            try
            {
                future.awaitUninterruptibly( 1000 );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to sent GracefulDisconnect", e );
            }
        }
    }


    /**
     * Blocks to synchronously send the a NoticeOfDisconnect message with
     * the resultCode set to unavailable(52) to all managed sessions except 
     * for the requestor of the GracefulShutdown.
     * 
     * @param requestor the session of the graceful shutdown requestor
     * @param sessions the sessions from mina
     */
    public static void sendNoticeOfDisconnect( List<IoSession> sessions, IoSession requestor )
    {
        List<WriteFuture> writeFutures = new ArrayList<WriteFuture>();

        // Send Notification of Disconnection messages to all connected clients.
        if ( sessions != null )
        {
            for ( IoSession session : sessions )
            {
                // make sure we do not send the disconnect mesasge to the
                // client which sent the initiating GracefulShutdown request
                if ( session.equals( requestor ) )
                {
                    continue;
                }

                try
                {
                    writeFutures.add( session.write( NoticeOfDisconnect.UNAVAILABLE ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed to sent NoD for client: " + session, e );
                }
            }
        }

        // And close the connections when the NoDs are sent.
        Iterator<IoSession> sessionIt = sessions.iterator();

        for ( WriteFuture future : writeFutures )
        {
            try
            {
                future.awaitUninterruptibly( 1000 );
                sessionIt.next().close( true );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to sent NoD.", e );
            }
        }
    }


    public static GracefulDisconnectResponse getGracefulDisconnect( int timeOffline, int delay )
    {
        // build the graceful disconnect message with replicationContexts
        return new GracefulDisconnectResponseImpl( timeOffline, delay );
    }


    public static void waitForDelay( int delay )
    {
        if ( delay > 0 )
        {
            // delay is in seconds
            long delayMillis = delay * 1000L;
            long startTime = System.currentTimeMillis();

            while ( ( System.currentTimeMillis() - startTime ) < delayMillis )
            {
                try
                {
                    Thread.sleep( 250 );
                }
                catch ( InterruptedException e )
                {
                    LOG.warn( "Got interrupted while waiting for delay before shutdown", e );
                }
            }
        }
    }


    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
