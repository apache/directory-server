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
package org.apache.directory.server.core.bootstrap.plugin; 


import java.util.Comparator; 

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.SystemSchemaConstants;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;


/**
 * A factory that generates an entry using the meta schema for schema 
 * elements.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributesFactory
{
    public Attributes getAttributes( Schema schema )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_SCHEMA_OC );
        entry.put( SystemSchemaConstants.CN_AT, schema.getSchemaName() );
        entry.put( MetaSchemaConstants.M_OWNER_AT, schema.getOwner() );
        
        if ( schema.isDisabled() )
        {
            entry.put( MetaSchemaConstants.M_DISABLED_AT, "TRUE" );
        }
        
        String[] dependencies = schema.getDependencies();
        if ( dependencies != null && dependencies.length > 0 )
        {
            Attribute attr = new AttributeImpl( MetaSchemaConstants.M_DEPENDENCIES_AT );
            for ( int ii = 0; ii < dependencies.length; ii++ )
            {
                attr.add( dependencies[ii] );
            }
            entry.put( attr );
        }
        
        return entry;
    }
    
    
    public Attributes getAttributes( SyntaxChecker syntaxChecker )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_SYNTAX_CHECKER_OC );
        entry.put( MetaSchemaConstants.M_OID_AT, syntaxChecker.getSyntaxOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, syntaxChecker.getClass().getName() );
        return entry;
    }

    
    public Attributes getAttributes( Syntax syntax )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_SYNTAX_OC );
        entry.put( MetaSchemaConstants.X_HUMAN_READIBLE_AT, getBoolean( syntax.isHumanReadible() ) );
        injectCommon( syntax, entry );
        return entry;
    }

    
    public Attributes getAttributes( String oid, Normalizer normalizer )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_NORMALIZER_OC );
        entry.put( MetaSchemaConstants.M_OID_AT, oid );
        entry.put( MetaSchemaConstants.M_FQCN_AT, normalizer.getClass().getName() );
        return entry;
    }

    
    public Attributes getAttributes( String oid, Comparator comparator )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_COMPARATOR_OC );
        entry.put( MetaSchemaConstants.M_OID_AT, oid );
        entry.put( MetaSchemaConstants.M_FQCN_AT, comparator.getClass().getName() );
        return entry;
    }


    /**
     * 
     * @param matchingRule
     * @return
     * @throws NamingException
     */
    public Attributes getAttributes( MatchingRule matchingRule ) throws NamingException
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_MATCHING_RULE_OC );
        entry.put( MetaSchemaConstants.M_SYNTAX_AT, matchingRule.getSyntax().getOid() );
        injectCommon( matchingRule, entry );
        return entry;
    }

    
    public Attributes getAttributes( MatchingRuleUse matchingRuleUse )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "" );
        return entry;
    }

    
    public Attributes getAttributes( DITStructureRule dITStructureRule )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "" );
        return entry;
    }

    
    public Attributes getAttributes( DITContentRule dITContentRule )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "" );
        return entry;
    }

    
    public Attributes getAttributes( NameForm nameForm )
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( "" );
        return entry;
    }


    /**
     * <pre>
     *    objectclass ( 1.3.6.1.4.1.18060.0.4.0.3.3
     *       NAME 'metaAttributeType'
     *       DESC 'meta definition of the AttributeType object'
     *       SUP metaTop
     *       STRUCTURAL
     *       MUST ( m-name $ m-syntax )
     *       MAY ( m-supAttributeType $ m-obsolete $ m-equality $ m-ordering $ 
     *             m-substr $ m-singleValue $ m-collective $ m-noUserModification $ 
     *             m-usage $ m-extensionAttributeType )
     *    )
     * </pre>
     * 
     * @param attributeType
     * @return
     * @throws NamingException
     */
    public Attributes getAttributes( AttributeType attributeType ) throws NamingException
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_ATTRIBUTE_TYPE_OC );
        entry.put( MetaSchemaConstants.M_SYNTAX_AT, attributeType.getSyntax().getOid() );
        entry.put( MetaSchemaConstants.M_COLLECTIVE_AT, getBoolean( attributeType.isCollective() ) );
        entry.put( MetaSchemaConstants.M_NO_USER_MODIFICATION_AT, getBoolean( ! attributeType.isCanUserModify() ) );
        entry.put( MetaSchemaConstants.M_SINGLE_VALUE_AT, getBoolean( attributeType.isSingleValue() ) );
        entry.put( MetaSchemaConstants.M_USAGE_AT, attributeType.getUsage().toString() );

        injectCommon( attributeType, entry );
        
        AttributeType superior = attributeType.getSuperior();
        if ( superior != null )
        {
            // use name if we can for clarity
            String sup = superior.getName();
            if ( sup == null )
            {
                sup = superior.getOid();
            }
            entry.put( MetaSchemaConstants.M_SUP_ATTRIBUTE_TYPE_AT, sup );
        }
        
        if ( attributeType.getEquality() != null )
        {
            String equality = attributeType.getEquality().getName();
            
            if ( equality == null )
            {
                equality = attributeType.getEquality().getOid();
            }
            
            entry.put( MetaSchemaConstants.M_EQUALITY_AT, equality );
        }

        if ( attributeType.getSubstr() != null )
        {
            String substr = attributeType.getSubstr().getName();
            
            if ( substr == null )
            {
                substr = attributeType.getSubstr().getOid();
            }
            
            entry.put( MetaSchemaConstants.M_SUBSTR_AT, substr );
        }

        if ( attributeType.getOrdering() != null )
        {
            String ordering = attributeType.getOrdering().getName();
            
            if ( ordering == null )
            {
                ordering = attributeType.getOrdering().getOid();
            }
            
            entry.put( MetaSchemaConstants.M_ORDERING_AT, ordering );
        }

        return entry;
    }

    
    /**
     * Creates the attributes of an entry representing an objectClass.
     * 
     * <pre>
     *  objectclass ( 1.3.6.1.4.1.18060.0.4.0.3.2
     *      NAME 'metaObjectclass'
     *      DESC 'meta definition of the objectclass object'
     *      SUP metaTop
     *      STRUCTURAL
     *      MUST m-oid
     *      MAY ( m-name $ m-obsolete $ m-supObjectClass $ m-typeObjectClass $ m-must $ 
     *            m-may $ m-extensionObjectClass )
     *  )
     * </pre>
     * 
     * @param objectClass the objectClass to produce a meta schema entry for
     * @return the attributes of the metaSchema entry representing the objectClass
     * @throws NamingException if there are any problems
     */
    public Attributes getAttributes( ObjectClass objectClass ) throws NamingException
    {
        Attributes entry = new AttributesImpl( SystemSchemaConstants.OBJECT_CLASS_AT, "top", true );
        entry.get( SystemSchemaConstants.OBJECT_CLASS_AT ).add( MetaSchemaConstants.META_OBJECT_CLASS_OC );
        entry.put( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT, objectClass.getType().toString() );
        
        injectCommon( objectClass, entry );

        // handle the superior objectClasses 
        if ( objectClass.getSuperClasses() != null && objectClass.getSuperClasses().length != 0 )
        {
            Attribute attr = new AttributeImpl( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT );
            ObjectClass[] superclasses = objectClass.getSuperClasses();
            for ( int ii = 0; ii < superclasses.length; ii++ )
            {
                attr.add( getNameOrNumericoid( superclasses[ii] ) ); 
            }
            entry.put( attr );
        }

        // add the must list
        if ( objectClass.getMustList() != null && objectClass.getMustList().length != 0 )
        {
            Attribute attr = new AttributeImpl( MetaSchemaConstants.M_MUST_AT );
            AttributeType[] mustList = objectClass.getMustList();
            for ( int ii = 0; ii < mustList.length; ii++ )
            {
                attr.add( getNameOrNumericoid( mustList[ii] ) );
            }
            entry.put( attr );
        }
        
        // add the may list
        if ( objectClass.getMayList() != null && objectClass.getMayList().length != 0 )
        {
            Attribute attr = new AttributeImpl( MetaSchemaConstants.M_MAY_AT );
            AttributeType[] mayList = objectClass.getMayList();
            for ( int ii = 0; ii < mayList.length; ii++ )
            {
                attr.add( getNameOrNumericoid( mayList[ii] ) );
            }
            entry.put( attr );
        }
        
        return entry;
    }

    
    private final String getNameOrNumericoid( SchemaObject object )
    {
        // first try to use userfriendly name if we can
        if ( object.getName() != null )
        {
            return object.getName();
        }
        
        return object.getOid();
    }
    
    
    private final void injectCommon( SchemaObject object, Attributes entry )
    {
        injectNames( object.getNames(), entry );
        entry.put( MetaSchemaConstants.M_OBSOLETE_AT, getBoolean( object.isObsolete() ) );
        entry.put( MetaSchemaConstants.M_OID_AT, object.getOid() );
        
        if ( object.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, object.getDescription() );
        }
    }
    
    
    private final void injectNames( String[] names, Attributes entry )
    {
        if ( names == null || names.length == 0 )
        {
            return;
        }
        
        Attribute attr = new AttributeImpl( MetaSchemaConstants.M_NAME_AT );
        for ( int ii = 0; ii < names.length; ii++ )
        {
            attr.add( names[ii] );
        }
        entry.put( attr );
    }

    
    private final String getBoolean( boolean value )
    {
        if ( value ) 
        {
            return "TRUE";
        }
        else
        {
            return "FALSE";
        }
    }
}
