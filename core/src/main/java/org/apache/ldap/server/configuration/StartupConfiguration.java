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
package org.apache.ldap.server.configuration;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.Attributes;

import org.apache.ldap.server.authn.AnonymousAuthenticator;
import org.apache.ldap.server.authn.SimpleAuthenticator;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.schema.bootstrap.ApacheSchema;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchema;
import org.apache.ldap.server.schema.bootstrap.CoreSchema;
import org.apache.ldap.server.schema.bootstrap.CosineSchema;
import org.apache.ldap.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.ldap.server.schema.bootstrap.JavaSchema;
import org.apache.ldap.server.schema.bootstrap.SystemSchema;
import org.apache.mina.registry.ServiceRegistry;
import org.apache.mina.registry.SimpleServiceRegistry;

/**
 * A {@link Configuration} that starts up ApacheDS.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StartupConfiguration extends Configuration
{
    private static final long serialVersionUID = 4826762196566871677L;

    protected File workingDirectory = new File( "server-work" );
    protected boolean allowAnonymousAccess;
    protected Set authenticatorConfigurations; // Set<AuthenticatorConfiguration>
    protected InterceptorChain interceptors = InterceptorChain.newDefaultChain();
    protected ServiceRegistry minaServiceRegistry = new SimpleServiceRegistry();
    protected int ldapPort = 389;
    protected int ldapsPort = 636;
    protected boolean enableKerberos;
    
    protected Set bootstrapSchemas; // Set<BootstrapSchema>
    protected Set contextPartitionConfigurations = new HashSet(); // Set<ContextPartitionConfiguration>
    protected Set testEntries = new HashSet(); // Set<Attributes>
    
    protected StartupConfiguration()
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
        
        // Set default bootstrap schemas
        set = new HashSet();
        
        set.add( new CoreSchema() );
        set.add( new CosineSchema() );        
        set.add( new ApacheSchema() );        
        set.add( new InetorgpersonSchema() );        
        set.add( new JavaSchema() );        
        set.add( new SystemSchema() );
        
        setBootstrapSchemas( set );
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
        Set newSet = ConfigurationUtil.getTypeSafeSet(
                authenticatorConfigurations, AuthenticatorConfiguration.class );
        
        Iterator i = newSet.iterator();
        while( i.hasNext() )
        {
            ( ( AuthenticatorConfiguration ) i.next() ).validate();
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
        this.bootstrapSchemas = ConfigurationUtil.getTypeSafeSet(
                bootstrapSchemas, BootstrapSchema.class );
    }

    /**
     * Returns {@link ContextPartitionConfiguration}s to configure context partitions.
     */
    public Set getContextPartitionConfigurations()
    {
        return ConfigurationUtil.getClonedSet( contextPartitionConfigurations );
    }

    /**
     * Sets {@link ContextPartitionConfiguration}s to configure context partitions.
     */
    protected void setContextPartitionConfigurations( Set contextParitionConfigurations )
    {
        Set newSet = ConfigurationUtil.getTypeSafeSet(
                contextParitionConfigurations, ContextPartitionConfiguration.class );
        
        Iterator i = newSet.iterator();
        while( i.hasNext() )
        {
            ( ( ContextPartitionConfiguration ) i.next() ).validate();
        }
        
        this.contextPartitionConfigurations = newSet;
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
     * Returns <tt>true</tt> if Kerberos support is enabled.
     */
    public boolean isEnableKerberos()
    {
        return enableKerberos;
    }

    /**
     * Sets whether to enable Kerberos support or not.
     */
    protected void setEnableKerberos( boolean enableKerberos )
    {
        this.enableKerberos = enableKerberos;
    }

    /**
     * Returns interceptor chain.
     */
    public InterceptorChain getInterceptors()
    {
        return interceptors;
    }

    /**
     * Sets interceptor chain.
     */
    protected void setInterceptors( InterceptorChain interceptors )
    {
        if( interceptors == null )
        {
            throw new ConfigurationException( "Interceptors cannot be null" );
        }
        this.interceptors = interceptors;
    }

    /**
     * Returns LDAP TCP/IP port number to listen to.
     */
    public int getLdapPort()
    {
        return ldapPort;
    }

    /**
     * Sets LDAP TCP/IP port number to listen to.
     */
    protected void setLdapPort( int ldapPort )
    {
        ConfigurationUtil.validatePortNumber( ldapPort );
        this.ldapPort = ldapPort;
    }

    /**
     * Returns LDAPS TCP/IP port number to listen to.
     */
    public int getLdapsPort()
    {
        return ldapsPort;
    }

    /**
     * Sets LDAPS TCP/IP port number to listen to.
     */
    protected void setLdapsPort( int ldapsPort )
    {
        ConfigurationUtil.validatePortNumber( ldapsPort );
        this.ldapsPort = ldapsPort;
    }

    /**
     * Returns <a href="http://directory.apache.org/subprojects/network/">MINA</a>
     * {@link ServiceRegistry} that will be used by ApacheDS.
     */
    public ServiceRegistry getMinaServiceRegistry()
    {
        return minaServiceRegistry;
    }

    /**
     * Sets <a href="http://directory.apache.org/subprojects/network/">MINA</a>
     * {@link ServiceRegistry} that will be used by ApacheDS.
     */
    protected void setMinaServiceRegistry( ServiceRegistry minaServiceRegistry )
    {
        if( interceptors == null )
        {
            throw new ConfigurationException( "MinaServiceRegistry cannot be null" );
        }
        this.minaServiceRegistry = minaServiceRegistry;
    }

    /**
     * Returns test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    public Set getTestEntries()
    {
        return ConfigurationUtil.getClonedSet( testEntries );
    }

    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     */
    protected void setTestEntries( Set testEntries )
    {
        this.testEntries = ConfigurationUtil.getTypeSafeSet(
                testEntries, Attributes.class );
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
        workingDirectory.mkdirs();
        if( !workingDirectory.exists() )
        {
            throw new ConfigurationException( "Working directory '" + workingDirectory + "' doesn't exist." );
        }
        if( !workingDirectory.isDirectory() )
        {
            throw new ConfigurationException( "Working directory '" + workingDirectory + "' is not a directory." );
        }

        this.workingDirectory = workingDirectory;
    }
    
    public void validate()
    {
        setWorkingDirectory( workingDirectory );
    }
}
