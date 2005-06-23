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
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.exception.LdapAuthenticationNotSupportedException;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ConfigurationException;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.partition.DefaultContextPartitionNexus;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;


/**
 * Default implementation of {@link ContextFactoryService}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class DefaultContextFactoryService implements ContextFactoryService, ContextFactoryConfiguration
{
    private ContextFactoryServiceListener listener;
    
    /** the initial context environment that fired up the backend subsystem */
    private Hashtable environment;
    
    /** the configuration */
    private StartupConfiguration configuration;

    /** the registries for system schema objects */
    private GlobalRegistries globalRegistries;

    /** the root nexus */
    private DefaultContextPartitionNexus partitionNexus;

    /** whether or not server is started for the first time */
    private boolean firstStart;

    /** The interceptor (or interceptor chain) for this service */
    private InterceptorChain interceptorChain;
    
    /** whether or not this instance has been shutdown */
    private boolean started = false;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a new instance.
     */
    public DefaultContextFactoryService()
    {
        // Register shutdown hook.
        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
            public void run()
            {
                try
                {
                    shutdown();
                }
                catch( NamingException e )
                {
                    e.printStackTrace();
                }
            }
        }, "ApacheDS Shutdown Hook" ) );
    }

    // ------------------------------------------------------------------------
    // BackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------

    public Context getJndiContext( String rootDN ) throws NamingException
    {
        return this.getJndiContext( null, null, "none", rootDN );
    }

    public synchronized Context getJndiContext( String principal, byte[] credential, String authentication, String rootDN ) throws NamingException
    {
        checkSecuritySettings( principal, credential, authentication );
        
        if ( !started )
        {
            return new DeadContext();
        }
        
        Hashtable environment = getEnvironment();
        if( principal != null )
        {
            environment.put( Context.SECURITY_PRINCIPAL, principal );
        }
        
        if( credential != null )
        {
            environment.put( Context.SECURITY_CREDENTIALS, credential );
        }
        
        if( authentication != null )
        {
            environment.put( Context.SECURITY_AUTHENTICATION, authentication );
        }
        
        if( rootDN == null )
        {
            rootDN = "";
        }
        environment.put( Context.PROVIDER_URL, rootDN );

        return new ServerLdapContext( this, environment );
    }

    public synchronized void startup( ContextFactoryServiceListener listener, Hashtable env ) throws NamingException
    {
        if( started )
        {
            return;
        }

        StartupConfiguration cfg = ( StartupConfiguration ) Configuration.toConfiguration( env );

        env.put( Context.PROVIDER_URL, "" );
        
        try
        {
            cfg.validate();
        }
        catch( ConfigurationException e )
        {
            NamingException ne = new LdapConfigurationException( "Invalid configuration." );
            ne.initCause( e );
            throw ne;
        }

        this.environment = env;
        this.configuration = cfg;
        
        listener.beforeStartup( this );
        try
        {
            initialize();
            firstStart = createBootstrapEntries();
            createTestEntries();
            this.listener = listener;
            started = true;
        }
        finally
        {
            listener.afterStartup( this );
        }
    }

    public synchronized void sync() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        listener.beforeSync( this );
        try
        {
            this.partitionNexus.sync();
        }
        finally
        {
            listener.afterSync( this );
        }
    }


    public synchronized void shutdown() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        listener.beforeShutdown( this );
        try
        {
            this.partitionNexus.sync();
            this.partitionNexus.destroy();
            this.interceptorChain.destroy();
            this.started = false;
        }
        finally
        {
            environment = null;
            interceptorChain = null;
            configuration = null;
            listener.afterShutdown( this );
        }
    }
    
    
    public Hashtable getEnvironment()
    {
        return ( Hashtable ) environment.clone();
    }
    
    public ContextFactoryConfiguration getConfiguration()
    {
        return this;
    }
    
    public StartupConfiguration getStartupConfiguration()
    {
        return configuration;
    }
    
    public GlobalRegistries getGlobalRegistries()
    {
        return globalRegistries;
    }

    public ContextPartitionNexus getPartitionNexus()
    {
        return partitionNexus;
    }
    
    public InterceptorChain getInterceptorChain()
    {
        return interceptorChain;
    }
    
    public boolean isFirstStart()
    {
        return firstStart;
    }
    
    public boolean isStarted()
    {
        return started;
    }
    
    /**
     * Checks to make sure security environment parameters are set correctly.
     *
     * @throws javax.naming.NamingException if the security settings are not correctly configured.
     */
    private void checkSecuritySettings( String principal, byte[] credential, String authentication ) throws NamingException
    {
        if( authentication == null )
        {
            authentication = "";
        }
        
        /*
         * If bind is simple make sure we have the credentials and the
         * principal name set within the environment, otherwise complain
         */
        if ( "simple".equalsIgnoreCase( authentication ) )
        {
            if ( credential == null )
            {
                throw new LdapConfigurationException( "missing required "
                        + Context.SECURITY_CREDENTIALS + " property for simple authentication" );
            }

            if ( principal == null )
            {
                throw new LdapConfigurationException( "missing required "
                        + Context.SECURITY_PRINCIPAL + " property for simple authentication" );
            }
        }
        /*
         * If bind is none make sure credentials and the principal
         * name are NOT set within the environment, otherwise complain
         */
        else if ( "none".equalsIgnoreCase( authentication ) )
        {
            if ( credential != null )
            {
                throw new LdapConfigurationException( "ambiguous bind "
                        + "settings encountered where bind is anonymous yet "
                        + Context.SECURITY_CREDENTIALS + " property is set" );
            }
            if ( principal != null )
            {
                throw new LdapConfigurationException( "ambiguous bind "
                        + "settings encountered where bind is anonymous yet "
                        + Context.SECURITY_PRINCIPAL + " property is set" );
            }
            
            if( !configuration.isAllowAnonymousAccess() )
            {
                throw new LdapNoPermissionException( "Anonymous access disabled." );
            }
        }
        else
        {
            /*
             * If bind is anything other than simple or none we need to
             * complain because SASL is not a supported auth method yet
             */
            throw new LdapAuthenticationNotSupportedException( "Unknown authentication type: '" + authentication + "'", ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
        }
    }


    /**
     * Returns true if we had to create the bootstrap entries on the first
     * start of the server.  Otherwise if all entries exist, meaning none
     * had to be created, then we are not starting for the first time.
     *
     * @throws javax.naming.NamingException
     */
    private boolean createBootstrapEntries() throws NamingException
    {
        boolean firstStart = false;

        // -------------------------------------------------------------------
        // create admin entry
        // -------------------------------------------------------------------

        /*
         * If the admin entry is there, then the database was already created
         */
        if ( !partitionNexus.hasEntry( ContextPartitionNexus.getAdminName() ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "person" );
            attributes.put( "objectClass", "organizationalPerson" );
            attributes.put( "objectClass", "inetOrgPerson" );
            attributes.put( "uid", ContextPartitionNexus.ADMIN_UID );
            attributes.put( "userPassword", ContextPartitionNexus.ADMIN_PW );
            attributes.put( "displayName", "Directory Superuser" );
            attributes.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            attributes.put( "displayName", "Directory Superuser" );
            
            partitionNexus.add( ContextPartitionNexus.ADMIN_PRINCIPAL, ContextPartitionNexus.getAdminName(), attributes );
        }

        // -------------------------------------------------------------------
        // create system users area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=users,ou=system" ) ) )
        {
            firstStart = true;
            
            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "organizationalUnit" );
            attributes.put( "ou", "users" );
            attributes.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=users,ou=system", new LdapName( "ou=users,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system groups area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=groups,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "organizationalUnit" );
            attributes.put( "ou", "groups" );
            attributes.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=groups,ou=system", new LdapName( "ou=groups,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system preferences area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "prefNodeName=sysPrefRoot,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "prefNode" );
            attributes.put( "objectClass", "extensibleObject" );
            attributes.put( "prefNodeName", "sysPrefRoot" );
            attributes.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            LdapName dn = new LdapName( "prefNodeName=sysPrefRoot,ou=system" );

            partitionNexus.add( "prefNodeName=sysPrefRoot,ou=system", dn, attributes );
        }

        return firstStart;
    }


    private void createTestEntries() throws NamingException
    {
        /*
         * Unfortunately to test non-root user startup of the core and make sure
         * all the appropriate functionality is there we need to load more user
         * entries at startup due to a chicken and egg like problem.  The value
         * of this property is a list of attributes to be added.
         */
        Iterator i = configuration.getTestEntries().iterator();
        while( i.hasNext() )
        {
            Attributes entry = ( Attributes ) i.next();
            entry.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL );
            entry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            
            Attribute dn = entry.remove( "dn" );
            AttributeTypeRegistry registry = globalRegistries.getAttributeTypeRegistry();
            NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( registry );
            DnParser parser = new DnParser( ncn );
            Name ndn = parser.parse( ( String ) dn.get() );
            
            partitionNexus.add( ( String ) dn.get(), ndn, entry );
        }
    }

    /**
     * Kicks off the initialization of the entire system.
     *
     * @throws javax.naming.NamingException if there are problems along the way
     */
    private void initialize() throws NamingException
    {
        // --------------------------------------------------------------------
        // Load the schema here and check that it is ok!
        // --------------------------------------------------------------------

        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();

        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        loader.load( configuration.getBootstrapSchemas(), bootstrapRegistries );

        java.util.List errors = bootstrapRegistries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();

            e.setRootCause( ( Throwable ) errors.get( 0 ) );

            throw e;
        }

        globalRegistries = new GlobalRegistries( bootstrapRegistries );
        
        partitionNexus = new DefaultContextPartitionNexus( new LockableAttributesImpl() );
        partitionNexus.init( this, null );
        
        interceptorChain = new InterceptorChain();
        interceptorChain.init( this );
    }
}
