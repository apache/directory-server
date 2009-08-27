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


import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


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

    /** name of directory containing ldapComparators */
    private static final String COMPARATORS_DIRNAME = "comparators";
    
    /** name of directory containing syntaxCheckers */
    private static final String SYNTAX_CHECKERS_DIRNAME = "syntaxCheckers";

    /** name of the directory containing normalizers */
    private static final String NORMALIZERS_DIRNAME = "normalizers";

    /** name of the directory containing syntaxes */
    private static final String SYNTAXES_DIRNAME = "syntaxes";
    
    /** name of the directory containing attributeTypes */
    private static final String ATTRIBUTE_TYPES_DIRNAME = "attributeTypes";
    
    /** name of the directory containing matchingRules */
    private final static String MATCHING_RULES_DIRNAME = "matchingRules";

    /** name of the directory containing objectClasses */
    private static final String OBJECT_CLASSES_DIRNAME = "objectClasses";
    
    /** name of the directory containing ditStructureRules */
    private static final String DIT_STRUCTURE_RULES_DIRNAME = "ditStructureRules";
    
    /** name of the directory containing ditContentRules */
    private static final String DIT_CONTENT_RULES_DIRNAME = "ditContentRules";
    
    /** name of the directory containing nameForms */
    private static final String NAME_FORMS_DIRNAME = "nameForms";
    
    /** name of the directory containing matchingRuleUses */
    private static final String MATCHING_RULE_USES_DIRNAME = "matchingRuleUse";

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
        loadSyntaxes( schema, registries );
        loadMatchingRules( schema, registries );
        loadAttributeTypes( schema, registries );
        loadObjectClasses( schema, registries );
        loadDitStructureRules( schema, registries );
        loadDitContentRules( schema, registries );
        loadNameForms( schema, registries );
        loadMatchingRuleUses( schema, registries );
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
    
    
    private void loadMatchingRules( Schema schema, Registries registries ) throws Exception
    {
        File matchingRulesDirectory = new File( getSchemaDirectory( schema ), 
            MATCHING_RULES_DIRNAME );
        File[] matchingRuleFiles = matchingRulesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : matchingRuleFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            MatchingRule matchingRule = factory.getMatchingRule( 
                entry.getEntry(), registries, schema.getSchemaName() );
            registries.getMatchingRuleRegistry().register( matchingRule );
        }
    }
    
    
    private void loadSyntaxes( Schema schema, Registries registries ) throws Exception
    {
        File syntaxesDirectory = new File( getSchemaDirectory( schema ), 
            SYNTAXES_DIRNAME );
        File[] syntaxFiles = syntaxesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : syntaxFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            LdapSyntax syntax = factory.getSyntax( 
                entry.getEntry(), registries, schema.getSchemaName() );
            registries.getLdapSyntaxRegistry().register( syntax );
        }
    }

    
    private void loadAttributeTypes( Schema schema, Registries registries ) throws Exception
    {
        File attributeTypeDirectory = new File ( getSchemaDirectory( schema ), 
            ATTRIBUTE_TYPES_DIRNAME );
        File[] attributeTypeFiles = attributeTypeDirectory.listFiles( ldifFilter );
        for ( File ldifFile : attributeTypeFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            AttributeType attributeType = factory.getAttributeType( 
                entry.getEntry(), registries, schema.getSchemaName() );
            registries.getAttributeTypeRegistry().register( attributeType );
        }
    }


    private void loadMatchingRuleUses( Schema schema, Registries registries ) throws Exception
    {
        File matchingRuleUsesDirectory = new File( getSchemaDirectory( schema ),
            MATCHING_RULE_USES_DIRNAME );
        File[] matchingRuleUseFiles = matchingRuleUsesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : matchingRuleUseFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            MatchingRuleUse matchingRuleUse = null;
            
            // TODO add factory method to generate the matchingRuleUse
            if ( true )
            {
                throw new NotImplementedException( "Need to implement factory " +
                		"method for creating a matchingRuleUse" );
            }
            
            registries.getMatchingRuleUseRegistry().register( matchingRuleUse );
        }
    }


    private void loadNameForms( Schema schema, Registries registries ) throws Exception
    {
        File nameFormsDirectory = new File( getSchemaDirectory( schema ),
            NAME_FORMS_DIRNAME );
        File[] nameFormFiles = nameFormsDirectory.listFiles( ldifFilter );
        for ( File ldifFile : nameFormFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            NameForm nameForm = null;

            // TODO add factory method to generate the nameForm
            if ( true )
            {
                throw new NotImplementedException( "Need to implement factory " +
                        "method for creating a nameForm" );
            }
            
            registries.getNameFormRegistry().register( nameForm );
        }
    }


    private void loadDitContentRules( Schema schema, Registries registries ) throws Exception
    {
        File ditContentRulesDirectory = new File( getSchemaDirectory( schema ),
            DIT_CONTENT_RULES_DIRNAME );
        File[] ditContentRuleFiles = ditContentRulesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : ditContentRuleFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            DITContentRule ditContentRule = null;
            
            // TODO add factory method to generate the ditContentRule
            if ( true )
            {
                throw new NotImplementedException( "Need to implement factory " +
                        "method for creating a ditContentRule" );
            }
            
            registries.getDitContentRuleRegistry().register( ditContentRule );
        }
    }


    private void loadDitStructureRules( Schema schema, Registries registries ) throws Exception
    {
        File ditStructureRulesDirectory = new File( getSchemaDirectory( schema ),
            DIT_STRUCTURE_RULES_DIRNAME );
        File[] ditStructureRuleFiles = ditStructureRulesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : ditStructureRuleFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            DITStructureRule ditStructureRule = null;
            
            // TODO add factory method to generate the ditContentRule
            if ( true )
            {
                throw new NotImplementedException( "Need to implement factory " +
                        "method for creating a ditStructureRule" );
            }
            
            registries.getDitStructureRuleRegistry().register( ditStructureRule );
        }
    }


    private void loadObjectClasses( Schema schema, Registries registries ) throws Exception
    {
        File objectClassesDirectory = new File( getSchemaDirectory( schema ),
            OBJECT_CLASSES_DIRNAME );
        File[] objectClassFiles = objectClassesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : objectClassFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            ObjectClass objectClass = factory.getObjectClass( 
                entry.getEntry(), registries, schema.getSchemaName() );
            registries.getObjectClassRegistry().register( objectClass );
        }
    }
}
