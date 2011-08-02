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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A structure storing the configuration on each consumer registred on a producer. It stores 
 * the following informations :
 * <ul>
 * <li>replicaId : the internal ID associated with the consumer</li>
 * <li>hostname : the consumer's host</li>
 * <li>searchFilter : the filter</li>
 * <li>lastSentCsn : the last CSN sent by the consumer</li>
 * <li>refreshNPersist : a flag indicating that the consumer is processing in Refresh and presist mode</li>
 * <li></li>
 * </ul>
 * A separate log is maintained for each syncrepl consumer  
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaEventLog implements Comparable<ReplicaEventLog>
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaEventLog.class );
    
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

    /** A flag used to indicate that the consumer is not up to date */
    private volatile boolean dirty;


    /**
     * Create a new instance of EventLog for a replica
     * @param replicaId The replica ID
     */
    public ReplicaEventLog( int replicaId )
    {
        this.replicaId = replicaId;
        this.searchCriteria = new NotificationCriteria();
        this.searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
    }


    /**
     * Instantiates a message queue and corresponding producer for storing DIT changes  
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
     * Stores the given message in the queue 
     *
     * @param message The message to store
     */
    public void log( ReplicaEventMessage message )
    {
        try
        {
            LOG.debug( "logging entry with Dn {} with the event {}", message.getEntry().getDn(), message.getEventType() );
            
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
     * Deletes the queue (to remove the log) and recreates a new queue instance
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


    /**
     * Re-create the queue
     * @throws Exception If the creation has failed
     */
    public void recreate() throws Exception
    {
        LOG.debug( "recreating the queue for the replica id {}", replicaId );
        queue = ( ActiveMQQueue ) amqSession.createQueue( getQueueName() );
        producer = ( ActiveMQMessageProducer ) amqSession.createProducer( queue );
    }


    /**
     * Stop the EventLog
     * 
     * @throws Exception If the stop failed
     */
    public void stop() throws Exception
    {
        // then close the producer and session, DO NOT close connection 
        producer.close();
        amqSession.close();
    }


    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + searchFilter.hashCode();
        result = 31 * result + hostName.hashCode();
        
        return result;
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo( ReplicaEventLog o )
    {
        if ( this.equals( o ) )
        {
            return 0;
        }

        return 1;
    }


    /**
     * @return The listener
     */
    public SyncReplSearchListener getPersistentListener()
    {
        return persistentListener;
    }


    /**
     * Set the listener
     * @param persistentListener The listener
     */
    public void setPersistentListener( SyncReplSearchListener persistentListener )
    {
        this.persistentListener = persistentListener;
    }


    /**
     * @return The search criteria
     */
    public NotificationCriteria getSearchCriteria()
    {
        return searchCriteria;
    }


    /**
     * Stores the search criteria
     * @param searchCriteria The search criteria
     */
    public void setSearchCriteria( NotificationCriteria searchCriteria )
    {
        this.searchCriteria = searchCriteria;
    }


    /**
     * @return true if the consumer is in Refresh And Persist mode
     */
    public boolean isRefreshNPersist()
    {
        return refreshNPersist;
    }


    /**
     * @param refreshNPersist if true, set the EventLog in Refresh and Persist mode
     */
    public void setRefreshNPersist( boolean refreshNPersist )
    {
        this.refreshNPersist = refreshNPersist;
    }


    /**
     * @return The replica ID
     */
    public int getId()
    {
        return replicaId;
    }


    /**
     * @return The last CSN sent by the consumer
     */
    public String getLastSentCsn()
    {
        return lastSentCsn;
    }


    /**
     * Update the last Sent CSN. If it's different from the present one, we
     * will set the dirty flag to true, and a replication will follow.
     *  
     * @param lastSentCsn The new Sent CSN
     */
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


    /**
     * @return The consumer Hostname
     */
    public String getHostName()
    {
        return hostName;
    }


    /**
     * Set the consumer hostname
     * @param hostName The consumer hostname
     */
    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }


    /**
     * @return The searchFilter
     */
    public String getSearchFilter()
    {
        return searchFilter;
    }


    /**
     * Set the searchFilter
     * @param searchFilter The searchFilter
     */
    public void setSearchFilter( String searchFilter )
    {
        this.searchFilter = searchFilter;
    }


    /**
     * @return True if the consumer is not up to date
     */
    public boolean isDirty()
    {
        return dirty;
    }


    /**
     * Set the dirty flag
     * @param dirty The current consumer status
     */
    public void setDirty( boolean dirty )
    {
        this.dirty = dirty;
    }


    /**
     * @return The queue name
     */
    public String getQueueName()
    {
        return "replicaId=" + replicaId;
    }


    /**
     * @return A cursor on top of the queue
     * @throws Exception If the cursor can't be created
     */
    public ReplicaEventLogCursor getCursor() throws Exception
    {
        Queue regionQueue = ( Queue ) brokerService.getRegionBroker().getDestinationMap().get( queue );
        
        return new ReplicaEventLogCursor( amqSession, queue, regionQueue );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "ClientMessageQueueLog [ipAddress=" + hostName + ", filter=" + searchFilter + ", replicaId=" + replicaId
            + ", lastSentCookie=" + lastSentCsn + "]";
    }
}
