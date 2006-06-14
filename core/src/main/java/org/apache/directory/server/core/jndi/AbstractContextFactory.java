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
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceListener;
import org.apache.directory.server.core.configuration.AddDirectoryPartitionConfiguration;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.RemoveDirectoryPartitionConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;


/**
 * A server-side JNDI provider implementation of {@link InitialContextFactory}.
 * This class can be utilized via JNDI API in the standard fashion:
 * <p>
 * <code>
 * Hashtable env = new Hashtable();
 * env.put( Context.PROVIDER_URL, "ou=system" );
 * env.put(
 * Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
 * InitialContext initialContext = new InitialContext( env );
 * </code>
 * <p>
 * Unfortunately, {@link InitialContext} creates a new instance of
 * {@link InitialContextFactory} implementation everytime it is instantiated,
 * so this factory maintains only a static, singleton instance of
 * {@link DirectoryService}, which provides actual implementation.
 * Please note that you'll also have to maintain any stateful information
 * as using singleton pattern if you're going to extend this factory.
 * <p>
 * This class implements {@link DirectoryServiceListener}.  This means that
 * you can listen to the changes occurs to {@link DirectoryService}, and
 * react to it (e.g. executing additional business logic).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * 
 * @see javax.naming.spi.InitialContextFactory
 */
public abstract class AbstractContextFactory implements InitialContextFactory, DirectoryServiceListener
{
    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    /**
     * Creates a new instance.
     */
    protected AbstractContextFactory()
    {
    }


    public final synchronized Context getInitialContext( Hashtable env ) throws NamingException
    {
        Configuration cfg = Configuration.toConfiguration( env );
        env = ( Hashtable ) env.clone();
        String principal = getPrincipal( env );
        byte[] credential = getCredential( env );
        String authentication = getAuthentication( env );
        String providerUrl = getProviderUrl( env );

        DirectoryService service = DirectoryService.getInstance( cfg.getInstanceId() );

        // Execute configuration
        if ( cfg instanceof ShutdownConfiguration )
        {
            service.shutdown();
        }
        else if ( cfg instanceof SyncConfiguration )
        {
            service.sync();
        }
        else if ( cfg instanceof StartupConfiguration )
        {
            service.startup( this, env );
        }
        else if ( cfg instanceof AddDirectoryPartitionConfiguration )
        {
            new DirectoryPartitionNexusProxy( service.getJndiContext( principal, credential, authentication, "" ),
                service ).addContextPartition( ( ( AddDirectoryPartitionConfiguration ) cfg )
                .getDirectoryPartitionConfiguration() );
        }
        else if ( cfg instanceof RemoveDirectoryPartitionConfiguration )
        {
            Context ctx = service.getJndiContext( principal, credential, authentication, "" );
            DirectoryPartitionNexusProxy proxy = new DirectoryPartitionNexusProxy( ctx, service );
            proxy.removeContextPartition( ( ( RemoveDirectoryPartitionConfiguration ) cfg ).getSuffix() );
        }
        else if ( service == null )
        {
            throw new NamingException( "Unknown configuration: " + cfg );
        }

        return service.getJndiContext( principal, credential, authentication, providerUrl );
    }


    public static String getProviderUrl( Hashtable env )
    {
        String providerUrl;
        Object value;
        value = env.get( Context.PROVIDER_URL );
        if ( value == null )
        {
            value = "";
        }
        providerUrl = value.toString();

        env.put( Context.PROVIDER_URL, providerUrl );

        return providerUrl;
    }


    public static String getAuthentication( Hashtable env )
    {
        String authentication;
        Object value = env.get( Context.SECURITY_AUTHENTICATION );
        if ( value == null )
        {
            authentication = "none";
        }
        else
        {
            authentication = value.toString();
        }

        env.put( Context.SECURITY_AUTHENTICATION, authentication );

        return authentication;
    }


    public static byte[] getCredential( Hashtable env ) throws ConfigurationException
    {
        byte[] credential;
        Object value = env.get( Context.SECURITY_CREDENTIALS );
        if ( value == null )
        {
            credential = null;
        }
        else if ( value instanceof String )
        {
            credential = ( ( String ) value ).getBytes();
        }
        else if ( value instanceof byte[] )
        {
            credential = ( byte[] ) value;
        }
        else
        {
            throw new ConfigurationException( "Can't convert '" + Context.SECURITY_CREDENTIALS + "' to byte[]." );
        }

        if ( credential != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, credential );
        }

        return credential;
    }


    public static String getPrincipal( Hashtable env )
    {
        String principal;
        Object value = env.get( Context.SECURITY_PRINCIPAL );
        if ( value == null )
        {
            principal = null;
        }
        else
        {
            principal = value.toString();
            env.put( Context.SECURITY_PRINCIPAL, principal );
        }

        return principal;
    }
}
