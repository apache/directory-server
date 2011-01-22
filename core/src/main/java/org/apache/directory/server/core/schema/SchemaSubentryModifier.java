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

import org.apache.directory.server.core.DNFactory;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.util.Base64;


/**
 * Responsible for translating modify operations on the subschemaSubentry into 
 * operations against entries within the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaSubentryModifier
{
    private static final Collection<String> BYPASS;
    
    static
    {
        Set<String> c = new HashSet<String>();
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        BYPASS = Collections.unmodifiableCollection( c );
    }
    
    private AttributesFactory factory = new AttributesFactory();

    /** The server schemaManager */
    private SchemaManager schemaManager;

    /** The DN factory */
    private DNFactory dnFactory;


    /**
     * 
     * Creates a new instance of SchemaSubentryModifier.
     *
     * @param schemaManager The server schemaManager
     * @param dnFactory The DN factory
     */
    public SchemaSubentryModifier( SchemaManager schemaManager, DNFactory dnFactory )
    {
        this.schemaManager = schemaManager;
        this.dnFactory = dnFactory;
    }
    
    
    private DN getDn( SchemaObject obj ) throws LdapInvalidDnException
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
        return dnFactory.create( buf.toString() );
    }
    

    public void add( OperationContext opContext, LdapComparatorDescription comparatorDescription ) throws LdapException
    {
        String schemaName = getSchema( comparatorDescription );   
        DN dn = dnFactory.create( 
            "m-oid=" + comparatorDescription.getOid(),
            SchemaConstants.COMPARATORS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );
        
        Entry entry = getEntry( dn, comparatorDescription );

        opContext.add( (Entry)entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, NormalizerDescription normalizerDescription ) throws LdapException
    {
        String schemaName = getSchema( normalizerDescription );
        DN dn = dnFactory.create( 
            "m-oid=" + normalizerDescription.getOid(),
            SchemaConstants.NORMALIZERS_PATH , 
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );
        
        Entry entry = getEntry( dn, normalizerDescription );

        opContext.add( (Entry)entry, BYPASS );
    }
    
    
    public void add( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws LdapException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        DN dn = dnFactory.create( 
            "m-oid=" + syntaxCheckerDescription.getOid(),
            SchemaConstants.SYNTAX_CHECKERS_PATH,
            "cn=" + schemaName, 
            SchemaConstants.OU_SCHEMA );
        
        Entry entry = getEntry( dn, syntaxCheckerDescription );
        opContext.add( (Entry)entry, BYPASS );
    }
    
    
    public void addSchemaObject( OperationContext opContext, SchemaObject obj ) throws LdapException
    {
        Schema schema = schemaManager.getLoadedSchema( obj.getSchemaName() );
        DN dn = getDn( obj );
        Entry entry = factory.getAttributes( obj, schema, schemaManager );
        entry.setDn( dn );

        opContext.add( entry, BYPASS );
    }


    public void deleteSchemaObject( OperationContext opContext, SchemaObject obj ) throws LdapException
    {
        DN dn = getDn( obj );
        opContext.delete( dn, BYPASS );
    }

    
    public void delete( OperationContext opContext, NormalizerDescription normalizerDescription ) throws LdapException
    {
        String schemaName = getSchema( normalizerDescription );
        DN dn = dnFactory.create( 
            "m-oid=" + normalizerDescription.getOid(),
            SchemaConstants.NORMALIZERS_PATH,
            "cn=" + schemaName, 
            SchemaConstants.OU_SCHEMA );
        
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, SyntaxCheckerDescription syntaxCheckerDescription ) throws LdapException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        DN dn = dnFactory.create( 
            "m-oid=" + syntaxCheckerDescription.getOid(), 
            SchemaConstants.SYNTAX_CHECKERS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );
        opContext.delete( dn, BYPASS );
    }


    public void delete( OperationContext opContext, LdapComparatorDescription comparatorDescription ) throws LdapException
    {
        String schemaName = getSchema( comparatorDescription );
        DN dn = dnFactory.create( 
            "m-oid=" + comparatorDescription.getOid(),
            SchemaConstants.COMPARATORS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );
        
        opContext.delete( dn, BYPASS );
    }


    private Entry getEntry( DN dn, LdapComparatorDescription comparatorDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );
        
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


    private Entry getEntry( DN dn, NormalizerDescription normalizerDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );

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
    
    
    private Entry getEntry( DN dn, SyntaxCheckerDescription syntaxCheckerDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );
        
        entry.put( SchemaConstants.OBJECT_CLASS_AT, 
            SchemaConstants.TOP_OC, 
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_SYNTAX_CHECKER_OC );

        entry.put( MetaSchemaConstants.M_OID_AT, syntaxCheckerDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, syntaxCheckerDescription.getFqcn() );

        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT, 
                Base64.decode(syntaxCheckerDescription.getBytecode().toCharArray()) );
        }
        
        if ( syntaxCheckerDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, syntaxCheckerDescription.getDescription() );
        }
        
        return entry;
    }
}
