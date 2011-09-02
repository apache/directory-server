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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.ldap.client.api.ConnectionClosedEventListener;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.ldap.replication.ReplicationConsumerConfig;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.shared.ldap.codec.controls.manageDsaIT.ManageDsaITDecorator;
import org.apache.directory.shared.ldap.extras.controls.SyncDoneValue;
import org.apache.directory.shared.ldap.extras.controls.SyncInfoValue;
import org.apache.directory.shared.ldap.extras.controls.SyncModifyDnType;
import org.apache.directory.shared.ldap.extras.controls.SyncRequestValue;
import org.apache.directory.shared.ldap.extras.controls.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.extras.controls.SyncStateValue;
import org.apache.directory.shared.ldap.extras.controls.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncInfoValueDecorator;
import org.apache.directory.shared.ldap.extras.controls.syncrepl_impl.SyncRequestValueDecorator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.NotNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of syncrepl slave a.k.a consumer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplicationConsumerImpl implements ConnectionClosedEventListener, ReplicationConsumer
{
    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( ReplicationConsumerImpl.class );
    private static final Logger CONSUMER_LOG = LoggerFactory.getLogger( "CONSUMER_LOG" );

    /** the syncrepl configuration */
    private SyncreplConfiguration config;

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

    /** the cookie file */
    private File cookieFile;

    /** flag to indicate whether the consumer was disconnected */
    private boolean disconnected;

    /** the core session */
    private CoreSession session;

    /** attributes on which modification should be ignored */
    private static final String[] MOD_IGNORE_AT = new String[]
        {
            SchemaConstants.ENTRY_UUID_AT, 
            SchemaConstants.ENTRY_CSN_AT, 
            SchemaConstants.MODIFIERS_NAME_AT,
            SchemaConstants.MODIFY_TIMESTAMP_AT, 
            SchemaConstants.CREATE_TIMESTAMP_AT, 
            SchemaConstants.CREATORS_NAME_AT, 
            SchemaConstants.ENTRY_PARENT_ID_AT 
        };

    /** A thread used to refresh in refreshOnly mode */
    private RefresherThread refreshThread;

    /** the cookie that was saved last time */
    private byte[] lastSavedCookie;

    /** The (entrtyUuid=*) filter */
    private static final PresenceNode ENTRY_UUID_PRESENCE_FILTER = new PresenceNode( SchemaConstants.ENTRY_UUID_AT );

    /** The set used for search attributes, containing only the entryUuid AT */
    private static final Set<AttributeTypeOptions> ENTRY_UUID_ATOP_SET = new HashSet<AttributeTypeOptions>();

    private Modification cookieMod;

    /** AttributeTypes used for replication */
    private static AttributeType COOKIE_AT_TYPE;
    private static AttributeType ENTRY_UUID_AT;


    /**
     * @return the config
     */
    public SyncreplConfiguration getConfig()
    {
        return config;
    }


    /**
     * Init the replication service
     * @param directoryservice The directory service
     */
    public void init( DirectoryService directoryservice ) throws Exception
    {
        this.directoryService = directoryservice;

        if ( config.isStoreCookieInFile() )
        {
            File cookieDir = new File( directoryservice.getInstanceLayout().getRunDirectory(), "cookies" );
            cookieDir.mkdir();

            cookieFile = new File( cookieDir, String.valueOf( config.getReplicaId() ) );
        }

        session = directoryService.getAdminSession();

        schemaManager = directoryservice.getSchemaManager();

        ENTRY_UUID_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT );
        COOKIE_AT_TYPE = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ADS_REPL_COOKIE );

        ENTRY_UUID_ATOP_SET.add( new AttributeTypeOptions( ENTRY_UUID_AT ) );

        Attribute cookieAttr = new DefaultAttribute( COOKIE_AT_TYPE );

        Modification cookieMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, cookieAttr );
        this.cookieMod = cookieMod;

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
        try
        {
            String providerHost = config.getRemoteHost();
            int port = config.getRemotePort();
            
            // Create a connection
            if ( connection == null )
            {
                connection = new LdapNetworkConnection( providerHost, port );
                connection.setTimeOut( -1L );
                
                if( config.isUseTls() )
                {
                    connection.getConfig().setTrustManagers( config.getTrustManager() );
                    connection.startTls();
                }
                
                connection.addConnectionClosedEventListener( this );
            }

            // Do a bind
            try
            {
                connection.bind( config.getReplUserDn(), Strings.utf8ToString( config.getReplUserPassword() ) );
                disconnected = false;
                return true;
            }
            catch ( LdapException le )
            {
                LOG.warn( "Failed to bind to the server with the given bind Dn {}", config.getReplUserDn() );
                LOG.warn( "", le );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to bind with the given bindDN and credentials", e );
        }

        return false;
    }


    /**
     *
     *  prepares a SearchRequest for syncing DIT content.
     *
     */
    public void prepareSyncSearchRequest() throws LdapException
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
    }


    public ResultCodeEnum handleSearchDone( SearchResultDone searchDone )
    {
        LOG.debug( "///////////////// handleSearchDone //////////////////" );

        SyncDoneValue ctrl = (SyncDoneValue)searchDone.getControls().get( SyncDoneValue.OID );

        if ( ( ctrl != null ) && ( ctrl.getCookie() != null ) )
        {
            syncCookie = ctrl.getCookie();
            LOG.debug( "assigning cookie from sync done value control: " + Strings.utf8ToString(syncCookie) );
            storeCookie();
        }

        LOG.debug( "//////////////// END handleSearchDone//////////////////////" );

        return searchDone.getLdapResult().getResultCode();
    }


    public void handleSearchReference( SearchResultReference searchRef )
    {
        // this method won't be called cause the provider will serve the referrals as
        // normal entry objects due to the usage of ManageDsaITControl in the search request
    }


    public void handleSearchResult( SearchResultEntry syncResult )
    {

        LOG.debug( "------------- starting handleSearchResult ------------" );

        SyncStateValue syncStateCtrl = ( SyncStateValue ) syncResult.getControl( SyncStateValue.OID );

        try
        {
            Entry remoteEntry = syncResult.getEntry();

            if ( syncStateCtrl.getCookie() != null )
            {
                syncCookie = syncStateCtrl.getCookie();
                LOG.debug( "assigning the cookie from sync state value control: "
                    + Strings.utf8ToString(syncCookie) );
            }

            SyncStateTypeEnum state = syncStateCtrl.getSyncStateType();

            LOG.debug( "state name {}", state.name() );

            // check to avoid conversion of UUID from byte[] to String
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "entryUUID = {}", Strings.uuidToString(syncStateCtrl.getEntryUUID()) );
            }

            switch ( state )
            {
                case ADD:
                    Dn remoteDn = directoryService.getDnFactory().create( remoteEntry.getDn().getName() );
                    
                    if ( !session.exists( remoteDn ) )
                    {
                        LOG.debug( "adding entry with dn {}", remoteDn );
                        LOG.debug( remoteEntry.toString() );
                        session.add( new DefaultEntry( schemaManager, remoteEntry ) );
                    }
                    else
                    {
                        LOG.debug( "updating entry in refreshOnly mode {}", remoteDn );
                        modify( remoteEntry );
                    }

                    break;

                case MODIFY:
                    LOG.debug( "modifying entry with dn {}", remoteEntry.getDn().getName() );
                    modify( remoteEntry );
                    
                    break;

                case MODDN:
                    String entryUuid = Strings.uuidToString( syncStateCtrl.getEntryUUID() ).toString();
                    applyModDnOperation( remoteEntry, entryUuid );
                    
                    break;

                case DELETE:
                    LOG.debug( "deleting entry with dn {}", remoteEntry.getDn().getName() );
                    // incase of a MODDN operation resulting in a branch to be moved out of scope
                    // ApacheDS replication provider sends a single delete event on the Dn of the moved branch
                    // so the branch needs to be recursively deleted here
                    deleteRecursive( remoteEntry.getDn(), null );
                    
                    break;

                case PRESENT:
                    LOG.debug( "entry present {}", remoteEntry );
                    break;
            }

            // store the cookie only if the above operation was successful
            if ( syncStateCtrl.getCookie() != null )
            {
                storeCookie();
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }

        LOG.debug( "------------- Ending handleSearchResult ------------" );
    }


    /**
     * {@inheritDoc}
     */
    public void handleSyncInfo( IntermediateResponse syncInfoResp )
    {
        try
        {
            LOG.debug( "............... inside handleSyncInfo ..............." );

            byte[] syncInfoBytes = syncInfoResp.getResponseValue();

            if ( syncInfoBytes == null )
            {
                return;
            }
            
            SyncInfoValueDecorator decorator = new SyncInfoValueDecorator( directoryService.getLdapCodecService() );
            SyncInfoValue syncInfoValue = ( SyncInfoValue ) decorator.decode( syncInfoBytes );

            byte[] cookie = syncInfoValue.getCookie();

            if ( cookie != null )
            {
                LOG.debug( "setting the cookie from the sync info: " + Strings.utf8ToString(cookie) );
                CONSUMER_LOG.debug( "setting the cookie from the sync info: " + Strings.utf8ToString(cookie) );
                syncCookie = cookie;
            }

            LOG.info( "refreshDeletes: " + syncInfoValue.isRefreshDeletes() );

            List<byte[]> uuidList = syncInfoValue.getSyncUUIDs();
            
            // if refreshDeletes set to true then delete all the entries with entryUUID
            // present in the syncIdSet
            if ( syncInfoValue.isRefreshDeletes() )
            {
                deleteEntries( uuidList, false );
            }
            else
            {
                deleteEntries( uuidList, true );
            }

            LOG.info( "refreshDone: " + syncInfoValue.isRefreshDone() );

            storeCookie();
        }
        catch ( Exception de )
        {
            LOG.error( "Failed to handle syncinfo message" );
            de.printStackTrace();
        }

        LOG.debug( ".................... END handleSyncInfo ..............." );
    }


    /**
     * {@inheritDoc}
     */
    public void connectionClosed()
    {
        if ( disconnected )
        {
            return;
        }

        boolean connected = false;

        while ( !connected )
        {
            try
            {
                Thread.sleep( config.getRefreshInterval() );
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Interrupted while sleeping before trying to reconnect", e );
            }

            LOG.debug( "Trying to reconnect" );
            connected = connect();
        }

        startSync();
    }


    /**
     * starts the synchronization operation
     */
    public void startSync()
    {
        CONSUMER_LOG.debug( "Starting the SyncRepl process" );
        
        // read the cookie if persisted
        readCookie();
        
        CONSUMER_LOG.debug( "Cookie read : ''", syncCookie );


        if ( config.isRefreshNPersist() )
        {
            try
            {
                LOG.debug( "==================== Refresh And Persist ==========" );
                doSyncSearch( SynchronizationModeEnum.REFRESH_AND_PERSIST, false );
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to sync with refreshAndPersist mode", e );
            }
        }
        else
        {
            refreshThread = new RefresherThread();
            refreshThread.start();
        }
    }


    /** 
     * {@inheritDoc}
     */
    public void setConfig( ReplicationConsumerConfig config )
    {
        this.config = ( SyncreplConfiguration ) config;
    }


    /**
     * {@inheritDoc}
     */
    public void start()
    {
        connect();
        startSync();
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        disconnect();
    }

    
    /**
     * {@inheritDoc}
     */
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
     * @throws Exception in case of any problems encountered while searching
     */
    private void doSyncSearch( SynchronizationModeEnum syncType, boolean reloadHint ) throws Exception
    {
        CONSUMER_LOG.debug( "In doSyncSearch, mode {}, reloadHint {}", syncType, reloadHint );
        // Prepare the Syncrepl Request
        SyncRequestValue syncReq = new SyncRequestValueDecorator( directoryService.getLdapCodecService() );

        syncReq.setMode( syncType );
        syncReq.setReloadHint( reloadHint );

         // If we have a persisted cookie, send it.
        if ( syncCookie != null )
        {
            LOG.debug( "searching with searchRequest, cookie '{}'", Strings.utf8ToString( syncCookie ) );
            syncReq.setCookie( syncCookie );
        }
        else
        {
            CONSUMER_LOG.debug( "searching with searchRequest, no cookie" );
        }

        searchRequest.addControl( syncReq );

        // Do the search
        SearchFuture sf = connection.searchAsync( searchRequest );

        Response resp = sf.get();
        
        // Now, process the responses. We loop until we have a connection termination or
        // a SearchResultDone (RefreshOnly mode)
        while ( !( resp instanceof SearchResultDone ) && !sf.isCancelled() && !disconnected )
        {
            if ( resp instanceof SearchResultEntry )
            {
                SearchResultEntry result = ( SearchResultEntry ) resp;
                //System.out.println( "++++++++++++>  Consumer has received : " + result.getEntry().getDn() );

                handleSearchResult( result );
            }
            else if ( resp instanceof SearchResultReference )
            {
                handleSearchReference( ( SearchResultReference ) resp );
            }
            else if ( resp instanceof IntermediateResponse )
            {
                handleSyncInfo( (IntermediateResponse) resp );
            }

            // Next entry
            resp = sf.get();
        }

        ResultCodeEnum resultCode = handleSearchDone( ( SearchResultDone ) resp );

        LOG.debug( "sync operation returned result code {}", resultCode );
        
        if ( resultCode == ResultCodeEnum.NO_SUCH_OBJECT )
        {
            // log the error and handle it appropriately
            LOG.warn( "given replication base Dn {} is not found on provider", config.getBaseDn() );
            
            if ( syncType == SynchronizationModeEnum.REFRESH_AND_PERSIST )
            {
                LOG.warn( "disconnecting the consumer running in refreshAndPersist mode from the provider" );
                disconnect();
            }
        }
        else if ( resultCode == ResultCodeEnum.E_SYNC_REFRESH_REQUIRED )
        {
            LOG.info( "unable to perform the content synchronization cause E_SYNC_REFRESH_REQUIRED" );
            
            try
            {
                deleteRecursive( new Dn( config.getBaseDn() ), null );
            }
            catch ( Exception e )
            {
                LOG
                    .error(
                        "Failed to delete the replica base as part of handling E_SYNC_REFRESH_REQUIRED, disconnecting the consumer",
                        e );
            }

            // Do a full update.
            removeCookie();
            doSyncSearch( syncType, true );
        }
    }


    public void disconnect()
    {
        disconnected = true;

        try
        {
            if ( refreshThread != null )
            {
                refreshThread.stopRefreshing();
            }

            connection.unBind();
            LOG.info( "Unbound from the server {}", config.getRemoteHost() );

            connection.close();
            LOG.info( "Connection closed for the server {}", config.getRemoteHost() );

            connection = null;

            // persist the cookie
            storeCookie();

            // reset the cookie
            syncCookie = null;
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to close the connection", e );
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
            if ( config.isStoreCookieInFile() )
            {
                CONSUMER_LOG.debug( "Storingthe cookie in a file : {}", cookieFile );
                FileOutputStream fout = new FileOutputStream( cookieFile );
                fout.write( syncCookie.length );
                fout.write( syncCookie );
                fout.close();
            }
            else
            {
                Attribute attr = cookieMod.getAttribute();
                attr.clear();
                attr.add( syncCookie );

                CONSUMER_LOG.debug( "Storing the cookie in the DIT : {}", config.getConfigEntryDn() );
                
                session.modify( config.getConfigEntryDn(), cookieMod );
                
                Entry entry = session.lookup( config.getConfigEntryDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );
                
                CONSUMER_LOG.debug( "stored entry : {}", entry );
            }

            lastSavedCookie = new byte[syncCookie.length];
            System.arraycopy( syncCookie, 0, lastSavedCookie, 0, syncCookie.length );

            LOG.debug( "stored the cookie" );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to store the cookie", e );
        }
    }


    /**
     * read the cookie
     */
    private void readCookie()
    {
        try
        {
            if ( config.isStoreCookieInFile() )
            {
                CONSUMER_LOG.debug( "The cookie is stored in a file : {}", cookieFile );
                
                if ( cookieFile.exists() && ( cookieFile.length() > 0 ) )
                {
                    FileInputStream fin = new FileInputStream( cookieFile );
                    syncCookie = new byte[fin.read()];
                    fin.read( syncCookie );
                    fin.close();

                    lastSavedCookie = new byte[syncCookie.length];
                    System.arraycopy( syncCookie, 0, lastSavedCookie, 0, syncCookie.length );

                    LOG.debug( "read the cookie from file: " + Strings.utf8ToString(syncCookie) );
                }
            }
            else
            {
                try
                {
                    Entry entry = session.lookup( config.getConfigEntryDn(), SchemaConstants.ALL_ATTRIBUTES_ARRAY );
                    
                    CONSUMER_LOG.debug( "The cookie is stored in the DIT : {}", entry );

                    if ( entry != null )
                    {
                        Attribute attr = entry.get( COOKIE_AT_TYPE );
                        
                        if ( attr != null )
                        {
                            syncCookie = attr.getBytes();
                            lastSavedCookie = syncCookie;
                            CONSUMER_LOG.debug( "Read cookie : '{}'", attr );
                            LOG.debug( "loaded cookie from DIT" );
                        }
                    }
                }
                catch ( Exception e )
                {
                    // can be ignored, most likely happens if there is no entry with the given Dn
                    // log in debug mode
                    LOG.debug( "Failed to read the cookie from the entry", e );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to read the cookie", e );
        }
    }


    /**
     * deletes the cookie and resets the syncCookie to null
     */
    public void removeCookie()
    {
        if ( config.isStoreCookieInFile() )
        {
            if ( cookieFile.exists() && ( cookieFile.length() > 0 ) )
            {
                boolean deleted = cookieFile.delete();
                LOG.info( "deleted cookie file {}", deleted );
            }
        }
        else
        {
            try
            {
                Attribute cookieAttr = new DefaultAttribute( COOKIE_AT_TYPE );
                Modification deleteCookieMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                    cookieAttr );
                session.modify( config.getConfigEntryDn(), deleteCookieMod );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to delete the cookie from the entry with Dn {}", config.getConfigEntryDn() );
                LOG.warn( "{}", e );
            }
        }

        LOG.info( "resetting sync cookie" );

        syncCookie = null;
        lastSavedCookie = null;
    }


    private void applyModDnOperation( Entry remoteEntry, String entryUuid ) throws Exception
    {
        LOG.debug( "MODDN for entry {}, new entry : {}", entryUuid, remoteEntry );
        
        // First, compute the MODDN type
        SyncModifyDnType modDnType = null;
        
        try
        {
            // Retrieve locally the moved or renamed entry
            String filter = "(entryUuid=" + entryUuid + ")";
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( Dn.ROOT_DSE );
            searchRequest.setFilter( filter );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.addAttributes( "entryUuid", "entryCsn", "*" );

            EntryFilteringCursor cursor = session.search( searchRequest );
            cursor.beforeFirst();
            cursor.next();

            Entry localEntry = cursor.get();

            cursor.close();

            // Compute the DN, parentDn and Rdn for both entries
            Dn localDn = localEntry.getDn();
            Dn remoteDn = directoryService.getDnFactory().create( remoteEntry.getDn().getName() );

            Dn localParentDn = localDn.getParent();
            Dn remoteParentDn = directoryService.getDnFactory().create( remoteDn.getParent().getName() );

            Rdn localRdn = localDn.getRdn();
            Rdn remoteRdn = directoryService.getDnFactory().create( remoteDn.getRdn().getName() ).getRdn();

            if ( localRdn.equals( remoteRdn ) )
            {
                // If the RDN are equals, it's a MOVE
                modDnType = SyncModifyDnType.MOVE;
            }
            else if ( localParentDn.equals( remoteParentDn ) )
            {
                // If the parentDn are equals, it's a RENAME
                modDnType = SyncModifyDnType.RENAME;
            }
            else
            {
                // Otherwise, it's a MOVE and RENAME
                modDnType = SyncModifyDnType.MOVE_AND_RENAME;
            }

            // Check if the OldRdn has been deleted
            boolean deleteOldRdn = remoteEntry.contains( localRdn.getNormType(), localRdn.getNormValue() );

            switch ( modDnType )
            {
                case MOVE:
                    LOG.debug( "moving {} to the new parent {}", localDn, remoteParentDn );
                    session.move( localDn, remoteParentDn );
                    
                    break;

                case RENAME:
                    LOG.debug( "renaming the Dn {} with new Rdn {} and deleteOldRdn flag set to {}", new String[]
                        { localDn.getName(), remoteRdn.getName(), String.valueOf( deleteOldRdn ) } );

                    session.rename( localDn, remoteRdn, deleteOldRdn );
                    
                    break;

                case MOVE_AND_RENAME:
                    LOG.debug(
                        "moveAndRename on the Dn {} with new newParent Dn {}, new Rdn {} and deleteOldRdn flag set to {}",
                        new String[]
                            { localDn.getName(), remoteParentDn.getName(), remoteRdn.getName(), String.valueOf( deleteOldRdn ) } );

                    session.moveAndRename( localDn, remoteParentDn, remoteRdn, deleteOldRdn );
                    
                    break;
            }
        }
        catch ( Exception e )
        {
            throw e;
        }
    }


    private void modify( Entry remoteEntry ) throws Exception
    {
        Entry localEntry = session.lookup( remoteEntry.getDn() );

        remoteEntry.removeAttributes( MOD_IGNORE_AT );

        List<Modification> mods = new ArrayList<Modification>();
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

        session.modify( remoteEntry.getDn(), mods );
    }


    /**
     * deletes the entries having the UUID given in the list
     *
     * @param uuidList the list of UUIDs
     * @throws Exception in case of any problems while deleting the entries
     */
    public void deleteEntries( List<byte[]> uuidList, boolean isRefreshPresent ) throws Exception
    {
        if ( uuidList == null || uuidList.isEmpty() )
        {
            return;
        }

        for ( byte[] uuid : uuidList )
        {
            LOG.info( "uuid: {}", Strings.uuidToString(uuid) );
        }

        // if it is refreshPresent list then send all the UUIDs for
        // filtering, otherwise breaking the list will cause the
        // other present entries to be deleted from DIT
        if ( isRefreshPresent )
        {
            LOG.debug( "refresh present syncinfo list has {} UUIDs", uuidList.size() );
            _deleteEntries_( uuidList, isRefreshPresent );
            return;
        }

        int NODE_LIMIT = 10;

        int count = uuidList.size() / NODE_LIMIT;

        int startIndex = 0;
        int i = 0;
        for ( ; i < count; i++ )
        {
            startIndex = i * NODE_LIMIT;
            _deleteEntries_( uuidList.subList( startIndex, startIndex + NODE_LIMIT ), isRefreshPresent );
        }

        if ( ( uuidList.size() % NODE_LIMIT ) != 0 )
        {
            // remove the remaining entries
            if ( count > 0 )
            {
                startIndex = i * NODE_LIMIT;
            }
            _deleteEntries_( uuidList.subList( startIndex, uuidList.size() ), isRefreshPresent );
        }
    }


    /**
     * do not call this method directly, instead call deleteEntries()
     *
     * @param limitedUuidList a list of UUIDs whose size is less than or equal to #NODE_LIMIT (node limit applies only for refreshDeletes list)
     * @param isRefreshPresent a flag indicating the type of entries present in the UUID list
     */
    private void _deleteEntries_( List<byte[]> limitedUuidList, boolean isRefreshPresent ) throws Exception
    {
        ExprNode filter = null;
        int size = limitedUuidList.size();
        if ( size == 1 )
        {
            String uuid = Strings.uuidToString(limitedUuidList.get(0));
            filter = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT,
                new org.apache.directory.shared.ldap.model.entry.StringValue( uuid ) );
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
                String uuid = Strings.uuidToString(limitedUuidList.get(i));
                ExprNode uuidEqNode = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT,
                    new org.apache.directory.shared.ldap.model.entry.StringValue( uuid ) );

                if ( isRefreshPresent )
                {
                    uuidEqNode = new NotNode( uuidEqNode );
                    ( (AndNode) filter ).addNode( uuidEqNode );
                }
                else
                {
                    ( (OrNode) filter ).addNode( uuidEqNode );
                }
            }
        }

        Dn dn = new Dn( schemaManager, config.getBaseDn() );

        LOG.debug( "selecting entries to be deleted using filter {}", filter.toString() );
        EntryFilteringCursor cursor = session.search( dn, SearchScope.SUBTREE, filter,
            AliasDerefMode.NEVER_DEREF_ALIASES, ENTRY_UUID_ATOP_SET );
        cursor.beforeFirst();

        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            deleteRecursive( entry.getDn(), null );
        }

        cursor.close();
    }

    /**
     * A Thread implementation for synchronizing the DIT in refreshOnly mode
     */
    private class RefresherThread extends Thread
    {
        /** A field used to tell the thread it should stop */
        private volatile boolean stop = false;
        
        /** A mutex used to make the thread sleeping for a moment */
        private final Object mutex = new Object();

        public RefresherThread()
        {
            setDaemon( true );
        }


        public void run()
        {
            while ( !stop )
            {
                LOG.debug( "==================== Refresh Only ==========" );

                try
                {
                    doSyncSearch( SynchronizationModeEnum.REFRESH_ONLY, false );

                    LOG.info( "--------------------- Sleep for a little while ------------------" );
                    mutex.wait( config.getRefreshInterval() );
                    LOG.debug( "--------------------- syncing again ------------------" );

                }
                catch ( InterruptedException ie )
                {
                    LOG.warn( "refresher thread interrupted" );
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to sync with refresh only mode", e );
                }
            }
        }


        public void stopRefreshing()
        {
            stop = true;
            
            // just in case if it is sleeping, wake up the thread
            mutex.notify();
        }
    }


    /**
     * removes all child entries present under the given Dn and finally the Dn itself
     *
     * Working:
     *          This is a recursive function which maintains a Map<Dn,Cursor>.
     *          The way the cascade delete works is by checking for children for a
     *          given Dn(i.e opening a search cursor) and if the cursor is empty
     *          then delete the Dn else for each entry's Dn present in cursor call
     *          deleteChildren() with the Dn and the reference to the map.
     *
     *          The reason for opening a search cursor is based on an assumption
     *          that an entry *might* contain children, consider the below DIT fragment
     *
     *          parent
     *          /     \
     *        child1   child2
     *                 /     \
     *               grand21  grand22
     *
     *           The below method works better in the case where the tree depth is >1
     *
     *   In the case of passing a non-null DeleteListener, the return value will always be null, cause the
     *   operation is treated as asynchronous and response result will be sent using the listener callback
     *
     * @param rootDn the Dn which will be removed after removing its children
     * @param map a map to hold the Cursor related to a Dn
     * @throws Exception If the Dn is not valid or if the deletion failed
     */
    private void deleteRecursive( Dn rootDn, Map<Dn, EntryFilteringCursor> cursorMap ) throws Exception
    {
        LOG.debug( "searching for {}", rootDn.getName() );
        EntryFilteringCursor cursor = null;

        try
        {
            if ( cursorMap == null )
            {
                cursorMap = new HashMap<Dn, EntryFilteringCursor>();
            }

            cursor = cursorMap.get( rootDn );

            if ( cursor == null )
            {
                cursor = session.search( rootDn, SearchScope.ONELEVEL, ENTRY_UUID_PRESENCE_FILTER,
                    AliasDerefMode.NEVER_DEREF_ALIASES, ENTRY_UUID_ATOP_SET );
                cursor.beforeFirst();
                LOG.debug( "putting cursor for {}", rootDn.getName() );
                cursorMap.put( rootDn, cursor );
            }

            if ( !cursor.next() ) // if this is a leaf entry's Dn
            {
                LOG.debug( "deleting {}", rootDn.getName() );
                cursorMap.remove( rootDn );
                cursor.close();
                session.delete( rootDn );
            }
            else
            {
                do
                {
                    Entry entry = cursor.get();

                    deleteRecursive( entry.getDn(), cursorMap );
                }
                while ( cursor.next() );

                cursorMap.remove( rootDn );
                cursor.close();
                LOG.debug( "deleting {}", rootDn.getName() );
                session.delete( rootDn );
            }
        }
        catch ( Exception e )
        {
            String msg = "Failed to delete child entries under the Dn " + rootDn.getName();
            LOG.error( msg, e );
            throw e;
        }
    }

}
