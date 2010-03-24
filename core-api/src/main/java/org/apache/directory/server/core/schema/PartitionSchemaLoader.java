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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.AbstractSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class that loads schemas from a partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionSchemaLoader extends AbstractSchemaLoader
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( PartitionSchemaLoader.class );

    private final SchemaPartitionDao dao;
    private Partition partition;

    /** The attributeType registry */
    private SchemaManager schemaManager;

    private final AttributeType mOidAT;
    private final AttributeType mNameAT;
    private final AttributeType cnAT;
    private final AttributeType byteCodeAT;
    private final AttributeType descAT;
    private final AttributeType fqcnAT;

    private static Map<String, DN> staticAttributeTypeDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticMatchingRulesDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticObjectClassesDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticComparatorsDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticNormalizersDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticSyntaxCheckersDNs = new HashMap<String, DN>();
    private static Map<String, DN> staticSyntaxesDNs = new HashMap<String, DN>();


    public PartitionSchemaLoader( Partition partition, SchemaManager schemaManager ) throws Exception
    {
        this.partition = partition;
        this.schemaManager = schemaManager;

        dao = new SchemaPartitionDaoImpl( this.partition, schemaManager );
        mOidAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_OID_AT );
        mNameAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_NAME_AT );
        cnAT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.CN_AT );
        byteCodeAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_BYTECODE_AT );
        descAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_DESCRIPTION_AT );
        fqcnAT = schemaManager.lookupAttributeTypeRegistry( MetaSchemaConstants.M_FQCN_AT );

        initStaticDNs( "system" );
        initStaticDNs( "core" );
        initStaticDNs( "apache" );
        initStaticDNs( "apachemeta" );
        initStaticDNs( MetaSchemaConstants.SCHEMA_OTHER );
        initStaticDNs( "collective" );
        initStaticDNs( "java" );
        initStaticDNs( "cosine" );
        initStaticDNs( "inetorgperson" );
    }


    private void initStaticDNs( String schemaName ) throws Exception
    {

        // Initialize AttributeType Dns
        DN dn = new DN( SchemaConstants.ATTRIBUTES_TYPE_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticAttributeTypeDNs.put( schemaName, dn );

        // Initialize ObjectClasses Dns
        dn = new DN( SchemaConstants.OBJECT_CLASSES_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticObjectClassesDNs.put( schemaName, dn );

        // Initialize MatchingRules Dns
        dn = new DN( SchemaConstants.MATCHING_RULES_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticMatchingRulesDNs.put( schemaName, dn );

        // Initialize Comparators Dns
        dn = new DN( SchemaConstants.COMPARATORS_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticComparatorsDNs.put( schemaName, dn );

        // Initialize Normalizers Dns
        dn = new DN( SchemaConstants.NORMALIZERS_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticNormalizersDNs.put( schemaName, dn );

        // Initialize SyntaxCheckers Dns
        dn = new DN( SchemaConstants.SYNTAX_CHECKERS_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticSyntaxCheckersDNs.put( schemaName, dn );

        // Initialize Syntaxes Dns
        dn = new DN( SchemaConstants.SYNTAXES_PATH, "cn=" + schemaName, SchemaConstants.OU_SCHEMA );

        dn.normalize( schemaManager.getNormalizerMapping() );
        staticSyntaxesDNs.put( schemaName, dn );

    }


    /**
     * Helper class used to update the static DNs for each kind of Schema Object
     */
    private DN updateDNs( Map<String, DN> staticDNs, String path, Schema schema ) throws LdapInvalidDnException
    {
        DN dn = staticDNs.get( schema.getSchemaName() );

        if ( dn == null )
        {
            dn = new DN( path, "cn=" + schema.getSchemaName(), SchemaConstants.OU_SCHEMA );

            dn.normalize( schemaManager.getNormalizerMapping() );
            staticDNs.put( schema.getSchemaName(), dn );
        }

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
        Set<ServerEntry> results = dao.listSchemaDependents( schemaName );

        if ( results.isEmpty() )
        {
            return dependees;
        }

        for ( ServerEntry sr : results )
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
        Set<ServerEntry> results = dao.listEnabledSchemaDependents( schemaName );

        if ( results.isEmpty() )
        {
            return dependees;
        }

        for ( ServerEntry sr : results )
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


    /**
     * {@inheritDoc}
     */
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
    public List<Entry> loadAttributeTypes( Schema... schemas ) throws Exception
    {
        List<Entry> attributeTypeList = new ArrayList<Entry>();

        for ( Schema schema : schemas )
        {
            DN dn = updateDNs( staticAttributeTypeDNs, SchemaConstants.ATTRIBUTES_TYPE_PATH, schema );

            // Check that we don't have an entry in the Dit for this schema
            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                // No : get out, no AttributeType to load
                return attributeTypeList;
            }

            LOG.debug( "{} schema: loading attributeTypes", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            // Loop on all the AttributeTypes and add them to the list
            while ( list.next() )
            {
                ServerEntry result = list.get();

                attributeTypeList.add( result );
            }
        }

        return attributeTypeList;
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
            DN dn = updateDNs( staticComparatorsDNs, SchemaConstants.COMPARATORS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return comparatorList;
            }

            LOG.debug( "{} schema: loading comparators", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ClonedServerEntry entry = list.get();

                comparatorList.add( entry );
            }
        }

        return comparatorList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitContentRules( Schema... schemas ) throws Exception
    {
        LOG.error( I18n.err( I18n.ERR_86 ) );

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadDitStructureRules( Schema... schemas ) throws Exception
    {
        LOG.error( I18n.err( I18n.ERR_87 ) );

        return null;
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
            DN dn = updateDNs( staticMatchingRulesDNs, SchemaConstants.MATCHING_RULES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return matchingRuleList;
            }

            LOG.debug( "{} schema: loading matchingRules", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ServerEntry entry = list.get();

                matchingRuleList.add( entry );
            }
        }

        return matchingRuleList;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadMatchingRuleUses( Schema... schemas ) throws Exception
    {
        LOG.error( I18n.err( I18n.ERR_88 ) );

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public List<Entry> loadNameForms( Schema... schemas ) throws Exception
    {
        LOG.error( I18n.err( I18n.ERR_89 ) );

        return null;
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
            DN dn = updateDNs( staticNormalizersDNs, SchemaConstants.NORMALIZERS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return normalizerList;
            }

            LOG.debug( "{} schema: loading normalizers", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ClonedServerEntry entry = list.get();

                normalizerList.add( entry );
            }
        }

        return normalizerList;
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
            DN dn = updateDNs( staticObjectClassesDNs, SchemaConstants.OBJECT_CLASSES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return objectClassList;
            }

            LOG.debug( "{} schema: loading objectClasses", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ClonedServerEntry entry = list.get();

                objectClassList.add( entry );
            }
        }

        return objectClassList;
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
            DN dn = updateDNs( staticSyntaxesDNs, SchemaConstants.SYNTAXES_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return syntaxList;
            }

            LOG.debug( "{} schema: loading syntaxes", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ServerEntry entry = list.get();

                syntaxList.add( entry );
            }
        }

        return syntaxList;
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
            DN dn = updateDNs( staticSyntaxCheckersDNs, SchemaConstants.SYNTAX_CHECKERS_PATH, schema );

            if ( !partition.hasEntry( new EntryOperationContext( null, dn ) ) )
            {
                return syntaxCheckerList;
            }

            LOG.debug( "{} schema: loading syntaxCsheckers", schema.getSchemaName() );

            EntryFilteringCursor list = partition.list( new ListOperationContext( null, dn ) );

            while ( list.next() )
            {
                ServerEntry entry = list.get();

                syntaxCheckerList.add( entry );
            }
        }

        return syntaxCheckerList;
    }


    private String getOid( ServerEntry entry ) throws Exception
    {
        EntryAttribute oid = entry.get( mOidAT );

        if ( oid == null )
        {
            return null;
        }

        return oid.getString();
    }


    private NormalizerDescription getNormalizerDescription( String schemaName, ServerEntry entry ) throws Exception
    {
        NormalizerDescription description = new NormalizerDescription( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( entry.get( fqcnAT ).getString() );

        EntryAttribute desc = entry.get( descAT );
        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( desc.getString() );
        }

        EntryAttribute bytecode = entry.get( byteCodeAT );

        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = bytecode.getBytes();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }


    private ClonedServerEntry lookupPartition( DN dn ) throws Exception
    {
        return partition.lookup( new LookupOperationContext( null, dn ) );
    }


    private LdapComparatorDescription getLdapComparatorDescription( String schemaName, ServerEntry entry )
        throws Exception
    {
        LdapComparatorDescription description = new LdapComparatorDescription( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( entry.get( fqcnAT ).getString() );

        EntryAttribute desc = entry.get( descAT );

        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( desc.getString() );
        }

        EntryAttribute bytecode = entry.get( byteCodeAT );

        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = bytecode.getBytes();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }


    private SyntaxCheckerDescription getSyntaxCheckerDescription( String schemaName, ServerEntry entry )
        throws Exception
    {
        SyntaxCheckerDescription description = new SyntaxCheckerDescription( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( entry.get( fqcnAT ).getString() );

        EntryAttribute desc = entry.get( descAT );

        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( desc.getString() );
        }

        EntryAttribute bytecode = entry.get( byteCodeAT );

        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = bytecode.getBytes();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }


    /**
     * @return the dao
     */
    public SchemaPartitionDao getDao()
    {
        return dao;
    }
}
