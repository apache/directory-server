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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.config.ConfigPartitionReader;
import org.apache.directory.server.config.LdifConfigExtractor;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.integration.http.HttpServer;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ntp.NtpServer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.CsnSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.UuidSyntaxChecker;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.LdapExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DirectoryServer bean used by both the daemon code and by the ServerMain here.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
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

    /** The Change Password server instance */
    private ChangePasswordServer changePwdServer;

    /** The Kerberos server instance */
    private KdcServer kdcServer;

    private HttpServer httpServer;

    private LdifPartition schemaLdifPartition;

    private SchemaManager schemaManager;

    private LdifPartition configPartition;

    private ConfigPartitionReader cpReader;

    // variables used during the initial startup to update the mandatory operational
    // attributes
    private UuidSyntaxChecker uuidChecker = new UuidSyntaxChecker();

    private CsnSyntaxChecker csnChecker = new CsnSyntaxChecker();

    private GeneralizedTimeSyntaxChecker timeChecker = new GeneralizedTimeSyntaxChecker();

    private static final Map<String, AttributeTypeOptions> MANDATORY_ENTRY_ATOP_MAP = new HashMap<String, AttributeTypeOptions>();

    private boolean isConfigPartitionFirstExtraction = false;

    private boolean isSchemaPartitionFirstExtraction = false;


    public void init( InstallationLayout layout, InstanceLayout instanceLayout ) throws Exception
    {
        File partitionsDir = instanceLayout.getPartitionsDirectory();
        if ( !partitionsDir.exists() )
        {
            LOG.info( "partition directory doesn't exist, creating {}", partitionsDir.getAbsolutePath() );
            partitionsDir.mkdirs();
        }

        LOG.info( "using partition dir {}", partitionsDir.getAbsolutePath() );
        initSchemaLdifPartition( partitionsDir );
        initConfigPartition( partitionsDir );

        cpReader = new ConfigPartitionReader( configPartition );

        // Initialize the LDAP server
        initLdap( layout, instanceLayout );

        // Initialize the NTP server
        initNtp( layout );

        // Initialize the DNS server (Not ready yet)
        // initDns( layout );

        // Initialize the DHCP server (Not ready yet)
        // initDhcp( layout );

        // Initialize the ChangePwd server (Not ready yet)
        initChangePwd( layout );

        // Initialize the Kerberos server
        initKerberos( layout );

        // initialize the jetty http server
        initHttpServer();
    }


    /**
     * initialize the schema partition by loading the schema LDIF files
     * 
     * @throws Exception in case of any problems while extracting and writing the schema files
     */
    private void initSchemaLdifPartition( File partitionsDir ) throws Exception
    {
        // Init the LdifPartition
        schemaLdifPartition = new LdifPartition();
        schemaLdifPartition.setWorkingDirectory( partitionsDir.getPath() + "/schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( partitionsDir, "schema" );

        if ( schemaRepository.exists() )
        {
            LOG.info( "schema partition already exists, skipping schema extraction" );
        }
        else
        {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( partitionsDir );
            extractor.extractOrCopy();
            isSchemaPartitionFirstExtraction = true;
        }

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err( I18n.ERR_317, LdapExceptionUtils.printErrors( errors ) ) );
        }
    }


    /**
     * 
     * initializes a LDIF partition for configuration
     * 
     * @param partitionsDir the directory where all the partitions' data is stored
     * @throws Exception in case of any issues while extracting the schema
     */
    private void initConfigPartition( File partitionsDir ) throws Exception
    {

        File configRepository = new File( partitionsDir, "config" );

        if ( configRepository.exists() )
        {
            LOG.info( "config partition already exists, skipping default config extraction" );
        }
        else
        {
            LdifConfigExtractor.extract( partitionsDir, true );
            isConfigPartitionFirstExtraction = true;
        }

        configPartition = new LdifPartition();
        configPartition.setId( "config" );
        configPartition.setSuffix( new DN( "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );
        configPartition.setWorkingDirectory( partitionsDir.getPath() + "/config" );
        configPartition.setPartitionDir( new File( configPartition.getWorkingDirectory() ) );

        configPartition.initialize();
    }


    /**
     * Initialize the LDAP server
     */
    private void initLdap( InstallationLayout layout, InstanceLayout instanceLayout ) throws Exception
    {
        LOG.info( "Starting the LDAP server" );

        printBanner( BANNER_LDAP );
        long startTime = System.currentTimeMillis();

        DirectoryService directoryService = cpReader.createDirectoryService();
        directoryService.setSchemaManager( schemaManager );

        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();
        schemaPartition.setWrappedPartition( schemaLdifPartition );
        schemaPartition.setSchemaManager( schemaManager );

        directoryService.addPartition( configPartition );

        directoryService.setWorkingDirectory( instanceLayout.getInstanceDirectory() );

        ldapServer = cpReader.createLdapServer();
        ldapServer.setDirectoryService( directoryService );

        directoryService.startup();

        AttributeType ocAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        MANDATORY_ENTRY_ATOP_MAP.put( ocAt.getName(), new AttributeTypeOptions( ocAt ) );

        AttributeType uuidAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT );
        MANDATORY_ENTRY_ATOP_MAP.put( uuidAt.getName(), new AttributeTypeOptions( uuidAt ) );

        AttributeType csnAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );
        MANDATORY_ENTRY_ATOP_MAP.put( csnAt.getName(), new AttributeTypeOptions( csnAt ) );

        AttributeType creatorAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATORS_NAME_AT );
        MANDATORY_ENTRY_ATOP_MAP.put( creatorAt.getName(), new AttributeTypeOptions( creatorAt ) );

        AttributeType createdTimeAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CREATE_TIMESTAMP_AT );
        MANDATORY_ENTRY_ATOP_MAP.put( createdTimeAt.getName(), new AttributeTypeOptions( createdTimeAt ) );

        if ( isConfigPartitionFirstExtraction )
        {
            LOG.info( "begining to update config partition LDIF files after modifying manadatory attributes" );

            updateMandatoryOpAttributes( configPartition, directoryService );

            LOG.info( "config partition data was successfully updated" );
        }

        if ( isSchemaPartitionFirstExtraction )
        {
            LOG.info( "begining to update schema partition LDIF files after modifying manadatory attributes" );

            updateMandatoryOpAttributes( schemaLdifPartition, directoryService );

            LOG.info( "schema partition data was successfully updated" );
        }

        // And start the server now
        start();

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "LDAP server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "" );
        }
    }


    /**
     * Initialize the NTP server
     */
    private void initNtp( InstallationLayout layout ) throws Exception
    {
        ntpServer = cpReader.createNtpServer();
        if ( ntpServer == null )
        {
            LOG
                .info( "Cannot find any reference to the NTP Server in the configuration : the server won't be started" );
            return;
        }

        getDirectoryService().startup();
        ntpServer.setDirectoryService( getDirectoryService() );

        System.out.println( "Starting the NTP server" );
        LOG.info( "Starting the NTP server" );

        printBanner( BANNER_NTP );
        long startTime = System.currentTimeMillis();

        ntpServer.start();
        System.out.println( "NTP Server started" );

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
     * Initialize the KERBEROS server
     */
    private void initKerberos( InstallationLayout layout ) throws Exception
    {
        kdcServer = cpReader.createKdcServer();
        if ( kdcServer == null )
        {
            LOG
                .info( "Cannot find any reference to the Kerberos Server in the configuration : the server won't be started" );
            return;
        }

        getDirectoryService().startup();
        kdcServer.setDirectoryService( getDirectoryService() );

        System.out.println( "Starting the Kerberos server" );
        LOG.info( "Starting the Kerberos server" );

        printBanner( BANNER_KERBEROS );
        long startTime = System.currentTimeMillis();

        kdcServer.start();

        System.out.println( "Kerberos server started" );

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Kerberos server: started in {} milliseconds", ( System.currentTimeMillis() - startTime ) + "" );
        }
    }


    /**
     * Initialize the Change Password server
     */
    private void initChangePwd( InstallationLayout layout ) throws Exception
    {

        changePwdServer = cpReader.createChangePwdServer();
        if ( changePwdServer == null )
        {
            LOG
                .info( "Cannot find any reference to the Change Password Server in the configuration : the server won't be started" );
            return;
        }

        getDirectoryService().startup();
        changePwdServer.setDirectoryService( getDirectoryService() );

        System.out.println( "Starting the Change Password server" );
        LOG.info( "Starting the Change Password server" );

        printBanner( BANNER_CHANGE_PWD );
        long startTime = System.currentTimeMillis();

        changePwdServer.start();

        System.out.println( "Change Password server started" );
        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Change Password server: started in {} milliseconds", ( System.currentTimeMillis() - startTime )
                + "" );
        }
    }


    private void initHttpServer() throws Exception
    {
        httpServer = cpReader.createHttpServer();
        if ( httpServer == null )
        {
            LOG
                .info( "Cannot find any reference to the HTTP Server in the configuration : the server won't be started" );
            return;
        }

        if ( httpServer != null )
        {
            httpServer.start( getDirectoryService() );
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


    public void start()
    {
        try
        {
            ldapServer.start();
        }
        catch ( Exception e )
        {
            LOG.error( "Cannot start the server : " + e.getMessage() );
        }
    }


    public void stop() throws Exception
    {

        // Stops the server
        if ( ldapServer != null )
        {
            ldapServer.stop();
        }

        if ( kdcServer != null )
        {
            kdcServer.stop();
        }

        if ( changePwdServer != null )
        {
            changePwdServer.stop();
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
        ldapServer.getDirectoryService().shutdown();
    }


    public void destroy()
    {
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

    private static final String BANNER_KERBEROS = "           _                     _          _  __ ____   ___    \n"
        + "          / \\   _ __    ___  ___| |__   ___| |/ /|  _ \\ / __|   \n"
        + "         / _ \\ | '_ \\ / _` |/ __| '_ \\ / _ \\ ' / | | | / /      \n"
        + "        / ___ \\| |_) | (_| | (__| | | |  __/ . \\ | |_| \\ \\__    \n"
        + "       /_/   \\_\\ .__/ \\__,_|\\___|_| |_|\\___|_|\\_\\|____/ \\___|   \n"
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

    private static final String BANNER_CHANGE_PWD = "         ___                              ___ __  __ __  ______    \n"
        + "        / __|_       ___ _ __   ____  ___|  _ \\ \\ \\ / / / |  _ \\   \n"
        + "       / /  | |__  / _` | '  \\ / ___\\/ _ \\ |_) \\ \\ / /\\/ /| | | |  \n"
        + "       \\ \\__| '_  \\ (_| | |\\  | |___ | __/  __/ \\ ' /   / | |_| |  \n"
        + "        \\___|_| |_|\\__,_|_| |_|\\__. |\\___| |     \\_/ \\_/  |____/   \n"
        + "                                  |_|    |_|                       \n";


    /**
     * Print the banner for a server
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
     * @throws Exception
     */
    public void updateMandatoryOpAttributes( Partition partition, DirectoryService dirService ) throws Exception
    {
        CoreSession session = dirService.getAdminSession();

        String adminDn = session.getEffectivePrincipal().getName();

        ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );

        EntryFilteringCursor cursor = session.search( partition.getSuffix(), SearchScope.SUBTREE, filter,
            AliasDerefMode.NEVER_DEREF_ALIASES, new HashSet( MANDATORY_ENTRY_ATOP_MAP.values() ) );
        cursor.beforeFirst();

        List<Modification> mods = new ArrayList<Modification>();

        while ( cursor.next() )
        {
            ClonedServerEntry entry = cursor.get();

            AttributeType atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.ENTRY_UUID_AT ).getAttributeType();

            EntryAttribute uuidAt = entry.get( atType );
            String uuid = ( uuidAt == null ? null : uuidAt.getString() );

            if ( !uuidChecker.isValidSyntax( uuid ) )
            {
                uuidAt = new DefaultEntryAttribute( atType, UUID.randomUUID().toString() );
            }

            Modification uuidMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, uuidAt );
            mods.add( uuidMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.ENTRY_CSN_AT ).getAttributeType();
            EntryAttribute csnAt = entry.get( atType );
            String csn = ( csnAt == null ? null : csnAt.getString() );

            if ( !csnChecker.isValidSyntax( csn ) )
            {
                csnAt = new DefaultEntryAttribute( atType, dirService.getCSN().toString() );
            }

            Modification csnMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, csnAt );
            mods.add( csnMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.CREATORS_NAME_AT ).getAttributeType();
            EntryAttribute creatorAt = entry.get( atType );
            String creator = ( creatorAt == null ? "" : creatorAt.getString().trim() );

            if ( ( creator.length() == 0 ) || ( !DN.isValid( creator ) ) )
            {
                creatorAt = new DefaultEntryAttribute( atType, adminDn );
            }

            Modification creatorMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, creatorAt );
            mods.add( creatorMod );

            atType = MANDATORY_ENTRY_ATOP_MAP.get( SchemaConstants.CREATE_TIMESTAMP_AT ).getAttributeType();
            EntryAttribute createdTimeAt = entry.get( atType );
            String createdTime = ( createdTimeAt == null ? null : createdTimeAt.getString() );

            if ( !timeChecker.isValidSyntax( createdTime ) )
            {
                createdTimeAt = new DefaultEntryAttribute( atType, DateUtils.getGeneralizedTime() );
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
