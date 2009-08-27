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


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
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
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


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

    /** the factory that generates respective SchemaObjects from LDIF entries */
    private final SchemaEntityFactory factory = new SchemaEntityFactory();
    
    /** directory containing the schema LDIF file for ou=schema */
    private final File baseDirectory;
    
    /** a filter for listing all the LDIF files within a directory */
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


    /**
     * {@inheritDoc}
     */
    public Schema getSchema( String schemaName ) throws Exception
    {
        return this.schemaMap.get( schemaName );
    }


    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
    public void loadWithDependencies( Schema schema, Registries registries ) throws Exception
    {
        Stack<String> beenthere = new Stack<String>();
        Map<String,Schema> notLoaded = new HashMap<String,Schema>();
        notLoaded.put( schema.getSchemaName(), schema );
        super.loadDepsFirst( schema, beenthere, notLoaded, schema, registries );
    }


    /**
     * Loads a single schema if it has not been loaded already.  If the schema
     * load request was made because some other schema depends on this one then
     * the schema is checked to see if it is disabled.  If disabled it is 
     * enabled with a write to disk and then loaded. Listeners are notified that
     * the schema has been loaded.
     * 
     * {@inheritDoc}
     */
    public void load( Schema schema, Registries registries, boolean isDepLoad ) throws Exception
    {
        // if we're loading a dependency and it has not been enabled on 
        // disk then enable it on disk before we proceed to load it
        if ( schema.isDisabled() && isDepLoad )
        {
            enableSchema( schema );
        }
        
        if ( registries.isSchemaLoaded( schema.getSchemaName() ) )
        {
            LOG.info( "Will not attempt to load already loaded '{}' " +
            		"schema: \n{}", schema.getSchemaName(), schema );
            return;
        }
        
        LOG.info( "Loading {} schema: \n{}", schema.getSchemaName(), schema );
        
        loadComparators( schema, registries );
        loadNormalizers( schema, registries );
        loadSyntaxCheckers( schema, registries );
        loadSyntaxes( schema, registries );
        loadMatchingRules( schema, registries );
        loadAttributeTypes( schema, registries );
        loadObjectClasses( schema, registries );
        loadMatchingRuleUses( schema, registries );
        loadDitContentRules( schema, registries );
        loadNameForms( schema, registries );
        loadDitStructureRules( schema, registries );

        notifyListenerOrRegistries( schema, registries );
    }

    
    /**
     * Utility method used to enable a specific schema on disk in the LDIF
     * based schema repository.  This method will remove the m-disabled AT
     * in the schema file and update the modifiersName and modifyTimestamp.
     * 
     * The modifiersName and modifyTimestamp on the schema.ldif file will
     * also be updated to indicate a change to the schema.
     *
     * @param schema the disabled schema to enable
     * @throws Exception if there are problems writing changes back to disk
     */
    private void enableSchema( Schema schema ) throws Exception
    {
        // -------------------------------------------------------------------
        // Modifying the foo schema foo.ldif file to be enabled but still
        // have to now update the timestamps and update the modifiersName
        // -------------------------------------------------------------------
        
        File schemaLdifFile = new File( new File( baseDirectory, "schema" ), 
            schema.getSchemaName() + "." + LDIF_EXT );
        LdifReader reader = new LdifReader( schemaLdifFile );
        LdifEntry ldifEntry = reader.next();
        Entry entry = ldifEntry.getEntry();
        
        entry.removeAttributes( "changeType" );
        entry.removeAttributes( SchemaConstants.MODIFIERS_NAME_AT );
        entry.removeAttributes( SchemaConstants.MODIFY_TIMESTAMP_AT );
        entry.removeAttributes( MetaSchemaConstants.M_DISABLED_AT );
        
        entry.add( SchemaConstants.MODIFIERS_NAME_AT, 
            ServerDNConstants.ADMIN_SYSTEM_DN );
        entry.add( SchemaConstants.MODIFY_TIMESTAMP_AT, 
            DateUtils.getGeneralizedTime() );
        
        FileWriter out = new FileWriter( schemaLdifFile );
        out.write( LdifUtils.convertEntryToLdif( entry ) );
        out.flush();
        out.close();
        
        // -------------------------------------------------------------------
        // Now we need to update the timestamp on the schema.ldif file which
        // shows that something changed below the schema directory in schema
        // -------------------------------------------------------------------
        
        schemaLdifFile = new File( baseDirectory, "schema." + LDIF_EXT );
        reader = new LdifReader( schemaLdifFile );
        ldifEntry = reader.next();
        entry = ldifEntry.getEntry();
        
        entry.removeAttributes( "changeType" );
        entry.removeAttributes( SchemaConstants.MODIFIERS_NAME_AT );
        entry.removeAttributes( SchemaConstants.MODIFY_TIMESTAMP_AT );

        entry.add( SchemaConstants.MODIFIERS_NAME_AT, 
            ServerDNConstants.ADMIN_SYSTEM_DN );
        entry.add( SchemaConstants.MODIFY_TIMESTAMP_AT, 
            DateUtils.getGeneralizedTime() );
        
        out = new FileWriter( schemaLdifFile );
        out.write( LdifUtils.convertEntryToLdif( entry ) );
        out.flush();
        out.close();
    }


    /**
     * Utility method to get the file for a schema directory.
     *
     * @param schema the schema to get the file for
     * @return the file for the specific schema directory
     */
    private final File getSchemaDirectory( Schema schema )
    {
        return new File( new File( baseDirectory, "schema" ), 
            schema.getSchemaName() );
    }
    
    
    /**
     * Loads the Comparators from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which comparators are loaded
     * @param registries the registries which are loaded with comparators
     * @throws Exception if there are failures accessing comparator information
     * stored in LDIF files
     */
    private void loadComparators( Schema schema, Registries registries ) throws Exception
    {
        File comparatorsDirectory = new File( getSchemaDirectory( schema ), 
            COMPARATORS_DIRNAME );
        
        if ( ! comparatorsDirectory.exists() )
        {
            return;
        }
        
        File[] comparators = comparatorsDirectory.listFiles( ldifFilter );
        for ( File ldifFile : comparators )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            LdapComparator<?> comparator = 
                factory.getLdapComparator( entry.getEntry(), registries );
            registries.getComparatorRegistry().register( comparator );
        }
    }
    
    
    /**
     * Loads the SyntaxCheckers from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which syntaxCheckers are loaded
     * @param targetRegistries the registries which are loaded with syntaxCheckers
     * @throws Exception if there are failures accessing syntaxChecker 
     * information stored in LDIF files
     */
    private void loadSyntaxCheckers( Schema schema, Registries registries ) throws Exception
    {
        File syntaxCheckersDirectory = new File( getSchemaDirectory( schema ), 
            SYNTAX_CHECKERS_DIRNAME );
        
        if ( ! syntaxCheckersDirectory.exists() )
        {
            return;
        }
        
        File[] syntaxCheckerFiles = syntaxCheckersDirectory.listFiles( ldifFilter );
        for ( File ldifFile : syntaxCheckerFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            SyntaxChecker syntaxChecker = 
                factory.getSyntaxChecker( entry.getEntry(), registries );
            registries.getSyntaxCheckerRegistry().register( syntaxChecker );
        }
    }
    
    
    /**
     * Loads the Normalizers from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which normalizers are loaded
     * @param registries the registries which are loaded with normalizers
     * @throws Exception if there are failures accessing normalizer information
     * stored in LDIF files
     */
    private void loadNormalizers( Schema schema, Registries registries ) throws Exception
    {
        File normalizersDirectory = new File( getSchemaDirectory( schema ), 
            NORMALIZERS_DIRNAME );
        
        if ( ! normalizersDirectory.exists() )
        {
            return;
        }
        
        File[] normalizerFiles = normalizersDirectory.listFiles( ldifFilter );
        for ( File ldifFile : normalizerFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            Normalizer normalizer =
                factory.getNormalizer( entry.getEntry(), registries );
            registries.getNormalizerRegistry().register( normalizer );
        }
    }
    
    
    /**
     * Loads the MatchingRules from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which matchingRules are loaded
     * @param registries the registries which are loaded with matchingRules
     * @throws Exception if there are failures accessing matchingRule 
     * information stored in LDIF files
     */
    private void loadMatchingRules( Schema schema, Registries registries ) throws Exception
    {
        File matchingRulesDirectory = new File( getSchemaDirectory( schema ), 
            MATCHING_RULES_DIRNAME );
        
        if ( ! matchingRulesDirectory.exists() )
        {
            return;
        }
        
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
    
    
    /**
     * Loads the Syntaxes from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which syntaxes are loaded
     * @param registries the registries which are loaded with syntaxes
     * @throws Exception if there are failures accessing comparator information
     * stored in LDIF files
     */
    private void loadSyntaxes( Schema schema, Registries registries ) throws Exception
    {
        File syntaxesDirectory = new File( getSchemaDirectory( schema ), 
            SYNTAXES_DIRNAME );
        
        if ( ! syntaxesDirectory.exists() )
        {
            return;
        }
        
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

    
    /**
     * Loads the AttributeTypes from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which attributeTypes are loaded
     * @param registries the registries which are loaded with attributeTypes
     * @throws Exception if there are failures accessing attributeTypes 
     * information stored in LDIF files
     */
    private void loadAttributeTypes( Schema schema, Registries registries ) throws Exception
    {
        File attributeTypesDirectory = new File ( getSchemaDirectory( schema ), 
            ATTRIBUTE_TYPES_DIRNAME );
        
        if ( ! attributeTypesDirectory.exists() )
        {
            return;
        }
        
        File[] attributeTypeFiles = attributeTypesDirectory.listFiles( ldifFilter );
        for ( File ldifFile : attributeTypeFiles )
        {
            LdifReader reader = new LdifReader( ldifFile );
            LdifEntry entry = reader.next();
            AttributeType attributeType = factory.getAttributeType( 
                entry.getEntry(), registries, schema.getSchemaName() );
            registries.getAttributeTypeRegistry().register( attributeType );
        }
    }


    /**
     * Loads the MatchingRuleUses from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which matchingRuleUses are loaded
     * @param registries the registries which are loaded with matchingRuleUses
     * @throws Exception if there are failures accessing matchingRuleUse 
     * information stored in LDIF files
     */
    private void loadMatchingRuleUses( Schema schema, Registries registries ) throws Exception
    {
        File matchingRuleUsesDirectory = new File( getSchemaDirectory( schema ),
            MATCHING_RULE_USES_DIRNAME );
        
        if ( ! matchingRuleUsesDirectory.exists() )
        {
            return;
        }
        
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


    /**
     * Loads the NameForms from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which nameForms are loaded
     * @param registries the registries which are loaded with nameForms
     * @throws Exception if there are failures accessing nameForm information
     * stored in LDIF files
     */
    private void loadNameForms( Schema schema, Registries registries ) throws Exception
    {
        File nameFormsDirectory = new File( getSchemaDirectory( schema ),
            NAME_FORMS_DIRNAME );
        
        if ( ! nameFormsDirectory.exists() )
        {
            return;
        }
        
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


    /**
     * Loads the DitContentRules from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which ditContentRules are loaded
     * @param registries the registries which are loaded with ditContentRules
     * @throws Exception if there are failures accessing ditContentRules 
     * information stored in LDIF files
     */
    private void loadDitContentRules( Schema schema, Registries registries ) throws Exception
    {
        File ditContentRulesDirectory = new File( getSchemaDirectory( schema ),
            DIT_CONTENT_RULES_DIRNAME );
        
        if ( ! ditContentRulesDirectory.exists() )
        {
            return;
        }
        
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


    /**
     * Loads the ditStructureRules from LDIF files in the supplied schema into 
     * the supplied registries.
     *
     * @param schema the schema for which ditStructureRules are loaded
     * @param registries the registries which are loaded with ditStructureRules
     * @throws Exception if there are failures accessing ditStructureRule 
     * information stored in LDIF files
     */
    private void loadDitStructureRules( Schema schema, Registries registries ) throws Exception
    {
        File ditStructureRulesDirectory = new File( getSchemaDirectory( schema ),
            DIT_STRUCTURE_RULES_DIRNAME );
        
        if ( ! ditStructureRulesDirectory.exists() )
        {
            return;
        }
        
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


    /**
     * Loads the ObjectClasses from LDIF files in the supplied schema into the 
     * supplied registries.
     *
     * @param schema the schema for which objectClasses are loaded
     * @param registries the registries which are loaded with objectClasses
     * @throws Exception if there are failures accessing objectClass information
     * stored in LDIF files
     */
    private void loadObjectClasses( Schema schema, Registries registries ) throws Exception
    {
        File objectClassesDirectory = new File( getSchemaDirectory( schema ),
            OBJECT_CLASSES_DIRNAME );
        
        if ( ! objectClassesDirectory.exists() )
        {
            return;
        }
        
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
