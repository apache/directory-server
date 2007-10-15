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
package org.apache.directory.server.core;


import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.jndi.AbstractContextFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.SchemaManager;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import java.io.File;
import java.util.List;
import java.util.Set;


/**
 * Provides JNDI service to {@link AbstractContextFactory}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class DirectoryService
{
    public static final String JNDI_KEY = DirectoryService.class.getName();

    public abstract PartitionNexus getPartitionNexus();

    public abstract InterceptorChain getInterceptorChain();

    public abstract void addPartition( Partition partition ) throws NamingException;
    
    public abstract void removePartition( Partition partition ) throws NamingException;

    public abstract Registries getRegistries();

    public abstract void setRegistries( Registries registries );

    public abstract SchemaManager getSchemaManager();

    public abstract void setSchemaManager( SchemaManager schemaManager );


    /**
     * Starts up this service.
     * 
     * @throws NamingException if failed to start up
     */
    public abstract void startup() throws NamingException;


    /**
     * Shuts down this service.
     * 
     * @throws NamingException if failed to shut down
     */
    public abstract void shutdown() throws NamingException;


    /**
     * Calls {@link Partition#sync()} for all registered {@link Partition}s.
     * @throws NamingException if synchronization failed
     */
    public abstract void sync() throws NamingException;


    /**
     * Returns <tt>true</tt> if this service is started.
     * @return true if the service has started, false otherwise
     */
    public abstract boolean isStarted();


    /**
     * Gets a JNDI {@link Context} to the RootDSE as an anonymous user.
     * This bypasses authentication within the server.
     *
     * @return a JNDI context to the RootDSE
     * @throws NamingException if failed to create a context
     */
    public abstract DirContext getJndiContext() throws NamingException;


    /**
     * Gets a JNDI {@link Context} to a specific entry as an anonymous user.
     * This bypasses authentication within the server.
     *
     * @param dn the distinguished name of the entry
     * @return a JNDI context to the entry at the specified DN
     * @throws NamingException if failed to create a context
     */
    public abstract DirContext getJndiContext( String dn ) throws NamingException;


    /**
     * Gets a JNDI {@link Context} to the RootDSE as a specific LDAP user principal.
     * This bypasses authentication within the server.
     *
     * @param principal the user to associate with the context
     * @return a JNDI context to the RootDSE as a specific user
     * @throws NamingException if failed to create a context
     */
    public abstract DirContext getJndiContext( LdapPrincipal principal ) throws NamingException;


    /**
     * Gets a JNDI {@link Context} to a specific entry as a specific LDAP user principal.
     * This bypasses authentication within the server.
     *
     * @param principal the user to associate with the context
     * @param dn the distinguished name of the entry
     * @return a JNDI context to the specified entry as a specific user
     * @throws NamingException if failed to create a context
     */
    public abstract DirContext getJndiContext( LdapPrincipal principal, String dn ) throws NamingException;


    /**
     * Returns a JNDI {@link Context} with the specified authentication information
     * (<tt>principal</tt>, <tt>credential</tt>, and <tt>authentication</tt>) and
     * <tt>baseName</tt>.
     * 
     * @param principalDn the distinguished name of the bind principal
     * @param principal {@link Context#SECURITY_PRINCIPAL} value
     * @param credential {@link Context#SECURITY_CREDENTIALS} value
     * @param authentication {@link Context#SECURITY_AUTHENTICATION} value
     * @param dn the distinguished name of the entry
     * @return a JNDI context to the specified entry as a specific user
     * @throws NamingException if failed to create a context
     */
    public abstract DirContext getJndiContext( LdapDN principalDn, String principal, byte[] credential,
        String authentication, String dn ) throws NamingException;


    public abstract void setInstanceId( String instanceId );


    public abstract String getInstanceId();


    /**
     * Gets the {@link Partition}s used by this DirectoryService.
     *
     * @return the set of partitions used
     */
    public abstract Set<? extends Partition> getPartitions();


    /**
     * Sets {@link Partition}s used by this DirectoryService.
     *
     * @param partitions the partitions to used
     */
    public abstract void setPartitions( Set<? extends Partition> partitions );


    /**
     * Returns <tt>true</tt> if access control checks are enabled.
     *
     * @return true if access control checks are enabled, false otherwise
     */
    public abstract boolean isAccessControlEnabled();


    /**
     * Sets whether to enable basic access control checks or not.
     *
     * @param accessControlEnabled true to enable access control checks, false otherwise
     */
    public abstract void setAccessControlEnabled( boolean accessControlEnabled );


    /**
     * Returns <tt>true</tt> if anonymous access is allowed on entries besides the RootDSE.
     * If the access control subsystem is enabled then access to some entries may not be
     * allowed even when full anonymous access is enabled.
     *
     * @return true if anonymous access is allowed on entries besides the RootDSE, false
     * if anonymous access is allowed to all entries.
     */
    public abstract boolean isAllowAnonymousAccess();


    /**
     * Sets whether to allow anonymous access to entries other than the RootDSE.  If the
     * access control subsystem is enabled then access to some entries may not be allowed
     * even when full anonymous access is enabled.
     *
     * @param enableAnonymousAccess true to enable anonymous access, false to disable it
     */
    public abstract void setAllowAnonymousAccess( boolean enableAnonymousAccess );


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    public abstract List<Interceptor> getInterceptors();


    /**
     * Sets the interceptors in the server.
     *
     * @param interceptors the interceptors to be used in the server.
     */
    public abstract void setInterceptors( List<Interceptor> interceptors );


    /**
     * Returns test directory entries({@link Entry}) to be loaded while
     * bootstrapping.
     *
     * @return test entries to load during bootstrapping
     */
    public abstract List<Entry> getTestEntries();


    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     *
     * @param testEntries the test entries to load while bootstrapping
     */
    public abstract void setTestEntries( List<? extends Entry> testEntries );


    /**
     * Returns working directory (counterpart of <tt>var/lib</tt>) where partitions are
     * stored by default.
     *
     * @return the directory where partition's are stored.
     */
    public abstract File getWorkingDirectory();


    /**
     * Sets working directory (counterpart of <tt>var/lib</tt>) where partitions are stored
     * by default.
     *
     * @param workingDirectory the directory where the server's partitions are stored by default.
     */
    public abstract void setWorkingDirectory( File workingDirectory );


    public abstract void validate();


    public abstract void setShutdownHookEnabled( boolean shutdownHookEnabled );


    public abstract boolean isShutdownHookEnabled();


    public abstract void setExitVmOnShutdown( boolean exitVmOnShutdown );


    public abstract boolean isExitVmOnShutdown();


    public abstract void setMaxSizeLimit( int maxSizeLimit );


    public abstract int getMaxSizeLimit();


    public abstract void setMaxTimeLimit( int maxTimeLimit );


    public abstract int getMaxTimeLimit();


    public abstract void setSystemPartition( Partition systemPartition );


    public abstract Partition getSystemPartition();


    public abstract boolean isDenormalizeOpAttrsEnabled();


    public abstract void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled );
}
