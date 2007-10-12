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


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.Control;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.ldap.support.AbandonHandler;
import org.apache.directory.server.ldap.support.AddHandler;
import org.apache.directory.server.ldap.support.BindHandler;
import org.apache.directory.server.ldap.support.CompareHandler;
import org.apache.directory.server.ldap.support.DefaultAbandonHandler;
import org.apache.directory.server.ldap.support.DefaultAddHandler;
import org.apache.directory.server.ldap.support.DefaultBindHandler;
import org.apache.directory.server.ldap.support.DefaultCompareHandler;
import org.apache.directory.server.ldap.support.DefaultDeleteHandler;
import org.apache.directory.server.ldap.support.DefaultExtendedHandler;
import org.apache.directory.server.ldap.support.DefaultModifyDnHandler;
import org.apache.directory.server.ldap.support.DefaultModifyHandler;
import org.apache.directory.server.ldap.support.DefaultSearchHandler;
import org.apache.directory.server.ldap.support.DefaultUnbindHandler;
import org.apache.directory.server.ldap.support.DeleteHandler;
import org.apache.directory.server.ldap.support.ExtendedHandler;
import org.apache.directory.server.ldap.support.ModifyDnHandler;
import org.apache.directory.server.ldap.support.ModifyHandler;
import org.apache.directory.server.ldap.support.SearchHandler;
import org.apache.directory.server.ldap.support.UnbindHandler;
import org.apache.directory.server.ldap.support.ssl.LdapsInitializer;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.asn1.codec.Asn1CodecDecoder;
import org.apache.directory.shared.asn1.codec.Asn1CodecEncoder;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
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
import org.apache.mina.transport.socket.nio.SocketAcceptor;
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
public class LdapServer extends ServiceConfiguration
{
    @SuppressWarnings ( { "UnusedDeclaration" } )
    private static final long serialVersionUID = 3757127143811666817L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdapServer.class.getName() );

    /** The default maximum size limit. */
    private static final int MAX_SIZE_LIMIT_DEFAULT = 100;

    /** The default maximum time limit. */
    private static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    /**
     * The default service pid.
     */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ldap";

    /**
     * The default service name.
     */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS LDAP Service";

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 389;

    /** the constant service name of this ldap protocol provider **/
    public static final String SERVICE_NAME = "ldap";

    /** a set of supported controls */
    private Set<String> supportedControls;

    /** The maximum size limit. */
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; // set to default value

    /** The maximum time limit. */
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT; // set to default value (milliseconds)

    /** Whether LDAPS is enabled. */
    private boolean enableLdaps;

    /** Whether to allow anonymous access. */
    private boolean allowAnonymousAccess = true; // allow by default

    /** The path to the certificate file. */
    private File ldapsCertificateFile = new File( "server-work" + File.separator + "certificates" + File.separator
        + "server.cert" );

    /** The certificate password. */
    private String ldapsCertificatePassword = "changeit";

    /** The extended operation handlers. */
    private final Collection<ExtendedOperationHandler> extendedOperationHandlers = new ArrayList<ExtendedOperationHandler>();

    /** The supported authentication mechanisms. */
    private Set<String> supportedMechanisms;

    /** The name of this host, validated during SASL negotiation. */
    private String saslHost = "ldap.example.com";

    /** The service principal, used by GSSAPI. */
    private String saslPrincipal = "ldap/ldap.example.com@EXAMPLE.COM";

    /** The quality of protection (QoP), used by DIGEST-MD5 and GSSAPI. */
    private List<String> saslQop;

    /** The list of realms serviced by this host. */
    private List<String> saslRealms;

    private AbandonHandler abandonHandler;
    private AddHandler addHandler;
    private BindHandler bindHandler;
    private CompareHandler compareHandler;
    private DeleteHandler deleteHandler;
    private ExtendedHandler extendedHandler;
    private ModifyHandler modifyHandler;
    private ModifyDnHandler modifyDnHandler;
    private SearchHandler searchHandler;
    private UnbindHandler unbindHandler;


    private SessionRegistry registry;

    /** the underlying provider codec factory */
    private ProtocolCodecFactory codecFactory;

    /** the MINA protocol handler */
    private final LdapProtocolHandler handler = new LdapProtocolHandler();

    private final SocketAcceptor socketAcceptor;

    private final DirectoryService directoryService;

    /** tracks state of the server */
    private boolean started;


    /**
     * Creates an LDAP protocol provider.
     *
     * @param socketAcceptor the mina socket acceptor wrapper
     * @param directoryService
     */
    public LdapServer( SocketAcceptor socketAcceptor, DirectoryService directoryService )
    {
        this.socketAcceptor = socketAcceptor;
        this.directoryService = directoryService;
        this.codecFactory = new ProtocolCodecFactoryImpl( directoryService );
        Hashtable<String,Object> copy = new Hashtable<String,Object>();
        copy.put( Context.PROVIDER_URL, "" );
        copy.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        copy.put( DirectoryService.JNDI_KEY, directoryService );
        this.registry = new SessionRegistry( this, copy );

        super.setIpPort( IP_PORT_DEFAULT );
        super.setEnabled( true );
        super.setServicePid( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );

        supportedMechanisms = new HashSet<String>();
        supportedMechanisms.add( "SIMPLE" );
        supportedMechanisms.add( "CRAM-MD5" );
        supportedMechanisms.add( "DIGEST-MD5" );
        supportedMechanisms.add( "GSSAPI" );

        saslQop = new ArrayList<String>();
        saslQop.add( "auth" );
        saslQop.add( "auth-int" );
        saslQop.add( "auth-conf" );

        saslRealms = new ArrayList<String>();
        saslRealms.add( "example.com" );

        this.supportedControls = new HashSet<String>();
        this.supportedControls.add( PersistentSearchControl.CONTROL_OID );
        this.supportedControls.add( EntryChangeControl.CONTROL_OID );
        this.supportedControls.add( SubentriesControl.CONTROL_OID );
        this.supportedControls.add( ManageDsaITControl.CONTROL_OID );
        this.supportedControls.add( CascadeControl.CONTROL_OID );

        setAbandonHandler( new DefaultAbandonHandler() );
        setAddHandler( new DefaultAddHandler() );
        setBindHandler( new DefaultBindHandler() );
        setCompareHandler( new DefaultCompareHandler() );
        setDeleteHandler( new DefaultDeleteHandler() );
        setExtendedHandler( new DefaultExtendedHandler() );
        setModifyHandler( new DefaultModifyHandler() );
        setModifyDnHandler( new DefaultModifyDnHandler() );
        setSearchHandler( new DefaultSearchHandler() );
        setUnbindHandler( new DefaultUnbindHandler() );
    }


    /**
     * @org.apache.xbean.InitMethod
     * @throws IOException if we cannot bind to the specified port
     * @throws NamingException if the LDAP server cannot be started
     */
    public void start() throws NamingException, IOException
    {
        if ( ! isEnabled() )
        {
            return;
        }

        IoFilterChainBuilder chain;
        if ( isEnableLdaps() )
        {
            char[] certPasswordChars = getLdapsCertificatePassword().toCharArray();
            String storePath = getLdapsCertificateFile().getPath();
            chain = LdapsInitializer.init( certPasswordChars, storePath );
        }
        else
        {
            chain = new DefaultIoFilterChainBuilder();
        }

        startLDAP0( getIpPort(), chain );
        started = true;
    }


    /**
     * @org.apache.xbean.DestroyMethod
     */
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
                        socketAcceptor.getManagedSessions( new InetSocketAddress( getIpPort() ) ) );
            }
            catch ( IllegalArgumentException e )
            {
                LOG.warn( "Seems like the LDAP service (" + getIpPort() + ") has already been unbound." );
                return;
            }

            socketAcceptor.unbind( new InetSocketAddress( getIpPort() ) );

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
        throws LdapNamingException, LdapConfigurationException
    {
        for ( ExtendedOperationHandler h : getExtendedOperationHandlers() )
        {
            addExtendedOperationHandler( h );
            LOG.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapProvider( this );
            PartitionNexus nexus = directoryService.getPartitionNexus();
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        try
        {
            SocketAcceptorConfig acceptorCfg = new SocketAcceptorConfig();

            // Disable the disconnection of the clients on unbind
            acceptorCfg.setDisconnectOnUnbind( false );
            acceptorCfg.setReuseAddress( true );
            acceptorCfg.setFilterChainBuilder( chainBuilder );
            acceptorCfg.setThreadModel( ThreadModel.MANUAL );

            acceptorCfg.getSessionConfig().setTcpNoDelay( true );

            socketAcceptor.bind( new InetSocketAddress( port ), getHandler(), acceptorCfg );
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
     */
    public void addExtendedOperationHandler( ExtendedOperationHandler eoh )
    {
        extendedHandler.addHandler( eoh );
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
        extendedHandler.removeHandler( oid );
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
        return extendedHandler.getHandler( oid );
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
     * Returns the path of the X509 (or JKS) certificate file for LDAPS.
     * The default value is <tt>"&lt;WORKDIR&gt;/certificates/server.cert"</tt>.
     *
     * @return The LDAPS certificate file.
     */
    public File getLdapsCertificateFile()
    {
        return ldapsCertificateFile;
    }


    /**
     * Sets the path of the SunX509 certificate file (either PKCS12 or JKS format)
     * for LDAPS.
     *
     * @param ldapsCertificateFile The path to the SunX509 certificate.
     */
    public void setLdapsCertificateFile( File ldapsCertificateFile )
    {
        if ( ldapsCertificateFile == null )
        {
            throw new ServiceConfigurationException( "LdapsCertificateFile cannot be null." );
        }
        this.ldapsCertificateFile = ldapsCertificateFile;
    }


    /**
     * Returns the password which is used to load the the SunX509 certificate file
     * (either PKCS12 or JKS format).
     * The default value is <tt>"changeit"</tt>.  This is the same value with what
     * <a href="http://jakarta.apache.org/tomcat/">Apache Jakarta Tomcat</a> uses by
     * default.
     *
     * @return The LDAPS certificate password.
     */
    public String getLdapsCertificatePassword()
    {
        return ldapsCertificatePassword;
    }


    /**
     * Sets the password which is used to load the LDAPS certificate file.
     *
     * @param ldapsCertificatePassword The certificate password.
     */
    public void setLdapsCertificatePassword( String ldapsCertificatePassword )
    {
        if ( ldapsCertificatePassword == null )
        {
            throw new ServiceConfigurationException( "LdapsCertificatePassword cannot be null." );
        }
        this.ldapsCertificatePassword = ldapsCertificatePassword;
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
     * Returns the desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     *
     * @return The desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public List<String> getSaslQop()
    {
        return saslQop;
    }


    /**
     * Sets the desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     *
     * @org.apache.xbean.Property nestedType="java.lang.String"
     *
     * @param saslQop The desired quality-of-protection, used by DIGEST-MD5 and GSSAPI.
     */
    public void setSaslQop( List<String> saslQop )
    {
        this.saslQop = saslQop;
    }


    /**
     * Returns the realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     *
     * @return The realms serviced by this SASL host, used by DIGEST-MD5 and GSSAPI.
     */
    public List getSaslRealms()
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
     * Returns the list of supported authentication mechanisms.
     *
     * @return The list of supported authentication mechanisms.
     */
    public Set<String> getSupportedMechanisms()
    {
        return supportedMechanisms;
    }


    /**
     * Sets the list of supported authentication mechanisms.
     *
     * @org.apache.xbean.Property propertyEditor="ListEditor" nestedType="java.lang.String"
     *
     * @param supportedMechanisms The list of supported authentication mechanisms.
     */
    public void setSupportedMechanisms( Set<String> supportedMechanisms )
    {
        this.supportedMechanisms = supportedMechanisms;
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public Set<String> getSupportedControls()
    {
        return supportedControls;
    }


    public void setSupportedControls( Set<String> supportedControls )
    {
        this.supportedControls = supportedControls;
    }


    public AbandonHandler getAbandonHandler()
    {
        return abandonHandler;
    }


    public void setAbandonHandler( AbandonHandler abandonHandler )
    {
        this.handler.removeMessageHandler( AbandonRequest.class );
        this.abandonHandler = abandonHandler;
        this.abandonHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( AbandonRequest.class, this.abandonHandler );
    }


    public AddHandler getAddHandler()
    {
        return addHandler;
    }


    public void setAddHandler( AddHandler addHandler )
    {
        this.handler.removeMessageHandler( AddRequest.class );
        this.addHandler = addHandler;
        this.addHandler.setProtocolProvider( this );
        //noinspection unchecked
        this.handler.addMessageHandler( AddRequest.class, this.addHandler );
    }


    public BindHandler getBindHandler()
    {
        return bindHandler;
    }


    public void setBindHandler( BindHandler bindHandler )
    {
        this.handler.removeMessageHandler( BindRequest.class );
        this.bindHandler = bindHandler;
        this.bindHandler.setProtocolProvider( this );
            this.bindHandler.setDirectoryService( directoryService );
        //noinspection unchecked
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


    public SessionRegistry getRegistry()
    {
        return registry;
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
                    catch ( NamingException e )
                    {
                        return false;
                    }
                }
            }) );
        }
    }

    private class LdapProtocolHandler extends DemuxingIoHandler
    {
        public void sessionCreated( IoSession session ) throws Exception
        {
            session.setAttribute( LdapServer.class.toString(), this );
            IoFilterChain filters = session.getFilterChain();
            filters.addLast( "codec", new ProtocolCodecFilter( codecFactory ) );
        }


        public void sessionClosed( IoSession session )
        {
            registry.remove( session );
        }


        public void messageReceived( IoSession session, Object message ) throws Exception
        {
            // Translate SSLFilter messages into LDAP extended request
            // defined in RFC #2830, 'Lightweight Directory Access Protocol (v3):
            // Extension for Transport Layer Security'.
            // 
            // The RFC specifies the payload should be empty, but we use
            // it to notify the TLS state changes.  This hack should be
            // OK from the viewpoint of security because StartTLS
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
            registry.remove( session );
            session.close();
        }
    }
}
