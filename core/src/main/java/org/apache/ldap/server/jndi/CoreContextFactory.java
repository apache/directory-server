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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.common.exception.LdapAuthenticationNotSupportedException;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.common.util.StringTools;
import org.apache.ldap.server.ApplicationPartition;
import org.apache.ldap.server.ContextPartition;
import org.apache.ldap.server.ContextPartitionConfig;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.SystemPartition;
import org.apache.ldap.server.auth.AbstractAuthenticator;
import org.apache.ldap.server.auth.AnonymousAuthenticator;
import org.apache.ldap.server.auth.AuthenticatorConfig;
import org.apache.ldap.server.auth.AuthenticatorContext;
import org.apache.ldap.server.auth.SimpleAuthenticator;
import org.apache.ldap.server.db.Database;
import org.apache.ldap.server.db.DefaultSearchEngine;
import org.apache.ldap.server.db.ExpressionEnumerator;
import org.apache.ldap.server.db.ExpressionEvaluator;
import org.apache.ldap.server.db.SearchEngine;
import org.apache.ldap.server.db.jdbm.JdbmDatabase;
import org.apache.ldap.server.jndi.ibs.FilterService;
import org.apache.ldap.server.jndi.ibs.FilterServiceImpl;
import org.apache.ldap.server.jndi.ibs.OperationalAttributeService;
import org.apache.ldap.server.jndi.ibs.SchemaService;
import org.apache.ldap.server.jndi.ibs.ServerExceptionService;
import org.apache.ldap.server.jndi.request.interceptor.Interceptor;
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

    /** default path to working directory if WKDIR_ENV property is not set */
    public static final String DEFAULT_WKDIR = "server-work";

    /** default schema classes for the SCHEMAS_ENV property if not set */
    protected static final String[] DEFAULT_SCHEMAS = new String[]
    {
        "org.apache.ldap.server.schema.bootstrap.CoreSchema",
        "org.apache.ldap.server.schema.bootstrap.CosineSchema",
        "org.apache.ldap.server.schema.bootstrap.ApacheSchema",
        "org.apache.ldap.server.schema.bootstrap.InetorgpersonSchema",
        "org.apache.ldap.server.schema.bootstrap.JavaSchema",
        "org.apache.ldap.server.schema.bootstrap.SystemSchema"
    };

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    /** The singleton JndiProvider instance */
    protected JndiProvider provider = null;

    /** the initial context environment that fired up the backend subsystem */
    protected Hashtable initialEnv;

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
        env = ( Hashtable ) env.clone();

        Context ctx = null;

        if ( env.containsKey( EnvKeys.SHUTDOWN ) )
        {
            if ( this.provider == null )
            {
                return new DeadContext();
            }

            try
            {
                this.provider.shutdown();
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
            }
            finally
            {
                ctx = new DeadContext();

                provider = null;

                initialEnv = null;
            }

            return ctx;
        }

        if ( env.containsKey( EnvKeys.SYNC ) )
        {
            provider.sync();

            return provider.getLdapContext( env );
        }

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
            if ( isAnonymous( env ) && env.containsKey( EnvKeys.DISABLE_ANONYMOUS ) )
            {
                throw new LdapNoPermissionException( "cannot bind as anonymous "
                        + "on startup while disabling anonymous binds w/ property: "
                        + EnvKeys.DISABLE_ANONYMOUS );
            }

            this.initialEnv = env;

            initialize();

            createMode = createAdminAccount();
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
     * Returns true if we had to create the admin account since this is the first time we started the server.  Otherwise
     * if the account exists then we are not starting for the first time.
     *
     * @throws javax.naming.NamingException
     */
    protected boolean createAdminAccount() throws NamingException
    {
        /*
         * If the admin entry is there, then the database was already created
         * before so we just need to lookup the userPassword field to see if
         * the password matches.
         */
        if ( nexus.hasEntry( ADMIN_NAME ) )
        {
            return false;
        }

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

        return true;
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

        String[] schemas = DEFAULT_SCHEMAS;

        if ( initialEnv.containsKey( EnvKeys.SCHEMAS ) )
        {
            String schemaList = ( String ) initialEnv.get( EnvKeys.SCHEMAS );

            schemaList = StringTools.deepTrim( schemaList );

            schemas = schemaList.split( " " );

            for ( int ii = 0; ii < schemas.length; ii++ )
            {
                schemas[ii] = schemas[ii].trim();
            }
        }

        loader.load( schemas, bootstrapRegistries );

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

        String wkdir = DEFAULT_WKDIR;

        if ( initialEnv.containsKey( EnvKeys.WKDIR ) )
        {
            wkdir = ( ( String ) initialEnv.get( EnvKeys.WKDIR ) ).trim();
        }

        File wkdirFile = new File( wkdir );

        if ( wkdirFile.isAbsolute() )
        {
            if ( !wkdirFile.exists() )
            {
                throw new NamingException( "working directory " + wkdir + " does not exist" );
            }
        }
        else
        {
            File current = new File( "." );

            mkdirs( current.getAbsolutePath(), wkdir );
        }

        LdapName suffix = new LdapName();

        suffix.add( SystemPartition.SUFFIX );

        Database db = new JdbmDatabase( suffix, suffix, wkdir );

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

        /*
         * Create and add the Authentication service interceptor to before
         * interceptor chain.
         */
        InvocationStateEnum[] state = new InvocationStateEnum[]{InvocationStateEnum.PREINVOCATION};

        boolean allowAnonymous = !initialEnv.containsKey( EnvKeys.DISABLE_ANONYMOUS );

        AuthenticationService authenticationService = new AuthenticationService();

        // create authenticator context
        AuthenticatorContext authenticatorContext = new AuthenticatorContext();
        authenticatorContext.setRootNexus( nexus );
        authenticatorContext.setAllowAnonymous( allowAnonymous );

        try // initialize default authenticators
        {
            // create anonymous authenticator
            AuthenticatorConfig authenticatorConfig = new AuthenticatorConfig();
            authenticatorConfig.setAuthenticatorName( "none" );
            authenticatorConfig.setAuthenticatorContext( authenticatorContext );

            AbstractAuthenticator authenticator = new AnonymousAuthenticator();
            authenticator.init( authenticatorConfig );
            authenticationService.register( authenticator );

            // create simple authenticator
            authenticatorConfig = new AuthenticatorConfig();
            authenticatorConfig.setAuthenticatorName( "simple" );
            authenticatorConfig.setAuthenticatorContext( authenticatorContext );

            authenticator = new SimpleAuthenticator();
            authenticator.init( authenticatorConfig );
            authenticationService.register( authenticator );
        }
        catch ( Exception e )
        {
            throw new NamingException( e.getMessage() );
        }

        AuthenticatorConfig[] configs = null;
        configs = AuthenticatorConfigBuilder
                .getAuthenticatorConfigs( initialEnv );

        for ( int ii = 0; ii < configs.length; ii++ )
        {
            try
            {
                configs[ii].setAuthenticatorContext( authenticatorContext );

                String authenticatorClass = configs[ii].getAuthenticatorClass();
                Class clazz = Class.forName( authenticatorClass );
                Constructor constructor = clazz.getConstructor( new Class[] { } );

                AbstractAuthenticator authenticator = ( AbstractAuthenticator ) constructor.newInstance( new Object[] { } );
                authenticator.init( configs[ii] );

                authenticationService.register( authenticator );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        provider.addInterceptor( authenticationService, state );

        /*
         * Create and add the Eve Exception service interceptor to both the
         * before and onError interceptor chains.
         */
        state = new InvocationStateEnum[]{InvocationStateEnum.POSTINVOCATION};

        FilterService filterService = new FilterServiceImpl();

        Interceptor interceptor = ( Interceptor ) filterService;

        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Authorization service interceptor to before
         * interceptor chain.
         */
        state = new InvocationStateEnum[]{InvocationStateEnum.PREINVOCATION};

        ConcreteNameComponentNormalizer normalizer;

        AttributeTypeRegistry atr = globalRegistries.getAttributeTypeRegistry();

        normalizer = new ConcreteNameComponentNormalizer( atr );

        interceptor = new AuthorizationService( normalizer, filterService );

        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve Exception service interceptor to both the
         * before and onError interceptor chains.
         */
        state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION, InvocationStateEnum.FAILUREHANDLING};

        interceptor = new ServerExceptionService( nexus );

        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve schema service interceptor to before chain.
         */
        state = new InvocationStateEnum[]{InvocationStateEnum.PREINVOCATION};

        interceptor = new SchemaService( nexus, globalRegistries, filterService );

        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve operational attribute managment service
         * interceptor to both the before and after interceptor chains.
         */
        state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION,
            InvocationStateEnum.POSTINVOCATION};

        interceptor = new OperationalAttributeService( nexus, globalRegistries, filterService );

        provider.addInterceptor( interceptor, state );

        // fire up the app partitions now!
        if ( initialEnv.get( EnvKeys.PARTITIONS ) != null )
        {
            startUpAppPartitions( wkdir );
        }
    }


    /**
     * Starts up all the application partitions that will be attached to naming contexts in the system.  Partition
     * database files are created within a subdirectory immediately under the Eve working directory base.
     *
     * @param eveWkdir the base Eve working directory
     * @throws javax.naming.NamingException if there are problems creating and starting these new application
     *                                      partitions
     */
    protected void startUpAppPartitions( String eveWkdir ) throws NamingException
    {
        OidRegistry oidRegistry = globalRegistries.getOidRegistry();

        AttributeTypeRegistry attributeTypeRegistry;

        attributeTypeRegistry = globalRegistries.getAttributeTypeRegistry();

        MatchingRuleRegistry reg = globalRegistries.getMatchingRuleRegistry();

        // start getting all the parameters from the initial environment
        ContextPartitionConfig[] configs = null;

        configs = PartitionConfigBuilder.getContextPartitionConfigs( initialEnv );

        for ( int ii = 0; ii < configs.length; ii++ )
        {
            // ----------------------------------------------------------------
            // create working directory under eve directory for app partition
            // ----------------------------------------------------------------

            String wkdir = eveWkdir + File.separator + configs[ii].getId();

            mkdirs( eveWkdir, configs[ii].getId() );

            // ----------------------------------------------------------------
            // create the database/store
            // ----------------------------------------------------------------

            Name upSuffix = new LdapName( configs[ii].getSuffix() );

            Normalizer dnNorm = reg.lookup( "distinguishedNameMatch" ) .getNormalizer();

            Name normSuffix = new LdapName( ( String ) dnNorm.normalize( configs[ii].getSuffix() ) );

            Database db = new JdbmDatabase( upSuffix, normSuffix, wkdir );

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

            for ( int jj = 0; jj < configs[ii].getIndices().length; jj++ )
            {
                attributeTypeList.add( attributeTypeRegistry
                        .lookup( configs[ii].getIndices()[jj] ) );
            }

            // ----------------------------------------------------------------
            // fire up the appPartition & register it with the nexus
            // ----------------------------------------------------------------

            AttributeType[] indexTypes = ( AttributeType[] ) attributeTypeList
                    .toArray( new AttributeType[attributeTypeList.size()] );

            String partitionClass = configs[ii].getPartitionClass();

            String properties = configs[ii].getProperties();

            ContextPartition partition = null;

            if ( partitionClass == null )
            {
                // If custom partition is not defined, use the ApplicationPartion.
                partition = new ApplicationPartition( upSuffix, normSuffix, db, eng, indexTypes );

            }
            else
            {
                // If custom partition is defined, instantiate it.
                try
                {
                    Class clazz = Class.forName( partitionClass );

                    Constructor constructor = clazz.getConstructor(
                            new Class[] { Name.class, Name.class, String.class } );

                    partition = ( ContextPartition ) constructor.newInstance(
                            new Object[] { upSuffix, normSuffix, properties } );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }

            if ( partition != null ) 
            { 
                nexus.register( partition );
            }

            // ----------------------------------------------------------------
            // add the nexus context entry
            // ----------------------------------------------------------------

            partition.add( configs[ii].getSuffix(), normSuffix, configs[ii].getAttributes() );
        }
    }


    /**
     * Recursively creates a bunch of directories from a base down to a path.
     *
     * @param base the base directory to start at
     * @param path the path to recursively create if we have to
     * @return true if the target directory has been created or exists, false if we fail along the way somewhere
     */
    protected boolean mkdirs( String base, String path )
    {
        String[] comps = path.split( "/" );

        File file = new File( base );

        if ( !file.exists() )
        {
            file.mkdirs();
        }

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            file = new File( file, comps[ii] );

            if ( !file.exists() )
            {
                file.mkdirs();
            }
        }

        return file.exists();
    }
}
