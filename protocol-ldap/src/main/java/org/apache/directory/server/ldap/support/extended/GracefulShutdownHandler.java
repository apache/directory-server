/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.directory.server.ldap.support.extended;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.extended.GracefulDisconnect;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownRequest;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownResponse;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GracefulShutdownHandler implements ExtendedOperationHandler
{
    private static final Logger log = LoggerFactory.getLogger( GracefulShutdownHandler.class );
    public static final Set EXTENSION_OIDS;

    static
    {
        Set set = new HashSet( 3 );
        set.add( GracefulShutdownRequest.EXTENSION_OID );
        set.add( GracefulShutdownResponse.EXTENSION_OID );
        set.add( GracefulDisconnect.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    public String getOid()
    {
        return GracefulShutdownRequest.EXTENSION_OID;
    }


    public void handleExtendedOperation( IoSession requestor, SessionRegistry registry, ExtendedRequest req )
        throws NamingException
    {
        DirectoryService service = null;
        ServerLdapContext slc = null;
        LdapContext ctx = registry.getLdapContext( requestor, null, false );
        ctx = ( LdapContext ) ctx.lookup( "" );

        // setup some of the variables we need and make sure they are of the 
        // right types otherwise send back an operations error in response
        if ( ctx instanceof ServerLdapContext )
        {
            slc = ( ServerLdapContext ) ctx;
            service = slc.getService();
        }
        else
        {
            log.error( "Encountered session context which was not a ServerLdapContext" );
            GracefulShutdownResponse msg = new GracefulShutdownResponse( req.getMessageId(),
                ResultCodeEnum.OPERATIONSERROR );
            msg.getLdapResult().setErrorMessage( "The session context was not a ServerLdapContext" );
            requestor.write( msg );
            return;
        }

        // make sue only the administrator can issue this shutdown request if 
        // not we respond to the requestor with with insufficientAccessRights(50)
        if ( !slc.getPrincipal().getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Rejected with insufficientAccessRights to attempt for server shutdown by "
                    + slc.getPrincipal().getName() );
            }

            requestor
                .write( new GracefulShutdownResponse( req.getMessageId(), ResultCodeEnum.INSUFFICIENTACCESSRIGHTS ) );
            return;
        }

        // -------------------------------------------------------------------
        // handle the body of this operation below here
        // -------------------------------------------------------------------

        IoAcceptor acceptor = ( IoAcceptor ) requestor.getService();
        List sessions = new ArrayList( acceptor.getManagedSessions( requestor.getServiceAddress() ) );
        StartupConfiguration cfg = service.getConfiguration().getStartupConfiguration();
        GracefulShutdownRequest gsreq = ( GracefulShutdownRequest ) req;

        // build the graceful disconnect message with replicationContexts
        DirectoryPartitionNexus nexus = service.getConfiguration().getPartitionNexus();
        GracefulDisconnect notice = getGracefulDisconnect( gsreq.getTimeOffline(), gsreq.getDelay(), nexus );

        // send (synch) the GracefulDisconnect to each client before unbinding
        sendGracefulDisconnect( sessions, notice, requestor );

        // wait for the specified delay before we unbind the service 
        waitForDelay( gsreq.getDelay() );

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
        acceptor.unbind( requestor.getServiceAddress() );

        // -------------------------------------------------------------------
        // synchronously send a NoD to clients that are not aware of this resp
        // after sending the NoD the client is disconnected if still connected
        // -------------------------------------------------------------------
        sendNoticeOfDisconnect( sessions, requestor );

        // -------------------------------------------------------------------
        // respond back to the client that requested the graceful shutdown w/
        // a success resultCode which confirms all clients have been notified
        // via the graceful disconnect or NoD and the service has been unbound
        // preventing new connections; after recieving this response the 
        // requestor should disconnect and stop using the connection
        // -------------------------------------------------------------------
        sendShutdownResponse( requestor, req.getMessageId() );

        if ( cfg.isExitVmOnShutdown() )
        {
            System.exit( 0 );
        }

        return;
    }


    /**
     * Sends a successful response.
     * 
     * @param requestor
     * @param messageId
     */
    public static void sendShutdownResponse( IoSession requestor, int messageId )
    {
        GracefulShutdownResponse msg = new GracefulShutdownResponse( messageId, ResultCodeEnum.SUCCESS );
        WriteFuture future = requestor.write( msg );
        future.join();
        if ( future.isWritten() )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Sent GracefulShutdownResponse to client: " + requestor.getRemoteAddress() );
            }
        }
        else
        {
            log.error( "Failed to write GracefulShutdownResponse to client: " + requestor.getRemoteAddress() );
        }
        requestor.close();
    }


    /**
     * Blocks to synchronously send the same GracefulDisconnect message to all 
     * managed sessions except for the requestor of the GracefulShutdown.
     * 
     * @param msg the graceful disconnec extended request to send
     * @param requestor the session of the graceful shutdown requestor
     */
    public static void sendGracefulDisconnect( List sessions, GracefulDisconnect msg, IoSession requestor )
    {
        List writeFutures = new ArrayList();

        // asynchronously send GracefulDisconnection messages to all connected
        // clients giving time for the message to arrive before we block 
        // waiting for message delivery to the client in the loop below

        if ( sessions != null )
        {
            for ( Iterator i = sessions.iterator(); i.hasNext(); )
            {
                IoSession session = session = ( IoSession ) i.next();

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
                    log.warn( "Failed to write GracefulDisconnect to client session: " + session, e );
                }
            }
        }

        // wait for GracefulDisconnect messages to be sent before returning
        for ( Iterator i = writeFutures.iterator(); i.hasNext(); )
        {
            WriteFuture future = ( WriteFuture ) i.next();
            try
            {
                future.join( 1000 );
            }
            catch ( Exception e )
            {
                log.warn( "Failed to sent GracefulDisconnect", e );
            }
        }
    }


    /**
     * Blocks to synchronously send the a NoticeOfDisconnect message with
     * the resultCode set to unavailable(52) to all managed sessions except 
     * for the requestor of the GracefulShutdown.
     * 
     * @param requestor the session of the graceful shutdown requestor
     */
    public static void sendNoticeOfDisconnect( List sessions, IoSession requestor )
    {
        List writeFutures = new ArrayList();

        // Send Notification of Disconnection messages to all connected clients.
        if ( sessions != null )
        {
            for ( Iterator i = sessions.iterator(); i.hasNext(); )
            {
                IoSession session = session = ( IoSession ) i.next();

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
                    log.warn( "Failed to sent NoD for client: " + session, e );
                }
            }
        }

        // And close the connections when the NoDs are sent.
        Iterator sessionIt = sessions.iterator();
        for ( Iterator i = writeFutures.iterator(); i.hasNext(); )
        {
            WriteFuture future = ( WriteFuture ) i.next();
            try
            {
                future.join( 1000 );
                ( ( IoSession ) sessionIt.next() ).close();
            }
            catch ( Exception e )
            {
                log.warn( "Failed to sent NoD.", e );
            }
        }
    }


    public static GracefulDisconnect getGracefulDisconnect( int timeOffline, int delay, DirectoryPartitionNexus nexus )
    {
        // build the graceful disconnect message with replicationContexts
        GracefulDisconnect notice = new GracefulDisconnect( timeOffline, delay );
        // @todo add the referral objects for replication contexts using setup code below
        //        Iterator list = nexus.listSuffixes( true );
        //        while ( list.hasNext() )
        //        {
        //            LdapName dn = new LdapName( ( String ) list.next() );
        //            DirectoryPartition partition = nexus.getPartition( dn );
        //        }
        return notice;
    }


    public static void waitForDelay( int delay )
    {
        if ( delay > 0 )
        {
            // delay is in seconds
            long delayMillis = delay * 1000;
            long startTime = System.currentTimeMillis();

            while ( ( System.currentTimeMillis() - startTime ) < delayMillis )
            {
                try
                {
                    Thread.sleep( 250 );
                }
                catch ( InterruptedException e )
                {
                    log.warn( "Got interrupted while waiting for delay before shutdown", e );
                }
            }
        }
    }


    public Set getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public void setLdapProvider( LdapProtocolProvider provider )
    {
    }
}
