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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.configuration.SyncConfiguration;


/**
 * A server-side provider implementation of a InitialContextFactory.  Can be
 * utilized via JNDI API in the standard fashion:
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
    private static final ContextFactoryContext provider = new DefaultContextFactoryContext();

    /**
     * Default constructor that sets the provider of this ServerContextFactory.
     */
    public AbstractContextFactory()
    {
    }
    
    public final synchronized Context getInitialContext( Hashtable env ) throws NamingException
    {
        Configuration cfg = Configuration.toConfiguration( env );
        
        String principal;
        String credential;
        String authentication;
        String providerUrl;

        env = ( Hashtable ) env.clone();

        // Remove properties that can be changed
        Object value = env.remove( Context.SECURITY_PRINCIPAL );
        if( value == null )
        {
            principal = null;
        }
        else
        {
            principal = value.toString();
        }
        
        value = env.remove( Context.SECURITY_CREDENTIALS );
        if( value == null )
        {
            credential = null;
        }
        else
        {
            credential = value.toString();
        }
        
        value = env.remove( Context.SECURITY_AUTHENTICATION );
        if( value == null )
        {
            authentication = "none";
        }
        else
        {
            authentication = value.toString();
        }

        value = env.remove( Context.PROVIDER_URL );
        if( value == null )
        {
            value = "";
        }
        providerUrl = value.toString();

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
            ( ( DefaultContextFactoryContext ) provider ).startup( this, env );
        }
        else
        {
            throw new NamingException( "Unknown configuration: " + cfg );
        }
        
        return provider.getJndiContext( principal, credential, authentication, providerUrl );
    }
    
    protected abstract void beforeStartup( ContextFactoryContext ctx ) throws NamingException;
    protected abstract void afterStartup( ContextFactoryContext ctx ) throws NamingException;
    protected abstract void beforeShutdown( ContextFactoryContext ctx ) throws NamingException;
    protected abstract void afterShutdown( ContextFactoryContext ctx ) throws NamingException;
    protected abstract void beforeSync( ContextFactoryContext ctx ) throws NamingException;
    protected abstract void afterSync( ContextFactoryContext ctx ) throws NamingException;
}
