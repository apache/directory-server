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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.ldap.server.partition.ContextPartition;

/**
 * Provides JNDI service to {@link AbstractContextFactory}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public interface ContextFactoryService
{
    /**
     * Starts up this service.
     * 
     * @param listener a listener that listens to the lifecycle of this service
     * @param environment JNDI {@link InitialContext} environment
     * 
     * @throws NamingException if failed to start up
     */
    void startup( ContextFactoryServiceListener listener, Hashtable environment ) throws NamingException;
    
    /**
     * Shuts down this service.
     * 
     * @throws NamingException if failed to shut down
     */
    void shutdown() throws NamingException;
    
    /**
     * Calls {@link ContextPartition#sync()} for all registered {@link ContextPartition}s.
     * @throws NamingException if synchronization failed
     */
    void sync() throws NamingException;
    
    /**
     * Returns <tt>true</tt> if this service is started.
     */
    boolean isStarted();
    
    /**
     * Returns the configuration of this service.
     */
    ContextFactoryConfiguration getConfiguration();
}
