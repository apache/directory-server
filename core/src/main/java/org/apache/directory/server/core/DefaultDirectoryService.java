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
package org.apache.directory.server.core;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.authz.AuthorizationService;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.jndi.AbstractContextFactory;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.DefaultDirectoryPartitionNexus;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.core.schema.global.GlobalRegistries;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DnOidContainer;
import org.apache.directory.shared.ldap.name.LdapName;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default implementation of {@link DirectoryService}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class DefaultDirectoryService extends DirectoryService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultDirectoryService.class );
    private static final String BINARY_KEY = "java.naming.ldap.attributes.binary";

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
    public DefaultDirectoryService(String instanceId)
    {
        if ( instanceId == null )
        {
            throw new NullPointerException( "instanceId" );
        }
        this.instanceId = instanceId;
    }


    // ------------------------------------------------------------------------
    // BackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------

    public Context getJndiContext( String rootDN ) throws NamingException
    {
        return this.getJndiContext( null, null, "none", rootDN );
    }


    public synchronized Context getJndiContext( String principal, byte[] credential, String authentication,
        String rootDN ) throws NamingException
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

        if ( principal != null )
        {
            environment.put( Context.SECURITY_PRINCIPAL, principal );
        }

        if ( credential != null )
        {
            environment.put( Context.SECURITY_CREDENTIALS, credential );
        }

        if ( authentication != null )
        {
            environment.put( Context.SECURITY_AUTHENTICATION, authentication );
        }

        if ( rootDN == null )
        {
            rootDN = "";
        }
        environment.put( Context.PROVIDER_URL, rootDN );

        return new ServerLdapContext( this, environment );
    }


    public synchronized void startup( DirectoryServiceListener listener, Hashtable env ) throws NamingException
    {
        if ( started )
        {
            return;
        }

        Hashtable envCopy = ( Hashtable ) env.clone();

        StartupConfiguration cfg = ( StartupConfiguration ) Configuration.toConfiguration( env );

        if ( cfg.isShutdownHookEnabled() )
        {
            Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        shutdown();
                    }
                    catch ( NamingException e )
                    {
                        log.warn( "Failed to shut down the directory service: "
                            + DefaultDirectoryService.this.instanceId, e );
                    }
                }
            }, "ApacheDS Shutdown Hook (" + instanceId + ')' ) );

            log.info( "ApacheDS shutdown hook has been registered with the runtime." );
        }
        else if ( log.isWarnEnabled() )
        {
            log.warn( "ApacheDS shutdown hook has NOT been registered with the runtime."
                + "  This default setting for standalone operation has been overriden." );
        }

        envCopy.put( Context.PROVIDER_URL, "" );

        try
        {
            cfg.validate();
        }
        catch ( ConfigurationException e )
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
        this.serviceListener = listener;
        started = true;
        if ( !startupConfiguration.getTestEntries().isEmpty() )
        {
            createTestEntries( env );
        }
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
    private void checkSecuritySettings( String principal, byte[] credential, String authentication )
        throws NamingException
    {
        if ( authentication == null )
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
                throw new LdapConfigurationException( "missing required " + Context.SECURITY_CREDENTIALS
                    + " property for simple authentication" );
            }

            if ( principal == null )
            {
                throw new LdapConfigurationException( "missing required " + Context.SECURITY_PRINCIPAL
                    + " property for simple authentication" );
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
                    + "settings encountered where bind is anonymous yet " + Context.SECURITY_CREDENTIALS
                    + " property is set" );
            }
            if ( principal != null )
            {
                throw new LdapConfigurationException( "ambiguous bind "
                    + "settings encountered where bind is anonymous yet " + Context.SECURITY_PRINCIPAL
                    + " property is set" );
            }

            if ( !startupConfiguration.isAllowAnonymousAccess() )
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
            throw new LdapAuthenticationNotSupportedException( "Unknown authentication type: '" + authentication + "'",
                ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
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

            partitionNexus.add( DirectoryPartitionNexus.ADMIN_PRINCIPAL, DirectoryPartitionNexus.getAdminName(),
                attributes );
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

            partitionNexus.add( "ou=partitions,ou=configuration,ou=system", new LdapName(
                "ou=partitions,ou=configuration,ou=system" ), attributes );
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

            partitionNexus.add( "ou=services,ou=configuration,ou=system", new LdapName(
                "ou=services,ou=configuration,ou=system" ), attributes );
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

            partitionNexus.add( "ou=interceptors,ou=configuration,ou=system", new LdapName(
                "ou=interceptors,ou=configuration,ou=system" ), attributes );
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
        if ( userPassword instanceof byte[] )
        {
            needToChangeAdminPassword = DirectoryPartitionNexus.ADMIN_PASSWORD.equals( new String(
                ( byte[] ) userPassword ) );
        }
        else if ( userPassword.toString().equals( DirectoryPartitionNexus.ADMIN_PASSWORD ) )
        {
            needToChangeAdminPassword = DirectoryPartitionNexus.ADMIN_PASSWORD.equals( userPassword.toString() );
        }

        if ( needToChangeAdminPassword )
        {
            log.warn( "You didn't change the admin password of directory service " + "instance '" + instanceId + "'.  "
                + "Please update the admin password as soon as possible " + "to prevent a possible security breach." );
        }
    }


    private void createTestEntries( Hashtable env ) throws NamingException
    {
        String principal = AbstractContextFactory.getPrincipal( env );
        byte[] credential = AbstractContextFactory.getCredential( env );
        String authentication = AbstractContextFactory.getAuthentication( env );
        ServerLdapContext ctx = ( ServerLdapContext ) getJndiContext( principal, credential, authentication, "" );

        Iterator i = startupConfiguration.getTestEntries().iterator();
        while ( i.hasNext() )
        {
            Attributes entry = ( Attributes ) ( ( Attributes ) i.next() ).clone();
            String dn = ( String ) entry.remove( "dn" ).get();

            try
            {
                ctx.createSubcontext( dn, entry );
            }
            catch ( Exception e )
            {
                log.warn( dn + " test entry already exists.", e );
            }
        }
    }


    private void setupOidsMap( BootstrapRegistries bootstrapRegistries ) throws NamingException
    {
        Iterator keys = bootstrapRegistries.getOidRegistry().getOidByName().keySet().iterator();

        Map oidsMap = new HashMap();
        Map oidName = new HashMap();

        while ( keys.hasNext() )
        {
            String name = StringTools.deepTrimToLower( ( String ) keys.next() );
            String principal = null;

            if ( OID.isOID( name ) )
            {
                continue;
            }

            String oid = bootstrapRegistries.getOidRegistry().getOid( name );

            OidNormalizer oidNormalizer = null;

            if ( oidName.containsKey( oid ) )
            {
                principal = StringTools.deepTrimToLower( ( String ) oidName.get( oid ) );

                if ( principal.length() > name.length() )
                {
                    OidNormalizer oldOidNormalizer = ( OidNormalizer ) oidsMap.get( principal );

                    oidNormalizer = new OidNormalizer( name, oldOidNormalizer.getNormalizer() );

                    oidName.remove( oid );
                    oidName.put( oid, name );
                    oidsMap.remove( principal );
                    oidsMap.remove( oid );
                    oidsMap.put( principal, oidNormalizer );
                    oidsMap.put( name, oidNormalizer );
                    oidsMap.put( oid, oidNormalizer );
                    continue;
                }
            }
            else
            {
                principal = name;
                oidName.put( oid, principal );

                if ( bootstrapRegistries.getNormalizerRegistry().hasNormalizer( oid ) )
                {
                    oidNormalizer = new OidNormalizer( principal, bootstrapRegistries.getNormalizerRegistry().lookup(
                        oid ) );
                }
                else
                {
                    oidNormalizer = new OidNormalizer( principal, new NoOpNormalizer() );
                }

                oidsMap.put( name, oidNormalizer );
                oidsMap.put( oid, oidNormalizer );
            }
        }

        DnOidContainer.setOids( oidsMap );
    }


    /**
     * Kicks off the initialization of the entire system.
     *
     * @throws javax.naming.NamingException if there are problems along the way
     */
    private void initialize() throws NamingException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "---> Initializing the DefaultDirectoryService " );
        }

        // --------------------------------------------------------------------
        // Load the schema here and check that it is ok!
        // --------------------------------------------------------------------

        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        loader.load( startupConfiguration.getBootstrapSchemas(), bootstrapRegistries );
        setupOidsMap( bootstrapRegistries );

        java.util.List errors = bootstrapRegistries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();

            e.setRootCause( ( Throwable ) errors.get( 0 ) );

            throw e;
        }

        globalRegistries = new GlobalRegistries( bootstrapRegistries );
        Set binaries = new HashSet();
        if ( this.environment.containsKey( BINARY_KEY ) )
        {
            if ( log.isInfoEnabled() )
            {
                log.info( "Startup environment contains " + BINARY_KEY );
            }

            String binaryIds = ( String ) this.environment.get( BINARY_KEY );
            if ( binaryIds == null )
            {
                if ( log.isWarnEnabled() )
                {
                    log.warn( BINARY_KEY + " in startup environment contains null value.  "
                        + "Using only schema info to set binary attributeTypes." );
                }
            }
            else
            {
                if ( !StringTools.isEmpty( binaryIds ) )
                {
                    String[] binaryArray = binaryIds.split( " " );

                    for ( int i = 0; i < binaryArray.length; i++ )
                    {
                        binaries.add( StringTools.lowerCase( StringTools.trim( binaryArray[i] ) ) );
                    }
                }

                if ( log.isInfoEnabled() )
                {
                    log.info( "Setting binaries to union of schema defined binaries and those provided in "
                        + BINARY_KEY );
                }
            }
        }

        // now get all the attributeTypes that are binary from the registry
        AttributeTypeRegistry registry = globalRegistries.getAttributeTypeRegistry();
        Iterator list = registry.list();
        while ( list.hasNext() )
        {
            AttributeType type = ( AttributeType ) list.next();
            if ( !type.getSyntax().isHumanReadible() )
            {
                // add the OID for the attributeType
                binaries.add( type.getOid() );

                // add the lowercased name for the names for the attributeType
                String[] names = type.getNames();
                for ( int ii = 0; ii < names.length; ii++ )
                {
                    binaries.add( StringTools.lowerCase( StringTools.trim( names[ii] ) ) );
                }
            }
        }

        this.environment.put( BINARY_KEY, binaries );
        if ( log.isDebugEnabled() )
        {
            log.debug( "binary ids used: " + binaries );
        }

        partitionNexus = new DefaultDirectoryPartitionNexus( new LockableAttributesImpl() );
        partitionNexus.init( configuration, null );

        interceptorChain = new InterceptorChain();
        interceptorChain.init( configuration );

        if ( log.isDebugEnabled() )
        {
            log.debug( "<--- DefaultDirectoryService initialized" );
        }
    }
}
