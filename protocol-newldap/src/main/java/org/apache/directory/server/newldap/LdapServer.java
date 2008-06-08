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
package org.apache.directory.server.newldap;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.ldap.Control;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.security.CoreKeyStoreSpi;
import org.apache.directory.server.newldap.handlers.CompareHandler;
import org.apache.directory.server.newldap.handlers.DefaultCompareHandler;
import org.apache.directory.server.newldap.handlers.DefaultDeleteHandler;
import org.apache.directory.server.newldap.handlers.DefaultExtendedHandler;
import org.apache.directory.server.newldap.handlers.DefaultModifyDnHandler;
import org.apache.directory.server.newldap.handlers.DefaultModifyHandler;
import org.apache.directory.server.newldap.handlers.DefaultSearchHandler;
import org.apache.directory.server.newldap.handlers.DefaultUnbindHandler;
import org.apache.directory.server.newldap.handlers.DeleteHandler;
import org.apache.directory.server.newldap.handlers.ExtendedHandler;
import org.apache.directory.server.newldap.handlers.LdapRequestHandler;
import org.apache.directory.server.newldap.handlers.ModifyDnHandler;
import org.apache.directory.server.newldap.handlers.ModifyHandler;
import org.apache.directory.server.newldap.handlers.NewAbandonHandler;
import org.apache.directory.server.newldap.handlers.NewAddHandler;
import org.apache.directory.server.newldap.handlers.NewBindHandler;
import org.apache.directory.server.newldap.handlers.SearchHandler;
import org.apache.directory.server.newldap.handlers.UnbindHandler;
import org.apache.directory.server.newldap.handlers.bind.*;
import org.apache.directory.server.newldap.handlers.ssl.LdapsInitializer;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.asn1.codec.Asn1CodecDecoder;
import org.apache.directory.shared.asn1.codec.Asn1CodecEncoder;
import org.apache.directory.shared.ldap.constants.SaslQoP;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AddRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.CascadeControl;
import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.directory.shared.ldap.message.DeleteRequest;
import org.apache.directory.shared.ldap.message.EntryChangeControl;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.MessageDecoder;
import org.apache.directory.shared.ldap.message.MessageEncoder;
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.directory.shared.ldap.message.ModifyRequest;
import org.apache.directory.shared.ldap.message.MutableControl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.directory.shared.ldap.message.ResponseCarryingMessageException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.message.ResultResponseRequest;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SubentriesControl;
import org.apache.directory.shared.ldap.message.UnbindRequest;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.handler.demux.DemuxingIoHandler;
import org.apache.mina.handler.demux.MessageHandler;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.util.SessionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDAP protocol provider implementation which dynamically associates
 * handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 * @org.apache.xbean.XBean
 */
public class LdapServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 3757127143811666817L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapServer.class.getName() );

    /** The default maximum size limit. */
    private static final int MAX_SIZE_LIMIT_DEFAULT = 100;

    /** The default maximum time limit. */
    private static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.newldap";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS LDAP Service";

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 389;

    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";

    
    
    /** a set of supported controls */
    private Set<String> supportedControls;

    /** 
     * The maximum size limit. 
     * @see {@link LdapServer#MAX_SIZE_LIMIT_DEFAULT }
     */
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; 

    /** 
     * The maximum time limit.
     * @see {@link LdapServer#MAX_TIME_LIMIT_DEFAULT }
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
    private CompareHandler compareHandler;
    private DeleteHandler deleteHandler;
    private ExtendedHandler extendedHandler;
    private ModifyHandler modifyHandler;
    private ModifyDnHandler modifyDnHandler;
    private SearchHandler searchHandler;
    private UnbindHandler unbindHandler;


    /** the underlying provider codec factory */
    private ProtocolCodecFactory codecFactory;

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler();

    /** tracks start state of the server */
    private boolean started;


    /**
     * Creates an LDAP protocol provider.
     */
    public LdapServer()
    {
        super.setIpPort( IP_PORT_DEFAULT );
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
    }


    /**
     * Install the LDAP request handlers.
     */
    private void installDefaultHandlers()
    {
        if ( getAbandonHandler() == null )
        {
            setAbandonHandler( new NewAbandonHandler() );
        }
        
        if ( getAddHandler() == null )
        {
            setAddHandler( new NewAddHandler() );
        }
        
        if ( getBindHandler() == null )
        {
            NewBindHandler handler = new NewBindHandler();
            handler.setSaslMechanismHandlers( saslMechanismHandlers );
            setBindHandler( handler );
        }
        
        if ( getCompareHandler() == null )
        {
            setCompareHandler( new DefaultCompareHandler() );
        }
        
        if ( getDeleteHandler() == null )
        {
            setDeleteHandler( new DefaultDeleteHandler() );
        }
        
        if ( getExtendedHandler() == null )
        {
            setExtendedHandler( new DefaultExtendedHandler() );
        }
        
        if ( getModifyHandler() == null )
        {
            setModifyHandler( new DefaultModifyHandler() );
        }
        
        if ( getModifyDnHandler() == null )
        {
            setModifyDnHandler( new DefaultModifyDnHandler() );
        }
        
        if ( getSearchHandler() == null )
        {
            setSearchHandler( new DefaultSearchHandler() );
        }
        
        if ( getUnbindHandler() == null )
        {
            setUnbindHandler( new DefaultUnbindHandler() );
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

        /*
         * The serveur is now initialized, we can
         * install the default requests handlers, which need 
         * access to the DirectoryServer instance.
         */ 
        installDefaultHandlers();      

        startLDAP0( getIpPort(), chain );
        
        started = true;
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
                        getSocketAcceptor().getManagedSessions( new InetSocketAddress( getIpPort() ) ) );
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
                future.join( 1000 );
                sessionIt.next().close();
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to sent NoD.", e );
        }
    }


    private void startLDAP0( int port, IoFilterChainBuilder chainBuilder )
        throws Exception
    {
        PartitionNexus nexus = getDirectoryService().getPartitionNexus();

        for ( ExtendedOperationHandler h : extendedOperationHandlers )
        {
            extendedHandler.addHandler( h );
            LOG.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapProvider( this );
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        nexus.registerSupportedSaslMechanisms( saslMechanismHandlers.keySet() );

        try
        {
            SocketAcceptorConfig acceptorCfg = new SocketAcceptorConfig();

            // Disable the disconnection of the clients on unbind
            acceptorCfg.setDisconnectOnUnbind( false );
            acceptorCfg.setReuseAddress( true );
            acceptorCfg.setFilterChainBuilder( chainBuilder );
            acceptorCfg.setThreadModel( ThreadModel.MANUAL );

            acceptorCfg.getSessionConfig().setTcpNoDelay( true );

            getSocketAcceptor().bind( new InetSocketAddress( port ), getHandler(), acceptorCfg );
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


    public ProtocolCodecFactory getCodecFactory()
    {
        return codecFactory;
    }


    public IoHandler getHandler()
    {
        return handler;
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
            extendedHandler.addHandler( eoh );
            eoh.setLdapProvider( this );
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
        if ( started )
        {
            extendedHandler.removeHandler( oid );

            // need to do something like this to make this work right
            //            PartitionNexus nexus = getDirectoryService().getPartitionNexus();
            //            nexus.unregisterSupportedExtensions( eoh.getExtensionOids() );
        }
        else
        {
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
        if ( started )
        {
            return extendedHandler.getHandler( oid );
        }
        else
        {
            for ( ExtendedOperationHandler h : extendedOperationHandlers )
            {
                if ( h.getOid().equals( oid ) )
                {
                    return h;
                }
            }
        }

        return null;
    }


    /**
     * Returns a {@link Map} of all registered OID-{@link ExtendedOperationHandler}
     * pairs.
     *
     * @return map of all extended operation handlers
     */
    public Map<String,ExtendedOperationHandler> getExtendedOperationHandlerMap()
    {
        return extendedHandler.getHandlerMap();
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
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.newldap.ExtendedOperationHandler"
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
        this.codecFactory = new ProtocolCodecFactoryImpl( directoryService );
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
        this.handler.removeMessageHandler( AbandonRequest.class );
        this.abandonHandler = abandonHandler;
        this.abandonHandler.setLdapServer( this );
        this.handler.addMessageHandler( AbandonRequest.class, this.abandonHandler );
    }


    public LdapRequestHandler<AddRequest> getAddHandler()
    {
        return addHandler;
    }


    public void setAddHandler( LdapRequestHandler<AddRequest> addHandler )
    {
        this.handler.removeMessageHandler( AddRequest.class );
        this.addHandler = addHandler;
        this.addHandler.setLdapServer( this );
        this.handler.addMessageHandler( AddRequest.class, this.addHandler );
    }


    public LdapRequestHandler<BindRequest> getBindHandler()
    {
        return bindHandler;
    }


    public void setBindHandler( LdapRequestHandler<BindRequest> bindHandler )
    {
        this.handler.removeMessageHandler( BindRequest.class );
        this.bindHandler = bindHandler;
        this.bindHandler.setLdapServer( this );
        this.handler.addMessageHandler( BindRequest.class, this.bindHandler );
    }


    public CompareHandler getCompareHandler()
    {
        return compareHandler;
    }


    public void setCompareHandler( CompareHandler compareHandler )
    {
        this.handler.removeMessageHandler( CompareRequest.class );
        this.compareHandler = compareHandler;
        this.compareHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( CompareRequest.class, this.compareHandler );
    }


    public DeleteHandler getDeleteHandler()
    {
        return deleteHandler;
    }


    public void setDeleteHandler( DeleteHandler deleteHandler )
    {
        this.handler.removeMessageHandler( DeleteRequest.class );
        this.deleteHandler = deleteHandler;
        this.deleteHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( DeleteRequest.class, this.deleteHandler );
    }


    public ExtendedHandler getExtendedHandler()
    {
        return extendedHandler;
    }


    public void setExtendedHandler( ExtendedHandler extendedHandler )
    {
        this.handler.removeMessageHandler( ExtendedRequest.class );
        this.extendedHandler = extendedHandler;
        this.extendedHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( ExtendedRequest.class, this.extendedHandler );
    }


    public ModifyHandler getModifyHandler()
    {
        return modifyHandler;
    }


    public void setModifyHandler( ModifyHandler modifyHandler )
    {
        this.handler.removeMessageHandler( ModifyRequest.class );
        this.modifyHandler = modifyHandler;
        this.modifyHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( ModifyRequest.class, this.modifyHandler );
    }


    public ModifyDnHandler getModifyDnHandler()
    {
        return modifyDnHandler;
    }


    public void setModifyDnHandler( ModifyDnHandler modifyDnHandler )
    {
        this.handler.removeMessageHandler( ModifyDnRequest.class );
        this.modifyDnHandler = modifyDnHandler;
        this.modifyDnHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( ModifyDnRequest.class, this.modifyDnHandler );
    }


    public SearchHandler getSearchHandler()
    {
        return searchHandler;
    }


    public void setSearchHandler( SearchHandler searchHandler )
    {
        this.handler.removeMessageHandler( SearchRequest.class );
        this.searchHandler = searchHandler;
        this.searchHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( SearchRequest.class, this.searchHandler );
    }


    public UnbindHandler getUnbindHandler()
    {
        return unbindHandler;
    }


    public void setUnbindHandler( UnbindHandler unbindHandler )
    {
        this.handler.removeMessageHandler( UnbindRequest.class );
        this.unbindHandler = unbindHandler;
        this.unbindHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( UnbindRequest.class, this.unbindHandler );
    }


    public boolean isStarted()
    {
        return started;
    }


    public void setStarted( boolean started )
    {
        this.started = started;
    }


    /**
     * A snickers based BER Decoder factory.
     */
    private static final class ProtocolCodecFactoryImpl implements ProtocolCodecFactory
    {
        final DirectoryService directoryService;


        public ProtocolCodecFactoryImpl( DirectoryService directoryService )
        {
            this.directoryService = directoryService;
        }


        public ProtocolEncoder getEncoder()
        {
            return new Asn1CodecEncoder( new MessageEncoder() );
        }


        public ProtocolDecoder getDecoder()
        {
            return new Asn1CodecDecoder( new MessageDecoder( new BinaryAttributeDetector()
            {
                public boolean isBinary( String id )
                {
                    AttributeTypeRegistry attrRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
                    try
                    {
                        AttributeType type = attrRegistry.lookup( id );
                        return ! type.getSyntax().isHumanReadable();
                    }
                    catch ( Exception e )
                    {
                        return false;
                    }
                }
            }) );
        }
    }
    
    
    Map<IoSession, LdapSession> ldapSessions = new ConcurrentHashMap<IoSession, LdapSession>( 100 );

    
    public LdapSession removeLdapSession( IoSession session )
    {
        LdapSession ldapSession = null; 
        
        synchronized ( ldapSessions )
        {
            ldapSession = ldapSessions.remove( session );
        }
        
        if ( ldapSession != null )
        {
            ldapSession.abandonAllOutstandingRequests();
        }
        
        return ldapSession;
    }
    
    
    public LdapSession getLdapSession( IoSession session )
    {
        return ldapSessions.get( session );
    }
    
    
    private class LdapProtocolHandler extends DemuxingIoHandler
    {
        public void sessionCreated( IoSession session ) throws Exception
        {
            LdapSession ldapSession = new LdapSession( session );
            IoFilterChain filters = session.getFilterChain();
            filters.addLast( "codec", new ProtocolCodecFilter( codecFactory ) );
            
            synchronized( ldapSessions )
            {
                ldapSessions.put( session, ldapSession );
            }
        }


        public void sessionClosed( IoSession session )
        {
            removeLdapSession( session );
        }


        public void messageReceived( IoSession session, Object message ) throws Exception
        {
            // Translate SSLFilter messages into LDAP extended request
            // defined in RFC #2830, 'Lightweight Directory Access Protocol (v3):
            // Extension for Transport Layer Security'.
            // 
            // The RFC specifies the payload should be empty, but we use
            // it to notify the TLS state changes.  This hack should be
            // OK from the viewpointd of security because StartTLS
            // handler should react to only SESSION_UNSECURED message
            // and degrade authentication level to 'anonymous' as specified
            // in the RFC, and this is no threat.

            if ( message == SSLFilter.SESSION_SECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "SECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }
            else if ( message == SSLFilter.SESSION_UNSECURED )
            {
                ExtendedRequest req = new ExtendedRequestImpl( 0 );
                req.setOid( "1.3.6.1.4.1.1466.20037" );
                req.setPayload( "UNSECURED".getBytes( "ISO-8859-1" ) );
                message = req;
            }

            if ( ( ( Request ) message ).getControls().size() > 0 && message instanceof ResultResponseRequest )
            {
                ResultResponseRequest req = ( ResultResponseRequest ) message;
                for ( Control control1 : req.getControls().values() )
                {
                    MutableControl control = ( MutableControl ) control1;
                    if ( control.isCritical() && !supportedControls.contains( control.getID() ) )
                    {
                        ResultResponse resp = req.getResultResponse();
                        resp.getLdapResult().setErrorMessage( "Unsupport critical control: " + control.getID() );
                        resp.getLdapResult().setResultCode( ResultCodeEnum.UNAVAILABLE_CRITICAL_EXTENSION );
                        session.write( resp );
                        return;
                    }
                }
            }

            super.messageReceived( session, message );
        }


        public void exceptionCaught( IoSession session, Throwable cause )
        {
            if ( cause.getCause() instanceof ResponseCarryingMessageException )
            {
                ResponseCarryingMessageException rcme = ( ResponseCarryingMessageException ) cause.getCause();
                session.write( rcme.getResponse() );
                return;
            }
            
            SessionLog.warn( session,
                "Unexpected exception forcing session to close: sending disconnect notice to client.", cause );
            session.write( NoticeOfDisconnect.PROTOCOLERROR );
            removeLdapSession( session );
            session.close();
        }
    }
}
