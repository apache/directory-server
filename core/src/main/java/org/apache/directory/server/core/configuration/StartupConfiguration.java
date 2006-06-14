/*
 *   @(#) $Id$
 *
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
package org.apache.directory.server.core.configuration;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.AnonymousAuthenticator;
import org.apache.directory.server.core.authn.AuthenticationService;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.apache.directory.server.core.authz.AuthorizationService;
import org.apache.directory.server.core.authz.DefaultAuthorizationService;
import org.apache.directory.server.core.collective.CollectiveAttributeService;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.exception.ExceptionService;
import org.apache.directory.server.core.normalization.NormalizationService;
import org.apache.directory.server.core.operational.OperationalAttributeService;
import org.apache.directory.server.core.referral.ReferralService;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.server.core.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchema;
import org.apache.directory.server.core.schema.bootstrap.CollectiveSchema;
import org.apache.directory.server.core.schema.bootstrap.CoreSchema;
import org.apache.directory.server.core.schema.bootstrap.CosineSchema;
import org.apache.directory.server.core.schema.bootstrap.InetorgpersonSchema;
import org.apache.directory.server.core.schema.bootstrap.JavaSchema;
import org.apache.directory.server.core.schema.bootstrap.SystemSchema;
import org.apache.directory.server.core.subtree.SubentryService;
import org.apache.directory.server.core.trigger.TriggerService;
import org.apache.directory.shared.ldap.ldif.Entry;


/**
 * A {@link Configuration} that starts up ApacheDS.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartupConfiguration extends Configuration
{
    private static final long serialVersionUID = 4826762196566871677L;

    private File workingDirectory = new File( "server-work" );
    private boolean exitVmOnShutdown = true; // allow by default
    private boolean shutdownHookEnabled = true; // allow by default
    private boolean allowAnonymousAccess = true; // allow by default
    private boolean accessControlEnabled = false; // turn off by default
    private Set authenticatorConfigurations; // Set<AuthenticatorConfiguration>
    private List interceptorConfigurations; // Set<InterceptorConfiguration>

    private Set bootstrapSchemas; // Set<BootstrapSchema>
    private Set contextPartitionConfigurations = new HashSet(); // Set<ContextPartitionConfiguration>
    private List testEntries = new ArrayList(); // List<Attributes>


    /**
     * Creates a new instance with default settings.
     */
    public StartupConfiguration()
    {
        setDefaultAuthenticatorConfigurations();
        setDefaultBootstrapSchemas();
        setDefaultInterceptorConfigurations();
    }


    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public StartupConfiguration(String instanceId)
    {
        setDefaultAuthenticatorConfigurations();
        setDefaultBootstrapSchemas();
        setDefaultInterceptorConfigurations();
        setInstanceId( instanceId );
    }


    private void setDefaultAuthenticatorConfigurations()
    {
        Set set;

        // Set default authenticator configurations
        set = new HashSet();

        MutableAuthenticatorConfiguration authCfg;

        // Anonymous
        authCfg = new MutableAuthenticatorConfiguration();
        authCfg.setName( "Anonymous" );
        authCfg.setAuthenticator( new AnonymousAuthenticator() );
        set.add( authCfg );

        // Simple
        authCfg = new MutableAuthenticatorConfiguration();
        authCfg.setName( "Simple" );
        authCfg.setAuthenticator( new SimpleAuthenticator() );
        set.add( authCfg );

        setAuthenticatorConfigurations( set );
    }


    private void setDefaultBootstrapSchemas()
    {
        Set set;
        // Set default bootstrap schemas
        set = new HashSet();

        set.add( new CoreSchema() );
        set.add( new CosineSchema() );
        set.add( new ApacheSchema() );
        set.add( new InetorgpersonSchema() );
        set.add( new JavaSchema() );
        set.add( new SystemSchema() );
        set.add( new CollectiveSchema() );

        setBootstrapSchemas( set );
    }


    private void setDefaultInterceptorConfigurations()
    {
        // Set default interceptor chains
        InterceptorConfiguration interceptorCfg;
        List list = new ArrayList();

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "normalizationService" );
        interceptorCfg.setInterceptor( new NormalizationService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "authenticationService" );
        interceptorCfg.setInterceptor( new AuthenticationService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( ReferralService.NAME );
        interceptorCfg.setInterceptor( new ReferralService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "authorizationService" );
        interceptorCfg.setInterceptor( new AuthorizationService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "defaultAuthorizationService" );
        interceptorCfg.setInterceptor( new DefaultAuthorizationService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "exceptionService" );
        interceptorCfg.setInterceptor( new ExceptionService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "schemaService" );
        interceptorCfg.setInterceptor( new SchemaService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "subentryService" );
        interceptorCfg.setInterceptor( new SubentryService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "operationalAttributeService" );
        interceptorCfg.setInterceptor( new OperationalAttributeService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "collectiveAttributeService" );
        interceptorCfg.setInterceptor( new CollectiveAttributeService() );
        list.add( interceptorCfg );

        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "eventService" );
        interceptorCfg.setInterceptor( new EventService() );
        list.add( interceptorCfg );
        
        interceptorCfg = new MutableInterceptorConfiguration();
        interceptorCfg.setName( "triggerService" );
        interceptorCfg.setInterceptor( new TriggerService() );
        list.add( interceptorCfg );

        setInterceptorConfigurations( list );
    }


    /**
     * Returns {@link AuthenticatorConfiguration}s to use for authenticating clients.
     */
    public Set getAuthenticatorConfigurations()
    {
        return ConfigurationUtil.getClonedSet( authenticatorConfigurations );
    }


    /**
     * Sets {@link AuthenticatorConfiguration}s to use for authenticating clients.
     */
    protected void setAuthenticatorConfigurations( Set authenticatorConfigurations )
    {
        Set newSet = ConfigurationUtil.getTypeSafeSet( authenticatorConfigurations, AuthenticatorConfiguration.class );

        Set names = new HashSet();
        Iterator i = newSet.iterator();
        while ( i.hasNext() )
        {
            AuthenticatorConfiguration cfg = ( AuthenticatorConfiguration ) i.next();
            cfg.validate();

            String name = cfg.getName();
            if ( names.contains( name ) )
            {
                throw new ConfigurationException( "Duplicate authenticator name: " + name );
            }
            names.add( name );
        }

        this.authenticatorConfigurations = newSet;
    }


    /**
     * Returns {@link BootstrapSchema}s to load while bootstrapping.
     */
    public Set getBootstrapSchemas()
    {
        return ConfigurationUtil.getClonedSet( bootstrapSchemas );
    }


    /**
     * Sets {@link BootstrapSchema}s to load while bootstrapping.
     */
    protected void setBootstrapSchemas( Set bootstrapSchemas )
    {
        this.bootstrapSchemas = ConfigurationUtil.getTypeSafeSet( bootstrapSchemas, BootstrapSchema.class );
    }


    /**
     * Returns {@link DirectoryPartitionConfiguration}s to configure context partitions.
     */
    public Set getContextPartitionConfigurations()
    {
        return ConfigurationUtil.getClonedSet( contextPartitionConfigurations );
    }


    /**
     * Sets {@link DirectoryPartitionConfiguration}s to configure context partitions.
     */
    protected void setContextPartitionConfigurations( Set contextParitionConfigurations )
    {
        Set newSet = ConfigurationUtil.getTypeSafeSet( contextParitionConfigurations,
            DirectoryPartitionConfiguration.class );

        Set names = new HashSet();
        Iterator i = newSet.iterator();
        while ( i.hasNext() )
        {
            DirectoryPartitionConfiguration cfg = ( DirectoryPartitionConfiguration ) i.next();
            cfg.validate();

            String name = cfg.getName();
            if ( names.contains( name ) )
            {
                throw new ConfigurationException( "Duplicate partition name: " + name );
            }
            names.add( name );
        }

        this.contextPartitionConfigurations = newSet;
    }


    /**
     * Returns <tt>true</tt> if access control checks are enbaled.
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
    public List getInterceptorConfigurations()
    {
        return ConfigurationUtil.getClonedList( interceptorConfigurations );
    }


    /**
     * Sets interceptor chain.
     */
    protected void setInterceptorConfigurations( List interceptorConfigurations )
    {
        List newList = ConfigurationUtil.getTypeSafeList( interceptorConfigurations, InterceptorConfiguration.class );

        Set names = new HashSet();
        Iterator i = newList.iterator();
        while ( i.hasNext() )
        {
            InterceptorConfiguration cfg = ( InterceptorConfiguration ) i.next();
            cfg.validate();

            String name = cfg.getName();
            if ( names.contains( name ) )
            {
                throw new ConfigurationException( "Duplicate interceptor name: " + name );
            }
            names.add( name );
        }

        this.interceptorConfigurations = interceptorConfigurations;
    }


    /**
     * Returns test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    public List getTestEntries()
    {
    	try
    	{
    		return ConfigurationUtil.getClonedAttributesList( testEntries );
    	}
    	catch ( CloneNotSupportedException cnse )
    	{
    		return null;
    	}
    }


    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    protected void setTestEntries( List testEntries )
    {
    	try
    	{
	        testEntries = ConfigurationUtil.getClonedAttributesList( ConfigurationUtil.getTypeSafeList( testEntries,
	            Entry.class ) );
	
	        this.testEntries = testEntries;
    	}
    	catch ( CloneNotSupportedException cnse )
    	{
    		this.testEntries = null;
    	}
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
}
