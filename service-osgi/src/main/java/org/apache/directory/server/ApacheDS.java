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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.directory.server.component.hub.ComponentHub;
import org.apache.directory.server.component.hub.client.HubClientInterceptors;
import org.apache.directory.server.component.hub.client.HubClientPartitions;
import org.apache.directory.server.component.hub.client.HubClientServers;
import org.apache.directory.server.component.utilities.ADSConstants;
import org.apache.directory.server.component.utilities.ADSOSGIEventsHelper;
import org.apache.directory.server.config.ConfigPartitionReader;
import org.apache.directory.server.config.LdifConfigExtractor;
import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.CsnSyntaxChecker;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.GeneralizedTimeSyntaxChecker;
import org.apache.directory.shared.ldap.model.schema.syntaxCheckers.UuidSyntaxChecker;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.util.exception.Exceptions;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * IPojo Component that represents live ApacheDS instance
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 */
@Component(name = "ApacheDSInstance")
@Instantiate(name = "ApacheDSInstance-Default")
public class ApacheDS implements EventHandler
{
    /** A logger for this class */
    private final Logger LOG = LoggerFactory
        .getLogger( ApacheDS.class );

    /** Property for specifying instance directory. It is "default" by default */
    @Property(name = "apacheds.instance.dir", value = "default", mandatory = true)
    private String instanceDir;

    /** BundleContext reference for apacheds-servive-osgi */
    public static BundleContext bundleContext;

    /** Instance Layout description. */
    private InstanceLayout instanceLayout;

    /** DirectoryService reference */
    private DirectoryService directoryService;

    /** The SchemaPartition */
    private SchemaPartition schemaPartition;

    /** The Schema partition */
    private LdifPartition schemaLdifPartition;

    /** The SchemaManager instance */
    private SchemaManager schemaManager;

    /** The configuration partition */
    private SingleFileLdifPartition configPartition;

    /** HubListener for Interceptors */
    private HubClientInterceptors interceptorsManager;

    /** HubListener for Partitions */
    private HubClientPartitions partitionsManager;

    /** HubListener for Servers */
    private HubClientServers serversManager;

    /** Reference to main plugin layer. */
    private ComponentHub componentHub;

    // variables used during the initial startup to update the mandatory operational
    // attributes
    /** The UUID syntax checker instance */
    private UuidSyntaxChecker uuidChecker = new UuidSyntaxChecker();

    /** The CSN syntax checker instance */
    private CsnSyntaxChecker csnChecker = new CsnSyntaxChecker();

    private GeneralizedTimeSyntaxChecker timeChecker = new GeneralizedTimeSyntaxChecker();

    private static final Map<String, AttributeTypeOptions> MANDATORY_ENTRY_ATOP_MAP = new HashMap<String, AttributeTypeOptions>();

    private boolean isSchemaPartitionFirstExtraction = false;

    private boolean isConfigPartitionFirstExtraction = false;

    private boolean coreInterceptorsReady = false;

    private boolean corePartitionsReady = false;


    /**
     * Will be called, when this component instance is validated,
     * Means all of its requirements are satisfied.
     *
     */
    @Validate
    public void validated()
    {
        /*
         * If ApacheDS component is validated, then it means,
         * mandatory instanceDir property has some value.
         */
        instanceLayout = new InstanceLayout( instanceDir );

        // Register the class for OSGI Event handling
        String[] topics = new String[]
            {
                ADSOSGIEventsHelper.getTopic_CoreInterceptorsReady( instanceDir ),
                ADSOSGIEventsHelper.getTopic_CorePartitionsReady( instanceDir )
        };

        Dictionary props = new Hashtable();
        props.put( EventConstants.EVENT_TOPIC, topics );
        bundleContext.registerService( EventHandler.class.getName(), this, props );

        /**
         * Calls the initialization and running on new thread
         * to seperate the execution from the IPojo management thread.
         */
        new Thread( new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    initiateComponentManagement();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        } ).start();
    }


    /**
     * Will be called when this component instance is invalidated,
     * means one of its requirements is lost.
     *
     */
    @Invalidate
    public void invalidated()
    {
        new Thread( new Runnable()
        {

            @Override
            public void run()
            {
                stopADS();
            }
        } ).start();
    }


    /**
     * Initiates the component management for ApacheDS.
     *
     */
    private void initiateComponentManagement() throws Exception
    {
        // Init SchemaPartition and 'Config Partition' for ComponentHub use.
        initSchemaManagerAndPartition();
        initConfigPartition();

        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                // Create the Component Hub object for component management.
                componentHub = ComponentHub.createIPojoInstance( instanceDir, schemaPartition, configPartition );
                
            }
        }).start();
        

        // Instantiate HubClients for Interceptors and Partitions
        interceptorsManager = new HubClientInterceptors();
        partitionsManager = new HubClientPartitions();

        // Register Hub Clients with the component hub
        //componentHub.registerListener( ADSConstants.ADS_COMPONENT_TYPE_INTERCEPTOR, interceptorsManager );
        //componentHub.registerListener( ADSConstants.ADS_COMPONENT_TYPE_PARTITION, partitionsManager );
    }


    private void initSchemaManagerAndPartition() throws Exception
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
        schemaManager = new DefaultSchemaManager( loader );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err( I18n.ERR_317, Exceptions.printErrors( errors ) ) );
        }

        // Init the LdifPartition
        schemaLdifPartition = new LdifPartition( schemaManager );
        schemaLdifPartition.setPartitionPath( schemaPartitionDirectory.toURI() );

        // Init the SchemaPartition
        schemaPartition = new SchemaPartition( schemaManager );
        schemaPartition.setWrappedPartition( schemaLdifPartition );
        schemaPartition.initialize();
    }


    /**
     * 
     * initializes a LDIF partition for configuration
     * 
     * @param instanceLayout the instance layout
     * @throws Exception in case of any issues while extracting the schema
     */
    private void initConfigPartition() throws Exception
    {
        File confFile = new File( instanceLayout.getConfDirectory(), LdifConfigExtractor.LDIF_CONFIG_FILE );

        if ( confFile.exists() )
        {
            LOG.info( "config partition already exists, skipping default config extraction" );
        }
        else
        {
            LdifConfigExtractor.extractSingleFileConfig( instanceLayout.getConfDirectory(),
                LdifConfigExtractor.LDIF_CONFIG_FILE, true );
            isConfigPartitionFirstExtraction = true;
        }

        configPartition = new SingleFileLdifPartition( schemaManager );
        configPartition.setId( "config" );
        configPartition.setPartitionPath( confFile.toURI() );
        configPartition.setSuffixDn( new Dn( schemaManager, "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );

        configPartition.initialize();
    }


    private void initDirectoryService() throws Exception
    {
        // Read the configuration
        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition );
        ConfigBean configBean = cpReader.readConfig();

        LOG.info( "Initializing the DirectoryService..." );

        long startTime = System.currentTimeMillis();

        DirectoryServiceBean directoryServiceBean = configBean.getDirectoryServiceBean();

        // Get the core interceptor and partitions list from HubClients.
        // Core ones must be set at the timt this method is called.
        List<Interceptor> coreInterceptors = interceptorsManager.getCoreInterceptors();
        Map<String, Partition> corePartitions = partitionsManager.getCorePartitions();

        directoryService = DirectoryServiceBuilder.createDirectoryService( directoryServiceBean, instanceLayout,
            schemaManager, coreInterceptors, corePartitions );

        directoryService.setSchemaPartition( schemaPartition );

        directoryService.addPartition( configPartition );

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

            // disable writes to the disk upon every modification to improve performance
            configPartition.setEnableRewriting( false );

            // perform updates
            updateMandatoryOpAttributes( configPartition, directoryService );

            // enable writes to disk, this will save the partition data first if found dirty
            configPartition.setEnableRewriting( true );

            LOG.info( "config partition data was successfully updated" );
        }

        if ( isSchemaPartitionFirstExtraction )
        {
            LOG.info( "begining to update schema partition LDIF files after modifying manadatory attributes" );

            updateMandatoryOpAttributes( schemaLdifPartition, directoryService );

            LOG.info( "schema partition data was successfully updated" );
        }

        LOG.info( "DirectoryService initialized in {} milliseconds", ( System.currentTimeMillis() - startTime ) );

    }


    /*
     * Stops the ApacheDS instance.
     */
    private void stopADS()
    {
        // Stops the servers first.
        serversManager.stopServers();

        try
        {
            // Shutdown the DirectoryService reference
            directoryService.shutdown();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    /**
     * OSGI Event Handler method
     */
    @Override
    public void handleEvent( Event event )
    {
        String topic = event.getTopic();
        if ( topic.equals( ADSOSGIEventsHelper.getTopic_CoreInterceptorsReady( instanceDir ) ) )
        {
            coreInterceptorsReady = true;
        }
        else if ( topic.equals( ADSOSGIEventsHelper.getTopic_CorePartitionsReady( instanceDir ) ) )
        {
            corePartitionsReady = true;
        }

        if ( coreInterceptorsReady && corePartitionsReady )
        {
            // Core Interceptors and Partitions are ready, create the DirectoryService reference
            try
            {
                initDirectoryService();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                return;
            }

            /*
             * DirectoryService is created, now inform the ComponentHub and HubClients
             * to pair with DirectoryService reference just created.
             */
            ServiceReference ref = bundleContext.getServiceReference( EventAdmin.class.getName() );
            if ( ref != null )
            {
                EventAdmin eventAdmin = ( EventAdmin ) bundleContext.getService( ref );

                Dictionary properties = new Hashtable();
                properties.put( ADSOSGIEventsHelper.ADS_EVENT_ARG_DS, directoryService );

                Event reportGeneratedEvent = new Event( ADSOSGIEventsHelper.getTopic_DSInitialized( instanceDir ),
                    properties );

                eventAdmin.sendEvent( reportGeneratedEvent );
            }
        }
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

        EntryFilteringCursor cursor = session.search( partition.getSuffixDn(), SearchScope.SUBTREE, filter,
            AliasDerefMode.NEVER_DEREF_ALIASES, new HashSet<AttributeTypeOptions>( MANDATORY_ENTRY_ATOP_MAP.values() ) );
        cursor.beforeFirst();

        List<Modification> mods = new ArrayList<Modification>();

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
                createdTimeAt = new DefaultAttribute( atType, DateUtils.getGeneralizedTime() );
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
