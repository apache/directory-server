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
import java.io.InputStream;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.util.PropertiesUtils;
import org.apache.ldap.common.ldif.LdifParser;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.ldif.LdifIterator;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.protocol.LdapProtocolProvider;
import org.apache.mina.common.TransportType;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;


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

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    private static Service minaService;

    private static ServiceRegistry minaRegistry;


    /**
     * Default constructor that sets the provider of this ServerContextFactory.
     */
    public ServerContextFactory()
    {
        super();
    }


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
                    minaRegistry.unbind( minaService );
                }
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
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
            startUpWireProtocol();
        }

        if ( createMode )
        {
            importLdif();
        }

        return ctx;
    }


    private void startUpWireProtocol() throws NamingException
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

        int port = PropertiesUtils.get( initialEnv, EnvKeys.LDAP_PORT, LDAP_PORT );

        Service service = new Service( "ldap", TransportType.SOCKET, port );

        try
        {
            if( registry == null )
            {
                registry = new SimpleServiceRegistry();
            }

            registry.bind( service, new LdapProtocolProvider( ( Hashtable ) initialEnv.clone() ) );
            
            minaService = service;

            minaRegistry = registry;
        }
        catch ( IOException e )
        {
            e.printStackTrace();

            String msg = "Failed to bind the service to the service registry: " + service;

            LdapConfigurationException e2 = new LdapConfigurationException( msg );

            e2.setRootCause( e );
        }
    }


    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into the newly created system partition to prime
     * it up for operation.  Note that only ou=system entries will be added - entries for other partitions cannot be
     * imported and will blow chunks.
     *
     * @throws javax.naming.NamingException if there are problems reading the ldif file and adding those entries to the system
     *                         partition
     */
    protected void importLdif() throws NamingException
    {
        Hashtable env = new Hashtable();

        env.putAll( initialEnv );

        env.put( Context.PROVIDER_URL, "ou=system" );

        LdapContext ctx = provider.getLdapContext( env );

        InputStream in = getClass().getResourceAsStream( "system.ldif" );

        LdifParser parser = new LdifParserImpl();

        try
        {
            LdifIterator iterator = new LdifIterator( in );

            while ( iterator.hasNext() )
            {
                Attributes attributes = new LockableAttributesImpl();

                String ldif = ( String ) iterator.next();

                parser.parse( attributes, ldif );

                Name dn = new LdapName( ( String ) attributes.remove( "dn" ).get() );

                dn.remove( 0 );

                ctx.createSubcontext( dn, attributes );
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system ldif file";

            NamingException ne = new LdapConfigurationException( msg );

            ne.setRootCause( e );

            throw ne;
        }
    }
}
