package org.apache.eve.jndi;


import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import javax.naming.Name;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ConfigurationException;
import javax.naming.directory.Attributes;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.util.DateUtils;

import org.apache.eve.RootNexus;
import org.apache.eve.SystemPartition;
import org.apache.eve.ApplicationPartition;
import org.apache.eve.jndi.ibs.*;
import org.apache.eve.db.*;
import org.apache.eve.db.jdbm.JdbmDatabase;
import org.apache.eve.schema.bootstrap.BootstrapRegistries;
import org.apache.eve.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.eve.schema.*;


/**
 * An LDAPd server-side provider implementation of a InitialContextFactory.
 * Can be utilized via JNDI API in the standard fashion:
 * <code>
 * Hashtable env = new Hashtable();
 * env.put( Context.PROVIDER_URL, "ou=system" );
 * env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
 * InitialContext initialContext = new InitialContext( env );
 * </code>
 * @see javax.naming.spi.InitialContextFactory
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveContextFactory implements InitialContextFactory
{
    // for convenience
    private static final String TYPE = Context.SECURITY_AUTHENTICATION;
    private static final String PRINCIPAL = Context.SECURITY_PRINCIPAL;
    //private static final String ADMIN = SystemPartition.ADMIN_PRINCIPAL;

    /** property used to shutdown the system */
    public static final String SHUTDOWN_OP_ENV = "eve.operation.shutdown";
    /** property used to sync the system with disk */
    public static final String SYNC_OP_ENV = "eve.operation.sync";
    /** key base for a set of user indices provided as comma sep list of attribute names or oids */
    public static final String USER_INDICES_ENV_BASE = "eve.user.db.indices";
    /** bootstrap prop: path to eve's working directory - relative or absolute */
    public static final String WKDIR_ENV = "eve.wkdir";
    /** default path to working directory if WKDIR_ENV property is not set */
    public static final String DEFAULT_WKDIR = "eve";
    /** a comma separated list of schema class files to load */
    public static final String SCHEMAS_ENV = "eve.schemas";
    /** bootstrap prop: if key is present it enables anonymous users */
    public static final String ANONYMOUS_ENV = "eve.enable.anonymous";

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    /** default schema classes for the SCHEMAS_ENV property if not set */
    private static final String[] DEFAULT_SCHEMAS = new String[]
    {
        "org.apache.eve.schema.bootstrap.AutofsSchema",
        "org.apache.eve.schema.bootstrap.CorbaSchema",
        "org.apache.eve.schema.bootstrap.CoreSchema",
        "org.apache.eve.schema.bootstrap.CosineSchema",
        "org.apache.eve.schema.bootstrap.EveSchema",
        "org.apache.eve.schema.bootstrap.InetorgpersonSchema",
        "org.apache.eve.schema.bootstrap.JavaSchema",
        "org.apache.eve.schema.bootstrap.Krb5kdcSchema",
        "org.apache.eve.schema.bootstrap.NisSchema",
        "org.apache.eve.schema.bootstrap.SystemSchema"
    };

    // ------------------------------------------------------------------------
    // Custom JNDI properties for adding new application partitions
    // ------------------------------------------------------------------------

    /** a comma separated list of partition names */
    public static final String PARTITIONS_ENV = "eve.db.partitions";
    /** the envprop key base to the suffix of a partition */
    public static final String SUFFIX_BASE_ENV = "eve.db.partition.suffix.";
    /** the envprop key base to the space separated list of indices for a partition */
    public static final String INDICES_BASE_ENV = "eve.db.partition.indices.";
    /** the envprop key base to the Attributes for the context nexus entry */
    public static final String ATTRIBUTES_BASE_ENV = "eve.db.partition.attributes.";

    // ------------------------------------------------------------------------
    // Members
    // ------------------------------------------------------------------------

    /** The singleton EveJndiProvider instance */
    private EveJndiProvider provider = null;
    /** the initial context environment that fired up the backend subsystem */
    private Hashtable initialEnv;


    private SystemPartition system;
    private GlobalRegistries globalRegistries;
    private RootNexus nexus;


    /**
     * Default constructor that sets the provider of this EveContextFactory.
     */
    public EveContextFactory()
    {
        EveJndiProvider.setProviderOn( this );
    }
    
    
    /**
     * Enables this EveContextFactory with a handle to the EveJndiProvider
     * singleton.
     * 
     * @param a_provider the system's singleton EveBackendSubsystem service.
     */
    void setProvider( EveJndiProvider a_provider )
    {
        provider = a_provider;
    }
    
    
    /**
     * @see javax.naming.spi.InitialContextFactory#getInitialContext(
     * java.util.Hashtable)
     */
    public Context getInitialContext( Hashtable env ) throws NamingException
    {
        if ( env.containsKey( SHUTDOWN_OP_ENV ) )
        {
            provider.shutdown();
            provider = null;
            initialEnv = null;
            return null;
        }

        if ( env.containsKey( SYNC_OP_ENV ) )
        {
            provider.sync();
            return null;
        }

        // fire up the backend subsystem if we need to
        if ( null == provider )
        {
            this.initialEnv = env;

            // check if we are trying to boostrap as another user
            if ( initialEnv.containsKey( PRINCIPAL ) &&
                 initialEnv.containsKey( TYPE ) &&
                 initialEnv.get( TYPE ).equals( "none" ) )
            {
                String msg = "Ambiguous configuration: " + TYPE;
                msg += " is set to none and the security principal";
                msg += " is set using " + PRINCIPAL + " as well";
                throw new ConfigurationException( msg );
            }
            else if ( ! initialEnv.containsKey( Context.SECURITY_PRINCIPAL ) &&
                   initialEnv.containsKey( Context.SECURITY_AUTHENTICATION ) &&
                   initialEnv.get( Context.SECURITY_AUTHENTICATION ).equals( "none" ) )
            {
                throw new ConfigurationException( "using authentication type none "
                        + "for anonymous binds while trying to bootstrap Eve "
                        + "- this is not allowed ONLY the admin can bootstrap" );
            }
            else if ( initialEnv.containsKey( Context.SECURITY_PRINCIPAL ) &&
                      ! initialEnv.get( Context.SECURITY_PRINCIPAL ).equals( SystemPartition.ADMIN_PRINCIPAL ) )
            {
                throw new ConfigurationException( "user "
                        + initialEnv.get( Context.SECURITY_PRINCIPAL )
                        + " is not allowed to bootstrap the system. ONLY the "
                        + "admin can bootstrap" );
            }

            initialize();
            createAdminAccount();
        }

        EveContext ctx = ( EveContext ) provider.getLdapContext( env );
        return ctx;
    }


    /**
     * Returns true if we had to create the admin account since this is the
     * first time we started the server.  Otherwise if the account exists then
     * we are not starting for the first time.
     *
     * @return
     * @throws NamingException
     */
    private boolean createAdminAccount() throws NamingException
    {
        Name admin = new LdapName( SystemPartition.ADMIN_PRINCIPAL );

        /*
         * If the admin entry is there, then the database was already created
         * before so we just need to lookup the userPassword field to see if
         * the password matches.
         */
        if ( nexus.hasEntry( admin ) )
        {
            return false;
        }

        Attributes attributes = new LockableAttributesImpl();
        attributes.put( "objectClass", "top" );
        attributes.put( "objectClass", "person" );
        attributes.put( "objectClass", "organizationalPerson" );
        attributes.put( "objectClass", "inetOrgPerson" );
        attributes.put( "uid", SystemPartition.ADMIN_UID );
        attributes.put( "displayName", "Directory Superuser" );
        attributes.put( "creatorsName", SystemPartition.ADMIN_PRINCIPAL );
        attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() );
        attributes.put( "displayName", "Directory Superuser" );

        if ( initialEnv.containsKey( Context.SECURITY_CREDENTIALS ) )
        {
            attributes.put( "userPassword", initialEnv.get(
                    Context.SECURITY_CREDENTIALS ) );
        }
        else
        {
            attributes.put( "userPassword", ArrayUtils.EMPTY_BYTE_ARRAY );
        }

        nexus.add( SystemPartition.ADMIN_PRINCIPAL, admin, attributes );
        return true;
    }


    private void initialize() throws NamingException
    {
        // --------------------------------------------------------------------
        // Load the schema here and check that it is ok!
        // --------------------------------------------------------------------

        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();

        String[] schemas = DEFAULT_SCHEMAS;
        if ( initialEnv.containsKey( SCHEMAS_ENV ) )
        {
            schemas = ( ( String ) initialEnv.get( SCHEMAS_ENV ) ).split( "," );
        }

        loader.load( schemas, bootstrapRegistries );
        List errors = bootstrapRegistries.checkRefInteg();
        if ( ! errors.isEmpty() )
        {
            NamingException e = new NamingException();
            e.setRootCause( ( Throwable ) errors.get( 0 ) );
            throw e;
        }

        // --------------------------------------------------------------------
        // Fire up the system partition
        // --------------------------------------------------------------------


        String wkdir = DEFAULT_WKDIR;
        if ( initialEnv.containsKey( WKDIR_ENV ) )
        {
            wkdir = ( ( String ) initialEnv.get( WKDIR_ENV ) ).trim();
        }

        File wkdirFile = new File( wkdir );
        if ( wkdirFile.isAbsolute() )
        {
            if ( ! wkdirFile.exists() )
            {
                throw new NamingException( "working directory " +  wkdir + " does not exist" );
            }
        }
        else
        {
            File current = new File( "." );
            mkdirs( current.getAbsolutePath(), wkdir );
        }

        LdapName suffix = new LdapName();
        suffix.add( SystemPartition.SUFFIX );
        Database db = new JdbmDatabase( suffix, wkdir );

        AttributeTypeRegistry attributeTypeRegistry;
        attributeTypeRegistry = bootstrapRegistries.getAttributeTypeRegistry();
        OidRegistry oidRegistry;
        oidRegistry = bootstrapRegistries.getOidRegistry();

        ExpressionEvaluator evaluator;
        evaluator = new ExpressionEvaluator( db, oidRegistry, attributeTypeRegistry );

        ExpressionEnumerator enumerator;
        enumerator = new ExpressionEnumerator( db, evaluator );

        SearchEngine eng = new DefaultSearchEngine( db, evaluator, enumerator );

        AttributeType[] attributes = new AttributeType[] {
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
        nexus = new RootNexus( system );
        provider = new EveJndiProvider( nexus );


        // --------------------------------------------------------------------
        // Adding interceptors
        // --------------------------------------------------------------------


        /*
         * Create and add the Eve Exception service interceptor to both the
         * before and onError interceptor chains.
         */
        InvocationStateEnum[] state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION
        };
        boolean allowAnonymous = initialEnv.containsKey( ANONYMOUS_ENV );
        ConcreteNameComponentNormalizer normalizer;
        normalizer = new ConcreteNameComponentNormalizer( globalRegistries.getAttributeTypeRegistry() );
        Interceptor interceptor = new AuthenticationService( nexus, normalizer, allowAnonymous );
        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve Exception service interceptor to both the
         * before and onError interceptor chains.
         */
        state = new InvocationStateEnum[]{ InvocationStateEnum.POSTINVOCATION };
        FilterService filterService = new FilterServiceImpl();
        interceptor = ( Interceptor ) filterService;
        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve Exception service interceptor to both the
         * before and onError interceptor chains.
         */
        state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION,
            InvocationStateEnum.FAILUREHANDLING
        };
        interceptor = new EveExceptionService( nexus );
        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve schema service interceptor to before chain.
         */
        state = new InvocationStateEnum[]{ InvocationStateEnum.PREINVOCATION };
        interceptor = new SchemaService( nexus, globalRegistries, filterService );
        provider.addInterceptor( interceptor, state );

        /*
         * Create and add the Eve operational attribute managment service
         * interceptor to both the before and after interceptor chains.
         */
        state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION,
            InvocationStateEnum.POSTINVOCATION
        };
        interceptor = new OperationalAttributeService( nexus, globalRegistries, filterService );
        provider.addInterceptor( interceptor, state );


        // fire up the app partitions now!
        if ( initialEnv.get( PARTITIONS_ENV ) != null )
        {
            initAppPartitions( wkdir );
        }
    }


    private void initAppPartitions( String eveWkdir ) throws NamingException
    {
        OidRegistry oidRegistry = globalRegistries.getOidRegistry();
        AttributeTypeRegistry attributeTypeRegistry;
        attributeTypeRegistry = globalRegistries.getAttributeTypeRegistry();
        MatchingRuleRegistry reg = globalRegistries.getMatchingRuleRegistry();

        // start getting all the parameters from the initial environment
        String[] names = ( ( String ) initialEnv.get( PARTITIONS_ENV ) ).split( " " );

        for ( int ii = 0; ii < names.length; ii++ )
        {
            // ----------------------------------------------------------------
            // create working directory under eve directory for app partition
            // ----------------------------------------------------------------

            String suffix = ( String ) initialEnv.get( SUFFIX_BASE_ENV + names[ii] );
            String wkdir = eveWkdir + File.separator + names[ii];
            mkdirs( eveWkdir, names[ii] );

            // ----------------------------------------------------------------
            // create the database/store
            // ----------------------------------------------------------------

            Name upSuffix = new LdapName( suffix );
            Normalizer dnNorm = reg.lookup( "distinguishedNameMatch" ).getNormalizer();
            Name normSuffix = new LdapName( ( String ) dnNorm.normalize( suffix ) );
            Database db = new JdbmDatabase( upSuffix, wkdir );

            // ----------------------------------------------------------------
            // create the search engine using db, enumerators and evaluators
            // ----------------------------------------------------------------

            ExpressionEvaluator evaluator;
            evaluator = new ExpressionEvaluator( db, oidRegistry, attributeTypeRegistry );
            ExpressionEnumerator enumerator;
            enumerator = new ExpressionEnumerator( db, evaluator );
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

            if ( initialEnv.containsKey( INDICES_BASE_ENV + names[ii] ) )
            {
                String[] indices = ( ( String ) initialEnv.get( INDICES_BASE_ENV
                        + names[ii] ) ).split( " " );

                for ( int jj = 0; jj < indices.length; jj++ )
                {
                    attributeTypeList.add( attributeTypeRegistry.lookup( indices[jj] ) );
                }
            }

            // ----------------------------------------------------------------
            // fire up the appPartition & register it with the next
            // ----------------------------------------------------------------

            AttributeType[] indexTypes = ( AttributeType[] ) attributeTypeList
                    .toArray( new AttributeType[attributeTypeList.size()] );
            ApplicationPartition partition = new ApplicationPartition( upSuffix,
                    normSuffix, db, eng, indexTypes );
            nexus.register( partition );

            // ----------------------------------------------------------------
            // add the nexus context entry
            // ----------------------------------------------------------------

            Attributes rootEntry = ( Attributes ) initialEnv.get(
                    ATTRIBUTES_BASE_ENV + names[ii] );
            partition.add( suffix, normSuffix, rootEntry );
        }
    }


    protected boolean mkdirs( String base, String path )
    {
        String[] comps = path.split( "/" );
        File file = new File( base );

        if ( ! file.exists() )
        {
            file.mkdirs();
        }

        for ( int ii = 0; ii < comps.length; ii++ )
        {
            file = new File( file, comps[ii] );
            if ( ! file.exists() )
            {
                file.mkdirs();
            }
        }

        return file.exists();
    }
}
