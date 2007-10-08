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
package org.apache.directory.server.configuration;


import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.jndi.JndiRecordStoreImpl;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.kdc.KerberosServer;
import org.apache.directory.server.kerberos.shared.store.JndiPrincipalStoreImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.server.ldap.support.ssl.LdapsInitializer;
import org.apache.directory.server.ntp.NtpConfiguration;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.common.*;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Apache Directory Server top level.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ApacheDS
{
    public static final String JNDI_KEY = ApacheDS.class.toString();
    public static final int MAX_THREADS_DEFAULT = 32;

    private static final String WINDOWSFILE_ATTR = "windowsFilePath";
    private static final String UNIXFILE_ATTR = "unixFilePath";
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDS.class.getName() );
    private static final String LDIF_FILES_DN = "ou=loadedLdifFiles,ou=configuration,ou=system";
    private static final long DEFAULT_SYNC_PERIOD_MILLIS = 20000;

    private int maxThreads = MAX_THREADS_DEFAULT; 
    private long synchPeriodMillis = DEFAULT_SYNC_PERIOD_MILLIS;
    private boolean enableNetworking = true;
    private Hashtable<String,Object> environment = new Hashtable<String,Object>();

    private File ldifDirectory;
    private final List<LdifLoadFilter> ldifFilters = new ArrayList<LdifLoadFilter>();

    private KdcConfiguration kdcConfiguration = new KdcConfiguration();
    private NtpConfiguration ntpConfiguration = new NtpConfiguration();
    private ChangePasswordConfiguration changePasswordConfiguration = new ChangePasswordConfiguration();
    private LdapConfiguration ldapConfiguration = new LdapConfiguration();
    private LdapConfiguration ldapsConfiguration = new LdapConfiguration();
    private DnsConfiguration dnsConfiguration = new DnsConfiguration();
    private DirectoryService directoryService = new DefaultDirectoryService();

    private IoAcceptor tcpAcceptor;
    protected IoAcceptor udpAcceptor;
    protected ExecutorService ioExecutor;
    protected ExecutorService logicExecutor;
    private boolean ldapsStarted;
    private boolean ldapStarted;
    private KerberosServer tcpKdcServer;
    private KerberosServer udpKdcServer;
    private ChangePasswordServer tcpChangePasswordServer;
    private ChangePasswordServer udpChangePasswordServer;
    private NtpServer tcpNtpServer;
    private NtpServer udpNtpServer;
    private DnsServer tcpDnsServer;
    private DnsServer udpDnsServer;


    public ApacheDS()
    {
        environment.put( JNDI_KEY, this );
        environment.put( Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.toString() );
        environment.put( Context.SECURITY_AUTHENTICATION, "simple" );
        ldapConfiguration.setEnabled( true );

        ByteBuffer.setAllocator( new SimpleByteBufferAllocator() );
        ByteBuffer.setUseDirectBuffers( false );

        ioExecutor = Executors.newCachedThreadPool();
        logicExecutor = Executors.newFixedThreadPool( maxThreads );
        udpAcceptor = new DatagramAcceptor();
        udpAcceptor.getFilterChain().addLast( "executor", new ExecutorFilter( logicExecutor ) );
        tcpAcceptor = new SocketAcceptor( Runtime.getRuntime().availableProcessors(), ioExecutor );
        tcpAcceptor.getFilterChain().addLast( "executor", new ExecutorFilter( logicExecutor ) );
    }


    public void startup() throws NamingException
    {
        loadLdifs();

        if ( ! directoryService.isStarted() )
        {
            directoryService.startup();
        }
        environment.put( JndiPropertyConstants.JNDI_LDAP_ATTRIBUTES_BINARY,
                directoryService.getEnvironment().get( JndiPropertyConstants.JNDI_LDAP_ATTRIBUTES_BINARY ) );        
        
        if ( enableNetworking )
        {
            startLDAP();
            startLDAPS();
            startKerberos();
            startChangePassword();
            startNTP();
            startDNS();
        }
    }


    public boolean isStarted()
    {
        return ldapStarted || ldapsStarted;
    }
    

    public void shutdown() throws NamingException
    {
        if ( ldapStarted )
        {
            stopLDAP0( ldapConfiguration.getIpPort() );
            ldapStarted = false;
        }

        if ( ldapsStarted )
        {
            stopLDAP0( ldapsConfiguration.getIpPort() );
            ldapsStarted = false;
        }

        if ( tcpKdcServer != null )
        {
            tcpKdcServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of KRB5 Service (TCP) complete: " + tcpKdcServer );
            }
            tcpKdcServer = null;
        }

        if ( udpKdcServer != null )
        {
            udpKdcServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of KRB5 Service (UDP) complete: " + udpKdcServer );
            }
            udpKdcServer = null;
        }

        if ( tcpChangePasswordServer != null )
        {
            tcpChangePasswordServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of Change Password Service (TCP) complete: " + tcpChangePasswordServer );
            }
            tcpChangePasswordServer = null;
        }

        if ( udpChangePasswordServer != null )
        {
            udpChangePasswordServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of Change Password Service (UDP) complete: " + udpChangePasswordServer );
            }
            udpChangePasswordServer = null;
        }

        if ( tcpNtpServer != null )
        {
            tcpNtpServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of NTP Service (TCP) complete: " + tcpNtpServer );
            }
            tcpNtpServer = null;
        }

        if ( udpNtpServer != null )
        {
            udpNtpServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of NTP Service (UDP) complete: " + udpNtpServer );
            }
            udpNtpServer = null;
        }

        if ( tcpDnsServer != null )
        {
            tcpDnsServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of DNS Service (TCP) complete: " + tcpDnsServer );
            }
            tcpDnsServer = null;
        }

        if ( udpDnsServer != null )
        {
            udpDnsServer.destroy();
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of DNS Service (UDP) complete: " + udpDnsServer );
            }
            udpDnsServer = null;
        }

        logicExecutor.shutdown();
        for (;;) {
            try {
                if ( logicExecutor.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) )
                {
                    break;
                }
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Failed to terminate logic executor", e );
            }
        }

        ioExecutor.shutdown();
        for (;;) {
            try {
                if ( ioExecutor.awaitTermination( Integer.MAX_VALUE, TimeUnit.SECONDS ) )
                {
                    break;
                }
            }
            catch ( InterruptedException e )
            {
                LOG.error( "Failed to terminate io executor", e );
            }
        }

        directoryService.shutdown();
    }


    public KdcConfiguration getKdcConfiguration()
    {
        return kdcConfiguration;
    }


    public void setKdcConfiguration( KdcConfiguration kdcConfiguration )
    {
        this.kdcConfiguration = kdcConfiguration;
    }


    public NtpConfiguration getNtpConfiguration()
    {
        return ntpConfiguration;
    }


    public void setNtpConfiguration( NtpConfiguration ntpConfiguration )
    {
        this.ntpConfiguration = ntpConfiguration;
    }


    public ChangePasswordConfiguration getChangePasswordConfiguration()
    {
        return changePasswordConfiguration;
    }


    public void setChangePasswordConfiguration( ChangePasswordConfiguration changePasswordConfiguration )
    {
        this.changePasswordConfiguration = changePasswordConfiguration;
    }


    public LdapConfiguration getLdapConfiguration()
    {
        return ldapConfiguration;
    }


    public void setLdapConfiguration( LdapConfiguration ldapConfiguration )
    {
        this.ldapConfiguration = ldapConfiguration;
    }


    public LdapConfiguration getLdapsConfiguration()
    {
        return ldapsConfiguration;
    }


    public void setLdapsConfiguration( LdapConfiguration ldapsConfiguration )
    {
        this.ldapsConfiguration = ldapsConfiguration;
    }


    public DnsConfiguration getDnsConfiguration()
    {
        return dnsConfiguration;
    }


    public void setDnsConfiguration( DnsConfiguration dnsConfiguration )
    {
        this.dnsConfiguration = dnsConfiguration;
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    public int getMaxThreads()
    {
        return maxThreads;
    }


    public void setMaxThreads( int maxThreads )
    {
        this.maxThreads = maxThreads;
        if ( maxThreads < 1 )
        {
            throw new IllegalArgumentException( "Number of max threads should be greater than 0" );
        }
    }


    public long getSynchPeriodMillis()
    {
        return synchPeriodMillis;
    }


    public void setSynchPeriodMillis( long synchPeriodMillis )
    {
        this.synchPeriodMillis = synchPeriodMillis;
    }


    public boolean isEnableNetworking()
    {
        return enableNetworking;
    }


    public void setEnableNetworking( boolean enableNetworking )
    {
        this.enableNetworking = enableNetworking;
    }


    public File getLdifDirectory()
    {
        if ( ldifDirectory == null )
        {
            return null;
        }
        else if ( ldifDirectory.isAbsolute() )
        {
            return this.ldifDirectory;
        }
        else
        {
            return new File( directoryService.getWorkingDirectory().getParent() , ldifDirectory.toString() );
        }
    }


    public void setAllowAnonymousAccess( boolean allowAnonymousAccess )
    {
        this.directoryService.setAllowAnonymousAccess( allowAnonymousAccess );
        this.ldapConfiguration.setAllowAnonymousAccess( allowAnonymousAccess );
        this.ldapsConfiguration.setAllowAnonymousAccess( allowAnonymousAccess );
    }


    public void setLdifDirectory( File ldifDirectory )
    {
        this.ldifDirectory = ldifDirectory;
    }


    public List<LdifLoadFilter> getLdifFilters()
    {
        return new ArrayList<LdifLoadFilter>( ldifFilters );
    }


    protected void setLdifFilters( List<LdifLoadFilter> filters )
    {
        this.ldifFilters.clear();
        this.ldifFilters.addAll( filters );
    }


    // ----------------------------------------------------------------------
    // From ServerContextFactory: presently in intermediate step but these
    // methods will be moved to the appropriate protocol service eventually.
    // This is here simply to start to remove the JNDI dependency then further
    // refactoring will be needed to place these where they belong.
    // ----------------------------------------------------------------------

    private void ensureLdifFileBase( DirContext root )
    {
        Attributes entry = new AttributesImpl( SchemaConstants.OU_AT, "loadedLdifFiles", true );
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );

        try
        {
            root.createSubcontext( LDIF_FILES_DN, entry );
            LOG.info( "Creating " + LDIF_FILES_DN );
        }
        catch ( NamingException e )
        {
            LOG.info( LDIF_FILES_DN + " exists" );
        }
    }


    private String buildProtectedFileEntry( File ldif )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( File.separatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR );
        buf.append( "=" );

        buf.append( StringTools.dumpHexPairs( StringTools.getBytesUtf8( getCanonical( ldif ) ) ) );

        buf.append( "," );
        buf.append( LDIF_FILES_DN );

        return buf.toString();
    }

    private void addFileEntry( DirContext root, File ldif ) throws NamingException
    {
		String rdnAttr = File.separatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR;
        String oc = File.separatorChar == '\\' ? ApacheSchemaConstants.WINDOWS_FILE_OC : ApacheSchemaConstants.UNIX_FILE_OC;

        Attributes entry = new AttributesImpl( rdnAttr, getCanonical( ldif ), true );
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( oc );
        root.createSubcontext( buildProtectedFileEntry( ldif ), entry );
    }


    private Attributes getLdifFileEntry( DirContext root, File ldif )
    {
        try
        {
            return root.getAttributes( buildProtectedFileEntry( ldif ), new String[]
                { SchemaConstants.CREATE_TIMESTAMP_AT } );
        }
        catch ( NamingException e )
        {
            return null;
        }
    }


    private String getCanonical( File file )
    {
        String canonical;

        try
        {
            canonical = file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            LOG.error( "could not get canonical path", e );
            return null;
        }

        return StringUtils.replace( canonical, "\\", "\\\\" );
    }


    private void loadLdifs() throws NamingException
    {
        // LOG and bail if property not set
        if ( ldifDirectory == null )
        {
            LOG.info( "LDIF load directory not specified.  No LDIF files will be loaded." );
            return;
        }

        // LOG and bail if LDIF directory does not exists
        if ( ! ldifDirectory.exists() )
        {
            LOG.warn( "LDIF load directory '" + getCanonical( ldifDirectory )
                + "' does not exist.  No LDIF files will be loaded." );
            return;
        }

        // get an initial context to the rootDSE for creating the LDIF entries
        //noinspection unchecked
        Hashtable<String, Object> env = ( Hashtable<String, Object> ) environment.clone();
        env.put( Context.PROVIDER_URL, "" );
        env.put( ApacheDS.JNDI_KEY, this );
        env.put( Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.getName() );
        DirContext root = new InitialDirContext( env );

        // make sure the configuration area for loaded ldif files is present
        ensureLdifFileBase( root );

        // if ldif directory is a file try to load it
        if ( ! ldifDirectory.isDirectory() )
        {
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "LDIF load directory '" + getCanonical( ldifDirectory )
                    + "' is a file.  Will attempt to load as LDIF." );
            }

            Attributes fileEntry = getLdifFileEntry( root, ldifDirectory );

            if ( fileEntry != null )
            {
                String time = ( String ) fileEntry.get( SchemaConstants.CREATE_TIMESTAMP_AT ).get();

                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Load of LDIF file '" + getCanonical( ldifDirectory )
                        + "' skipped.  It has already been loaded on " + time + "." );
                }

                return;
            }

            LdifFileLoader loader = new LdifFileLoader( root, ldifDirectory, ldifFilters );
            loader.execute();

            addFileEntry( root, ldifDirectory );
            return;
        }

        // get all the ldif files within the directory (should be sorted alphabetically)
        File[] ldifFiles = ldifDirectory.listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                boolean isLdif = pathname.getName().toLowerCase().endsWith( ".ldif" );
                return pathname.isFile() && pathname.canRead() && isLdif;
            }
        } );

        // LOG and bail if we could not find any LDIF files
        if ( ldifFiles == null || ldifFiles.length == 0 )
        {
            LOG.warn( "LDIF load directory '" + getCanonical( ldifDirectory )
                + "' does not contain any LDIF files.  No LDIF files will be loaded." );
            return;
        }

        // load all the ldif files and load each one that is loaded
        for ( File ldifFile : ldifFiles )
        {
            Attributes fileEntry = getLdifFileEntry( root, ldifFile );

            if ( fileEntry != null )
            {
                String time = ( String ) fileEntry.get( SchemaConstants.CREATE_TIMESTAMP_AT ).get();
                LOG.info( "Load of LDIF file '" + getCanonical( ldifFile )
                        + "' skipped.  It has already been loaded on " + time + "." );
                continue;
            }

            LdifFileLoader loader = new LdifFileLoader( root, ldifFile, ldifFilters );
            int count = loader.execute();
            LOG.info( "Loaded " + count + " entries from LDIF file '" + getCanonical( ldifFile ) + "'" );
            addFileEntry( root, ldifFile );
        }
    }


    /**
     * Starts up the LDAP protocol provider to service LDAP requests
     *
     * @throws NamingException if there are problems starting the LDAP provider
     */
    private void startLDAP() throws NamingException
    {
        // Skip if disabled
        if ( ! ldapConfiguration.isEnabled() )
        {
            return;
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        startLDAP0( ldapConfiguration, ldapConfiguration.getIpPort(), chain );
    }


    /**
     * Starts up the LDAPS protocol provider to service LDAPS requests
     *
     * @throws NamingException if there are problems starting the LDAPS provider
     */
    private void startLDAPS() throws NamingException
    {
        // Skip if disabled
        if ( !( ldapsConfiguration.isEnabled() && ldapsConfiguration.isEnableLdaps() ) )
        {
            return;
        }

        char[] certPasswordChars = ldapsConfiguration.getLdapsCertificatePassword().toCharArray();
        String storePath = ldapsConfiguration.getLdapsCertificateFile().getPath();

        IoFilterChainBuilder chain = LdapsInitializer.init( certPasswordChars, storePath );
        ldapsStarted = true;

        startLDAP0( ldapsConfiguration, ldapsConfiguration.getIpPort(), chain );
    }


    private void startLDAP0( LdapConfiguration ldapConfig, int port, IoFilterChainBuilder chainBuilder )
        throws LdapNamingException, LdapConfigurationException
    {
        // Register all extended operation handlers.
        LdapProtocolProvider protocolProvider = new LdapProtocolProvider( directoryService, ldapConfig );

        for ( ExtendedOperationHandler h : ldapConfig.getExtendedOperationHandlers() )
        {
            protocolProvider.addExtendedOperationHandler( h );
            LOG.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapProvider( protocolProvider );
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

            tcpAcceptor.bind( new InetSocketAddress( port ), protocolProvider.getHandler(), acceptorCfg );
            ldapStarted = true;

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


    private void startKerberos()
    {
        // Skip if disabled
        if ( ! kdcConfiguration.isEnabled() )
        {
            return;
        }

        try
        {
            PrincipalStore kdcStore = new JndiPrincipalStoreImpl( kdcConfiguration, directoryService );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( ThreadModel.MANUAL );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setThreadModel( ThreadModel.MANUAL );

            tcpKdcServer = new KerberosServer( kdcConfiguration, tcpAcceptor, tcpConfig, kdcStore );
            udpKdcServer = new KerberosServer( kdcConfiguration, udpAcceptor, udpConfig, kdcStore );
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to start the Kerberos service", t );
        }
    }


    private void startChangePassword()
    {
        // Skip if disabled
        if ( ! changePasswordConfiguration.isEnabled() )
        {
            return;
        }

        try
        {
            PrincipalStore store = new JndiPrincipalStoreImpl( changePasswordConfiguration, directoryService );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( ThreadModel.MANUAL );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setThreadModel( ThreadModel.MANUAL );

            tcpChangePasswordServer = new ChangePasswordServer( changePasswordConfiguration,
                    tcpAcceptor, tcpConfig, store );
            udpChangePasswordServer = new ChangePasswordServer( changePasswordConfiguration,
                    udpAcceptor, udpConfig, store );
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to start the Change Password service", t );
        }
    }


    private void startNTP()
    {
        // Skip if disabled
        if ( ! ntpConfiguration.isEnabled() )
        {
            return;
        }

        try
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( ThreadModel.MANUAL );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setThreadModel( ThreadModel.MANUAL );

            tcpNtpServer = new NtpServer( ntpConfiguration, tcpAcceptor, tcpConfig );
            udpNtpServer = new NtpServer( ntpConfiguration, udpAcceptor, udpConfig );
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to start the NTP service", t );
        }
    }


    private void startDNS()
    {
        // Skip if disabled
        if ( ! dnsConfiguration.isEnabled() )
        {
            return;
        }

        try
        {
            RecordStore store = new JndiRecordStoreImpl( dnsConfiguration, directoryService );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( ThreadModel.MANUAL );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setThreadModel( ThreadModel.MANUAL );

            tcpDnsServer = new DnsServer( dnsConfiguration, tcpAcceptor, tcpConfig, store );
            udpDnsServer = new DnsServer( dnsConfiguration, udpAcceptor, udpConfig, store );
        }
        catch ( Throwable t )
        {
            LOG.error( "Failed to start the DNS service", t );
        }
    }


    private void stopLDAP0( int port )
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
                sessions = new ArrayList<IoSession>( tcpAcceptor.getManagedSessions( new InetSocketAddress( port ) ) );
            }
            catch ( IllegalArgumentException e )
            {
                LOG.warn( "Seems like the LDAP service (" + port + ") has already been unbound." );
                return;
            }

            tcpAcceptor.unbind( new InetSocketAddress( port ) );

            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Unbind of an LDAP service (" + port + ") is complete." );
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


    public Hashtable<String, Object> getEnvironment()
    {
        return environment;
    }


    public void setEnvironment( Hashtable<String, Object> environment )
    {
        this.environment = environment;
    }
}
