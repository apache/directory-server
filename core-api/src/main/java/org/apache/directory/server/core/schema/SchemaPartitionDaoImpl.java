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

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.NumericOidSyntaxChecker;
import org.apache.directory.shared.ldap.schemaloader.SchemaEntityFactory;
import org.apache.directory.shared.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A specialized data access object for managing schema objects in the
 * schema partition.
 *
 * WARNING:
 * This dao operates directly on a partition.  Hence no interceptors are available
 * to perform the various expected services of respective interceptors.  Take care
 * to normalize all filters and distinguished names.
 *
 * A single write operation exists for enabling schemas needed for operating indices
 * in partitions and enabling schemas that are dependencies of other schemas that
 * are enabled.  In both these limited cases there is no need to worry about issues
 * with a lack of replication propagation because these same updates will take place
 * on replicas when the original operation is propagated or when replicas start up.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaPartitionDaoImpl implements SchemaPartitionDao
{
    /** static class logger */
    private final Logger LOG = LoggerFactory.getLogger( getClass() );
    private static final NumericOidSyntaxChecker NUMERIC_OID_CHECKER = new NumericOidSyntaxChecker();
    private static final String[] SCHEMA_ATTRIBUTES = new String[]
        { SchemaConstants.CREATORS_NAME_AT_OID, "m-dependencies", SchemaConstants.OBJECT_CLASS_AT_OID,
          SchemaConstants.CN_AT_OID, "m-disabled" };

    private final Partition partition;
    private final SchemaEntityFactory factory;
    private final SchemaManager schemaManager;

    private final AttributeType M_NAME_AT;
    private final AttributeType M_OID_AT;
    private final AttributeType OBJECT_CLASS_AT;
    private final AttributeType M_SYNTAX_AT;
    private final AttributeType M_ORDERING_AT;
    private final AttributeType M_SUBSTRING_AT;
    private final AttributeType M_EQUALITY_AT;
    private final AttributeType M_SUP_ATTRIBUTE_TYPE_AT;
    private final AttributeType M_MUST_AT;
    private final AttributeType M_MAY_AT;
    private final AttributeType M_AUX_AT;
    private final AttributeType M_OC_AT;
    private final AttributeType M_SUP_OBJECT_CLASS_AT;
    private final AttributeType M_DEPENDENCIES_AT;

    private final Set<AttributeTypeOptions> schemaAttributesToReturn = new HashSet<AttributeTypeOptions>();
    private final AttributeType disabledAttributeType;


    /**
     * Creates a schema dao object backing information within a schema partition.
     *
     * @param partition the schema partition
     * @param schemaManager the Server schemaManager used to start up the schema partition
     * @throws NamingException if there are problems initializing this schema partion dao
     */
    public SchemaPartitionDaoImpl( Partition partition, SchemaManager schemaManager ) throws Exception
    {
        this.partition = partition;
        this.factory = new SchemaEntityFactory();
        this.schemaManager = schemaManager;

        this.M_NAME_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_NAME_AT );
        this.disabledAttributeType = schemaManager.getAttributeType( MetaSchemaConstants.M_DISABLED_AT );
        this.M_OID_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_OID_AT );
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        this.M_SYNTAX_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_SYNTAX_AT );
        this.M_ORDERING_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_ORDERING_AT );
        this.M_EQUALITY_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_EQUALITY_AT );
        this.M_SUBSTRING_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_SUBSTR_AT );
        this.M_SUP_ATTRIBUTE_TYPE_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT );
        this.M_MUST_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_MUST_AT );
        this.M_MAY_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_MAY_AT );
        this.M_AUX_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_AUX_AT );
        this.M_OC_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_OC_AT );
        this.M_SUP_OBJECT_CLASS_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT );
        this.M_DEPENDENCIES_AT = schemaManager.getAttributeType( MetaSchemaConstants.M_DEPENDENCIES_AT );

        for ( String attrId : SCHEMA_ATTRIBUTES )
        {
            AttributeTypeOptions ato = new AttributeTypeOptions( schemaManager.lookupAttributeTypeRegistry( attrId ) );
            schemaAttributesToReturn.add( ato );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#getSchemas()
     */
    public Map<String, Schema> getSchemas() throws Exception
    {
        Map<String, Schema> schemas = new HashMap<String, Schema>();
        EntryFilteringCursor list = listSchemas();

        while ( list.next() )
        {
            Entry sr = list.get();
            Schema schema = factory.getSchema( sr );
            schemas.put( schema.getSchemaName(), schema );
        }

        return schemas;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#getSchemaNames()
     */
    public Set<String> getSchemaNames() throws Exception
    {
        Set<String> schemaNames = new HashSet<String>();
        EntryFilteringCursor list = listSchemas();

        while ( list.next() )
        {
            Entry sr = list.get();
            schemaNames.add( sr.get( SchemaConstants.CN_AT ).getString() );
        }

        return schemaNames;
    }


    private EntryFilteringCursor listSchemas() throws Exception
    {
        Dn base = new Dn( SchemaConstants.OU_SCHEMA, schemaManager );
        ExprNode filter = new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_SCHEMA_OC ) );

        SearchOperationContext searchContext = new SearchOperationContext( null );
        searchContext.setDn( base );
        searchContext.setScope( SearchScope.ONELEVEL );
        searchContext.setReturningAttributes( schemaAttributesToReturn );
        searchContext.setFilter( filter );
        return partition.search( searchContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#getSchema(java.lang.String)
     */
    public Schema getSchema( String schemaName ) throws Exception
    {
        Dn dn = new Dn( "cn=" + schemaName + ",ou=schema", schemaManager );
        return factory.getSchema( partition.lookup( new LookupOperationContext( null, dn ) ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#hasMatchingRule(java.lang.String)
     */
    public boolean hasMatchingRule( String oid ) throws Exception
    {
        BranchNode filter = new AndNode();
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            MetaSchemaConstants.META_MATCHING_RULE_OC ) ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new EqualityNode<String>( M_OID_AT, new StringValue( oid ) ) );
        }
        else
        {
            filter.addNode( new EqualityNode<String>( M_NAME_AT, new StringValue( oid.toLowerCase() ) ) );
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return false;
            }

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_430, oid ) );
            }

            return true;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#hasAttributeType(java.lang.String)
     */
    public boolean hasAttributeType( String oid ) throws Exception
    {
        BranchNode filter = new AndNode();
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC ) ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new EqualityNode<String>( M_OID_AT, new StringValue( oid ) ) );
        }
        else
        {
            filter.addNode( new EqualityNode<String>( M_NAME_AT, new StringValue( oid.toLowerCase() ) ) );
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return false;
            }

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_431, oid ) );
            }

            return true;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#hasObjectClass(java.lang.String)
     */
    public boolean hasObjectClass( String oid ) throws Exception
    {
        BranchNode filter = new AndNode();
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_OBJECT_CLASS_OC ) ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new EqualityNode<String>( M_OID_AT, new StringValue( oid ) ) );
        }
        else
        {
            filter.addNode( new EqualityNode<String>( M_NAME_AT, new StringValue( oid.toLowerCase() ) ) );
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return false;
            }

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_431, oid ) );
            }

            return true;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#hasSyntax(java.lang.String)
     */
    public boolean hasSyntax( String oid ) throws Exception
    {
        BranchNode filter = new AndNode();
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
                new StringValue( MetaSchemaConstants.META_SYNTAX_OC ) ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new EqualityNode<String>( M_OID_AT, new StringValue( oid ) ) );
        }
        else
        {
            filter.addNode( new EqualityNode<String>( M_NAME_AT, new StringValue( oid.toLowerCase() ) ) );
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return false;
            }

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_432, oid ) );
            }

            return true;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#hasSyntaxChecker(java.lang.String)
     */
    public boolean hasSyntaxChecker( String oid ) throws Exception
    {
        BranchNode filter = new AndNode();
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_SYNTAX_CHECKER_OC ) ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new EqualityNode<String>( M_OID_AT, new StringValue( oid ) ) );
        }
        else
        {
            filter.addNode( new EqualityNode<String>( M_NAME_AT, new StringValue( oid.toLowerCase() ) ) );
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return false;
            }

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_433, oid ) );
            }

            return true;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#findSchema(java.lang.String)
     */
    public String findSchema( String entityName ) throws Exception
    {
        Dn dn = findDn( entityName );
        if ( dn == null )
        {
            return null;
        }

        Rdn rdn = dn.getRdn( 1 );

        if ( !rdn.getNormType().equalsIgnoreCase( SchemaConstants.CN_AT_OID ) )
        {
            throw new NamingException( I18n.err( I18n.ERR_434, dn.getNormName(), SchemaConstants.CN_AT_OID, rdn.getNormType() ) );
        }

        return rdn.getNormValue().getString();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#findDn(java.lang.String)
     */
    public Dn findDn( String entityName ) throws Exception
    {
        Entry sr = find( entityName );
        Dn dn = sr.getDn();
        dn.normalize( schemaManager );
        return dn;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#find(java.lang.String)
     */
    public Entry find( String entityName ) throws Exception
    {
        BranchNode filter = new OrNode();
        SimpleNode<String> nameAVA = new EqualityNode<String>( M_NAME_AT,
            new StringValue( entityName.toLowerCase() ) );
        SimpleNode<String> oidAVA = new EqualityNode<String>( M_OID_AT,
            new StringValue( entityName.toLowerCase() ) );
        filter.addNode( nameAVA );
        filter.addNode( oidAVA );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            if ( !cursor.next() )
            {
                return null;
            }

            Entry sr = cursor.get();

            if ( cursor.next() )
            {
                throw new NamingException( I18n.err( I18n.ERR_435, entityName ) );
            }

            return sr;
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#enableSchema(java.lang.String)
     */
    public void enableSchema( String schemaName ) throws Exception
    {
        Dn dn = new Dn( "cn=" + schemaName + ",ou=schema", schemaManager );
        Entry entry = partition.lookup( new LookupOperationContext( null, dn ) );
        EntryAttribute disabledAttr = entry.get( disabledAttributeType );
        List<Modification> mods = new ArrayList<Modification>( 3 );

        if ( disabledAttr == null )
        {
            LOG.warn( "Does not make sense: you're trying to enable {} schema which is already enabled", schemaName );
            return;
        }

        boolean isDisabled = disabledAttr.contains( "TRUE" );

        if ( !isDisabled )
        {
            LOG.warn( "Does not make sense: you're trying to enable {} schema which is already enabled", schemaName );
            return;
        }

        mods.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, new DefaultEntryAttribute(
            MetaSchemaConstants.M_DISABLED_AT, schemaManager.getAttributeType( MetaSchemaConstants.M_DISABLED_AT ) ) ) );

        mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, new DefaultEntryAttribute(
            SchemaConstants.MODIFIERS_NAME_AT, schemaManager.getAttributeType( SchemaConstants.MODIFIERS_NAME_AT ),
            ServerDNConstants.ADMIN_SYSTEM_DN ) ) );

        mods.add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, new DefaultEntryAttribute(
            SchemaConstants.MODIFY_TIMESTAMP_AT, schemaManager.getAttributeType( SchemaConstants.MODIFY_TIMESTAMP_AT ), DateUtils
                .getGeneralizedTime() ) ) );

        partition.modify( new ModifyOperationContext( null, dn, mods ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listSyntaxDependents(java.lang.String)
     */
    public Set<Entry> listSyntaxDependents( String numericOid ) throws Exception
    {
        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        // subfilter for (| (objectClass=metaMatchingRule) (objectClass=metaAttributeType))
        BranchNode or = new OrNode();
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_MATCHING_RULE_OC.toLowerCase() ) ) );
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC.toLowerCase() ) ) );

        filter.addNode( or );
        filter.addNode( new EqualityNode<String>( M_SYNTAX_AT, new StringValue( numericOid.toLowerCase() ) ) );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                set.add( cursor.get() );
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listMatchingRuleDependents(org.apache.directory.shared.ldap.schema.MatchingRule)
     */
    public Set<Entry> listMatchingRuleDependents( MatchingRule mr ) throws Exception
    {
        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        // ( objectClass = metaAttributeType )
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC.toLowerCase() ) ) );

        BranchNode or = new OrNode();
        or.addNode( new EqualityNode<String>( M_ORDERING_AT, new StringValue( mr.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_SUBSTRING_AT, new StringValue( mr.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_EQUALITY_AT, new StringValue( mr.getOid() ) ) );
        filter.addNode( or );

        List<String> names = mr.getNames();

        if ( ( names != null ) && ( names.size() > 0 ) )
        {
            for ( String name : names )
            {
                or.addNode( new EqualityNode<String>( M_ORDERING_AT, new StringValue( name.toLowerCase() ) ) );
                or.addNode( new EqualityNode<String>( M_SUBSTRING_AT, new StringValue( name.toLowerCase() ) ) );
                or.addNode( new EqualityNode<String>( M_EQUALITY_AT, new StringValue( name.toLowerCase() ) ) );
            }
        }

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                set.add( cursor.get() );
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listAllNames()
     */
    public EntryFilteringCursor listAllNames() throws Exception
    {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        BranchNode filter = new AndNode();

        // (& (m-oid=*) (m-name=*) )
        filter.addNode( new PresenceNode( M_OID_AT ) );
        filter.addNode( new PresenceNode( M_NAME_AT ) );

        SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
            filter, searchControls );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

        return partition.search( searchOperationContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listAttributeTypeDependents(org.apache.directory.shared.ldap.schema.AttributeType)
     */
    public Set<Entry> listAttributeTypeDependents( AttributeType at ) throws Exception
    {
        /*
         * Right now the following inefficient filter is being used:
         *
         * ( &
         *      ( | ( objectClass = metaAttributeType ) ( objectClass = metaObjectClass ) )
         *      ( | ( m-oid = $oid ) ( m-must = $oid ) ( m-supAttributeType = $oid ) )
         * )
         *
         * the reason why this is inefficient is because the or terms have large scan counts
         * and several loops are going to be required.  The following search is better because
         * it constrains the results better:
         *
         * ( |
         *      ( & ( objectClass = metaAttributeType ) ( m-supAttributeType = $oid ) )
         *      ( & ( objectClass = metaObjectClass ) ( | ( m-may = $oid ) ( m-must = $oid ) ) )
         * )
         */

        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        // ( objectClass = metaAttributeType )
        BranchNode or = new OrNode();
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC.toLowerCase() ) ) );
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_OBJECT_CLASS_OC.toLowerCase() ) ) );
        filter.addNode( or );

        or = new OrNode();
        or.addNode( new EqualityNode<String>( M_MAY_AT, new StringValue( at.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_MUST_AT, new StringValue( at.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_SUP_ATTRIBUTE_TYPE_AT, new StringValue( at.getOid() ) ) );
        filter.addNode( or );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                set.add( cursor.get() );
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listSchemaDependents(java.lang.String)
     */
    public Set<Entry> listSchemaDependents( String schemaName ) throws Exception
    {
        /*
         * The following filter is being used:
         *
         * ( & ( objectClass = metaSchema ) ( m-dependencies = $schemaName ) )
         */

        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( MetaSchemaConstants.META_SCHEMA_OC.toLowerCase() ) ) );
        filter.addNode( new EqualityNode<String>( M_DEPENDENCIES_AT,
            new StringValue( schemaName.toLowerCase() ) ) );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                set.add( cursor.get() );
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listEnabledSchemaDependents(java.lang.String)
     */
    public Set<Entry> listEnabledSchemaDependents( String schemaName ) throws Exception
    {
        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            MetaSchemaConstants.META_SCHEMA_OC.toLowerCase() ) ) );
        filter.addNode( new EqualityNode<String>( M_DEPENDENCIES_AT, new StringValue(
            schemaName.toLowerCase() ) ) );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                Entry sr = cursor.get();
                EntryAttribute disabled = sr.get( disabledAttributeType );

                if ( disabled == null )
                {
                    set.add( sr );
                }
                else if ( disabled.get().toString().equals( "FALSE" ) )
                {
                    set.add( sr );
                }
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaPartitionDao#listObjectClassDependents(org.apache.directory.shared.ldap.schema.ObjectClass)
     */
    public Set<Entry> listObjectClassDependents( ObjectClass oc ) throws Exception
    {
        /*
         * Right now the following inefficient filter is being used:
         *
         * ( &
         *      ( | ( objectClass = metaObjectClass ) ( objectClass = metaDITContentRule )
         *          ( objectClass = metaNameForm ) )
         *      ( | ( m-oc = $oid ) ( m-aux = $oid ) ( m-supObjectClass = $oid ) )
         * )
         *
         * The reason why this is inefficient is because the or terms have large scan counts
         * and several loops are going to be required.  For example all the objectClasses and
         * all the metaDITContentRules and all the metaNameForm candidates will be a massive
         * number.  This is probably going to be bigger than the 2nd term where a candidate
         * satisfies one of the terms.
         *
         * The following search is better because it constrains the results better:
         *
         * ( |
         *      ( & ( objectClass = metaNameForm ) ( m-oc = $oid ) )
         *      ( & ( objectClass = metaObjectClass ) ( m-supObjectClass = $oid ) )
         *      ( & ( objectClass = metaDITContentRule ) ( m-aux = $oid ) )
         * )
         */

        Set<Entry> set = new HashSet<Entry>();
        BranchNode filter = new AndNode();

        BranchNode or = new OrNode();
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( MetaSchemaConstants.META_NAME_FORM_OC
            .toLowerCase() ) ) );
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue( MetaSchemaConstants.META_OBJECT_CLASS_OC
            .toLowerCase() ) ) );
        or.addNode( new EqualityNode<String>( OBJECT_CLASS_AT, new StringValue(
            MetaSchemaConstants.META_DIT_CONTENT_RULE_OC.toLowerCase() ) ) );
        filter.addNode( or );

        or = new OrNode();
        or.addNode( new EqualityNode<String>( M_AUX_AT, new StringValue( oc.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_OC_AT, new StringValue( oc.getOid() ) ) );
        or.addNode( new EqualityNode<String>( M_SUP_OBJECT_CLASS_AT, new StringValue( oc.getOid() ) ) );
        filter.addNode( or );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        EntryFilteringCursor cursor = null;

        try
        {
            SearchOperationContext searchOperationContext = new SearchOperationContext( null, partition.getSuffix(),
                filter, searchControls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            cursor = partition.search( searchOperationContext );

            while ( cursor.next() )
            {
                set.add( cursor.get() );
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }

        return set;
    }
}
