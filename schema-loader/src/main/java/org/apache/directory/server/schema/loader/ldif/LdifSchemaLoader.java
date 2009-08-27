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
package org.apache.directory.server.schema.loader.ldif;


import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.DefaultSchema;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;


/**
 * Loads schema data from an LDIF files containing entries representing schema
 * objects, using the meta schema format.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class LdifSchemaLoader extends AbstractSchemaLoader
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdifSchemaLoader.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private final File baseDirectory;
    private final Map<String,Schema> schemaMap = new HashMap<String,Schema>();
    private final FilenameFilter ldifFilter = new FilenameFilter()
    {
        public boolean accept( File file, String name )
        {
            return name.endsWith( "ldif" );
        }
    };


    /**
     * Creates a new LDIF based SchemaLoader. The constructor checks to make
     * sure the supplied base directory exists and contains a schema.ldif file
     * and if not complains about it.
     *
     * @param baseDirectory the schema LDIF base directory
     * @throws Exception if the base directory does not exist or does not
     * a valid schema.ldif file
     */
    public LdifSchemaLoader( File baseDirectory ) throws Exception
    {
        this.baseDirectory = baseDirectory;

        if ( ! baseDirectory.exists() )
        {
            throw new IllegalArgumentException( "Provided baseDirectory '" +
                    baseDirectory.getAbsolutePath() + "' does not exist." );
        }

        File schemaLdif = new File( baseDirectory, "schema.ldif" );
        if ( ! schemaLdif.exists() )
        {
            throw new FileNotFoundException( "Expecting to find a schema.ldif file in provided baseDirectory " +
                    "path '" + baseDirectory.getAbsolutePath() + "' but no such file found." );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Using '{}' as the base schema load directory.", baseDirectory );
        }
        
        initializeSchemas();
    }


    /**
     * Scans for LDIF files just describing the various schema contained in
     * the schema repository.
     *
     * @throws Exception
     */
    private void initializeSchemas() throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Initializing schema" );
        }
        
        File schemaDirectory = new File( baseDirectory, "schema" );
        String[] ldifFiles = schemaDirectory.list( ldifFilter );

        for ( int ii = 0; ii < ldifFiles.length; ii++ )
        {
            LdifReader reader = new LdifReader( new File( schemaDirectory, ldifFiles[ii] ) );
            LdifEntry entry = reader.next();
            Schema schema = ldifToSchema( entry );
            schemaMap.put( schema.getSchemaName(), schema );
        }
    }


    private Schema ldifToSchema( LdifEntry entry ) throws Exception
    {
        String name = entry.get( SchemaConstants.CN_AT ).getString();

        boolean isDisabled = false;
        if ( entry.get( "m-disabled" ) != null )
        {
            isDisabled = Boolean.getBoolean( entry.get( "m-disabled" ).getString() );
        }

        String[] dependencies = null;
        if ( entry.get( "m-dependencies" ) != null )
        {
            dependencies = new String[ entry.get( "m-dependencies" ).size() ];
            int ii = 0;
            Iterator<Value<?>> list = entry.get( "m-dependencies" ).getAll();
            while ( list.hasNext() )
            {
                dependencies[ii] = list.next().getString();
                ii++;
            }
        }

        return new DefaultSchema( name, null, dependencies, isDisabled );
    }


    public Schema getSchema( String schemaName ) throws Exception
    {
        return null;
    }


    public Schema getSchema( String schemaName, Properties schemaProperties ) throws Exception
    {
        return null;
    }


    public void loadWithDependencies( Collection<Schema> schemas, Registries registries ) throws Exception
    {
    }


    public void loadWithDependencies( Schema schemas, Registries registries ) throws Exception
    {
    }


    public void load( Schema schema, Registries registries, boolean isDepLoad ) throws Exception
    {
    }
}
