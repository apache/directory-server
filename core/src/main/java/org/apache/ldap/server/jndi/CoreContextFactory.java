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
 * A simplistic implementation of {@link AbstractContextFactory}.
 * This class simply extends {@link AbstractContextFactory} and leaves all
 * abstract hook methods as empty.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreContextFactory extends AbstractContextFactory implements InitialContextFactory
{
    /**
     * Creates a new instance.
     */
    public CoreContextFactory()
    {
    }

    /**
     * Does nothing by default.
     */
    protected void beforeStartup( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }

    /**
     * Does nothing by default.
     */
    protected void afterStartup( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }
    
    /**
     * Does nothing by default.
     */
    protected void beforeShutdown( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }
    
    /**
     * Does nothing by default.
     */
    protected void afterShutdown( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }
    
    /**
     * Does nothing by default.
     */
    protected void beforeSync( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }

    /**
     * Does nothing by default.
     */
    protected void afterSync( ContextFactoryConfiguration ctx ) throws NamingException
    {
    }
}
