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
package org.apache.ldap.server.jndi;


import java.io.IOException;
import java.io.FileFilter;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import org.apache.kerberos.kdc.KdcConfiguration;
import org.apache.kerberos.kdc.KerberosServer;
import org.apache.kerberos.store.JndiPrincipalStoreImpl;
import org.apache.kerberos.store.PrincipalStore;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.configuration.ServerStartupConfiguration;
import org.apache.ldap.server.protocol.ExtendedOperationHandler;
import org.apache.ldap.server.protocol.LdapProtocolProvider;
import org.apache.mina.common.TransportType;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.ntp.NtpServer;
import org.apache.ntp.NtpConfiguration;
import org.apache.protocol.common.LoadStrategy;
import org.apache.protocol.common.store.LdifFileLoader;
import org.apache.changepw.ChangePasswordServer;
import org.apache.changepw.ChangePasswordConfiguration;
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

    private static Service ldapService;
    private static KerberosServer kdcServer;
    private static ChangePasswordServer changePasswordServer;
    private static NtpServer ntpServer;
    private static ServiceRegistry minaRegistry;


    protected ServiceRegistry getMinaRegistry()
    {
        return minaRegistry;
    }


    public void afterShutdown( DirectoryService service )
    {
        if ( minaRegistry != null )
        {
            if ( ldapService != null )
            {
                minaRegistry.unbind( ldapService );
                if ( log.isInfoEnabled() )
                {
                    log.info( "Unbind of LDAP Service complete: " + ldapService );
                }
                ldapService = null;
            }

            if ( kdcServer != null )
            {
                kdcServer.destroy();
                if ( log.isInfoEnabled() )
                {
                    log.info( "Unbind of KRB5 Service complete: " + kdcServer );
                }
                kdcServer = null;
            }

            if ( changePasswordServer != null )
            {
                changePasswordServer.destroy();
                if ( log.isInfoEnabled() )
                {
                    log.info( "Unbind of Change Password Service complete: " + changePasswordServer );
                }
                changePasswordServer = null;
            }

            if ( ntpServer != null )
            {
                ntpServer.destroy();
                if ( log.isInfoEnabled() )
                {
                    log.info( "Unbind of NTP Service complete: " + ntpServer );
                }
                ntpServer = null;
            }
        }
    }


    public void afterStartup( DirectoryService service ) throws NamingException
    {
        ServerStartupConfiguration cfg =
            ( ServerStartupConfiguration ) service.getConfiguration().getStartupConfiguration();
        Hashtable env = service.getConfiguration().getEnvironment();

        loadLdifs( service );

        if ( cfg.isEnableNetworking() )
        {
            setupRegistry( cfg );
            startLdapProtocol( cfg, env );

            if ( cfg.isEnableKerberos() )
            {
                try
                {
                    KdcConfiguration kdcConfiguration = new KdcConfiguration( env, LoadStrategy.PROPS );
                    PrincipalStore kdcStore = new JndiPrincipalStoreImpl( kdcConfiguration, this );
                    kdcServer = new KerberosServer( kdcConfiguration, minaRegistry, kdcStore );
                }
                catch ( Throwable t )
                {
                    log.error( "Failed to start the Kerberos service", t );
                }
            }

            if ( cfg.isEnableChangePassword() )
            {
                try
                {
                    ChangePasswordConfiguration changePasswordConfiguration = new ChangePasswordConfiguration( env, LoadStrategy.PROPS );
                    PrincipalStore store = new JndiPrincipalStoreImpl( changePasswordConfiguration, this );
                    changePasswordServer = new ChangePasswordServer( changePasswordConfiguration, minaRegistry, store );
                }
                catch ( Throwable t )
                {
                    log.error( "Failed to start the Change Password service", t );
                }
            }

            if ( cfg.isEnableNtp() )
            {
                try
                {
                    NtpConfiguration ntpConfig = new NtpConfiguration( env, LoadStrategy.PROPS );
                    ntpServer = new NtpServer( ntpConfig, minaRegistry );
                }
                catch ( Throwable t )
                {
                    log.error( "Failed to start the NTP service", t );
                }
            }
        }
    }


    private void ensureLdifFileBase( DirContext root ) throws NamingException
    {
        Attributes entry = new BasicAttributes( "ou", "loadedLdifFiles", true );
        entry.put( "objectClass", "top" );
        entry.get( "objectClass" ).add( "organizationalUnit" );
        try
        {
            root.createSubcontext( LDIF_FILES_DN, entry );
            log.info( "Creating " + LDIF_FILES_DN );
        }
        catch( NamingException e ) { log.info( LDIF_FILES_DN + " exists" );}
    }


    private final static String WINDOWSFILE_ATTR = "windowsFilePath";
    private final static String UNIXFILE_ATTR = "unixFilePath";
    private final static String WINDOWSFILE_OC = "windowsFile";
    private final static String UNIXFILE_OC = "unixFile";
    private void addFileEntry( DirContext root, File ldif ) throws NamingException
    {
        String rdnAttr = File.pathSeparatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR;
        String oc = File.pathSeparatorChar == '\\' ? WINDOWSFILE_OC : UNIXFILE_OC;
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


    private Attributes getLdifFileEntry( DirContext root, File ldif ) throws NamingException
    {
        String rdnAttr = File.pathSeparatorChar == '\\' ? WINDOWSFILE_ATTR : UNIXFILE_ATTR;
        StringBuffer buf = new StringBuffer();
        buf.append( rdnAttr );
        buf.append( "=" );
        buf.append( getCanonical( ldif ) );
        buf.append( "," );
        buf.append( LDIF_FILES_DN );

        try
        {
            return root.getAttributes( buf.toString(), new String[]{ "createTimestamp" });
        }
        catch ( NamingException e )
        {
            return null;
        }
    }


    private String getCanonical( File file ) throws NamingException
    {
        String canonical = null;
        try
        {
            canonical = file.getCanonicalPath();
        }
        catch (IOException e)
        {
            log.error( "could not get canonical path", e );
            return null;
        }
        return canonical;
    }


    private void loadLdifs( DirectoryService service ) throws NamingException
    {
        ServerStartupConfiguration cfg =
            ( ServerStartupConfiguration ) service.getConfiguration().getStartupConfiguration();

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
                    + "' does not exist.  No LDIF files will be loaded.");
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
        });

        // log and bail if we could not find any LDIF files
        if ( ldifFiles == null || ldifFiles.length == 0 )
        {
            log.warn( "LDIF load directory '" + getCanonical( cfg.getLdifDirectory() )
                    + "' does not contain any LDIF files.  No LDIF files will be loaded.");
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
     * Starts up the MINA registry so various protocol providers can be started.
     */
    private void setupRegistry( ServerStartupConfiguration cfg )
    {
        minaRegistry = cfg.getMinaServiceRegistry();
    }


    /**
     * Starts up the LDAP protocol provider to service LDAP requests
     *
     * @throws NamingException if there are problems starting the LDAP provider
     */
    private void startLdapProtocol( ServerStartupConfiguration cfg, Hashtable env ) throws NamingException
    {
        int port = cfg.getLdapPort();
        Service service = new Service( "ldap", TransportType.SOCKET, new InetSocketAddress( port ) );

        // Register all extended operation handlers.
        LdapProtocolProvider protocolProvider = new LdapProtocolProvider( ( Hashtable ) env.clone() );
        for( Iterator i = cfg.getExtendedOperationHandlers().iterator(); i.hasNext(); )
        {
            ExtendedOperationHandler h = ( ExtendedOperationHandler ) i.next();
            protocolProvider.addExtendedOperationHandler( h );
        }
        
        try
        {
            minaRegistry.bind( service, protocolProvider );
            ldapService = service;
            if ( log.isInfoEnabled() )
            {
                log.info( "Successful bind of LDAP Service completed: " + ldapService );
            }
        }
        catch ( IOException e )
        {
            String msg = "Failed to bind the LDAP protocol service to the service registry: " + service;
            LdapConfigurationException lce = new LdapConfigurationException( msg );
            lce.setRootCause( e );
            log.error( msg, e );
            throw lce;
        }
    }
}
