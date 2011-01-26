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
package org.apache.directory.server.ldap.replication;


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
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITDecorator;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncmodifydn.SyncModifyDnControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncmodifydn.SyncModifyDnControlDecoder;
import org.apache.directory.shared.ldap.message.control.replication.SyncModifyDnType;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.NotNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Implementation of syncrepl slave a.k.a consumer.
 *
 * TODO write test cases
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncReplConsumer implements ConnectionClosedEventListener
{

    /** the syncrepl configuration */
    private SyncreplConfiguration config;

    /** the sync cookie sent by the server */
    private byte[] syncCookie;

    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SyncReplConsumer.class );

    /** connection to the syncrepl provider */
    private LdapNetworkConnection connection;

    /** the search request with control */
    private SearchRequest searchRequest;

    /** a reference to the directoryService */
    private DirectoryService directoryService;

    /** the schema manager */
    private SchemaManager schemaManager;

    /** the decoder for syncinfovalue control */
    private SyncInfoValueControlDecoder decoder = new SyncInfoValueControlDecoder();

    /** the cookie file */
    private File cookieFile;

    /** flag to indicate whether the consumer was disconnected */
    private boolean disconnected;

    /** the core session */
    private CoreSession session;

    private SyncDoneValueControlDecoder syncDoneControlDecoder = new SyncDoneValueControlDecoder();

    private SyncStateValueControlDecoder syncStateControlDecoder = new SyncStateValueControlDecoder();

    private SyncModifyDnControlDecoder syncModifyDnControlDecoder = new SyncModifyDnControlDecoder();

    /** attributes on which modification should be ignored */
    private static final String[] MOD_IGNORE_AT = new String[]
        { SchemaConstants.ENTRY_UUID_AT, SchemaConstants.ENTRY_CSN_AT, SchemaConstants.MODIFIERS_NAME_AT,
            SchemaConstants.MODIFY_TIMESTAMP_AT, SchemaConstants.CREATE_TIMESTAMP_AT, SchemaConstants.CREATORS_NAME_AT };

    private RefresherThread refreshThread;

    /** the cookie that was saved last time */
    private byte[] lastSavedCookie;

    private static AttributeType ENTRY_UUID_AT;

    private static final PresenceNode ENTRY_UUID_PRESENCE_FILTER = new PresenceNode( SchemaConstants.ENTRY_UUID_AT );

    private static final Set<AttributeTypeOptions> ENTRY_UUID_ATOP_SET = new HashSet<AttributeTypeOptions>();

    private List<Modification> cookieModLst;

    private Dn configEntryDn;

    private static AttributeType COOKIE_AT_TYPE;


    /**
     * @return the config
     */
    public SyncreplConfiguration getConfig()
    {
        return config;
    }


    public void init( DirectoryService directoryservice, SyncreplConfiguration config ) throws Exception
    {
        this.config = config;
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

        ENTRY_UUID_ATOP_SET.add( new AttributeTypeOptions( ENTRY_UUID_AT ) );

        COOKIE_AT_TYPE = schemaManager.lookupAttributeTypeRegistry( "ads-replCookie" );
        EntryAttribute cookieAttr = new DefaultEntryAttribute( COOKIE_AT_TYPE );

        Modification cookieMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, cookieAttr );
        cookieModLst = new ArrayList<Modification>( 1 );
        cookieModLst.add( cookieMod );

        configEntryDn = new Dn( config.getConfigEntryDn(), schemaManager );

        prepareSyncSearchRequest();
    }


    public boolean connect()
    {
        try
        {
            String providerHost = config.getProviderHost();
            int port = config.getPort();
            
            // Create a connection
            if ( connection == null )
            {
                connection = new LdapNetworkConnection( providerHost, port );
                
                if( config.isUseTls() )
                {
                    connection.getConfig().setTrustManagers( config.getTrustManager() );
                    connection.startTls();
                }
                
                connection.addConnectionClosedEventListener( this );
            }

            // Do a bind
            BindResponse bindResponse = connection.bind( config.getReplUserDn(), config.getReplUserPassword() );

            // Now get the result
            LdapResult ldapResult = bindResponse.getLdapResult();

            if ( ldapResult.getResultCode() != ResultCodeEnum.SUCCESS )
            {
                LOG.warn( "Failed to bind to the server with the given bind Dn {} and credentials: {}", config.getReplUserDn(), ldapResult );
            }
            else
            {
                return true;
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
            searchRequest.addControl( new ManageDsaITDecorator() );
        }
    }


    public ResultCodeEnum handleSearchDone( SearchResultDone searchDone )
    {
        LOG.debug( "///////////////// handleSearchDone //////////////////" );

        Control ctrl = searchDone.getControls().get( SyncDoneValueControl.CONTROL_OID );
        SyncDoneValueControl syncDoneCtrl = new SyncDoneValueControl();
        try
        {
            if ( ctrl != null )
            {
                syncDoneCtrl = ( SyncDoneValueControl ) syncDoneControlDecoder.decode( ctrl.getValue(), syncDoneCtrl );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to decode the syncDoneControlCodec", e );
        }

        if ( syncDoneCtrl.getCookie() != null )
        {
            syncCookie = syncDoneCtrl.getCookie();
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

        SyncStateValueControl syncStateCtrl = new SyncStateValueControl();

        try
        {
            Entry remoteEntry = syncResult.getEntry();

            Control ctrl = syncResult.getControls().get( SyncStateValueControl.CONTROL_OID );

            try
            {
                syncStateCtrl = ( SyncStateValueControl ) syncStateControlDecoder.decode( ctrl.getValue(),
                    syncStateCtrl );
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to decode syncStateControl", e );
            }

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
                    if ( !session.exists( remoteEntry.getDn() ) )
                    {
                        LOG.debug( "adding entry with dn {}", remoteEntry.getDn().getName() );
                        LOG.debug( remoteEntry.toString() );
                        session.add( new DefaultEntry( schemaManager, remoteEntry ) );
                    }
                    else
                    {
                        LOG.debug( "updating entry in refreshOnly mode {}", remoteEntry.getDn().getName() );
                        modify( remoteEntry );
                    }

                    break;

                case MODIFY:
                    LOG.debug( "modifying entry with dn {}", remoteEntry.getDn().getName() );
                    modify( remoteEntry );
                    break;

                case MODDN:
                    Control adsModDnControl = syncResult.getControls().get( SyncModifyDnControl.CONTROL_OID );
                    //Apache Directory Server's special control
                    SyncModifyDnControl syncModDnControl = new SyncModifyDnControl();

                    LOG.debug( "decoding the SyncModifyDnControl.." );
                    syncModDnControl = ( SyncModifyDnControl ) syncModifyDnControlDecoder.decode( adsModDnControl
                        .getValue(), syncModDnControl );

                    applyModDnOperation( syncModDnControl );
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

            byte[] syncinfo = syncInfoResp.getResponseValue();
            SyncInfoValueControl syncInfoValue = ( SyncInfoValueControl ) decoder.decode( syncinfo, null );

            byte[] cookie = syncInfoValue.getCookie();

            if ( cookie != null )
            {
                LOG.debug( "setting the cookie from the sync info: " + Strings.utf8ToString(cookie) );
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
        // read the cookie if persisted
        readCookie();

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
     * performs a search on connection with updated syncRequest control.
     *
     * @throws Exception in case of any problems encountered while searching
     */
    private void doSyncSearch( SynchronizationModeEnum syncType, boolean reloadHint ) throws Exception
    {
        SyncRequestValueControl syncReq = new SyncRequestValueControl();

        syncReq.setMode( syncType );
        syncReq.setReloadHint( reloadHint );

        if ( syncCookie != null )
        {
            LOG.debug( "searching with searchRequest, cookie '{}'", Strings.utf8ToString(syncCookie) );
            syncReq.setCookie( syncCookie );
        }

        searchRequest.addControl( syncReq );

        // Do the search
        SearchFuture sf = connection.searchAsync( searchRequest );

        Response resp = sf.get();

        while ( !( resp instanceof SearchResultDone ) && !sf.isCancelled() )
        {
            if ( resp instanceof SearchResultEntry )
            {
                handleSearchResult( ( SearchResultEntry ) resp );
            }
            else if ( resp instanceof SearchResultReference )
            {
                handleSearchReference( ( SearchResultReference ) resp );
            }
            else if ( resp instanceof IntermediateResponse )
            {
                handleSyncInfo( (IntermediateResponse) resp );
            }

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
                disconnet();
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
                disconnet();
            }

            removeCookie();
            doSyncSearch( syncType, true );
        }
    }


    public void disconnet()
    {
        disconnected = true;

        try
        {
            if ( refreshThread != null )
            {
                refreshThread.stopRefreshing();
            }

            connection.unBind();
            LOG.info( "Unbound from the server {}", config.getProviderHost() );

            connection.close();
            LOG.info( "Connection closed for the server {}", config.getProviderHost() );

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
        if ( syncCookie == null )
        {
            return;
        }

        if ( lastSavedCookie != null && Arrays.equals( syncCookie, lastSavedCookie ) )
        {
            return;
        }

        try
        {
            if ( config.isStoreCookieInFile() )
            {
                FileOutputStream fout = new FileOutputStream( cookieFile );
                fout.write( syncCookie.length );
                fout.write( syncCookie );
                fout.close();
            }
            else
            {
                EntryAttribute attr = cookieModLst.get( 0 ).getAttribute();
                attr.clear();
                attr.add( syncCookie );

                session.modify( configEntryDn, cookieModLst );
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
                    Entry entry = session.lookup( configEntryDn, new String[]
                        { COOKIE_AT_TYPE.getName() } );
                    if ( entry != null )
                    {
                        EntryAttribute attr = entry.get( COOKIE_AT_TYPE );
                        if ( attr != null )
                        {
                            syncCookie = attr.getBytes();
                            lastSavedCookie = syncCookie;
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
                EntryAttribute cookieAttr = new DefaultEntryAttribute( COOKIE_AT_TYPE );
                Modification deleteCookieMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                    cookieAttr );
                List<Modification> deleteModLst = new ArrayList<Modification>();
                deleteModLst.add( deleteCookieMod );
                session.modify( configEntryDn, deleteModLst );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to delete the cookie from the entry with Dn {}", configEntryDn );
                LOG.warn( "{}", e );
            }
        }

        LOG.info( "resetting sync cookie" );

        syncCookie = null;
        lastSavedCookie = null;
    }


    private void applyModDnOperation( SyncModifyDnControl modDnControl ) throws Exception
    {
        SyncModifyDnType modDnType = modDnControl.getModDnType();

        Dn entryDn = new Dn( modDnControl.getEntryDn() );
        switch ( modDnType )
        {
            case MOVE:

                LOG.debug( "moving {} to the new parent {}", entryDn, modDnControl.getNewSuperiorDn() );

                session.move( entryDn, new Dn( modDnControl.getNewSuperiorDn() ) );
                break;

            case RENAME:

                Rdn newRdn = new Rdn( modDnControl.getNewRdn() );
                boolean deleteOldRdn = modDnControl.isDeleteOldRdn();
                LOG.debug( "renaming the Dn {} with new Rdn {} and deleteOldRdn flag set to {}", new String[]
                    { entryDn.getName(), newRdn.getName(), String.valueOf( deleteOldRdn ) } );

                session.rename( entryDn, newRdn, deleteOldRdn );
                break;

            case MOVEANDRENAME:

                Dn newParentDn = new Dn( modDnControl.getNewSuperiorDn() );
                newRdn = new Rdn( modDnControl.getNewRdn() );
                deleteOldRdn = modDnControl.isDeleteOldRdn();

                LOG.debug(
                    "moveAndRename on the Dn {} with new newParent Dn {}, new Rdn {} and deleteOldRdn flag set to {}",
                    new String[]
                        { entryDn.getName(), newParentDn.getName(), newRdn.getName(), String.valueOf( deleteOldRdn ) } );

                session.moveAndRename( entryDn, newParentDn, newRdn, deleteOldRdn );
        }
    }


    private void modify( Entry remoteEntry ) throws Exception
    {
        Entry localEntry = session.lookup( remoteEntry.getDn() );

        remoteEntry.removeAttributes( MOD_IGNORE_AT );

        List<Modification> mods = new ArrayList<Modification>();
        Iterator<EntryAttribute> itr = localEntry.iterator();

        while ( itr.hasNext() )
        {
            EntryAttribute localAttr = itr.next();
            String attrId = localAttr.getId();
            Modification mod;
            EntryAttribute remoteAttr = remoteEntry.get( attrId );

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

        Dn dn = new Dn( config.getBaseDn(), schemaManager );

        LOG.debug( "selecting entries to be deleted using filter {}", filter.toString() );
        EntryFilteringCursor cursor = session.search( dn, SearchScope.SUBTREE, filter,
            AliasDerefMode.NEVER_DEREF_ALIASES, ENTRY_UUID_ATOP_SET );
        cursor.beforeFirst();

        while ( cursor.next() )
        {
            ClonedServerEntry entry = cursor.get();
            deleteRecursive( entry.getDn(), null );
        }

        cursor.close();
    }

    /**
     * A Thread implementation for synchronizing the DIT in refreshOnly mode
     */
    private class RefresherThread extends Thread
    {
        private volatile boolean stop;


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
                    Thread.sleep( config.getRefreshInterval() );
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
            // just incase if it is sleeping
            this.interrupt();
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
                    ClonedServerEntry entry = cursor.get();

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
