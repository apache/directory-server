/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core;


import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.changelog.ChangeLog;
import org.apache.directory.server.core.changelog.ChangeLogEvent;
import org.apache.directory.server.core.changelog.ChangeLogInterceptor;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.changelog.Tag;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.partition.DefaultPartitionNexus;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.server.core.schema.PartitionSchemaLoader;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.schema.SchemaOperationControl;
import org.apache.directory.server.core.schema.SchemaPartitionDao;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachemetaSchema;
import org.apache.directory.server.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.server.schema.bootstrap.partition.DbFileListing;
import org.apache.directory.server.schema.bootstrap.partition.SchemaPartitionExtractor;
import org.apache.directory.server.schema.registries.DefaultOidRegistry;
import org.apache.directory.server.schema.registries.DefaultRegistries;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Default implementation of {@link DirectoryService}.
 * 
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultDirectoryService implements  DirectoryService
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultDirectoryService.class );

    private SchemaService schemaService;

    /** the registries for system schema objects */
    private Registries registries;

    /** the root nexus */
    private DefaultPartitionNexus partitionNexus;

    /** whether or not server is started for the first time */
    private boolean firstStart;

    /** The interceptor (or interceptor chain) for this service */
    private InterceptorChain interceptorChain;

    /** whether or not this instance has been shutdown */
    private boolean started;

    /** the change log service */
    private ChangeLog changeLog;

    private LdapDN adminDn;

    /** remove me after implementation is completed */
    private static final String PARTIAL_IMPL_WARNING =
            "WARNING: the changelog is only partially operational and will revert\n" +
            "state without consideration of who made the original change.  All reverting " +
            "changes are made by the admin user.\n Furthermore the used controls are not at " +
            "all taken into account";

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a new instance.
     */
    public DefaultDirectoryService()
    {
        setDefaultInterceptorConfigurations();
        changeLog = new DefaultChangeLog();
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    public static final int MAX_SIZE_LIMIT_DEFAULT = 100;
    public static final int MAX_TIME_LIMIT_DEFAULT = 10000;

    private String instanceId;
    private File workingDirectory = new File( "server-work" );
    private boolean exitVmOnShutdown = true; // allow by default
    private boolean shutdownHookEnabled = true; // allow by default
    private boolean allowAnonymousAccess = true; // allow by default
    private boolean accessControlEnabled; // off by default
    private boolean denormalizeOpAttrsEnabled; // off by default
    private int maxSizeLimit = MAX_SIZE_LIMIT_DEFAULT; // set to default value
    private int maxTimeLimit = MAX_TIME_LIMIT_DEFAULT; // set to default value (milliseconds)
    private List<Interceptor> interceptors;
    private Partition systemPartition;
    private Set<Partition> partitions = new HashSet<Partition>();
    private List<? extends Entry> testEntries = new ArrayList<Entry>(); // List<Attributes>



    public void setInstanceId( String instanceId )
    {
        this.instanceId = instanceId;
    }


    public String getInstanceId()
    {
        return instanceId;
    }


    /**
     * Gets the {@link Partition}s used by this DirectoryService.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.partition.Partition"
     * @return the set of partitions used
     */
    public Set<? extends Partition> getPartitions()
    {
        Set<Partition> cloned = new HashSet<Partition>();
        cloned.addAll( partitions );
        return cloned;
    }


    /**
     * Sets {@link Partition}s used by this DirectoryService.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.partition.Partition"
     * @param partitions the partitions to used
     */
    public void setPartitions( Set<? extends Partition> partitions )
    {
        Set<Partition> cloned = new HashSet<Partition>();
        cloned.addAll( partitions );
        Set<String> names = new HashSet<String>();
        for ( Partition partition : cloned )
        {
            String id = partition.getId();
            if ( names.contains( id ) )
            {
                LOG.warn( "Encountered duplicate partition {} identifier.", id );
            }
            names.add( id );
        }

        this.partitions = cloned;
    }


    /**
     * Returns <tt>true</tt> if access control checks are enabled.
     *
     * @return true if access control checks are enabled, false otherwise
     */
    public boolean isAccessControlEnabled()
    {
        return accessControlEnabled;
    }


    /**
     * Sets whether to enable basic access control checks or not.
     *
     * @param accessControlEnabled true to enable access control checks, false otherwise
     */
    public void setAccessControlEnabled( boolean accessControlEnabled )
    {
        this.accessControlEnabled = accessControlEnabled;
    }


    /**
     * Returns <tt>true</tt> if anonymous access is allowed on entries besides the RootDSE.
     * If the access control subsystem is enabled then access to some entries may not be
     * allowed even when full anonymous access is enabled.
     *
     * @return true if anonymous access is allowed on entries besides the RootDSE, false
     * if anonymous access is allowed to all entries.
     */
    public boolean isAllowAnonymousAccess()
    {
        return allowAnonymousAccess;
    }


    /**
     * Sets whether to allow anonymous access to entries other than the RootDSE.  If the
     * access control subsystem is enabled then access to some entries may not be allowed
     * even when full anonymous access is enabled.
     *
     * @param enableAnonymousAccess true to enable anonymous access, false to disable it
     */
    public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {
        this.allowAnonymousAccess = enableAnonymousAccess;
    }


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    public List<Interceptor> getInterceptors()
    {
        List<Interceptor> cloned = new ArrayList<Interceptor>();
        cloned.addAll( interceptors );
        return cloned;
    }


    /**
     * Sets the interceptors in the server.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.interceptor.Interceptor"
     * @param interceptors the interceptors to be used in the server.
     */
    public void setInterceptors( List<Interceptor> interceptors ) 
    {
        Set<String> names = new HashSet<String>();
        for ( Interceptor interceptor : interceptors )
        {
            String name = interceptor.getName();
            if ( names.contains( name ) )
            {
                LOG.warn( "Encountered duplicate definitions for {} interceptor", interceptor.getName() );
            }
            names.add( name );
        }

        this.interceptors = interceptors;
    }


    /**
     * Returns test directory entries({@link Entry}) to be loaded while
     * bootstrapping.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.shared.ldap.ldif.Entry"
     * @return test entries to load during bootstrapping
     */
    public List<Entry> getTestEntries()
    {
        List<Entry> cloned = new ArrayList<Entry>();
        cloned.addAll( testEntries );
        return cloned;
    }


    /**
     * Sets test directory entries({@link Attributes}) to be loaded while
     * bootstrapping.
     *
     * @org.apache.xbean.Property nestedType="org.apache.directory.shared.ldap.ldif.Entry"
     * @param testEntries the test entries to load while bootstrapping
     */
    public void setTestEntries( List<? extends Entry> testEntries )
    {
        //noinspection MismatchedQueryAndUpdateOfCollection
        List<Entry> cloned = new ArrayList<Entry>();
        cloned.addAll( testEntries );
        this.testEntries = testEntries;
    }


    /**
     * Returns working directory (counterpart of <tt>var/lib</tt>) where partitions are
     * stored by default.
     *
     * @return the directory where partition's are stored.
     */
    public File getWorkingDirectory()
    {
        return workingDirectory;
    }


    /**
     * Sets working directory (counterpart of <tt>var/lib</tt>) where partitions are stored
     * by default.
     *
     * @param workingDirectory the directory where the server's partitions are stored by default.
     */
    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }


    public void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {
        this.shutdownHookEnabled = shutdownHookEnabled;
    }


    public boolean isShutdownHookEnabled()
    {
        return shutdownHookEnabled;
    }


    public void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {
        this.exitVmOnShutdown = exitVmOnShutdown;
    }


    public boolean isExitVmOnShutdown()
    {
        return exitVmOnShutdown;
    }


    public void setMaxSizeLimit( int maxSizeLimit )
    {
        this.maxSizeLimit = maxSizeLimit;
    }


    public int getMaxSizeLimit()
    {
        return maxSizeLimit;
    }


    public void setMaxTimeLimit( int maxTimeLimit )
    {
        this.maxTimeLimit = maxTimeLimit;
    }


    public int getMaxTimeLimit()
    {
        return maxTimeLimit;
    }

    public void setSystemPartition( Partition systemPartition )
    {
        this.systemPartition = systemPartition;
    }


    public Partition getSystemPartition()
    {
        return systemPartition;
    }


    public boolean isDenormalizeOpAttrsEnabled()
    {
        return denormalizeOpAttrsEnabled;
    }


    public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
    {
        this.denormalizeOpAttrsEnabled = denormalizeOpAttrsEnabled;
    }


    public ChangeLog getChangeLog()
    {
        return changeLog;
    }


    public void setChangeLog( ChangeLog changeLog )
    {
        this.changeLog = changeLog;
    }


    public void addPartition( Partition parition ) throws NamingException
    {
        partitions.add( parition );

        if ( ! started )
        {
            return;
        }

        AddContextPartitionOperationContext addPartitionCtx = new AddContextPartitionOperationContext( parition );
        partitionNexus.addContextPartition( addPartitionCtx );
    }


    public void removePartition( Partition partition ) throws NamingException
    {
        partitions.remove( partition );

        if ( ! started )
        {
            return;
        }

        RemoveContextPartitionOperationContext removePartitionCtx =
                new RemoveContextPartitionOperationContext( partition.getSuffixDn() );
        partitionNexus.removeContextPartition( removePartitionCtx );
    }


    // ------------------------------------------------------------------------
    // BackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------


    private void setDefaultInterceptorConfigurations()
    {
        // Set default interceptor chains
        List<Interceptor> list = new ArrayList<Interceptor>();

        list.add( new NormalizationInterceptor() );
        list.add( new AuthenticationInterceptor() );
        list.add( new ReferralInterceptor() );
        list.add( new AciAuthorizationInterceptor() );
        list.add( new DefaultAuthorizationInterceptor() );
        list.add( new ExceptionInterceptor() );
        list.add( new ChangeLogInterceptor() );
        list.add( new OperationalAttributeInterceptor() );
        list.add( new SchemaInterceptor() );
        list.add( new SubentryInterceptor() );
        list.add( new CollectiveAttributeInterceptor() );
        list.add( new EventInterceptor() );
        list.add( new TriggerInterceptor() );

        setInterceptors( list );
    }


    public LdapContext getJndiContext() throws NamingException
    {
        return this.getJndiContext( null, null, null, AuthenticationLevel.NONE.toString(), "" );
    }


    public LdapContext getJndiContext( String dn ) throws NamingException
    {
        return this.getJndiContext( null, null, null, AuthenticationLevel.NONE.toString(), dn );
    }


    public LdapContext getJndiContext( LdapPrincipal principal ) throws NamingException
    {
        return new ServerLdapContext( this, principal, new LdapDN() );
    }


    public LdapContext getJndiContext( LdapPrincipal principal, String dn ) throws NamingException
    {
        return new ServerLdapContext( this, principal, new LdapDN( dn ) );
    }


    public synchronized LdapContext getJndiContext( LdapDN principalDn, String principal, byte[] credential,
        String authentication, String rootDN ) throws NamingException
    {
        checkSecuritySettings( principal, credential, authentication );

        if ( !started )
        {
            return new DeadContext();
        }

        Hashtable<String, Object> environment = new Hashtable<String, Object>();
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
        environment.put( DirectoryService.JNDI_KEY, this );

        return new ServerLdapContext( this, environment );
    }


    public long revert() throws NamingException
    {
        if ( changeLog == null || ! changeLog.isEnabled() )
        {
            throw new IllegalStateException( "The change log must be enabled to revert to previous log revisions." );
        }

        Tag latest = changeLog.getLatest();
        if ( null != latest )
        {
            if ( latest.getRevision() < changeLog.getCurrentRevision() )
            {
                return revert( latest.getRevision() );
            }
            else
            {
                LOG.info( "Ignoring request to revert without changes since the latest tag." );
                return changeLog.getCurrentRevision();
            }
        }

        throw new IllegalStateException( "There must be at least one tag to revert to the latest tag." );
    }


    public long revert( long revision ) throws NamingException
    {
        if ( changeLog == null || ! changeLog.isEnabled() )
        {
            throw new IllegalStateException( "The change log must be enabled to revert to previous log revisions." );
        }

        if ( revision < 0 )
        {
            throw new IllegalArgumentException( "revision must be greater than or equal to 0" );
        }

        if ( revision >= changeLog.getChangeLogStore().getCurrentRevision() )
        {
            throw new IllegalArgumentException( "revision must be less than the current revision" );
        }

        DirContext ctx = getJndiContext( new LdapPrincipal( adminDn, AuthenticationLevel.SIMPLE ) );
        Cursor<ChangeLogEvent> cursor = changeLog.getChangeLogStore().findAfter( revision );

        /*
         * BAD, BAD, BAD!!!
         *
         * No synchronization no nothing.  Just getting this to work for now
         * so we can revert tests.  Any production grade use of this feature
         * needs to synchronize on all changes while the revert is in progress.
         *
         * How about making this operation transactional?
         *
         * First of all just stop using JNDI and construct the operations to
         * feed into the interceptor pipeline.
         */

        try
        {
            LOG.warn( PARTIAL_IMPL_WARNING );
            cursor.afterLast();
            while ( cursor.previous() ) // apply ldifs in reverse order
            {
                ChangeLogEvent event = cursor.get();
                Entry reverse = event.getReverseLdif();

                switch( reverse.getChangeType().getChangeType() )
                {
                    case( ChangeType.ADD_ORDINAL ):
                        ctx.createSubcontext( reverse.getDn(), reverse.getAttributes() );
                        break;
                    case( ChangeType.DELETE_ORDINAL ):
                        ctx.destroySubcontext( reverse.getDn() );
                        break;
                    case( ChangeType.MODIFY_ORDINAL ):
                        ctx.modifyAttributes( reverse.getDn(), reverse.getModificationItemsArray() );
                        break;
                    case( ChangeType.MODDN_ORDINAL ):
                        // NOT BREAK - both ModDN and ModRDN handling is the same
                    case( ChangeType.MODRDN_ORDINAL ):
                        if ( reverse.isDeleteOldRdn() )
                        {
                            ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "true" );
                        }
                        else
                        {
                            ctx.addToEnvironment( "java.naming.ldap.deleteRDN", "true" );
                        }

                        ctx.rename( reverse.getDn(), event.getForwardLdif().getDn() );
                        break;
                    default:
                        throw new NotImplementedException( "Reverts of change type " + reverse.getChangeType()
                                + " has not yet been implemented!");
                }
            }
        }
        catch ( IOException e )
        {
            throw new NamingException( "Encountered a failure while trying to revert to a previous revision: "
                    + revision );
        }

        return changeLog.getCurrentRevision();
    }


    /**
     * @throws NamingException if the LDAP server cannot be started
     */
    public synchronized void startup() throws NamingException
    {
        if ( started )
        {
            return;
        }

        if ( shutdownHookEnabled )
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
                        LOG.warn( "Failed to shut down the directory service: "
                            + DefaultDirectoryService.this.instanceId, e );
                    }
                }
            }, "ApacheDS Shutdown Hook (" + instanceId + ')' ) );

            LOG.info( "ApacheDS shutdown hook has been registered with the runtime." );
        }
        else if ( LOG.isWarnEnabled() )
        {
            LOG.warn( "ApacheDS shutdown hook has NOT been registered with the runtime."
                + "  This default setting for standalone operation has been overriden." );
        }

        initialize();
        firstStart = createBootstrapEntries();
        showSecurityWarnings();
        started = true;
        
        adminDn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
        adminDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );

        if ( !testEntries.isEmpty() )
        {
            createTestEntries();
        }
    }


    public synchronized void sync() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        this.changeLog.sync();
        this.partitionNexus.sync();
    }


    public synchronized void shutdown() throws NamingException
    {
        if ( !started )
        {
            return;
        }

        this.changeLog.sync();
        this.changeLog.destroy();

        this.partitionNexus.sync();
        this.partitionNexus.destroy();
        this.interceptorChain.destroy();
        this.started = false;
        setDefaultInterceptorConfigurations();
    }


    public Registries getRegistries()
    {
        return registries;
    }


    public void setRegistries( Registries registries )
    {
        this.registries = registries;
    }


    public SchemaService getSchemaService()
    {
        return schemaService;
    }


    public void setSchemaService( SchemaService schemaService )
    {
        this.schemaService = schemaService;
    }


    public PartitionNexus getPartitionNexus()
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
     * @param authentication the mechanism for authentication
     * @param credential the password
     * @param principal the distinguished name of the principal
     */
    private void checkSecuritySettings( String principal, byte[] credential, String authentication )
        throws NamingException
    {
        if ( authentication == null )
        {
            authentication = "";
        }

        /*
         * If bind is strong make sure we have the principal name
         * set within the environment, otherwise complain
         */
        if ( AuthenticationLevel.STRONG.toString().equalsIgnoreCase( authentication ) )
        {
            if ( principal == null )
            {
                throw new LdapConfigurationException( "missing required " + Context.SECURITY_PRINCIPAL
                    + " property for strong authentication" );
            }
        }
        /*
         * If bind is simple make sure we have the credentials and the
         * principal name set within the environment, otherwise complain
         */
        else if ( AuthenticationLevel.SIMPLE.toString().equalsIgnoreCase( authentication ) )
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
        else if ( AuthenticationLevel.NONE.toString().equalsIgnoreCase( authentication ) )
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

            if ( !allowAnonymousAccess )
            {
                throw new LdapNoPermissionException( "Anonymous access disabled." );
            }
        }
        else
        {
            /*
             * If bind is anything other than strong, simple, or none we need to complain
             */
            throw new LdapAuthenticationNotSupportedException( "Unknown authentication type: '" + authentication + "'",
                ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
        }
    }


    /**
     * Returns true if we had to create the bootstrap entries on the first
     * start of the server.  Otherwise if all entries exist, meaning none
     * had to be created, then we are not starting for the first time.
     *
     * @return true if the bootstrap entries had to be created, false otherwise
     * @throws javax.naming.NamingException if entries cannot be created
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
        if ( !partitionNexus.hasEntry( new EntryOperationContext( PartitionNexus.getAdminName() ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.PERSON_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_PERSON_OC );
            objectClass.add( SchemaConstants.INET_ORG_PERSON_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.UID_AT, PartitionNexus.ADMIN_UID );
            attributes.put( SchemaConstants.USER_PASSWORD_AT, PartitionNexus.ADMIN_PASSWORD );
            attributes.put( SchemaConstants.DISPLAY_NAME_AT, "Directory Superuser" );
            attributes.put( SchemaConstants.CN_AT, "system administrator" );
            attributes.put( SchemaConstants.SN_AT, "administrator" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
            attributes.put( SchemaConstants.DISPLAY_NAME_AT, "Directory Superuser" );

            partitionNexus.add( new AddOperationContext( PartitionNexus.getAdminName(),
                attributes ) );
        }

        // -------------------------------------------------------------------
        // create system users area
        // -------------------------------------------------------------------

        Map<String,OidNormalizer> oidsMap = registries.getAttributeTypeRegistry().getNormalizerMapping();
        LdapDN userDn = new LdapDN( "ou=users,ou=system" );
        userDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( userDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "users" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( userDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create system groups area
        // -------------------------------------------------------------------

        LdapDN groupDn = new LdapDN( "ou=groups,ou=system" );
        groupDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( groupDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "groups" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( groupDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create administrator group
        // -------------------------------------------------------------------

        LdapDN name = new LdapDN( ServerDNConstants.ADMINISTRATORS_GROUP_DN );
        name.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( name ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.GROUP_OF_UNIQUE_NAMES_OC );
            attributes.put( objectClass );
            attributes.put( SchemaConstants.CN_AT, "Administrators" );
            attributes.put( SchemaConstants.UNIQUE_MEMBER_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( name, attributes ) );
            
            Interceptor authzInterceptor = interceptorChain.get( AciAuthorizationInterceptor.class.getName() );
            
            if ( authzInterceptor == null )
            {
                LOG.error( "The Authorization service is null : this is not allowed" );
                throw new NamingException( "The Authorization service is null" );
            }
            
            if ( !( authzInterceptor instanceof AciAuthorizationInterceptor ) )
            {
                LOG.error( "The Authorization service is not set correctly : '{}' is an incorect interceptor",
                    authzInterceptor.getClass().getName() );
                throw new NamingException( "The Authorization service is incorrectly set" );
                
            }

            AciAuthorizationInterceptor authzSrvc = ( AciAuthorizationInterceptor ) authzInterceptor;
            authzSrvc.cacheNewGroup( name, attributes );

        }

        // -------------------------------------------------------------------
        // create system configuration area
        // -------------------------------------------------------------------

        LdapDN configurationDn = new LdapDN( "ou=configuration,ou=system" );
        configurationDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( configurationDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "configuration" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( configurationDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create system configuration area for partition information
        // -------------------------------------------------------------------

        LdapDN partitionsDn = new LdapDN( "ou=partitions,ou=configuration,ou=system" );
        partitionsDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( partitionsDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "partitions" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( partitionsDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create system configuration area for services
        // -------------------------------------------------------------------

        LdapDN servicesDn = new LdapDN( "ou=services,ou=configuration,ou=system" );
        servicesDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( servicesDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "services" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( servicesDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create system configuration area for interceptors
        // -------------------------------------------------------------------

        LdapDN interceptorsDn = new LdapDN( "ou=interceptors,ou=configuration,ou=system" );
        interceptorsDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( interceptorsDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OU_AT, "interceptors" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( interceptorsDn, attributes ) );
        }

        // -------------------------------------------------------------------
        // create system preferences area
        // -------------------------------------------------------------------

        LdapDN sysPrefRootDn = new LdapDN( "prefNodeName=sysPrefRoot,ou=system");
        sysPrefRootDn.normalize( oidsMap );
        
        if ( !partitionNexus.hasEntry( new EntryOperationContext( sysPrefRootDn ) ) )
        {
            firstStart = true;

            Attributes attributes = new AttributesImpl();
            Attribute objectClass = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClass.add( SchemaConstants.TOP_OC );
            objectClass.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            attributes.put( objectClass );

            attributes.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.EXTENSIBLE_OBJECT_OC );
            attributes.put( "prefNodeName", "sysPrefRoot" );
            attributes.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
            attributes.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );

            partitionNexus.add( new AddOperationContext( sysPrefRootDn, attributes ) );
        }

        return firstStart;
    }


    /**
     * Displays security warning messages if any possible secutiry issue is found.
     * @throws NamingException if there are failures parsing and accessing internal structures
     */
    private void showSecurityWarnings() throws NamingException
    {
        // Warn if the default password is not changed.
        boolean needToChangeAdminPassword = false;

        LdapDN adminDn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN );
        adminDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        
        Attributes adminEntry = partitionNexus.lookup( new LookupOperationContext( adminDn ) );
        Object userPassword = adminEntry.get( SchemaConstants.USER_PASSWORD_AT ).get();
        if ( userPassword instanceof byte[] )
        {
            needToChangeAdminPassword = PartitionNexus.ADMIN_PASSWORD.equals( new String(
                ( byte[] ) userPassword ) );
        }
        else if ( userPassword.toString().equals( PartitionNexus.ADMIN_PASSWORD ) )
        {
            needToChangeAdminPassword = PartitionNexus.ADMIN_PASSWORD.equals( userPassword.toString() );
        }

        if ( needToChangeAdminPassword )
        {
            LOG.warn( "You didn't change the admin password of directory service " + "instance '" + instanceId + "'.  "
                + "Please update the admin password as soon as possible " + "to prevent a possible security breach." );
        }
    }


    /**
     * Adds test entries into the core.
     *
     * @todo this may no longer be needed when JNDI is not used for bootstrapping
     * 
     * @throws NamingException if the creation of test entries fails.
     */
    private void createTestEntries() throws NamingException
    {
        LdapPrincipal principal = new LdapPrincipal( adminDn, AuthenticationLevel.SIMPLE );
        ServerLdapContext ctx = new ServerLdapContext( this, principal, new LdapDN() );

        for ( Entry testEntry : testEntries )
        {
            try
            {
                Entry entry = testEntry.clone();
                Attributes attributes = entry.getAttributes();
                String dn = entry.getDn();

                try
                {
                    ctx.createSubcontext( dn, attributes );
                }
                catch ( Exception e )
                {
                    LOG.warn( dn + " test entry already exists.", e );
                }
            }
            catch ( CloneNotSupportedException cnse )
            {
                LOG.warn( "Cannot clone the entry ", cnse );
            }
        }
    }


    /**
     * Kicks off the initialization of the entire system.
     *
     * @throws javax.naming.NamingException if there are problems along the way
     */
    private void initialize() throws NamingException
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "---> Initializing the DefaultDirectoryService " );
        }

        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------

        // setup temporary loader and temp registry 
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        OidRegistry oidRegistry = new DefaultOidRegistry();
        registries = new DefaultRegistries( "bootstrap", loader, oidRegistry );

        // load essential bootstrap schemas 
        Set<Schema> bootstrapSchemas = new HashSet<Schema>();
        bootstrapSchemas.add( new ApachemetaSchema() );
        bootstrapSchemas.add( new ApacheSchema() );
        bootstrapSchemas.add( new CoreSchema() );
        bootstrapSchemas.add( new SystemSchema() );
        loader.loadWithDependencies( bootstrapSchemas, registries );

        // run referential integrity tests
        List<Throwable> errors = registries.checkRefInteg();
        if ( !errors.isEmpty() )
        {
            NamingException e = new NamingException();
            e.setRootCause( errors.get( 0 ) );
            throw e;
        }
        
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );
        
        // --------------------------------------------------------------------
        // If not present extract schema partition from jar
        // --------------------------------------------------------------------

        File schemaDirectory = new File( workingDirectory, "schema" );
        SchemaPartitionExtractor extractor;
        if ( ! schemaDirectory.exists() )
        {
            try
            {
                extractor = new SchemaPartitionExtractor( workingDirectory );
                extractor.extract();
            }
            catch ( IOException e )
            {
                NamingException ne = new NamingException( "Failed to extract pre-loaded schema partition." );
                ne.setRootCause( e );
                throw ne;
            }
        }
        
        // --------------------------------------------------------------------
        // Initialize schema partition
        // --------------------------------------------------------------------

        JdbmPartition schemaPartition = new JdbmPartition();
        schemaPartition.setId( "schema" );
        schemaPartition.setCacheSize( 1000 );

        DbFileListing listing;
        try 
        {
            listing = new DbFileListing();
        }
        catch( IOException e )
        {
            throw new LdapNamingException( "Got IOException while trying to read DBFileListing: " + e.getMessage(), 
                ResultCodeEnum.OTHER );
        }

        Set<Index> indexedAttributes = new HashSet<Index>();
        for ( String attributeId : listing.getIndexedAttributes() )
        {
            indexedAttributes.add( new JdbmIndex( attributeId ) );
        }

        schemaPartition.setIndexedAttributes( indexedAttributes );
        schemaPartition.setSuffix( "ou=schema" );
        
        Attributes entry = new AttributesImpl();
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
        entry.put( SchemaConstants.OU_AT, "schema" );
        schemaPartition.setContextEntry( entry );
        schemaPartition.init( this );

        // --------------------------------------------------------------------
        // Enable schemas of all indices of partition configurations 
        // --------------------------------------------------------------------

        /*
         * We need to make sure that every attribute indexed by a partition is
         * loaded into the registries on the next step.  So here we must enable
         * the schemas of those attributes so they are loaded into the global
         * registries.
         */
        
        SchemaPartitionDao dao = new SchemaPartitionDao( schemaPartition, registries );
        Map<String,Schema> schemaMap = dao.getSchemas();
        Set<Partition> partitions = new HashSet<Partition>();
        partitions.add( systemPartition );
        partitions.addAll( this.partitions );

        for ( Partition partition : partitions )
        {
            if ( partition instanceof BTreePartition )
            {
                JdbmPartition btpconf = ( JdbmPartition ) partition;
                for ( Index index : btpconf.getIndexedAttributes() )
                {
                    String schemaName = dao.findSchema( index.getAttributeId() );
                    if ( schemaName == null )
                    {
                        throw new NamingException( "Index on unidentified attribute: " + index.toString() );
                    }

                    Schema schema = schemaMap.get( schemaName );
                    if ( schema.isDisabled() )
                    {
                        dao.enableSchema( schemaName );
                    }
                }
            }
        }
        
        // --------------------------------------------------------------------
        // Initialize schema subsystem and reset registries
        // --------------------------------------------------------------------
        
        PartitionSchemaLoader schemaLoader = new PartitionSchemaLoader( schemaPartition, registries );
        Registries globalRegistries = new DefaultRegistries( "global", schemaLoader, oidRegistry );
        schemaLoader.loadEnabled( globalRegistries );
        registries = globalRegistries;
        SerializableComparator.setRegistry( globalRegistries.getComparatorRegistry() );

        SchemaOperationControl schemaControl = new SchemaOperationControl( registries, schemaLoader,
            new SchemaPartitionDao( schemaPartition, registries ) );

        schemaService = new SchemaService( registries, schemaPartition, schemaControl );


        partitionNexus = new DefaultPartitionNexus( new AttributesImpl() );
        partitionNexus.init( this );
        partitionNexus.addContextPartition( new AddContextPartitionOperationContext( schemaPartition ) );

        interceptorChain = new InterceptorChain();
        interceptorChain.init( this );

        if ( changeLog.isEnabled() )
        {
            changeLog.init( this );
        }

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "<--- DefaultDirectoryService initialized" );
        }
    }
}
