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


import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.util.SessionLog;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.SimpleCSN;
import org.apache.directory.mitosis.operation.AddEntryOperation;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.service.ReplicationContext;
import org.apache.directory.mitosis.service.ReplicationContext.State;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.mitosis.service.protocol.message.BeginLogEntriesAckMessage;
import org.apache.directory.mitosis.service.protocol.message.BeginLogEntriesMessage;
import org.apache.directory.mitosis.service.protocol.message.EndLogEntriesAckMessage;
import org.apache.directory.mitosis.service.protocol.message.EndLogEntriesMessage;
import org.apache.directory.mitosis.service.protocol.message.LogEntryAckMessage;
import org.apache.directory.mitosis.service.protocol.message.LogEntryMessage;
import org.apache.directory.mitosis.service.protocol.message.LoginAckMessage;
import org.apache.directory.mitosis.service.protocol.message.LoginMessage;
import org.apache.directory.mitosis.store.ReplicationLogIterator;
import org.apache.directory.mitosis.store.ReplicationStore;


/**
 * {@link ReplicationContextHandler} that implements client-side replication logic
 * which sends any changes out-of-date to server.
 *
 * @author Trustin Lee
 * @version $Rev: 116 $, $Date: 2006-09-18 13:47:53Z $
 */
public class ReplicationClientContextHandler implements ReplicationContextHandler
{
    public void contextBegin( ReplicationContext ctx ) throws Exception
    {
        // Send a login message.
        LoginMessage m = new LoginMessage( ctx.getNextSequence(), ctx.getService().getConfiguration().getReplicaId() );
        ctx.getSession().write( m );

        // Set write timeout
        ctx.getSession().setWriteTimeout( ctx.getConfiguration().getResponseTimeout() );

        // Check update vector of the remote peer every 5 seconds.
        ctx.getSession().setIdleTime( IdleStatus.BOTH_IDLE, 5 );
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
        if ( message instanceof LogEntryMessage || message instanceof LoginMessage )
        {
            ctx.scheduleExpiration( message );
        }
    }


    public void exceptionCaught( ReplicationContext ctx, Throwable cause ) throws Exception
    {
        SessionLog.warn( ctx.getSession(), "Unexpected exception.", cause );
        ctx.getSession().close();
    }


    public void contextIdle( ReplicationContext ctx, IdleStatus status ) throws Exception
    {
        // If this cilent is logged in, all responses for sent messages
        // (LogEntryMessages) is received, and no write request is pending,
        // it means previous replication process ended or this is the
        // first replication attempt.
        if ( ctx.getState() == State.READY && ctx.getScheduledExpirations() == 0
            && ctx.getSession().getScheduledWriteRequests() == 0 )
        {
            beginReplication( ctx );
        }
    }


    private void onLoginAck( ReplicationContext ctx, LoginAckMessage message )
    {
        if ( message.getResponseCode() != Constants.OK )
        {
            SessionLog.warn( ctx.getSession(), "Login attempt failed: " + message.getResponseCode() );
            ctx.getSession().close();
            return;
        }

        Iterator i = ctx.getConfiguration().getPeerReplicas().iterator();
        while ( i.hasNext() )
        {
            Replica replica = ( Replica ) i.next();
            if ( replica.getId().equals( message.getReplicaId() ) )
            {
                if ( replica.getAddress().getAddress().equals(
                    ( ( InetSocketAddress ) ctx.getSession().getRemoteAddress() ).getAddress() ) )
                {
                    ctx.setPeer( replica );
                    ctx.setState( State.READY );

                    beginReplication( ctx );
                    return;
                }
                else
                {
                    SessionLog.warn( ctx.getSession(), "Peer address mismatches: "
                        + ctx.getSession().getRemoteAddress() + " (expected: " + replica.getAddress() );
                    ctx.getSession().close();
                    return;
                }
            }
        }

        SessionLog.warn( ctx.getSession(), "Unknown peer replica ID: " + message.getReplicaId() );
        ctx.getSession().close();
    }


    private void beginReplication( ReplicationContext ctx )
    {
        // Initiate replication process asking update vector.
        ctx.getSession().write( new BeginLogEntriesMessage( ctx.getNextSequence() ) );
    }


    private void onLogEntryAck( ReplicationContext ctx, LogEntryAckMessage message ) throws Exception
    {
        if ( message.getResponseCode() != Constants.OK )
        {
            SessionLog.warn( ctx.getSession(), "Remote peer failed to execute a log entry." );
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
            SessionLog.warn( ctx.getSession(), "Failed to get update vector.", e );
            ctx.getSession().close();
            return;
        }

        // Do full-DIT transfer if the peer is new and I'm not new.
        try
        {
            if ( myPV.size() > 0 && yourUV.size() == 0 )
            {
                SessionLog.warn( ctx.getSession(), "Starting a whole DIT transfer." );
                sendAllEntries( ctx );
            }
            else
            {
                SessionLog.warn( ctx.getSession(), "Starting a partial replication log transfer." );
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
        Attributes rootDSE = ctx.getServiceConfiguration().getPartitionNexus().getRootDSE();

        Attribute namingContextsAttr = rootDSE.get( "namingContexts" );
        if ( namingContextsAttr == null || namingContextsAttr.size() == 0 )
        {
            SessionLog.warn( ctx.getSession(), "No namingContexts attributes in rootDSE." );
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

            SessionLog.info( ctx.getSession(), "Sending entries under '" + contextName + '\'' );

            Map mapping = ctx.getServiceConfiguration().getGlobalRegistries().getAttributeTypeRegistry()
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
        NamingEnumeration e = ctx.getServiceConfiguration().getPartitionNexus().search( contextName,
            ctx.getServiceConfiguration().getEnvironment(),
            new PresenceNode( org.apache.directory.mitosis.common.Constants.OBJECT_CLASS_OID ), ctrl );

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
                CSN csn = null;
                try
                {
                    csn = new SimpleCSN( String.valueOf( entryCSNAttr.get() ) );
                }
                catch ( IllegalArgumentException ex )
                {
                    SessionLog.warn( ctx.getSession(), "An entry with improper entryCSN: " + sr.getName() );
                    continue;
                }

                // Convert the entry into AddEntryOperation log.
                Operation op = new AddEntryOperation( csn, new LdapDN( sr.getName() ), attrs );

                // Send a LogEntry message for the entry.
                ctx.getSession().write( new LogEntryMessage( ctx.getNextSequence(), op ) );
            }
        }
        finally
        {
            e.close();
        }
    }


    private void sendReplicationLogs( ReplicationContext ctx, CSNVector myPV, CSNVector yourUV )
    {
        Iterator i = myPV.getReplicaIds().iterator();
        while ( i.hasNext() )
        {
            ReplicaId replicaId = ( ReplicaId ) i.next();
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
                ctx.getSession().write( new LogEntryMessage( ctx.getNextSequence(), op ) );
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
