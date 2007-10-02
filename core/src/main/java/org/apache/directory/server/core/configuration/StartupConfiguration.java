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
package org.apache.directory.server.core.configuration;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.directory.server.core.authn.AuthenticationService;
import org.apache.directory.server.core.authz.AuthorizationService;
import org.apache.directory.server.core.authz.DefaultAuthorizationService;
import org.apache.directory.server.core.collective.CollectiveAttributeService;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.normalization.NormalizationService;
import org.apache.directory.server.core.exception.ExceptionService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.operational.OperationalAttributeService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.referral.ReferralService;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.trigger.TriggerService;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.Attributes;
import java.io.File;
import java.util.*;


/**
 * A {@link Configuration} that starts up ApacheDS.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartupConfiguration extends Configuration
{
    /** The logger for this class */
    private static final Logger log = LoggerFactory.getLogger( StartupConfiguration.class );

    private static final long serialVersionUID = 4826762196566871677L;

    public static final int MAX_SIZE_LIMIT_DEFAULT = 100;
    public static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    private File workingDirectory = new File( "server-work" );
    private boolean exitVmOnShutdown = true; // allow by default
    private boolean shutdownHookEnabled = true; // allow by default
    private boolean allowAnonymousAccess = true; // allow by default
    private boolean accessControlEnabled; // off by default
    private boolean denormalizeOpAttrsEnabled; // off by default
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; // set to default value
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT; // set to default value (milliseconds)
    private List<Interceptor> interceptors;
    private Partition systemPartition;
    private Set<? extends Partition> partitions = new HashSet<Partition>();
    private List<? extends Entry> testEntries = new ArrayList<Entry>(); // List<Attributes>


    /**
     * Creates a new instance with default settings.
     */
    public StartupConfiguration()
    {
        setDefaultInterceptorConfigurations();
    }


    /**
     * Creates a new instance with default settings that operates on the
     * {@link org.apache.directory.server.core.DirectoryService} with the specified ID.
     */
    public StartupConfiguration(String instanceId)
    {
        setDefaultInterceptorConfigurations();
        setInstanceId( instanceId );
    }

    private void setDefaultInterceptorConfigurations()
    {
        // Set default interceptor chains
        Interceptor interceptorCfg;
        List<Interceptor> list = new ArrayList<Interceptor>();

        list.add( new NormalizationService() );

        list.add( new AuthenticationService() );

        list.add( new ReferralService() );

        list.add( new AuthorizationService() );

        list.add( new DefaultAuthorizationService() );

        list.add( new ExceptionService() );

        list.add( new OperationalAttributeService() );

        list.add( new SchemaService() );

        list.add( new SubentryService() );

        list.add( new CollectiveAttributeService() );

        list.add( new EventService() );

        list.add( new TriggerService() );

        setInterceptors( list );
    }


    /**
     * Returns {@link Partition}s to configure context partitions.
     */
    public Set<? extends Partition> getPartitions()
    {
        Set<Partition> cloned = new HashSet<Partition>();
        cloned.addAll( partitions );
        return cloned;
    }


    /**
     * Sets {@link Partition}s to configure context partitions.
     */
    protected void setPartitions( Set<? extends Partition> contextParitions )
    {
        Set<Partition> cloned = new HashSet<Partition>();
        cloned.addAll( contextParitions );
        Set<String> names = new HashSet<String>();
        Iterator<? extends Partition> i = cloned.iterator();
        while ( i.hasNext() )
        {
            Partition p = i.next();

            String id = p.getId();
            if ( names.contains( id ) )
            {
                throw new ConfigurationException( "Duplicate partition id: " + id );
            }
            names.add( id );
        }

        this.partitions = cloned;
    }


    /**
     * Returns <tt>true</tt> if access control checks are enabled.
     */
    public boolean isAccessControlEnabled()
    {
        return accessControlEnabled;
    }


    /**
     * Sets whether to enable basic access control checks or not
     */
    protected void setAccessControlEnabled( boolean accessControlEnabled )
    {
        this.accessControlEnabled = accessControlEnabled;
    }


    /**
     * Returns <tt>true</tt> if anonymous access is allowed.
     */
    public boolean isAllowAnonymousAccess()
    {
        return allowAnonymousAccess;
    }


    /**
     * Sets whether to allow anonymous access or not
     */
    protected void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {
        this.allowAnonymousAccess = enableAnonymousAccess;
    }


    /**
     * Returns interceptor chain.
     */
    @SuppressWarnings("unchecked")
    public List<Interceptor> getInterceptors()
    {
        List<Interceptor> cloned = new ArrayList<Interceptor>();
        cloned.addAll( interceptors );
        return cloned;
    }


    /**
     * Sets interceptor chain.
     */
    protected void setInterceptors( List<Interceptor> interceptors)
    {

        Set<String> names = new HashSet<String>();
        Iterator i = interceptors.iterator();
        for (Interceptor interceptor : interceptors) {

            String name = interceptor.getName();
            if (names.contains(name)) {
                throw new ConfigurationException("Duplicate interceptor name: " + name);
            }
            names.add(name);
        }

        this.interceptors = interceptors;
    }


    /**
     * Returns test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    public List<Entry> getTestEntries()
    {
        List<Entry> cloned = new ArrayList<Entry>();
        cloned.addAll( testEntries );
        return cloned;
    }


    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    protected void setTestEntries( List<? extends Entry> testEntries )
    {
        List<Entry> cloned = new ArrayList<Entry>();
        cloned.addAll( testEntries );
        this.testEntries = testEntries;
    }


    /**
     * Returns working directory (counterpart of <tt>var/lib</tt>).
     */
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }


    /**
     * Sets working directory (counterpart of <tt>var/lib</tt>).
     */
    protected void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }


    public void validate()
    {
        setWorkingDirectory( workingDirectory );
    }


    protected void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {
        this.shutdownHookEnabled = shutdownHookEnabled;
    }


    public boolean isShutdownHookEnabled()
    {
        return shutdownHookEnabled;
    }


    protected void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {
        this.exitVmOnShutdown = exitVmOnShutdown;
    }


    public boolean isExitVmOnShutdown()
    {
        return exitVmOnShutdown;
    }


    protected void setMaxSizeLimit( int maxSizeLimit )
    {
        this.maxSizeLimit = maxSizeLimit;
    }


    public int getMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    protected void setMaxTimeLimit( int maxTimeLimit )
    {
        this.maxTimeLimit = maxTimeLimit;
    }


    public int getMaxTimeLimit()
    {
        return maxTimeLimit;
    }

    protected void setSystemPartition( Partition systemPartition )
    {
        this.systemPartition = systemPartition;
    }


    public Partition getSystemPartition()
    {
        return systemPartition;
    }


    public boolean isDenormalizeOpAttrsEnabled()
    {
        return denormalizeOpAttrsEnabled;
    }
    
    
    protected void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
    {
        this.denormalizeOpAttrsEnabled = denormalizeOpAttrsEnabled;
    }
}
