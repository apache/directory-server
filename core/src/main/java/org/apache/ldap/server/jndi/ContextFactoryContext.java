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

import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.SystemPartition;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.schema.GlobalRegistries;

public interface ContextFactoryContext
{
    /**
     * Returns the initial context environment of this context factory.
     */
    Hashtable getEnvironment();
    
    /**
     * Returns the startup configuration of this context factory.
     */
    StartupConfiguration getConfiguration();
    
    /**
     * Returns the system partition used by this context factory.
     */
    SystemPartition getSystemPartition();

    /**
     * Returns the registries for system schema objects
     */
    GlobalRegistries getGlobalRegistries();

    /**
     * Returns the root nexus of this context factory.
     */
    RootNexus getRootNexus();
    
    /**
     * Returns <tt>true</tt> if this context is started for the first time
     * and bootstrap entries have been created.
     */
    boolean isFirstStart();
    
    /**
     * Returns <tt>true</tt> if this context is started.
     */
    boolean isStarted();
    
    Context getJndiContext() throws NamingException;
    Context getJndiContext( String rootDN ) throws NamingException;
    Context getJndiContext( String username, String password, String rootDN ) throws NamingException;

    /**
     * Invokes {@link Invocation} to this context.
     */
    Object invoke( Invocation call ) throws NamingException;
    
    void sync() throws NamingException;
    
    void shutdown() throws NamingException;
}
