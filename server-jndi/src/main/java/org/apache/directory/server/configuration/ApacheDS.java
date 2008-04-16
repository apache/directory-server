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
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.server.protocol.shared.store.LdifLoadFilter;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

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
    private final LdapServer ldapServer;
    
    /** The LDAPS server protocol handler */
    private final LdapServer ldapsServer;
    
    /** The directory service */
    private final DirectoryService directoryService;


    /**
     * Creates a new instance of the ApacheDS server
     *  
     * @param directoryService 
     * @param ldapServer
     * @param ldapsServer
     */
    public ApacheDS( DirectoryService directoryService, LdapServer ldapServer, LdapServer ldapsServer )
    {
        LOG.info(  "Starting the Apache Directory Server" );

        if ( directoryService == null )
        {
            this.directoryService = new DefaultDirectoryService();
        }
		else
		{        
        	this.directoryService = directoryService;
        }
        
        this.ldapServer = ldapServer;
        this.ldapsServer = ldapsServer;
        ByteBuffer.setAllocator( new SimpleByteBufferAllocator() );
        ByteBuffer.setUseDirectBuffers( false );
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
    public void startup() throws NamingException, IOException
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
        if ( ldapServer != null && ! ldapServer.isStarted() )
        {
            LOG.debug( "3. Starting the LDAP server" );
            ldapServer.start();
        }

        // Start the LDAPS  server
        if ( ldapsServer != null && ! ldapsServer.isStarted() )
        {
            LOG.debug(  "4. Starting the LDAPS server" );
            ldapsServer.start();
        }
        
        LOG.debug( "Server successfully started" );
    }


    public boolean isStarted()
    {
        if ( ldapServer != null || ldapsServer != null )
        {
             return ( ldapServer != null && ldapServer.isStarted() )
                     || ( ldapsServer != null && ldapsServer.isStarted() );
        }
        
        return directoryService.isStarted();
    }
    

    public void shutdown() throws NamingException
    {
        if ( ldapServer != null && ldapServer.isStarted() )
        {
            ldapServer.stop();
        }

        if ( ldapsServer != null && ldapsServer.isStarted() )
        {
            ldapsServer.stop();
        }

        directoryService.shutdown();
    }


    public LdapServer getLdapServer()
    {
        return ldapServer;
    }


    public LdapServer getLdapsServer()
    {
        return ldapsServer;
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
        
        if ( ldapServer != null )
        {
            ldapServer.setAllowAnonymousAccess( allowAnonymousAccess );
        }
        
        if ( ldapsServer != null )
        {
            ldapsServer.setAllowAnonymousAccess( allowAnonymousAccess );
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
    private void ensureLdifFileBase( DirContext root )
    {
        Attributes entry = new AttributesImpl( SchemaConstants.OU_AT, "loadedLdifFiles", true );
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );

        try
        {
            root.createSubcontext( ServerDNConstants.LDIF_FILES_DN, entry );
            LOG.info( "Creating " + ServerDNConstants.LDIF_FILES_DN );
        }
        catch ( NamingException e )
        {
            LOG.info( ServerDNConstants.LDIF_FILES_DN + " exists" );
        }
    }


    /**
     * Create a string containing a hex dump of the loaded ldif file name.
     * 
     * It is associated with the attributeType wrt to the underlying system.
     */
    private String buildProtectedFileEntry( File ldif )
    {
        String fileSep = File.separatorChar == '\\' ? 
                ApacheSchemaConstants.WINDOWS_FILE_AT : 
                ApacheSchemaConstants.UNIX_FILE_AT;

        return  fileSep + 
                "=" + 
                StringTools.dumpHexPairs( StringTools.getBytesUtf8( getCanonical( ldif ) ) ) +
                "," + 
                ServerDNConstants.LDIF_FILES_DN; 
    }

    
    private void addFileEntry( DirContext root, File ldif ) throws NamingException
    {
        String rdnAttr = File.separatorChar == '\\' ? 
            ApacheSchemaConstants.WINDOWS_FILE_AT : 
            ApacheSchemaConstants.UNIX_FILE_AT;
        String oc = File.separatorChar == '\\' ? ApacheSchemaConstants.WINDOWS_FILE_OC : ApacheSchemaConstants.UNIX_FILE_OC;

        Attributes entry = new AttributesImpl( rdnAttr, getCanonical( ldif ), true );
        entry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC );
        entry.get( SchemaConstants.OBJECT_CLASS_AT ).add( oc );
        root.createSubcontext( buildProtectedFileEntry( ldif ), entry );
    }


    /**
     * 
     * @param root
     * @param ldif
     * @return
     */
    private Attributes getLdifFileEntry( DirContext root, File ldif )
    {
        try
        {
            return root.getAttributes( buildProtectedFileEntry( ldif ), new String[]
                { SchemaConstants.CREATE_TIMESTAMP_AT } );
        }
        catch ( NamingException e )
        {
            return null;
        }
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
    private void loadLdif( DirContext root, File ldifFile ) throws NamingException
    {
        Attributes fileEntry = getLdifFileEntry( root, ldifFile );

        if ( fileEntry != null )
        {
            String time = ( String ) fileEntry.get( SchemaConstants.CREATE_TIMESTAMP_AT ).get();
            LOG.info( "Load of LDIF file '" + getCanonical( ldifFile )
                    + "' skipped.  It has already been loaded on " + time + "." );
        }
        else
        {
            LdifFileLoader loader = new LdifFileLoader( root, ldifFile, ldifFilters );
            int count = loader.execute();
            LOG.info( "Loaded " + count + " entries from LDIF file '" + getCanonical( ldifFile ) + "'" );
            addFileEntry( root, ldifFile );
        }
    }
    
    
    /**
     * Load the ldif files if there are some
     */
    public void loadLdifs() throws NamingException
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
        
        LdapPrincipal admin = new LdapPrincipal( dn, AuthenticationLevel.STRONG );
        
        
        DirContext root = directoryService.getJndiContext( admin );
        ensureLdifFileBase( root );

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
                loadLdif( root, ldifDirectory );
            }
            catch ( NamingException ne )
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
                    loadLdif( root, ldifFile );
                }
                catch ( NamingException ne )
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
