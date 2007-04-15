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
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
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
        Set<String> bypass = new HashSet<String>();
        bypass.add( "authenticationService" );
        bypass.add( "referralService" );
        bypass.add( "authorizationService" );
        bypass.add( "defaultAuthorizationService" );
        bypass.add( "exceptionService" );
        bypass.add( "schemaService" );
        BYPASS = Collections.unmodifiableCollection( bypass );
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
    

    public void addSchemaObject( SchemaObject obj ) throws NamingException
    {
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        Schema schema = dao.getSchema( obj.getSchema() );
        LdapDN dn = getDn( obj );
        Attributes attrs = factory.getAttributes( obj, schema );
        proxy.add( new AddOperationContext( dn, attrs ), BYPASS );
    }


    public void deleteSchemaObject( SchemaObject obj ) throws NamingException
    {
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = getDn( obj );
        proxy.delete( new DeleteOperationContext( dn ), BYPASS );
    }

    
    public void delete( NormalizerDescription normalizerDescription ) throws NamingException
    {
        String schemaName = getSchema( normalizerDescription );
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getNumericOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        proxy.delete( new DeleteOperationContext( dn ), BYPASS );
    }


    public void delete( SyntaxCheckerDescription syntaxCheckerDescription ) throws NamingException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getNumericOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        proxy.delete( new DeleteOperationContext( dn ), BYPASS );
    }


    public void delete( ComparatorDescription comparatorDescription ) throws NamingException
    {
        String schemaName = getSchema( comparatorDescription );
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getNumericOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        proxy.delete( new DeleteOperationContext( dn ), BYPASS );
    }


    public void add( ComparatorDescription comparatorDescription ) throws NamingException
    {
        String schemaName = getSchema( comparatorDescription );   
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + comparatorDescription.getNumericOid() + ",ou=comparators,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( comparatorDescription );
        proxy.add( new AddOperationContext( dn, attrs ), BYPASS );
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


    public void add( NormalizerDescription normalizerDescription ) throws NamingException
    {
        String schemaName = getSchema( normalizerDescription );
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + normalizerDescription.getNumericOid() + ",ou=normalizers,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( normalizerDescription );
        proxy.add( new AddOperationContext( dn, attrs ), BYPASS );
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


    public void add( SyntaxCheckerDescription syntaxCheckerDescription ) throws NamingException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        PartitionNexusProxy proxy = InvocationStack.getInstance().peek().getProxy();
        LdapDN dn = new LdapDN( "m-oid=" + syntaxCheckerDescription.getNumericOid() + ",ou=syntaxCheckers,cn=" 
            + schemaName + ",ou=schema" );
        Attributes attrs = getAttributes( syntaxCheckerDescription );
        proxy.add( new AddOperationContext( dn, attrs ), BYPASS );
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
