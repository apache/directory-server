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
package org.apache.directory.server.jndi;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.store.JndiRecordStoreImpl;
import org.apache.directory.server.dns.store.RecordStore;
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
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


/**
 * Adds additional bootstrapping for server socket listeners when firing
 * up the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 * @see javax.naming.spi.InitialContextFactory
 */
public class ServerContextFactory extends CoreContextFactory
{
    private static final Logger log = LoggerFactory.getLogger( ServerContextFactory.class.getName() );
    private static final String LDIF_FILES_DN = "ou=loadedLdifFiles,ou=configuration,ou=system";

    protected static IoAcceptor tcpAcceptor;
    protected static IoAcceptor udpAcceptor;
    protected static ThreadPoolExecutor threadPoolExecutor;
    protected static ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance( "ApacheDS" );

    private static boolean ldapStarted;
    private static boolean ldapsStarted;
    private static KerberosServer tcpKdcServer;
    private static KerberosServer udpKdcServer;
    private static ChangePasswordServer tcpChangePasswordServer;
    private static ChangePasswordServer udpChangePasswordServer;
    private static NtpServer tcpNtpServer;
    private static NtpServer udpNtpServer;
    private static DnsServer udpDnsServer;
    private DirectoryService directoryService;


    public void beforeStartup( DirectoryService service )
    {
        int maxThreads = service.getConfiguration().getStartupConfiguration().getMaxThreads();
        threadPoolExecutor = new ThreadPoolExecutor( maxThreads, maxThreads, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue() );
        threadModel.setExecutor( threadPoolExecutor );

        udpAcceptor = new DatagramAcceptor();
        tcpAcceptor = new SocketAcceptor();

        this.directoryService = service;
    }


    public void afterShutdown( DirectoryService service )
    {
        ServerStartupConfiguration cfg = ( ServerStartupConfiguration ) service.getConfiguration()
            .getStartupConfiguration();

        LdapConfiguration ldapCfg = cfg.getLdapConfiguration();
        LdapConfiguration ldapsCfg = cfg.getLdapsConfiguration();

        if ( ldapStarted )
        {
            stopLDAP0( ldapCfg.getIpPort() );
            ldapStarted = false;
        }

        if ( ldapsStarted )
        {
            stopLDAP0( ldapsCfg.getIpPort() );
            ldapsStarted = false;
        }

        if ( tcpKdcServer != null )
        {
            tcpKdcServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of KRB5 Service (TCP) complete: " + tcpKdcServer );
            }
            tcpKdcServer = null;
        }

        if ( udpKdcServer != null )
        {
            udpKdcServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of KRB5 Service (UDP) complete: " + udpKdcServer );
            }
            udpKdcServer = null;
        }

        if ( tcpChangePasswordServer != null )
        {
            tcpChangePasswordServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of Change Password Service (TCP) complete: " + tcpChangePasswordServer );
            }
            tcpChangePasswordServer = null;
        }

        if ( udpChangePasswordServer != null )
        {
            udpChangePasswordServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of Change Password Service (UDP) complete: " + udpChangePasswordServer );
            }
            udpChangePasswordServer = null;
        }

        if ( tcpNtpServer != null )
        {
            tcpNtpServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of NTP Service (TCP) complete: " + tcpNtpServer );
            }
            tcpNtpServer = null;
        }

        if ( udpNtpServer != null )
        {
            udpNtpServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of NTP Service complete: " + udpNtpServer );
            }
            udpNtpServer = null;
        }

        if ( udpDnsServer != null )
        {
            udpDnsServer.destroy();
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of DNS Service complete: " + udpDnsServer );
            }
            udpDnsServer = null;
        }
    }


    public void afterStartup( DirectoryService service ) throws NamingException
    {
        ServerStartupConfiguration cfg = ( ServerStartupConfiguration ) service.getConfiguration()
            .getStartupConfiguration();
        Hashtable env = service.getConfiguration().getEnvironment();
        LdapConfiguration ldapCfg = cfg.getLdapConfiguration();
        LdapConfiguration ldapsCfg = cfg.getLdapsConfiguration();

        if ( !cfg.isAllowAnonymousAccess() )
        {
            ldapCfg.setAllowAnonymousAccess( false );
            ldapsCfg.setAllowAnonymousAccess( false );
        }

        loadLdifs( service );

        if ( cfg.isEnableNetworking() )
        {
            startLDAP( ldapCfg, env );
            startLDAPS( ldapsCfg, env );
            startKerberos( cfg.getKdcConfiguration() );
            startChangePassword( cfg.getChangePasswordConfiguration() );
            startNTP( cfg.getNtpConfiguration() );
            startDNS( cfg.getDnsConfiguration() );
        }
    }


    private void ensureLdifFileBase( DirContext root )
    {
        Attributes entry = new AttributesImpl( "ou", "loadedLdifFiles", true );
        entry.put( "objectClass", "top" );
        entry.get( "objectClass" ).add( "organizationalUnit" );
        try
        {
            root.createSubcontext( LDIF_FILES_DN, entry );
            log.info( "Creating " + LDIF_FILES_DN );
        }
        catch ( NamingException e )
        {
            log.info( LDIF_FILES_DN + " exists" );
        }
    }

    private final static String WINDOWSFILE_ATTR = "windowsFilePath";
    private final static String UNIXFILE_ATTR = "unixFilePath";
    private final static String WINDOWSFILE_OC = "windowsFile";
    private final static String UNIXFILE_OC = "unixFile";


    private void addFileEntry( DirContext root, File ldif ) throws NamingException
    {
        String rdnAttr = File.separatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR;
        String oc = File.separatorChar == '\\' ? WINDOWSFILE_OC : UNIXFILE_OC;
        StringBuffer buf = new StringBuffer();
        buf.append( rdnAttr );
        buf.append( "=" );
        buf.append( getCanonical( ldif ) );
        buf.append( "," );
        buf.append( LDIF_FILES_DN );

        Attributes entry = new AttributesImpl( rdnAttr, getCanonical( ldif ), true );
        entry.put( "objectClass", "top" );
        entry.get( "objectClass" ).add( oc );
        root.createSubcontext( buf.toString(), entry );
    }


    private Attributes getLdifFileEntry( DirContext root, File ldif )
    {
        String rdnAttr = File.separatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR;
        StringBuffer buf = new StringBuffer();
        buf.append( rdnAttr );
        buf.append( "=" );
        buf.append( getCanonical( ldif ) );
        buf.append( "," );
        buf.append( LDIF_FILES_DN );

        try
        {
            return root.getAttributes( buf.toString(), new String[]
                { "createTimestamp" } );
        }
        catch ( NamingException e )
        {
            return null;
        }
    }


    private String getCanonical( File file )
    {
        String canonical = null;
        try
        {
            canonical = file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            log.error( "could not get canonical path", e );
            return null;
        }

        return StringUtils.replace( canonical, "\\", "\\\\" );
    }


    private void loadLdifs( DirectoryService service ) throws NamingException
    {
        ServerStartupConfiguration cfg = ( ServerStartupConfiguration ) service.getConfiguration()
            .getStartupConfiguration();

        // log and bail if property not set
        if ( cfg.getLdifDirectory() == null )
        {
            log.info( "LDIF load directory not specified.  No LDIF files will be loaded." );
            return;
        }

        // log and bail if LDIF directory does not exists
        if ( !cfg.getLdifDirectory().exists() )
        {
            log.warn( "LDIF load directory '" + getCanonical( cfg.getLdifDirectory() )
                + "' does not exist.  No LDIF files will be loaded." );
            return;
        }

        // get an initial context to the rootDSE for creating the LDIF entries
        Hashtable env = ( Hashtable ) service.getConfiguration().getEnvironment().clone();
        env.put( Context.PROVIDER_URL, "" );
        DirContext root = ( DirContext ) this.getInitialContext( env );

        // make sure the configuration area for loaded ldif files is present
        ensureLdifFileBase( root );

        // if ldif directory is a file try to load it
        if ( !cfg.getLdifDirectory().isDirectory() )
        {
            log.info( "LDIF load directory '" + getCanonical( cfg.getLdifDirectory() )
                + "' is a file.  Will attempt to load as LDIF." );
            Attributes fileEntry = getLdifFileEntry( root, cfg.getLdifDirectory() );
            if ( fileEntry != null )
            {
                String time = ( String ) fileEntry.get( "createTimestamp" ).get();
                log.info( "Load of LDIF file '" + getCanonical( cfg.getLdifDirectory() )
                    + "' skipped.  It has already been loaded on " + time + "." );
                return;
            }
            LdifFileLoader loader = new LdifFileLoader( root, cfg.getLdifDirectory(), cfg.getLdifFilters() );
            loader.execute();

            addFileEntry( root, cfg.getLdifDirectory() );
            return;
        }

        // get all the ldif files within the directory (should be sorted alphabetically)
        File[] ldifFiles = cfg.getLdifDirectory().listFiles( new FileFilter()
        {
            public boolean accept( File pathname )
            {
                boolean isLdif = pathname.getName().toLowerCase().endsWith( ".ldif" );
                return pathname.isFile() && pathname.canRead() && isLdif;
            }
        } );

        // log and bail if we could not find any LDIF files
        if ( ldifFiles == null || ldifFiles.length == 0 )
        {
            log.warn( "LDIF load directory '" + getCanonical( cfg.getLdifDirectory() )
                + "' does not contain any LDIF files.  No LDIF files will be loaded." );
            return;
        }

        // load all the ldif files and load each one that is loaded
        for ( int ii = 0; ii < ldifFiles.length; ii++ )
        {
            Attributes fileEntry = getLdifFileEntry( root, ldifFiles[ii] );
            if ( fileEntry != null )
            {
                String time = ( String ) fileEntry.get( "createTimestamp" ).get();
                log.info( "Load of LDIF file '" + getCanonical( ldifFiles[ii] )
                    + "' skipped.  It has already been loaded on " + time + "." );
                continue;
            }
            LdifFileLoader loader = new LdifFileLoader( root, ldifFiles[ii], cfg.getLdifFilters() );
            int count = loader.execute();
            log.info( "Loaded " + count + " entries from LDIF file '" + getCanonical( ldifFiles[ii] ) + "'" );
            if ( fileEntry == null )
            {
                addFileEntry( root, ldifFiles[ii] );
            }
        }
    }


    /**
     * Starts up the LDAP protocol provider to service LDAP requests
     *
     * @throws NamingException if there are problems starting the LDAP provider
     */
    private void startLDAP( LdapConfiguration ldapConfig, Hashtable env ) throws NamingException
    {
        // Skip if disabled
        if ( !ldapConfig.isEnabled() )
        {
            return;
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();

        startLDAP0( ldapConfig, env, ldapConfig.getIpPort(), chain );
    }


    /**
     * Starts up the LDAPS protocol provider to service LDAPS requests
     *
     * @throws NamingException if there are problems starting the LDAPS provider
     */
    private void startLDAPS( LdapConfiguration ldapsConfig, Hashtable env ) throws NamingException
    {
        // Skip if disabled
        if ( !( ldapsConfig.isEnabled() && ldapsConfig.isEnableLdaps() ) )
        {
            return;
        }

        char[] certPasswordChars = ldapsConfig.getLdapsCertificatePassword().toCharArray();
        String storePath = ldapsConfig.getLdapsCertificateFile().getPath();

        IoFilterChainBuilder chain = LdapsInitializer.init( certPasswordChars, storePath );
        ldapsStarted = true;

        startLDAP0( ldapsConfig, env, ldapsConfig.getIpPort(), chain );
    }


    private void startLDAP0( LdapConfiguration ldapConfig, Hashtable env, int port, IoFilterChainBuilder chainBuilder )
        throws LdapNamingException, LdapConfigurationException
    {
        // Register all extended operation handlers.
        LdapProtocolProvider protocolProvider = new LdapProtocolProvider( ldapConfig, ( Hashtable ) env.clone() );

        for ( Iterator i = ldapConfig.getExtendedOperationHandlers().iterator(); i.hasNext(); )
        {
            ExtendedOperationHandler h = ( ExtendedOperationHandler ) i.next();
            protocolProvider.addExtendedOperationHandler( h );
            log.info( "Added Extended Request Handler: " + h.getOid() );
            h.setLdapProvider( protocolProvider );
            PartitionNexus nexus = directoryService.getConfiguration().getPartitionNexus();
            nexus.registerSupportedExtensions( h.getExtensionOids() );
        }

        try
        {
            // Disable the disconnection of the clients on unbind
            SocketAcceptorConfig acceptorCfg = new SocketAcceptorConfig();
            acceptorCfg.setDisconnectOnUnbind( false );
            acceptorCfg.setReuseAddress( true );
            acceptorCfg.setFilterChainBuilder( chainBuilder );
            acceptorCfg.setThreadModel( threadModel );

            ( ( SocketSessionConfig ) ( acceptorCfg.getSessionConfig() ) ).setTcpNoDelay( true );

            tcpAcceptor.bind( new InetSocketAddress( port ), protocolProvider.getHandler(), acceptorCfg );
            ldapStarted = true;

            if ( log.isInfoEnabled() )
            {
                log.info( "Successful bind of an LDAP Service (" + port + ") is complete." );
            }
        }
        catch ( IOException e )
        {
            String msg = "Failed to bind an LDAP service (" + port + ") to the service registry.";
            LdapConfigurationException lce = new LdapConfigurationException( msg );
            lce.setRootCause( e );
            log.error( msg, e );
            throw lce;
        }
    }


    private void startKerberos( KdcConfiguration kdcConfig )
    {
        // Skip if disabled
        if ( !kdcConfig.isEnabled() )
        {
            return;
        }

        try
        {
            PrincipalStore kdcStore = new JndiPrincipalStoreImpl( kdcConfig, this );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( threadModel );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setFilterChainBuilder( new DefaultIoFilterChainBuilder() );
            tcpConfig.setThreadModel( threadModel );

            tcpKdcServer = new KerberosServer( kdcConfig, tcpAcceptor, tcpConfig, kdcStore );
            udpKdcServer = new KerberosServer( kdcConfig, udpAcceptor, udpConfig, kdcStore );
        }
        catch ( Throwable t )
        {
            log.error( "Failed to start the Kerberos service", t );
        }
    }


    private void startChangePassword( ChangePasswordConfiguration changePasswordConfig )
    {
        // Skip if disabled
        if ( !changePasswordConfig.isEnabled() )
        {
            return;
        }

        try
        {
            PrincipalStore store = new JndiPrincipalStoreImpl( changePasswordConfig, this );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( threadModel );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setFilterChainBuilder( new DefaultIoFilterChainBuilder() );
            tcpConfig.setThreadModel( threadModel );

            tcpChangePasswordServer = new ChangePasswordServer( changePasswordConfig, tcpAcceptor, tcpConfig, store );
            udpChangePasswordServer = new ChangePasswordServer( changePasswordConfig, udpAcceptor, udpConfig, store );
        }
        catch ( Throwable t )
        {
            log.error( "Failed to start the Change Password service", t );
        }
    }


    private void startNTP( NtpConfiguration ntpConfig )
    {
        // Skip if disabled
        if ( !ntpConfig.isEnabled() )
        {
            return;
        }

        try
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( threadModel );

            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            tcpConfig.setFilterChainBuilder( new DefaultIoFilterChainBuilder() );
            tcpConfig.setThreadModel( threadModel );

            tcpNtpServer = new NtpServer( ntpConfig, tcpAcceptor, tcpConfig );
            udpNtpServer = new NtpServer( ntpConfig, udpAcceptor, udpConfig );
        }
        catch ( Throwable t )
        {
            log.error( "Failed to start the NTP service", t );
        }
    }


    private void startDNS( DnsConfiguration dnsConfig )
    {
        // Skip if disabled
        if ( !dnsConfig.isEnabled() )
        {
            return;
        }

        try
        {
            RecordStore store = new JndiRecordStoreImpl( dnsConfig, this );

            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( threadModel );

            udpDnsServer = new DnsServer( dnsConfig, udpAcceptor, udpConfig, store );
        }
        catch ( Throwable t )
        {
            log.error( "Failed to start the DNS service", t );
        }
    }


    private void stopLDAP0( int port )
    {
        try
        {
            // we should unbind the service before we begin sending the notice 
            // of disconnect so new connections are not formed while we process
            List writeFutures = new ArrayList();

            // If the socket has already been unbound as with a successful 
            // GracefulShutdownRequest then this will complain that the service
            // is not bound - this is ok because the GracefulShutdown has already
            // sent notices to to the existing active sessions
            List sessions = null;
            try
            {
                sessions = new ArrayList( tcpAcceptor.getManagedSessions( new InetSocketAddress( port ) ) );
            }
            catch ( IllegalArgumentException e )
            {
                log.warn( "Seems like the LDAP service (" + port + ") has already been unbound." );
                return;
            }

            tcpAcceptor.unbind( new InetSocketAddress( port ) );
            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of an LDAP service (" + port + ") is complete." );
                log.info( "Sending notice of disconnect to existing clients sessions." );
            }

            // Send Notification of Disconnection messages to all connected clients.
            if ( sessions != null )
            {
                for ( Iterator i = sessions.iterator(); i.hasNext(); )
                {
                    IoSession session = ( IoSession ) i.next();
                    writeFutures.add( session.write( NoticeOfDisconnect.UNAVAILABLE ) );
                }
            }

            // And close the connections when the NoDs are sent.
            Iterator sessionIt = sessions.iterator();
            for ( Iterator i = writeFutures.iterator(); i.hasNext(); )
            {
                WriteFuture future = ( WriteFuture ) i.next();
                future.join( 1000 );
                ( ( IoSession ) sessionIt.next() ).close();
            }
        }
        catch ( Exception e )
        {
            log.warn( "Failed to sent NoD.", e );
        }
    }
}
