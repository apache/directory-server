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

package org.apache.directory.server.ldap.replication.provider;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manage the consumers on the provider : add them, and remove them.
 * 
 * All the consumers configuration will be stored in the 'ou=consumers,ou=system' branch.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplConsumerManager
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ReplConsumerManager.class );

    /** A logger for the replication provider */
    private static final Logger PROVIDER_LOG = LoggerFactory.getLogger( Loggers.PROVIDER_LOG.getName() );

    /** The admin session used to commuicate with the backend */
    private CoreSession adminSession;

    /** The DirectoryService instance */
    private DirectoryService directoryService;

    /** The schema manager instance */
    private SchemaManager schemaManager;

    /** The replication factory DN */
    private static final String REPL_CONSUMER_DN_STR = "ou=consumers,ou=system";
    private Dn replConsumerDn;

    /** The consumers' ou value */
    private static final String CONSUMERS = "consumers";

    /** An AdsReplLastSentCsn AT instance */
    private AttributeType adsReplLastSentCsn;

    /** A map containing the last sent CSN for every connected consumer */
    private Map<Integer, Modification> modMap = new ConcurrentHashMap<Integer, Modification>();


    /**
     * Create a new instance of the producer replication manager.
     * 
     * @param directoryService The directoryService instance
     * @throws Exception if we add an error while creating the configuration
     */
    public ReplConsumerManager( DirectoryService directoryService ) throws Exception
    {
        this.directoryService = directoryService;
        adminSession = directoryService.getAdminSession();
        schemaManager = directoryService.getSchemaManager();
        replConsumerDn = directoryService.getDnFactory().create( REPL_CONSUMER_DN_STR );
        adsReplLastSentCsn = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_LAST_SENT_CSN );

        PROVIDER_LOG.debug( "Starting the replication consumer manager" );
        createConsumersBranch();
    }


    /**
     * Initialize the replication Store, creating the ou=consumers,ou=system entry (only if it does not exist yet)
     */
    private void createConsumersBranch() throws Exception
    {
        if ( !adminSession.exists( replConsumerDn ) )
        {
            LOG.debug( "creating the entry for storing replication consumers' details" );
            PROVIDER_LOG
                .debug( "Creating the entry for storing replication consumers' details in {}", replConsumerDn );

            Entry entry = new DefaultEntry( schemaManager, replConsumerDn,
                SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ORGANIZATIONAL_UNIT_OC,
                SchemaConstants.OU_AT, CONSUMERS );

            adminSession.add( entry );
        }
    }


    /**
     * Add a new consumer entry in ou=consumers,ou=system
     * 
     * @param replica The added consumer replica
     * @throws Exception If the addition failed
     */
    public void addConsumerEntry( ReplicaEventLog replica ) throws Exception
    {
        if ( replica == null )
        {
            // No consumer ? Get out...
            return;
        }

        PROVIDER_LOG.debug( "Adding a consumer for replica {}", replica.toString() );

        // Check that we don't already have an entry for this consumer
        Dn consumerDn = directoryService.getDnFactory().create(
            SchemaConstants.ADS_DS_REPLICA_ID + "=" + replica.getId() + "," + replConsumerDn );

        if ( adminSession.exists( consumerDn ) )
        {
            // Error...
            String message = "The replica " + consumerDn.getName() + " already exists";
            LOG.error( message );
            PROVIDER_LOG.error( message );
            throw new LdapEntryAlreadyExistsException( message );
        }

        // Create the new consumer entry
        Entry entry = new DefaultEntry( schemaManager, consumerDn,
            SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ADS_REPL_EVENT_LOG,
            SchemaConstants.ADS_DS_REPLICA_ID, String.valueOf( replica.getId() ),
            SchemaConstants.ADS_REPL_ALIAS_DEREF_MODE, replica.getSearchCriteria().getAliasDerefMode().getJndiValue(),
            SchemaConstants.ADS_SEARCH_BASE_DN, replica.getSearchCriteria().getBase().getName(),
            SchemaConstants.ADS_REPL_LAST_SENT_CSN, replica.getLastSentCsn(),
            SchemaConstants.ADS_REPL_SEARCH_SCOPE, replica.getSearchCriteria().getScope().getLdapUrlValue(),
            SchemaConstants.ADS_REPL_REFRESH_N_PERSIST, String.valueOf( replica.isRefreshNPersist() ),
            SchemaConstants.ADS_REPL_SEARCH_FILTER, replica.getSearchFilter(),
            SchemaConstants.ADS_REPL_LOG_MAX_IDLE, String.valueOf( replica.getMaxIdlePeriod() ),
            SchemaConstants.ADS_REPL_LOG_PURGE_THRESHOLD_COUNT, String.valueOf( replica.getPurgeThresholdCount() ) );

        adminSession.add( entry );

        replica.setConsumerEntryDn( consumerDn );

        LOG.debug( "stored replication consumer entry {}", consumerDn );
    }


    /**
     * Delete an existing consumer entry from ou=consumers,ou=system
     * 
     * @param replica The added consumer replica
     * @throws Exception If the addition failed
     */
    public void deleteConsumerEntry( ReplicaEventLog replica ) throws LdapException
    {
        if ( replica == null )
        {
            // No consumer ? Get out...
            return;
        }

        // Check that we have an entry for this consumer
        Dn consumerDn = directoryService.getDnFactory().create(
            SchemaConstants.ADS_DS_REPLICA_ID + "=" + replica.getId() + "," + replConsumerDn );

        PROVIDER_LOG.debug( "Trying to delete the consumer entry {}", consumerDn );

        if ( !adminSession.exists( consumerDn ) )
        {
            // Error...
            String message = "The replica " + consumerDn.getName() + " does not exist";
            LOG.error( message );
            PROVIDER_LOG.debug( message );
            return;
        }

        // Delete the consumer entry
        adminSession.delete( consumerDn );

        LOG.debug( "Deleted replication consumer entry {}", consumerDn );
    }


    /**
     * Store the new CSN sent by the consumer in place of the previous one.
     * 
     * @param replica The consumer informations
     * @throws Exception If the update failed
     */
    public void updateReplicaLastSentCsn( ReplicaEventLog replica ) throws Exception
    {
        Modification mod = modMap.get( replica.getId() );
        Attribute lastSentCsnAt = null;

        if ( mod == null )
        {
            mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, adsReplLastSentCsn,
                replica.getLastSentCsn() );

            modMap.put( replica.getId(), mod );
        }
        else
        {
            lastSentCsnAt = mod.getAttribute();
            lastSentCsnAt.clear(); // clearing is mandatory
            lastSentCsnAt.add( replica.getLastSentCsn() );
        }

        Dn dn = directoryService.getDnFactory().create(
            SchemaConstants.ADS_DS_REPLICA_ID + "=" + replica.getId() + "," + replConsumerDn );
        adminSession.modify( dn, mod );

        LOG.debug( "updated last sent CSN of consumer entry {}", dn );
        PROVIDER_LOG.debug( "updated the LastSentCSN of consumer entry {}", dn );
    }


    /**
     * Get the list of consumers' configuration
     * 
     * @return A list of all the consumer configuration stored on the provider
     * @throws Exception If we had an error while building this list
     */
    public List<ReplicaEventLog> getReplicaEventLogs() throws Exception
    {
        List<ReplicaEventLog> replicas = new ArrayList<ReplicaEventLog>();

        // Search for all the consumers
        ExprNode filter = new EqualityNode<String>( directoryService.getAtProvider().getObjectClass(), 
            new Value( directoryService.getAtProvider().getObjectClass(), SchemaConstants.ADS_REPL_EVENT_LOG ) );
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( replConsumerDn );
        searchRequest.setScope( SearchScope.ONELEVEL );
        searchRequest.setFilter( filter );
        searchRequest.addAttributes( SchemaConstants.ALL_ATTRIBUTES_ARRAY );

        Cursor<Entry> cursor = adminSession.search( searchRequest );

        // Now loop on each consumer configuration
        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            ReplicaEventLog replica = convertEntryToReplica( entry );
            replicas.add( replica );
        }

        cursor.close();

        // Now, we can return the list of replicas
        return replicas;
    }


    /**
     * Convert the stored entry to a valid ReplicaEventLog structure
     */
    private ReplicaEventLog convertEntryToReplica( Entry entry ) throws Exception
    {
        String id = entry.get( SchemaConstants.ADS_DS_REPLICA_ID ).getString();
        ReplicaEventLog replica = new ReplicaEventLog( directoryService, Integer.parseInt( id ) );

        NotificationCriteria searchCriteria = new NotificationCriteria( schemaManager );

        String aliasMode = entry.get( SchemaConstants.ADS_REPL_ALIAS_DEREF_MODE ).getString();
        searchCriteria.setAliasDerefMode( AliasDerefMode.getDerefMode( aliasMode ) );

        String baseDn = entry.get( SchemaConstants.ADS_SEARCH_BASE_DN ).getString();
        searchCriteria.setBase( new Dn( schemaManager, baseDn ) );

        Attribute lastSentCsnAt = entry.get( SchemaConstants.ADS_REPL_LAST_SENT_CSN );

        if ( lastSentCsnAt != null )
        {
            replica.setLastSentCsn( lastSentCsnAt.getString() );
        }

        String scope = entry.get( SchemaConstants.ADS_REPL_SEARCH_SCOPE ).getString();
        int scopeIntVal = SearchScope.getSearchScope( scope );
        searchCriteria.setScope( SearchScope.getSearchScope( scopeIntVal ) );

        String filter = entry.get( SchemaConstants.ADS_REPL_SEARCH_FILTER ).getString();
        searchCriteria.setFilter( filter );
        replica.setSearchFilter( filter );

        replica.setRefreshNPersist( Boolean.parseBoolean( entry.get( SchemaConstants.ADS_REPL_REFRESH_N_PERSIST )
            .getString() ) );

        searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
        replica.setSearchCriteria( searchCriteria );

        int maxIdlePeriod = Integer.parseInt( entry.get( SchemaConstants.ADS_REPL_LOG_MAX_IDLE ).getString() );
        replica.setMaxIdlePeriod( maxIdlePeriod );

        int purgeThreshold = Integer.parseInt( entry.get( SchemaConstants.ADS_REPL_LOG_PURGE_THRESHOLD_COUNT )
            .getString() );
        replica.setPurgeThresholdCount( purgeThreshold );

        // explicitly mark the replica as not-dirty, cause we just loaded it from 
        // the store, this prevents updating the replica info immediately after loading
        replica.setDirty( false );

        replica.setConsumerEntryDn( entry.getDn() );

        return replica;
    }
}
