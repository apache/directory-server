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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.config.ConfigurationException;
import org.apache.directory.server.config.beans.ChangeLogBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.JournalBean;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.journal.JournalStore;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.journal.DefaultJournal;
import org.apache.directory.server.core.journal.DefaultJournalStore;
import org.apache.directory.shared.ldap.model.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DirectoryServiceBuilder
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DirectoryServiceBuilder.class );

    /** LDIF file filter */
    private static FilenameFilter ldifFilter = new FilenameFilter()
    {
        public boolean accept( File file, String name )
        {
            if ( file.isDirectory() )
            {
                return true;
            }

            return Strings.toLowerCase( file.getName() ).endsWith( ".ldif" );
        }
    };


    public static DirectoryService createDirectoryService( DirectoryServiceBean directoryServiceBean,
        InstanceLayout instanceLayout,
        SchemaManager schemaManager, List<Interceptor> coreInterceptors, Map<String, Partition> corePartitions )
        throws Exception
    {

        DirectoryService directoryService = new DefaultDirectoryService();

        // The schemaManager
        directoryService.setSchemaManager( schemaManager );

        // MUST attributes
        // DirectoryService ID
        directoryService.setInstanceId( directoryServiceBean.getDirectoryServiceId() );

        // Replica ID
        directoryService.setReplicaId( directoryServiceBean.getDsReplicaId() );

        // WorkingDirectory
        directoryService.setInstanceLayout( instanceLayout );

        // Interceptors
        directoryService.setInterceptors( coreInterceptors );

        // Partitions
        Partition systemPartition = corePartitions.remove( "system" );

        if ( systemPartition == null )
        {
            //throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        directoryService.setSystemPartition( systemPartition );
        directoryService.setPartitions( new HashSet<Partition>( corePartitions.values() ) );

        // MAY attributes
        // AccessControlEnabled
        directoryService.setAccessControlEnabled( directoryServiceBean.isDsAccessControlEnabled() );

        // AllowAnonymousAccess
        directoryService.setAllowAnonymousAccess( directoryServiceBean.isDsAllowAnonymousAccess() );

        // ChangeLog
        ChangeLog cl = createChangeLog( directoryServiceBean.getChangeLog() );

        if ( cl != null )
        {
            directoryService.setChangeLog( cl );
        }

        // DenormalizedOpAttrsEnabled
        directoryService.setDenormalizeOpAttrsEnabled( directoryServiceBean.isDsDenormalizeOpAttrsEnabled() );

        // Journal
        Journal journal = createJournal( directoryServiceBean.getJournal() );

        if ( journal != null )
        {
            directoryService.setJournal( journal );
        }

        // MaxPDUSize
        directoryService.setMaxPDUSize( directoryServiceBean.getDsMaxPDUSize() );

        // PasswordHidden
        directoryService.setPasswordHidden( directoryServiceBean.isDsPasswordHidden() );

        // SyncPeriodMillis
        directoryService.setSyncPeriodMillis( directoryServiceBean.getDsSyncPeriodMillis() );

        // testEntries
        String entryFilePath = directoryServiceBean.getDsTestEntries();

        if ( entryFilePath != null )
        {
            directoryService.setTestEntries( readTestEntries( entryFilePath ) );
        }

        // Enabled
        if ( !directoryServiceBean.isEnabled() )
        {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
            // TODO
        }

        return directoryService;
    }


    private static Journal createJournal( JournalBean journalBean )
    {
        if ( ( journalBean == null ) || journalBean.isDisabled() )
        {
            return null;
        }

        Journal journal = new DefaultJournal();

        journal.setRotation( journalBean.getJournalRotation() );
        journal.setEnabled( journalBean.isEnabled() );

        JournalStore store = new DefaultJournalStore();

        store.setFileName( journalBean.getJournalFileName() );
        store.setWorkingDirectory( journalBean.getJournalWorkingDir() );

        journal.setJournalStore( store );

        return journal;
    }


    /**
     * Read the configuration for the ChangeLog system
     * 
     * @param changelogBean The Bean containing the ChangeLog configuration
     * @return The instantiated ChangeLog element
     */
    public static ChangeLog createChangeLog( ChangeLogBean changeLogBean )
    {
        if ( ( changeLogBean == null ) || changeLogBean.isDisabled() )
        {
            return null;
        }

        ChangeLog changeLog = new DefaultChangeLog();

        changeLog.setEnabled( changeLogBean.isEnabled() );
        changeLog.setExposed( changeLogBean.isChangeLogExposed() );

        return changeLog;
    }


    /**
     * Load the Test entries
     * 
     * @param entryFilePath The place on disk where the test entries are stored
     * @return A list of LdifEntry elements
     * @throws ConfigurationException If we weren't able to read the entries
     */
    private static List<LdifEntry> readTestEntries( String entryFilePath ) throws ConfigurationException
    {
        List<LdifEntry> entries = new ArrayList<LdifEntry>();

        File file = new File( entryFilePath );

        if ( !file.exists() )
        {
            LOG.warn( "LDIF test entry file path doesn't exist {}", entryFilePath );
        }
        else
        {
            LOG.debug( "parsing the LDIF file(s) present at the path {}", entryFilePath );

            try
            {
                loadEntries( file, entries );
            }
            catch ( LdapLdifException e )
            {
                String message = "Error while parsing a LdifEntry : " + e.getMessage();
                LOG.error( message );
                throw new ConfigurationException( message );
            }
            catch ( IOException e )
            {
                String message = "cannot read the Ldif entries from the " + entryFilePath + " location";
                LOG.error( message );
                throw new ConfigurationException( message );
            }
        }

        return entries;
    }


    /**
     * Load the entries from a Ldif file recursively
     * @throws LdapLdifException
     * @throws IOException
     */
    private static void loadEntries( File ldifFile, List<LdifEntry> entries ) throws LdapLdifException, IOException
    {
        if ( ldifFile.isDirectory() )
        {
            File[] files = ldifFile.listFiles( ldifFilter );

            for ( File f : files )
            {
                loadEntries( f, entries );
            }
        }
        else
        {
            LdifReader reader = new LdifReader();

            try
            {
                entries.addAll( reader.parseLdifFile( ldifFile.getAbsolutePath() ) );
            }
            finally
            {
                reader.close();
            }
        }
    }
}
