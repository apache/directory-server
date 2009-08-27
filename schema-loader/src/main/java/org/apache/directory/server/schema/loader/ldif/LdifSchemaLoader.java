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


import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
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
    /** ldif file extension used */
    private static final String LDIF_EXT = "ldif";
    
    /** ou=schema LDIF file name */
    private static final String OU_SCHEMA_LDIF = "schema." + LDIF_EXT;
    
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( LdifSchemaLoader.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** name of directory containing LdapComparators */
    private static final String COMPARATORS_DIRNAME = "comparators";
    
    /** name of directory containing SyntaxCheckers */
    private static final String SYNTAX_CHECKERS_DIRNAME = "syntaxCheckers";

    /** name of the directory containing Normalizers */
    private static final String NORMALIZERS_DIRNAME = "normalizers";

    private final SchemaEntityFactory factory = new SchemaEntityFactory();
    private final File baseDirectory;
    private final Map<String,Schema> schemaMap = new HashMap<String,Schema>();
    private final FilenameFilter ldifFilter = new FilenameFilter()
    {
        public boolean accept( File file, String name )
        {
            return name.endsWith( LDIF_EXT );
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

        File schemaLdif = new File( baseDirectory, OU_SCHEMA_LDIF );
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
            Schema schema = factory.getSchema( entry.getEntry() );
            schemaMap.put( schema.getSchemaName(), schema );
            
            if ( IS_DEBUG )
            {
                LOG.debug( "Schema Initialized ... \n{}", schema );
            }
        }
    }


    public Schema getSchema( String schemaName ) throws Exception
    {
        return this.schemaMap.get( schemaName );
    }


    public void loadWithDependencies( Collection<Schema> schemas, Registries registries ) throws Exception
    {
        Map<String,Schema> notLoaded = new HashMap<String,Schema>();
        
        for ( Schema schema : schemas )
        {
            if ( ! registries.isSchemaLoaded( schema.getSchemaName() ) )
            {
                notLoaded.put( schema.getSchemaName(), schema );
            }
        }
        
        for ( Schema schema : notLoaded.values() )
        {
            Stack<String> beenthere = new Stack<String>();
            super.loadDepsFirst( schema, beenthere, notLoaded, schema, registries );
        }
    }


    public void loadWithDependencies( Schema schema, Registries registries ) throws Exception
    {
        Stack<String> beenthere = new Stack<String>();
        Map<String,Schema> notLoaded = new HashMap<String,Schema>();
        notLoaded.put( schema.getSchemaName(), schema );
        super.loadDepsFirst( schema, beenthere, notLoaded, schema, registries );
    }


    public void load( Schema schema, Registries registries, boolean isDepLoad ) throws Exception
    {
        if ( registries.isSchemaLoaded( schema.getSchemaName() ) )
        {
            LOG.info( "Will not attempt to load already loaded '{}' " +
            		"schema: \n{}", schema.getSchemaName(), schema );
            return;
        }
        
        LOG.info( "Loading {} schema: \n{}", schema.getSchemaName(), schema );
        
        loadSyntaxCheckers( schema, registries );
        loadComparators( schema, registries );
        loadNormalizers( schema, registries );
    }
    
    
    private File getSchemaDirectory( Schema schema )
    {
        return new File( new File( baseDirectory, "schema" ), 
            schema.getSchemaName() );
    }
    
    
    private void loadComparators( Schema schema, Registries targetRegistries ) throws Exception
    {
        File comparatorsDirectory = new File( getSchemaDirectory( schema ), 
            COMPARATORS_DIRNAME );
        File[] comparators = comparatorsDirectory.listFiles( ldifFilter );
        for ( File ldifFile : comparators )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            LdapComparator<?> comparator = 
                factory.getLdapComparator( entry.getEntry(), targetRegistries );
            targetRegistries.getComparatorRegistry().register( comparator );
        }
    }
    
    
    private void loadSyntaxCheckers( Schema schema, Registries targetRegistries ) throws Exception
    {
        File syntaxCheckersDirectory = new File( getSchemaDirectory( schema ), 
            SYNTAX_CHECKERS_DIRNAME );
        File[] syntaxCheckerFiles = syntaxCheckersDirectory.listFiles( ldifFilter );
        for ( File ldifFile : syntaxCheckerFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            SyntaxChecker syntaxChecker = 
                factory.getSyntaxChecker( entry.getEntry(), targetRegistries );
            targetRegistries.getSyntaxCheckerRegistry().register( syntaxChecker );
        }
    }
    
    
    private void loadNormalizers( Schema schema, Registries targetRegistries ) throws Exception
    {
        File normalizersDirectory = new File( getSchemaDirectory( schema ), 
            NORMALIZERS_DIRNAME );
        File[] normalizerFiles = normalizersDirectory.listFiles( ldifFilter );
        for ( File ldifFile : normalizerFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            Normalizer normalizer =
                factory.getNormalizer( entry.getEntry(), targetRegistries );
            targetRegistries.getNormalizerRegistry().register( normalizer );
        }
    }
}
