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
package org.apache.directory.server.protocol.shared.store;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Support for commands to load an LDIF file into a DirContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifFileLoader
{
    /**
     * the log for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger( LdifFileLoader.class );

    /**
     * a handle on the top core session
     */
    protected CoreSession coreSession;
    /**
     * the LDIF file or directory containing LDIFs to load
     */
    protected File ldif;
    /**
     * the filters to use while loading entries into the server
     */
    protected final List<LdifLoadFilter> filters;
    /**
     * the class loader to use if we cannot file the file as a path
     */
    protected final ClassLoader loader;
    /**
     * the total count of entries loaded
     */
    private int count;


    /**
     * Creates a new instance of LdifFileLoader.
     *
     * @param coreSession  the context to load the entries into.
     * @param ldif the file of LDIF entries to load.
     */
    public LdifFileLoader( CoreSession coreSession, String ldif )
    {
        this( coreSession, new File( ldif ), null );
    }


    /**
     * Creates a new instance of LdifFileLoader.
     *
     * @param coreSession The CoreSession instance
     * @param ldif The ldif file to load
     * @param filters The search filter to use
     */
    public LdifFileLoader( CoreSession coreSession, File ldif, List<? extends LdifLoadFilter> filters )
    {
        this( coreSession, ldif, filters, null );
    }


    /**
     * Creates a new instance of LdifFileLoader.
     *
     * @param coreSession The CoreSession instance
     * @param ldif The ldif file to load
     * @param filters The search filter to use
     * @param loader The LdifLoader to use
     */
    public LdifFileLoader( CoreSession coreSession, File ldif, List<? extends LdifLoadFilter> filters,
        ClassLoader loader )
    {
        this.coreSession = coreSession;
        this.ldif = ldif;
        this.loader = loader;

        if ( filters == null )
        {
            this.filters = Collections.emptyList();
        }
        else
        {
            this.filters = Collections.unmodifiableList( filters );
        }
    }


    /**
     * Applies filters making sure failures in one filter do not effect another.
     *
     * @param dn the Dn of the entry
     * @param entry the attributes of the entry
     * @return true if all filters passed the entry, false otherwise
     */
    private boolean applyFilters( Dn dn, Entry entry )
    {
        boolean accept = true;
        final int limit = filters.size();

        if ( limit == 0 )
        {
            return true;
        } // don't waste time with loop

        for ( int ii = 0; ii < limit; ii++ )
        {
            try
            {
                accept &= ( filters.get( ii ) ).filter( ldif, dn, entry, coreSession );
            }
            catch ( LdapException e )
            {
                LOG.warn( "filter " + filters.get( ii ) + " was bypassed due to failures", e );
            }

            // early bypass if entry is rejected
            if ( !accept )
            {
                return false;
            }
        }
        return true;
    }


    /**
     * Opens the LDIF file and loads the entries into the context.
     *
     * @return The count of entries created.
     */
    public int execute()
    {
        try ( InputStream in = getLdifStream() )
        {
            for ( LdifEntry ldifEntry : new LdifReader( in ) )
            {
                Dn dn = ldifEntry.getDn();

                if ( ldifEntry.isEntry() )
                {
                    Entry entry = ldifEntry.getEntry();
                    boolean filterAccepted = applyFilters( dn, entry );

                    if ( !filterAccepted )
                    {
                        continue;
                    }

                    try
                    {
                        coreSession.lookup( dn );
                        LOG.info( "Found {}, will not create.", dn );
                    }
                    catch ( Exception e )
                    {
                        try
                        {
                            coreSession.add(
                                new DefaultEntry(
                                    coreSession.getDirectoryService().getSchemaManager(), entry ) );
                            count++;
                            LOG.info( "Created {}.", dn );
                        }
                        catch ( LdapException e1 )
                        {
                            LOG.info( "Could not create entry " + entry, e1 );
                        }
                    }
                }
                else
                {
                    //modify
                    List<Modification> items = ldifEntry.getModifications();

                    try
                    {
                        coreSession.modify( dn, items );
                        LOG.info( "Modified: {} with modificationItems: {}", dn, items );
                    }
                    catch ( LdapException e )
                    {
                        LOG.info( "Could not modify: {} with modificationItems: {}", dn, items, e );
                    }
                }
            }
        }
        catch ( FileNotFoundException fnfe )
        {
            LOG.error( I18n.err( I18n.ERR_40001_LDIF_FILE_DOES_NOT_EXIST ) );
        }
        catch ( Exception ioe )
        {
            LOG.error( I18n.err( I18n.ERR_40002_FAILED_TO_IMPORT_LDIF ), ioe );
        }

        return count;
    }


    /**
     * Tries to find an LDIF file either on the file system or packaged within a jar.
     *
     * @return the input stream to the ldif file.
     * @throws FileNotFoundException if the file cannot be found.
     */
    private InputStream getLdifStream() throws IOException
    {
        if ( ldif.exists() )
        {
            return Files.newInputStream( ldif.toPath() );
        }
        else
        {
            InputStream in;

            // use ldif.getPath() to resolve the relative paths
            if ( loader != null )
            {
                in = loader.getResourceAsStream( ldif.getPath() );
                if ( in != null )
                {
                    return in;
                }
            }

            // if file not on system see if something is bundled with the jar ...
            in = getClass().getResourceAsStream( ldif.getPath() );
            if ( in != null )
            {
                return in;
            }

            in = ClassLoader.getSystemResourceAsStream( ldif.getPath() );
            if ( in != null )
            {
                return in;
            }

            throw new FileNotFoundException( I18n.err( I18n.ERR_40001_LDIF_FILE_DOES_NOT_EXIST ) );
        }
    }
}
