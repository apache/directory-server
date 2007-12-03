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
import org.apache.directory.server.core.changelog.ChangeLog;
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
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.util.List;
import java.util.Set;


/**
 * Provides JNDI service to {@link AbstractContextFactory}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface DirectoryService
{
    String JNDI_KEY = DirectoryService.class.getName();

    /**
     * Reverts the server's state to an earlier revision.  Note that the revsion number
     * still increases to revert back even though the state reverted to is the same.
     * Note that implementations may lock the server from making changes or searching
     * the directory until this operation has completed.
     *
     * @param revision the revision number to revert to
     * @return the new revision reached by applying all changes needed to revert to the
     * original state
     * @throws NamingException if there are problems reverting back to the earlier state
     * @throws IllegalArgumentException if the revision provided is greater than the current
     * revision or less than 0
     * @throws UnsupportedOperationException if this feature is not supported by the
     * change log
     */
    long revert( long revision ) throws NamingException;


    /**
     * Reverts the server's state to the latest tagged snapshot if one was taken.  If
     * there is no tag a illegal state exception will result.  If the latest revision
     * is not earlier than the current revision (both are same), then no changes were
     * made to the directory to be reverted.  In this case we return the current
     * revision and do nothiig loggin the fact that we ignored the request to revert.
     *
     * @return the new revision reached by applying all changes needed to revert
     * to the new state or the same version before this call if no revert actually
     * took place
     * @throws NamingException if there are problems reverting back to the earlier state
     * @throws UnsupportedOperationException if this feature is not supported by the
     * change log
     */
    long revert() throws NamingException;


    PartitionNexus getPartitionNexus();


    InterceptorChain getInterceptorChain();


    void addPartition( Partition partition ) throws NamingException;
    

    void removePartition( Partition partition ) throws NamingException;


    Registries getRegistries();


    void setRegistries( Registries registries );


    SchemaManager getSchemaManager();


    void setSchemaManager( SchemaManager schemaManager );


    /**
     * Starts up this service.
     * 
     * @throws NamingException if failed to start up
     */
    void startup() throws NamingException;


    /**
     * Shuts down this service.
     * 
     * @throws NamingException if failed to shut down
     */
    void shutdown() throws NamingException;


    /**
     * Calls {@link Partition#sync()} for all registered {@link Partition}s.
     * @throws NamingException if synchronization failed
     */
    void sync() throws NamingException;


    /**
     * Returns <tt>true</tt> if this service is started.
     * @return true if the service has started, false otherwise
     */
    boolean isStarted();


    /**
     * Gets a JNDI {@link Context} to the RootDSE as an anonymous user.
     * This bypasses authentication within the server.
     *
     * @return a JNDI context to the RootDSE
     * @throws NamingException if failed to create a context
     */
    LdapContext getJndiContext() throws NamingException;


    /**
     * Gets a JNDI {@link Context} to a specific entry as an anonymous user.
     * This bypasses authentication within the server.
     *
     * @param dn the distinguished name of the entry
     * @return a JNDI context to the entry at the specified DN
     * @throws NamingException if failed to create a context
     */
    LdapContext getJndiContext( String dn ) throws NamingException;


    /**
     * Gets a JNDI {@link Context} to the RootDSE as a specific LDAP user principal.
     * This bypasses authentication within the server.
     *
     * @param principal the user to associate with the context
     * @return a JNDI context to the RootDSE as a specific user
     * @throws NamingException if failed to create a context
     */
    LdapContext getJndiContext( LdapPrincipal principal ) throws NamingException;


    /**
     * Gets a JNDI {@link Context} to a specific entry as a specific LDAP user principal.
     * This bypasses authentication within the server.
     *
     * @param principal the user to associate with the context
     * @param dn the distinguished name of the entry
     * @return a JNDI context to the specified entry as a specific user
     * @throws NamingException if failed to create a context
     */
    LdapContext getJndiContext( LdapPrincipal principal, String dn ) throws NamingException;


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
    LdapContext getJndiContext( LdapDN principalDn, String principal, byte[] credential,
        String authentication, String dn ) throws NamingException;


    void setInstanceId( String instanceId );


    String getInstanceId();


    /**
     * Gets the {@link Partition}s used by this DirectoryService.
     *
     * @return the set of partitions used
     */
    Set<? extends Partition> getPartitions();


    /**
     * Sets {@link Partition}s used by this DirectoryService.
     *
     * @param partitions the partitions to used
     */
    void setPartitions( Set<? extends Partition> partitions );


    /**
     * Returns <tt>true</tt> if access control checks are enabled.
     *
     * @return true if access control checks are enabled, false otherwise
     */
    boolean isAccessControlEnabled();


    /**
     * Sets whether to enable basic access control checks or not.
     *
     * @param accessControlEnabled true to enable access control checks, false otherwise
     */
    void setAccessControlEnabled( boolean accessControlEnabled );


    /**
     * Returns <tt>true</tt> if anonymous access is allowed on entries besides the RootDSE.
     * If the access control subsystem is enabled then access to some entries may not be
     * allowed even when full anonymous access is enabled.
     *
     * @return true if anonymous access is allowed on entries besides the RootDSE, false
     * if anonymous access is allowed to all entries.
     */
    boolean isAllowAnonymousAccess();


    /**
     * Sets whether to allow anonymous access to entries other than the RootDSE.  If the
     * access control subsystem is enabled then access to some entries may not be allowed
     * even when full anonymous access is enabled.
     *
     * @param enableAnonymousAccess true to enable anonymous access, false to disable it
     */
    void setAllowAnonymousAccess( boolean enableAnonymousAccess );


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    List<Interceptor> getInterceptors();


    /**
     * Sets the interceptors in the server.
     *
     * @param interceptors the interceptors to be used in the server.
     */
    void setInterceptors( List<Interceptor> interceptors );


    /**
     * Returns test directory entries({@link Entry}) to be loaded while
     * bootstrapping.
     *
     * @return test entries to load during bootstrapping
     */
    List<Entry> getTestEntries();


    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     *
     * @param testEntries the test entries to load while bootstrapping
     */
    void setTestEntries( List<? extends Entry> testEntries );


    /**
     * Returns working directory (counterpart of <tt>var/lib</tt>) where partitions are
     * stored by default.
     *
     * @return the directory where partition's are stored.
     */
    File getWorkingDirectory();


    /**
     * Sets working directory (counterpart of <tt>var/lib</tt>) where partitions are stored
     * by default.
     *
     * @param workingDirectory the directory where the server's partitions are stored by default.
     */
    void setWorkingDirectory( File workingDirectory );


    /**
     * Sets the shutdown hook flag which controls whether or not this DirectoryService
     * registers a JVM shutdown hook to flush caches and synchronize to disk safely.  This is
     * enabled by default.
     *
     * @param shutdownHookEnabled true to enable the shutdown hook, false to disable
     */
    void setShutdownHookEnabled( boolean shutdownHookEnabled );


    /**
     * Checks to see if this DirectoryService has registered a JVM shutdown hook
     * to flush caches and synchronize to disk safely.  This is enabled by default.
     *
     * @return true if a shutdown hook is registered, false if it is not
     */
    boolean isShutdownHookEnabled();


    void setExitVmOnShutdown( boolean exitVmOnShutdown );


    boolean isExitVmOnShutdown();


    void setMaxSizeLimit( int maxSizeLimit );


    int getMaxSizeLimit();


    void setMaxTimeLimit( int maxTimeLimit );


    int getMaxTimeLimit();


    void setSystemPartition( Partition systemPartition );


    Partition getSystemPartition();


    boolean isDenormalizeOpAttrsEnabled();


    void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled );


    /**
     * Gets the ChangeLog service for this DirectoryService used for tracking
     * changes (revisions) to the server and using them to revert the server
     * to earier revisions.
     *
     * @return the change log service
     */
    ChangeLog getChangeLog();


    /**
     * Sets the ChangeLog service for this DirectoryService used for tracking
     * changes (revisions) to the server and using them to revert the server
     * to earier revisions.
     *
     * @param changeLog the change log service to set
     */
    void setChangeLog( ChangeLog changeLog );
}
