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
import javax.naming.directory.Attributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.utils.AttributesFactory;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;
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

    
    public SchemaSubentryModifier( SchemaPartitionDao dao )
    {
        this.dao = dao;
    }
    
    
    private LdapDN getDn( SchemaObject obj ) throws NamingException
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "m-oid=" ).append( obj.getOid() ).append( ",ou=" );

        if ( obj instanceof Syntax )
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

        buf.append( ",cn=" ).append( obj.getSchema() ).append( ",ou=schema" );
        return new LdapDN( buf.toString() );
    }
    

    public void add( OperationContext opContext, ComparatorDescription comparatorDescription ) throws Exception
    {
        String schemaName = getSchema( comparatorDescription );   
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getNumericOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( comparatorDescription );
        ServerEntry entry = ServerEntryUtils.toServerEntry( attrs, dn, 
            opContext.getSession().getDirectoryService().getRegistries() );

        opContext.add( entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, NormalizerDescription normalizerDescription ) throws Exception
    {
        String schemaName = getSchema( normalizerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getNumericOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( normalizerDescription );
        ServerEntry entry = ServerEntryUtils.toServerEntry( attrs, dn, 
            opContext.getSession().getDirectoryService().getRegistries() );

        opContext.add( entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws Exception
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getNumericOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( syntaxCheckerDescription );
        ServerEntry entry = ServerEntryUtils.toServerEntry( attrs, dn, 
            opContext.getSession().getDirectoryService().getRegistries() );
        opContext.add( entry, BYPASS );
    }
    
    
    public void addSchemaObject( OperationContext opContext, SchemaObject obj ) throws Exception
    {
        Schema schema = dao.getSchema( obj.getSchema() );
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
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getNumericOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws Exception
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getNumericOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, ComparatorDescription comparatorDescription ) throws Exception
    {
        String schemaName = getSchema( comparatorDescription );
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getNumericOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        opContext.delete( dn, BYPASS );
    }


    private Attributes getAttributes( ComparatorDescription comparatorDescription )
    {
        AttributesImpl attributes = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, true );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaTop" );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaComparator" );
        attributes.put( MetaSchemaConstants.M_OID_AT, comparatorDescription.getNumericOid() );
        attributes.put( MetaSchemaConstants.M_FQCN_AT, comparatorDescription.getFqcn() );

        if ( comparatorDescription.getBytecode() != null )
        {
            attributes.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( comparatorDescription.getBytecode().toCharArray() ) );
        }
        
        if ( comparatorDescription.getDescription() != null )
        {
            attributes.put( MetaSchemaConstants.M_DESCRIPTION_AT, comparatorDescription.getDescription() );
        }
        
        return attributes;
    }


    private Attributes getAttributes( NormalizerDescription normalizerDescription )
    {
        AttributesImpl attributes = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, true );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaTop" );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaNormalizer" );
        attributes.put( MetaSchemaConstants.M_OID_AT, normalizerDescription.getNumericOid() );
        attributes.put( MetaSchemaConstants.M_FQCN_AT, normalizerDescription.getFqcn() );

        if ( normalizerDescription.getBytecode() != null )
        {
            attributes.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( normalizerDescription.getBytecode().toCharArray() ) );
        }
        
        if ( normalizerDescription.getDescription() != null )
        {
            attributes.put( MetaSchemaConstants.M_DESCRIPTION_AT, normalizerDescription.getDescription() );
        }
        
        return attributes;
    }


    private String getSchema( AbstractSchemaDescription desc ) 
    {
        if ( desc.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA ) )
        {
            return desc.getExtensions().get( MetaSchemaConstants.X_SCHEMA ).get( 0 );
        }
        
        return MetaSchemaConstants.SCHEMA_OTHER;
    }
    
    
    private Attributes getAttributes( SyntaxCheckerDescription syntaxCheckerDescription )
    {
        AttributesImpl attributes = new AttributesImpl( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, true );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaTop" );
        attributes.get( SchemaConstants.OBJECT_CLASS_AT ).add( "metaSyntaxChecker" );
        attributes.put( MetaSchemaConstants.M_OID_AT, syntaxCheckerDescription.getNumericOid() );
        attributes.put( MetaSchemaConstants.M_FQCN_AT, syntaxCheckerDescription.getFqcn() );

        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            attributes.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode( syntaxCheckerDescription.getBytecode().toCharArray() ) );
        }
        
        if ( syntaxCheckerDescription.getDescription() != null )
        {
            attributes.put( MetaSchemaConstants.M_DESCRIPTION_AT, syntaxCheckerDescription.getDescription() );
        }
        
        return attributes;
    }
}
