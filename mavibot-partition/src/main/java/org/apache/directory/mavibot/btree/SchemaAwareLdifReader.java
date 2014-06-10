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
import java.io.IOException;
import java.io.StringReader;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdapLdifException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO SchemaAwareLdifReader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaAwareLdifReader extends LdifReader
{
    private SchemaManager schemaManager;

    private static final Logger LOG = LoggerFactory.getLogger( SchemaAwareLdifReader.class );
    
    public SchemaAwareLdifReader( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;
    }

    @Override
    protected LdifEntry createLdifEntry()
    {
        Entry entry = new DefaultEntry( schemaManager );
        return new LdifEntry( entry );
    }
    
    
    /**
     * 
     * parse a single entry from the given LDIF text.
     *
     * @param ldif the LDIF string
     * @return an LdifEntry
     * @throws LdapLdifException
     */
    public LdifEntry parseLdifEntry( String ldif ) throws LdapLdifException
    {
        LOG.debug( "Starts parsing ldif buffer" );

        if ( Strings.isEmpty( ldif ) )
        {
            return null;
        }

        BufferedReader reader = new BufferedReader( new StringReader( ldif ) );

        try
        {
            this.reader = reader;

            // First get the version - if any -
            version = parseVersion();
            return parseEntry();
        }
        catch ( LdapLdifException ne )
        {
            LOG.error( I18n.err( I18n.ERR_12069, ne.getLocalizedMessage() ) );
            throw new LdapLdifException( I18n.err( I18n.ERR_12070 ), ne );
        }
        catch ( LdapException le )
        {
            throw new LdapLdifException( le.getMessage(), le );
        }
        finally
        {
            // Close the reader
            try
            {
                reader.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapLdifException( I18n.err( I18n.ERR_12024_CANNOT_CLOSE_FILE ), ioe );
            }

        }
    }

}
