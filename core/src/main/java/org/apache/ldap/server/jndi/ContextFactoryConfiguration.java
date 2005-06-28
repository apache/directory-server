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

import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.schema.GlobalRegistries;

/**
 * Represents the global configuration of currently running
 * {@link ContextFactoryService}.  You can access all properties of
 * {@link ContextFactoryService} and get JNDI {@link Context}s it provides
 * via this interface.
 */
public interface ContextFactoryConfiguration
{
    /**
     * Returns the instance ID of the {@link ContextFactoryService}.
     */
    String getInstanceId();
    
    /**
     * Returns the listener that listens to service events.
     */
    ContextFactoryServiceListener getServiceListener();
    
    /**
     * Returns the initial context environment of the {@link ContextFactoryService}.
     */
    Hashtable getEnvironment();
    
    /**
     * Returns the startup configuration of the {@link ContextFactoryService}.
     */
    StartupConfiguration getStartupConfiguration();
    
    /**
     * Returns the registries for system schema objects of the {@link ContextFactoryService}.
     */
    GlobalRegistries getGlobalRegistries();

    /**
     * Returns the {@link ContextPartitionNexus} of the {@link ContextFactoryService}.
     */
    ContextPartitionNexus getPartitionNexus();
    
    /**
     * Returns the interceptor chain of the {@link ContextFactoryService}.
     */
    InterceptorChain getInterceptorChain();
    
    /**
     * Returns <tt>true</tt> if this service is started
     * and bootstrap entries have been created for the first time.
     */
    boolean isFirstStart();
    
    /**
     * Returns an anonymous JNDI {@link Context} with the specified <tt>baseName</tt>
     * @throws NamingException if failed to create a context
     */
    Context getJndiContext( String baseName ) throws NamingException;
    
    /**
     * Returns a JNDI {@link Context} with the specified authentication information
     * (<tt>principal</tt>, <tt>credential</tt>, and <tt>authentication</tt>) and
     * <tt>baseName</tt>.
     * 
     * @param principal {@link Context#SECURITY_PRINCIPAL} value
     * @param credential {@link Context#SECURITY_CREDENTIALS} value
     * @param authentication {@link Context#SECURITY_AUTHENTICATION} value
     * @throws NamingException if failed to create a context
     */
    Context getJndiContext( String principal, byte[] credential, String authentication, String baseName ) throws NamingException;
}
