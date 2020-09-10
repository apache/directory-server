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


import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.directory.api.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SaslQoP;
import org.apache.directory.api.ldap.model.exception.LdapConfigurationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AbandonRequest;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.CompareRequest;
import org.apache.directory.api.ldap.model.message.CompareResponse;
import org.apache.directory.api.ldap.model.message.DeleteRequest;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;
import org.apache.directory.api.ldap.model.message.IntermediateResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchResultReference;
import org.apache.directory.api.ldap.model.message.UnbindRequest;
import org.apache.directory.api.ldap.model.message.extended.NoticeOfDisconnect;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;
import org.apache.directory.server.ldap.handlers.LdapResponseHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.request.AbandonRequestHandler;
import org.apache.directory.server.ldap.handlers.request.AddRequestHandler;
import org.apache.directory.server.ldap.handlers.request.BindRequestHandler;
import org.apache.directory.server.ldap.handlers.request.CompareRequestHandler;
import org.apache.directory.server.ldap.handlers.request.DeleteRequestHandler;
import org.apache.directory.server.ldap.handlers.request.ExtendedRequestHandler;
import org.apache.directory.server.ldap.handlers.request.ModifyDnRequestHandler;
import org.apache.directory.server.ldap.handlers.request.ModifyRequestHandler;
import org.apache.directory.server.ldap.handlers.request.SearchRequestHandler;
import org.apache.directory.server.ldap.handlers.request.UnbindRequestHandler;
import org.apache.directory.server.ldap.handlers.response.AddResponseHandler;
import org.apache.directory.server.ldap.handlers.response.BindResponseHandler;
import org.apache.directory.server.ldap.handlers.response.CompareResponseHandler;
import org.apache.directory.server.ldap.handlers.response.DeleteResponseHandler;
import org.apache.directory.server.ldap.handlers.response.ExtendedResponseHandler;
import org.apache.directory.server.ldap.handlers.response.IntermediateResponseHandler;
import org.apache.directory.server.ldap.handlers.response.ModifyDnResponseHandler;
import org.apache.directory.server.ldap.handlers.response.ModifyResponseHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultDoneHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultEntryHandler;
import org.apache.directory.server.ldap.handlers.response.SearchResultReferenceHandler;
import org.apache.directory.server.ldap.handlers.sasl.MechanismHandler;
import org.apache.directory.server.ldap.handlers.ssl.LdapsInitializer;
import org.apache.directory.server.ldap.replication.consumer.PingerThread;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationStatusEnum;
import org.apache.directory.server.ldap.replication.provider.ReplicationRequestHandler;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.executor.UnorderedThreadPoolExecutor;
import org.apache.mina.handler.demux.MessageHandler;
import org.apache.mina.transport.socket.AbstractSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapServer extends DirectoryBackedService
{
    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapServer.class );

    /** Logger for the replication consumer */
    private static final Logger CONSUMER_LOG = LoggerFactory.getLogger( Loggers.CONSUMER_LOG.getName() );

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

    /** The maximum size for an incoming PDU */
    private int maxPDUSize = Integer.MAX_VALUE;

    /** If LDAPS is activated : the external Keystore file, if defined */
    private String keystoreFile;

    /** If LDAPS is activated : the certificate password */
    private String certificatePassword;

    /** The extended operation handlers. */
    private final Collection<ExtendedOperationHandler<? extends ExtendedRequest, ? extends ExtendedResponse>> extendedOperationHandlers =
        new ArrayList<>();

    /** The supported authentication mechanisms. */
    private Map<String, MechanismHandler> saslMechanismHandlers = new HashMap<>();

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
    // MessageReceived handlers
    private LdapRequestHandler<AbandonRequest> abandonRequestHandler;
    private LdapRequestHandler<AddRequest> addRequestHandler;
    private LdapRequestHandler<BindRequest> bindRequestHandler;
    private LdapRequestHandler<CompareRequest> compareRequestHandler;
    private LdapRequestHandler<DeleteRequest> deleteRequestHandler;
    private ExtendedRequestHandler extendedRequestHandler;
    private LdapRequestHandler<ModifyRequest> modifyRequestHandler;
    private LdapRequestHandler<ModifyDnRequest> modifyDnRequestHandler;
    private LdapRequestHandler<SearchRequest> searchRequestHandler;
    private LdapRequestHandler<UnbindRequest> unbindRequestHandler;

    // MessageSent handlers
    private LdapResponseHandler<AddResponse> addResponseHandler;
    private LdapResponseHandler<BindResponse> bindResponseHandler;
    private LdapResponseHandler<CompareResponse> compareResponseHandler;
    private LdapResponseHandler<DeleteResponse> deleteResponseHandler;
    private ExtendedResponseHandler extendedResponseHandler;
    private LdapResponseHandler<ModifyResponse> modifyResponseHandler;
    private LdapResponseHandler<IntermediateResponse> intermediateResponseHandler;
    private LdapResponseHandler<ModifyDnResponse> modifyDnResponseHandler;
    private LdapResponseHandler<SearchResultEntry> searchResultEntryHandler;
    private LdapResponseHandler<SearchResultReference> searchResultReferenceHandler;
    private LdapResponseHandler<SearchResultDone> searchResultDoneHandler;

    /** the underlying provider codec factory */
    private ProtocolCodecFactory codecFactory = LdapApiServiceFactory.getSingleton().getProtocolCodecFactory();

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler( this );

    /** tracks start state of the server */
    private boolean started;

    /**
     * Whether or not confidentiality (TLS secured connection) is required:
     * disabled by default.
     */
    private boolean confidentialityRequired;

    private List<IoFilterChainBuilder> chainBuilders = new ArrayList<>();

    /** The handler responsible for the replication */
    private ReplicationRequestHandler replicationReqHandler;

    /** The list of replication consumers */
    private List<ReplicationConsumer> replConsumers;

    private KeyManagerFactory keyManagerFactory;
    private TrustManager[] trustManagers;

    /** the time interval between subsequent pings to each replication provider */
    private int pingerSleepTime;

    /**
     * the list of cipher suites to be used in LDAPS and StartTLS
     * @deprecated See the {@link TcpTransport} class that contains this list
     **/
    @Deprecated
    private List<String> enabledCipherSuites = new ArrayList<>();


    /**
     * Creates an LDAP protocol provider.
     */
    public LdapServer()
    {
        super.setEnabled( true );
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );

        saslQop = new HashSet<>();
        saslQop.add( SaslQoP.AUTH.getValue() );
        saslQop.add( SaslQoP.AUTH_INT.getValue() );
        saslQop.add( SaslQoP.AUTH_CONF.getValue() );
        saslQopString = SaslQoP.AUTH.getValue() + ',' + SaslQoP.AUTH_INT.getValue() + ','
            + SaslQoP.AUTH_CONF.getValue();

        saslRealms = new ArrayList<>();
        saslRealms.add( "example.com" );

        this.supportedControls = new HashSet<>();
    }


    /**
     * Install the LDAP request handlers.
     */
    private void installDefaultHandlers()
    {
        if ( getAbandonRequestHandler() == null )
        {
            setAbandonHandler( new AbandonRequestHandler() );
        }

        if ( getAddRequestHandler() == null )
        {
            setAddHandlers( new AddRequestHandler(), new AddResponseHandler() );
        }

        if ( getBindRequestHandler() == null )
        {
            BindRequestHandler bindRequestHandler = new BindRequestHandler();
            bindRequestHandler.setSaslMechanismHandlers( saslMechanismHandlers );

            setBindHandlers( bindRequestHandler, new BindResponseHandler() );
        }

        if ( getCompareRequestHandler() == null )
        {
            setCompareHandlers( new CompareRequestHandler(), new CompareResponseHandler() );
        }

        if ( getDeleteRequestHandler() == null )
        {
            setDeleteHandlers( new DeleteRequestHandler(), new DeleteResponseHandler() );
        }

        if ( getExtendedRequestHandler() == null )
        {
            setExtendedHandlers( new ExtendedRequestHandler(), new ExtendedResponseHandler() );
        }

        if ( getIntermediateResponseHandler() == null )
        {
            setIntermediateHandler( new IntermediateResponseHandler() );
        }

        if ( getModifyRequestHandler() == null )
        {
            setModifyHandlers( new ModifyRequestHandler(), new ModifyResponseHandler() );
        }

        if ( getModifyDnRequestHandler() == null )
        {
            setModifyDnHandlers( new ModifyDnRequestHandler(), new ModifyDnResponseHandler() );
        }

        if ( getSearchRequestHandler() == null )
        {
            setSearchHandlers( new SearchRequestHandler(),
                new SearchResultEntryHandler(),
                new SearchResultReferenceHandler(),
                new SearchResultDoneHandler() );
        }

        if ( getUnbindRequestHandler() == null )
        {
            setUnbindHandler( new UnbindRequestHandler() );
        }
    }


    /**
     * reloads the SSL context by replacing the existing SslFilter
     * with a new SslFilter after reloading the keystore.
     *
     * Note: should be called to reload the keystore after changing the digital certificate.
     * @throws Exception If the SSLContext can't be reloaded
     */
    public void reloadSslContext() throws Exception
    {
        if ( !started )
        {
            return;
        }

        LOG.info( "reloading SSL context..." );

        keyManagerFactory = CertificateUtil.loadKeyStore( keystoreFile, certificatePassword );

        String sslFilterName = "sslFilter";

        for ( IoFilterChainBuilder chainBuilder : chainBuilders )
        {
            DefaultIoFilterChainBuilder dfcb = ( ( DefaultIoFilterChainBuilder ) chainBuilder );

            if ( dfcb.contains( sslFilterName ) )
            {
                // Get the TcpTransport
                TcpTransport tcpTransport = null;

                for ( Transport transport : getTransports() )
                {
                    if ( transport instanceof TcpTransport )
                    {
                        tcpTransport = ( TcpTransport ) transport;
                        break;
                    }
                }

                DefaultIoFilterChainBuilder newChain = ( DefaultIoFilterChainBuilder ) LdapsInitializer
                    .init( this, tcpTransport );
                dfcb.replace( sslFilterName, newChain.get( sslFilterName ) );
                newChain = null;
            }
        }

        StartTlsHandler handler = ( StartTlsHandler ) getExtendedOperationHandler( StartTlsHandler.EXTENSION_OID );

        if ( handler != null )
        {
            handler.setLdapServer( this );
        }

        LOG.info( "reloaded SSL context successfully" );
    }


    /**
     * @throws IOException if we cannot bind to the specified port
     * @throws Exception if the LDAP server cannot be started
     */
    @Override
    public void start() throws Exception
    {
        if ( !isEnabled() )
        {
            return;
        }

        keyManagerFactory = CertificateUtil.loadKeyStore( keystoreFile, certificatePassword );

        if ( trustManagers == null )
        {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( ( KeyStore ) null );
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        /*
         * The server is now initialized, we can
         * install the default requests handlers, which need
         * access to the DirectoryServer instance.
         */
        installDefaultHandlers();

        PartitionNexus nexus = getDirectoryService().getPartitionNexus();

        for ( ExtendedOperationHandler<? extends ExtendedRequest, ? extends ExtendedResponse> h : extendedOperationHandlers )
        {
            LOG.info( "Added Extended Request Handler: {}", h.getOid() );
            h.setLdapServer( this );
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        nexus.registerSupportedSaslMechanisms( saslMechanismHandlers.keySet() );

        // Install the replication handler if we have one
        startReplicationProducer();

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
                chain = LdapsInitializer.init( this, ( TcpTransport ) transport );
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

        // And start the replication consumers on this server
        // these should be started only after starting the network see DIRSERVER-1894
        startReplicationConsumers();

        started = true;

        LOG.info( "Ldap service started." );
    }


    /**
     * Install the replication handler if we have one
     */
    public void startReplicationProducer()
    {
        if ( replicationReqHandler != null )
        {
            replicationReqHandler.start( this );
            ( ( SearchRequestHandler ) getSearchRequestHandler() ).setReplicationReqHandler( replicationReqHandler );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
                List<WriteFuture> writeFutures = new ArrayList<>();

                // If the socket has already been unbound as with a successful
                // GracefulShutdownRequest then this will complain that the service
                // is not bound - this is ok because the GracefulShutdown has already
                // sent notices to to the existing active sessions
                List<IoSession> sessions;

                try
                {
                    sessions = new ArrayList<>( getSocketAcceptor( transport ).getManagedSessions().values() );
                }
                catch ( IllegalArgumentException e )
                {
                    LOG.warn( "Seems like the LDAP service ({}) has already been unbound.", getPort() );
                    return;
                }

                getSocketAcceptor( transport ).dispose();

                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Unbind of an LDAP service ({}) is complete.", getPort() );
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
                    sessionIt.next().closeNow();
                }

                if ( replicationReqHandler != null )
                {
                    replicationReqHandler.stop();
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
                LOG.info( "Successful bind of an LDAP Service ({}) is completed.", transport.getPort() );
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
     * Starts the replication consumers
     *
     * @throws LdapException If the consumer can't be started
     */
    public void startReplicationConsumers() throws Exception
    {
        if ( ( replConsumers != null ) && !replConsumers.isEmpty() )
        {
            final PingerThread pingerThread = new PingerThread( pingerSleepTime );
            pingerThread.start();

            for ( final ReplicationConsumer consumer : replConsumers )
            {
                consumer.init( getDirectoryService() );

                Runnable consumerTask = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            while ( true )
                            {
                                if ( CONSUMER_LOG.isDebugEnabled() )
                                {
                                    MDC.put( "Replica", consumer.getId() );
                                }

                                LOG.info( "starting the replication consumer with {}", consumer );
                                CONSUMER_LOG.info( "starting the replication consumer with {}", consumer );
                                boolean isConnected = consumer.connect( ReplicationConsumer.NOW );

                                if ( isConnected )
                                {
                                    pingerThread.addConsumer( consumer );

                                    // We are now connected, start the replication
                                    ReplicationStatusEnum status = null;

                                    do
                                    {
                                        status = consumer.startSync();
                                    }
                                    while ( status == ReplicationStatusEnum.REFRESH_REQUIRED );

                                    if ( status == ReplicationStatusEnum.STOPPED )
                                    {
                                        // Exit the loop
                                        break;
                                    }
                                }
                            }
                        }
                        catch ( Exception e )
                        {
                            LOG.error( "Failed to start consumer {}", consumer );
                            CONSUMER_LOG.error( "Failed to start consumer  {}", consumer );
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
            for ( ReplicationConsumer consumer : replConsumers )
            {
                LOG.info( "stopping the consumer with id {}", consumer.getId() );
                consumer.stop();
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
    public void addExtendedOperationHandler( ExtendedOperationHandler<? extends ExtendedRequest,
            ? extends ExtendedResponse> eoh ) throws LdapException
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
     * Deregister an {@link ExtendedOperationHandler} with the specified <tt>oid</tt>
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

        ExtendedOperationHandler<?, ?> handler = null;

        for ( ExtendedOperationHandler<?, ?> extendedOperationHandler : extendedOperationHandlers )
        {
            if ( extendedOperationHandler.getOid().equals( oid ) )
            {
                handler = extendedOperationHandler;
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
    public ExtendedOperationHandler<? extends ExtendedRequest, ? extends ExtendedResponse> getExtendedOperationHandler(
        String oid )
    {
        for ( ExtendedOperationHandler<? extends ExtendedRequest, ? extends ExtendedResponse>
                extendedOperationHandler : extendedOperationHandlers )
        {
            if ( extendedOperationHandler.getOid().equals( oid ) )
            {
                return extendedOperationHandler;
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
     * @param transport The LDAP transport
     * @return <tt>true</tt> if LDAPS is enabled.
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
        this.maxTimeLimit = maxTimeLimit; //TODO review the time parameters used all over the server and convert to seconds
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
    public Collection<ExtendedOperationHandler<? extends ExtendedRequest, ? extends ExtendedResponse>> getExtendedOperationHandlers()
    {
        return new ArrayList<>(
            extendedOperationHandlers );
    }


    /**
     * Sets the {@link ExtendedOperationHandler}s.
     *
     * @param handlers A collection of {@link ExtendedOperationHandler}s.
     */
    public void setExtendedOperationHandlers(
        Collection<ExtendedOperationHandler<ExtendedRequest, ExtendedResponse>> handlers )
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
     * @return the supported SASL mechanisms
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


    @Override
    public void setDirectoryService( DirectoryService directoryService )
    {
        super.setDirectoryService( directoryService );
        Iterator<String> itr = directoryService.getLdapCodecService().registeredRequestControls();

        while ( itr.hasNext() )
        {
            supportedControls.add( itr.next() );
        }

        itr = directoryService.getLdapCodecService().registeredResponseControls();

        while ( itr.hasNext() )
        {
            supportedControls.add( itr.next() );
        }
    }


    public Set<String> getSupportedControls()
    {
        return supportedControls;
    }


    /**
     * @return The MessageReceived handler for the AbandonRequest
     */
    public MessageHandler<AbandonRequest> getAbandonRequestHandler()
    {
        return abandonRequestHandler;
    }


    /**
     * Inject the MessageReceived handler into the IoHandler
     *
     * @param abandonRequestdHandler The AbandonRequest message received handler
     */
    public void setAbandonHandler( LdapRequestHandler<AbandonRequest> abandonRequestdHandler )
    {
        this.handler.removeReceivedMessageHandler( AbandonRequest.class );
        this.abandonRequestHandler = abandonRequestdHandler;
        this.abandonRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( AbandonRequest.class, this.abandonRequestHandler );
    }


    /**
     * @return The MessageReceived handler for the AddRequest
     */
    public LdapRequestHandler<AddRequest> getAddRequestHandler()
    {
        return addRequestHandler;
    }


    /**
     * @return The MessageSent handler for the AddResponse
     */
    public LdapResponseHandler<AddResponse> getAddResponseHandler()
    {
        return addResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param addRequestHandler The AddRequest message received handler
     * @param addResponseHandler The AddResponse message sent handler
     */
    public void setAddHandlers( LdapRequestHandler<AddRequest> addRequestHandler,
        LdapResponseHandler<AddResponse> addResponseHandler )
    {
        this.handler.removeReceivedMessageHandler( AddRequest.class );
        this.addRequestHandler = addRequestHandler;
        this.addRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( AddRequest.class, this.addRequestHandler );

        this.handler.removeSentMessageHandler( AddResponse.class );
        this.addResponseHandler = addResponseHandler;
        this.addResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( AddResponse.class, this.addResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the BindRequest
     */
    public LdapRequestHandler<BindRequest> getBindRequestHandler()
    {
        return bindRequestHandler;
    }


    /**
     * @return The MessageSent handler for the BindResponse
     */
    public LdapResponseHandler<BindResponse> getBindResponseHandler()
    {
        return bindResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param bindRequestHandler The BindRequest message received handler
     * @param bindResponseHandler The BindResponse message sent handler
     */
    public void setBindHandlers( LdapRequestHandler<BindRequest> bindRequestHandler,
        LdapResponseHandler<BindResponse> bindResponseHandler )
    {
        handler.removeReceivedMessageHandler( BindRequest.class );
        this.bindRequestHandler = bindRequestHandler;
        this.bindRequestHandler.setLdapServer( this );
        handler.addReceivedMessageHandler( BindRequest.class, this.bindRequestHandler );

        handler.removeSentMessageHandler( BindResponse.class );
        this.bindResponseHandler = bindResponseHandler;
        this.bindResponseHandler.setLdapServer( this );
        handler.addSentMessageHandler( BindResponse.class, this.bindResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the CompareRequest
     */
    public LdapRequestHandler<CompareRequest> getCompareRequestHandler()
    {
        return compareRequestHandler;
    }


    /**
     * @return The MessageSent handler for the CompareResponse
     */
    public LdapResponseHandler<CompareResponse> getCompareResponseHandler()
    {
        return compareResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param compareRequestHandler The CompareRequest message received handler
     * @param compareResponseHandler The CompareResponse message sent handler
     */
    public void setCompareHandlers( LdapRequestHandler<CompareRequest> compareRequestHandler,
        LdapResponseHandler<CompareResponse> compareResponseHandler )
    {
        handler.removeReceivedMessageHandler( CompareRequest.class );
        this.compareRequestHandler = compareRequestHandler;
        this.compareRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( CompareRequest.class, this.compareRequestHandler );

        handler.removeReceivedMessageHandler( CompareResponse.class );
        this.compareResponseHandler = compareResponseHandler;
        this.compareResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( CompareResponse.class, this.compareResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the DeleteRequest
     */
    public LdapRequestHandler<DeleteRequest> getDeleteRequestHandler()
    {
        return deleteRequestHandler;
    }


    /**
     * @return The MessageSent handler for the DeleteResponse
     */
    public LdapResponseHandler<DeleteResponse> getDeleteResponseHandler()
    {
        return deleteResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param deleteRequestHandler The DeleteRequest message received handler
     * @param deleteResponseHandler The DeleteResponse message sent handler
     */
    public void setDeleteHandlers( LdapRequestHandler<DeleteRequest> deleteRequestHandler,
        LdapResponseHandler<DeleteResponse> deleteResponseHandler )
    {
        handler.removeReceivedMessageHandler( DeleteRequest.class );
        this.deleteRequestHandler = deleteRequestHandler;
        this.deleteRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( DeleteRequest.class, this.deleteRequestHandler );

        handler.removeSentMessageHandler( DeleteResponse.class );
        this.deleteResponseHandler = deleteResponseHandler;
        this.deleteResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( DeleteResponse.class, this.deleteResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the ExtendedRequest
     */
    public LdapRequestHandler<ExtendedRequest> getExtendedRequestHandler()
    {
        return extendedRequestHandler;
    }


    /**
     * @return The MessageSent handler for the ExtendedResponse
     */
    public LdapResponseHandler<ExtendedResponse> getExtendedResponseHandler()
    {
        return extendedResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param extendedRequestHandler The ExtendedRequest message received handler
     * @param extendedResponseHandler The ExtendedResponse message sent handler
     */
    @SuppressWarnings(
        { "unchecked", "rawtypes" })
    public void setExtendedHandlers( ExtendedRequestHandler extendedRequestHandler,
        ExtendedResponseHandler extendedResponseHandler )
    {
        handler.removeReceivedMessageHandler( ExtendedRequest.class );
        this.extendedRequestHandler = extendedRequestHandler;
        this.extendedRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ExtendedRequest.class,
            this.extendedRequestHandler );

        handler.removeSentMessageHandler( ExtendedResponse.class );
        this.extendedResponseHandler = extendedResponseHandler;
        this.extendedResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( ExtendedResponse.class, this.extendedResponseHandler );
    }


    /**
     * @return The MessageSent handler for the IntermediateResponse
     */
    public LdapResponseHandler<IntermediateResponse> getIntermediateResponseHandler()
    {
        return intermediateResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param intermediateResponseHandler The IntermediateResponse message sent handler
     */
    public void setIntermediateHandler( LdapResponseHandler<IntermediateResponse> intermediateResponseHandler )
    {
        handler.removeSentMessageHandler( IntermediateResponse.class );
        this.intermediateResponseHandler = intermediateResponseHandler;
        this.intermediateResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( IntermediateResponse.class, this.intermediateResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the ModifyRequest
     */
    public LdapRequestHandler<ModifyRequest> getModifyRequestHandler()
    {
        return modifyRequestHandler;
    }


    /**
     * @return The MessageSent handler for the ModifyResponse
     */
    public LdapResponseHandler<ModifyResponse> getModifyResponseHandler()
    {
        return modifyResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param modifyRequestHandler The ModifyRequest message received handler
     * @param modifyResponseHandler The ModifyResponse message sent handler
     */
    public void setModifyHandlers( LdapRequestHandler<ModifyRequest> modifyRequestHandler,
        LdapResponseHandler<ModifyResponse> modifyResponseHandler )
    {
        handler.removeReceivedMessageHandler( ModifyRequest.class );
        this.modifyRequestHandler = modifyRequestHandler;
        this.modifyRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ModifyRequest.class, this.modifyRequestHandler );

        handler.removeSentMessageHandler( ModifyResponse.class );
        this.modifyResponseHandler = modifyResponseHandler;
        this.modifyResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( ModifyResponse.class, this.modifyResponseHandler );
    }


    /**
     * @return The MessageSent handler for the ModifyDnRequest
     */
    public LdapRequestHandler<ModifyDnRequest> getModifyDnRequestHandler()
    {
        return modifyDnRequestHandler;
    }


    /**
     * @return The MessageSent handler for the ModifyDnResponse
     */
    public LdapResponseHandler<ModifyDnResponse> getModifyDnResponseHandler()
    {
        return modifyDnResponseHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param modifyDnRequestHandler The ModifyDnRequest message received handler
     * @param modifyDnResponseHandler The ModifyDnResponse message sent handler
     */
    public void setModifyDnHandlers( LdapRequestHandler<ModifyDnRequest> modifyDnRequestHandler,
        LdapResponseHandler<ModifyDnResponse> modifyDnResponseHandler )
    {
        handler.removeReceivedMessageHandler( ModifyDnRequest.class );
        this.modifyDnRequestHandler = modifyDnRequestHandler;
        this.modifyDnRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( ModifyDnRequest.class, this.modifyDnRequestHandler );

        handler.removeSentMessageHandler( ModifyDnResponse.class );
        this.modifyDnResponseHandler = modifyDnResponseHandler;
        this.modifyDnResponseHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( ModifyDnResponse.class, this.modifyDnResponseHandler );
    }


    /**
     * @return The MessageReceived handler for the SearchRequest
     */
    public LdapRequestHandler<SearchRequest> getSearchRequestHandler()
    {
        return searchRequestHandler;
    }


    /**
     * @return The MessageSent handler for the SearchResultEntry
     */
    public LdapResponseHandler<SearchResultEntry> getSearchResultEntryHandler()
    {
        return searchResultEntryHandler;
    }


    /**
     * @return The MessageSent handler for the SearchResultReference
     */
    public LdapResponseHandler<SearchResultReference> getSearchResultReferenceHandler()
    {
        return searchResultReferenceHandler;
    }


    /**
     * @return The MessageSent handler for the SearchResultDone
     */
    public LdapResponseHandler<SearchResultDone> getSearchResultDoneHandler()
    {
        return searchResultDoneHandler;
    }


    /**
     * Inject the MessageReceived and MessageSent handler into the IoHandler
     *
     * @param searchRequestHandler The SearchRequest message received handler
     * @param searchResultEntryHandler The SearchResultEntry message sent handler
     * @param searchResultReferenceHandler The SearchResultReference message sent handler
     * @param searchResultDoneHandler The SearchResultDone message sent handler
     */
    public void setSearchHandlers( LdapRequestHandler<SearchRequest> searchRequestHandler,
        LdapResponseHandler<SearchResultEntry> searchResultEntryHandler,
        LdapResponseHandler<SearchResultReference> searchResultReferenceHandler,
        LdapResponseHandler<SearchResultDone> searchResultDoneHandler
        )
    {
        this.handler.removeReceivedMessageHandler( SearchRequest.class );
        this.searchRequestHandler = searchRequestHandler;
        this.searchRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( SearchRequest.class, this.searchRequestHandler );

        this.handler.removeSentMessageHandler( SearchResultEntry.class );
        this.searchResultEntryHandler = searchResultEntryHandler;
        this.searchResultEntryHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( SearchResultEntry.class, this.searchResultEntryHandler );

        this.handler.removeSentMessageHandler( SearchResultReference.class );
        this.searchResultReferenceHandler = searchResultReferenceHandler;
        this.searchResultReferenceHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( SearchResultReference.class, this.searchResultReferenceHandler );

        this.handler.removeSentMessageHandler( SearchResultDone.class );
        this.searchResultDoneHandler = searchResultDoneHandler;
        this.searchResultDoneHandler.setLdapServer( this );
        this.handler.addSentMessageHandler( SearchResultDone.class, this.searchResultDoneHandler );
    }


    /**
     * @return The MessageReceived handler for the UnbindRequest
     */
    public LdapRequestHandler<UnbindRequest> getUnbindRequestHandler()
    {
        return unbindRequestHandler;
    }


    /**
     * Inject the MessageReceived handler into the IoHandler
     *
     * @param unbindRequestHandler The UnbindRequest message received handler
     */
    public void setUnbindHandler( LdapRequestHandler<UnbindRequest> unbindRequestHandler )
    {
        this.handler.removeReceivedMessageHandler( UnbindRequest.class );
        this.unbindRequestHandler = unbindRequestHandler;
        this.unbindRequestHandler.setLdapServer( this );
        this.handler.addReceivedMessageHandler( UnbindRequest.class, this.unbindRequestHandler );
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


    @Override
    public boolean isStarted()
    {
        return started;
    }


    /**
     */
    @Override
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
     * @return The certificate password
     */
    public String getCertificatePassword()
    {
        return certificatePassword;
    }


    /**
     * Set the certificate password.
     * @param certificatePassword the certificate password
     */
    public void setCertificatePassword( String certificatePassword )
    {
        this.certificatePassword = certificatePassword;
    }


    public void setReplicationReqHandler( ReplicationRequestHandler replicationProvider )
    {
        this.replicationReqHandler = replicationProvider;
    }


    public ReplicationRequestHandler getReplicationReqHandler()
    {
        return replicationReqHandler;
    }


    public void setReplConsumers( List<ReplicationConsumer> replConsumers )
    {
        this.replConsumers = replConsumers;
    }


    /**
     * @return the key manager factory of the server keystore
     */
    public KeyManagerFactory getKeyManagerFactory()
    {
        return keyManagerFactory;
    }

    /**
     * @return the trust managers of the server
     */
    public TrustManager[] getTrustManagers()
    {
        return trustManagers;
    }

    public void setTrustManagers( TrustManager[] trustManagers )
    {
        this.trustManagers = trustManagers;
    }

    /**
     * @return The maximum allowed size for an incoming PDU
     */
    public int getMaxPDUSize()
    {
        return maxPDUSize;
    }


    /**
     * Set the maximum allowed size for an incoming PDU
     * @param maxPDUSize A positive number of bytes for the PDU. A negative or
     * null value will be transformed to {@link Integer#MAX_VALUE}
     */
    public void setMaxPDUSize( int maxPDUSize )
    {
        if ( maxPDUSize <= 0 )
        {
            maxPDUSize = Integer.MAX_VALUE;
        }

        this.maxPDUSize = maxPDUSize;
    }


    /**
     * @return the number of seconds pinger thread sleeps between subsequent pings
     */
    public int getReplPingerSleepTime()
    {
        return pingerSleepTime;
    }


    /**
     * The number of seconds pinger thread should sleep before pinging the providers
     *
     * @param pingerSleepTime The delay between 2 pings
     */
    public void setReplPingerSleepTime( int pingerSleepTime )
    {
        this.pingerSleepTime = pingerSleepTime;
    }


    /**
     * Gives the list of enabled cipher suites
     * <br>
     * This method has been deprecated, please set this list in the TcpTransport class
     * <br>
     *
     * @return The list of ciphers that can be used
     * @deprecated Set this list in the {@link TcpTransport} class
     */
    @Deprecated
    public List<String> getEnabledCipherSuites()
    {
        return enabledCipherSuites;
    }


    /**
     * Sets the list of cipher suites to be used in LDAPS and StartTLS
     * <br>
     * This method has been deprecated, please set this list in the TcpTransport class
     * <br>
     *
     * @param enabledCipherSuites if null the default cipher suites will be used
     * @deprecated Get this list from the {@link TcpTransport} class
     */
    @Deprecated
    public void setEnabledCipherSuites( List<String> enabledCipherSuites )
    {
        this.enabledCipherSuites = enabledCipherSuites;
    }


    /**
     * @see Object#toString()
     */
    @Override
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
