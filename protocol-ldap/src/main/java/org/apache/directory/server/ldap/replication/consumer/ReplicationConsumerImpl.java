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
package org.apache.directory.server.ldap.replication.consumer;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.directory.api.ldap.codec.controls.manageDsaIT.ManageDsaITDecorator;
import org.apache.directory.api.ldap.extras.controls.SynchronizationModeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncDone.SyncDoneValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncRequest.SyncRequestValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateTypeEnum;
import org.apache.directory.api.ldap.extras.controls.syncrepl.syncState.SyncStateValue;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueDecorator;
import org.apache.directory.api.ldap.extras.intermediate.syncrepl.SyncInfoValue;
import org.apache.directory.api.ldap.extras.intermediate.syncrepl_impl.SyncInfoValueDecorator;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.IntermediateResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultReference;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.message.controls.SortKey;
import org.apache.directory.api.ldap.model.message.controls.SortRequest;
import org.apache.directory.api.ldap.model.message.controls.SortRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.StringConstants;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.ConnectionClosedEventListener;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.replication.ReplicationConsumerConfig;
import org.apache.directory.server.ldap.replication.SyncReplConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * Implementation of syncrepl slave a.k.a consumer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicationConsumerImpl implements ConnectionClosedEventListener, ReplicationConsumer
{
    /** A dedicated logger for the consumer */
    private static final Logger CONSUMER_LOG = LoggerFactory.getLogger( Loggers.CONSUMER_LOG.getName() );

    /** the syncrepl configuration */
    private SyncReplConfiguration config;

    /** the sync cookie sent by the server */
    private byte[] syncCookie;

    /** connection to the syncrepl provider */
    private LdapNetworkConnection connection;

    /** the search request with control */
    private SearchRequest searchRequest;

    /** a reference to the directoryService */
    private DirectoryService directoryService;

    /** the schema manager */
    private SchemaManager schemaManager;

    /** flag to indicate whether the consumer was disconnected */
    private volatile boolean disconnected;

    /** the core session */
    private CoreSession session;

    /** attributes on which modification should be ignored */
    private static final String[] MOD_IGNORE_AT = new String[]
        {
            SchemaConstants.ENTRY_UUID_AT,
            SchemaConstants.ENTRY_DN_AT,
            SchemaConstants.CREATE_TIMESTAMP_AT,
            SchemaConstants.CREATORS_NAME_AT,
            ApacheSchemaConstants.ENTRY_PARENT_ID_AT,
            SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT,
            SchemaConstants.CONTEXT_CSN_AT,
            ApacheSchemaConstants.NB_CHILDREN_AT,
            ApacheSchemaConstants.NB_SUBORDINATES_AT
    };

    /** the cookie that was saved last time */
    private byte[] lastSavedCookie;

    private volatile boolean reload = false;

    /** The (entrtyUuid=*) filter */
    private static final PresenceNode ENTRY_UUID_PRESENCE_FILTER = new PresenceNode( SchemaConstants.ENTRY_UUID_AT );

    private Modification cookieMod;

    private Modification ridMod;

    /** AttributeTypes used for replication */
    private AttributeType adsReplCookieAT;
    private AttributeType adsDsReplicaIdAT;

    private static final Map<String, Object> UUID_LOCK_MAP = new LRUMap( 1000 );


    /**
     * @return the config
     */
    @Override
    public SyncReplConfiguration getConfig()
    {
        return config;
    }


    /**
     * Init the replication service
     * @param directoryservice The directory service
     */
    @Override
    public void init( DirectoryService directoryservice ) throws Exception
    {
        this.directoryService = directoryservice;

        session = directoryService.getAdminSession();

        schemaManager = directoryservice.getSchemaManager();

        adsReplCookieAT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_COOKIE );
        adsDsReplicaIdAT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ADS_DS_REPLICA_ID );

        Attribute cookieAttr = new DefaultAttribute( adsReplCookieAT );
        cookieMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, cookieAttr );

        Attribute ridAttr = new DefaultAttribute( adsDsReplicaIdAT );
        ridMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, ridAttr );

        prepareSyncSearchRequest();
    }


    /**
     * Connect to the remote server. Note that a SyncRepl consumer will be connected to only
     * one remote server
     *
     * @return true if the connections have been successful.
     */
    public boolean connect()
    {
        String providerHost = config.getRemoteHost();
        int port = config.getRemotePort();

        try
        {
            // Create a connection
            if ( connection == null )
            {
                connection = new LdapNetworkConnection( providerHost, port );
                connection.setSchemaManager( schemaManager );

                if ( config.isUseTls() )
                {
                    connection.getConfig().setTrustManagers( config.getTrustManager() );
                    connection.getConfig().setUseTls( true );
                }

                connection.addConnectionClosedEventListener( this );
            }

            // Try to connect
            if ( connection.connect() )
            {
                CONSUMER_LOG.info( "Consumer {} connected to producer {}", config.getReplicaId(), config.getProducer() );

                // Do a bind
                try
                {
                    connection.bind( config.getReplUserDn(), Strings.utf8ToString( config.getReplUserPassword() ) );
                    disconnected = false;

                    return true;
                }
                catch ( LdapException le )
                {
                    CONSUMER_LOG.warn( "Failed to bind to the producer {} with the given bind Dn {}",
                        config.getProducer(), config.getReplUserDn() );
                    CONSUMER_LOG.warn( "", le );
                    disconnected = true;
                }
            }
            else
            {
                CONSUMER_LOG.warn( "Consumer {} cannot connect to producer {}", config.getReplicaId(),
                    config.getProducer() );
                disconnected = true;

                return false;
            }
        }
        catch ( Exception e )
        {
            CONSUMER_LOG.error( "Failed to connect to the producer {}, cause : {}", config.getProducer(),
                e.getMessage() );
            disconnected = true;
        }

        return false;
    }


    /**
     *  prepares a SearchRequest for syncing DIT content.
     *
     */
    private void prepareSyncSearchRequest() throws LdapException
    {
        String baseDn = config.getBaseDn();

        searchRequest = new SearchRequestImpl();

        searchRequest.setBase( new Dn( baseDn ) );
        searchRequest.setFilter( config.getFilter() );
        searchRequest.setSizeLimit( config.getSearchSizeLimit() );
        searchRequest.setTimeLimit( config.getSearchTimeout() );

        searchRequest.setDerefAliases( config.getAliasDerefMode() );
        searchRequest.setScope( config.getSearchScope() );
        searchRequest.setTypesOnly( false );

        searchRequest.addAttributes( config.getAttributes() );

        if ( !config.isChaseReferrals() )
        {
            searchRequest.addControl( new ManageDsaITDecorator( directoryService.getLdapCodecService(),
                new ManageDsaITImpl() ) );
        }

        if ( CONSUMER_LOG.isDebugEnabled() )
        {
            MDC.put( "Replica", Integer.toString( config.getReplicaId() ) );
            CONSUMER_LOG.debug( "Configuring consumer {}", config );
        }
    }


    private ResultCodeEnum handleSearchResultDone( SearchResultDone searchDone )
    {
        CONSUMER_LOG.debug( "///////////////// handleSearchDone //////////////////" );

        SyncDoneValue ctrl = ( SyncDoneValue ) searchDone.getControls().get( SyncDoneValue.OID );

        if ( ( ctrl != null ) && ( ctrl.getCookie() != null ) )
        {
            syncCookie = ctrl.getCookie();
            CONSUMER_LOG.debug( "assigning cookie from sync done value control: {}", Strings.utf8ToString( syncCookie ) );
            storeCookie();
        }

        CONSUMER_LOG.debug( "//////////////// END handleSearchDone//////////////////////" );

        reload = false;

        return searchDone.getLdapResult().getResultCode();
    }


    private void handleSearchReference( SearchResultReference searchRef )
    {
        // this method won't be called cause the provider will serve the referrals as
        // normal entry objects due to the usage of ManageDsaITControl in the search request
    }


    /**
     * Process a SearchResultEntry received from a consumer. We have to handle all the
     * cases :
     * - Add
     * - Modify
     * - Moddn
     * - Delete
     * - Present
     * @param syncResult
     */
    private void handleSearchResultEntry( SearchResultEntry syncResult )
    {
        CONSUMER_LOG.debug( "------------- starting handleSearchResult ------------" );

        SyncStateValue syncStateCtrl = ( SyncStateValue ) syncResult.getControl( SyncStateValue.OID );

        try
        {
            Entry remoteEntry = new DefaultEntry( schemaManager, syncResult.getEntry() );
            String uuid = remoteEntry.get( directoryService.getAtProvider().getEntryUUID() ).getString();
            // lock on UUID to serialize the updates when there are multiple consumers
            // connected to several producers and to the *same* base/partition
            Object lock = getLockFor( uuid );

            synchronized ( lock )
            {
                int rid = -1;

                if ( syncStateCtrl.getCookie() != null )
                {
                    syncCookie = syncStateCtrl.getCookie();
                    rid = LdapProtocolUtils.getReplicaId( Strings.utf8ToString( syncCookie ) );
                    CONSUMER_LOG.debug( "assigning the cookie from sync state value control: {}",
                        Strings.utf8ToString( syncCookie ) );
                }

                SyncStateTypeEnum state = syncStateCtrl.getSyncStateType();

                // check to avoid conversion of UUID from byte[] to String
                if ( CONSUMER_LOG.isDebugEnabled() )
                {
                    CONSUMER_LOG.debug( "state name {}", state.name() );
                    CONSUMER_LOG.debug( "entryUUID = {}", Strings.uuidToString( syncStateCtrl.getEntryUUID() ) );
                }

                Dn remoteDn = remoteEntry.getDn();

                switch ( state )
                {
                    case ADD:
                        boolean remoteDnExist = false;

                        try
                        {
                            remoteDnExist = session.exists( remoteDn );
                        }
                        catch ( LdapNoSuchObjectException lnsoe )
                        {
                            CONSUMER_LOG.error( lnsoe.getMessage() );
                        }

                        if ( !remoteDnExist )
                        {
                            CONSUMER_LOG.debug( "adding entry with dn {}", remoteDn );
                            CONSUMER_LOG.debug( remoteEntry.toString() );
                            AddOperationContext addContext = new AddOperationContext( session, remoteEntry );
                            addContext.setReplEvent( true );
                            addContext.setRid( rid );

                            OperationManager operationManager = directoryService.getOperationManager();
                            operationManager.add( addContext );
                        }
                        else
                        {
                            CONSUMER_LOG.debug( "updating entry in refreshOnly mode {}", remoteDn );
                            modify( remoteEntry, rid );
                        }

                        break;

                    case MODIFY:
                        CONSUMER_LOG.debug( "modifying entry with dn {}", remoteEntry.getDn().getName() );
                        modify( remoteEntry, rid );

                        break;

                    case MODDN:
                        String entryUuid = Strings.uuidToString( syncStateCtrl.getEntryUUID() );
                        applyModDnOperation( remoteEntry, entryUuid, rid );

                        break;

                    case DELETE:
                        CONSUMER_LOG.debug( "deleting entry with dn {}", remoteEntry.getDn().getName() );

                        if ( !session.exists( remoteDn ) )
                        {
                            CONSUMER_LOG
                                .debug(
                                    "looks like entry {} was already deleted in a prior update (possibly from another provider), skipping delete",
                                    remoteDn );
                        }
                        else
                        {
                            // incase of a MODDN operation resulting in a branch to be moved out of scope
                            // ApacheDS replication provider sends a single delete event on the Dn of the moved branch
                            // so the branch needs to be recursively deleted here
                            deleteRecursive( remoteEntry.getDn(), rid );
                        }

                        break;

                    case PRESENT:
                        CONSUMER_LOG.debug( "entry present {}", remoteEntry );
                        break;

                    default:
                        throw new IllegalArgumentException( "Unexpected sync state " + state );
                }

                // store the cookie only if the above operation was successful
                if ( syncStateCtrl.getCookie() != null )
                {
                    storeCookie();
                }
            }
        }
        catch ( Exception e )
        {
            CONSUMER_LOG.error( e.getMessage(), e );
        }

        CONSUMER_LOG.debug( "------------- Ending handleSearchResult ------------" );
    }


    /**
     * {@inheritDoc}
     */
    private void handleSyncInfo( IntermediateResponse syncInfoResp )
    {
        try
        {
            CONSUMER_LOG.debug( "............... inside handleSyncInfo ..............." );

            byte[] syncInfoBytes = syncInfoResp.getResponseValue();

            if ( syncInfoBytes == null )
            {
                return;
            }

            SyncInfoValueDecorator decorator = new SyncInfoValueDecorator( directoryService.getLdapCodecService() );
            SyncInfoValue syncInfoValue = ( SyncInfoValue ) decorator.decode( syncInfoBytes );

            byte[] cookie = syncInfoValue.getCookie();

            if ( CONSUMER_LOG.isDebugEnabled() )
            {
                CONSUMER_LOG.debug( "Received a SyncInfoValue from producer {} : {}", config.getProducer(),
                    syncInfoValue );
            }

            int replicaId = -1;

            if ( cookie != null )
            {
                if ( CONSUMER_LOG.isDebugEnabled() )
                {
                    CONSUMER_LOG.debug( "setting the cookie from the sync info: {}", Strings.utf8ToString( cookie ) );
                    CONSUMER_LOG.debug( "setting the cookie from the sync info: {}", Strings.utf8ToString( cookie ) );
                }

                syncCookie = cookie;

                String cookieString = Strings.utf8ToString( syncCookie );
                replicaId = LdapProtocolUtils.getReplicaId( cookieString );
            }

            CONSUMER_LOG.info( "refreshDeletes: {}", syncInfoValue.isRefreshDeletes() );

            List<byte[]> uuidList = syncInfoValue.getSyncUUIDs();

            // if refreshDeletes set to true then delete all the entries with entryUUID
            // present in the syncIdSet
            if ( syncInfoValue.isRefreshDeletes() )
            {
                deleteEntries( uuidList, false, replicaId );
            }
            else
            {
                deleteEntries( uuidList, true, replicaId );
            }

            CONSUMER_LOG.info( "refreshDone: {}", syncInfoValue.isRefreshDone() );

            storeCookie();
        }
        catch ( Exception de )
        {
            CONSUMER_LOG.error( "Failed to handle syncinfo message", de );
        }

        CONSUMER_LOG.debug( ".................... END handleSyncInfo ..............." );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionClosed()
    {
        if ( CONSUMER_LOG.isDebugEnabled() )
        {
            MDC.put( "Replica", Integer.toString( config.getReplicaId() ) );
            CONSUMER_LOG.debug( "Consumer {} session with {} has been closed ", config.getReplicaId(),
                config.getProducer() );
        }

        disconnect();
    }


    /**
     * Starts the synchronization operation
     */
    @Override
    public ReplicationStatusEnum startSync()
    {
        CONSUMER_LOG.debug( "Starting the SyncRepl process for consumer {}", config.getReplicaId() );

        // read the cookie if persisted
        readCookie();

        if ( config.isRefreshNPersist() )
        {
            try
            {
                CONSUMER_LOG.debug( "==================== Refresh And Persist ==========" );

                return doSyncSearch( SynchronizationModeEnum.REFRESH_AND_PERSIST, reload );
            }
            catch ( Exception e )
            {
                CONSUMER_LOG.error( "Failed to sync with refreshAndPersist mode", e );
                return ReplicationStatusEnum.DISCONNECTED;
            }
        }
        else
        {
            return doRefreshOnly();
        }
    }


    private ReplicationStatusEnum doRefreshOnly()
    {
        while ( !disconnected )
        {
            CONSUMER_LOG.debug( "==================== Refresh Only ==========" );

            try
            {
                doSyncSearch( SynchronizationModeEnum.REFRESH_ONLY, reload );

                CONSUMER_LOG.debug( "--------------------- Sleep for {} seconds ------------------",
                    ( config.getRefreshInterval() / 1000 ) );
                Thread.sleep( config.getRefreshInterval() );
                CONSUMER_LOG.debug( "--------------------- syncing again ------------------" );

            }
            catch ( InterruptedException ie )
            {
                CONSUMER_LOG.warn( "refresher thread interrupted" );

                return ReplicationStatusEnum.DISCONNECTED;
            }
            catch ( Exception e )
            {
                CONSUMER_LOG.error( "Failed to sync with refresh only mode", e );
                return ReplicationStatusEnum.DISCONNECTED;
            }
        }

        return ReplicationStatusEnum.STOPPED;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig( ReplicationConsumerConfig config )
    {
        this.config = ( SyncReplConfiguration ) config;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean connect( boolean now )
    {
        boolean connected = false;

        if ( now )
        {
            connected = connect();
        }

        while ( !connected )
        {
            try
            {
                CONSUMER_LOG.debug( "Consumer {} cannot connect to {}, wait 5 seconds.", config.getReplicaId(),
                    config.getProducer() );

                // try to establish a connection for every 5 seconds
                Thread.sleep( 5000 );
            }
            catch ( InterruptedException e )
            {
                CONSUMER_LOG.warn( "Consumer {} Interrupted while trying to reconnect to the provider {}",
                    config.getReplicaId(), config.getProducer() );
            }

            connected = connect();
        }

        // TODO : we may have cases were we get here with the connected flag to false. With the above
        // code, thi sis not possible

        return connected;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ping()
    {
        boolean connected = !disconnected;

        boolean restartSync = false;

        if ( disconnected )
        {
            connected = connect();
            restartSync = connected;
        }

        if ( connected )
        {
            CONSUMER_LOG.debug( "PING : The consumer {} is alive", config.getReplicaId() );

            // DIRSERVER-2014
            if ( restartSync )
            {
                CONSUMER_LOG.warn( "Restarting the disconnected consumer {}", config.getReplicaId() );
                disconnected = false;
                startSync();
            }
        }
        else
        {
            CONSUMER_LOG.debug( "PING : The consumer {} cannot be connected", config.getReplicaId() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if ( !disconnected )
        {
            disconnect();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return String.valueOf( getConfig().getReplicaId() );
    }


    /**
     * Performs a search on connection with updated syncRequest control. The provider
     * will initiate an UpdateContant or an initContent depending on the current consumer
     * status, accordingly to the cookie's content.
     * If the mode is refreshOnly, the server will send a SearchResultDone when all the modified
     * entries have been sent.
     * If the mode is refreshAndPersist, the provider never send a SearchResultDone, so we keep
     * receiving modifications' notifications on the consumer, and never exit the loop, unless
     * some communication error occurs.
     *
     * @param syncType The synchornization type, either REFRESH_ONLY or REFRESH_AND_PERSIST
     * @param reloadHint A flag used to tell the server that we want a reload
     * @return The replication status
     * @throws Exception in case of any problems encountered while searching
     */
    private ReplicationStatusEnum doSyncSearch( SynchronizationModeEnum syncType, boolean reloadHint ) throws Exception
    {
        CONSUMER_LOG.debug( "Starting synchronization mode {}, reloadHint {}", syncType, reloadHint );
        // Prepare the Syncrepl Request
        SyncRequestValue syncReq = new SyncRequestValueDecorator( directoryService.getLdapCodecService() );

        syncReq.setMode( syncType );
        syncReq.setReloadHint( reloadHint );

        // If we have a persisted cookie, send it.
        if ( syncCookie != null )
        {
            CONSUMER_LOG.debug( "searching on {} with searchRequest, cookie '{}'", config.getProducer(),
                Strings.utf8ToString( syncCookie ) );
            syncReq.setCookie( syncCookie );
        }
        else
        {
            CONSUMER_LOG.debug( "searching on {} with searchRequest, no cookie", config.getProducer() );
        }

        searchRequest.addControl( syncReq );

        // Do the search. We use a searchAsync because we want to get SearchResultDone responses
        SearchFuture sf = connection.searchAsync( searchRequest );

        Response resp = sf.get();

        CONSUMER_LOG.debug( "Response from {} : {}", config.getProducer(), resp );

        // Now, process the responses. We loop until we have a connection termination or
        // a SearchResultDone (RefreshOnly mode)
        while ( !( resp instanceof SearchResultDone ) && !sf.isCancelled() && !disconnected )
        {
            if ( resp instanceof SearchResultEntry )
            {
                SearchResultEntry result = ( SearchResultEntry ) resp;

                handleSearchResultEntry( result );
            }
            else if ( resp instanceof SearchResultReference )
            {
                handleSearchReference( ( SearchResultReference ) resp );
            }
            else if ( resp instanceof IntermediateResponse )
            {
                handleSyncInfo( ( IntermediateResponse ) resp );
            }

            // Next entry
            resp = sf.get();
            CONSUMER_LOG.debug( "Response from {} : {}", config.getProducer(), resp );
        }

        if ( sf.isCancelled() )
        {

            CONSUMER_LOG.debug( "Search sync on {} has been canceled ", config.getProducer(), sf.getCause() );

            return ReplicationStatusEnum.DISCONNECTED;
        }
        else if ( disconnected )
        {
            CONSUMER_LOG.debug( "Disconnected from {}", config.getProducer() );

            return ReplicationStatusEnum.DISCONNECTED;
        }
        else
        {
            ResultCodeEnum resultCode = handleSearchResultDone( ( SearchResultDone ) resp );

            CONSUMER_LOG.debug( "Rsultcode of Sync operation from {} : {}", config.getProducer(), resultCode );

            if ( resultCode == ResultCodeEnum.NO_SUCH_OBJECT )
            {
                // log the error and handle it appropriately
                CONSUMER_LOG.warn( "The base Dn {} is not found on provider {}", config.getBaseDn(),
                    config.getProducer() );

                CONSUMER_LOG.warn( "Disconnecting the Refresh&Persist consumer from provider {}", config.getProducer() );
                disconnect();

                return ReplicationStatusEnum.DISCONNECTED;
            }
            else if ( resultCode == ResultCodeEnum.E_SYNC_REFRESH_REQUIRED )
            {
                CONSUMER_LOG.warn( "Full SYNC_REFRESH required from {}", config.getProducer() );

                reload = true;

                try
                {
                    CONSUMER_LOG.debug( "Deleting baseDN {}", config.getBaseDn() );

                    // FIXME taking a backup right before deleting might be a good thing, just to be safe.
                    // the backup file can be deleted after reload completes successfully

                    // the 'rid' value is not taken into consideration when 'reload' is set
                    // so any dummy value is fine
                    deleteRecursive( new Dn( config.getBaseDn() ), -1000 );
                }
                catch ( Exception e )
                {
                    CONSUMER_LOG
                        .error(
                            "Failed to delete the replica base as part of handling E_SYNC_REFRESH_REQUIRED, disconnecting the consumer",
                            e );
                }

                // Do a full update.
                removeCookie();

                CONSUMER_LOG.debug( "Re-doing a syncRefresh from producer {}", config.getProducer() );

                return ReplicationStatusEnum.REFRESH_REQUIRED;
            }
            else
            {
                CONSUMER_LOG.debug( "Got result code {} from producer {}. Replication stopped", resultCode,
                    config.getProducer() );
                return ReplicationStatusEnum.DISCONNECTED;
            }
        }
    }


    /**
     * Disconnect from the producer
     */
    private void disconnect()
    {
        disconnected = true;

        try
        {
            if ( ( connection != null ) && connection.isConnected() )
            {
                connection.unBind();
                CONSUMER_LOG.info( "Unbound from the server {}", config.getProducer() );

                if ( CONSUMER_LOG.isDebugEnabled() )
                {
                    MDC.put( "Replica", Integer.toString( config.getReplicaId() ) );
                    CONSUMER_LOG.info( "Unbound from the server {}", config.getProducer() );
                }

                connection.close();
                CONSUMER_LOG.info( "Connection closed for the server {}", config.getProducer() );

                connection = null;
            }
        }
        catch ( Exception e )
        {
            CONSUMER_LOG.error( "Failed to close the connection", e );
        }
        finally
        {
            // persist the cookie
            storeCookie();

            // reset the cookie
            syncCookie = null;
        }
    }


    /**
     * stores the cookie.
     */
    private void storeCookie()
    {
        CONSUMER_LOG.debug( "Storing the cookie '{}'", Strings.utf8ToString( syncCookie ) );

        if ( syncCookie == null )
        {
            return;
        }

        if ( ( lastSavedCookie != null ) && Arrays.equals( syncCookie, lastSavedCookie ) )
        {
            return;
        }

        try
        {
            Attribute attr = cookieMod.getAttribute();
            attr.clear();
            attr.add( syncCookie );

            String cookieString = Strings.utf8ToString( syncCookie );
            int replicaId = LdapProtocolUtils.getReplicaId( cookieString );

            Attribute ridAt = ridMod.getAttribute();
            ridAt.clear();
            ridAt.add( String.valueOf( replicaId ) );

            CONSUMER_LOG.debug( "Storing the cookie in the DIT : {}", config.getConfigEntryDn() );

            session.modify( config.getConfigEntryDn(), cookieMod );
            CONSUMER_LOG.debug( "stored the cookie in entry {}", config.getConfigEntryDn() );

            lastSavedCookie = new byte[syncCookie.length];
            System.arraycopy( syncCookie, 0, lastSavedCookie, 0, syncCookie.length );
        }
        catch ( Exception e )
        {
            CONSUMER_LOG.error( "Failed to store the cookie in consumer entry {}", config.getConfigEntryDn(), e );
        }
    }


    /**
     * Read the cookie for a consumer
     */
    private void readCookie()
    {
        try
        {
            Entry entry = session.lookup( config.getConfigEntryDn(), SchemaConstants.ADS_REPL_COOKIE );

            if ( entry != null )
            {
                Attribute attr = entry.get( adsReplCookieAT );

                if ( attr != null )
                {
                    syncCookie = attr.getBytes();
                    lastSavedCookie = syncCookie;
                    String syncCookieString = Strings.utf8ToString( syncCookie );
                    CONSUMER_LOG.debug( "Loaded cookie {} for consumer {}", syncCookieString, config.getReplicaId() );
                }
                else
                {
                    CONSUMER_LOG.debug( "No cookie found for consumer {}", config.getReplicaId() );
                }
            }
            else
            {
                CONSUMER_LOG.debug( "Cannot find the configuration '{}' in the DIT for consumer {}",
                    config.getConfigEntryDn(), config.getReplicaId() );
            }
        }
        catch ( Exception e )
        {
            // can be ignored, most likely happens if there is no entry with the given Dn
            // log in debug mode
            CONSUMER_LOG.debug( "Failed to read the cookie, cannot find the entry '{}' in the DIT for consumer {}",
                config.getConfigEntryDn(),
                config.getReplicaId() );
        }
    }


    /**
     * deletes the cookie and resets the syncCookie to null
     */
    private void removeCookie()
    {
        try
        {
            Attribute cookieAttr = new DefaultAttribute( adsReplCookieAT );
            Modification deleteCookieMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                cookieAttr );
            session.modify( config.getConfigEntryDn(), deleteCookieMod );
            CONSUMER_LOG.info( "resetting sync cookie of the consumer with config entry Dn {}",
                config.getConfigEntryDn() );
        }
        catch ( Exception e )
        {
            CONSUMER_LOG.warn( "Failed to delete the cookie from the consumer with config entry Dn {}",
                config.getConfigEntryDn() );
            CONSUMER_LOG.warn( "{}", e );
        }

        syncCookie = null;
        lastSavedCookie = null;
    }


    private void applyModDnOperation( Entry remoteEntry, String entryUuid, int rid ) throws Exception
    {
        CONSUMER_LOG.debug( "MODDN for entry {}, new entry : {}", entryUuid, remoteEntry );

        // Retrieve locally the moved or renamed entry
        String filter = "(entryUuid=" + entryUuid + ")";
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( new Dn( schemaManager, config.getBaseDn() ) );
        searchRequest.setFilter( filter );
        searchRequest.setScope( SearchScope.SUBTREE );
        searchRequest.addAttributes( SchemaConstants.ENTRY_UUID_AT, SchemaConstants.ENTRY_CSN_AT,
            SchemaConstants.ALL_USER_ATTRIBUTES );

        Cursor<Entry> cursor = session.search( searchRequest );
        cursor.beforeFirst();

        Entry localEntry = null;

        if ( cursor.next() )
        {
            localEntry = cursor.get();
        }

        cursor.close();

        // can happen in MMR scenario
        if ( localEntry == null )
        {
            return;
        }

        if ( config.isMmrMode() )
        {
            Csn localCsn = new Csn( localEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
            Csn remoteCsn = new Csn( remoteEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );

            if ( localCsn.compareTo( remoteCsn ) >= 0 )
            {
                // just discard the received modified entry, that is old
                CONSUMER_LOG.debug( "local modification is latest, discarding the modDn operation dn {}",
                    remoteEntry.getDn() );
                return;
            }
        }

        // Compute the DN, parentDn and Rdn for both entries
        Dn localDn = localEntry.getDn();
        Dn remoteDn = directoryService.getDnFactory().create( remoteEntry.getDn().getName() );

        Dn localParentDn = localDn.getParent();
        Dn remoteParentDn = directoryService.getDnFactory().create( remoteDn.getParent().getName() );

        Rdn localRdn = localDn.getRdn();
        Rdn remoteRdn = directoryService.getDnFactory().create( remoteDn.getRdn().getName() ).getRdn();

        // Check if the OldRdn has been deleted
        boolean deleteOldRdn = !remoteEntry.contains( localRdn.getNormType(), localRdn.getValue() );

        if ( localRdn.equals( remoteRdn ) )
        {
            // If the RDN are equals, it's a MOVE
            CONSUMER_LOG.debug( "moving {} to the new parent {}", localDn, remoteParentDn );
            MoveOperationContext movCtx = new MoveOperationContext( session, localDn, remoteParentDn );
            movCtx.setReplEvent( true );
            movCtx.setRid( rid );
            directoryService.getOperationManager().move( movCtx );
        }
        else if ( localParentDn.equals( remoteParentDn ) )
        {
            // If the parentDn are equals, it's a RENAME
            CONSUMER_LOG.debug( "renaming the Dn {} with new Rdn {} and deleteOldRdn flag set to {}",
                localDn.getName(), remoteRdn.getName(), String.valueOf( deleteOldRdn ) );

            RenameOperationContext renCtx = new RenameOperationContext( session, localDn, remoteRdn,
                deleteOldRdn );
            renCtx.setReplEvent( true );
            renCtx.setRid( rid );
            directoryService.getOperationManager().rename( renCtx );
        }
        else
        {
            // Otherwise, it's a MOVE and RENAME
            CONSUMER_LOG.debug(
                "moveAndRename on the Dn {} with new newParent Dn {}, new Rdn {} and deleteOldRdn flag set to {}",
                localDn.getName(),
                remoteParentDn.getName(),
                remoteRdn.getName(),
                String.valueOf( deleteOldRdn ) );

            MoveAndRenameOperationContext movRenCtx = new MoveAndRenameOperationContext( session, localDn,
                remoteParentDn, remoteRdn, deleteOldRdn );
            movRenCtx.setReplEvent( true );
            movRenCtx.setRid( rid );
            directoryService.getOperationManager().moveAndRename( movRenCtx );
        }
    }


    private void modify( Entry remoteEntry, int rid ) throws Exception
    {
        String[] attributes = computeAttributes( config.getAttributes(), SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );

        LookupOperationContext lookupCtx =
            new LookupOperationContext( session, remoteEntry.getDn(), attributes );

        lookupCtx.setSyncreplLookup( true );

        Entry localEntry;

        Partition partition = session.getDirectoryService().getPartitionNexus().getPartition( remoteEntry.getDn() );

        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            lookupCtx.setTransaction( partitionTxn );
            localEntry = session.getDirectoryService().getOperationManager().lookup( lookupCtx );
        }

        if ( config.isMmrMode() )
        {
            Csn localCsn = new Csn( localEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
            Csn remoteCsn = new Csn( remoteEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );

            if ( localCsn.compareTo( remoteCsn ) >= 0 )
            {
                // just discard the received modified entry, that is old
                CONSUMER_LOG.debug( "local modification is latest, discarding the modification of dn {}",
                    remoteEntry.getDn() );
                return;
            }
        }

        remoteEntry.removeAttributes( MOD_IGNORE_AT );
        localEntry.removeAttributes( MOD_IGNORE_AT );

        List<Modification> mods = new ArrayList<>();
        Iterator<Attribute> itr = localEntry.iterator();

        while ( itr.hasNext() )
        {
            Attribute localAttr = itr.next();
            String attrId = localAttr.getId();
            Modification mod;
            Attribute remoteAttr = remoteEntry.get( attrId );

            if ( remoteAttr != null ) // would be better if we compare the values also? or will it consume more time?
            {
                mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, remoteAttr );
                remoteEntry.remove( remoteAttr );
            }
            else
            {
                mod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, localAttr );
            }

            mods.add( mod );
        }

        if ( remoteEntry.size() > 0 )
        {
            itr = remoteEntry.iterator();

            while ( itr.hasNext() )
            {
                mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, itr.next() ) );
            }
        }

        List<Modification> serverModifications = new ArrayList<>( mods.size() );

        for ( Modification mod : mods )
        {
            serverModifications.add( new DefaultModification( directoryService.getSchemaManager(), mod ) );
        }

        ModifyOperationContext modifyContext = new ModifyOperationContext( session, remoteEntry.getDn(),
            serverModifications );
        modifyContext.setReplEvent( true );
        modifyContext.setRid( rid );

        OperationManager operationManager = directoryService.getOperationManager();
        operationManager.modify( modifyContext );
    }


    /**
     * Create a new list combining a list and a newly added attribute
     */
    private String[] computeAttributes( String[] attributes, String addedAttribute )
    {
        if ( attributes != null )
        {
            if ( addedAttribute != null )
            {
                String[] combinedAttributes = new String[attributes.length + 1];

                System.arraycopy( attributes, 0, combinedAttributes, 0, attributes.length );
                combinedAttributes[attributes.length] = addedAttribute;

                return combinedAttributes;
            }
            else
            {
                return attributes;
            }
        }
        else
        {
            if ( addedAttribute != null )
            {
                return new String[]
                    { addedAttribute };
            }
            else
            {
                return StringConstants.EMPTY_STRINGS;
            }
        }
    }


    /**
     * deletes the entries having the UUID given in the list
     *
     * @param uuidList the list of UUIDs
     * @param replicaId TODO
     * @throws Exception in case of any problems while deleting the entries
     */
    private void deleteEntries( List<byte[]> uuidList, boolean isRefreshPresent, int replicaId ) throws Exception
    {
        if ( uuidList == null || uuidList.isEmpty() )
        {
            return;
        }

        // if it is refreshPresent list then send all the UUIDs for
        // filtering, otherwise breaking the list will cause the
        // other present entries to be deleted from DIT
        if ( isRefreshPresent )
        {
            CONSUMER_LOG.debug( "refresh present syncinfo list has {} UUIDs", uuidList.size() );
            processDelete( uuidList, isRefreshPresent, replicaId );
            return;
        }

        int nodeLimit = 10;

        int count = uuidList.size() / nodeLimit;

        int startIndex = 0;
        int i = 0;
        for ( ; i < count; i++ )
        {
            startIndex = i * nodeLimit;
            processDelete( uuidList.subList( startIndex, startIndex + nodeLimit ), isRefreshPresent, replicaId );
        }

        if ( ( uuidList.size() % nodeLimit ) != 0 )
        {
            // remove the remaining entries
            if ( count > 0 )
            {
                startIndex = i * nodeLimit;
            }

            processDelete( uuidList.subList( startIndex, uuidList.size() ), isRefreshPresent, replicaId );
        }
    }


    /**
     * do not call this method directly, instead call deleteEntries()
     *
     * @param limitedUuidList a list of UUIDs whose size is less than or equal to #NODE_LIMIT (node limit applies only for refreshDeletes list)
     * @param isRefreshPresent a flag indicating the type of entries present in the UUID list
     * @param replicaId TODO
     */
    private void processDelete( List<byte[]> limitedUuidList, boolean isRefreshPresent, int replicaId )
        throws Exception
    {
        ExprNode filter = null;
        int size = limitedUuidList.size();
        if ( size == 1 )
        {
            String uuid = Strings.uuidToString( limitedUuidList.get( 0 ) );

            filter = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT, new Value( uuid ).getValue() );
            if ( isRefreshPresent )
            {
                filter = new NotNode( filter );
            }
        }
        else
        {
            if ( isRefreshPresent )
            {
                filter = new AndNode();
            }
            else
            {
                filter = new OrNode();
            }

            for ( int i = 0; i < size; i++ )
            {
                String uuid = Strings.uuidToString( limitedUuidList.get( i ) );
                ExprNode uuidEqNode = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT, new Value( uuid ) .getValue() );

                if ( isRefreshPresent )
                {
                    uuidEqNode = new NotNode( uuidEqNode );
                    ( ( AndNode ) filter ).addNode( uuidEqNode );
                }
                else
                {
                    ( ( OrNode ) filter ).addNode( uuidEqNode );
                }
            }
        }

        Dn dn = new Dn( schemaManager, config.getBaseDn() );

        if ( CONSUMER_LOG.isDebugEnabled() )
        {
            CONSUMER_LOG.debug( "selecting entries to be deleted using filter {}", filter );
        }

        SearchRequest req = new SearchRequestImpl();
        req.setBase( dn );
        req.setFilter( filter );
        req.setScope( SearchScope.SUBTREE );
        req.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
        // the ENTRY_DN_AT must be in the attribute list, otherwise sorting fails
        req.addAttributes( SchemaConstants.ENTRY_DN_AT );

        SortKey sk = new SortKey( SchemaConstants.ENTRY_DN_AT, "2.5.13.1" );
        SortRequest ctrl = new SortRequestImpl();
        ctrl.addSortKey( sk );
        req.addControl( ctrl );

        OperationManager operationManager = directoryService.getOperationManager();

        Cursor<Entry> cursor = session.search( req );
        cursor.beforeFirst();

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            DeleteOperationContext ctx = new DeleteOperationContext( session );
            ctx.setReplEvent( true );
            ctx.setRid( replicaId );

            // DO NOT generate replication event if this is being deleted as part of
            // e_sync_refresh_required
            if ( reload )
            {
                ctx.setGenerateNoReplEvt( true );
            }

            ctx.setDn( entry.getDn() );
            operationManager.delete( ctx );
        }

        cursor.close();
    }


    private synchronized Object getLockFor( String uuid )
    {
        Object lock = UUID_LOCK_MAP.get( uuid );

        if ( lock == null )
        {
            lock = new Object();
            UUID_LOCK_MAP.put( uuid, lock );
        }

        return lock;
    }


    /**
     * removes all child entries present under the given Dn and finally the Dn itself
     *
     * @param rootDn the Dn which will be removed after removing its children
     * @param rid the replica ID
     * @throws Exception If the Dn is not valid or if the deletion failed
     */
    private void deleteRecursive( Dn rootDn, int rid ) throws Exception
    {
        CONSUMER_LOG.debug( "searching for Dn {} and its children before deleting", rootDn.getName() );
        Cursor<Entry> cursor = null;

        try
        {
            SearchRequest req = new SearchRequestImpl();
            req.setBase( rootDn );
            req.setFilter( ENTRY_UUID_PRESENCE_FILTER );
            req.setScope( SearchScope.SUBTREE );
            req.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
            // the ENTRY_DN_AT must be in the attribute list, otherwise sorting fails
            req.addAttributes( SchemaConstants.ENTRY_DN_AT );

            SortKey sk = new SortKey( SchemaConstants.ENTRY_DN_AT, "2.5.13.1" );

            SortRequest ctrl = new SortRequestImpl();
            ctrl.addSortKey( sk );
            req.addControl( ctrl );

            cursor = session.search( req );
            cursor.beforeFirst();

            OperationManager operationManager = directoryService.getOperationManager();

            while ( cursor.next() )
            {
                Entry e = cursor.get();

                DeleteOperationContext ctx = new DeleteOperationContext( session );
                ctx.setReplEvent( true );
                ctx.setRid( rid );

                // DO NOT generate replication event if this is being deleted as part of
                // e_sync_refresh_required
                if ( reload )
                {
                    ctx.setGenerateNoReplEvt( true );
                }

                ctx.setDn( e.getDn() );

                operationManager.delete( ctx );
            }
        }
        catch ( Exception e )
        {
            String msg = "Failed to delete the Dn " + rootDn.getName() + " and its children (if any present)";
            CONSUMER_LOG.error( msg, e );
            throw e;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Consumer " ).append( config );

        return sb.toString();
    }
}
