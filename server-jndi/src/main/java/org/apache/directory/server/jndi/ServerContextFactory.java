/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.jndi;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.kdc.KerberosServer;
import org.apache.directory.server.kerberos.shared.store.JndiPrincipalStoreImpl;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapProtocolProvider;
import org.apache.directory.server.ntp.NtpConfiguration;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.server.protocol.shared.LoadStrategy;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoFilterChainBuilder;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.thread.ThreadPoolFilter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    protected static final IoAcceptor tcpAcceptor = new SocketAcceptor();
    protected static final IoAcceptor udpAcceptor = new DatagramAcceptor();
    protected static final ThreadPoolFilter threadPool;
    
    static
    {
        threadPool = new ThreadPoolFilter();
        tcpAcceptor.getFilterChain().addFirst( "threadPool", threadPool );
        udpAcceptor.getFilterChain().addFirst( "threadPool", threadPool );
    }

    private static boolean ldapStarted;
    private static boolean ldapsStarted;
    private static KerberosServer tcpKdcServer;
    private static KerberosServer udpKdcServer;
    private static ChangePasswordServer tcpChangePasswordServer;
    private static ChangePasswordServer udpChangePasswordServer;
    private static NtpServer tcpNtpServer;
    private static NtpServer udpNtpServer;
    private DirectoryService directoryService;


    public void beforeStartup( DirectoryService service )
    {
        threadPool.setMaximumPoolSize( service.getConfiguration().getStartupConfiguration().getMaxThreads() );
        this.directoryService = service;
    }


    public void afterShutdown( DirectoryService service )
    {
        ServerStartupConfiguration cfg = ( ServerStartupConfiguration ) 
            service.getConfiguration().getStartupConfiguration();
        
        if ( ldapStarted )
        {
            stopLDAP0( cfg.getLdapPort() );
            ldapStarted = false;
        }

        if ( ldapsStarted )
        {
            stopLDAP0( cfg.getLdapsPort() );
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
    }


    public void afterStartup( DirectoryService service ) throws NamingException
    {
        ServerStartupConfiguration cfg = ( ServerStartupConfiguration ) service.getConfiguration()
            .getStartupConfiguration();
        Hashtable env = service.getConfiguration().getEnvironment();

        loadLdifs( service );

        if ( cfg.isEnableNetworking() )
        {
            startLDAP( cfg, env );
            startLDAPS( cfg, env );
            startKerberos( cfg, env );
            startChangePassword( cfg, env );
            startNTP( cfg, env );
        }
    }


    private void ensureLdifFileBase( DirContext root )
    {
        Attributes entry = new BasicAttributes( "ou", "loadedLdifFiles", true );
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

        Attributes entry = new BasicAttributes( rdnAttr, getCanonical( ldif ), true );
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
    private void startLDAP( ServerStartupConfiguration cfg, Hashtable env ) throws NamingException
    {
        // Skip if disabled
        int port = cfg.getLdapPort();
        if ( port < 0 )
        {
            return;
        }

        startLDAP0( cfg, env, port, new DefaultIoFilterChainBuilder() );
    }


    /**
     * Starts up the LDAPS protocol provider to service LDAPS requests
     *
     * @throws NamingException if there are problems starting the LDAPS provider
     */
    private void startLDAPS( ServerStartupConfiguration cfg, Hashtable env ) throws NamingException
    {
        // Skip if disabled
        if ( !cfg.isEnableLdaps() )
        {
            return;
        }

        // We use the reflection API in case this is not running on JDK 1.5+.
        IoFilterChainBuilder chain;
        try
        {
            chain = ( IoFilterChainBuilder ) Class.forName( "org.apache.directory.server.ssl.LdapsInitializer", true,
                ServerContextFactory.class.getClassLoader() ).getMethod( "init", new Class[]
                { ServerStartupConfiguration.class } ).invoke( null, new Object[]
                { cfg } );
        }
        catch ( InvocationTargetException e )
        {
            if ( e.getCause() instanceof NamingException )
            {
                throw ( NamingException ) e.getCause();
            }
            else
            {
                throw ( NamingException ) new NamingException( "Failed to load LDAPS initializer." ).initCause( e
                    .getCause() );
            }
        }
        catch ( Exception e )
        {
            throw ( NamingException ) new NamingException( "Failed to load LDAPS initializer." ).initCause( e );
        }

        startLDAP0( cfg, env, cfg.getLdapsPort(), chain );
    }


    private void startLDAP0( ServerStartupConfiguration cfg, Hashtable env, int port,
        IoFilterChainBuilder chainBuilder ) throws LdapNamingException, LdapConfigurationException
    {
        // Register all extended operation handlers.
        LdapProtocolProvider protocolProvider = new LdapProtocolProvider( cfg, ( Hashtable ) env.clone() );

        for ( Iterator i = cfg.getExtendedOperationHandlers().iterator(); i.hasNext(); )
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
            ((SocketSessionConfig)(acceptorCfg.getSessionConfig())).setTcpNoDelay( true );
            
            tcpAcceptor.bind( new InetSocketAddress( port ), protocolProvider.getHandler(), acceptorCfg );

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


    private void startKerberos( ServerStartupConfiguration cfg, Hashtable env )
    {
        if ( cfg.isEnableKerberos() )
        {
            try
            {
                KdcConfiguration kdcConfiguration = new KdcConfiguration( env, LoadStrategy.PROPS );
                PrincipalStore kdcStore = new JndiPrincipalStoreImpl( kdcConfiguration, this );
                tcpKdcServer = new KerberosServer( kdcConfiguration, tcpAcceptor, kdcStore );
                udpKdcServer = new KerberosServer( kdcConfiguration, udpAcceptor, kdcStore );
            }
            catch ( Throwable t )
            {
                log.error( "Failed to start the Kerberos service", t );
            }
        }
    }


    private void startChangePassword( ServerStartupConfiguration cfg, Hashtable env )
    {
        if ( cfg.isEnableChangePassword() )
        {
            try
            {
                ChangePasswordConfiguration changePasswordConfiguration = new ChangePasswordConfiguration( env,
                    LoadStrategy.PROPS );
                PrincipalStore store = new JndiPrincipalStoreImpl( changePasswordConfiguration, this );
                tcpChangePasswordServer = new ChangePasswordServer( changePasswordConfiguration, tcpAcceptor, store );
                udpChangePasswordServer = new ChangePasswordServer( changePasswordConfiguration, udpAcceptor, store );
            }
            catch ( Throwable t )
            {
                log.error( "Failed to start the Change Password service", t );
            }
        }
    }


    private void startNTP( ServerStartupConfiguration cfg, Hashtable env )
    {
        if ( cfg.isEnableNtp() )
        {
            try
            {
                NtpConfiguration ntpConfig = new NtpConfiguration( env, LoadStrategy.PROPS );
                tcpNtpServer = new NtpServer( ntpConfig, tcpAcceptor );
                udpNtpServer = new NtpServer( ntpConfig, udpAcceptor );
            }
            catch ( Throwable t )
            {
                log.error( "Failed to start the NTP service", t );
            }
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
