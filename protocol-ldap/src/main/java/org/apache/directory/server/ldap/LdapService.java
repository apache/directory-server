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
import java.net.InetSocketAddress;
import java.security.KeyStore;
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
import org.apache.directory.server.ldap.handlers.LdapRequestHandler;
import org.apache.directory.server.ldap.handlers.AbandonHandler;
import org.apache.directory.server.ldap.handlers.AddHandler;
import org.apache.directory.server.ldap.handlers.BindHandler;
import org.apache.directory.server.ldap.handlers.CompareHandler;
import org.apache.directory.server.ldap.handlers.DeleteHandler;
import org.apache.directory.server.ldap.handlers.ExtendedHandler;
import org.apache.directory.server.ldap.handlers.ModifyDnHandler;
import org.apache.directory.server.ldap.handlers.ModifyHandler;
import org.apache.directory.server.ldap.handlers.SearchHandler;
import org.apache.directory.server.ldap.handlers.UnbindHandler;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.ssl.LdapsInitializer;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.shared.ldap.constants.SaslQoP;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.UnbindRequest;
import org.apache.directory.shared.ldap.message.control.CascadeControl;
import org.apache.directory.shared.ldap.message.control.EntryChangeControl;
import org.apache.directory.shared.ldap.message.control.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.control.PagedSearchControl;
import org.apache.directory.shared.ldap.message.control.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.control.SubentriesControl;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.handler.demux.MessageHandler;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 688548 $
 * @org.apache.xbean.XBean
 */
public class LdapService extends DirectoryBackedService
{
    /** Value (0) for configuration where size limit is unlimited. */
    public static final int NO_SIZE_LIMIT = 0;

    /** Value (0) for configuration where time limit is unlimited. */
    public static final int NO_TIME_LIMIT = 0;

    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";

    
    
    private static final long serialVersionUID = 3757127143811666817L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapService.class.getName() );

    /** The default maximum size limit. */
    private static final int MAX_SIZE_LIMIT_DEFAULT = 100;

    /** The default maximum time limit. */
    private static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ldap";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS LDAP Service";

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 389;

    /** the session manager for this LdapService */
    private LdapSessionManager ldapSessionManager = new LdapSessionManager();
    
    /** a set of supported controls */
    private Set<String> supportedControls;

    /** 
     * The maximum size limit. 
     * @see {@link LdapService#MAX_SIZE_LIMIT_DEFAULT }
     */
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; 

    /** 
     * The maximum time limit.
     * @see {@link LdapService#MAX_TIME_LIMIT_DEFAULT }
     */
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT; 

    /** Whether LDAPS is enabled: disabled by default. */
    private boolean enableLdaps;

    /** Whether to allow anonymous access: enabled by default. */
    private boolean allowAnonymousAccess = true;

    /** The extended operation handlers. */
    private final Collection<ExtendedOperationHandler> extendedOperationHandlers =
        new ArrayList<ExtendedOperationHandler>();

    /** The supported authentication mechanisms. */
    private Map<String, MechanismHandler> saslMechanismHandlers =
        new HashMap<String, MechanismHandler>();

    /** The name of this host, validated during SASL negotiation. */
    private String saslHost = "ldap.example.com";

    /** The service principal, used by GSSAPI. */
    private String saslPrincipal = "ldap/ldap.example.com@EXAMPLE.COM";

    /** The quality of protection (QoP), used by DIGEST-MD5 and GSSAPI. */
    private Set<String> saslQop;
    private String      saslQopString;

    /** The list of realms serviced by this host. */
    private List<String> saslRealms;

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
    private final LdapProtocolHandler handler = new LdapProtocolHandler(this);

    /** tracks start state of the server */
    private boolean started;

    /** 
     * Whether or not confidentiality (TLS secured connection) is required: 
     * disabled by default. 
     */
    private boolean confidentialityRequired;


    /**
     * Creates an LDAP protocol provider.
     */
    public LdapService()
    {
        super.setTcpPort( IP_PORT_DEFAULT );
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
        this.supportedControls.add( PersistentSearchControl.CONTROL_OID );
        this.supportedControls.add( EntryChangeControl.CONTROL_OID );
        this.supportedControls.add( SubentriesControl.CONTROL_OID );
        this.supportedControls.add( ManageDsaITControl.CONTROL_OID );
        this.supportedControls.add( CascadeControl.CONTROL_OID );
        this.supportedControls.add( PagedSearchControl.CONTROL_OID );
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


    /**
     * @throws IOException if we cannot bind to the specified port
     * @throws NamingException if the LDAP server cannot be started
     */
    public void start() throws Exception
    {
        if ( ! isEnabled() )
        {
            return;
        }

        IoFilterChainBuilder chain;
        
        if ( isEnableLdaps() )
        {
            Provider provider = Security.getProvider( "SUN" );
            LOG.debug( "provider = {}", provider );
            CoreKeyStoreSpi coreKeyStoreSpi = new CoreKeyStoreSpi( getDirectoryService() );
            KeyStore keyStore = new KeyStore( coreKeyStoreSpi, provider, "JKS" ) {};
            try
            {
                keyStore.load( null, null );
            }
            catch ( Exception e )
            {
                // nothing really happens with this keystore
            }
            chain = LdapsInitializer.init( keyStore );
        }
        else
        {
            chain = new DefaultIoFilterChainBuilder();
        }
        
        // Inject the codec into the chain
        ((DefaultIoFilterChainBuilder)chain).addLast( "codec", 
        		new ProtocolCodecFilter( this.getProtocolCodecFactory() ) );

        /*
         * The server is now initialized, we can
         * install the default requests handlers, which need 
         * access to the DirectoryServer instance.
         */ 
        installDefaultHandlers();      

        startLDAP0( getTcpPort(), chain );
        
        started = true;
        
        if ( isEnableLdaps() )
        {
            LOG.info( "Ldaps service started." );
            System.out.println( "Ldaps service started." );
        }
        else
        {
            LOG.info( "Ldap service started." );
            System.out.println( "Ldap service started." );
        }
    }


    public void stop()
    {
        try
        {
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
                sessions = new ArrayList<IoSession>(
                        getSocketAcceptor().getManagedSessions().values() );
            }
            catch ( IllegalArgumentException e )
            {
                LOG.warn( "Seems like the LDAP service (" + getIpPort() + ") has already been unbound." );
                return;
            }

            getSocketAcceptor().unbind( new InetSocketAddress( getIpPort() ) );

            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of an LDAP service (" + getIpPort() + ") is complete." );
                LOG.info( "Sending notice of disconnect to existing clients sessions." );
            }

            // Send Notification of Disconnection messages to all connected clients.
            if ( sessions != null )
            {
                for ( IoSession session:sessions )
                {
                    writeFutures.add( session.write( NoticeOfDisconnect.UNAVAILABLE ) );
                }
            }

            // And close the connections when the NoDs are sent.
            Iterator<IoSession> sessionIt = sessions.iterator();

            for ( WriteFuture future:writeFutures )
            {
                future.await( 1000L );
                sessionIt.next().close( true );
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to sent NoD.", e );
        }

        if ( isEnableLdaps() )
        {
            LOG.info( "Ldaps service stopped." );
            System.out.println( "Ldaps service stopped." );
        }
        else
        {
            LOG.info( "Ldap service stopped." );
            System.out.println( "Ldap service stopped." );
        }
    }


    private void startLDAP0( int port, IoFilterChainBuilder chainBuilder )
        throws Exception
    {
        PartitionNexus nexus = getDirectoryService().getPartitionNexus();

        for ( ExtendedOperationHandler h : extendedOperationHandlers )
        {
            LOG.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapServer( this );
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        nexus.registerSupportedSaslMechanisms( saslMechanismHandlers.keySet() );

        try
        {
        	// First, create the acceptor with the configured number of threads (if defined)
        	int nbTcpThreads = getNbTcpThreads();
        	SocketAcceptor acceptor;
        	
        	if ( nbTcpThreads > 0 )
        	{
        		acceptor = new NioSocketAcceptor( nbTcpThreads );
        	}
        	else
        	{
        		acceptor = new NioSocketAcceptor();
        	}
        		
        	setSocketAcceptor( acceptor );
        	
        	// Now, configure the acceptor
            // Disable the disconnection of the clients on unbind
        	acceptor.setCloseOnDeactivation( false );
        	
        	// Allow the port to be reused even if the socket is in TIME_WAIT state
        	acceptor.setReuseAddress( true );
        	
        	// No Nagle's algorithm
        	acceptor.getSessionConfig().setTcpNoDelay( true );
        	
        	// Inject the chain
        	acceptor.setFilterChainBuilder( chainBuilder );
        	
        	// Inject the protocol handler
        	acceptor.setHandler( getHandler() );
        	
        	// Bind to the configured address
        	acceptor.bind( new InetSocketAddress( port ) );
        	
        	// We are done !
            started = true;

            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Successful bind of an LDAP Service (" + port + ") is complete." );
            }
        }
        catch ( IOException e )
        {
            String msg = "Failed to bind an LDAP service (" + port + ") to the service registry.";
            LdapConfigurationException lce = new LdapConfigurationException( msg );
            lce.setRootCause( e );
            LOG.error( msg, e );
            throw lce;
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
     * Registeres the specified {@link ExtendedOperationHandler} to this
     * protocol provider to provide a specific LDAP extended operation.
     *
     * @param eoh an extended operation handler
     * @throws NamingException on failure to add the handler
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
        //            PartitionNexus nexus = getDirectoryService().getPartitionNexus();
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
     * Sets the mode for this LdapService to accept requests with or without a
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
     * operations on this LdapService.
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
    public boolean isEnableLdaps()
    {
        return enableLdaps;
    }


    /**
     * Sets if LDAPS is enabled or not.
     *
     * @param enableLdaps Whether LDAPS is enabled.
     */
    public void setEnableLdaps( boolean enableLdaps )
    {
        this.enableLdaps = enableLdaps;
    }


    /**
     * Returns <code>true</code> if anonymous access is allowed.
     *
     * @return True if anonymous access is allowed.
     */
    public boolean isAllowAnonymousAccess()
    {
        return allowAnonymousAccess;
    }


    /**
     * Sets whether to allow anonymous access or not.
     *
     * @param enableAnonymousAccess Set <code>true</code> to allow anonymous access.
     */
    public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {
        this.allowAnonymousAccess = enableAnonymousAccess;
    }


    /**
     * Sets the maximum size limit in number of entries to return for search.
     *
     * @param maxSizeLimit the maximum number of entries to return for search
     */
    public void setMaxSizeLimit( int maxSizeLimit )
    {
        this.maxSizeLimit = maxSizeLimit;
    }


    /**
     * Returns the maximum size limit in number of entries to return for search.
     *
     * @return The maximum size limit.
     */
    public int getMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    /**
     * Sets the maximum time limit in miliseconds to conduct a search.
     *
     * @param maxTimeLimit the maximum length of time in milliseconds for search
     */
    public void setMaxTimeLimit( int maxTimeLimit )
    {
        this.maxTimeLimit = maxTimeLimit;
    }


    /**
     * Returns the maximum time limit in milliseonds to conduct a search.
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
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.ldap.ExtendedOperationHandler"
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
     * Sets the desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     * 
     * We build a string from this list, where QoP are comma delimited 
     *
     * @org.apache.xbean.Property nestedType="java.lang.String"
     *
     * @param saslQop The desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslQop( Set<String> saslQop )
    {
        StringBuilder qopList = new StringBuilder();
        boolean isFirst = true;

        for ( String qop:saslQop )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                qopList.append( ',' );
            }
            
            qopList.append( qop );
        }

        this.saslQopString = qopList.toString();
        this.saslQop = saslQop;
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
     * @org.apache.xbean.Property nestedType="java.lang.String"
     *
     * @param saslRealms The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslRealms( List<String> saslRealms )
    {
        this.saslRealms = saslRealms;
    }


    /**
     * @org.apache.xbean.Map flat="true" dups="replace" keyName="mech-name"
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


    public void setSupportedControls( Set<String> supportedControls )
    {
        this.supportedControls = supportedControls;
    }


    public MessageHandler<AbandonRequest> getAbandonHandler()
    {
        return abandonHandler;
    }


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


    public void setStarted( boolean started )
    {
        this.started = started;
    }
}
