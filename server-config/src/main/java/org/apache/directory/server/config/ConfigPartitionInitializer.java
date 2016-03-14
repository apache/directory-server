/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config;


import java.io.File;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tool for initializing the configuration patition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigPartitionInitializer
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigPartitionInitializer.class );

    private SchemaManager schemaManager;

    private InstanceLayout instanceLayout;

    private DnFactory dnFactory;

    private CacheService cacheService;


    /**
     * Creates a new instance of ConfigPartitionHelper.
     *
     * @param instanceLayout the instance layout where the configuration partition lives in
     * @param dnFactory the DN factory
     * @param cacheService the cache service
     * @param schemaManager the schema manager
     */
    public ConfigPartitionInitializer( InstanceLayout instanceLayout, DnFactory dnFactory, CacheService cacheService,
        SchemaManager schemaManager )
    {
        this.instanceLayout = instanceLayout;
        this.dnFactory = dnFactory;
        this.cacheService = cacheService;
        this.schemaManager = schemaManager;
    }


    /**
     * Initializes the configuration partition. If no configuration partition exists the default
     * configuration is extracted. If the old single-file configuration exists it is migrated 
     * to new multi-file LDIF partition. 
     *
     * @return the initialized configuration partition
     * @throws Exception
     */
    public LdifPartition initConfigPartition() throws Exception
    {
        LdifPartition configPartition = new LdifPartition( schemaManager, dnFactory );
        configPartition.setId( "config" );
        configPartition.setPartitionPath( instanceLayout.getConfDirectory().toURI() );
        configPartition.setSuffixDn( new Dn( schemaManager, "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );
        configPartition.setCacheService( cacheService );

        File newConfigDir = new File( instanceLayout.getConfDirectory(), configPartition.getSuffixDn().getName() );

        File oldConfFile = new File( instanceLayout.getConfDirectory(), LdifConfigExtractor.LDIF_CONFIG_FILE );

        boolean migrate = false;

        File tempConfFile = null;

        if ( oldConfFile.exists() )
        {
            if ( newConfigDir.exists() )
            {
                // conflict, which one to choose
                String msg = "Conflict in selecting configuration source, both " + LdifConfigExtractor.LDIF_CONFIG_FILE
                    + " and " + newConfigDir.getName() + " exist" + " delete either one of them and restart the server";
                LOG.warn( msg );
                throw new IllegalStateException( msg );
            }

            migrate = true;
        }
        else if ( !newConfigDir.exists() )
        {
            String file = LdifConfigExtractor.extractSingleFileConfig( instanceLayout.getConfDirectory(),
                LdifConfigExtractor.LDIF_CONFIG_FILE, true );
            tempConfFile = new File( file );
        }

        LdifReader reader = null;

        if ( migrate )
        {
            LOG.info( "Old config partition detected, converting to multiple LDIF file configuration model" );
            reader = new LdifReader( oldConfFile, schemaManager );
        }
        else if ( tempConfFile != null )
        {
            LOG.info( "Creating default configuration" );
            reader = new LdifReader( tempConfFile, schemaManager );
        }

        if ( reader != null )
        {
            // sometimes user may have forgotten to delete ou=config.ldif after deleting ou=config folder
            File residue = new File( instanceLayout.getConfDirectory(), "ou=config.ldif" );
            if ( residue.exists() )
            {
                residue.delete();
            }

            // just for the sake of above check the initialization part is kept here
            // and in the below else block
            configPartition.initialize();

            CsnFactory csnFactory = new CsnFactory( 0 );

            while ( reader.hasNext() )
            {
                Entry entry = reader.next().getEntry();

                // add the mandatory attributes
                if ( !entry.containsAttribute( SchemaConstants.ENTRY_UUID_AT ) )
                {
                    String uuid = UUID.randomUUID().toString();
                    entry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
                }

                if ( !entry.containsAttribute( SchemaConstants.ENTRY_CSN_AT ) )
                {
                    entry.removeAttributes( SchemaConstants.ENTRY_CSN_AT );
                    entry.add( SchemaConstants.ENTRY_CSN_AT, csnFactory.newInstance().toString() );
                }

                if ( !entry.containsAttribute( SchemaConstants.CREATORS_NAME_AT ) )
                {
                    entry.add( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
                }

                if ( !entry.containsAttribute( SchemaConstants.CREATE_TIMESTAMP_AT ) )
                {
                    entry.add( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
                }

                AddOperationContext addContext = new AddOperationContext( null, entry );
                configPartition.add( addContext );
            }

            reader.close();

            if ( migrate )
            {
                oldConfFile.renameTo( new File( oldConfFile.getAbsolutePath() + "_migrated" ) );
            }
            else
            {
                tempConfFile.delete();
            }
        }
        else
        {
            configPartition.initialize();
        }

        return configPartition;
    }

}
