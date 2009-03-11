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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerModification;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.Control;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.codec.LdapResult;
import org.apache.directory.shared.ldap.codec.TwixTransformer;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlDecoder;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.codec.intermediate.IntermediateResponse;
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
import org.apache.mina.util.AvailablePortFinder;
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


    public void init( DirectoryService directoryservice )
    {
        this.directoryService = directoryservice;
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
                // FIXME rmove this at the time of integration with ADS
                //System.exit( 2 );
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
        //TODO openLdap cries if this flag is set 
        // searchRequest.setDerefAliases( LdapConstants.DEREF_ALWAYS );
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

        if ( !config.isRefreshPersist() )
        {
            config.setRefreshPersist( true );
        }

        if ( syncDoneCtrl.getCookie() != null )
        {
            syncCookie = syncDoneCtrl.getCookie();
            LOG.debug( "assigning cookie from sync done value control: " + StringTools.utf8ToString( syncCookie ) );
        }
        else
        {
            LOG.info( "cookie in syncdone message is null" );
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
            Entry clientEntry = syncResult.getEntry();

            Control ctrl = syncResult.getCurrentControl();
            SyncStateValueControlCodec syncStateCtrl = ( SyncStateValueControlCodec ) ctrl.getControlValue();

            if ( syncStateCtrl.getCookie() != null )
            {
                syncCookie = syncStateCtrl.getCookie();
                LOG.debug( "assigning the cookie from sync state value control: "
                    + StringTools.utf8ToString( syncCookie ) );
            }

            SyncStateTypeEnum state = syncStateCtrl.getSyncStateType();

            LOG.debug( "state name {}" + state.name() );
            LOG.debug( "entryUUID = " + StringTools.utf8ToString( syncStateCtrl.getEntryUUID() ) );
            CoreSession session = directoryService.getAdminSession();

            if ( state == SyncStateTypeEnum.ADD )
            {

                if ( !session.exists( clientEntry.getDn() ) )
                {
                    LOG.debug( "adding entry with dn {}", clientEntry.getDn().getUpName() );
                    LOG.debug( clientEntry.toString() );
                    session.add( new DefaultServerEntry( directoryService.getRegistries(), clientEntry ) );
                }
            }
            else if ( state == SyncStateTypeEnum.MODIFY )
            {
                // WARN FIXME inefficient delta calculation
                // FIXME won't work for deleted attributes
                LOG.debug( "modifying entry with dn {}", clientEntry.getDn().getUpName() );

                List<Modification> mods = new ArrayList<Modification>();
                Iterator<EntryAttribute> itr = clientEntry.iterator();
                while ( itr.hasNext() )
                {
                    Modification mod = new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, itr.next() );
                    mods.add( mod );
                }

                session.modify( clientEntry.getDn(), mods );
            }
            else if ( state == SyncStateTypeEnum.DELETE )
            {
                LOG.debug( "deleting entry with dn {}", clientEntry.getDn().getUpName() );
                directoryService.getAdminSession().delete( clientEntry.getDn() );
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
        }

        LOG.debug( "------------- starting handleSearchResult ------------" );
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

            LOG.info( "The uuid list " + uuidList );// receives a list of UUIDs of the entries what to do with them???
            LOG.info( "refreshDeletes: " + syncInfoValue.isRefreshDeletes() );
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
     * starts the syn operation
     * 
     * TODO should run in a separate thread
     * 
     */
    public void startSync()
    {
        if ( searchRequest == null )
        {
            return;
        }

        try
        {

            int pass = 1;

            do
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
                
                try
                {
                    LOG.debug( "======================================================== Pass #" + pass + "==========" );
                    pass++;
                    LOG.debug( "searching with searchRequest..." + StringTools.utf8ToString( syncReq.getCookie() ) );
                    connection.search( searchRequest );

                    LOG.info( "--------------------- Sleep for a little while ------------------" );

                    Thread.sleep( config.getConsumerInterval() );

                    LOG.debug( "--------------------- syncing again ------------------" );
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to sync", e );
                }
            }
            while( syncReq.getMode() != SynchronizationModeEnum.REFRESH_AND_PERSIST );// end of while loop
            
            LOG.debug( "**************** exiting the while loop ***************" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    public void disconnet()
    {
        try
        {
            connection.unBind();
            LOG.info( "Unbound from the server {}", config.getProviderHost() );

            connection.close();
            LOG.info( "Connection closed for the server {}", config.getProviderHost() );

            directoryService.shutdown();
            LOG.info( "stopped directory service" );
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to close the connection", e );
        }
    }


    private void startEmbeddedServer( File workDir )
    {
        try
        {
            directoryService = new DefaultDirectoryService();
            directoryService.setShutdownHookEnabled( false );
            directoryService.setWorkingDirectory( workDir );
            int consumerPort = AvailablePortFinder.getNextAvailable( 1024 );
            LdapService ldapService = new LdapService();
            ldapService.setTcpTransport( new TcpTransport( consumerPort ) );
            ldapService.setDirectoryService( directoryService );

            LdapDN suffix = new LdapDN( config.getBaseDn() );
            JdbmPartition partition = new JdbmPartition();
            partition.setSuffix( suffix.getUpName() );
            partition.setId( "syncrepl" );
            partition.setSyncOnWrite( true );
            partition.init( directoryService );

            directoryService.addPartition( partition );

            directoryService.startup();

            ldapService.addExtendedOperationHandler( new StartTlsHandler() );
            ldapService.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            ldapService.start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * The main starting point
     */
    public static void main( String[] args ) throws Exception
    {
        final SyncreplConsumer agent = new SyncreplConsumer();

        SyncreplConfiguration config = new SyncreplConfiguration();
        config.setProviderHost( "localhost" );
        config.setPort( 389 );
        config.setBindDn( "cn=admin,dc=nodomain" );
        config.setCredentials( "secret" );
        config.setBaseDn( "dc=test,dc=nodomain" );
        config.setFilter( "(objectclass=*)" );
        config.setSearchScope( SearchScope.SUBTREE.getJndiScope() );

        agent.setConfig( config );

        final File workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/work" );

        if ( workDir.exists() )
        {
            FileUtils.forceDelete( workDir );
        }
        else
        {
            workDir.mkdirs();
        }

        agent.startEmbeddedServer( workDir );

        agent.connect();
        agent.bind();
        agent.prepareSyncSearchRequest();
        agent.startSync();
    }
}
