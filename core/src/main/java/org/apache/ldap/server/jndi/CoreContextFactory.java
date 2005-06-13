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

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;


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
 * @version $Rev$
 * @see javax.naming.spi.InitialContextFactory
 */
public class CoreContextFactory extends AbstractContextFactory implements InitialContextFactory
{
    /**
     * Creates a new instance.
     */
    public CoreContextFactory()
    {
    }

    protected void beforeStartup( ContextFactoryContext ctx ) throws NamingException
    {
    }

    protected void afterStartup( ContextFactoryContext ctx ) throws NamingException
    {
    }
    
    protected void beforeShutdown( ContextFactoryContext ctx ) throws NamingException
    {
    }
    
    protected void afterShutdown( ContextFactoryContext ctx ) throws NamingException
    {
    }
    
    protected void beforeSync( ContextFactoryContext ctx ) throws NamingException
    {
    }

    protected void afterSync( ContextFactoryContext ctx ) throws NamingException
    {
    }
}
