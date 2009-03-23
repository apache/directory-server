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


import org.apache.commons.lang.StringUtils;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.impl.DefaultDirectoryService;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Apache Directory Server top level.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
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
    private final LdapService ldapService;
    
    /** The LDAPS server protocol handler */
    private final LdapService ldapsService;
    
    /** The directory service */
    private final DirectoryService directoryService;


    /**
     * Creates a new instance of the ApacheDS server
     *  
     * @param directoryService 
     * @param ldapService
     * @param ldapsService
     */
    public ApacheDS( DirectoryService directoryService, LdapService ldapService, LdapService ldapsService )
    {
        LOG.info( "Starting the Apache Directory Server" );

        if ( directoryService == null )
        {
            this.directoryService = new DefaultDirectoryService();
        }
        else
        {        
            this.directoryService = directoryService;
        }
        
        this.ldapService = ldapService;
        this.ldapsService = ldapsService;
        IoBuffer.setAllocator( new SimpleBufferAllocator() );
        IoBuffer.setUseDirectBuffer( false );
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
        
        // Start the directory service if not started yet
        if ( ! directoryService.isStarted() )
        {
            LOG.debug( "1. Starting the DirectoryService" );
            directoryService.startup();
        }

        // Load the LDIF files - if any - into the server
        loadLdifs();

        // Start the LDAP server
        if ( ldapService != null && ! ldapService.isStarted() )
        {
            LOG.debug( "3. Starting the LDAP server" );
            ldapService.start();
        }

        // Start the LDAPS  server
        if ( ldapsService != null && ! ldapsService.isStarted() )
        {
            LOG.debug(  "4. Starting the LDAPS server" );
            ldapsService.start();
        }
        
        LOG.debug( "Server successfully started" );
    }


    public boolean isStarted()
    {
        if ( ldapService != null || ldapsService != null )
        {
             return ( ldapService != null && ldapService.isStarted() )
                     || ( ldapsService != null && ldapsService.isStarted() );
        }
        
        return directoryService.isStarted();
    }
    

    public void shutdown() throws Exception
    {
        if ( ldapService != null && ldapService.isStarted() )
        {
            ldapService.stop();
        }

        if ( ldapsService != null && ldapsService.isStarted() )
        {
            ldapsService.stop();
        }

        directoryService.shutdown();
    }


    public LdapService getLdapService()
    {
        return ldapService;
    }


    public LdapService getLdapsService()
    {
        return ldapsService;
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
     * Get the directory where 
     * @return
     */
    public File getLdifDirectory()
    {
        return ldifDirectory;
    }


    public void setAllowAnonymousAccess( boolean allowAnonymousAccess )
    {
        LOG.info( "Set the allowAnonymousAccess flag to {}", allowAnonymousAccess );
        
        directoryService.setAllowAnonymousAccess( allowAnonymousAccess );
        
        if ( ldapService != null )
        {
            ldapService.setAllowAnonymousAccess( allowAnonymousAccess );
        }
        
        if ( ldapsService != null )
        {
            ldapsService.setAllowAnonymousAccess( allowAnonymousAccess );
        }
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
        LdapDN dn = new LdapDN( ServerDNConstants.LDIF_FILES_DN );
        ServerEntry entry = null;
        
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
            entry = directoryService.newEntry( new LdapDN( ServerDNConstants.LDIF_FILES_DN ) );
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
    private LdapDN buildProtectedFileEntryDn( File ldif ) throws Exception
    {
        String fileSep = File.separatorChar == '\\' ? 
                ApacheSchemaConstants.WINDOWS_FILE_AT : 
                ApacheSchemaConstants.UNIX_FILE_AT;

        return  new LdapDN( fileSep + 
                "=" + 
                StringTools.dumpHexPairs( StringTools.getBytesUtf8( getCanonical( ldif ) ) ) + 
                "," + 
                ServerDNConstants.LDIF_FILES_DN ); 
    }

    
    private void addFileEntry( File ldif ) throws Exception
    {
        String rdnAttr = File.separatorChar == '\\' ? 
            ApacheSchemaConstants.WINDOWS_FILE_AT : 
            ApacheSchemaConstants.UNIX_FILE_AT;
        String oc = File.separatorChar == '\\' ? ApacheSchemaConstants.WINDOWS_FILE_OC : ApacheSchemaConstants.UNIX_FILE_OC;

        ServerEntry entry = directoryService.newEntry( buildProtectedFileEntryDn( ldif ) );
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
            LOG.error( "could not get canonical path", e );
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
    private void loadLdif( File ldifFile ) throws Exception
    {
        ClonedServerEntry fileEntry = null;
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
     * Load the ldif files if there are some
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


        LdapDN dn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN );
        
        // Must normalize the dn or - IllegalStateException!
        AttributeTypeRegistry reg = directoryService.getRegistries().getAttributeTypeRegistry();
        dn.normalize( reg.getNormalizerMapping() );
        
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
                LOG.error( "Cannot load the ldif file '{}', error : ",
                    ldifDirectory.getAbsolutePath(), 
                    ne.getMessage() );
                throw ne;
            }
        }
        else
        {
            // get all the ldif files within the directory (should be sorted alphabetically)
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
                    LOG.error( "Cannot load the ldif file '{}', error : {}", 
                        ldifFile.getAbsolutePath(), 
                        ne.getMessage() );
                    throw ne;
                }
            }
        }
    }
}
