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
package org.apache.directory.server.core.schema;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.ResourceMap;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An schema extractor that adds schema LDIF entries directly to the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaLdifToPartitionExtractor implements SchemaLdifExtractor
{
    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaLdifToPartitionExtractor.class );

    /**
     * The pattern to extract the schema from LDIF files.
     * java.util.regex.Pattern is immutable so only one instance is needed for all uses.
     */
    private static final Pattern EXTRACT_PATTERN = Pattern.compile( ".*schema" + "[/\\Q\\\\E]" + "ou=schema.*\\.ldif" );

    private final CsnFactory csnFactory = new CsnFactory( 0 );

    /** The extracted flag. */
    private boolean extracted;

    private final SchemaManager schemaManager;
    private final Partition partition;


    /**
     * Creates an extractor which adds schema LDIF entries directly to the schema partition.
     * The bootstrap schema manager must at least know the 'apachemeta' schema.
     *
     * @param schemaManager the bootstrap schema manager
     * @param partition the destination partition
     */
    public SchemaLdifToPartitionExtractor( SchemaManager schemaManager, Partition partition ) throws LdapException
    {
        this.schemaManager = schemaManager;
        this.partition = partition;

        Dn dn = new Dn( schemaManager, SchemaConstants.OU_SCHEMA );
        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( null, dn );
        if ( partition.hasEntry( hasEntryContext ) )
        {
            LOG.info( "Schema entry 'ou=schema' exists: extracted state set to true." );
            extracted = true;
        }
        else
        {
            LOG.info( "Schema entry 'ou=schema' does NOT exist: extracted state set to false." );
            extracted = false;
        }
    }


    /**
     * Gets whether or not the schema has already been added to the schema partition.
     *
     * @return true if schema has already been added to the schema partition
     */
    @Override
    public boolean isExtracted()
    {
        return extracted;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void extractOrCopy( boolean overwrite ) throws IOException
    {
        Map<String, Boolean> resources = ResourceMap.getResources( EXTRACT_PATTERN );

        // must sort the map to ensure parent entries are added before children
        resources = new TreeMap<>( resources );

        if ( !extracted || overwrite )
        {
            for ( Map.Entry<String, Boolean> entry : resources.entrySet() )
            {
                if ( entry.getValue() )
                {
                    addFromClassLoader( entry.getKey() );
                }
                else
                {
                    File resource = new File( entry.getKey() );
                    addLdifFile( resource );
                }
            }

            extracted = true;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void extractOrCopy() throws IOException
    {
        extractOrCopy( false );
    }


    /**
     * Adds an schema entry from an LDIF file.
     *
     * @param source the source file to copy
     * @throws IOException if there are IO errors or the source does not exist
     */
    private void addLdifFile( File source ) throws IOException
    {
        LOG.debug( "copyFile(): source = {}", source );

        if ( !source.getParentFile().exists() )
        {
            throw new FileNotFoundException( I18n.err( I18n.ERR_08002, source.getAbsolutePath() ) );
        }

        FileInputStream in = new FileInputStream( source );
        addFromStream( in, source.getAbsolutePath() );
    }


    /**
     * Adds an schema entry from a class loader resource.
     *
     * @param resource the LDIF schema resource
     * @throws IOException if there are IO errors
     */
    private void addFromClassLoader( String resource ) throws IOException
    {
        InputStream in = DefaultSchemaLdifExtractor.getUniqueResourceAsStream( resource,
            "LDIF file in schema repository" );
        addFromStream( in, resource );
    }


    /**
     * Adds an schema entry from the given stream to the schema partition
     *
     * @param in the input stream
     * @param source the source
     * @throws IOException signals that an I/O exception has occurred.
     */
    private void addFromStream( InputStream in, String source ) throws IOException
    {
        try
        {
            LdifReader ldifReader = new LdifReader( in );
            boolean first = true;
            LdifEntry ldifEntry = null;

            try
            {
                while ( ldifReader.hasNext() )
                {
                    if ( first )
                    {
                        ldifEntry = ldifReader.next();

                        if ( ldifEntry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
                        {
                            // No UUID, let's create one
                            UUID entryUuid = UUID.randomUUID();
                            ldifEntry.addAttribute( SchemaConstants.ENTRY_UUID_AT, entryUuid.toString() );
                        }
                        if ( ldifEntry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
                        {
                            // No CSN, let's create one
                            Csn csn = csnFactory.newInstance();
                            ldifEntry.addAttribute( SchemaConstants.ENTRY_CSN_AT, csn.toString() );
                        }

                        first = false;
                    }
                    else
                    {
                        // throw an exception : we should not have more than one entry per schema ldif file
                        String msg = I18n.err( I18n.ERR_08003, source );
                        LOG.error( msg );
                        throw new InvalidObjectException( msg );
                    }
                }
            }
            finally
            {
                ldifReader.close();
            }

            // inject the entry if any
            if ( ldifEntry != null )
            {
                Entry entry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );
                AddOperationContext addContext = new AddOperationContext( null, entry );
                partition.add( addContext );
            }
        }
        catch ( LdapException ne )
        {
            String msg = I18n.err( I18n.ERR_08004, source, ne.getLocalizedMessage() );
            LOG.error( msg );
            throw new InvalidObjectException( msg );
        }
    }

}
