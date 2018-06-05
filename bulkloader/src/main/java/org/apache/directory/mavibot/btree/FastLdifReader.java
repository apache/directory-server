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
package org.apache.directory.mavibot.btree;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdapLdifException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DnFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An LDIF reader that gathers an entry's DN, length and offset.
 * This is a special parser implemented for use in bulk loader tool.
 * 
 * This class is not suitable for general purpose use.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
/** no qualifier */ class FastLdifReader extends LdifReader
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( FastLdifReader.class );

    /** the pre-fetched DnTuple */
    private DnTuple firstFetchedTuple;

    /** the next tuple */
    private DnTuple nextTuple;
    
    /** The DnFactory */
    private DnFactory dnFactory;
    

    /**
     * 
     * Creates a new instance of FastLdifReader.
     *
     * @param file the LDIF file
     * @throws LdapException 
     * @throws FileNotFoundException 
     */
    public FastLdifReader( File file, DnFactory dnFactory ) throws LdapException, FileNotFoundException
    {
        super();
        reader = new PositionBufferedReader( new FileReader( file ) );
        this.dnFactory = dnFactory;
        validateDn = false;
        
        init();
    }


    @Override
    public void init() throws LdapException
    {
        lines = new ArrayList<String>();
        position = 0;
        version = DEFAULT_VERSION;
        containsChanges = false;
        containsEntries = false;
        
        // No need to validate the Dn while we are parsing it from the LDIF file
        validateDn = false;

        // First get the version - if any -
        fastParseVersion();
        firstFetchedTuple = parseDnAlone();
    }
    
    
    /**
     * Parse the version from the ldif input.
     *
     * @return A number representing the version (default to 1)
     * @throws LdapLdifException If the version is incorrect or if the input is incorrect
     */
    private void fastParseVersion() throws LdapLdifException
    {
        // First, read a list of lines
        fastReadLines();

        if ( lines.size() == 0 )
        {
            LOG.warn( I18n.msg( I18n.MSG_13414_LDIF_FILE_EMPTY ) );
            return;
        }

        // get the first line
        String line = lines.get( 0 );

        // <ldif-file> ::= "version:" <fill> <number>
        if ( line.startsWith( "version:" ) )
        {
            // Ok, skip the line
            position += "version:".length();

            // We have found the version, just discard the line from the list
            lines.remove( 0 );
        }

        return;
    }


    @Override
    public boolean hasNext()
    {
        return ( firstFetchedTuple != null );
    }


    public DnTuple getDnTuple()
    {
        return nextTuple;
    }


    @Override
    public LdifEntry next()
    {
        try
        {
            LOG.debug( I18n.msg( I18n.MSG_13411_NEXT_CALLED ) );

            nextTuple = firstFetchedTuple;
            
            // Read all the lines for one single entry
            fastReadLines();

            try
            {
                firstFetchedTuple = parseDnAlone();
            }
            catch ( LdapLdifException ne )
            {
                error = ne;
                throw new NoSuchElementException( ne.getMessage() );
            }
            catch ( LdapException le )
            {
                throw new NoSuchElementException( le.getMessage() );
            }

            //System.out.println( nextTuple );
            LOG.debug( "next(): -- saving DnTuple {}\n", nextTuple );

            return null;
        }
        catch ( LdapLdifException ne )
        {
            ne.printStackTrace();
            LOG.error( I18n.err( I18n.ERR_13430_PREMATURE_LDIF_ITERATOR_TERMINATION ) );
            error = ne;
            return null;
        }
    }


    /**
     * Get teh DN from an entry, ignoring the remaining data
     */
    private DnTuple parseDnAlone() throws LdapException
    {
        if ( ( lines == null ) || ( lines.size() == 0 ) )
        {
            LOG.debug( I18n.msg( I18n.MSG_13408_END_OF_LDIF ) );
            
            return null;
        }

        // The entry must start with a dn: or a dn::
        String line = lines.get( 0 );

        lineNumber -= ( lines.size() - 1 );

        String name = parseDn( line );

        Dn dn = dnFactory.create( name );

        DnTuple tuple = new DnTuple( dn, entryOffset, (int)(offset - entryOffset) );

        return tuple;
    }

    protected String getLine() throws IOException
    {
        return ( ( PositionBufferedReader ) reader ).readLine();
    }


    /**
     * Reads an entry in a ldif buffer, and returns the resulting lines, without
     * comments, and unfolded.
     *
     * The lines represent *one* entry.
     *
     * @throws LdapLdifException If something went wrong
     */
    private void fastReadLines() throws LdapLdifException
    {
        String line;
        boolean insideComment = true;
        boolean isFirstLine = true;

        lines.clear();
        entryOffset = offset;

        StringBuffer sb = new StringBuffer();

        try
        {
            while ( ( line = getLine() ) != null )
            {
                lineNumber++;

                if ( line.length() == 0 )
                {
                    if ( isFirstLine )
                    {
                        continue;
                    }
                    else
                    {
                        // The line is empty, we have read an entry
                        insideComment = false;
                        offset = ((PositionBufferedReader)reader).getFilePos();

                        break;
                    }
                }

                // We will read the first line which is not a comment
                switch ( line.charAt( 0 ) )
                {
                    case '#':
                        insideComment = true;
                        break;

                    case ' ':
                        isFirstLine = false;

                        if ( insideComment )
                        {
                            continue;
                        }
                        else if ( sb.length() == 0 )
                        {
                            LOG.error( I18n.err( I18n.ERR_13424_EMPTY_CONTINUATION_LINE ) );
                            throw new LdapLdifException( I18n.err( I18n.ERR_13462_LDIF_PARSING_ERROR ) );
                        }
                        else
                        {
                            sb.append( line.substring( 1 ) );
                        }

                        insideComment = false;
                        break;

                    default:
                        isFirstLine = false;

                        // We have found a new entry
                        // First, stores the previous one if any.
                        if ( sb.length() != 0 )
                        {
                            lines.add( sb.toString() );
                        }

                        sb = new StringBuffer( line );
                        insideComment = false;
                        break;
                }

                offset = ((PositionBufferedReader)reader).getFilePos();
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapLdifException( I18n.err( I18n.ERR_13463_ERROR_WHILE_READING_LDIF_LINE ), ioe );
        }

        // Stores the current line if necessary.
        if ( sb.length() != 0 )
        {
            lines.add( sb.toString() );
        }
    }
}
