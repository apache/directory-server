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


import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.configuration.SyncConfiguration;
import org.apache.ldap.server.partition.ContextPartition;


/**
 * A server-side provider implementation of {@link InitialContextFactory}.
 * This class can be utilized via JNDI API in the standard fashion:
 *
 * <code>
 * Hashtable env = new Hashtable();
 * env.put( Context.PROVIDER_URL, "ou=system" );
 * env.put(
 * Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );
 * InitialContext initialContext = new InitialContext( env );
 * </code>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * 
 * @see javax.naming.spi.InitialContextFactory
 */
public abstract class AbstractContextFactory implements InitialContextFactory
{
    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    /** The singleton JndiProvider instance */
    private static final ContextFactoryConfiguration provider = new DefaultContextFactoryConfiguration();

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
        
        String principal = extractPrincipal( env );
        byte[] credential = extractCredential( env );
        String authentication = extractAuthentication( env );
        String providerUrl = extractProviderUrl( env );

        // Execute configuration
        if( cfg instanceof ShutdownConfiguration )
        {
            provider.shutdown();
        }
        else if( cfg instanceof SyncConfiguration )
        {
            provider.sync();
        }
        else if( cfg instanceof StartupConfiguration )
        {
            ( ( DefaultContextFactoryConfiguration ) provider ).startup( this, env );
        }
        else if( provider == null )
        {
            throw new NamingException( "Unknown configuration: " + cfg );
        }
        
        return provider.getJndiContext( principal, credential, authentication, providerUrl );
    }

    private String extractProviderUrl( Hashtable env )
    {
        String providerUrl;
        Object value;
        value = env.remove( Context.PROVIDER_URL );
        if( value == null )
        {
            value = "";
        }
        providerUrl = value.toString();
        return providerUrl;
    }

    private String extractAuthentication( Hashtable env )
    {
        String authentication;
        Object value = env.remove( Context.SECURITY_AUTHENTICATION );
        if( value == null )
        {
            authentication = "none";
        }
        else
        {
            authentication = value.toString();
        }
        return authentication;
    }

    private byte[] extractCredential( Hashtable env ) throws ConfigurationException
    {
        byte[] credential;
        Object value = env.remove( Context.SECURITY_CREDENTIALS );
        if( value == null )
        {
            credential = null;
        }
        else if( value instanceof String )
        {
            credential = ( ( String ) value ).getBytes();
        }
        else if( value instanceof byte[] )
        {
            credential = ( byte[] ) value;
        }
        else
        {
            throw new ConfigurationException( "Can't convert '" + Context.SECURITY_CREDENTIALS + "' to byte[]." );
        }
        return credential;
    }

    private String extractPrincipal( Hashtable env )
    {
        String principal;
        Object value = env.remove( Context.SECURITY_PRINCIPAL );
        if( value == null )
        {
            principal = null;
        }
        else
        {
            principal = value.toString();
        }
        return principal;
    }
    
    /**
     * Invoked before starting up JNDI provider.
     */
    protected abstract void beforeStartup( ContextFactoryConfiguration ctx ) throws NamingException;
    /**
     * Invoked after starting up JNDI provider.
     */
    protected abstract void afterStartup( ContextFactoryConfiguration ctx ) throws NamingException;
    /**
     * Invoked before shutting down JNDI provider.
     */
    protected abstract void beforeShutdown( ContextFactoryConfiguration ctx ) throws NamingException;
    /**
     * Invoked after shutting down JNDI provider.
     */
    protected abstract void afterShutdown( ContextFactoryConfiguration ctx ) throws NamingException;
    /**
     * Invoked before calling {@link ContextPartition#sync()} for all registered {@link ContextPartition}s.
     */
    protected abstract void beforeSync( ContextFactoryConfiguration ctx ) throws NamingException;
    /**
     * Invoked after calling {@link ContextPartition#sync()} for all registered {@link ContextPartition}s.
     */
    protected abstract void afterSync( ContextFactoryConfiguration ctx ) throws NamingException;
}
