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
package org.apache.directory.mitosis.service.protocol.handler;


import org.apache.directory.mitosis.common.*;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.AddEntryOperation;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.service.ReplicationContext;
import org.apache.directory.mitosis.service.ReplicationContext.State;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.mitosis.service.protocol.message.*;
import org.apache.directory.mitosis.store.ReplicationLogIterator;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.util.SessionLog;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.net.InetSocketAddress;
import java.util.Map;


/**
 * {@link ReplicationContextHandler} that implements client-side replication
 * logic which sends any changes out-of-date to server.  The following is
 * the detailed protocol flow and the description of the replication logic
 * execution.
 * <ul>
 * <li><tt>ClientConnectionManager</tt> connects the client to the server.</li>
 * <li>The client sends {@link LoginMessage} to the server.</li>
 * <li>The server responds with {@link LoginAckMessage} to the client
 *     <ul>
 *     <li>Unless the response code is {@link Constants#OK}, disconnect.
 *         Next connection attempt is performed by
 *         <tt>ClientConnectionManager</tt> later.</li>
 *     <li>Otherwise, the state of the {@link ReplicationContext} changes to
 *         {@link State#READY}, and proceed.</li>
 *     </ul></li>
 * <li>The client tries to transfer the data that server needs from
 *     in {@link ReplicationStore} periodically using
 *     {@link #contextIdle(ReplicationContext, IdleStatus)} event,
 *     which is implemented using <tt>sessionIdle</tt> event in MINA. 
 *     <ul>
 *     <li>The client sends a {@link BeginLogEntriesMessage} to the server.</li>
 *     <li>The server responds with {@link BeginLogEntriesAckMessage}.
 *         <ul>
 *         <li>If the response code is {@link Constants#OK},
 *             <ul>
 *             <li>{@link BeginLogEntriesAckMessage} contains a
 *                 Update Vector (UV) of the server. The client compares
 *                 the received UV and the client's Purge Vector (PV).
 *                 <ul>
 *                 <li>If the PV is greater than the UV, this means the client
 *                     can't send all operation logs that server needs to get
 *                     synchronized.  This usually means that the server has
 *                     been offline for too long time and got out-of-sync
 *                     finally due to the log-purging process of the client
 *                     side (see {@link ReplicationConfiguration#getLogMaxAge()}).
 *                     The clients sends all entries in the DIT to the server,
 *                     and the server overwrites its current DIT with the
 *                     received entries.</li>
 *                 <li>Otherwise, the client sends only the changed part since
 *                     the last synchronization by querying its
 *                     {@link ReplicationStore} by calling
 *                     {@link ReplicationStore#getLogs(CSNVector, boolean)}.</li>
 *                 <li>The data transfer is very simple.  It's asynchronous
 *                     request-response exchange.  The client sends {@link LogEntryMessage},
 *                     and then the server responds with {@link LogEntryAckMessage}.</li>
 *             </ul></li>
 *         <li>If the response code is not {@link Constants#OK}, retry later.</li>
 *         </ul></li>
 *     </ul></li>
 * </ul>
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev: 116 $, $Date: 2006-09-18 13:47:53Z $
 */
public class ReplicationClientContextHandler implements ReplicationContextHandler
{
    public void contextBegin( ReplicationContext ctx ) throws Exception
    {
        // Send a login message.
        LoginMessage m = new LoginMessage( ctx.getNextSequence(), ctx.getService().getConfiguration().getReplicaId() );
        writeTimeLimitedMessage( ctx, m );

        // Set write timeout
        ctx.getSession().setWriteTimeout( ctx.getConfiguration().getResponseTimeout() );

        // Check update vector of the remote peer periodically.
        ctx.getSession().setIdleTime(
        		IdleStatus.BOTH_IDLE,
        		ctx.getConfiguration().getReplicationInterval());
    }


    public void contextEnd( ReplicationContext ctx ) throws Exception
    {
    }


    public void messageReceived( ReplicationContext ctx, Object message ) throws Exception
    {
        ctx.cancelExpiration( ( ( BaseMessage ) message ).getSequence() );

        if ( ctx.getState() == State.READY )
        {
            if ( message instanceof LogEntryAckMessage )
            {
                onLogEntryAck( ctx, ( LogEntryAckMessage ) message );
            }
            else if ( message instanceof BeginLogEntriesAckMessage )
            {
                onBeginLogEntriesAck( ctx, ( BeginLogEntriesAckMessage ) message );
            }
            else if ( message instanceof EndLogEntriesAckMessage )
            {
                // Do nothing
            }
            else
            {
                onUnexpectedMessage( ctx, message );
            }
        }
        else
        {
            if ( message instanceof LoginAckMessage )
            {
                onLoginAck( ctx, ( LoginAckMessage ) message );
            }
            else
            {
                onUnexpectedMessage( ctx, message );
            }
        }
    }


    public void messageSent( ReplicationContext ctx, Object message ) throws Exception
    {
    }
    
    
    /**
     * A helper to write a message and schedule that message for expiration.
     *
     * @param ctx the replication context
     * @param message the message to replicate
     * @return the write future to block on this replication message transmission
     */
    public WriteFuture writeTimeLimitedMessage( ReplicationContext ctx, Object message )
    {
        ctx.scheduleExpiration( message );
        return ctx.getSession().write( message );
    }


    public void exceptionCaught( ReplicationContext ctx, Throwable cause ) throws Exception
    {
        if ( SessionLog.isWarnEnabled( ctx.getSession() ) )
        {
            SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId()
                    +"] Unexpected exception.", cause );
        }
        ctx.getSession().close();
    }


    public void contextIdle( ReplicationContext ctx, IdleStatus status ) throws Exception
    {
        beginReplication( ctx );
    }


    private void onLoginAck( ReplicationContext ctx, LoginAckMessage message )
    {
        if ( message.getResponseCode() != Constants.OK )
        {
            SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Login attempt failed: " + message.getResponseCode() );
            ctx.getSession().close();
            return;
        }

        for ( Replica replica : ctx.getConfiguration().getPeerReplicas() )
        {
            if ( replica.getId().equals( message.getReplicaId() ) )
            {
                if ( replica.getAddress().getAddress().equals(
                        ( ( InetSocketAddress ) ctx.getSession().getRemoteAddress() ).getAddress() ) )
                {
                    ctx.setPeer( replica );
                    ctx.setState( State.READY );
                    return;
                }
                else
                {
                    SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId()
                            + "] Peer address mismatches: "
                            + ctx.getSession().getRemoteAddress() + " (expected: " + replica.getAddress() );
                    ctx.getSession().close();
                    return;
                }
            }
        }

        SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Unknown peer replica ID: " + message.getReplicaId() );
        ctx.getSession().close();
    }


    public boolean beginReplication( ReplicationContext ctx )
    {
        // If this cilent is logged in, all responses for sent messages
        // (LogEntryMessages) is received, and no write request is pending,
        // it means previous replication process ended or this is the
        // first replication attempt.
        if ( ctx.getState() == State.READY && ctx.getScheduledExpirations() <= 0
            && ctx.getSession().getScheduledWriteRequests() <= 0 )
        {
        	// Initiate replication process asking update vector.
            if ( SessionLog.isDebugEnabled( ctx.getSession() ) ) {
                SessionLog.debug( ctx.getSession(), "(" +
                    ctx.getConfiguration().getReplicaId().getId() + "->" +
                    (ctx.getPeer() != null ? ctx.getPeer().getId().getId() : "null") +
                    ") Beginning replication. " );
            }
        	ctx.getSession().write( new BeginLogEntriesMessage( ctx.getNextSequence() ) );
        	return true;
        }
        else
        {
            if ( SessionLog.isDebugEnabled( ctx.getSession() ) ) {
                SessionLog.debug( ctx.getSession(), "(" +
                    ctx.getConfiguration().getReplicaId().getId() + "->" +
                    (ctx.getPeer() != null ? ctx.getPeer().getId().getId() : "null") +
                    ") Couldn't begin replication.  State:" + ctx.getState() + ", scheduledExpirations:"
                        + ctx.getScheduledExpirations() +
                        ", scheduledWriteRequests:" + ctx.getSession().getScheduledWriteRequests() );
            }
        	return false;
        }
    }


    private void onLogEntryAck( ReplicationContext ctx, LogEntryAckMessage message ) throws Exception
    {
        if ( message.getResponseCode() != Constants.OK )
        {
            SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Remote peer failed to execute a log entry." );
            ctx.getSession().close();
        }
    }


    private void onBeginLogEntriesAck( ReplicationContext ctx, BeginLogEntriesAckMessage message )
        throws NamingException
    {
        // Start transaction only when the server says OK.
        if ( message.getResponseCode() != Constants.OK )
        {
            return;
        }

        ReplicationStore store = ctx.getConfiguration().getStore();
        CSNVector yourUV = message.getUpdateVector();
        CSNVector myPV;
        try
        {
            myPV = store.getPurgeVector();
        }
        catch ( Exception e )
        {
            SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Failed to get update vector.", e );
            ctx.getSession().close();
            return;
        }

        // Do full-DIT transfer if the peer is new and I'm not new.
        try
        {
            if ( myPV.size() > 0 && yourUV.size() == 0 )
            {
                SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Starting a whole DIT transfer." );
                sendAllEntries( ctx );
            }
            else
            {
                SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Starting a partial replication log transfer." );
                sendReplicationLogs( ctx, myPV, yourUV );
            }
        }
        finally
        {
            // Send EngLogEntries message to release the remote peer resources.
            ctx.getSession().write( new EndLogEntriesMessage( ctx.getNextSequence() ) );
        }
    }


    private void sendAllEntries( ReplicationContext ctx ) throws NamingException
    {
        Attributes rootDSE = ctx.getDirectoryService().getPartitionNexus().getRootDSE( null );

        Attribute namingContextsAttr = rootDSE.get( "namingContexts" );
        if ( namingContextsAttr == null || namingContextsAttr.size() == 0 )
        {
            SessionLog.warn( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] No namingContexts attributes in rootDSE." );
            return;
        }

        // Iterate all context partitions to send all entries of them.
        NamingEnumeration e = namingContextsAttr.getAll();
        while ( e.hasMore() )
        {
            Object value = e.next();

            // Convert attribute value to JNDI name.
            LdapDN contextName;
            if ( value instanceof LdapDN )
            {
                contextName = ( LdapDN ) value;
            }
            else
            {
                contextName = new LdapDN( String.valueOf( value ) );
            }

            SessionLog.info( ctx.getSession(), "[Replica-"+ ctx.getConfiguration().getReplicaId() +
                    "] Sending entries under '" + contextName + '\'' );

            Map<String, OidNormalizer> mapping = ctx.getDirectoryService().getRegistries().getAttributeTypeRegistry()
                .getNormalizerMapping();
            contextName.normalize( mapping );
            sendAllEntries( ctx, contextName );
        }
    }


    private void sendAllEntries( ReplicationContext ctx, LdapDN contextName ) throws NamingException
    {
        // Retrieve all subtree including the base entry
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration e = ctx.getDirectoryService().getPartitionNexus().search(
            new SearchOperationContext( contextName,
            ctx.getDirectoryService().getEnvironment(),
            new PresenceNode( SchemaConstants.OBJECT_CLASS_AT_OID ), ctrl ) );

        try
        {
            while ( e.hasMore() )
            {
                SearchResult sr = ( SearchResult ) e.next();
                Attributes attrs = sr.getAttributes();

                // Skip entries without entryCSN attribute.
                Attribute entryCSNAttr = attrs.get( org.apache.directory.mitosis.common.Constants.ENTRY_CSN );
                if ( entryCSNAttr == null )
                {
                    continue;
                }

                // Get entryCSN of the entry.  Skip if entryCSN value is invalid. 
                CSN csn;
                try
                {
                    csn = new DefaultCSN( String.valueOf( entryCSNAttr.get() ) );
                }
                catch ( IllegalArgumentException ex )
                {
                    SessionLog.warn( ctx.getSession(), "An entry with improper entryCSN: " + sr.getName() );
                    continue;
                }

                // Convert the entry into AddEntryOperation log.
                LdapDN dn = new LdapDN( sr.getName() );
                dn.normalize( ctx.getDirectoryService().getRegistries()
                        .getAttributeTypeRegistry().getNormalizerMapping() );
                Operation op = new AddEntryOperation( csn, dn, attrs );

                // Send a LogEntry message for the entry.
                writeTimeLimitedMessage( ctx, new LogEntryMessage( ctx.getNextSequence(), op ) );
            }
        }
        finally
        {
            e.close();
        }
    }


    @SuppressWarnings("unchecked")
    private void sendReplicationLogs( ReplicationContext ctx, CSNVector myPV, CSNVector yourUV )
    {
        for ( ReplicaId replicaId : myPV.getReplicaIds() )
        {
            CSN myCSN = myPV.getCSN( replicaId );
            CSN yourCSN = yourUV.getCSN( replicaId );
            if ( yourCSN != null && ( myCSN == null || yourCSN.compareTo( myCSN ) < 0 ) )
            {
                SessionLog.warn( ctx.getSession(), "Remote update vector (" + yourUV
                        + ") is out-of-date.  Full replication is required." );
                ctx.getSession().close();
                return;
            }
        }

        ReplicationLogIterator logIt = ctx.getConfiguration().getStore().getLogs( yourUV, false );
        try
        {
            while ( logIt.next() )
            {
                Operation op = logIt.getOperation();
                writeTimeLimitedMessage( ctx, new LogEntryMessage( ctx.getNextSequence(), op ) );
            }
        }
        finally
        {
            logIt.close();
        }
    }


    private void onUnexpectedMessage( ReplicationContext ctx, Object message )
    {
        SessionLog.warn( ctx.getSession(), "Unexpected message: " + message );
        ctx.getSession().close();
    }
}
