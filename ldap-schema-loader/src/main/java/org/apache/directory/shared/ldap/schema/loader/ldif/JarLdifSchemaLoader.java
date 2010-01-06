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
package org.apache.directory.shared.ldap.schema.loader.ldif;


import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.ResourceMap;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Loads schema data from LDIF files containing entries representing schema
 * objects, using the meta schema format.
 * 
 * This class is used only for tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class JarLdifSchemaLoader extends AbstractSchemaLoader
{
    /** ldif file extension used */
    private static final String LDIF_EXT = "ldif";
    
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( JarLdifSchemaLoader.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** a map of all the resources in this jar */
    private static final Map<String,Boolean> RESOURCE_MAP = ResourceMap.getResources( Pattern.compile( ".*schema/ou=schema.*" ) );

    
    /**
     * Creates a new LDIF based SchemaLoader. The constructor checks to make
     * sure the supplied base directory exists and contains a schema.ldif file
     * and if not complains about it.
     *
     * @throws Exception if the base directory does not exist or does not
     * a valid schema.ldif file
     */
    public JarLdifSchemaLoader() throws Exception
    {
        initializeSchemas();
    }

    
    private final URL getResource( String resource, String msg ) throws Exception
    {
        if ( RESOURCE_MAP.get( resource ) )
        {
            return DefaultSchemaLdifExtractor.getUniqueResource( resource, msg );
        }
        else
        {
            return new File( resource ).toURI().toURL();
        }
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
        
        for ( String file : RESOURCE_MAP.keySet() )
        {
            Pattern pat = Pattern.compile( ".*schema/ou=schema/cn=[a-z0-9-_]*\\." + LDIF_EXT );
            
            if ( pat.matcher( file ).matches() )
            {
                URL resource = getResource( file, "schema LDIF file" );
                InputStream in = resource.openStream();
                
                try
                {
                    LdifReader reader = new LdifReader( in );
                    LdifEntry entry = reader.next();
                    reader.close();
                    Schema schema = getSchema( entry.getEntry() );
                    schemaMap.put( schema.getSchemaName(), schema );
                    
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Schema Initialized ... \n{}", schema );
                    }
                }
                catch ( Exception e )
                {
                    LOG.error( "Failed to load schema LDIF file " + file, e );
                    throw e;
                }
                finally
                {
                    in.close();
                }
            }
        }
    }


    /**
     * Utility method to get the path for a schema directory.
     *
     * @param schema the schema to get the path for
     * @return the path for the specific schema directory
     */
    private final String getSchemaDirectory( Schema schema )
    {
        return "schema/ou=schema/cn=" + schema.getSchemaName();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadComparators( Schema... schemas ) throws Exception
    {
        List<Entry> comparatorList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return comparatorList;
        }
        
        for ( Schema schema : schemas )
        {
            String comparatorsDirectory = getSchemaDirectory( schema ) 
                + "/" + SchemaConstants.COMPARATORS_PATH;
            
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + comparatorsDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "comparator LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    comparatorList.add( entry.getEntry() );
                }
            }
        }
        
        return comparatorList;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadSyntaxCheckers( Schema... schemas ) throws Exception
    {
        List<Entry> syntaxCheckerList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return syntaxCheckerList;
        }
        
        for ( Schema schema : schemas )
        {
            String syntaxCheckersDirectory = getSchemaDirectory( schema ) 
                +  "/" + SchemaConstants.SYNTAX_CHECKERS_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + syntaxCheckersDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "syntaxChecker LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
                    
                    syntaxCheckerList.add( entry.getEntry() );
                }
            }
        }
        
        return syntaxCheckerList;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadNormalizers( Schema... schemas ) throws Exception
    {
        List<Entry> normalizerList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return normalizerList;
        }
        
        for ( Schema schema : schemas )
        {
            String normalizersDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.NORMALIZERS_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + normalizersDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "normalizer LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
                    
                    normalizerList.add( entry.getEntry() );
                }
            }
        }
        
        return normalizerList;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadMatchingRules( Schema... schemas ) throws Exception
    {
        List<Entry> matchingRuleList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return matchingRuleList;
        }
        
        for ( Schema schema : schemas )
        {
            String matchingRulesDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.MATCHING_RULES_PATH;
            
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + matchingRulesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "matchingRules LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    matchingRuleList.add( entry.getEntry() );
                }
            }
        }
        
        return matchingRuleList;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadSyntaxes( Schema... schemas ) throws Exception
    {
        List<Entry> syntaxList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return syntaxList;
        }
        
        for ( Schema schema : schemas )
        {
            String syntaxesDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.SYNTAXES_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + syntaxesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "syntax LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
                    
                    syntaxList.add( entry.getEntry() );
                }
            }
        }
        
        return syntaxList;
    }

    
    /**
     * {@inheritDoc}
     */
    public List<Entry> loadAttributeTypes( Schema... schemas ) throws Exception
    {
        List<Entry> attributeTypeList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return attributeTypeList;
        }
        
        for ( Schema schema : schemas )
        {
        	// check that the attributeTypes directory exists for the schema
            String attributeTypesDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.ATTRIBUTES_TYPE_PATH;
            
            // get list of attributeType LDIF schema files in attributeTypes
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + attributeTypesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "attributeType LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    attributeTypeList.add( entry.getEntry() );
                }
            }
        }
        
        return attributeTypeList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadMatchingRuleUses( Schema... schemas ) throws Exception
    {
        List<Entry> matchingRuleUseList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return matchingRuleUseList;
        }
        
        for ( Schema schema : schemas )
        {
            String matchingRuleUsesDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.MATCHING_RULE_USE_PATH;
            
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + matchingRuleUsesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "matchingRuleUse LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    matchingRuleUseList.add( entry.getEntry() );
                }
            }
        }
        
        return matchingRuleUseList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadNameForms( Schema... schemas ) throws Exception
    {
        List<Entry> nameFormList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return nameFormList;
        }
        
        for ( Schema schema : schemas )
        {
            String nameFormsDirectory = getSchemaDirectory( schema ) + "/" + SchemaConstants.NAME_FORMS_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + nameFormsDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "nameForm LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    nameFormList.add( entry.getEntry() );
                }
            }
        }
        
        return nameFormList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitContentRules( Schema... schemas ) throws Exception
    {
        List<Entry> ditContentRulesList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return ditContentRulesList;
        }
        
        for ( Schema schema : schemas )
        {
            String ditContentRulesDirectory = getSchemaDirectory( schema ) + "/" + 
                SchemaConstants.DIT_CONTENT_RULES_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + ditContentRulesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "ditContentRule LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
                    
                    ditContentRulesList.add( entry.getEntry() );
                }
            }
        }
        
        return ditContentRulesList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitStructureRules( Schema... schemas ) throws Exception
    {
        List<Entry> ditStructureRuleList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return ditStructureRuleList;
        }
        
        for ( Schema schema : schemas )
        {
            String ditStructureRulesDirectory = getSchemaDirectory( schema )
                + "/" + SchemaConstants.DIT_STRUCTURE_RULES_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + ditStructureRulesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "ditStructureRule LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
                    
                    ditStructureRuleList.add( entry.getEntry() );
                }
            }
        }
        
        return ditStructureRuleList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadObjectClasses( Schema... schemas ) throws Exception
    {
        List<Entry> objectClassList = new ArrayList<Entry>();
        
        if ( schemas == null )
        {
            return objectClassList;
        }
        
        for ( Schema schema : schemas )
        {
        	// get objectClasses directory, check if exists, return if not
        	String objectClassesDirectory = getSchemaDirectory( schema ) + "/" + SchemaConstants.OBJECT_CLASSES_PATH;
    
            for ( String resourcePath : RESOURCE_MAP.keySet() )
            {
                Pattern regex = Pattern.compile( ".*" + objectClassesDirectory + "/m-oid=.*\\." + LDIF_EXT );
                
                if ( regex.matcher( resourcePath ).matches() )
                {
                    URL resource = getResource( resourcePath, "objectClass LDIF file" );
                    LdifReader reader = new LdifReader( resource.openStream() );
                    LdifEntry entry = reader.next();
                    reader.close();
    
                    objectClassList.add( entry.getEntry() );
                }
            }
        }
        
        return objectClassList;
    }
}
