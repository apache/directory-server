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
package org.apache.eve.jndi;


import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.naming.*;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.Attributes;
import javax.naming.spi.InitialContextFactory;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.Normalizer;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.common.util.PropertiesUtils;
import org.apache.ldap.common.ldif.LdifIterator;
import org.apache.ldap.common.ldif.LdifParser;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.exception.LdapAuthenticationNotSupportedException;
import org.apache.ldap.common.exception.LdapNoPermissionException;

import org.apache.eve.RootNexus;
import org.apache.eve.SystemPartition;
import org.apache.eve.ApplicationPartition;
import org.apache.eve.protocol.LdapProtocolProvider;
import org.apache.eve.exception.EveNamingException;
import org.apache.eve.exception.EveConfigurationException;
import org.apache.eve.jndi.ibs.*;
import org.apache.eve.db.*;
import org.apache.eve.db.jdbm.JdbmDatabase;
import org.apache.eve.schema.bootstrap.BootstrapRegistries;
import org.apache.eve.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.eve.schema.*;
import org.apache.seda.DefaultFrontend;
import org.apache.seda.DefaultFrontendFactory;
import org.apache.seda.listener.TCPListenerConfig;
import org.apache.seda.protocol.InetServiceEntry;
import org.apache.seda.protocol.TransportTypeEnum;
import org.apache.seda.protocol.DefaultInetServicesDatabase;
import org.apache.seda.protocol.ProtocolProvider;


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
    /** the default LDAP port to use */
    private static final int LDAP_PORT = 389;

    // for convenience
    private static final String TYPE = Context.SECURITY_AUTHENTICATION;
    private static final String CREDS = Context.SECURITY_CREDENTIALS;
    private static final String PRINCIPAL = Context.SECURITY_PRINCIPAL;
    private static final String ADMIN = SystemPartition.ADMIN_PRINCIPAL;
    private static final Name ADMIN_NAME = SystemPartition.getAdminDn();

    // ------------------------------------------------------------------------
    // Custom JNDI properties
    // ------------------------------------------------------------------------

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


    /** key used to disable the networking layer (wire protocol) */
    public static final String DISABLE_PROTOCOL = "eve.net.disable.protocol";
    public static final String EVE_LDAP_PORT = "eve.net.ldap.port";
    public static final String EVE_LDAPS_PORT = "eve.net.ldaps.port";

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
    // Members
    // ------------------------------------------------------------------------

    /** The singleton EveJndiProvider instance */
    private EveJndiProvider provider = null;
    /** the initial context environment that fired up the backend subsystem */
    private Hashtable initialEnv;
    private SystemPartition system;
    private GlobalRegistries globalRegistries;
    private RootNexus nexus;


    private DefaultFrontend fe;
    private InetServiceEntry srvEntry;
    private ProtocolProvider proto;
    private TCPListenerConfig tcpConfig;


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
        env = ( Hashtable ) env.clone();
        Context ctx = null;

        if ( env.containsKey( SHUTDOWN_OP_ENV ) )
        {
            if ( this.provider == null )
            {
                // monitor.shutDownCalledOnStoppedProvider()
                return new DeadContext();
            }

            try
            {
                this.provider.shutdown();

                if ( this.fe != null )
                {
                    this.fe.stop();
                }
            }
            catch( Throwable t )
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

        if ( env.containsKey( SYNC_OP_ENV ) )
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
            if ( isAnonymous( env ) && ! env.containsKey( ANONYMOUS_ENV ) )
            {
                throw new LdapNoPermissionException( "cannot bind as anonymous "
                    + " on startup without enabling anonymous bind property: "
                    + ANONYMOUS_ENV );
            }

            this.initialEnv = env;

            initialize();
            boolean createMode = createAdminAccount();

            if ( createMode )
            {
                importLdif();
            }

            // fire up the front end if we have not explicitly disabled it
            if ( ! initialEnv.containsKey( DISABLE_PROTOCOL ) )
            {
                startUpWireProtocol();
            }
        }

        ctx = ( EveContext ) provider.getLdapContext( env );
        return ctx;
    }


    /**
     * Checks to make sure security environment parameters are set correctly.
     *
     * @throws NamingException if the security settings are not correctly
     * configured.
     */
    private void checkSecuritySettings( Hashtable env ) throws NamingException
    {
        if ( env.containsKey( TYPE ) && env.get( TYPE ) != null )
        {
            /*
             * If bind is simple make sure we have the credentials and the
             * principal name set within the environment, otherwise complain
             */
            if ( env.get( TYPE ).equals( "simple" ) )
            {
                if ( ! env.containsKey( CREDS ) )
                {
                    throw new LdapConfigurationException( "missing required " +
                            CREDS + " property for simple authentication" );
                }

                if ( ! env.containsKey( PRINCIPAL ) )
                {
                    throw new LdapConfigurationException( "missing required " +
                            PRINCIPAL + " property for simple authentication" );
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
                    throw new LdapConfigurationException( "ambiguous bind " +
                            "settings encountered where bind is anonymous yet "
                            + CREDS + " property is set" );
                }
                if ( env.containsKey( PRINCIPAL ) )
                {
                    throw new LdapConfigurationException( "ambiguous bind " +
                            "settings encountered where bind is anonymous yet "
                            + PRINCIPAL + " property is set" );
                }
            }
            /*
             * If bind is anything other than simple or none we need to
             * complain because SASL is not a supported auth method yet
             */
            else
            {
                throw new LdapAuthenticationNotSupportedException(
                    ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
            }
        }
        else if ( env.containsKey( CREDS ) )
        {
            if ( ! env.containsKey( PRINCIPAL ) )
            {
                throw new LdapConfigurationException( "credentials provided " +
                        "without principal name property: " + PRINCIPAL );
            }
        }
    }


    /**
     * Checks to see if an anonymous bind is being attempted.
     *
     * @return true if bind is anonymous, false otherwise
     */
    private boolean isAnonymous( Hashtable env )
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
     * Returns true if we had to create the admin account since this is the
     * first time we started the server.  Otherwise if the account exists then
     * we are not starting for the first time.
     *
     * @return
     * @throws NamingException
     */
    private boolean createAdminAccount() throws NamingException
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
     * @throws NamingException if there are problems along the way
     */
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
            for ( int ii = 0; ii < schemas.length; ii++ )
            {
                schemas[ii] = schemas[ii].trim();
            }
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
        enumerator = new ExpressionEnumerator( db, attributeTypeRegistry, evaluator );

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
        nexus = new RootNexus( system, new LockableAttributesImpl() );
        provider = new EveJndiProvider( nexus );


        // --------------------------------------------------------------------
        // Adding interceptors
        // --------------------------------------------------------------------


        /*
         * Create and add the Authentication service interceptor to before
         * interceptor chain.
         */
        InvocationStateEnum[] state = new InvocationStateEnum[]{
            InvocationStateEnum.PREINVOCATION
        };
        boolean allowAnonymous = initialEnv.containsKey( ANONYMOUS_ENV );
        Interceptor interceptor = new AuthenticationService( nexus, allowAnonymous );
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
         * Create and add the Authorization service interceptor to before
         * interceptor chain.
         */
        state = new InvocationStateEnum[]{ InvocationStateEnum.PREINVOCATION };
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
            startUpAppPartitions( wkdir );
        }
    }


    private void startUpWireProtocol() throws NamingException
    {
        try
        {
            fe = ( DefaultFrontend ) new DefaultFrontendFactory().create();
        }
        catch ( Exception e )
        {
            String msg = "Failed to initialize the frontend subsystem!";
            NamingException ne = new EveConfigurationException( msg );
            ne.setRootCause( e );
            ne.setResolvedName( new LdapName( ( String ) initialEnv.get( Context.PROVIDER_URL ) ) );
            throw ne;
        }

        proto = new LdapProtocolProvider( ( Hashtable) initialEnv.clone(), fe.getEventRouter() );

        int port = PropertiesUtils.get( initialEnv, EVE_LDAP_PORT, LDAP_PORT );
        srvEntry = new InetServiceEntry( proto.getName(), port, proto, TransportTypeEnum.TCP );
        ( ( DefaultInetServicesDatabase ) fe.getInetServicesDatabase()).addEntry( srvEntry );

        try
        {
            tcpConfig = new TCPListenerConfig( InetAddress.getLocalHost(), srvEntry );
        }
        catch ( UnknownHostException e )
        {
            e.printStackTrace();
            String msg = "Could not recognize the host!";
            EveConfigurationException e2 = new EveConfigurationException( msg );
            e2.setRootCause( e );
        }

        try
        {
            fe.getTCPListenerManager().bind( tcpConfig );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            String msg = "We failed to bind to the port!";
            EveConfigurationException e2 = new EveConfigurationException( msg );
            e2.setRootCause( e );
        }
    }


    /**
     * Starts up all the application partitions that will be attached to naming
     * contexts in the system.  Partition database files are created within a
     * subdirectory immediately under the Eve working directory base.
     *
     * @param eveWkdir the base Eve working directory
     * @throws javax.naming.NamingException if there are problems creating and starting these
     * new application partitions
     */
    private void startUpAppPartitions( String eveWkdir ) throws NamingException
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

            Attributes rootAttrs;
            Object rootEntry = initialEnv.get( ATTRIBUTES_BASE_ENV + names[ii] );

            if ( rootEntry instanceof String )
            {
                rootAttrs = new LockableAttributesImpl();
                String ldif = ( ( String ) rootEntry ).trim().replace( '*', '\n' );
                ( new LdifParserImpl() ).parse( rootAttrs, ldif );
            }
            else if ( rootEntry instanceof Attributes )
            {
                rootAttrs = ( Attributes ) rootEntry;
            }
            else
            {
                throw new EveNamingException( "The root entry env property was"
                        + " not of an expected type: " + rootEntry,
                        ResultCodeEnum.OTHER );
            }

            partition.add( suffix, normSuffix, rootAttrs );
        }
    }


    /**
     * Recursively creates a bunch of directories from a base down to a path.
     *
     * @param base the base directory to start at
     * @param path the path to recursively create if we have to
     * @return true if the target directory has been created or exists, false
     * if we fail along the way somewhere
     */
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


    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into
     * the newly created system partition to prime it up for operation.  Note
     * that only ou=system entries will be added - entries for other partitions
     * cannot be imported and will blow chunks.
     *
     * @throws NamingException if there are problems reading the ldif file and
     * adding those entries to the system partition
     */
    protected void importLdif() throws NamingException
    {
        Hashtable env = new Hashtable();
        env.putAll( initialEnv );
        env.put( Context.PROVIDER_URL, "ou=system" );
        LdapContext ctx = provider.getLdapContext( env );
        InputStream in = ( InputStream ) getClass().getResourceAsStream( "system.ldif" );
        LdifParser parser = new LdifParserImpl();

        try
        {
            LdifIterator iterator = new LdifIterator( in );
            while ( iterator.hasNext() )
            {
                Attributes attributes = new LockableAttributesImpl();
                String ldif = ( String ) iterator.next();
                parser.parse( attributes, ldif );
                Name dn = new LdapName( ( String ) attributes.remove( "dn" ).get() );

                dn.remove( 0 );
                ctx.createSubcontext( dn, attributes );
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system ldif file";
            NamingException ne = new LdapConfigurationException( msg );
            ne.setRootCause( e );
            throw ne;
        }
    }
}
