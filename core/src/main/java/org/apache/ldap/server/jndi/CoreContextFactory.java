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
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.spi.InitialContextFactory;

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
import org.apache.ldap.server.ApplicationPartition;
import org.apache.ldap.server.ContextPartition;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.SystemPartition;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.configuration.SyncConfiguration;
import org.apache.ldap.server.db.Database;
import org.apache.ldap.server.db.DefaultSearchEngine;
import org.apache.ldap.server.db.ExpressionEnumerator;
import org.apache.ldap.server.db.ExpressionEvaluator;
import org.apache.ldap.server.db.SearchEngine;
import org.apache.ldap.server.db.jdbm.JdbmDatabase;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ConcreteNameComponentNormalizer;
import org.apache.ldap.server.schema.GlobalRegistries;
import org.apache.ldap.server.schema.MatchingRuleRegistry;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;


/**
 * A server-side provider implementation of a InitialContextFactory.  Can be
 * utilized via JNDI API in the standard fashion:
 *
 * <code>
 * Hashtable env = new Hashtable();
 * env.put( Context.PROVIDER_URL, "ou=system" );
 * env.put(
 * Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );
 * InitialContext initialContext = new InitialContext( env );
 * </code>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 * @see javax.naming.spi.InitialContextFactory
 */
public class CoreContextFactory implements InitialContextFactory
{
    /*
     * @todo this class needs to be better broken down - its in disarray; too much
     * fuctionality in one place which can be better organized
     */

    /** shorthand reference to the authentication type property */
    private static final String TYPE = Context.SECURITY_AUTHENTICATION;

    /** shorthand reference to the authentication credentials property */
    private static final String CREDS = Context.SECURITY_CREDENTIALS;

    /** shorthand reference to the authentication principal property */
    protected static final String PRINCIPAL = Context.SECURITY_PRINCIPAL;

    /** shorthand reference to the admin principal name */
    protected static final String ADMIN = SystemPartition.ADMIN_PRINCIPAL;

    /** shorthand reference to the admin principal distinguished name */
    protected static final Name ADMIN_NAME = SystemPartition.getAdminDn();

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    /** The singleton JndiProvider instance */
    protected JndiProvider provider = null;

    /** the initial context environment that fired up the backend subsystem */
    protected Hashtable initialEnv;
    
    /** the configuration */
    protected StartupConfiguration configuration;

    /** the system partition used by the context factory */
    protected SystemPartition system;

    /** the registries for system schema objects */
    protected GlobalRegistries globalRegistries;

    /** the root nexus */
    protected RootNexus nexus;

    /** whether or not server is started for the first time */
    protected boolean createMode;


    /**
     * Default constructor that sets the provider of this ServerContextFactory.
     */
    public CoreContextFactory()
    {
        JndiProvider.setProviderOn( this );
    }


    /**
     * Enables this ServerContextFactory with a handle to the JndiProvider singleton.
     *
     * @param provider the system's singleton BackendSubsystem service.
     */
    void setProvider( JndiProvider provider )
    {
        this.provider = provider;
    }


    public Context getInitialContext( Hashtable env ) throws NamingException
    {

        Configuration cfg = Configuration.toConfiguration( env );
        
        Context ctx;
        if( cfg instanceof ShutdownConfiguration )
        {
            if ( this.provider == null )
            {
                return new DeadContext();
            }

            try
            {
                this.provider.shutdown();
                return new DeadContext();
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
            }
            finally
            {
                provider = null;
                initialEnv = null;
                configuration = null;
            }
        }
        else if( cfg instanceof SyncConfiguration )
        {
            if ( this.provider == null )
            {
                return new DeadContext();
            }
            
            provider.sync();
            return provider.getLdapContext( env );
        }
        
        StartupConfiguration startupCfg = ( StartupConfiguration ) cfg;

        checkSecuritySettings( env );

        if ( isAnonymous( env ) )
        {
            env.put( PRINCIPAL, "" );
        }

        // fire up the backend subsystem if we need to
        if ( null == provider )
        {
            // we need to check this here instead of in AuthenticationService
            // because otherwise we are going to start up the system incorrectly
            if ( isAnonymous( env ) && !startupCfg.isAllowAnonymousAccess() )
            {
                throw new LdapNoPermissionException(
                        "ApacheDS is configured to disallow anonymous access" );
            }

            startupCfg.validate();
            this.initialEnv = env;
            this.configuration = startupCfg;
            initialize();

            createMode = createBootstrapEntries();

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
                entry.put( "creatorsName", ADMIN );
                entry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
                
                Attribute dn = entry.remove( "dn" );
                AttributeTypeRegistry registry = globalRegistries.getAttributeTypeRegistry();
                NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( registry );
                DnParser parser = new DnParser( ncn );
                Name ndn = parser.parse( ( String ) dn.get() );
                
                nexus.add( ( String ) dn.get(), ndn, entry );
            }
        }

        ctx = ( ServerContext ) provider.getLdapContext( env );

        return ctx;
    }


    /**
     * Checks to make sure security environment parameters are set correctly.
     *
     * @throws javax.naming.NamingException if the security settings are not correctly configured.
     */
    protected void checkSecuritySettings( Hashtable env ) throws NamingException
    {
        if ( env.containsKey( TYPE ) && env.get( TYPE ) != null )
        {
            /*
             * If bind is simple make sure we have the credentials and the
             * principal name set within the environment, otherwise complain
             */
            if ( env.get( TYPE ).equals( "simple" ) )
            {
                if ( !env.containsKey( CREDS ) )
                {
                    throw new LdapConfigurationException( "missing required "
                            + CREDS + " property for simple authentication" );
                }

                if ( !env.containsKey( PRINCIPAL ) )
                {
                    throw new LdapConfigurationException( "missing required "
                            + PRINCIPAL + " property for simple authentication" );
                }
            }
            /*
             * If bind is none make sure credentials and the principal
             * name are NOT set within the environment, otherwise complain
             */
            else if ( env.get( TYPE ).equals( "none" ) )
            {
                if ( env.containsKey( CREDS ) )
                {
                    throw new LdapConfigurationException( "ambiguous bind "
                            + "settings encountered where bind is anonymous yet "
                            + CREDS + " property is set" );
                }
                if ( env.containsKey( PRINCIPAL ) )
                {
                    throw new LdapConfigurationException( "ambiguous bind "
                            + "settings encountered where bind is anonymous yet "
                            + PRINCIPAL + " property is set" );
                }
            }
            /*
             * If bind is anything other than simple or none we need to
             * complain because SASL is not a supported auth method yet
             */
            else
            {
                throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
            }
        }
        else if ( env.containsKey( CREDS ) )
        {
            if ( !env.containsKey( PRINCIPAL ) )
            {
                throw new LdapConfigurationException( "credentials provided "
                        + "without principal name property: " + PRINCIPAL );
            }
        }
    }


    /**
     * Checks to see if an anonymous bind is being attempted.
     *
     * @return true if bind is anonymous, false otherwise
     */
    protected boolean isAnonymous( Hashtable env )
    {

        if ( env.containsKey( TYPE ) && env.get( TYPE ) != null )
        {
            if ( env.get( TYPE ).equals( "none" ) )
            {
                return true;
            }

            return false;
        }

        if ( env.containsKey( CREDS ) )
        {
            return false;
        }

        return true;
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
        boolean isFirstStart = false;

        // -------------------------------------------------------------------
        // create admin entry
        // -------------------------------------------------------------------

        /*
         * If the admin entry is there, then the database was already created
         */
        if ( nexus.hasEntry( ADMIN_NAME ) )
        {
            isFirstStart = false;
        }
        else
        {
            isFirstStart = true;

            Attributes attributes = new LockableAttributesImpl();

            attributes.put( "objectClass", "top" );

            attributes.put( "objectClass", "person" );

            attributes.put( "objectClass", "organizationalPerson" );

            attributes.put( "objectClass", "inetOrgPerson" );

            attributes.put( "uid", SystemPartition.ADMIN_UID );

            attributes.put( "userPassword", SystemPartition.ADMIN_PW );

            attributes.put( "displayName", "Directory Superuser" );

            attributes.put( "creatorsName", ADMIN );

            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            attributes.put( "displayName", "Directory Superuser" );

            nexus.add( ADMIN, ADMIN_NAME, attributes );
        }

        // -------------------------------------------------------------------
        // create system users area
        // -------------------------------------------------------------------

        if ( nexus.hasEntry( new LdapName( "ou=users,ou=system" ) ) )
        {
            isFirstStart = false;
        }
        else
        {
            isFirstStart = true;

            Attributes attributes = new LockableAttributesImpl();

            attributes.put( "objectClass", "top" );

            attributes.put( "objectClass", "organizationalUnit" );

            attributes.put( "ou", "users" );

            attributes.put( "creatorsName", ADMIN );

            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            nexus.add( "ou=users,ou=system", new LdapName( "ou=users,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system groups area
        // -------------------------------------------------------------------

        if ( nexus.hasEntry( new LdapName( "ou=groups,ou=system" ) ) )
        {
            isFirstStart = false;
        }
        else
        {
            isFirstStart = true;

            Attributes attributes = new LockableAttributesImpl();

            attributes.put( "objectClass", "top" );

            attributes.put( "objectClass", "organizationalUnit" );

            attributes.put( "ou", "groups" );

            attributes.put( "creatorsName", ADMIN );

            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            nexus.add( "ou=groups,ou=system", new LdapName( "ou=groups,ou=system" ), attributes );
        }

        // -------------------------------------------------------------------
        // create system preferences area
        // -------------------------------------------------------------------

        if ( nexus.hasEntry( new LdapName( "prefNodeName=sysPrefRoot,ou=system" ) ) )
        {
            isFirstStart = false;
        }
        else
        {
            isFirstStart = true;

            Attributes attributes = new LockableAttributesImpl();

            attributes.put( "objectClass", "top" );

            attributes.put( "objectClass", "prefNode" );

            attributes.put( "objectClass", "extensibleObject" );

            attributes.put( "prefNodeName", "sysPrefRoot" );

            attributes.put( "creatorsName", ADMIN );

            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );

            LdapName dn = new LdapName( "prefNodeName=sysPrefRoot,ou=system" );

            nexus.add( "prefNodeName=sysPrefRoot,ou=system", dn, attributes );
        }

        return isFirstStart;
    }


    /**
     * Kicks off the initialization of the entire system.
     *
     * @throws javax.naming.NamingException if there are problems along the way
     */
    protected void initialize() throws NamingException
    {
        // --------------------------------------------------------------------
        // Load the schema here and check that it is ok!
        // --------------------------------------------------------------------

        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();

        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        loader.load( configuration.getBootstrapSchemas(), bootstrapRegistries );

        List errors = bootstrapRegistries.checkRefInteg();

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

        Database db = new JdbmDatabase( suffix, suffix, workDir.getPath() );

        AttributeTypeRegistry attributeTypeRegistry;

        attributeTypeRegistry = bootstrapRegistries .getAttributeTypeRegistry();

        OidRegistry oidRegistry;

        oidRegistry = bootstrapRegistries.getOidRegistry();

        ExpressionEvaluator evaluator;

        evaluator = new ExpressionEvaluator( db, oidRegistry, attributeTypeRegistry );

        ExpressionEnumerator enumerator;

        enumerator = new ExpressionEnumerator( db, attributeTypeRegistry, evaluator );

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

        system = new SystemPartition( db, eng, attributes );
        globalRegistries = new GlobalRegistries( system, bootstrapRegistries );
        nexus = new RootNexus( system, new LockableAttributesImpl() );
        provider = new JndiProvider( nexus );

        // --------------------------------------------------------------------
        // Adding interceptors
        // --------------------------------------------------------------------
        InterceptorChain interceptor = configuration.getInterceptors();
        interceptor.init( new InterceptorContext( configuration, system, globalRegistries, nexus ) );

        provider.setInterceptor( interceptor );

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
    protected void startUpAppPartitions() throws NamingException
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
            Database db = new JdbmDatabase( upSuffix, normSuffix, partitionWorkDir.getPath() );

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
                nexus.register( partition );
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
