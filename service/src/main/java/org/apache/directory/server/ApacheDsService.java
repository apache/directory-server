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
package org.apache.directory.server;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.CsnSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.config.ConfigPartitionReader;
import org.apache.directory.server.config.ConfigPartitionInitializer;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.HttpServerBean;
import org.apache.directory.server.config.beans.JdbmPartitionBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.NtpServerBean;
import org.apache.directory.server.config.builder.ServiceBuilder;
import org.apache.directory.server.config.listener.ConfigChangeListener;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.event.EventType;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.security.CertificateUtil;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.integration.http.HttpServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ntp.NtpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class used to start various servers in a given {@link InstanceLayout}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("restriction")
public class ApacheDsService
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDsService.class );

    /** The LDAP server instance */
    private LdapServer ldapServer;

    /** The NTP server instance */
    private NtpServer ntpServer;

    /** The DNS server instance */
    //    private DnsServer dnsServer;

    /** The started HttpServer */
    private HttpServer httpServer;

    /** The Schema partition */
    private LdifPartition schemaLdifPartition;

    /** The SchemaManager instance */
    private SchemaManager schemaManager;

    /** The configuration partition */
    private LdifPartition configPartition;

    // variables used during the initial startup to update the mandatory operational
    // attributes
    /** The UUID syntax checker instance */
    private UuidSyntaxChecker uuidChecker = UuidSyntaxChecker.INSTANCE;

    /** The CSN syntax checker instance */
    private CsnSyntaxChecker csnChecker = CsnSyntaxChecker.INSTANCE;

    private GeneralizedTimeSyntaxChecker timeChecker = GeneralizedTimeSyntaxChecker.INSTANCE;

    private static final Map<String, AttributeTypeOptions> MANDATORY_ENTRY_ATOP_MAP = new HashMap<>();
    private static final String[] MANDATORY_ENTRY_ATOP_AT = new String[5];

    private boolean isSchemaPartitionFirstExtraction = false;


    /**
     * Starts various services configured according to the
     * 
     * @param instanceLayout the on disk location's layout of the instance to be started
     * @throws Exception If we failed to start the server
     */
    public void start( InstanceLayout instanceLayout ) throws Exception
    {
        start( instanceLayout, true );
    }

    
    /**
     * starts various services configured according to the
     * configuration present in the given instance's layout
     *
     * @param instanceLayout the on disk location's layout of the instance to be started
     * @param startServers Tell the server that the various servers have to be started too
     * @throws Exception If we failed to start the server
     */
    public void start( InstanceLayout instanceLayout, boolean startServers ) throws Exception
    {
        File partitionsDir = instanceLayout.getPartitionsDirectory();

        if ( !partitionsDir.exists() )
        {
            LOG.info( "partition directory doesn't exist, creating {}", partitionsDir.getAbsolutePath() );

            if ( !partitionsDir.mkdirs() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, partitionsDir ) );
            }
        }

        LOG.info( "using partition dir {}", partitionsDir.getAbsolutePath() );

        initSchemaManager( instanceLayout );
        DnFactory bootstrapDnFactory = new DefaultDnFactory( schemaManager, 100 );
        initSchemaLdifPartition( instanceLayout, bootstrapDnFactory );
        initConfigPartition( instanceLayout, bootstrapDnFactory );

        // Read the configuration
        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );

        ConfigBean configBean = cpReader.readConfig();

        DirectoryServiceBean directoryServiceBean = configBean.getDirectoryServiceBean();

        /*
         * Calculate the DN cache size: from all defined partitions get the max cache size setting.
         * Note: currently only JDBM partition beans have such a setting.
         */
        int dnCacheSize = directoryServiceBean.getPartitions().stream()
            .filter( JdbmPartitionBean.class::isInstance )
            .map( JdbmPartitionBean.class::cast )
            .map( JdbmPartitionBean::getPartitionCacheSize )
            .mapToInt( Integer::intValue )
            .max().orElse( AbstractBTreePartition.DEFAULT_CACHE_SIZE );
        DnFactory dnFactory = new DefaultDnFactory( schemaManager, dnCacheSize );

        // Initialize the DirectoryService now
        DirectoryService directoryService = initDirectoryService( instanceLayout, directoryServiceBean, dnFactory );

        // start the LDAP server
        LdapServerBean ldapServerBean = directoryServiceBean.getLdapServerBean();
        
        if ( ldapServerBean.getLdapServerKeystoreFile() == null )
        {
            File ldapServerKeystoreFile = instanceLayout.getKeyStoreFile();
            
            if ( !ldapServerKeystoreFile.exists() )
            {
                ldapServerBean.setLdapServerCertificatePassword( "secret" );

                // We need to create a KeyStore
                ldapServerKeystoreFile = CertificateUtil.createTempKeyStore( "tempks", "secret".toCharArray() );
            }
            
            ldapServerBean.setLdapServerKeystoreFile( ldapServerKeystoreFile.getAbsolutePath() );
        }
        
        if ( ldapServerBean.getLdapServerCertificatePassword() == null )
        {
            ldapServerBean.setLdapServerCertificatePassword( "secret" );
        }
        
        startLdap( ldapServerBean, directoryService, startServers );

        // start the NTP server
        startNtp( directoryServiceBean.getNtpServerBean(), directoryService, startServers );

        // Initialize the DNS server (Not ready yet)
        // initDns( configBean );

        // Initialize the DHCP server (Not ready yet)
        // initDhcp( configBean );

        // start the jetty http server
        startHttpServer( directoryServiceBean.getHttpServerBean(), directoryService, startServers );
        
        LOG.info( "Registering config change listener" );
        ConfigChangeListener configListener = new ConfigChangeListener( cpReader, directoryService );

        NotificationCriteria criteria = new NotificationCriteria( directoryService.getSchemaManager() );
        criteria.setBase( configPartition.getSuffixDn() );
        criteria.setEventMask( EventType.ALL_EVENT_TYPES_MASK );
        
        PresenceNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );
        criteria.setFilter( filter );
        criteria.setScope( SearchScope.SUBTREE );
        
        directoryService.getEventService().addListener( configListener, criteria );
    }


    /**
     * Try to repair the partitions. Precondition is that this service was started before.
     *
     * @param instanceLayout the on disk location's layout of the intance to be repaired
     * @throws Exception If the repair failed
     */
    public void repair( InstanceLayout instanceLayout ) throws Exception
    {
        File partitionsDir = instanceLayout.getPartitionsDirectory();

        System.out.println( "Repairing partition dir " + partitionsDir.getAbsolutePath() );

        Set<? extends Partition> partitions = getDirectoryService().getPartitions();

        // Iterate on the partitions to repair them
        for ( Partition partition : partitions )
        {
            try
            {
                partition.repair();
            }
            catch ( Exception e )
            {
                System.out.println( "Failed to repair the partition " + partition.getId() );
                e.printStackTrace();
                return;
            }
        }
    }


    /**
     * Initialize the schema Manager by loading the schema LDIF files
     * 
     * @param instanceLayout the instance layout
     * @throws Exception in case of any problems while extracting and writing the schema files
     */
    private void initSchemaManager( InstanceLayout instanceLayout ) throws Exception
    {
        File schemaPartitionDirectory = new File( instanceLayout.getPartitionsDirectory(), "schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        if ( schemaPartitionDirectory.exists() )
        {
            LOG.info( "schema partition already exists, skipping schema extraction" );
        }
        else
        {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( instanceLayout.getPartitionsDirectory() );
            extractor.extractOrCopy();
            isSchemaPartitionFirstExtraction = true;
        }

        SchemaLoader loader = new LdifSchemaLoader( schemaPartitionDirectory );
        schemaManager = new DefaultSchemaManager( loader.getAllSchemas() );
        
        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( !errors.isEmpty() )
        {
            throw new Exception( I18n.err( I18n.ERR_317, Exceptions.printErrors( errors ) ) );
        }
    }


    /**
     * Initialize the schema partition
     * 
     * @param instanceLayout the instance layout
     */
    private void initSchemaLdifPartition( InstanceLayout instanceLayout, DnFactory dnFactory )
    {
        File schemaPartitionDirectory = new File( instanceLayout.getPartitionsDirectory(), "schema" );

        // Init the LdifPartition
        schemaLdifPartition = new LdifPartition( schemaManager, dnFactory );
        schemaLdifPartition.setPartitionPath( schemaPartitionDirectory.toURI() );
    }


    /**
     * 
     * initializes a LDIF partition for configuration
     * 
     * @param instanceLayout the instance layout
     * @param cacheService the Cache service
     * @throws Exception in case of any issues while extracting the schema
     */
    private void initConfigPartition( InstanceLayout instanceLayout, DnFactory dnFactory )
        throws Exception
    {
        ConfigPartitionInitializer initializer = new ConfigPartitionInitializer( instanceLayout, dnFactory,
            schemaManager );
        configPartition = initializer.initConfigPartition();
    }


    private DirectoryService initDirectoryService( InstanceLayout instanceLayout,
        DirectoryServiceBean directoryServiceBean, DnFactory dnFactory ) throws Exception
    {
         LOG.info( "Initializing the DirectoryService..." );

        long startTime = System.currentTimeMillis();

        DirectoryService directoryService = ServiceBuilder.createDirectoryService( directoryServiceBean,
            instanceLayout, schemaManager );

        // Inject the DnFactory
        directoryService.setDnFactory( dnFactory );

        // The schema partition
        SchemaPartition schemaPartition = new SchemaPartition( schemaManager );
        schemaPartition.setWrappedPartition( schemaLdifPartition );
        directoryService.setSchemaPartition( schemaPartition );

        directoryService.addPartition( configPartition );

        // Store the default directories
        directoryService.setInstanceLayout( instanceLayout );

        directoryService.startup();

        AttributeType ocAt = directoryService.getAtProvider().getObjectClass();
        MANDATORY_ENTRY_ATOP_MAP.put( ocAt.getName(), new AttributeTypeOptions( ocAt ) );

        AttributeType uuidAt = directoryService.getAtProvider().getEntryUUID();
        MANDATORY_ENTRY_ATOP_MAP.put( uuidAt.getName(), new AttributeTypeOptions( uuidAt ) );

        AttributeType csnAt = directoryService.getAtProvider().getEntryCSN();
        MANDATORY_ENTRY_ATOP_MAP.put( csnAt.getName(), new AttributeTypeOptions( csnAt ) );

        AttributeType creatorAt = directoryService.getAtProvider().getCreatorsName();
        MANDATORY_ENTRY_ATOP_MAP.put( creatorAt.getName(), new AttributeTypeOptions( creatorAt ) );

        AttributeType createdTimeAt = directoryService.getAtProvider().getCreateTimestamp();
        MANDATORY_ENTRY_ATOP_MAP.put( createdTimeAt.getName(), new AttributeTypeOptions( createdTimeAt ) );

        int pos = 0;

        for ( AttributeTypeOptions attributeTypeOptions : MANDATORY_ENTRY_ATOP_MAP.values() )
        {
            MANDATORY_ENTRY_ATOP_AT[pos++] = attributeTypeOptions.getAttributeType().getName();
        }

        if ( isSchemaPartitionFirstExtraction )
        {
            LOG.info( "begining to update schema partition LDIF files after modifying manadatory attributes" );

            updateMandatoryOpAttributes( schemaLdifPartition, directoryService );

            LOG.info( "schema partition data was successfully updated" );
        }

        LOG.info( "DirectoryService initialized in {} milliseconds", ( System.currentTimeMillis() - startTime ) );

        return directoryService;
    }


    /**
     * start the LDAP server
     */
    private void startLdap( LdapServerBean ldapServerBean, DirectoryService directoryService, boolean startServers ) throws Exception
    {
        LOG.info( "Starting the LDAP server" );
        long startTime = System.currentTimeMillis();
        
        // Add a reference to the KeyStore file, or create one if missing

        ldapServer = ServiceBuilder.createLdapServer( ldapServerBean, directoryService );

        if ( ldapServer == null )
        {
            LOG.info( "Cannot find any reference to the LDAP Server in the configuration : the server won't be started" );
            return;
        }

        printBanner( BANNER_LDAP );

        // And start the server now if required
        if ( startServers )
        {
            ldapServer.start();
        }

        LOG.info( "LDAP server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) );
    }


    /**
     * start the NTP server
     */
    private void startNtp( NtpServerBean ntpServerBean, DirectoryService directoryService, boolean startServers ) throws Exception
    {
        LOG.info( "Starting the NTP server" );
        long startTime = System.currentTimeMillis();

        ntpServer = ServiceBuilder.createNtpServer( ntpServerBean, directoryService );

        if ( ntpServer == null )
        {
            LOG.info( "Cannot find any reference to the NTP Server in the configuration : the server won't be started" );
            return;
        }

        printBanner( BANNER_NTP );

        if ( startServers )
        {
            ntpServer.start();
        }

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "NTP server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "" );
        }
    }


    /**
     * Initialize the DNS server
     */
    //    private void initDns( InstanceLayout layout ) throws Exception
    //    {
    //        if ( factory == null )
    //        {
    //            return;
    //        }
    //
    //        try
    //        {
    //            dnsServer = ( DnsServer ) factory.getBean( "dnsServer" );
    //        }
    //        catch ( Exception e )
    //        {
    //            LOG.info( "Cannot find any reference to the DNS Server in the configuration : the server won't be started" );
    //            return;
    //        }
    //
    //        System.out.println( "Starting the DNS server" );
    //        LOG.info( "Starting the DNS server" );
    //
    //        printBanner( BANNER_DNS );
    //        long startTime = System.currentTimeMillis();
    //
    //        dnsServer.start();
    //        System.out.println( "DNS Server started" );
    //
    //        if ( LOG.isInfoEnabled() )
    //        {
    //            LOG.info( "DNS server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "" );
    //        }
    //    }


    /**
     * start the embedded HTTP server
     */
    private void startHttpServer( HttpServerBean httpServerBean, DirectoryService directoryService, boolean startServers ) throws Exception
    {
        httpServer = ServiceBuilder.createHttpServer( httpServerBean, directoryService );

        if ( httpServer == null )
        {
            LOG.info( "Cannot find any reference to the HTTP Server in the configuration : the server won't be started" );
            return;
        }

        LOG.info( "Starting the Http server" );
        long startTime = System.currentTimeMillis();

        if ( startServers )
        {
            httpServer.start( getDirectoryService() );
        }

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Http server: started in {} milliseconds", ( System.currentTimeMillis() - startTime )
                + "" );
        }
    }


    public DirectoryService getDirectoryService()
    {
        return ldapServer.getDirectoryService();
    }


    public void synch() throws Exception
    {
        ldapServer.getDirectoryService().sync();
    }


    public void stop() throws Exception
    {
        DirectoryService directoryService = null;
        
        // Stops the server
        if ( ldapServer != null )
        {
            ldapServer.stop();
            directoryService = ldapServer.getDirectoryService();
        }

        if ( ntpServer != null )
        {
            ntpServer.stop();
        }

        if ( httpServer != null )
        {
            httpServer.stop();
        }

        // We now have to stop the underlaying DirectoryService
        if ( directoryService != null )
        {
            directoryService.shutdown();
        }
    }

    private static final String BANNER_LDAP = "           _                     _          ____  ____   \n"
        + "          / \\   _ __    ___  ___| |__   ___|  _ \\/ ___|  \n"
        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | \\___ \\  \n"
        + "        / ___ \\| |_) | (_| | (__| | | |  __/ |_| |___) | \n"
        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|____/  \n"
        + "               |_|                                       \n";

    private static final String BANNER_NTP = "           _                     _          _   _ _____ _ __    \n"
        + "          / \\   _ __    ___  ___| |__   ___| \\ | |_  __| '_ \\   \n"
        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ .\\| | | | | |_) |  \n"
        + "        / ___ \\| |_) | (_| | (__| | | |  __/ |\\  | | | | .__/   \n"
        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|_| \\_| |_| |_|      \n"
        + "               |_|                                              \n";


    //    private static final String BANNER_DNS =
    //          "           _                     _          ____  _   _ ____    \n"
    //        + "          / \\   _ __    ___  ___| |__   ___|  _ \\| \\ | / ___|   \n"
    //        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | |  \\| \\__  \\   \n"
    //        + "        / ___ \\| |_) | (_| | (__| | | |  __/ |_| | . ' |___) |  \n"
    //        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|_|\\__|____/   \n"
    //        + "               |_|                                              \n";
    //
    //
    //    private static final String BANNER_DHCP =
    //          "           _                     _          ____  _   _  ___ ____  \n"
    //        + "          / \\   _ __    ___  ___| |__   ___|  _ \\| | | |/ __|  _ \\ \n"
    //        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ | | | |_| / /  | |_) )\n"
    //        + "        / ___ \\| |_) | (_| | (__| | | |  __/ |_| |  _  \\ \\__|  __/ \n"
    //        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|____/|_| |_|\\___|_|    \n"
    //        + "               |_|                                                 \n";

    /**
     * Print the banner for a server
     * 
     * @param bannerConstant The banner to print
     */
    public static void printBanner( String bannerConstant )
    {
        System.out.println( bannerConstant );
    }


    /**
     * 
     * adds mandatory operational attributes {@link #MANDATORY_ENTRY_ATOP_MAP} and updates all the LDIF files.
     * WARN: this method is only called for the first time when schema and config files are bootstrapped
     *       afterwards it is the responsibility of the user to ensure correctness of LDIF files if modified
     *       by hand
     * 
     * Note: we do these modifications explicitly cause we have no idea if each entry's LDIF file has the
     *       correct values for all these mandatory attributes
     * 
     * @param partition instance of the partition Note: should only be those which are loaded before starting the DirectoryService
     * @param dirService the DirectoryService instance
     * @throws Exception  If the update failed
     */
    public void updateMandatoryOpAttributes( Partition partition, DirectoryService dirService ) throws Exception
    {
        CoreSession session = dirService.getAdminSession();

        String adminDn = session.getEffectivePrincipal().getName();

        ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );

        Cursor<Entry> cursor = session.search( partition.getSuffixDn(), SearchScope.SUBTREE, filter,
            AliasDerefMode.NEVER_DEREF_ALIASES, MANDATORY_ENTRY_ATOP_AT );
        cursor.beforeFirst();

        List<Modification> mods = new ArrayList<>();

        while ( cursor.next() )
        {
            Entry entry = cursor.get();

            AttributeType atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.ENTRY_UUID_AT ).getAttributeType();

            Attribute uuidAt = entry.get( atType );
            String uuid = ( uuidAt == null ? null : uuidAt.getString() );

            if ( !uuidChecker.isValidSyntax( uuid ) )
            {
                uuidAt = new DefaultAttribute( atType, UUID.randomUUID().toString() );
            }

            Modification uuidMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, uuidAt );
            mods.add( uuidMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.ENTRY_CSN_AT ).getAttributeType();
            Attribute csnAt = entry.get( atType );
            String csn = ( csnAt == null ? null : csnAt.getString() );

            if ( !csnChecker.isValidSyntax( csn ) )
            {
                csnAt = new DefaultAttribute( atType, dirService.getCSN().toString() );
            }

            Modification csnMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, csnAt );
            mods.add( csnMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.CREATORS_NAME_AT ).getAttributeType();
            Attribute creatorAt = entry.get( atType );
            String creator = ( creatorAt == null ? "" : creatorAt.getString().trim() );

            if ( ( creator.length() == 0 ) || ( !Dn.isValid( creator ) ) )
            {
                creatorAt = new DefaultAttribute( atType, adminDn );
            }

            Modification creatorMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, creatorAt );
            mods.add( creatorMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.CREATE_TIMESTAMP_AT ).getAttributeType();
            Attribute createdTimeAt = entry.get( atType );
            String createdTime = ( createdTimeAt == null ? null : createdTimeAt.getString() );

            if ( !timeChecker.isValidSyntax( createdTime ) )
            {
                createdTimeAt = new DefaultAttribute( atType, DateUtils.getGeneralizedTime( dirService.getTimeProvider() ) );
            }

            Modification createdMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, createdTimeAt );
            mods.add( createdMod );

            if ( !mods.isEmpty() )
            {
                LOG.debug( "modifying the entry {} after adding missing manadatory operational attributes",
                    entry.getDn() );
                ModifyOperationContext modifyContext = new ModifyOperationContext( session );
                modifyContext.setEntry( entry );
                modifyContext.setDn( entry.getDn() );
                modifyContext.setModItems( mods );
                partition.modify( modifyContext );
            }

            mods.clear();
        }

        cursor.close();
    }

}
