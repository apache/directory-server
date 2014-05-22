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


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.ChangeType;
import org.apache.directory.api.ldap.model.ldif.LdapLdifException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
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

    private static final Logger LOG = LoggerFactory.getLogger( FastLdifReader.class );

    /** the pre-fetched DnTuple */
    private DnTuple firstFetchedTuple;

    /** the next tuple */
    private DnTuple nextTuple;

    /**
     * 
     * Creates a new instance of FastLdifReader.
     *
     * @param file the LDIF file
     * @throws LdapLdifException
     */
    public FastLdifReader( File file ) throws LdapLdifException
    {
        super( file );
    }


    @Override
    protected void init() throws LdapException
    {
        lines = new ArrayList<String>();
        position = 0;
        version = DEFAULT_VERSION;
        containsChanges = false;
        containsEntries = false;

        // First get the version - if any -
        version = parseVersion();
        firstFetchedTuple = parseDnAlone();
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
            LOG.debug( "next(): -- called" );

            nextTuple = firstFetchedTuple;
            
            readLines();

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

            LOG.debug( "next(): -- saving DnTuple {}\n", nextTuple );

            return null;
        }
        catch ( LdapLdifException ne )
        {
            LOG.error( I18n.err( I18n.ERR_12071 ) );
            error = ne;
            return null;
        }
    }


    private DnTuple parseDnAlone() throws LdapException
    {
        if ( ( lines == null ) || ( lines.size() == 0 ) )
        {
            LOG.debug( "The entry is empty : end of ldif file" );
            return null;
        }

        // The entry must start with a dn: or a dn::
        String line = lines.get( 0 );

        lineNumber -= ( lines.size() - 1 );

        String name = parseDn( line );

        Dn dn = new Dn( name );

        DnTuple tuple = new DnTuple( dn, entryOffset, entryLen );

        // Ok, we have found a Dn
        //LdifEntry entry = new LdifEntry( entryLen, entryOffset );

        //entry.setDn( dn );

        // We remove this dn from the lines
        lines.remove( 0 );

        // Now, let's iterate through the other lines
        Iterator<String> iter = lines.iterator();

        // This flag is used to distinguish between an entry and a change
        int type = LDIF_ENTRY;

        // The following boolean is used to check that a control is *not*
        // found elswhere than just after the dn
        boolean controlSeen = false;

        // We use this boolean to check that we do not have AttributeValues
        // after a change operation
        boolean changeTypeSeen = false;

        ChangeType operation = ChangeType.Add;
        String lowerLine;

        while ( iter.hasNext() )
        {
            lineNumber++;

            // Each line could start either with an OID, an attribute type, with
            // "control:" or with "changetype:"
            line = iter.next();
            lowerLine = Strings.toLowerCase( line );

            // We have three cases :
            // 1) The first line after the Dn is a "control:"
            // 2) The first line after the Dn is a "changeType:"
            // 3) The first line after the Dn is anything else
            if ( lowerLine.startsWith( "control:" ) )
            {
                if ( containsEntries )
                {
                    LOG.error( I18n.err( I18n.ERR_12004_CHANGE_NOT_ALLOWED ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12005_NO_CHANGE ) );
                }

                containsChanges = true;

                if ( controlSeen )
                {
                    LOG.error( I18n.err( I18n.ERR_12050 ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12051 ) );
                }

                // Parse the control
                // SKIP it
            }
            else if ( lowerLine.startsWith( "changetype:" ) )
            {
                if ( containsEntries )
                {
                    LOG.error( I18n.err( I18n.ERR_12004_CHANGE_NOT_ALLOWED ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12005_NO_CHANGE ) );
                }

                containsChanges = true;

                if ( changeTypeSeen )
                {
                    LOG.error( I18n.err( I18n.ERR_12052 ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12053 ) );
                }

                // A change request
                type = CHANGE;
                controlSeen = true;

                operation = parseChangeType( line );

                if ( operation != ChangeType.Add )
                {
                    throw new IllegalArgumentException( "ChangeType " + operation + " is not allowed during bulk load" );
                }
                // Parse the change operation in a separate function
                // SKIP it
                while ( iter.hasNext() )
                {
                    iter.next();
                }

                changeTypeSeen = true;
            }
            else if ( line.indexOf( ':' ) > 0 )
            {
                if ( containsChanges )
                {
                    LOG.error( I18n.err( I18n.ERR_12004_CHANGE_NOT_ALLOWED ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12005_NO_CHANGE ) );
                }

                containsEntries = true;

                if ( controlSeen || changeTypeSeen )
                {
                    LOG.error( I18n.err( I18n.ERR_12054 ) );
                    throw new LdapLdifException( I18n.err( I18n.ERR_12055 ) );
                }

                // SKIP it
                //parseAttributeValue( entry, line, lowerLine );
                type = LDIF_ENTRY;
            }
            else
            {
                // Invalid attribute Value
                LOG.error( I18n.err( I18n.ERR_12056 ) );
                throw new LdapLdifException( I18n.err( I18n.ERR_12057_BAD_ATTRIBUTE ) );
            }
        }

        if ( type == LDIF_ENTRY )
        {
            LOG.debug( "Read an entry : {}", tuple );
        }
        else if ( type == CHANGE )
        {
            //entry.setChangeType( operation );
            LOG.debug( "Read a modification : {}", tuple );
        }
        else
        {
            LOG.error( I18n.err( I18n.ERR_12058_UNKNOWN_ENTRY_TYPE ) );
            throw new LdapLdifException( I18n.err( I18n.ERR_12059_UNKNOWN_ENTRY ) );
        }

        return tuple;
    }

}
