/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.DirectoryService;
import javax.naming.InitialContext;
import javax.naming.spi.InitialContextFactory;


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
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * 
 * @see javax.naming.spi.InitialContextFactory
 */
public abstract class AbstractContextFactory implements InitialContextFactory
{
}
