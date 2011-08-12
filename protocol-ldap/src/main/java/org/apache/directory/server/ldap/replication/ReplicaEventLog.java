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


import java.io.File;
import java.io.IOException;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmTable;
import org.apache.directory.server.core.partition.impl.btree.jdbm.StringSerializer;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A structure storing the configuration on each consumer registered on a producer. It stores 
 * the following informations :
 * <ul>
 * <li>replicaId : the internal ID associated with the consumer</li>
 * <li>hostname : the consumer's host</li>
 * <li>searchFilter : the filter</li>
 * <li>lastSentCsn : the last CSN sent by the consumer</li>
 * <li>refreshNPersist : a flag indicating that the consumer is processing in Refresh and presist mode</li>
 * <li></li>
 * </ul>
 * A separate log is maintained for each syncrepl consumer.<br/>
 * We also associate a Queue with each structure, which will store the messages to send to the consumer.
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
    /** The Journal */
    private JdbmTable<String, ReplicaEventMessage> journal;

    /** the underlying file  */
    private File journalFile;

    /** The record manager*/
    private RecordManager recman;

    /** A flag used to indicate that the consumer is not up to date */
    private volatile boolean dirty;


    /**
     * Creates a new instance of EventLog for a replica
     * @param replicaId The replica ID
     */
    public ReplicaEventLog( DirectoryService directoryService, int replicaId ) throws IOException
    {
        SchemaManager schemaManager = directoryService.getSchemaManager();
        this.replicaId = replicaId;
        this.searchCriteria = new NotificationCriteria();
        this.searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );

        // Create the journal file, or open it of it exists
        File logDir = directoryService.getInstanceLayout().getLogDirectory();
        journalFile = new File( logDir, "journalRepl." + replicaId );
        recman = new BaseRecordManager( journalFile.getAbsolutePath() );

        SerializableComparator<String> comparator = new SerializableComparator<String>( SchemaConstants.CSN_ORDERING_MATCH_MR_OID );
        comparator.setSchemaManager( schemaManager );
        
        journal = new JdbmTable<String, ReplicaEventMessage>( schemaManager, "replication", recman, comparator, 
            new StringSerializer(), new ReplicaEventMessageSerializer( schemaManager ) );
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
            LOG.debug( "logging entry with Dn {} with the event {}", message.getEntry().getDn(), message.getChangeType() );

            String entryCsn = message.getEntry().get( SchemaConstants.ENTRY_CSN_AT ).getString();
            journal.put( entryCsn, message );
            journal.sync();
            recman.commit();
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
     * @throws Exception If the queue can't be deleted
     */
    public void truncate() throws Exception
    {
    }


    /**
     * Re-create the queue
     * @throws Exception If the creation has failed
     */
    public void recreate() throws Exception
    {
        LOG.debug( "recreating the queue for the replica id {}", replicaId );
    }


    /**
     * Stop the EventLog
     * 
     * @throws Exception If the stop failed
     */
    public void stop() throws Exception
    {
        // Close the producer and session, DO NOT close connection 
        if ( journal != null )
        {
            journal.close();
        }

        journal = null;

        if ( recman != null )
        {
            recman.close();
        }

        recman = null;
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
     * @param consumerCsn the consumer's CSN extracted from cookie
     * @return A cursor on top of the queue
     * @throws Exception If the cursor can't be created
     */
    public Cursor<Tuple<String, ReplicaEventMessage>> getCursor( String consumerCsn ) throws Exception
    {
        Cursor<Tuple<String, ReplicaEventMessage>> cursor = journal.cursor( consumerCsn );
        
        return cursor;
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
