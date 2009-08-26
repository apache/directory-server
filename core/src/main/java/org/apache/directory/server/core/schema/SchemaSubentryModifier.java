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


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.server.utils.AttributesFactory;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.Base64;


/**
 * Responsible for translating modify operations on the subschemaSubentry into 
 * operations against entries within the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaSubentryModifier
{
    private static final Collection<String> BYPASS;
    
    static
    {
        Set<String> c = new HashSet<String>();
//        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
//        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
//        c.add( SubentryInterceptor.class.getName() );
//        c.add( CollectiveAttributeInterceptor.class.getName() );
//        c.add( EventInterceptor.class.getName() );
//        c.add( TriggerInterceptor.class.getName() );
        BYPASS = Collections.unmodifiableCollection( c );
    }
    
    private AttributesFactory factory = new AttributesFactory();
    private final SchemaPartitionDao dao;
    
    /** The server registries */
    private Registries registries; 

    
    /**
     * 
     * Creates a new instance of SchemaSubentryModifier.
     *
     * @param registries The server registries
     * @param dao
     */
    public SchemaSubentryModifier( Registries registries, SchemaPartitionDao dao )
    {
        this.registries = registries;
        this.dao = dao;
    }
    
    
    private LdapDN getDn( SchemaObject obj ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "m-oid=" ).append( obj.getOid() ).append( ",ou=" );

        if ( obj instanceof LdapSyntax )
        {
            buf.append( "syntaxes" );
        }
        else if ( obj instanceof MatchingRule )
        {
            buf.append( SchemaConstants.MATCHING_RULES_AT );
        }
        else if ( obj instanceof AttributeType )
        {
            buf.append( SchemaConstants.ATTRIBUTE_TYPES_AT );
        }
        else if ( obj instanceof ObjectClass )
        {
            buf.append( SchemaConstants.OBJECT_CLASSES_AT );
        }
        else if ( obj instanceof MatchingRuleUse )
        {
            buf.append( SchemaConstants.MATCHING_RULE_USE_AT );
        }
        else if ( obj instanceof DITStructureRule )
        {
            buf.append( SchemaConstants.DIT_STRUCTURE_RULES_AT );
        }
        else if ( obj instanceof DITContentRule )
        {
            buf.append( SchemaConstants.DIT_CONTENT_RULES_AT );
        }
        else if ( obj instanceof NameForm )
        {
            buf.append( SchemaConstants.NAME_FORMS_AT );
        }

        buf.append( ",cn=" ).append( obj.getSchemaName() ).append( ",ou=schema" );
        return new LdapDN( buf.toString() );
    }
    

    public void add( OperationContext opContext, LdapComparatorDescription comparatorDescription ) throws Exception
    {
        String schemaName = getSchema( comparatorDescription );   
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        Entry entry = getEntry( dn, comparatorDescription );

        opContext.add( (ServerEntry)entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, NormalizerDescription normalizerDescription ) throws Exception
    {
        String schemaName = getSchema( normalizerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        Entry entry = getEntry( dn, normalizerDescription );

        opContext.add( (ServerEntry)entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws Exception
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        Entry entry = getEntry( dn, syntaxCheckerDescription );
        opContext.add( (ServerEntry)entry, BYPASS );
    }
    
    
    public void addSchemaObject( OperationContext opContext, SchemaObject obj ) throws Exception
    {
        Schema schema = dao.getSchema( obj.getSchemaName() );
        LdapDN dn = getDn( obj );
        ServerEntry entry = factory.getAttributes( obj, schema, 
            opContext.getSession().getDirectoryService().getRegistries() );
        entry.setDn( dn );

        opContext.add( entry, BYPASS );
    }


    public void deleteSchemaObject( OperationContext opContext, SchemaObject obj ) throws Exception
    {
        LdapDN dn = getDn( obj );
        opContext.delete( dn, BYPASS );
    }

    
    public void delete( OperationContext opContext, NormalizerDescription normalizerDescription ) throws Exception
    {
        String schemaName = getSchema( normalizerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws Exception
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, LdapComparatorDescription comparatorDescription ) throws Exception
    {
        String schemaName = getSchema( comparatorDescription );
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    private Entry getEntry( LdapDN dn, LdapComparatorDescription comparatorDescription )
    {
        Entry entry = new DefaultServerEntry( registries, dn );
        
        entry.put( SchemaConstants.OBJECT_CLASS_AT, 
                    SchemaConstants.TOP_OC, 
                    MetaSchemaConstants.META_TOP_OC,
                    MetaSchemaConstants.META_COMPARATOR_OC );
        
        entry.put( MetaSchemaConstants.M_OID_AT, comparatorDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, comparatorDescription.getFqcn() );

        if ( comparatorDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( comparatorDescription.getBytecode().toCharArray() ) );
        }
        
        if ( comparatorDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, comparatorDescription.getDescription() );
        }
        
        return entry;
    }


    private Entry getEntry( LdapDN dn, NormalizerDescription normalizerDescription )
    {
        Entry entry = new DefaultServerEntry( registries, dn );

        entry.put( SchemaConstants.OBJECT_CLASS_AT, 
            SchemaConstants.TOP_OC, 
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_NORMALIZER_OC );
        
        entry.put( MetaSchemaConstants.M_OID_AT, normalizerDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, normalizerDescription.getFqcn() );

        if ( normalizerDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( normalizerDescription.getBytecode().toCharArray() ) );
        }
        
        if ( normalizerDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, normalizerDescription.getDescription() );
        }
        
        return entry;
    }


    private String getSchema( SchemaObject desc ) 
    {
        if ( desc.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA ) )
        {
            return desc.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
        }
        
        return MetaSchemaConstants.SCHEMA_OTHER;
    }
    
    
    private Entry getEntry( LdapDN dn, SyntaxCheckerDescription syntaxCheckerDescription )
    {
        Entry entry = new DefaultServerEntry( registries, dn );
        
        entry.put( SchemaConstants.OBJECT_CLASS_AT, 
            SchemaConstants.TOP_OC, 
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_SYNTAX_CHECKER_OC );

        entry.put( MetaSchemaConstants.M_OID_AT, syntaxCheckerDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, syntaxCheckerDescription.getFqcn() );

        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( syntaxCheckerDescription.getBytecode().toCharArray() ) );
        }
        
        if ( syntaxCheckerDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, syntaxCheckerDescription.getDescription() );
        }
        
        return entry;
    }
}
