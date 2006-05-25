/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.protocol.shared.store;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Support for commands to load an LDIF file into a DirContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifFileLoader
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( LdifFileLoader.class );

    /** a handle on the top initial context: get new context from this */
    protected DirContext ctx;
    /** the LDIF file or directory containing LDIFs to load */
    protected File ldif;
    /** the filters to use while loading entries into the server */
    protected final List filters;
    /** the class loader to use if we cannot file the file as a path */
    protected final ClassLoader loader;
    /** the total count of entries loaded */
    private int count;


    /**
     * Creates the LDIF file loader command.
     *
     * @param ctx the context to load the entries into.
     * @param ldif the file of LDIF entries to load.
     */
    public LdifFileLoader(DirContext ctx, String ldif)
    {
        this( ctx, new File( ldif ), null );
    }


    public LdifFileLoader(DirContext ctx, File ldif, List filters)
    {
        this( ctx, ldif, filters, null );
    }


    public LdifFileLoader(DirContext ctx, File ldif, List filters, ClassLoader loader)
    {
        this.ctx = ctx;
        this.ldif = ldif;
        this.loader = loader;

        if ( filters == null )
        {
            this.filters = Collections.EMPTY_LIST;
        }
        else
        {
            this.filters = Collections.unmodifiableList( filters );
        }
    }


    /**
     * Applies filters making sure failures in one filter do not effect another.
     *
     * @param dn the DN of the entry
     * @param entry the attributes of the entry
     * @return true if all filters passed the entry, false otherwise
     */
    private boolean applyFilters( String dn, Attributes entry )
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
                accept &= ( ( LdifLoadFilter ) filters.get( ii ) ).filter( ldif, dn, entry, ctx );
            }
            catch ( NamingException e )
            {
                log.warn( "filter " + filters.get( ii ) + " was bypassed due to failures", e );
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
     */
    public int execute()
    {
        Name rdn;
        InputStream in = null;

        try
        {
            in = getLdifStream();
            LdifReader ldifIterator = new LdifReader( new BufferedReader( new InputStreamReader( in ) ) );

            while ( ldifIterator.hasNext() )
            {
                Entry entry = ( Entry ) ldifIterator.next();
                
                String dn = entry.getDn();
                
                if ( entry.isEntry() == false)
                {
                	// If the entry is a modification, just skip it
                	continue;
                }

                Attributes attributes = entry.getAttributes();
                boolean filterAccepted = applyFilters( dn, attributes );

                if ( !filterAccepted )
                {
                    continue;
                }

                rdn = getRelativeName( ctx, dn );

                try
                {
                    ctx.lookup( rdn );
                    log.info( "Found {}, will not create.", rdn );
                }
                catch ( Exception e )
                {
                    ctx.createSubcontext( rdn, attributes );
                    count++;
                    log.info( "Created {}.", rdn );
                }
            }
        }
        catch ( FileNotFoundException fnfe )
        {
            log.error( "LDIF file does not exist." );
        }
        catch ( Exception ioe )
        {
            log.error( "Failed to import LDIF into backing store.", ioe );
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( Exception e )
                {
                    log.error( "failed to close stream", e );
                }
            }
        }

        return count;
    }


    private Name getRelativeName( DirContext ctx, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn;

        try
        {
            Name ctxRoot = new CompoundName( ctx.getNameInNamespace(), props );
            searchBaseDn = new CompoundName( baseDn, props );

            if ( !searchBaseDn.startsWith( ctxRoot ) )
            {
                throw new NamingException( "Invalid search base " + baseDn );
            }

            for ( int ii = 0; ii < ctxRoot.size(); ii++ )
            {
                searchBaseDn.remove( 0 );
            }
        }
        catch ( NamingException e )
        {
            throw new NamingException( "Failed to initialize search base " + baseDn );
        }

        return searchBaseDn;
    }


    /**
     * Tries to find an LDIF file either on the file system or packaged within a jar.
     *
     * @return the input stream to the ldif file.
     * @throws FileNotFoundException if the file cannot be found.
     */
    private InputStream getLdifStream() throws FileNotFoundException
    {
        InputStream in;

        if ( ldif.exists() )
        {
            in = new FileInputStream( ldif );
        }
        else
        {
            if ( loader != null && ( in = loader.getResourceAsStream( ldif.getName() ) ) != null )
            {
                return in;
            }

            // if file not on system see if something is bundled with the jar ...
            in = getClass().getResourceAsStream( ldif.getName() );
            if ( in != null )
            {
                return in;
            }

            in = ClassLoader.getSystemResourceAsStream( ldif.getName() );
            if ( in != null )
            {
                return in;
            }

            throw new FileNotFoundException( "LDIF file does not exist." );
        }

        return in;
    }
}
