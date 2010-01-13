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

package org.apache.directory.server.config;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used for reading the configuration present in a Partition
 * and instantiate the necessary objects like DirectoryService, Interceptors etc.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigPartitionReader
{

    /** the partition which holds the configuration data */
    private Partition configPartition;

    /** the search engine of the partition */
    private SearchEngine<ServerEntry> se;

    private SchemaManager schemaManager;
    
    private File workDir;
    
    private static final Logger LOG = LoggerFactory.getLogger( ConfigPartitionReader.class );


    /**
     * 
     * Creates a new instance of ConfigPartitionReader.
     *
     * @param configPartition the non null config partition
     */
    public ConfigPartitionReader( LdifPartition configPartition )
    {
        if ( configPartition == null )
        {
            throw new IllegalArgumentException( "Config partition cannot be null" );
        }

        if ( !configPartition.isInitialized() )
        {
            throw new IllegalStateException( "the config partition is not initialized" );
        }

        this.configPartition = configPartition;
        se = configPartition.getSearchEngine();
        this.schemaManager = configPartition.getSchemaManager();
        workDir = configPartition.getPartitionDir().getParentFile();        
    }


    /**
     * 
     * instantiates a DirectoryService based on the configuration present in the partition 
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public DirectoryService getDirectoryService() throws Exception
    {

        PresenceNode filter = new PresenceNode( "ads-directoryServiceId" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        IndexCursor cursor = se.cursor( configPartition.getSuffixDn(), AliasDerefMode.NEVER_DEREF_ALIASES, filter,
            controls );

        if ( !cursor.next() )
        {
            throw new Exception( "No directoryService instance was configured under the DN "
                + configPartition.getSuffixDn() );
        }

        ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor.get();
        cursor.close();

        ClonedServerEntry dsEntry = configPartition.lookup( forwardEntry.getId() );

        LOG.debug( "dirServiceEntry {}", dsEntry );

        DirectoryService dirService = new DefaultDirectoryService();
        // MUST attributes
        dirService.setInstanceId( getString( "ads-directoryServiceId", dsEntry ) );
        dirService.setReplicaId( getInt( "ads-dsReplicaId", dsEntry ) );

        LdapDN interceptorsDN = new LdapDN( dsEntry.get( "ads-dsInterceptors" ).getString() );
        interceptorsDN.normalize( configPartition.getSchemaManager().getNormalizerMapping() );
        List<Interceptor> interceptors = getInterceptors( interceptorsDN );
        dirService.setInterceptors( interceptors );

        LdapDN partitionsDN = new LdapDN( dsEntry.get( "ads-dsPartitions" ).getString() );
        partitionsDN.normalize( configPartition.getSchemaManager().getNormalizerMapping() );
        
        Map<String, Partition> partitions = getPartitions( partitionsDN );

        Partition system = partitions.remove( "system" );
        dirService.setSystemPartition( system );
        
        dirService.setPartitions( new HashSet( partitions.values() ) );
        
        // MAY attributes
        EntryAttribute acEnabledAttr = dsEntry.get( "ads-dsAccessControlEnabled" );
        if ( acEnabledAttr != null )
        {
            dirService.setAccessControlEnabled( Boolean.parseBoolean( acEnabledAttr.getString() ) );
        }

        EntryAttribute anonAccessAttr = dsEntry.get( "ads-dsAllowAnonymousAccess" );
        if ( anonAccessAttr != null )
        {
            dirService.setAllowAnonymousAccess( Boolean.parseBoolean( anonAccessAttr.getString() ) );
        }

        EntryAttribute changeLogAttr = dsEntry.get( "ads-dsChangeLog" );
        if ( changeLogAttr != null )
        {
            // configure CL
        }

        EntryAttribute denormAttr = dsEntry.get( "ads-dsDenormalizeOpAttrsEnabled" );
        if ( denormAttr != null )
        {
            dirService.setDenormalizeOpAttrsEnabled( Boolean.parseBoolean( denormAttr.getString() ) );
        }

        EntryAttribute journalAttr = dsEntry.get( "ads-dsJournal" );
        if ( journalAttr != null )
        {
            // dirService.setJournal(  );
        }

        EntryAttribute maxPduAttr = dsEntry.get( "ads-dsMaxPDUSize" );
        if ( maxPduAttr != null )
        {
            dirService.setMaxPDUSize( Integer.parseInt( maxPduAttr.getString() ) );
        }

        EntryAttribute passwordHidAttr = dsEntry.get( "ads-dsPasswordHidden" );
        if ( passwordHidAttr != null )
        {
            dirService.setPasswordHidden( Boolean.parseBoolean( passwordHidAttr.getString() ) );
        }

        EntryAttribute replAttr = dsEntry.get( "ads-dsReplication" );
        if ( replAttr != null )
        {
            // configure replication
        }

        EntryAttribute syncPeriodAttr = dsEntry.get( "ads-dsSyncPeriodMillis" );
        if ( syncPeriodAttr != null )
        {
            // FIXME the DirectoryService interface doesn't have this setter
            //dirService.setSyncPeriodMillis( Long.parseLong( syncPeriodAttr.getString() ) );
        }

        EntryAttribute testEntryAttr = dsEntry.get( "ads-dsTestEntries" );
        if ( testEntryAttr != null )
        {
            //process the test entries, should this be a FS location?
        }

        if ( !isEnabled( dsEntry ) )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
        }
        
        return dirService;
    }


    /**
     * reads the Interceptor configuration and instantiates them in the order specified
     *
     * @param interceptorsDN the DN under which interceptors are configured
     * @return a list of instantiated Interceptor objects
     * @throws Exception
     */
    private List<Interceptor> getInterceptors( LdapDN interceptorsDN ) throws Exception
    {
        PresenceNode filter = new PresenceNode( "ads-interceptorId" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        IndexCursor cursor = se.cursor( interceptorsDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );

        Set<InterceptorConfig> set = new TreeSet<InterceptorConfig>();

        while ( cursor.next() )
        {
            ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor.get();
            ServerEntry interceptorEntry = configPartition.lookup( forwardEntry.getId() );

            String id = getString( "ads-interceptorId", interceptorEntry );
            String fqcn = getString( "ads-interceptorClassName", interceptorEntry );
            int order = getInt( "ads-interceptorOrder", interceptorEntry );

            InterceptorConfig intConfig = new InterceptorConfig( id, fqcn, order );
            set.add( intConfig );
        }
    
        cursor.close();
        
        List<Interceptor> interceptors = new ArrayList<Interceptor>();

        for( InterceptorConfig iconfig : set )
        {
            try
            {
                LOG.debug( "loading the interceptor class {} and instantiating", iconfig.getFqcn() );
                Interceptor ic = ( Interceptor ) Class.forName( iconfig.getFqcn() ).newInstance();
                interceptors.add( ic );
            }
            catch( Exception e )
            {
                throw e;
            }
        }
        
        return interceptors;
    }


    private Map<String,Partition> getPartitions( LdapDN partitionsDN ) throws Exception
    {
        PresenceNode filter = new PresenceNode( "ads-partitionId" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        IndexCursor cursor = se.cursor( partitionsDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
        
        Map<String,Partition> partitions = new HashMap<String,Partition>();
        
        while( cursor.next() )
        {
            ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor.get();
            ServerEntry partitionEntry = configPartition.lookup( forwardEntry.getId() );

            if( !isEnabled( partitionEntry ) )
            {
                continue;
            }
            EntryAttribute ocAttr = partitionEntry.get( "objectClass" );
            if( ocAttr.contains( "ads-jdbmPartition" ) )
            {
                JdbmPartition partition = getJdbmPartition( partitionEntry );
                partitions.put( partition.getId(), partition );
            }
            else
            {
                throw new NotImplementedException( "yet to implement" );
            }
        }
        
        cursor.close();
        
        return partitions;
    }
    
    
    private JdbmPartition getJdbmPartition( ServerEntry partitionEntry ) throws Exception
    {
        JdbmPartition partition = new JdbmPartition();
        partition.setSchemaManager( schemaManager );
        
        partition.setId( getString( "ads-partitionId", partitionEntry ) );
        partition.setPartitionDir( new File( workDir, partition.getId() ) );
        
        partition.setSuffix( getString( "ads-partitionSuffix", partitionEntry ) );
        
        EntryAttribute cacheAttr = partitionEntry.get( "ads-partitionCacheSize" );
        if( cacheAttr != null )
        {
            partition.setCacheSize( Integer.parseInt( cacheAttr.getString() ) );
        }
        
        EntryAttribute optimizerAttr = partitionEntry.get( "ads-jdbmPartitionOptimizerEnabled" );
        if( optimizerAttr != null )
        {
            partition.setOptimizerEnabled( Boolean.parseBoolean( optimizerAttr.getString() ) );
        }
        
        EntryAttribute syncAttr = partitionEntry.get( "ads-partitionSyncOnWrite" );
        if( syncAttr != null )
        {
            partition.setSyncOnWrite( Boolean.parseBoolean( syncAttr.getString() ) );
        }

        String indexesDN = partitionEntry.get( "ads-partitionIndexedAttributes" ).getString();
        
        Set<Index<?,ServerEntry>> indexedAttributes = getIndexes( new LdapDN( indexesDN ) );
        partition.setIndexedAttributes( indexedAttributes );

        return partition;
    }
    

    private Set<Index<?,ServerEntry>> getIndexes( LdapDN indexesDN ) throws Exception
    {
        PresenceNode filter = new PresenceNode( "ads-indexAttributeId" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        IndexCursor cursor = se.cursor( indexesDN, AliasDerefMode.NEVER_DEREF_ALIASES, filter, controls );
        
        Set<Index<?,ServerEntry>> indexes = new HashSet<Index<?,ServerEntry>>();
        
        while( cursor.next() )
        {
            ForwardIndexEntry<Long, Long> forwardEntry = ( ForwardIndexEntry<Long, Long> ) cursor.get();
            ServerEntry indexEntry = configPartition.lookup( forwardEntry.getId() );
            if( !isEnabled( indexEntry ) )
            {
                continue;
            }
            
            EntryAttribute ocAttr = indexEntry.get( "objectClass" );
            if( ocAttr.contains( "ads-jdbmIndex" ) )
            {
                indexes.add( getJdbmIndex( indexEntry ) );
            }
            else
            {
                throw new NotImplementedException( "yet to implement" );
            }
        }
        
        return indexes;
    }
    
    
    private JdbmIndex<?, ServerEntry> getJdbmIndex( ServerEntry indexEntry ) throws Exception
    {
        JdbmIndex<?, ServerEntry> index = new JdbmIndex();
        index.setAttributeId( getString( "ads-indexAttributeId", indexEntry ) );
        EntryAttribute cacheAttr = indexEntry.get( "ads-indexCacheSize" );
        if( cacheAttr != null )
        {
            index.setCacheSize( Integer.parseInt( cacheAttr.getString() ) );
        }
        
        return index;
    }
    
    
    /**
     * internal class used for holding the Interceptor classname and order configuration
     */
    private class InterceptorConfig implements Comparable<InterceptorConfig>
    {

        private String id;
        private String fqcn;
        private int order;


        public InterceptorConfig( String id, String fqcn, int order )
        {
            if( order < 1 )
            {
                throw new IllegalArgumentException( "Invalid interceptor order" );
            }
            
            this.id = id;
            this.fqcn = fqcn;
            this.order = order;
        }


        public int compareTo( InterceptorConfig o )
        {
            if ( order > o.order )
            {
                return 1;
            }
            else if ( order < o.order )
            {
                return -1;
            }

            return 0;
        }


        /**
         * @return the fqcn
         */
        public String getFqcn()
        {
            return fqcn;
        }


        /**
         * @return the id
         */
        public String getId()
        {
            return id;
        }

    }


    private boolean getBoolean( String attrName, Entry entry ) throws Exception
    {
        return Boolean.parseBoolean( entry.get( attrName ).getString() );
    }


    private String getString( String attrName, Entry entry ) throws Exception
    {
        return entry.get( attrName ).getString();
    }


    private int getInt( String attrName, Entry entry ) throws Exception
    {
        return Integer.parseInt( entry.get( attrName ).getString() );
    }

    
    private boolean isEnabled( Entry entry ) throws Exception
    {
        EntryAttribute enabledAttr = entry.get( "ads-enabled" );
        if( enabledAttr != null )
        {
            return Boolean.parseBoolean( enabledAttr.getString() );
        }
        else
        {
            return true;
        }
        
    }
}
