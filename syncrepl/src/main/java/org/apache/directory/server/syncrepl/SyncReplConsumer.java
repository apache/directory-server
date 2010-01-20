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
package org.apache.directory.server.syncrepl;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.naming.ldap.Control;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.listeners.IntermediateResponseListener;
import org.apache.directory.shared.ldap.client.api.listeners.SearchListener;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.client.api.messages.IntermediateResponse;
import org.apache.directory.shared.ldap.client.api.messages.LdapResult;
import org.apache.directory.shared.ldap.client.api.messages.SearchRequest;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultDone;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultReference;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlDecoder;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SyncDoneValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncRequestValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * An agent capable of communicate with some LDAP servers.
 * 
 * TODO write test cases
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncReplConsumer implements SearchListener, IntermediateResponseListener
{

    /** the syncrepl configuration */
    private SyncreplConfiguration config;

    /** the sync cookie sent by the server */
    private byte[] syncCookie;

    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SyncReplConsumer.class.getSimpleName() );

    /** conection to the syncrepl provider */
    private LdapConnection connection;

    /** the search request with control */
    private SearchRequest searchRequest;

    /** the syncrequest control */
    private SyncRequestValueControl syncReq;

    /** a reference to the directoryService */
    private DirectoryService directoryService;

    /** the schema manager */
    private SchemaManager schemaManager;

    /** the decoder for syncinfovalue control */
    private SyncInfoValueControlDecoder decoder = new SyncInfoValueControlDecoder();

    /** the cookie file */
    private File cookieFile;

    /** flag to indicate whether the consumer was diconncted */
    private boolean disconnected;

    /** the core session */
    private CoreSession session;

    private SyncDoneValueControlDecoder syncDoneControlDecoder = new SyncDoneValueControlDecoder();

    private SyncStateValueControlDecoder syncStateControlDecoder = new SyncStateValueControlDecoder();

    /** attributes on which modification should be ignored */
    private static final String[] MOD_IGNORE_AT = new String[] { "entryUUID", "entryCSN" }; //{ "1.3.6.1.1.16.4", "1.3.6.1.4.1.4203.666.1.7" };

    /** flag to indicate whether the current phase is for deleting entries */
    private boolean refreshDeletes;

    /** flag set after receiving refreshPresent Sync Info message */
    private boolean refreshDone;

    private RefresherThread refreshThread;


    /**
     * @return the config
     */
    public SyncreplConfiguration getConfig()
    {
        return config;
    }


    /**
     * @param config the config to set
     */
    public void setConfig( SyncreplConfiguration config )
    {
        this.config = config;
    }


    public void init( DirectoryService directoryservice ) throws Exception
    {
        this.directoryService = directoryservice;

        File cookieDir = new File( directoryservice.getWorkingDirectory(), "cookies" );
        cookieDir.mkdir();

        cookieFile = new File( cookieDir, String.valueOf( config.getReplicaId() ) );

        session = directoryService.getAdminSession();

        schemaManager = directoryservice.getSchemaManager();
    }


    public boolean bind()
    {
        try
        {
            String providerHost = config.getProviderHost();
            int port = config.getPort();

            // Create a connection
            if ( connection == null )
            {
                connection = new LdapConnection( providerHost, port );
            }

            // Do a bind
            BindResponse bindResponse = connection.bind( config.getBindDn(), config.getCredentials() );

            // Check that it' not null and valid
            if ( bindResponse == null )
            {
                LOG.error( "Failed to bind with the given bindDN and credentials", bindResponse );
                return false;
            }

            // Now get the result
            LdapResult ldapResult = bindResponse.getLdapResult();

            if ( ldapResult.getResultCode() != ResultCodeEnum.SUCCESS )
            {
                LOG.warn( "Failed to bind on the server : {}", ldapResult );
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
    public void prepareSyncSearchRequest()
    {
        String baseDn = config.getBaseDn();

        searchRequest = new SearchRequest();

        searchRequest.setBaseDn( baseDn );
        searchRequest.setFilter( config.getFilter() );
        searchRequest.setSizeLimit( config.getSearchSizeLimit() );
        searchRequest.setTimeLimit( config.getSearchTimeout() );

        // the only valid values are NEVER_DEREF_ALIASES and DEREF_FINDING_BASE_OBJ
        searchRequest.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
        searchRequest.setScope( SearchScope.getSearchScope( config.getSearchScope() ) );
        searchRequest.setTypesOnly( false );

        searchRequest.addAttributes( config.getAttributes() );

        syncReq = new SyncRequestValueControl();

        if ( config.isRefreshPersist() )
        {
            syncReq.setMode( SynchronizationModeEnum.REFRESH_AND_PERSIST );
        }
        else
        {
            syncReq.setMode( SynchronizationModeEnum.REFRESH_ONLY );
        }

        syncReq.setReloadHint( false );
    }


    public void handleSearchDone( SearchResultDone searchDone )
    {
        LOG.debug( "///////////////// handleSearchDone //////////////////" );

        Control ctrl = searchDone.getControl( SyncDoneValueControl.CONTROL_OID );
        SyncDoneValueControlCodec syncDoneCtrl = null;
        try
        {
            syncDoneCtrl = ( SyncDoneValueControlCodec ) syncDoneControlDecoder.decode( ctrl.getEncodedValue() );
            refreshDeletes = syncDoneCtrl.isRefreshDeletes();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to decode the syncDoneControlCodec", e );
        }

        if ( syncDoneCtrl.getCookie() != null )
        {
            syncCookie = syncDoneCtrl.getCookie();
            LOG.debug( "assigning cookie from sync done value control: " + StringTools.utf8ToString( syncCookie ) );
        }

        LOG.debug( "//////////////// END handleSearchDone//////////////////////" );
    }


    public void handleSearchReference( SearchResultReference searchRef )
    {
        LOG.error( "!!!!!!!!!!!!!!!!! TODO handle SearchReference messages !!!!!!!!!!!!!!!!" );
    }


    public void handleSearchResult( SearchResultEntry syncResult )
    {

        LOG.debug( "------------- starting handleSearchResult ------------" );

        try
        {
            Entry remoteEntry = syncResult.getEntry();

            // for refreshOnly
            if ( !config.isRefreshPersist() )
            {
                if ( !refreshDeletes )
                {
                    LOG.info( "the number of attributes present in the entry {} during present phase {}", remoteEntry
                        .getDn().getName(), remoteEntry.size() );
                }
            }

            Control ctrl = syncResult.getControl( SyncStateValueControl.CONTROL_OID );
            SyncStateValueControlCodec syncStateCtrl = null;

            try
            {
                syncStateCtrl = ( SyncStateValueControlCodec ) syncStateControlDecoder.decode( ctrl.getEncodedValue() );
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to decode syncStateControl", e );
            }

            if ( syncStateCtrl.getCookie() != null )
            {
                syncCookie = syncStateCtrl.getCookie();
                LOG.debug( "assigning the cookie from sync state value control: "
                    + StringTools.utf8ToString( syncCookie ) );
            }

            SyncStateTypeEnum state = syncStateCtrl.getSyncStateType();

            LOG.debug( "state name {}", state.name() );

            LOG.debug( "entryUUID = {}", StringTools.uuidToString( syncStateCtrl.getEntryUUID() ) );

            switch ( state )
            {
                case ADD:
                    if ( !session.exists( remoteEntry.getDn() ) )
                    {
                        LOG.debug( "adding entry with dn {}", remoteEntry.getDn().getName() );
                        LOG.debug( remoteEntry.toString() );
                        session.add( new DefaultServerEntry( schemaManager, remoteEntry ) );
                    }

                    break;

                case MODIFY:
                    modify( remoteEntry );
                    break;

                case DELETE:
                    LOG.debug( "deleting entry with dn {}", remoteEntry.getDn().getName() );
                    session.delete( remoteEntry.getDn() );
                    break;
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
    public void handleSyncInfo( byte[] syncinfo )
    {
        try
        {
            LOG.debug( "............... inside handleSyncInfo ..............." );

            SyncInfoValueControlCodec syncInfoValue = ( SyncInfoValueControlCodec ) decoder.decode( syncinfo );

            byte[] cookie = syncInfoValue.getCookie();

            if ( cookie != null )
            {
                LOG.debug( "setting the cookie from the sync info: " + StringTools.utf8ToString( cookie ) );
                syncCookie = cookie;
            }

            List<byte[]> uuidList = syncInfoValue.getSyncUUIDs();

            LOG.info( "refreshDeletes: " + syncInfoValue.isRefreshDeletes() );
            if ( uuidList != null )
            {
                for ( byte[] uuid : uuidList )
                {
                    LOG.info( "uuid: {}", StringTools.utf8ToString( uuid ) );
                }
            }

            refreshDeletes = syncInfoValue.isRefreshDeletes();
            refreshDone = syncInfoValue.isRefreshDone();

            // if refreshDeletes set to true then delete all the entries with entryUUID
            // present in the syncIdSet 
            if ( syncInfoValue.isRefreshDeletes() && ( uuidList != null ) )
            {
                for ( byte[] uuid : uuidList )
                {
                    // TODO similar to delete based on DN there should be 
                    // a method to delete an Entry based on entryUUID
                    LOG.debug( "FIXME deleting the entry with entryUUID: {}", UUID.nameUUIDFromBytes( uuid ) );
                }
            }

            LOG.info( "refreshDone: " + syncInfoValue.isRefreshDone() );
        }
        catch ( DecoderException de )
        {
            LOG.error( "Failed to handle syncinfo message" );
            de.printStackTrace();
        }

        LOG.debug( ".................... END handleSyncInfo ..............." );
    }


    /**
     * {@inheritDoc}
     */
    public void handleSessionClosed()
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
                Thread.sleep( config.getConsumerInterval() );
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Interrupted while sleeping before trying to reconnect", e );
            }

            LOG.debug( "Trying to reconnect" );
            connected = bind();
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

        if ( config.isRefreshPersist() )
        {
            try
            {
                LOG.debug( "==================== Refresh And Persist ==========" );
                doSyncSearch( SynchronizationModeEnum.REFRESH_AND_PERSIST );
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
    private void doSyncSearch( SynchronizationModeEnum syncType ) throws Exception
    {
        SyncRequestValueControl syncReq = new SyncRequestValueControl();

        syncReq.setMode( syncType );
        if ( syncCookie != null )
        {
            LOG.debug( "searching with searchRequest, cookie '{}'", StringTools.utf8ToString( syncCookie ) );
            syncReq.setCookie( syncCookie );
        }

        searchRequest.add( syncReq );

        // Do the search
        connection.search( searchRequest, this );
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
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to close the connection", e );
        }

    }


    /**
     * stores the cookie in a file.
     */
    private void storeCookie()
    {
        if ( syncCookie == null )
        {
            return;
        }

        try
        {
            FileOutputStream fout = new FileOutputStream( cookieFile );
            fout.write( syncCookie.length );
            fout.write( syncCookie );
            fout.close();

            LOG.debug( "stored the cookie" );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to store the cookie", e );
        }
    }


    /**
     * read the cookie from a file(if exists).
     */
    private void readCookie()
    {
        try
        {
            if ( cookieFile.exists() && ( cookieFile.length() > 0 ) )
            {
                FileInputStream fin = new FileInputStream( cookieFile );
                syncCookie = new byte[fin.read()];
                fin.read( syncCookie );
                fin.close();

                LOG.debug( "read the cookie from file: " + StringTools.utf8ToString( syncCookie ) );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to read the cookie", e );
        }
    }


    /**
     * deletes the cookie file(if exists) 
     */
    public void deleteCookieFile()
    {
        if ( cookieFile != null && cookieFile.exists() )
        {
            LOG.debug( "deleting the cookie file" );
            cookieFile.delete();
        }
    }


    private void modify( Entry remoteEntry ) throws Exception
    {
        LOG.debug( "modifying entry with dn {}", remoteEntry.getDn().getName() );

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
                mod = new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, remoteAttr );
                remoteEntry.remove( remoteAttr );
            }
            else
            {
                mod = new ServerModification( ModificationOperation.REMOVE_ATTRIBUTE, localAttr );
            }

            mods.add( mod );
        }

        if ( remoteEntry.size() > 0 )
        {
            itr = remoteEntry.iterator();
            while ( itr.hasNext() )
            {
                mods.add( new ServerModification( ModificationOperation.ADD_ATTRIBUTE, itr.next() ) );
            }
        }

        session.modify( remoteEntry.getDn(), mods );
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
                    doSyncSearch( SynchronizationModeEnum.REFRESH_ONLY );

                    LOG.info( "--------------------- Sleep for a little while ------------------" );
                    Thread.sleep( config.getConsumerInterval() );
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


    //====================== SearchListener methods ====================================

    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.client.api.listeners.SearchListener#entryFound(org.apache.directory.shared.ldap.client.api.LdapConnection, org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry)
     */
    public void entryFound( LdapConnection connection, SearchResultEntry searchResultEntry ) throws LdapException
    {
        handleSearchResult( searchResultEntry );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.client.api.listeners.SearchListener#referralFound(org.apache.directory.shared.ldap.client.api.LdapConnection, org.apache.directory.shared.ldap.client.api.messages.SearchResultReference)
     */
    public void referralFound( LdapConnection connection, SearchResultReference searchResultReference )
        throws LdapException
    {
        handleSearchReference( searchResultReference );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.client.api.listeners.SearchListener#searchDone(org.apache.directory.shared.ldap.client.api.LdapConnection, org.apache.directory.shared.ldap.client.api.messages.SearchResultDone)
     */
    public void searchDone( LdapConnection connection, SearchResultDone searchResultDone ) throws LdapException
    {
        handleSearchDone( searchResultDone );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.client.api.listeners.IntermediateResponseListener#responseReceived(org.apache.directory.shared.ldap.client.api.LdapConnection, org.apache.directory.shared.ldap.client.api.messages.IntermediateResponse)
     */
    public void responseReceived( LdapConnection connection, IntermediateResponse intermediateResponse )
    {
        handleSyncInfo( intermediateResponse.getResponseValue() );
    }

}
