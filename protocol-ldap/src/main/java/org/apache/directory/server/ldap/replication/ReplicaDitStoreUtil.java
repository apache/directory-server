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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO ReplicaDitStoreUtil.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicaDitStoreUtil
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicaDitStoreUtil.class );

    /** The admin session used to commuicate with the backend */
    private CoreSession adminSession;

    /** The schema manager instance */
    private SchemaManager schemaManager;

    /** The replication factory DN */
    private static final String REPL_CONSUMER_DN_STR = "ou=consumers,ou=system";
    private static Dn REPL_CONSUMER_DN;

    /** An ObjectClass AT instance */
    private static AttributeType OBJECT_CLASS_AT;

    private Map<Integer, List<Modification>> modMap = new HashMap<Integer, List<Modification>>();


    public ReplicaDitStoreUtil( DirectoryService dirService ) throws Exception
    {
        adminSession = dirService.getAdminSession();
        schemaManager = dirService.getSchemaManager();
        REPL_CONSUMER_DN = dirService.getDnFactory().create( REPL_CONSUMER_DN_STR );
        OBJECT_CLASS_AT = dirService.getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );

        init();
    }


    /**
     * Initialize the replication Store, creating the ou=consumers,ou=system entry
     */
    private void init() throws Exception
    {
        if ( !adminSession.exists( REPL_CONSUMER_DN ) )
        {
            LOG.debug( "creating the entry for storing replication consumers' details" );
            
            Entry entry = new DefaultEntry( schemaManager , REPL_CONSUMER_DN,
                SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ORGANIZATIONAL_UNIT_OC,
                SchemaConstants.OU_AT, "consumers" );

            adminSession.add( entry );
        }
    }


    /**
     * Add a new consumer entry
     * 
     * @param replica
     * @throws Exception
     */
    public void addConsumerEntry( ReplicaEventLog replica ) throws Exception
    {
        if ( replica == null )
        {
            return;
        }

        Dn replicaDn = new Dn( schemaManager, SchemaConstants.ADS_DS_REPLICA_ID + "=" + replica.getId() + "," + REPL_CONSUMER_DN );
        Entry entry = new DefaultEntry( schemaManager, replicaDn,
            SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ADS_REPL_EVENT_LOG,
            SchemaConstants.ADS_DS_REPLICA_ID, String.valueOf( replica.getId() ),
            SchemaConstants.ADS_REPL_ALIAS_DEREF_MODE, replica.getSearchCriteria().getAliasDerefMode().getJndiValue(),
            SchemaConstants.ADS_SEARCH_BASE_DN, replica.getSearchCriteria().getBase().getName(),
            SchemaConstants.ADS_REPL_LAST_SENT_CSN, replica.getLastSentCsn(),
            SchemaConstants.ADS_REPL_SEARCH_SCOPE, replica.getSearchCriteria().getScope().getLdapUrlValue(),
            SchemaConstants.ADS_REPL_REFRESH_N_PERSIST, String.valueOf( replica.isRefreshNPersist() ),
            SchemaConstants.ADS_REPL_SEARCH_FILTER, replica.getSearchFilter() );

        adminSession.add( entry );
        LOG.debug( "stored replication consumer entry {}", entry.getDn() );
    }


    public void updateReplicaLastSentCsn( ReplicaEventLog replica ) throws Exception
    {
        List<Modification> mods = modMap.get( replica.getId() );
        Attribute lastSentCsnAt = null;
        
        if ( mods == null )
        {
            lastSentCsnAt = new DefaultAttribute( schemaManager
                .lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_LAST_SENT_CSN ) );
            lastSentCsnAt.add( replica.getLastSentCsn() );

            Modification mod = new DefaultModification();
            mod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
            mod.setAttribute( lastSentCsnAt );

            mods = new ArrayList<Modification>( 1 );
            mods.add( mod );
        }
        else
        {
            lastSentCsnAt = mods.get( 0 ).getAttribute();
            lastSentCsnAt.clear(); // clearing is mandatory
            lastSentCsnAt.add( replica.getLastSentCsn() );
        }

        Dn dn = new Dn( schemaManager, SchemaConstants.ADS_DS_REPLICA_ID + "=" + replica.getId() + "," + REPL_CONSUMER_DN );
        adminSession.modify( dn, mods );
        LOG.debug( "updated last sent CSN of consumer entry {}", dn );
    }


    public List<ReplicaEventLog> getReplicaEventLogs() throws Exception
    {
        List<ReplicaEventLog> replicas = new ArrayList<ReplicaEventLog>();

        ExprNode filter = new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( SchemaConstants.ADS_REPL_EVENT_LOG ) );
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( REPL_CONSUMER_DN );
        searchRequest.setScope( SearchScope.ONELEVEL );
        searchRequest.setFilter( filter );
        searchRequest.addAttributes( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES );
        
        EntryFilteringCursor cursor = adminSession.search( searchRequest );

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            ReplicaEventLog replica = convertEntryToReplica( entry );
            replicas.add( replica );
        }
        
        cursor.close();

        return replicas;
    }


    private ReplicaEventLog convertEntryToReplica( Entry entry ) throws Exception
    {
        String id = entry.get( SchemaConstants.ADS_DS_REPLICA_ID ).getString();
        ReplicaEventLog replica = new ReplicaEventLog( Integer.parseInt( id ) );

        NotificationCriteria searchCriteria = new NotificationCriteria();

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

        replica.setRefreshNPersist( Boolean.parseBoolean( entry.get( SchemaConstants.ADS_REPL_REFRESH_N_PERSIST ).getString() ) );
        
        searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
        replica.setSearchCriteria( searchCriteria );

        // explicitly mark the replica as not-dirty, cause we just loaded it from 
        // the store, this prevents updating the replica info immediately after loading
        replica.setDirty( false );

        return replica;
    }
}
