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
package org.apache.directory.server.ldap;


import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.security.CoreKeyStoreSpi;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.handlers.AbandonHandler;
import org.apache.directory.server.ldap.handlers.AddHandler;
import org.apache.directory.server.ldap.handlers.BindHandler;
import org.apache.directory.server.ldap.handlers.CompareHandler;
import org.apache.directory.server.ldap.handlers.DeleteHandler;
import org.apache.directory.server.ldap.handlers.ExtendedHandler;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;
import org.apache.directory.server.ldap.handlers.ModifyDnHandler;
import org.apache.directory.server.ldap.handlers.ModifyHandler;
import org.apache.directory.server.ldap.handlers.SearchHandler;
import org.apache.directory.server.ldap.handlers.UnbindHandler;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.ssl.LdapsInitializer;
import org.apache.directory.server.ldap.replication.ReplicationProvider;
import org.apache.directory.server.ldap.replication.SyncReplConsumer;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.ISyncDoneValue;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.ISyncRequestValue;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.ISyncStateValue;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.model.constants.SaslQoP;
import org.apache.directory.shared.ldap.model.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.model.message.AbandonRequest;
import org.apache.directory.shared.ldap.model.message.AddRequest;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.CompareRequest;
import org.apache.directory.shared.ldap.model.message.DeleteRequest;
import org.apache.directory.shared.ldap.model.message.ExtendedRequest;
import org.apache.directory.shared.ldap.model.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.UnbindRequest;
import org.apache.directory.shared.ldap.model.message.controls.Cascade;
import org.apache.directory.shared.ldap.model.message.controls.EntryChange;
import org.apache.directory.shared.ldap.model.message.controls.ManageDsaIT;
import org.apache.directory.shared.ldap.model.message.controls.PagedResults;
import org.apache.directory.shared.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.shared.ldap.model.message.controls.Subentries;
import org.apache.directory.shared.ldap.model.message.controls.SyncInfoValue;
import org.apache.directory.shared.util.Strings;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.UnorderedThreadPoolExecutor;
import org.apache.mina.handler.demux.MessageHandler;
import org.apache.mina.transport.socket.AbstractSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 3757127143811666817L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapServer.class.getName() );

    /** Value (0) for configuration where size limit is unlimited. */
    public static final long NO_SIZE_LIMIT = 0;

    /** Value (0) for configuration where time limit is unlimited. */
    public static final int NO_TIME_LIMIT = 0;

    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";

    /** The default maximum size limit. */
    private static final long MAX_SIZE_LIMIT_DEFAULT = 100;

    /** The default maximum time limit. */
    private static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ldap";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS LDAP Service";

    /** the session manager for this LdapServer */
    private LdapSessionManager ldapSessionManager = new LdapSessionManager();

    /** a set of supported controls */
    private Set<String> supportedControls;

    /** 
     * The maximum size limit. 
     * @see {@link LdapServer#MAX_SIZE_LIMIT_DEFAULT }
     */
    private long maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT;

    /** 
     * The maximum time limit.
     * @see {@link LdapServer#MAX_TIME_LIMIT_DEFAULT }
     */
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT;

    /** If LDAPS is activated : the external Keystore file, if defined */
    private String keystoreFile;

    /** If LDAPS is activated : the certificate password */
    private String certificatePassword;

    /** The extended operation handlers. */
    private final Collection<ExtendedOperationHandler> extendedOperationHandlers = new ArrayList<ExtendedOperationHandler>();

    /** The supported authentication mechanisms. */
    private Map<String, MechanismHandler> saslMechanismHandlers = new HashMap<String, MechanismHandler>();

    /** The name of this host, validated during SASL negotiation. */
    private String saslHost = "ldap.example.com";

    /** The service principal, used by GSSAPI. */
    private String saslPrincipal = "ldap/ldap.example.com@EXAMPLE.COM";

    /** The quality of protection (QoP), used by DIGEST-MD5 and GSSAPI. */
    private Set<String> saslQop;
    private String saslQopString;

    /** The list of realms serviced by this host. */
    private List<String> saslRealms;

    /** The protocol handlers */
    private LdapRequestHandler<AbandonRequest> abandonHandler;
    private LdapRequestHandler<AddRequest> addHandler;
    private LdapRequestHandler<BindRequest> bindHandler;
    private LdapRequestHandler<CompareRequest> compareHandler;
    private LdapRequestHandler<DeleteRequest> deleteHandler;
    private LdapRequestHandler<ExtendedRequest> extendedHandler;
    private LdapRequestHandler<ModifyRequest> modifyHandler;
    private LdapRequestHandler<ModifyDnRequest> modifyDnHandler;
    private LdapRequestHandler<SearchRequest> searchHandler;
    private LdapRequestHandler<UnbindRequest> unbindHandler;

    /** the underlying provider codec factory */
    private ProtocolCodecFactory codecFactory;

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler( this );

    /** tracks start state of the server */
    private boolean started;

    /** 
     * Whether or not confidentiality (TLS secured connection) is required: 
     * disabled by default. 
     */
    private boolean confidentialityRequired;

    private KeyStore keyStore = null;

    private List<IoFilterChainBuilder> chainBuilders = new ArrayList<IoFilterChainBuilder>();

    private ReplicationProvider replicationProvider;

    private List<SyncreplConfiguration> providerConfigs;

    private List<SyncReplConsumer> replConsumers;


    /**
     * Creates an LDAP protocol provider.
     */
    public LdapServer()
    {
        super.setEnabled( true );
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );

        saslQop = new HashSet<String>();
        saslQop.add( SaslQoP.QOP_AUTH );
        saslQop.add( SaslQoP.QOP_AUTH_INT );
        saslQop.add( SaslQoP.QOP_AUTH_CONF );
        saslQopString = SaslQoP.QOP_AUTH + ',' + SaslQoP.QOP_AUTH_INT + ',' + SaslQoP.QOP_AUTH_CONF;

        saslRealms = new ArrayList<String>();
        saslRealms.add( "example.com" );

        this.supportedControls = new HashSet<String>();
        this.supportedControls.add( PersistentSearch.OID );
        this.supportedControls.add( EntryChange.OID );
        this.supportedControls.add( Subentries.OID );
        this.supportedControls.add( ManageDsaIT.OID );
        this.supportedControls.add( Cascade.OID );
        this.supportedControls.add( PagedResults.OID );
        // Replication controls
        this.supportedControls.add( ISyncDoneValue.OID );
        this.supportedControls.add( SyncInfoValue.OID );
        this.supportedControls.add( ISyncRequestValue.OID );
        this.supportedControls.add( ISyncStateValue.OID );
    }


    /**
     * Install the LDAP request handlers.
     */
    private void installDefaultHandlers()
    {
        if ( getAbandonHandler() == null )
        {
            setAbandonHandler( new AbandonHandler() );
        }

        if ( getAddHandler() == null )
        {
            setAddHandler( new AddHandler() );
        }

        if ( getBindHandler() == null )
        {
            BindHandler handler = new BindHandler();
            handler.setSaslMechanismHandlers( saslMechanismHandlers );
            setBindHandler( handler );
        }

        if ( getCompareHandler() == null )
        {
            setCompareHandler( new CompareHandler() );
        }

        if ( getDeleteHandler() == null )
        {
            setDeleteHandler( new DeleteHandler() );
        }

        if ( getExtendedHandler() == null )
        {
            setExtendedHandler( new ExtendedHandler() );
        }

        if ( getModifyHandler() == null )
        {
            setModifyHandler( new ModifyHandler() );
        }

        if ( getModifyDnHandler() == null )
        {
            setModifyDnHandler( new ModifyDnHandler() );
        }

        if ( getSearchHandler() == null )
        {
            setSearchHandler( new SearchHandler() );
        }

        if ( getUnbindHandler() == null )
        {
            setUnbindHandler( new UnbindHandler() );
        }
    }

    private class AdsKeyStore extends KeyStore
    {
        public AdsKeyStore( KeyStoreSpi keyStoreSpi, Provider provider, String type )
        {
            super( keyStoreSpi, provider, type );
        }
    }


    /**
     * loads the digital certificate either from a keystore file or from the admin entry in DIT
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void loadKeyStore() throws Exception
    {
        if ( Strings.isEmpty(keystoreFile) )
        {
            Provider provider = Security.getProvider( "SUN" );
            LOG.debug( "provider = {}", provider );
            CoreKeyStoreSpi coreKeyStoreSpi = new CoreKeyStoreSpi( getDirectoryService() );
            keyStore = new KeyStore( coreKeyStoreSpi, provider, "JKS" )
            {
            };

            try
            {
                keyStore.load( null, null );
            }
            catch ( Exception e )
            {
                // nothing really happens with this keystore
            }
        }
        else
        {
            keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream( keystoreFile );
                keyStore.load( fis, null );
            }
            finally
            {
                if ( fis != null )
                {
                    fis.close();
                }
            }
        }
    }


    /**
     * reloads the SSL context by replacing the existing SslFilter
     * with a new SslFilter after reloading the keystore.
     * 
     * Note: should be called to reload the keystore after changing the digital certificate.
     */
    public void reloadSslContext() throws Exception
    {
        if ( !started )
        {
            return;
        }

        LOG.info( "reloading SSL context..." );

        loadKeyStore();

        String sslFilterName = "sslFilter";
        for ( IoFilterChainBuilder chainBuilder : chainBuilders )
        {
            DefaultIoFilterChainBuilder dfcb = ( ( DefaultIoFilterChainBuilder ) chainBuilder );
            if ( dfcb.contains( sslFilterName ) )
            {
                DefaultIoFilterChainBuilder newChain = ( DefaultIoFilterChainBuilder ) LdapsInitializer.init( keyStore,
                    certificatePassword );
                dfcb.replace( sslFilterName, newChain.get( sslFilterName ) );
                newChain = null;
            }
        }

        StartTlsHandler handler = ( StartTlsHandler ) getExtendedOperationHandler( StartTlsHandler.EXTENSION_OID );
        if ( handler != null )
        {
            //FIXME dirty hack. IMO StartTlsHandler's code requires a cleanup
            // cause the keystore loading and sslcontext creation code is duplicated
            // both in the LdapService as well as StatTlsHandler
            handler.setLdapServer( this );
        }

        LOG.info( "reloaded SSL context successfully" );
    }


    /**
     * @throws IOException if we cannot bind to the specified port
     * @throws Exception if the LDAP server cannot be started
     */
    public void start() throws Exception
    {
        if ( !isEnabled() )
        {
            return;
        }

        for ( Transport transport : transports )
        {
            if ( !( transport instanceof TcpTransport ) )
            {
                LOG.warn( "Cannot listen on an UDP transport : {}", transport );
                continue;
            }

            IoFilterChainBuilder chain;

            if ( transport.isSSLEnabled() )
            {
                loadKeyStore();
                chain = LdapsInitializer.init( keyStore, certificatePassword );
            }
            else
            {
                chain = new DefaultIoFilterChainBuilder();
            }

            // Inject the codec into the chain
            ( ( DefaultIoFilterChainBuilder ) chain ).addLast( "codec", new ProtocolCodecFilter( this
                .getProtocolCodecFactory() ) );

            // Now inject an ExecutorFilter for the write operations
            // We use the same number of thread than the number of IoProcessor
            // (NOTE : this has to be double checked)
            ( ( DefaultIoFilterChainBuilder ) chain ).addLast( "executor", new ExecutorFilter(
                new UnorderedThreadPoolExecutor( transport.getNbThreads() ), IoEventType.MESSAGE_RECEIVED ) );

			/*
            // Trace all the incoming and outgoing message to the console
            ( ( DefaultIoFilterChainBuilder ) chain ).addLast( "logger", new IoFilterAdapter()
                {
                    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception 
                    {
                        System.out.println( ">>> Message received : " + message );
                        nextFilter.messageReceived(session, message);
                    }

                    public void filterWrite(NextFilter nextFilter, IoSession session,
                            WriteRequest writeRequest) throws Exception 
                    {
                        System.out.println( "<<< Message sent : " + writeRequest.getMessage() );
                        nextFilter.filterWrite(session, writeRequest);
                    }
                });
            */

            startNetwork( transport, chain );
        }
        
        /*
         * The server is now initialized, we can
         * install the default requests handlers, which need 
         * access to the DirectoryServer instance.
         */
        installDefaultHandlers();

        PartitionNexus nexus = getDirectoryService().getPartitionNexus();

        for ( ExtendedOperationHandler h : extendedOperationHandlers )
        {
            LOG.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapServer( this );
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        nexus.registerSupportedSaslMechanisms( saslMechanismHandlers.keySet() );

        if ( replicationProvider != null )
        {
            replicationProvider.init( this );
            ( ( SearchHandler ) getSearchHandler() ).setReplicationProvider( replicationProvider );
        }

        startConsumers();

        started = true;

        LOG.info( "Ldap service started." );
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        try
        {
            for ( Transport transport : transports )
            {
                if ( !( transport instanceof TcpTransport ) )
                {
                    continue;
                }

                // we should unbind the service before we begin sending the notice
                // of disconnect so new connections are not formed while we process
                List<WriteFuture> writeFutures = new ArrayList<WriteFuture>();

                // If the socket has already been unbound as with a successful
                // GracefulShutdownRequest then this will complain that the service
                // is not bound - this is ok because the GracefulShutdown has already
                // sent notices to to the existing active sessions
                List<IoSession> sessions;

                try
                {
                    sessions = new ArrayList<IoSession>( getSocketAcceptor( transport ).getManagedSessions().values() );
                }
                catch ( IllegalArgumentException e )
                {
                    LOG.warn( "Seems like the LDAP service (" + getPort() + ") has already been unbound." );
                    return;
                }

                getSocketAcceptor( transport ).dispose();

                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Unbind of an LDAP service (" + getPort() + ") is complete." );
                    LOG.info( "Sending notice of disconnect to existing clients sessions." );
                }

                // Send Notification of Disconnection messages to all connected clients.
                if ( sessions != null )
                {
                    for ( IoSession session : sessions )
                    {
                        writeFutures.add( session.write( NoticeOfDisconnect.UNAVAILABLE ) );
                    }
                }

                // And close the connections when the NoDs are sent.
                Iterator<IoSession> sessionIt = sessions.iterator();

                for ( WriteFuture future : writeFutures )
                {
                    future.await( 1000L );
                    sessionIt.next().close( true );
                }

                if ( replicationProvider != null )
                {
                    replicationProvider.stop();
                }
            }

            stopConsumers();
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to sent NoD.", e );
        }

        started = false;
        LOG.info( "Ldap service stopped." );
    }


    private void startNetwork( Transport transport, IoFilterChainBuilder chainBuilder ) throws Exception
    {
        if ( transport.getBackLog() < 0 )
        {
            // Set the backlog to the default value when it's below 0
            transport.setBackLog( 50 );
        }

        chainBuilders.add( chainBuilder );

        try
        {
            SocketAcceptor acceptor = getSocketAcceptor( transport );

            // Now, configure the acceptor
            // Disable the disconnection of the clients on unbind
            acceptor.setCloseOnDeactivation( false );

            // No Nagle's algorithm
            acceptor.getSessionConfig().setTcpNoDelay( true );

            // Inject the chain
            acceptor.setFilterChainBuilder( chainBuilder );

            // Inject the protocol handler
            acceptor.setHandler( getHandler() );

            ( ( AbstractSocketSessionConfig ) acceptor.getSessionConfig() ).setReadBufferSize( 64 * 1024 );
            ( ( AbstractSocketSessionConfig ) acceptor.getSessionConfig() ).setSendBufferSize( 64 * 1024 );

            // Bind to the configured address
            acceptor.bind();

            // We are done !
            started = true;

            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Successful bind of an LDAP Service (" + transport.getPort() + ") is completed." );
            }
        }
        catch ( IOException e )
        {
            String msg = I18n.err( I18n.ERR_171, transport.getPort() );
            LdapConfigurationException lce = new LdapConfigurationException( msg );
            lce.setCause( e );
            LOG.error( msg, e );
            throw lce;
        }
    }


    /**
     * starts the replication consumers
     */
    private void startConsumers() throws Exception
    {
        if ( providerConfigs != null )
        {
            replConsumers = new ArrayList<SyncReplConsumer>( providerConfigs.size() );

            for ( final SyncreplConfiguration config : providerConfigs )
            {
                Runnable consumerTask = new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            SyncReplConsumer consumer = new SyncReplConsumer();
                            LOG.info( "starting the replication consumer with config {}", config );
                            consumer.init( getDirectoryService(), config );
                            consumer.connect();
                            replConsumers.add( consumer );
                            consumer.startSync();
                        }
                        catch ( Exception e )
                        {
                            LOG.error( "Failed to start the consumer with config {}", config );
                            throw new RuntimeException( e );
                        }
                    }
                };

                Thread consumerThread = new Thread( consumerTask );
                consumerThread.setDaemon( true );
                consumerThread.start();
            }
        }
    }


    /**
     * stops the replication consumers
     */
    private void stopConsumers()
    {
        if ( replConsumers != null )
        {
            for ( SyncReplConsumer consumer : replConsumers )
            {
                LOG.info( "stopping the consumer with id {}", consumer.getConfig().getReplicaId() );
                consumer.disconnet();
            }
        }
    }


    public String getName()
    {
        return SERVICE_NAME;
    }


    public IoHandler getHandler()
    {
        return handler;
    }


    public LdapSessionManager getLdapSessionManager()
    {
        return ldapSessionManager;
    }


    public ProtocolCodecFactory getProtocolCodecFactory()
    {
        return codecFactory;
    }


    // ------------------------------------------------------------------------
    // Configuration Methods
    // ------------------------------------------------------------------------

    /**
     * Registers the specified {@link ExtendedOperationHandler} to this
     * protocol provider to provide a specific LDAP extended operation.
     *
     * @param eoh an extended operation handler
     * @throws Exception on failure to add the handler
     */
    public void addExtendedOperationHandler( ExtendedOperationHandler eoh ) throws Exception
    {
        if ( started )
        {
            eoh.setLdapServer( this );
            PartitionNexus nexus = getDirectoryService().getPartitionNexus();
            nexus.registerSupportedExtensions( eoh.getExtensionOids() );
        }
        else
        {
            extendedOperationHandlers.add( eoh );
        }
    }


    /**
     * Deregisteres an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * from this protocol provider.
     *
     * @param oid the numeric identifier for the extended operation associated with
     * the handler to remove
     */
    public void removeExtendedOperationHandler( String oid )
    {
        // need to do something like this to make this work right
        //            DefaultPartitionNexus nexus = getDirectoryService().getPartitionNexus();
        //            nexus.unregisterSupportedExtensions( eoh.getExtensionOids() );

        ExtendedOperationHandler handler = null;
        for ( ExtendedOperationHandler h : extendedOperationHandlers )
        {
            if ( h.getOid().equals( oid ) )
            {
                handler = h;
                break;
            }
        }
        extendedOperationHandlers.remove( handler );
    }


    /**
     * Returns an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
     * which is registered to this protocol provider.
     *
     * @param oid the oid of the extended request of associated with the extended
     * request handler
     * @return the exnteded operation handler
     */
    public ExtendedOperationHandler getExtendedOperationHandler( String oid )
    {
        for ( ExtendedOperationHandler h : extendedOperationHandlers )
        {
            if ( h.getOid().equals( oid ) )
            {
                return h;
            }
        }

        return null;
    }


    /**
     * Sets the mode for this LdapServer to accept requests with or without a
     * TLS secured connection via either StartTLS extended operations or using
     * LDAPS.
     * 
     * @param confidentialityRequired true to require confidentiality
     */
    public void setConfidentialityRequired( boolean confidentialityRequired )
    {
        this.confidentialityRequired = confidentialityRequired;
    }


    /**
     * Gets whether or not TLS secured connections are required to perform 
     * operations on this LdapServer.
     * 
     * @return true if TLS secured connections are required, false otherwise
     */
    public boolean isConfidentialityRequired()
    {
        return confidentialityRequired;
    }


    /**
     * Returns <tt>true</tt> if LDAPS is enabled.
     *
     * @return True if LDAPS is enabled.
     */
    public boolean isEnableLdaps( Transport transport )
    {
        return transport.isSSLEnabled();
    }


    /**
     * Sets the maximum size limit in number of entries to return for search.
     *
     * @param maxSizeLimit the maximum number of entries to return for search
     */
    public void setMaxSizeLimit( long maxSizeLimit )
    {
        this.maxSizeLimit = maxSizeLimit;
    }


    /**
     * Returns the maximum size limit in number of entries to return for search.
     *
     * @return The maximum size limit.
     */
    public long getMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    /**
     * Sets the maximum time limit in milliseconds to conduct a search.
     *
     * @param maxTimeLimit the maximum length of time in milliseconds for search
     */
    public void setMaxTimeLimit( int maxTimeLimit )
    {
        this.maxTimeLimit = maxTimeLimit;
    }


    /**
     * Returns the maximum time limit in milliseconds to conduct a search.
     *
     * @return The maximum time limit in milliseconds for search
     */
    public int getMaxTimeLimit()
    {
        return maxTimeLimit;
    }


    /**
     * Gets the {@link ExtendedOperationHandler}s.
     *
     * @return A collection of {@link ExtendedOperationHandler}s.
     */
    public Collection<ExtendedOperationHandler> getExtendedOperationHandlers()
    {
        return new ArrayList<ExtendedOperationHandler>( extendedOperationHandlers );
    }


    /**
     * Sets the {@link ExtendedOperationHandler}s.
     *
     * @param handlers A collection of {@link ExtendedOperationHandler}s.
     */
    public void setExtendedOperationHandlers( Collection<ExtendedOperationHandler> handlers )
    {
        this.extendedOperationHandlers.clear();
        this.extendedOperationHandlers.addAll( handlers );
    }


    /**
     * Returns the FQDN of this SASL host, validated during SASL negotiation.
     *
     * @return The FQDN of this SASL host, validated during SASL negotiation.
     */
    public String getSaslHost()
    {
        return saslHost;
    }


    /**
     * Sets the FQDN of this SASL host, validated during SASL negotiation.
     *
     * @param saslHost The FQDN of this SASL host, validated during SASL negotiation.
     */
    public void setSaslHost( String saslHost )
    {
        this.saslHost = saslHost;
    }


    /**
     * Returns the Kerberos principal name for this LDAP service, used by GSSAPI.
     *
     * @return The Kerberos principal name for this LDAP service, used by GSSAPI.
     */
    public String getSaslPrincipal()
    {
        return saslPrincipal;
    }


    /**
     * Sets the Kerberos principal name for this LDAP service, used by GSSAPI.
     *
     * @param saslPrincipal The Kerberos principal name for this LDAP service, used by GSSAPI.
     */
    public void setSaslPrincipal( String saslPrincipal )
    {
        this.saslPrincipal = saslPrincipal;
    }


    /**
     * Returns the quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     *
     * @return The quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public String getSaslQopString()
    {
        return saslQopString;
    }


    /**
     * Returns the Set of quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     *
     * @return The quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public Set<String> getSaslQop()
    {
        return saslQop;
    }


    /**
     * Returns the realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     *
     * @return The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public List<String> getSaslRealms()
    {
        return saslRealms;
    }


    /**
     * Sets the realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     *
    * @param saslRealms The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslRealms( List<String> saslRealms )
    {
        this.saslRealms = saslRealms;
    }


    /**
     */
    public Map<String, MechanismHandler> getSaslMechanismHandlers()
    {
        return saslMechanismHandlers;
    }


    public void setSaslMechanismHandlers( Map<String, MechanismHandler> saslMechanismHandlers )
    {
        this.saslMechanismHandlers = saslMechanismHandlers;
    }


    public MechanismHandler addSaslMechanismHandler( String mechanism, MechanismHandler handler )
    {
        return this.saslMechanismHandlers.put( mechanism, handler );
    }


    public MechanismHandler removeSaslMechanismHandler( String mechanism )
    {
        return this.saslMechanismHandlers.remove( mechanism );
    }


    public MechanismHandler getMechanismHandler( String mechanism )
    {
        return this.saslMechanismHandlers.get( mechanism );
    }


    public Set<String> getSupportedMechanisms()
    {
        return saslMechanismHandlers.keySet();
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        super.setDirectoryService( directoryService );
        this.codecFactory = new LdapProtocolCodecFactory( directoryService );
    }


    public Set<String> getSupportedControls()
    {
        return supportedControls;
    }


    /**
     */
    public void setSupportedControls( Set<String> supportedControls )
    {
        this.supportedControls = supportedControls;
    }


    public MessageHandler<AbandonRequest> getAbandonHandler()
    {
        return abandonHandler;
    }


    /**
     * @param abandonHandler The AbandonRequest handler
     */
    public void setAbandonHandler( LdapRequestHandler<AbandonRequest> abandonHandler )
    {
        this.handler.removeReceivedMessageHandler( AbandonRequest.class );
        this.abandonHandler = abandonHandler;
        this.abandonHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( AbandonRequest.class, this.abandonHandler );
    }


    public LdapRequestHandler<AddRequest> getAddHandler()
    {
        return addHandler;
    }


    /**
     * @param addHandler The AddRequest handler
     */
    public void setAddHandler( LdapRequestHandler<AddRequest> addHandler )
    {
        this.handler.removeReceivedMessageHandler( AddRequest.class );
        this.addHandler = addHandler;
        this.addHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( AddRequest.class, this.addHandler );
    }


    public LdapRequestHandler<BindRequest> getBindHandler()
    {
        return bindHandler;
    }


    /**
     * @param bindHandler The BindRequest handler
     */
    public void setBindHandler( LdapRequestHandler<BindRequest> bindHandler )
    {
        this.bindHandler = bindHandler;
        this.bindHandler.setLdapServer( this );

        handler.removeReceivedMessageHandler( BindRequest.class );
        handler.addReceivedMessageHandler( BindRequest.class, this.bindHandler );
    }


    public LdapRequestHandler<CompareRequest> getCompareHandler()
    {
        return compareHandler;
    }


    /**
     * @param compareHandler The CompareRequest handler
     */
    public void setCompareHandler( LdapRequestHandler<CompareRequest> compareHandler )
    {
        this.handler.removeReceivedMessageHandler( CompareRequest.class );
        this.compareHandler = compareHandler;
        this.compareHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( CompareRequest.class, this.compareHandler );
    }


    public LdapRequestHandler<DeleteRequest> getDeleteHandler()
    {
        return deleteHandler;
    }


    /**
     * @param deleteHandler The DeleteRequest handler
     */
    public void setDeleteHandler( LdapRequestHandler<DeleteRequest> deleteHandler )
    {
        this.handler.removeReceivedMessageHandler( DeleteRequest.class );
        this.deleteHandler = deleteHandler;
        this.deleteHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( DeleteRequest.class, this.deleteHandler );
    }


    public LdapRequestHandler<ExtendedRequest> getExtendedHandler()
    {
        return extendedHandler;
    }


    /**
     * @param extendedHandler The ExtendedRequest handler
     */
    public void setExtendedHandler( LdapRequestHandler<ExtendedRequest> extendedHandler )
    {
        this.handler.removeReceivedMessageHandler( ExtendedRequest.class );
        this.extendedHandler = extendedHandler;
        this.extendedHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ExtendedRequest.class, this.extendedHandler );
    }


    public LdapRequestHandler<ModifyRequest> getModifyHandler()
    {
        return modifyHandler;
    }


    /**
     * @param modifyHandler The ModifyRequest handler
     */
    public void setModifyHandler( LdapRequestHandler<ModifyRequest> modifyHandler )
    {
        this.handler.removeReceivedMessageHandler( ModifyRequest.class );
        this.modifyHandler = modifyHandler;
        this.modifyHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ModifyRequest.class, this.modifyHandler );
    }


    public LdapRequestHandler<ModifyDnRequest> getModifyDnHandler()
    {
        return modifyDnHandler;
    }


    /**
     * @param modifyDnHandler The ModifyDNRequest handler
     */
    public void setModifyDnHandler( LdapRequestHandler<ModifyDnRequest> modifyDnHandler )
    {
        this.handler.removeReceivedMessageHandler( ModifyDnRequest.class );
        this.modifyDnHandler = modifyDnHandler;
        this.modifyDnHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ModifyDnRequest.class, this.modifyDnHandler );
    }


    public LdapRequestHandler<SearchRequest> getSearchHandler()
    {
        return searchHandler;
    }


    /**
     * @param searchHandler The SearchRequest handler
     */
    public void setSearchHandler( LdapRequestHandler<SearchRequest> searchHandler )
    {
        this.handler.removeReceivedMessageHandler( SearchRequest.class );
        this.searchHandler = searchHandler;
        this.searchHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( SearchRequest.class, this.searchHandler );
    }


    public LdapRequestHandler<UnbindRequest> getUnbindHandler()
    {
        return unbindHandler;
    }


    /**
     * @return The underlying TCP transport port, or -1 if no transport has been 
     * initialized
     */
    public int getPort()
    {
        if ( transports == null )
        {
            return -1;
        }

        for ( Transport transport : transports )
        {
            if ( transport instanceof UdpTransport )
            {
                continue;
            }

            if ( !transport.isSSLEnabled() )
            {
                return transport.getPort();
            }
        }

        return -1;
    }


    /**
     * @return The underlying SSL enabled TCP transport port, or -1 if no transport has been 
     * initialized
     */
    public int getPortSSL()
    {
        if ( transports == null )
        {
            return -1;
        }

        for ( Transport transport : transports )
        {
            if ( transport instanceof UdpTransport )
            {
                continue;
            }

            if ( transport.isSSLEnabled() )
            {
                return transport.getPort();
            }
        }

        return -1;
    }


    /**
     * @param unbindHandler The UnbindRequest handler
     */
    public void setUnbindHandler( LdapRequestHandler<UnbindRequest> unbindHandler )
    {
        this.handler.removeReceivedMessageHandler( UnbindRequest.class );
        this.unbindHandler = unbindHandler;
        this.unbindHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( UnbindRequest.class, this.unbindHandler );
    }


    public boolean isStarted()
    {
        return started;
    }


    /**
     */
    public void setStarted( boolean started )
    {
        this.started = started;
    }


    /**
     * @return The keystore path
     */
    public String getKeystoreFile()
    {
        return keystoreFile;
    }


    /**
     * Set the external keystore path
     * @param keystoreFile The external keystore path
     */
    public void setKeystoreFile( String keystoreFile )
    {
        this.keystoreFile = keystoreFile;
    }


    /**
     * @return The certificate passord
     */
    public String getCertificatePassword()
    {
        return certificatePassword;
    }


    /**
     * Set the certificate passord.
     * @param certificatePassword the certificate passord
     */
    public void setCertificatePassword( String certificatePassword )
    {
        this.certificatePassword = certificatePassword;
    }


    public void setReplicationProvider( ReplicationProvider replicationProvider )
    {
        this.replicationProvider = replicationProvider;
    }


    public void setReplProviderConfigs( List<SyncreplConfiguration> providerConfigs )
    {
        this.providerConfigs = providerConfigs;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "LdapServer[" ).append( getServiceName() ).append( "], listening on :" ).append( '\n' );

        if ( getTransports() != null )
        {
            for ( Transport transport : getTransports() )
            {
                sb.append( "    " ).append( transport ).append( '\n' );
            }
        }

        return sb.toString();
    }
}
