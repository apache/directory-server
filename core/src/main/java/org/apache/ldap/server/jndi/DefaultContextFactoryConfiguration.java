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

import java.io.File;
import java.util.ArrayList;
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
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.partition.ApplicationPartition;
import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.partition.PartitionNexus;
import org.apache.ldap.server.partition.RootNexus;
import org.apache.ldap.server.partition.store.impl.btree.DefaultSearchEngine;
import org.apache.ldap.server.partition.store.impl.btree.ExpressionEnumerator;
import org.apache.ldap.server.partition.store.impl.btree.ExpressionEvaluator;
import org.apache.ldap.server.partition.store.impl.btree.BTreeContextPartition;
import org.apache.ldap.server.partition.store.impl.btree.SearchEngine;
import org.apache.ldap.server.partition.store.impl.btree.jdbm.JdbmBTreeContextPartition;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.MatchingRuleRegistry;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;


/**
 * Provides everything required to {@link AbstractContextFactory}.
 * FIXME Rename to DefaultContextFactoryContext
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class DefaultContextFactoryConfiguration implements ContextFactoryConfiguration
{
    private AbstractContextFactory factory;
    
    /** the initial context environment that fired up the backend subsystem */
    private Hashtable environment;
    
    /** the configuration */
    private StartupConfiguration configuration;

    /** the system partition used by the context factory */
    private SystemPartition systemPartition;

    /** the registries for system schema objects */
    private GlobalRegistries globalRegistries;

    /** the root nexus */
    private RootNexus rootNexus;

    /** whether or not server is started for the first time */
    private boolean firstStart;

    /** The interceptor (or interceptor chain) for this provider */
    private InterceptorChain interceptorChain;
    
    /** PartitionNexus proxy wrapping nexus to inject services */
    private final PartitionNexus proxy = new RootNexusProxy(this);

    /** whether or not this instance has been shutdown */
    private boolean started = false;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Creates a new instance.
     */
    public DefaultContextFactoryConfiguration()
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

        return new ServerLdapContext( proxy, environment );
    }

    public synchronized void startup( AbstractContextFactory factory, Hashtable env ) throws NamingException
    {
        if( started )
        {
            return;
        }

        StartupConfiguration cfg = ( StartupConfiguration ) Configuration.toConfiguration( env );

        env.put( Context.PROVIDER_URL, "" );
        
        cfg.validate();
        this.environment = env;
        this.configuration = cfg;
        
        factory.beforeStartup( this );
        try
        {
            initialize();
            firstStart = createBootstrapEntries();
            createTestEntries();
            this.factory = factory;
            started = true;
        }
        finally
        {
            factory.afterStartup( this );
        }
    }

    public synchronized void sync() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        factory.beforeSync( this );
        try
        {
            this.rootNexus.sync();
        }
        finally
        {
            factory.afterSync( this );
        }
    }


    public synchronized void shutdown() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        factory.beforeShutdown( this );
        try
        {
            this.rootNexus.sync();
            this.rootNexus.destroy();
            this.interceptorChain.destroy();
            this.started = false;
        }
        finally
        {
            environment = null;
            interceptorChain = null;
            configuration = null;
            factory.afterShutdown( this );
        }
    }
    
    
    public Hashtable getEnvironment()
    {
        return ( Hashtable ) environment.clone();
    }
    
    public StartupConfiguration getConfiguration()
    {
        return configuration;
    }
    
    public SystemPartition getSystemPartition()
    {
        return systemPartition;
    }

    public GlobalRegistries getGlobalRegistries()
    {
        return globalRegistries;
    }

    public RootNexus getRootNexus()
    {
        return rootNexus;
    }
    
    public boolean isFirstStart()
    {
        return firstStart;
    }
    
    public boolean isStarted()
    {
        return started;
    }
    
    public Object invoke( Invocation call ) throws NamingException
    {
        if( !started )
        {
            throw new IllegalStateException( "ApacheDS is not started yet." );
        }
        
        interceptorChain.process( call );
        return call.getReturnValue();
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
        if ( !rootNexus.hasEntry( SystemPartition.ADMIN_PRINCIPAL_NAME ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "person" );
            attributes.put( "objectClass", "organizationalPerson" );
            attributes.put( "objectClass", "inetOrgPerson" );
            attributes.put( "uid", SystemPartition.ADMIN_UID );
            attributes.put( "userPassword", SystemPartition.ADMIN_PW );
            attributes.put( "displayName", "Directory Superuser" );
            attributes.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            attributes.put( "displayName", "Directory Superuser" );
            
            rootNexus.add( SystemPartition.ADMIN_PRINCIPAL, SystemPartition.ADMIN_PRINCIPAL_NAME, attributes );
        }

        // -------------------------------------------------------------------
        // create system users area
        // -------------------------------------------------------------------

        if ( !rootNexus.hasEntry( new LdapName( "ou=users,ou=system" ) ) )
        {
            firstStart = true;
            
            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "organizationalUnit" );
            attributes.put( "ou", "users" );
            attributes.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            rootNexus.add( "ou=users,ou=system", new LdapName( "ou=users,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system groups area
        // -------------------------------------------------------------------

        if ( !rootNexus.hasEntry( new LdapName( "ou=groups,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "organizationalUnit" );
            attributes.put( "ou", "groups" );
            attributes.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            rootNexus.add( "ou=groups,ou=system", new LdapName( "ou=groups,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system preferences area
        // -------------------------------------------------------------------

        if ( !rootNexus.hasEntry( new LdapName( "prefNodeName=sysPrefRoot,ou=system" ) ) )
        {
            firstStart = true;

            Attributes attributes = new LockableAttributesImpl();
            attributes.put( "objectClass", "top" );
            attributes.put( "objectClass", "prefNode" );
            attributes.put( "objectClass", "extensibleObject" );
            attributes.put( "prefNodeName", "sysPrefRoot" );
            attributes.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            LdapName dn = new LdapName( "prefNodeName=sysPrefRoot,ou=system" );

            rootNexus.add( "prefNodeName=sysPrefRoot,ou=system", dn, attributes );
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
            entry.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
            entry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            
            Attribute dn = entry.remove( "dn" );
            AttributeTypeRegistry registry = globalRegistries.getAttributeTypeRegistry();
            NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( registry );
            DnParser parser = new DnParser( ncn );
            Name ndn = parser.parse( ( String ) dn.get() );
            
            rootNexus.add( ( String ) dn.get(), ndn, entry );
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

        // --------------------------------------------------------------------
        // Fire up the system partition
        // --------------------------------------------------------------------

        File workDir = configuration.getWorkingDirectory();

        LdapName suffix = new LdapName();
        suffix.add( SystemPartition.SUFFIX );

        BTreeContextPartition db = new JdbmBTreeContextPartition( suffix, suffix, workDir.getPath() );
        AttributeTypeRegistry attributeTypeRegistry = bootstrapRegistries .getAttributeTypeRegistry();
        OidRegistry oidRegistry = bootstrapRegistries.getOidRegistry();
        ExpressionEvaluator evaluator = new ExpressionEvaluator( db, oidRegistry, attributeTypeRegistry );
        ExpressionEnumerator enumerator = new ExpressionEnumerator( db, attributeTypeRegistry, evaluator );
        SearchEngine eng = new DefaultSearchEngine( db, evaluator, enumerator );

        AttributeType[] attributes = new AttributeType[]
        {
            attributeTypeRegistry.lookup( SystemPartition.ALIAS_OID ),
            attributeTypeRegistry.lookup( SystemPartition.EXISTANCE_OID ),
            attributeTypeRegistry.lookup( SystemPartition.HIERARCHY_OID ),
            attributeTypeRegistry.lookup( SystemPartition.NDN_OID ),
            attributeTypeRegistry.lookup( SystemPartition.ONEALIAS_OID ),
            attributeTypeRegistry.lookup( SystemPartition.SUBALIAS_OID ),
            attributeTypeRegistry.lookup( SystemPartition.UPDN_OID )
        };

        systemPartition = new SystemPartition( db, eng, attributes );
        globalRegistries = new GlobalRegistries( systemPartition, bootstrapRegistries );
        rootNexus = new RootNexus( systemPartition, new LockableAttributesImpl() );
        
        interceptorChain = new InterceptorChain( configuration.getInterceptorConfigurations() );
        interceptorChain.init( new InterceptorContext( configuration, systemPartition, globalRegistries, rootNexus ) );

        // fire up the app partitions now!
        startUpAppPartitions();
    }

    /**
     * Starts up all the application partitions that will be attached to naming contexts in the system.  Partition
     * database files are created within a subdirectory immediately under the Eve working directory base.
     *
     * @throws javax.naming.NamingException if there are problems creating and starting these new application
     *                                      partitions
     */
    private void startUpAppPartitions() throws NamingException
    {
        OidRegistry oidRegistry = globalRegistries.getOidRegistry();
        AttributeTypeRegistry attributeTypeRegistry;
        attributeTypeRegistry = globalRegistries.getAttributeTypeRegistry();
        MatchingRuleRegistry reg = globalRegistries.getMatchingRuleRegistry();

        File workDir = configuration.getWorkingDirectory();

        Iterator i = configuration.getContextPartitionConfigurations().iterator();
        while( i.hasNext() )
        {
            ContextPartitionConfiguration cfg = ( ContextPartitionConfiguration ) i.next();
            
            // ----------------------------------------------------------------
            // create working directory under eve directory for app partition
            // ----------------------------------------------------------------

            File partitionWorkDir = new File( workDir.getPath() + File.separator + cfg.getName() );
            partitionWorkDir.mkdirs();

            // ----------------------------------------------------------------
            // create the database/store
            // ----------------------------------------------------------------

            Name upSuffix = new LdapName( cfg.getSuffix() );
            Normalizer dnNorm = reg.lookup( "distinguishedNameMatch" ) .getNormalizer();
            Name normSuffix = new LdapName( ( String ) dnNorm.normalize( cfg.getSuffix() ) );
            BTreeContextPartition db = new JdbmBTreeContextPartition( upSuffix, normSuffix, partitionWorkDir.getPath() );

            // ----------------------------------------------------------------
            // create the search engine using db, enumerators and evaluators
            // ----------------------------------------------------------------

            ExpressionEvaluator evaluator;
            evaluator = new ExpressionEvaluator( db, oidRegistry, attributeTypeRegistry );
            ExpressionEnumerator enumerator;
            enumerator = new ExpressionEnumerator( db, attributeTypeRegistry, evaluator );
            SearchEngine eng = new DefaultSearchEngine( db, evaluator, enumerator );

            // ----------------------------------------------------------------
            // fill up a list with the AttributeTypes for the system indices
            // ----------------------------------------------------------------

            ArrayList attributeTypeList = new ArrayList();
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.ALIAS_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.EXISTANCE_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.HIERARCHY_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.NDN_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.ONEALIAS_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.SUBALIAS_OID ) );
            attributeTypeList.add( attributeTypeRegistry.lookup( SystemPartition.UPDN_OID ) );

            // ----------------------------------------------------------------
            // if user indices are specified add those attribute types as well
            // ----------------------------------------------------------------

            Iterator j = cfg.getIndexedAttributes().iterator();
            while( j.hasNext() )
            {
                String attribute = ( String ) j.next();
                attributeTypeList.add( attributeTypeRegistry
                        .lookup( attribute ) );
            }

            // ----------------------------------------------------------------
            // fire up the appPartition & register it with the nexus
            // ----------------------------------------------------------------

            AttributeType[] indexTypes = ( AttributeType[] ) attributeTypeList
                    .toArray( new AttributeType[attributeTypeList.size()] );

            ContextPartition partition = cfg.getContextPartition();

            if ( partition == null )
            {
                // If custom partition is not defined, use the ApplicationPartion.
                partition = new ApplicationPartition( db, eng, indexTypes );
            }

            // Initialize the partition
            try
            {
                partition.init( upSuffix, normSuffix );
                rootNexus.register( partition );
            }
            catch ( Exception e )
            {
                throw ( NamingException ) new NamingException(
                        "Failed to initialize custom partition." ).initCause( e );
            }

            // ----------------------------------------------------------------
            // add the nexus context entry
            // ----------------------------------------------------------------

            partition.add( cfg.getSuffix(), normSuffix, cfg.getContextEntry() );
        }
    }
}
