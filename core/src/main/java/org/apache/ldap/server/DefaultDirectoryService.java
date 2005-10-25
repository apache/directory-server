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
package org.apache.ldap.server;

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
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.DnParser;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.server.authz.AuthorizationService;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ConfigurationException;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.jndi.DeadContext;
import org.apache.ldap.server.jndi.ServerLdapContext;
import org.apache.ldap.server.partition.DefaultDirectoryPartitionNexus;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default implementation of {@link DirectoryService}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class DefaultDirectoryService extends DirectoryService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultDirectoryService.class );

    private final String instanceId;

    private final DirectoryServiceConfiguration configuration = new DefaultDirectoryServiceConfiguration( this );

    private DirectoryServiceListener serviceListener;
    
    /** the initial context environment that fired up the backend subsystem */
    private Hashtable environment;
    
    /** the configuration */
    private StartupConfiguration startupConfiguration;

    /** the registries for system schema objects */
    private GlobalRegistries globalRegistries;

    /** the root nexus */
    private DefaultDirectoryPartitionNexus partitionNexus;

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
    public DefaultDirectoryService( String instanceId )
    {
        if( instanceId == null )
        {
            throw new NullPointerException( "instanceId" );
        }
        
        this.instanceId = instanceId;
        
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
                    log.warn(
                            "Failed to shut down the directory service: " +
                            DefaultDirectoryService.this.instanceId, e );
                }
            }
        }, "ApacheDS Shutdown Hook (" + instanceId + ')' ) );
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
        environment.remove( Context.SECURITY_PRINCIPAL );
        environment.remove( Context.SECURITY_CREDENTIALS );
        environment.remove( Context.SECURITY_AUTHENTICATION );
        
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

    public synchronized void startup( DirectoryServiceListener listener, Hashtable env ) throws NamingException
    {
        Hashtable envCopy = ( Hashtable ) env.clone();

        if( started )
        {
            return;
        }

        StartupConfiguration cfg = ( StartupConfiguration ) Configuration.toConfiguration( env );
        envCopy.put( Context.PROVIDER_URL, "" );
        
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

        this.environment = envCopy;
        this.startupConfiguration = cfg;
        
        listener.beforeStartup( this );

        initialize();
        firstStart = createBootstrapEntries();
        showSecurityWarnings();
        createTestEntries();
        this.serviceListener = listener;
        started = true;
        listener.afterStartup( this );
    }

    public synchronized void sync() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        serviceListener.beforeSync( this );
        try
        {
            this.partitionNexus.sync();
        }
        finally
        {
            serviceListener.afterSync( this );
        }
    }


    public synchronized void shutdown() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        serviceListener.beforeShutdown( this );
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
            startupConfiguration = null;
            serviceListener.afterShutdown( this );
        }
    }
    
    public String getInstanceId()
    {
        return instanceId;
    }
    
    public DirectoryServiceConfiguration getConfiguration()
    {
        return configuration;
    }
    
    
    public Hashtable getEnvironment()
    {
        return ( Hashtable ) environment.clone();
    }
    
    public DirectoryServiceListener getServiceListener()
    {
        return serviceListener;
    }
    
    public StartupConfiguration getStartupConfiguration()
    {
        return startupConfiguration;
    }
    
    public GlobalRegistries getGlobalRegistries()
    {
        return globalRegistries;
    }

    public DirectoryPartitionNexus getPartitionNexus()
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
            
            if( !startupConfiguration.isAllowAnonymousAccess() )
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
        if ( !partitionNexus.hasEntry( DirectoryPartitionNexus.getAdminName() ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "person" );
            objectClass.add( "organizationalPerson" );
            objectClass.add( "inetOrgPerson" );
            attributes.put( objectClass );

            attributes.put( "uid", DirectoryPartitionNexus.ADMIN_UID );
            attributes.put( "userPassword", DirectoryPartitionNexus.ADMIN_PASSWORD );
            attributes.put( "displayName", "Directory Superuser" );
            attributes.put( "cn", "system administrator" );
            attributes.put( "sn", "administrator" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            attributes.put( "displayName", "Directory Superuser" );
            
            partitionNexus.add( DirectoryPartitionNexus.ADMIN_PRINCIPAL, DirectoryPartitionNexus.getAdminName(), attributes );
        }

        // -------------------------------------------------------------------
        // create system users area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=users,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "users" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
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
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "groups" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=groups,ou=system", new LdapName( "ou=groups,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create administrator group
        // -------------------------------------------------------------------

        String upName = "cn=Administrators,ou=groups,ou=system";
        Name normName = new LdapName( "cn=administrators,ou=groups,ou=system" );
        if ( !partitionNexus.hasEntry( normName ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "groupOfUniqueNames" );
            attributes.put( objectClass );
            attributes.put( "cn", "Administrators" );
            attributes.put( "uniqueMember", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( upName, normName, attributes );
            AuthorizationService authzSrvc = ( AuthorizationService ) interceptorChain.get( "authorizationService" );
            authzSrvc.cacheNewGroup( upName, normName, attributes );
        }

        // -------------------------------------------------------------------
        // create system configuration area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=configuration,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "configuration" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=configuration,ou=system", new LdapName( "ou=configuration,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system configuration area for partition information
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=partitions,ou=configuration,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "partitions" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=partitions,ou=configuration,ou=system",
                    new LdapName( "ou=partitions,ou=configuration,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system configuration area for services
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=services,ou=configuration,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "services" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=services,ou=configuration,ou=system",
                    new LdapName( "ou=services,ou=configuration,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system configuration area for interceptors
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "ou=interceptors,ou=configuration,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "ou", "interceptors" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            partitionNexus.add( "ou=interceptors,ou=configuration,ou=system",
                    new LdapName( "ou=interceptors,ou=configuration,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system preferences area
        // -------------------------------------------------------------------

        if ( !partitionNexus.hasEntry( new LdapName( "prefNodeName=sysPrefRoot,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            Attribute objectClass = new LockableAttributeImpl( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "organizationalUnit" );
            attributes.put( objectClass );

            attributes.put( "objectClass", "extensibleObject" );
            attributes.put( "prefNodeName", "sysPrefRoot" );
            attributes.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            LdapName dn = new LdapName( "prefNodeName=sysPrefRoot,ou=system" );

            partitionNexus.add( "prefNodeName=sysPrefRoot,ou=system", dn, attributes );
        }

        return firstStart;
    }
    
    /**
     * Displays security warning messages if any possible secutiry issue is found.
     */
    private void showSecurityWarnings() throws NamingException
    {
        // Warn if the default password is not changed.
        boolean needToChangeAdminPassword = false;
        
        Attributes adminEntry = partitionNexus.lookup( new LdapName( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) );
        Object userPassword = adminEntry.get( "userPassword" ).get();
        if( userPassword instanceof byte[] )
        {
            needToChangeAdminPassword = DirectoryPartitionNexus.ADMIN_PASSWORD.equals( new String( ( byte[] ) userPassword ) );
        }
        else if ( userPassword.toString().equals( new String( DirectoryPartitionNexus.ADMIN_PASSWORD ) ) )
        {
            needToChangeAdminPassword = DirectoryPartitionNexus.ADMIN_PASSWORD.equals( userPassword.toString() );
        }
        
        if( needToChangeAdminPassword )
        {
            log.warn(
                    "You didn't change the admin password of directory service " +
                    "instance '" + instanceId + "'.  " +
                    "Please update the admin password as soon as possible " +
                    "to prevent a possible security breach." );
        }
    }
    
    private void createTestEntries() throws NamingException
    {
        /*
         * Unfortunately to test non-root user startup of the core and make sure
         * all the appropriate functionality is there we need to load more user
         * entries at startup due to a chicken and egg like problem.  The value
         * of this property is a list of attributes to be added.
         */
        Iterator i = startupConfiguration.getTestEntries().iterator();
        while( i.hasNext() )
        {
            Attributes entry = ( Attributes ) i.next();
            entry.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            entry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            
            Attribute dn = ( Attribute ) entry.get( "dn" ).clone();
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
        loader.load( startupConfiguration.getBootstrapSchemas(), bootstrapRegistries );

        java.util.List errors = bootstrapRegistries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();

            e.setRootCause( ( Throwable ) errors.get( 0 ) );

            throw e;
        }

        globalRegistries = new GlobalRegistries( bootstrapRegistries );
        
        partitionNexus = new DefaultDirectoryPartitionNexus( new LockableAttributesImpl() );
        partitionNexus.init( configuration, null );
        
        interceptorChain = new InterceptorChain();
        interceptorChain.init( configuration );
    }
}
