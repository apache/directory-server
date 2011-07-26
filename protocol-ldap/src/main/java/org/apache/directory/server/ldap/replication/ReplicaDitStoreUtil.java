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
import org.apache.directory.server.core.LdapCoreSessionConnection;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.EntryCursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
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
    private CoreSession adminSession;

    private SchemaManager schemaManager;

    private static final String REPL_CONSUMER_DN = "ou=consumers,ou=system";

    private static final Logger LOG = LoggerFactory.getLogger( ReplicaDitStoreUtil.class );

    private Map<Integer, List<Modification>> modMap = new HashMap<Integer, List<Modification>>();

    private LdapCoreSessionConnection coreConnection;


    public ReplicaDitStoreUtil( DirectoryService dirService ) throws Exception
    {
        this.adminSession = dirService.getAdminSession();
        this.schemaManager = dirService.getSchemaManager();
        coreConnection = new LdapCoreSessionConnection( adminSession );

        init();
    }


    /**
     * Initialize the replication Store, creating the pu=consumers,ou=system entry
     */
    private void init() throws Exception
    {
        Dn replConsumerDn = new Dn( schemaManager, REPL_CONSUMER_DN );

        if ( !adminSession.exists( replConsumerDn ) )
        {
            LOG.debug( "creating the entry for storing replication consumers' details" );
            
            Entry entry = new DefaultEntry( schemaManager , replConsumerDn,
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

        Dn replicaDn = new Dn( schemaManager, "ads-dsReplicaId=" + replica.getId() + "," + REPL_CONSUMER_DN );
        Entry entry = new DefaultEntry( schemaManager, replicaDn,
            SchemaConstants.OBJECT_CLASS_AT, "ads-replEventLog",
            "ads-dsReplicaId", String.valueOf( replica.getId() ),
            "ads-replAliasDerefMode", replica.getSearchCriteria().getAliasDerefMode().getJndiValue(),
            "ads-searchBaseDN", replica.getSearchCriteria().getBase().getName(),
            "ads-replLastSentCsn", replica.getLastSentCsn(),
            "ads-replSearchScope", replica.getSearchCriteria().getScope().getLdapUrlValue(),
            "ads-replRefreshNPersist", String.valueOf( replica.isRefreshNPersist() ),
            "ads-replSearchFilter", replica.getSearchFilter() );

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
                .lookupAttributeTypeRegistry( "ads-replLastSentCsn" ) );
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

        Dn dn = new Dn( schemaManager, "ads-dsReplicaId=" + replica.getId() + "," + REPL_CONSUMER_DN );
        adminSession.modify( dn, mods );
        LOG.debug( "updated last sent CSN of consumer entry {}", dn );
    }


    public List<ReplicaEventLog> getReplicaEventLogs() throws Exception
    {
        List<ReplicaEventLog> replicas = new ArrayList<ReplicaEventLog>();

        EntryCursor cursor = coreConnection.search( REPL_CONSUMER_DN, "(objectClass=ads-replEventLog)",
            SearchScope.ONELEVEL, "+", "*" );

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
        String id = entry.get( "ads-dsReplicaId" ).getString();
        ReplicaEventLog replica = new ReplicaEventLog( Integer.parseInt( id ) );

        NotificationCriteria searchCriteria = new NotificationCriteria();

        String aliasMode = entry.get( "ads-replAliasDerefMode" ).getString();
        searchCriteria.setAliasDerefMode( AliasDerefMode.getDerefMode( aliasMode ) );

        String baseDn = entry.get( "ads-searchBaseDN" ).getString();
        searchCriteria.setBase( new Dn( schemaManager, baseDn ) );

        Attribute lastSentCsnAt = entry.get( "ads-replLastSentCsn" );
        
        if ( lastSentCsnAt != null )
        {
            replica.setLastSentCsn( lastSentCsnAt.getString() );
        }

        String scope = entry.get( "ads-replSearchScope" ).getString();
        int scopeIntVal = SearchScope.getSearchScope( scope );
        searchCriteria.setScope( SearchScope.getSearchScope( scopeIntVal ) );

        String filter = entry.get( "ads-replSearchFilter" ).getString();
        searchCriteria.setFilter( filter );
        replica.setSearchFilter( filter );

        replica.setRefreshNPersist( Boolean.parseBoolean( entry.get( "ads-replRefreshNPersist" ).getString() ) );
        
        searchCriteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
        replica.setSearchCriteria( searchCriteria );

        // explicitly mark the replica as not-dirty, cause we just loaded it from 
        // the store, this prevents updating the replica info immediately after loading
        replica.setDirty( false );

        return replica;
    }
}
