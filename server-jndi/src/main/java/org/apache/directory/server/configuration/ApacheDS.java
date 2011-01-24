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
package org.apache.directory.server.configuration;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Apache Directory Server top level.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApacheDS
{
    private static final Logger LOG = LoggerFactory.getLogger( ApacheDS.class.getName() );
    
    /** Default delay between two flushes to the backend */
    private static final long DEFAULT_SYNC_PERIOD_MILLIS = 20000;

    /** Wainting period between two flushes to the backend */
    private long synchPeriodMillis = DEFAULT_SYNC_PERIOD_MILLIS;

    /** Directory where are stored the LDIF files to be loaded at startup */
    private File ldifDirectory;
    
    private final List<LdifLoadFilter> ldifFilters = new ArrayList<LdifLoadFilter>();

    /** The LDAP server protocol handler */
    private final LdapServer ldapServer;
    
    /** The directory service */
    private DirectoryService directoryService;


    /**
     * Creates a new instance of the ApacheDS server
     *  
     * @param ldapServer The ldap server protocol handler
     */
    public ApacheDS( LdapServer ldapServer ) throws Exception
    {
        LOG.info( "Starting the Apache Directory Server" );

        this.ldapServer = ldapServer;
        
        directoryService = ldapServer.getDirectoryService();
        
        if ( directoryService == null )
        {
            directoryService = new DefaultDirectoryService();
        }
    }


    /**
     * Start the server :
     *  <li>initialize the DirectoryService</li>
     *  <li>start the LDAP server</li>
     *  <li>start the LDAPS server</li>
     *  
     * @throws NamingException If the server cannot be started
     * @throws IOException If an IO error occured while reading some file
     */
    public void startup() throws Exception
    {
        LOG.debug( "Starting the server" );
        
        initSchema();
        
        SchemaManager schemaManager = directoryService.getSchemaManager();
        
        if ( ! directoryService.isStarted() )
        {
            // inject the schema manager and set the partition directory
            // once the CiDIT gets done we need not do this kind of dirty hack
            Set<? extends Partition> partitions = directoryService.getPartitions();
         
            for( Partition p : partitions )
            {
                if( p instanceof BTreePartition )
                {
                    File partitionPath = new File( directoryService.getInstanceLayout().getPartitionsDirectory(), p.getId() );
                    ( ( BTreePartition ) p ).setPartitionPath( partitionPath.toURI() );
                }
                
                if( p.getSchemaManager() == null )
                {
                    LOG.info( "setting the schema manager for partition {}", p.getSuffix() );
                    p.setSchemaManager( schemaManager );
                }
            }
            
            Partition sysPartition = directoryService.getSystemPartition();
            
            if( sysPartition instanceof BTreePartition )
            {
                File partitionPath = new File( directoryService.getInstanceLayout().getPartitionsDirectory(), sysPartition.getId() );
                ( ( BTreePartition ) sysPartition ).setPartitionPath( partitionPath.toURI() );
            }

            if( sysPartition.getSchemaManager() == null )
            {
                LOG.info( "setting the schema manager for partition {}", sysPartition.getSuffix() );
                sysPartition.setSchemaManager( schemaManager );
            }
            
            // Start the directory service if not started yet
            LOG.debug( "1. Starting the DirectoryService" );
            directoryService.startup();
        }

        // Load the LDIF files - if any - into the server
        loadLdifs();

        // Start the LDAP server
        if ( ldapServer != null && ! ldapServer.isStarted() )
        {
            LOG.debug( "3. Starting the LDAP server" );
            ldapServer.start();
        }

        LOG.debug( "Server successfully started" );
    }


    public boolean isStarted()
    {
        if ( ldapServer != null )
        {
             return ( ldapServer.isStarted() );
        }
        
        return directoryService.isStarted();
    }
    

    public void shutdown() throws Exception
    {
        if ( ldapServer != null && ldapServer.isStarted() )
        {
            ldapServer.stop();
        }

        directoryService.shutdown();
    }


    public LdapServer getLdapServer()
    {
        return ldapServer;
    }


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    public long getSynchPeriodMillis()
    {
        return synchPeriodMillis;
    }


    public void setSynchPeriodMillis( long synchPeriodMillis )
    {
        LOG.info( "Set the synchPeriodMillis to {}", synchPeriodMillis );
        this.synchPeriodMillis = synchPeriodMillis;
    }

    
    /**
     * Get the directory where the LDIF files are stored
     * 
     * @return The directory where the LDIF files are stored
     */
    public File getLdifDirectory()
    {
        return ldifDirectory;
    }


    public void setLdifDirectory( File ldifDirectory )
    {
        LOG.info( "The LDIF directory file is {}", ldifDirectory.getAbsolutePath() );
        this.ldifDirectory = ldifDirectory;
    }
    
    
    // ----------------------------------------------------------------------
    // From CoreContextFactory: presently in intermediate step but these
    // methods will be moved to the appropriate protocol service eventually.
    // This is here simply to start to remove the JNDI dependency then further
    // refactoring will be needed to place these where they belong.
    // ----------------------------------------------------------------------


    /**
     * Check that the entry where are stored the loaded Ldif files is created.
     * 
     * If not, create it.
     * 
     * The files are stored in ou=loadedLdifFiles,ou=configuration,ou=system
     */
    private void ensureLdifFileBase() throws Exception
    {
        Dn dn = new Dn( ServerDNConstants.LDIF_FILES_DN );
        Entry entry = null;
        
        try
        {
            entry = directoryService.getAdminSession().lookup( dn );
        }
        catch( Exception e )
        {
            LOG.info( "Failure while looking up {}. The entry will be created now.", ServerDNConstants.LDIF_FILES_DN, e );
        }

        if ( entry == null )
        {
            entry = directoryService.newEntry( new Dn( ServerDNConstants.LDIF_FILES_DN ) );
            entry.add( SchemaConstants.OU_AT, "loadedLdifFiles" );
            entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_UNIT_OC );
    
            directoryService.getAdminSession().add( entry );
        }
    }


    /**
     * Create a string containing a hex dump of the loaded ldif file name.
     * 
     * It is associated with the attributeType wrt to the underlying system.
     */
    private Dn buildProtectedFileEntryDn( File ldif ) throws Exception
    {
        String fileSep = File.separatorChar == '\\' ? 
                ApacheSchemaConstants.WINDOWS_FILE_AT : 
                ApacheSchemaConstants.UNIX_FILE_AT;

        return  new Dn( fileSep +
                "=" + 
                Strings.dumpHexPairs( Strings.getBytesUtf8(getCanonical(ldif)) ) +
                "," + 
                ServerDNConstants.LDIF_FILES_DN ); 
    }

    
    private void addFileEntry( File ldif ) throws Exception
    {
        String rdnAttr = File.separatorChar == '\\' ? 
            ApacheSchemaConstants.WINDOWS_FILE_AT : 
            ApacheSchemaConstants.UNIX_FILE_AT;
        String oc = File.separatorChar == '\\' ? ApacheSchemaConstants.WINDOWS_FILE_OC : ApacheSchemaConstants.UNIX_FILE_OC;

        Entry entry = directoryService.newEntry( buildProtectedFileEntryDn( ldif ) );
        entry.add( rdnAttr, getCanonical( ldif ) );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, oc );
        directoryService.getAdminSession().add( entry );
    }


    private String getCanonical( File file )
    {
        String canonical;

        try
        {
            canonical = file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            LOG.error( I18n.err( I18n.ERR_179 ), e );
            return null;
        }

        return StringUtils.replace( canonical, "\\", "\\\\" );
    }


    /**
     * Load a ldif into the directory.
     *  
     * @param root The context in which we will inject the entries
     * @param ldifFile The ldif file to read
     * @throws NamingException If something went wrong while loading the entries
     */
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void loadLdif( File ldifFile ) throws Exception
    {
        Entry fileEntry = null;
        
        try
        {
            fileEntry = directoryService.getAdminSession().lookup( buildProtectedFileEntryDn( ldifFile ) );
        }
        catch( Exception e )
        {
            // if does not exist
        }
        
        if ( fileEntry != null )
        {
            String time = ((ClonedServerEntry)fileEntry).getOriginalEntry().get( SchemaConstants.CREATE_TIMESTAMP_AT ).getString();
            LOG.info( "Load of LDIF file '" + getCanonical( ldifFile )
                    + "' skipped.  It has already been loaded on " + time + "." );
        }
        else
        {
            LdifFileLoader loader = new LdifFileLoader( directoryService.getAdminSession(), ldifFile, ldifFilters );
            int count = loader.execute();
            LOG.info( "Loaded " + count + " entries from LDIF file '" + getCanonical( ldifFile ) + "'" );
            addFileEntry( ldifFile );
        }
    }
    
    
    /**
     * Load the existing LDIF files in alphabetic order
     */
    public void loadLdifs() throws Exception
    {
        // LOG and bail if property not set
        if ( ldifDirectory == null )
        {
            LOG.info( "LDIF load directory not specified.  No LDIF files will be loaded." );
            return;
        }

        // LOG and bail if LDIF directory does not exists
        if ( ! ldifDirectory.exists() )
        {
            LOG.warn( "LDIF load directory '{}' does not exist.  No LDIF files will be loaded.",
                getCanonical( ldifDirectory ) );
            return;
        }


        Dn dn = new Dn( ServerDNConstants.ADMIN_SYSTEM_DN, directoryService.getSchemaManager() );
        
        ensureLdifFileBase();

        // if ldif directory is a file try to load it
        if ( ldifDirectory.isFile() )
        {
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "LDIF load directory '{}' is a file. Will attempt to load as LDIF.",
                    getCanonical( ldifDirectory ) );
            }

            try
            {
                loadLdif( ldifDirectory );
            }
            catch ( Exception ne )
            {
                // If the file can't be read, log the error, and stop
                // loading LDIFs.
                LOG.error( I18n.err( I18n.ERR_180, ldifDirectory.getAbsolutePath(), ne.getLocalizedMessage() ) );
                throw ne;
            }
        }
        else
        {
            // get all the ldif files within the directory
            File[] ldifFiles = ldifDirectory.listFiles( new FileFilter()
            {
                public boolean accept( File pathname )
                {
                    boolean isLdif = pathname.getName().toLowerCase().endsWith( ".ldif" );
                    return pathname.isFile() && pathname.canRead() && isLdif;
                }
            } );
    
            // LOG and bail if we could not find any LDIF files
            if ( ( ldifFiles == null ) || ( ldifFiles.length == 0 ) )
            {
                LOG.warn( "LDIF load directory '{}' does not contain any LDIF files. No LDIF files will be loaded.", 
                    getCanonical( ldifDirectory ) );
                return;
            }

            // Sort ldifFiles in alphabetic order
            Arrays.sort( ldifFiles, new Comparator<File>()
            {
                public int compare( File f1, File f2)
                {
                    return f1.getName().compareTo( f2 .getName() );
                }
            });
            
            // load all the ldif files and load each one that is loaded
            for ( File ldifFile : ldifFiles )
            {
                try
                {
                    LOG.info(  "Loading LDIF file '{}'", ldifFile.getName() );
                    loadLdif( ldifFile );
                }
                catch ( Exception ne )
                {
                    // If the file can't be read, log the error, and stop
                    // loading LDIFs.
                    LOG.error( I18n.err( I18n.ERR_180, ldifFile.getAbsolutePath(), ne.getLocalizedMessage() ) );
                    throw ne;
                }
            }
        }
    }
    
    
    /**
     * initialize the schema partition by loading the schema LDIF files
     * 
     * @throws Exception in case of any problems while extracting and writing the schema files
     */
    private void initSchema() throws Exception
    {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = directoryService.getInstanceLayout().getPartitionsDirectory().getPath();
        ldifPartition.setPartitionPath( new File( workingDirectory, "schema" ).toURI() );

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        
        if( schemaRepository.exists() )
        {
            LOG.info( "schema partition already exists, skipping schema extraction" );
        }
        else
        {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
            extractor.extractOrCopy();
        }

        schemaPartition.setWrappedPartition( ldifPartition );

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        directoryService.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();
        
        schemaPartition.setSchemaManager( schemaManager );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( I18n.err( I18n.ERR_317, Exceptions.printErrors(errors) ) );
        }
    }

}
