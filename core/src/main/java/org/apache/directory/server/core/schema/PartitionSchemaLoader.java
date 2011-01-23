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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.normalization.FilterNormalizingVisitor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class that loads schemas from a partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionSchemaLoader extends AbstractSchemaLoader
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( PartitionSchemaLoader.class );

    private final SchemaPartitionDao dao;
    private Partition partition;

    /** The attributeType registry */
    private SchemaManager schemaManager;

    private final AttributeType cnAT;


    public PartitionSchemaLoader( Partition partition, SchemaManager schemaManager ) throws Exception
    {
        this.partition = partition;
        this.schemaManager = schemaManager;

        dao = new SchemaPartitionDaoImpl( this.partition, schemaManager );
        cnAT = schemaManager.getAttributeType( SchemaConstants.CN_AT );

        initializeSchemas();
    }


    private void initializeSchemas() throws Exception
    {
        Dn dn = new Dn( SchemaConstants.OU_SCHEMA, schemaManager );

        // Check that the ou=schema entry exists
        if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
        {
            return;
        }

        LOG.debug( "Loading schemas" );

        // One-level search for all schema entries
        SearchOperationContext searchCtx = new SearchOperationContext( null );
        searchCtx.setDn( dn );
        searchCtx.setScope( SearchScope.ONELEVEL );
        ExprNode filter = FilterParser.parse( schemaManager, "(objectClass=metaSchema)" );
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        FilterNormalizingVisitor visitor = new FilterNormalizingVisitor( ncn, schemaManager );
        filter.accept( visitor );
        searchCtx.setFilter( filter );
        EntryFilteringCursor list = partition.search( searchCtx );

        // Loop on all the schema entries
        while ( list.next() )
        {
            Entry entry = list.get();
            Schema schema = getSchema( entry );
            schemaMap.put( schema.getSchemaName(), schema );
        }
    }


    /**
     * Gets the base Dn for an Schema Object
     */
    private Dn getBaseDN( String path, Schema schema ) throws LdapInvalidDnException
    {
        Dn dn = new Dn( schemaManager, path, "cn=" + schema.getSchemaName(), SchemaConstants.OU_SCHEMA );

        return dn;
    }


    /**
     * Lists the names of the schemas that depend on the schema name provided.
     * 
     * @param schemaName the name of the schema to find dependents for
     * @return a set of schemas (String names) that depend on the schema
     * @throws Exception if there are problems searching the schema partition
     */
    public Set<String> listDependentSchemaNames( String schemaName ) throws Exception
    {
        Set<String> dependees = new HashSet<String>();
        Set<Entry> results = dao.listSchemaDependents( schemaName );

        if ( results.isEmpty() )
        {
            return dependees;
        }

        for ( Entry sr : results )
        {
            EntryAttribute cn = sr.get( cnAT );
            dependees.add( cn.getString() );
        }

        return dependees;
    }


    /**
     * Lists the names of the enabled schemas that depend on the schema name 
     * provided.
     * 
     * @param schemaName the name of the schema to find dependents for
     * @return a set of enabled schemas (String names) that depend on the schema
     * @throws Exception if there are problems searching the schema partition
     */
    public Set<String> listEnabledDependentSchemaNames( String schemaName ) throws Exception
    {
        Set<String> dependees = new HashSet<String>();
        Set<Entry> results = dao.listEnabledSchemaDependents( schemaName );

        if ( results.isEmpty() )
        {
            return dependees;
        }

        for ( Entry sr : results )
        {
            EntryAttribute cn = sr.get( cnAT );
            dependees.add( cn.getString() );
        }

        return dependees;
    }


    public Map<String, Schema> getSchemas() throws Exception
    {
        return dao.getSchemas();
    }


    public Set<String> getSchemaNames() throws Exception
    {
        return dao.getSchemaNames();
    }


    public Schema getSchema( String schemaName )
    {
        try
        {
            return dao.getSchema( schemaName );
        }
        catch ( Exception e )
        {
            // TODO fixme
            return null;
        }
    }


    public final void load( Schema schema, Registries targetRegistries, boolean isDepLoad ) throws Exception
    {
        // if we're loading a dependency and it has not been enabled on 
        // disk then enable it on disk before we proceed to load it
        if ( schema.isDisabled() && isDepLoad )
        {
            dao.enableSchema( schema.getSchemaName() );
        }

        if ( targetRegistries.isSchemaLoaded( schema.getSchemaName() ) )
        {
            LOG.debug( "schema {} already seems to be loaded", schema.getSchemaName() );
            return;
        }

        LOG.debug( "loading {} schema ...", schema.getSchemaName() );

        loadComparators( schema );
        loadNormalizers( schema );
        loadSyntaxCheckers( schema );
        loadSyntaxes( schema );
        loadMatchingRules( schema );
        loadAttributeTypes( schema );
        loadObjectClasses( schema );
        loadMatchingRuleUses( schema );
        loadDitContentRules( schema );
        loadNameForms( schema );

        // order does matter here so some special trickery is needed
        // we cannot load a DSR before the DSRs it depends on are loaded?
        // TODO need to confirm this ( or we must make the class for this and use deferred 
        // resolution until everything is available?

        loadDitStructureRules( schema );

        notifyListenerOrRegistries( schema, targetRegistries );
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadAttributeTypes( Schema... schemas ) throws LdapException
    {
        List<Entry> attributeTypeList = new ArrayList<Entry>();

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.ATTRIBUTES_TYPE_PATH, schema );

            // Check that we don't have an entry in the Dit for this schema
            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                // No : get out, no AttributeType to load
                return attributeTypeList;
            }

            LOG.debug( "{} schema: loading attributeTypes", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                // Loop on all the AttributeTypes and add them to the list
                while ( list.next() )
                {
                    Entry result = list.get();
    
                    attributeTypeList.add( result );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return attributeTypeList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadComparators( Schema... schemas ) throws LdapException
    {
        List<Entry> comparatorList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return comparatorList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.COMPARATORS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return comparatorList;
            }

            LOG.debug( "{} schema: loading comparators", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                while ( list.next() )
                {
                    ClonedServerEntry entry = list.get();
    
                    comparatorList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return comparatorList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitContentRules( Schema... schemas ) throws LdapException
    {
        LOG.error( I18n.err( I18n.ERR_86 ) );

        List<Entry> ditContentRuleList = new ArrayList<Entry>();
        return ditContentRuleList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitStructureRules( Schema... schemas ) throws LdapException
    {
        LOG.error( I18n.err( I18n.ERR_87 ) );

        List<Entry> ditStructureRuleList = new ArrayList<Entry>();
        return ditStructureRuleList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadMatchingRules( Schema... schemas ) throws LdapException
    {
        List<Entry> matchingRuleList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return matchingRuleList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.MATCHING_RULES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return matchingRuleList;
            }

            LOG.debug( "{} schema: loading matchingRules", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                while ( list.next() )
                {
                    Entry entry = list.get();
    
                    matchingRuleList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return matchingRuleList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadMatchingRuleUses( Schema... schemas ) throws LdapException
    {
        LOG.error( I18n.err( I18n.ERR_88 ) );

        List<Entry> matchingRuleUsesList = new ArrayList<Entry>();
        return matchingRuleUsesList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadNameForms( Schema... schemas ) throws LdapException
    {
        LOG.error( I18n.err( I18n.ERR_89 ) );

        List<Entry> nameFormList = new ArrayList<Entry>();
        return nameFormList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadNormalizers( Schema... schemas ) throws LdapException
    {
        List<Entry> normalizerList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return normalizerList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.NORMALIZERS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return normalizerList;
            }

            LOG.debug( "{} schema: loading normalizers", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                while ( list.next() )
                {
                    ClonedServerEntry entry = list.get();
    
                    normalizerList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return normalizerList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadObjectClasses( Schema... schemas ) throws LdapException
    {
        List<Entry> objectClassList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return objectClassList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.OBJECT_CLASSES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return objectClassList;
            }

            LOG.debug( "{} schema: loading objectClasses", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                while ( list.next() )
                {
                    ClonedServerEntry entry = list.get();
    
                    objectClassList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return objectClassList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadSyntaxes( Schema... schemas ) throws LdapException
    {
        List<Entry> syntaxList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return syntaxList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.SYNTAXES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return syntaxList;
            }

            LOG.debug( "{} schema: loading syntaxes", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {
                while ( list.next() )
                {
                    Entry entry = list.get();
    
                    syntaxList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return syntaxList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadSyntaxCheckers( Schema... schemas ) throws LdapException
    {
        List<Entry> syntaxCheckerList = new ArrayList<Entry>();

        if ( schemas == null )
        {
            return syntaxCheckerList;
        }

        for ( Schema schema : schemas )
        {
            Dn dn = getBaseDN( SchemaConstants.SYNTAX_CHECKERS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return syntaxCheckerList;
            }

            LOG.debug( "{} schema: loading syntaxCsheckers", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            try
            {            
                while ( list.next() )
                {
                    Entry entry = list.get();
    
                    syntaxCheckerList.add( entry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }

        return syntaxCheckerList;
    }


    /**
     * @return the dao
     */
    public SchemaPartitionDao getDao()
    {
        return dao;
    }
}
