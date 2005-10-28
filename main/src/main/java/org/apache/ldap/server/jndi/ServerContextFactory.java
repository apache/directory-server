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
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.NamingException;

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
    private static Logger log = LoggerFactory.getLogger( ServerContextFactory.class.getName() );
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

        if ( cfg.isEnableNetworking() )
        {
            setupRegistry( cfg );
            startLdapProtocol( cfg, env );

            if ( cfg.isEnableKerberos() )
            {
                KdcConfiguration kdcConfiguration = new KdcConfiguration( env, LoadStrategy.PROPS );
                PrincipalStore kdcStore = new JndiPrincipalStoreImpl( kdcConfiguration, this );
                kdcServer = new KerberosServer( kdcConfiguration, minaRegistry, kdcStore );
            }

            if ( cfg.isEnableChangePassword() )
            {
                ChangePasswordConfiguration changePasswordConfiguration = new ChangePasswordConfiguration( env, LoadStrategy.PROPS );
                PrincipalStore store = new JndiPrincipalStoreImpl( changePasswordConfiguration, this );
                changePasswordServer = new ChangePasswordServer( changePasswordConfiguration, minaRegistry, store );
            }

            if ( cfg.isEnableNtp() )
            {
                NtpConfiguration ntpConfig = new NtpConfiguration( env, LoadStrategy.PROPS );
                ntpServer = new NtpServer( ntpConfig, minaRegistry );
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
