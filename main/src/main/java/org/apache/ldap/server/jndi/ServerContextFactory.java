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
import java.util.Properties;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.Control;

import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.PropertiesUtils;
import org.apache.ldap.server.protocol.LdapProtocolProvider;
import org.apache.mina.common.TransportType;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;
import org.apache.kerberos.service.KdcConfiguration;
import org.apache.kerberos.protocol.KerberosProtocolProvider;
import org.apache.kerberos.store.PrincipalStore;
import org.apache.kerberos.store.JndiPrincipalStoreImpl;


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
    /** the default LDAP port to use */
    private static final int LDAP_PORT = 389;

    private static final ServiceRegistry DEFAULT_MINA_REGISTRY;
    
    private static Service ldapService;

    private static Service kerberosService;

    private static ServiceRegistry minaRegistry;


    static
    {
        ServiceRegistry tmp = null;

        try
        {
            tmp = new SimpleServiceRegistry();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        
        DEFAULT_MINA_REGISTRY = tmp;
    }

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------


    /**
     * Checks first for a shutdown operation and if so stops the server.  Otherwise
     * it initializes the networking subsystem starting up the mina registery and
     * other protocol provider services if enabled including the LDAP provider.
     *
     * @param env the JNDI environment parameters
     * @return the new LDAP context to be wrapped by InitialContext
     * @throws NamingException if there are problems starting or stopping the server
     */
    public Context getInitialContext( Hashtable env ) throws NamingException
    {
        Context ctx = null;

        if ( env.containsKey( EnvKeys.SHUTDOWN ) )
        {
            if ( this.provider == null )
            {
                return new DeadContext();
            }

            try
            {
                this.provider.shutdown();

                if ( minaRegistry != null )
                {
                    if ( ldapService != null )
                    {
                        minaRegistry.unbind( ldapService );

                        ldapService = null;
                    }

                    if ( kerberosService != null )
                    {
                        minaRegistry.unbind( kerberosService );

                        kerberosService = null;
                    }
                }
            }
            catch( NamingException ne )
            {
                throw ne;
            }
            catch( Throwable t )
            {
                NamingException ne = new NamingException( "Failed to shutdown." );

                ne.setRootCause( t );

                throw ne;
            }
            finally
            {
                ctx = new DeadContext();

                provider = null;

                initialEnv = null;
            }

            return ctx;
        }

        ctx = super.getInitialContext( env );

        // fire up the front end if we have not explicitly disabled it

        if ( initialEnv != null && ! initialEnv.containsKey( EnvKeys.DISABLE_PROTOCOL ) )
        {
            setupRegistry();

            startLdapProtocol();

            if ( initialEnv.containsKey( EnvKeys.ENABLE_KERBEROS ) )
            {
                startKerberosProtocol();
            }
        }

        return ctx;
    }


    /**
     * Starts up the MINA registry so various protocol providers can be started.
     *
     * @throws NamingException if there is a problem initializing the registry
     */
    private void setupRegistry() throws NamingException
    {
        ServiceRegistry registry = null;

        if ( initialEnv.containsKey( EnvKeys.PASSTHRU ) )
        {
            registry = ( ServiceRegistry ) initialEnv.get( EnvKeys.PASSTHRU );

            if ( registry != null )
            {
                initialEnv.put( EnvKeys.PASSTHRU, "Handoff Succeeded!" );
            }
        }

        if( DEFAULT_MINA_REGISTRY == null )
        {
            throw new NamingException( "Default MINA service registry is not available." );
        }

        if( registry == null )
        {
            registry = DEFAULT_MINA_REGISTRY;
        }

        minaRegistry = registry;
    }


    /**
     * Starts the Kerberos protocol provider which is backed by the LDAP store.
     *
     * @throws NamingException if there are problems starting up the Kerberos provider
     */
    private void startKerberosProtocol() throws NamingException
    {
        /*
         * Looks like KdcConfiguration takes properties and we use Hashtable for JNDI
         * so I'm copying over the String based properties into a new Properties obj.
         */

        Properties props = new Properties();

        Iterator list = initialEnv.keySet().iterator();

        while ( list.hasNext() )
        {
            String key = ( String ) list.next();

            if ( initialEnv.get( key ) instanceof String )
            {
                props.setProperty( key, ( String ) initialEnv.get( key ) );
            }
        }

        KdcConfiguration config = new KdcConfiguration( props );

        int port = PropertiesUtils.get( initialEnv, KdcConfiguration.KERBEROS_PORT_KEY, KdcConfiguration.DEFAULT_KERBEROS_PORT );

        Service service= new Service( "kerberos", TransportType.DATAGRAM, new InetSocketAddress( port ) );

        InitialLdapContext ctx = new InitialLdapContext( initialEnv, new Control[]{} );

        Object baseStr = initialEnv.get( JndiPrincipalStoreImpl.KDC_STORE_SEARCHBASE );

        if ( baseStr == null )
        {
            throw new LdapConfigurationException( "Trying to start kerberos service without setting " + JndiPrincipalStoreImpl.KDC_STORE_SEARCHBASE );
        }

        LdapName base = new LdapName( ( String ) baseStr );

        PrincipalStore store = new JndiPrincipalStoreImpl( ctx, base );

        try
        {
            minaRegistry.bind( service, new KerberosProtocolProvider( config, store ) );

            kerberosService = service;
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }


    /**
     * Starts up the LDAP protocol provider to service LDAP requests
     *
     * @throws NamingException if there are problems starting the LDAP provider
     */
    private void startLdapProtocol() throws NamingException
    {
        int port = PropertiesUtils.get( initialEnv, EnvKeys.LDAP_PORT, LDAP_PORT );

        Service service = new Service( "ldap", TransportType.SOCKET, new InetSocketAddress( port ) );

        try
        {
            minaRegistry.bind( service, new LdapProtocolProvider( ( Hashtable ) initialEnv.clone() ) );

            ldapService = service;
        }
        catch ( IOException e )
        {
            String msg = "Failed to bind the LDAP protocol service to the service registry: " + service;

            LdapConfigurationException lce = new LdapConfigurationException( msg );

            lce.setRootCause( e );

            throw lce;
        }
    }
}
