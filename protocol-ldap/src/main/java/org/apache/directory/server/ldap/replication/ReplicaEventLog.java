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

package org.apache.directory.server.ldap.replication;


import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.ActiveMQSession;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.Queue;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A message log used for storing the changes done on DIT on a syncrepl consumer's search base
 * A separate log is maintained for each syncrepl consumer  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventLog
{

    /** IP address of the syncrepl consumer */
    private String hostName;

    /** the unmodified search filter as it was when received from the client */
    private String searchFilter;

    /** the csn that was sent to the client during the last sync session*/
    private String lastSentCsn;

    /** the persistent listener */
    private SyncReplSearchListener persistentListener;

    /** notification criteria used by the persistent sea*/
    private NotificationCriteria searchCriteria;

    /** the replica id */
    private int replicaId;

    /** flag indicating refreshAndPersist mode */
    private boolean refreshNPersist;

    // fields that won't be serialized

    /** the ActiveMQ session */
    private ActiveMQSession amqSession;

    /** the Queue used for storing messages */
    private ActiveMQQueue queue;

    /** message producer for Queue */
    private ActiveMQMessageProducer producer;

    /** the messaging system's connection */
    private ActiveMQConnection amqConnection;

    /** ActiveMQ's BrokerService */
    private BrokerService brokerService;

    private volatile boolean dirty;

    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLog.class );


    public ReplicaEventLog()
    {

    }


    public ReplicaEventLog( int replicaId )
    {
        this.replicaId = replicaId;
        this.searchCriteria = new NotificationCriteria();
        this.searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
    }


    /**
     * instantiates a message queue and corresponding producer for storing DIT changes  
     *
     * @param amqConnection ActiveMQ connection
     * @param brokerService ActiveMQ's broker service
     * @throws Exception
     */
    public void configure( final ActiveMQConnection amqConnection, final BrokerService brokerService ) throws Exception
    {
        if ( ( amqSession == null ) || !amqSession.isRunning() )
        {
            this.amqConnection = amqConnection;
            amqSession = ( ActiveMQSession ) amqConnection.createSession( false, ActiveMQSession.AUTO_ACKNOWLEDGE );
            queue = ( ActiveMQQueue ) amqSession.createQueue( getQueueName() );
            producer = ( ActiveMQMessageProducer ) amqSession.createProducer( queue );
            this.brokerService = brokerService;
        }
    }


    /**
     * stores the given EventType and Entry in the queue 
     *
     * @param event the EventType
     * @param entry the modified Entry
     */
    public void log( EventType event, Entry entry )
    {
        LOG.debug( "logging entry with Dn {} with the event {}", entry.getDn(), event );
        log( new ReplicaEventMessage( event, entry ) );
    }


    public void log( ReplicaEventMessage message )
    {
        try
        {
            ActiveMQObjectMessage ObjectMessage = ( ActiveMQObjectMessage ) amqSession.createObjectMessage();
            ObjectMessage.setObject( message );
            producer.send( ObjectMessage );
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to insert the entry into syncrepl log", e );
        }
    }


    /**
     * deletes the queue (to remove the log) and recreates a new queue instance
     * with the same queue name. Also creates the corresponding message producer
     *
     * @throws Exception
     */
    public void truncate() throws Exception
    {
        producer.close();

        String queueName = queue.getQueueName();
        LOG.debug( "deleting the queue {}", queueName );
        amqConnection.destroyDestination( queue );
        queue = null;
    }


    public void recreate() throws Exception
    {
        LOG.debug( "recreating the queue for the replica id {}", replicaId );
        queue = ( ActiveMQQueue ) amqSession.createQueue( getQueueName() );
        producer = ( ActiveMQMessageProducer ) amqSession.createProducer( queue );
    }


    public void stop() throws Exception
    {
        // then close the producer and session, DO NOT close connection 
        producer.close();
        amqSession.close();
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof ReplicaEventLog ) )
        {
            return false;
        }

        ReplicaEventLog other = ( ReplicaEventLog ) obj;

        if ( replicaId != other.getId() )
        {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + searchFilter.hashCode();
        result = prime * result + hostName.hashCode();
        return result;
    }


    public int compareTo( ReplicaEventLog o )
    {
        if ( this.equals( o ) )
        {
            return 0;
        }

        return 1;
    }


    public SyncReplSearchListener getPersistentListener()
    {
        return persistentListener;
    }


    public void setPersistentListener( SyncReplSearchListener persistentListener )
    {
        this.persistentListener = persistentListener;
    }


    public NotificationCriteria getSearchCriteria()
    {
        return searchCriteria;
    }


    public void setSearchCriteria( NotificationCriteria searchCriteria )
    {
        this.searchCriteria = searchCriteria;
    }


    public boolean isRefreshNPersist()
    {
        return refreshNPersist;
    }


    public void setRefreshNPersist( boolean refreshNPersist )
    {
        this.refreshNPersist = refreshNPersist;
    }


    public int getId()
    {
        return replicaId;
    }


    public String getLastSentCsn()
    {
        return lastSentCsn;
    }


    public void setLastSentCsn( String lastSentCsn )
    {
        // set only if there is a change in cookie value
        // this will avoid setting the dirty flag which eventually is used for
        // storing the details of this log
        if ( !lastSentCsn.equals( this.lastSentCsn ) )
        {
            this.lastSentCsn = lastSentCsn;
            dirty = true;
        }
    }


    public String getHostName()
    {
        return hostName;
    }


    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }


    public String getSearchFilter()
    {
        return searchFilter;
    }


    public void setSearchFilter( String searchFilter )
    {
        this.searchFilter = searchFilter;
    }


    public boolean isDirty()
    {
        return dirty;
    }


    public void setDirty( boolean dirty )
    {
        this.dirty = dirty;
    }


    public String getQueueName()
    {
        return "replicaId=" + replicaId;
    }


    public ReplicaEventLogCursor getCursor() throws Exception
    {
        Queue regionQueue = ( Queue ) brokerService.getRegionBroker().getDestinationMap().get( queue );
        
        return new ReplicaEventLogCursor( amqSession, queue, regionQueue );
    }


    @Override
    public String toString()
    {
        return "ClientMessageQueueLog [ipAddress=" + hostName + ", filter=" + searchFilter + ", replicaId=" + replicaId
            + ", lastSentCookie=" + lastSentCsn + "]";
    }
}
