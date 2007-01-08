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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.AssertionEnum;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.syntax.NumericOidSyntaxChecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A specialized data access object for managing schema objects in the
 * schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaPartitionDao
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( SchemaPartitionDao.class );
    private final static NumericOidSyntaxChecker NUMERIC_OID_CHECKER = new NumericOidSyntaxChecker();


    private final Partition partition;
    private final Registries bootstrapRegistries;
    private final SchemaEntityFactory factory;
    private final OidRegistry oidRegistry;
    private final AttributeTypeRegistry attrRegistry;
    
    private final String M_NAME_OID;
    private final String CN_OID;
    private final String M_OID_OID;
    private final String OBJECTCLASS_OID;
    private final String META_SYNTAX_OID;
    
    private final AttributeType disabledAttributeType;
    
    
    /**
     * Creates a schema dao object backing information within a schema partition.
     * 
     * @param partition
     * @throws NamingException 
     */
    public SchemaPartitionDao( Partition partition, Registries bootstrapRegistries ) throws NamingException
    {
        this.partition = partition;
        this.bootstrapRegistries = bootstrapRegistries;
        this.factory = new SchemaEntityFactory( this.bootstrapRegistries );
        this.oidRegistry = this.bootstrapRegistries.getOidRegistry();
        this.attrRegistry = this.bootstrapRegistries.getAttributeTypeRegistry();
        
        this.M_NAME_OID = oidRegistry.getOid( MetaSchemaConstants.M_NAME_AT );
        this.CN_OID = oidRegistry.getOid( SystemSchemaConstants.CN_AT );
        this.disabledAttributeType = attrRegistry.lookup( MetaSchemaConstants.M_DISABLED_AT );
        this.M_OID_OID = oidRegistry.getOid( MetaSchemaConstants.M_OID_AT );
        this.OBJECTCLASS_OID = oidRegistry.getOid( SystemSchemaConstants.OBJECT_CLASS_AT );
        this.META_SYNTAX_OID = oidRegistry.getOid( MetaSchemaConstants.M_SYNTAX_AT );
    }
    
    
    public Map<String,Schema> getSchemas() throws NamingException
    {
        Map<String,Schema> schemas = new HashMap<String,Schema>();
        NamingEnumeration list = listSchemas();
        while( list.hasMore() )
        {
            SearchResult sr = ( SearchResult ) list.next();
            Schema schema = factory.getSchema( sr.getAttributes() ); 
            schemas.put( schema.getSchemaName(), schema );
        }
        
        return schemas;
    }

    
    public Set<String> getSchemaNames() throws NamingException
    {
        Set<String> schemaNames = new HashSet<String>();
        NamingEnumeration list = listSchemas();
        while( list.hasMore() )
        {
            SearchResult sr = ( SearchResult ) list.next();
            schemaNames.add( ( String ) sr.getAttributes().get( "cn" ).get() );
        }
        
        return schemaNames;
    }
    
    
    private NamingEnumeration listSchemas() throws NamingException
    {
        LdapDN base = new LdapDN( "ou=schema" );
        base.normalize( attrRegistry.getNormalizerMapping() );
        ExprNode filter = new SimpleNode( oidRegistry.getOid( "objectClass" ), "metaSchema", AssertionEnum.EQUALITY );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        return partition.search( base, new HashMap(), filter, searchControls );
    }


    public Schema getSchema( String schemaName ) throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=" + schemaName + ",ou=schema" );
        dn.normalize( attrRegistry.getNormalizerMapping() );
        return factory.getSchema( partition.lookup( dn ) );
    }


    public Schema getSchema( String schemaName, Properties schemaProperties ) throws NamingException
    {
        return getSchema( schemaName ); 
    }
    
    
    public boolean hasMatchingRule( String oid ) throws NamingException
    {
        BranchNode filter = new BranchNode( AssertionEnum.AND );
        filter.addNode( new SimpleNode( OBJECTCLASS_OID, 
            MetaSchemaConstants.META_MATCHING_RULE_OC, AssertionEnum.EQUALITY ) );

        if ( NUMERIC_OID_CHECKER.isValidSyntax( oid ) )
        {
            filter.addNode( new SimpleNode( M_OID_OID, oid, AssertionEnum.EQUALITY ) );
        }
        else
        {
            filter.addNode( new SimpleNode( M_NAME_OID, oid.toLowerCase(), AssertionEnum.EQUALITY ) );
        }
        
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> ne = null;

        try
        {
            ne = partition.search( partition.getSuffix(), new HashMap(), filter, searchControls );
            
            if ( ! ne.hasMore() )
            {
                return false;
            }
            
            if ( ne.hasMore() )
            {
                throw new NamingException( "Got more than one matchingRule for oid of " + oid );
            }

            return true;
        }
        finally
        {
            ne.close();
        }
    }
    
    
    /**
     * Given the non-normalized name (alias) or the OID for a schema entity.  This 
     * method finds the schema under which that entity is located. 
     * 
     * NOTE: this method presumes that all alias names across schemas are unique.  
     * This should be the case for LDAP but this can potentially be violated so 
     * we should make sure this is a unique name.
     * 
     * @param entityName one of the names of the entity or it's numeric id
     * @return the name of the schema that contains that entity or null if no entity with 
     * that alias name exists
     * @throws NamingException if more than one entity has the name, or if there 
     * are underlying data access problems
     */
    public String findSchema( String entityName ) throws NamingException
    {
        LdapDN dn = findDn( entityName );
        if ( dn == null )
        {
            return null;
        }
        
        Rdn rdn = dn.getRdn( 1 );
        if ( ! rdn.getType().equalsIgnoreCase( CN_OID ) )
        {
            throw new NamingException( "Attribute of second rdn in dn '" + dn.toNormName() 
                + "' expected to be CN oid of " + CN_OID + " but was " + rdn.getType() );
        }
        
        return ( String ) rdn.getValue();
    }

    
    public LdapDN findDn( String entityName ) throws NamingException
    {
        SearchResult sr = find( entityName );
        LdapDN dn = new LdapDN( sr.getName() );
        dn.normalize( attrRegistry.getNormalizerMapping() );
        return dn;
    }
    

    /**
     * Given the non-normalized name (alias) or the OID for a schema entity.  This 
     * method finds the entry of the schema entity. 
     * 
     * NOTE: this method presumes that all alias names across schemas are unique.  
     * This should be the case for LDAP but this can potentially be violated so 
     * we should make sure this is a unique name.
     * 
     * @param entityName one of the names of the entity or it's numeric id
     * @return the search result for the entity or null if no such entity exists with 
     * that alias or numeric oid
     * @throws NamingException if more than one entity has the name, or if there 
     * are underlying data access problems
     */
    public SearchResult find( String entityName ) throws NamingException
    {
        BranchNode filter = new BranchNode( AssertionEnum.OR );
        SimpleNode nameAVA = new SimpleNode( M_NAME_OID, entityName.toLowerCase(), AssertionEnum.EQUALITY );
        SimpleNode oidAVA = new SimpleNode( M_OID_OID, entityName.toLowerCase(), AssertionEnum.EQUALITY );
        filter.addNode( nameAVA );
        filter.addNode( oidAVA );
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> ne = null;
        
        try
        {
            ne = partition.search( partition.getSuffix(), new HashMap(), filter, searchControls );
            
            if ( ! ne.hasMore() )
            {
                return null;
            }
            
            SearchResult sr = ne.next();
            if ( ne.hasMore() )
            {
                throw new NamingException( "Got more than one result for the entity name: " + entityName );
            }

            return sr;
        }
        finally
        {
            if ( ne != null )
            {
                ne.close();
            }
        }
    }


    public void enableSchema( String schemaName ) throws NamingException
    {
        LdapDN dn = new LdapDN( "cn=" + schemaName + ",ou=schema" );
        dn.normalize( attrRegistry.getNormalizerMapping() );
        Attributes entry = partition.lookup( dn );
        Attribute disabledAttr = ServerUtils.getAttribute( disabledAttributeType, entry );
        ModificationItemImpl[] mods = new ModificationItemImpl[1];
        
        if ( disabledAttr == null )
        {
            log.warn( "Does not make sense: you're trying to enable {} schema which is already enabled", schemaName );
            return;
        }
        
        boolean isDisabled = ( ( String ) disabledAttr.get() ).equalsIgnoreCase( "TRUE" );
        if ( ! isDisabled )
        {
            log.warn( "Does not make sense: you're trying to enable {} schema which is already enabled", schemaName );
            return;
        }
        
        mods[0] = new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, 
            new AttributeImpl( MetaSchemaConstants.M_DISABLED_AT ) );
        
        partition.modify( dn, mods );
    }


    /**
     * Returns the set of matchingRules and attributeTypes which depend on the 
     * provided syntax.
     *
     * @param numericOid the numeric identifier for the entity
     * @return
     */
    public Set<SearchResult> listSyntaxDependies( String numericOid ) throws NamingException
    {
        Set<SearchResult> set = new HashSet<SearchResult>( );
        BranchNode filter = new BranchNode( AssertionEnum.AND );
        
        // subfilter for (| (objectClass=metaMatchingRule) (objectClass=metaAttributeType))  
        BranchNode or = new BranchNode( AssertionEnum.OR );
        or.addNode( new SimpleNode( OBJECTCLASS_OID, 
            MetaSchemaConstants.META_MATCHING_RULE_OC.toLowerCase(), AssertionEnum.EQUALITY ) );
        or.addNode( new SimpleNode( OBJECTCLASS_OID, 
            MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC.toLowerCase(), AssertionEnum.EQUALITY ) );
        
        filter.addNode( or );
        filter.addNode( new SimpleNode( META_SYNTAX_OID, 
            numericOid.toLowerCase(), AssertionEnum.EQUALITY ) );

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> ne = null;
        
        try
        {
            ne = partition.search( partition.getSuffix(), new HashMap(), filter, searchControls );
            while( ne.hasMore() )
            {
                set.add( ne.next() );
            }
        }
        finally
        {
            if ( ne != null )
            {
                ne.close();
            }
        }
        
        return set;
    }
}
