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
package org.apache.directory.mitosis.syncrepl;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.Control;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.codec.LdapResult;
import org.apache.directory.shared.ldap.codec.TwixTransformer;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.codec.search.SearchRequest;
import org.apache.directory.shared.ldap.codec.search.SearchResultDone;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntry;
import org.apache.directory.shared.ldap.codec.search.SearchResultReference;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SyncRequestValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
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
public class SyncreplConsumer implements ConsumerCallback
{

    /** the syncrepl configuration */
    private SyncreplConfiguration config;

    /** the sync cookie sent by the server */
    private byte[] syncCookie;

    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SyncreplConsumer.class );

    /** conection to the syncrepl provider */
    private LdapConnection connection;

    /** the search request with control */
    private SearchRequest searchRequest;

    /** the syncrequest control */
    private SyncRequestValueControl syncReq;

    /** a reference to the directoryservice */
    private DirectoryService directoryService;

    /** the decoder for syncinfovalue control */
    private SyncInfoValueControlDecoder decoder = new SyncInfoValueControlDecoder();

    /** the cookie file */
    private File cookieFile;
    
    /** flag to indicate whether the consumer was diconncted */
    private boolean disconnected;

    /** the core session */
    private CoreSession session;
    
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


    /**
     * A helper method to quickly quit the program
     */
    private static void quit( LdapConnection connection ) throws IOException
    {
        connection.close();
        System.exit( 1 );
    }


    /**
     * A helper method to check that we didn't get an error.
     */
    private static void checkldapResult( LdapConnection connection, LdapResult ldapResult ) throws IOException
    {
        if ( ldapResult.getResultCode() != ResultCodeEnum.SUCCESS )
        {
            LOG.debug( "failed to bind on the server : " + ldapResult );
            quit( connection );
        }
    }


    public void init( DirectoryService directoryservice ) throws Exception
    {
        this.directoryService = directoryservice;

        File cookieDir = new File( directoryservice.getWorkingDirectory(), "cookies" );
        cookieDir.mkdir();
        
        cookieFile = new File( cookieDir, String.valueOf( config.getReplicaId() ) );
        
        session = directoryService.getAdminSession();
    }


    public boolean connect()
    {
        String providerHost = config.getProviderHost();
        int port = config.getPort();

        // Create a connection
        connection = new LdapConnectionImpl( providerHost, port );

        try
        {
            // Connect to the server
            boolean connected = connection.connect();

            if ( !connected )
            {
                LOG.warn( "Failed to connect to the syncrepl provder host {} running at port {}", providerHost, port );
            }
            else
            {
                connection.addConsumer( this );
                return connected;
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to connect to the syncrepl provder host {} running at port {}", providerHost, port );
            LOG.error( e.getMessage(), e );
        }

        return false;
    }


    public boolean bind()
    {
        try
        {
            // Do a bind
            LdapResponse bindResponse = connection.bind( config.getBindDn(), config.getCredentials() );

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
        try
        {
            searchRequest.setBaseObject( new LdapDN( baseDn ) );
        }
        catch ( Exception e )
        {
            LOG.error( "Invalid base DN {}", baseDn );
            LOG.error( e.getMessage(), e );
            searchRequest = null;
            return;
        }

        try
        {
            ExprNode filterNode = FilterParser.parse( config.getFilter() );
            searchRequest.setFilter( TwixTransformer.transformFilter( filterNode ) );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to parse the filter expression {}", config.getFilter() );
            LOG.error( e.getMessage(), e );
            searchRequest = null;
            return;
        }

        searchRequest.setSizeLimit( config.getSearchSizeLimit() );
        searchRequest.setTimeLimit( config.getSearchTimeout() );
        // the only valid values are NEVER_DEREF_ALIASES and DEREF_FINDING_BASE_OBJ
        searchRequest.setDerefAliases( LdapConstants.NEVER_DEREF_ALIASES );
        searchRequest.setScope( SearchScope.getSearchScope( config.getSearchScope() ) );
        searchRequest.setTypesOnly( false );

        String attributes = config.getAttributes();
        if ( attributes == null || attributes.trim().length() == 0 )
        {
            searchRequest.addAttribute( SchemaConstants.ALL_USER_ATTRIBUTES );
        }
        else
        {
            String[] attrs = attributes.trim().split( "," );
            for ( String s : attrs )
            {
                s = s.trim();
                if ( s.length() > 0 )
                {
                    searchRequest.addAttribute( s );
                }
            }
        }

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

        Control control = new Control();
        control.setControlType( SyncRequestValueControl.CONTROL_OID );
        control.setCriticality( syncReq.isCritical() );
        control.setControlValue( syncReq.getEncodedValue() );

        searchRequest.addControl( control );
    }


    public void handleSearchDone( SearchResultDone searchDone )
    {
        LOG.debug( "///////////////// handleSearchDone //////////////////" );
        Control ctrl = searchDone.getCurrentControl();
        SyncDoneValueControlCodec syncDoneCtrl = ( SyncDoneValueControlCodec ) ctrl.getControlValue();

        if ( syncDoneCtrl.getCookie() != null )
        {
            syncCookie = syncDoneCtrl.getCookie();
            LOG.debug( "assigning cookie from sync done value control: " + StringTools.utf8ToString( syncCookie ) );
        }
        else
        {
            LOG.info( "cookie in syncdone message is null" );
        }

        if ( !config.isRefreshPersist() )
        {
            // Now, switch to refreshAndPresist
            config.setRefreshPersist( true );
            LOG.debug( "Swithing to RefreshAndPersist" );
         
            try
            {
                // the below call is required to send the updated cookie
                // and refresh mode change (i.e to refreshAndPersist stage)
                // cause while the startSync() method sleeps even after the 'sync done'
                // message arrives as part of initial searchRequest with 'refreshOnly' mode.
                // During this sleep time any 'modifications' happened on the server 
                // to already fetched entries will be sent as SearchResultEntries with
                // SyncState value as 'ADD' which will conflict with the DNs of initially received entries
                
                // TODO thinking of alternative ways to implement this in case of large DITs 
                // where the modification to entry(ies) can happen before the initial sync is done
                doSyncSearch();
            }
            catch( Exception e )
            {
                LOG.error( "Failed to send a search request with RefreshAndPersist mode", e );
            }
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

            Control ctrl = syncResult.getCurrentControl();
            SyncStateValueControlCodec syncStateCtrl = ( SyncStateValueControlCodec ) ctrl.getControlValue();

            if ( syncStateCtrl.getCookie() != null )
            {
                syncCookie = syncStateCtrl.getCookie();
                LOG.debug( "assigning the cookie from sync state value control: "
                    + StringTools.utf8ToString( syncCookie ) );
            }

            SyncStateTypeEnum state = syncStateCtrl.getSyncStateType();

            LOG.debug( "state name {}", state.name() );
            LOG.debug( "entryUUID = {}", UUID.nameUUIDFromBytes( syncStateCtrl.getEntryUUID() ) );

            switch ( state )
            {
            	case ADD :
	                if ( !session.exists( remoteEntry.getDn() ) )
	                {
	                    LOG.debug( "adding entry with dn {}", remoteEntry.getDn().getUpName() );
	                    LOG.debug( remoteEntry.toString() );
	                    session.add( new DefaultServerEntry( directoryService.getRegistries(), remoteEntry ) );
	                }

	                break;
            
            	case MODIFY :
	                modify( remoteEntry );
	                break;

            	case DELETE :
	                LOG.debug( "deleting entry with dn {}", remoteEntry.getDn().getUpName() );
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
     * atm does nothinng except examinig and printing the content of syncinfovalue control
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
            if( uuidList != null )
            {
                for( byte[] uuid : uuidList )
                {
                    LOG.info( "uuid: {}", StringTools.utf8ToString( uuid ) );
                }
            }

            // if refreshDeletes set to true then delete all the entries with entryUUID
            // present in the syncIdSet 
            if( syncInfoValue.isRefreshDeletes() && ( uuidList != null ) )
            {
                for( byte[] uuid : uuidList )
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
        if( disconnected )
        {
            return;
        }
        
        boolean connected = false;
        
        while( !connected )
        {
            try
            {
                Thread.sleep( config.getConsumerInterval() );
            }
            catch( InterruptedException e )
            {
                LOG.error( "Interrupted while sleeping before trying to reconnect", e );
            }

            LOG.debug( "Trying to reconnect" );
            connected = connect();
        }
        
        bind();
        startSync();
    }


    /**
     * starts the syn operation
     */
    public void startSync()
    {
        // read the cookie if persisted
        readCookie();
        
        if( config.isRefreshPersist() )
        {
            try
            {
                LOG.debug( "==================== Refresh And Persist ==========" );
                doSyncSearch();    
            }
            catch( Exception e )
            {
                LOG.error( "Failed to sync with refreshAndPersist mode", e );
            }
            
            return;
        }
        
        // continue till refreshAndPersist mode is not set
        while( !config.isRefreshPersist() )
        {
            
            LOG.debug( "==================== Refresh Only ==========" );
            
            try
            {
                if ( ( syncReq.getCookie() == null ) || ( syncReq.getCookie().length == 0 ) )
                {
                    LOG.debug( "First search (no cookie)" );
                }
                else
                {
                    LOG.debug( "searching with searchRequest, cookie '{}'", StringTools.utf8ToString( syncReq.getCookie() ) );
                }
                
                doSyncSearch();

                LOG.info( "--------------------- Sleep for a little while ------------------" );
                Thread.sleep( config.getConsumerInterval() );
                LOG.debug( "--------------------- syncing again ------------------" );

            }
            catch ( Exception e )
            {
                LOG.error( "Failed to sync with refresh only mode", e );
            }
        }
        
        LOG.debug( "**************** Done with the initial sync ***************" );
    }

    
    /**
     * 
     * performs a search on connection with updated syncrequest control.
     *
     * @throws Exception in case of any problems encountered while searching
     */
    private void doSyncSearch() throws Exception
    {
        if( config.isRefreshPersist() )
        {
            syncReq.setMode( SynchronizationModeEnum.REFRESH_AND_PERSIST );
        }

        if ( syncCookie != null )
        {
            syncReq.setCookie( syncCookie );
        }
        
        searchRequest.getCurrentControl().setControlValue( syncReq.getEncodedValue() );

        connection.search( searchRequest );
    }

    
    public void disconnet()
    {
        disconnected = true;

        try
        {
            connection.unBind();
            LOG.info( "Unbound from the server {}", config.getProviderHost() );

            connection.close();
            LOG.info( "Connection closed for the server {}", config.getProviderHost() );

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
        if( syncCookie == null )
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
        catch( Exception e )
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
            if( cookieFile.exists() && cookieFile.length() > 0 )
            {
                FileInputStream fin = new FileInputStream( cookieFile );
                syncCookie = new byte[ fin.read() ];
                fin.read( syncCookie );
                fin.close();
                LOG.debug( "read the cookie from file: " + StringTools.utf8ToString( syncCookie ) );
            }
        }
        catch( Exception e )
        {
            LOG.error( "Failed to read the cookie", e );
        }
    }

    
    /**
     * deletes the cookie file(if exists) 
     */
    public void deleteCookieFile()
    {
        if( cookieFile != null && cookieFile.exists() )
        {
            LOG.debug( "deleting the cookie file" );
            cookieFile.delete();
        }
    }
    
    
    private void modify( Entry remoteEntry ) throws Exception 
    {
        LOG.debug( "modifying entry with dn {}", remoteEntry.getDn().getUpName() );
        
        Entry localEntry = session.lookup( remoteEntry.getDn() );
        
        List<Modification> mods = new ArrayList<Modification>();
        Iterator<EntryAttribute> itr = localEntry.iterator();
     
        while ( itr.hasNext() )
        {
            EntryAttribute localAttr = itr.next();
            String attrId = localAttr.getId();
            Modification mod;
            EntryAttribute remoteAttr = remoteEntry.get( attrId );
            
            if (  remoteAttr != null ) // would be better if we compare the values also? or will it consume more time?
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

        if( remoteEntry.size() > 0 )
        {
            itr = remoteEntry.iterator();
            while( itr.hasNext() )
            {
                mods.add( new ServerModification( ModificationOperation.ADD_ATTRIBUTE, itr.next() ) );
            }
        }
        
        session.modify( remoteEntry.getDn(), mods );
    }
}
