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


import java.text.ParseException;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.MutableSchemaObject;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.DITContentRuleDescription;
import org.apache.directory.shared.ldap.schema.syntax.DITStructureRuleDescription;
import org.apache.directory.shared.ldap.schema.syntax.LdapSyntaxDescription;
import org.apache.directory.shared.ldap.schema.syntax.MatchingRuleDescription;
import org.apache.directory.shared.ldap.schema.syntax.MatchingRuleUseDescription;
import org.apache.directory.shared.ldap.schema.syntax.NameFormDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.ComparatorDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.DITContentRuleDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.DITStructureRuleDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.LdapSyntaxDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.MatchingRuleDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.MatchingRuleUseDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.NameFormDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.NormalizerDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.ObjectClassDescriptionSchemaParser;
import org.apache.directory.shared.ldap.schema.syntax.parser.SyntaxCheckerDescriptionSchemaParser;


/**
 * Parses descriptions using a number of different parsers for schema descriptions.
 * Also checks to make sure some things are valid as it's parsing paramters of
 * certain entity types.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DescriptionParsers
{
    private static final String OTHER_SCHEMA = "other";
    private static final String[] EMPTY = new String[0];
    private static final Integer[] EMPTY_INT_ARRAY = new Integer[0];

    private static final ComparatorDescription[] EMPTY_COMPARATORS = new ComparatorDescription[0];
    private static final NormalizerDescription[] EMPTY_NORMALIZERS = new NormalizerDescription[0];
    private static final SyntaxCheckerDescription[] EMPTY_SYNTAX_CHECKERS = new SyntaxCheckerDescription[0];
    private static final Syntax[] EMPTY_SYNTAXES = new Syntax[0];
    private static final MatchingRule[] EMPTY_MATCHING_RULES = new MatchingRule[0];
    private static final AttributeType[] EMPTY_ATTRIBUTE_TYPES = new AttributeType[0];
    private static final ObjectClass[] EMPTY_OBJECT_CLASSES = new ObjectClass[0];
    private static final MatchingRuleUse[] EMPTY_MATCHING_RULE_USES = new MatchingRuleUse[0];
    private static final DITStructureRule[] EMPTY_DIT_STRUCTURE_RULES = new DITStructureRule[0];
    private static final DITContentRule[] EMPTY_DIT_CONTENT_RULES = new DITContentRule[0];
    private static final NameForm[] EMPTY_NAME_FORMS = new NameForm[0];

    private final Registries globalRegistries;
    
    private final ComparatorDescriptionSchemaParser comparatorParser =
        new ComparatorDescriptionSchemaParser();
    private final NormalizerDescriptionSchemaParser normalizerParser =
        new NormalizerDescriptionSchemaParser();
    private final SyntaxCheckerDescriptionSchemaParser syntaxCheckerParser =
        new SyntaxCheckerDescriptionSchemaParser();
    private final LdapSyntaxDescriptionSchemaParser syntaxParser =
        new LdapSyntaxDescriptionSchemaParser();
    private final MatchingRuleDescriptionSchemaParser matchingRuleParser =
        new MatchingRuleDescriptionSchemaParser();
    private final AttributeTypeDescriptionSchemaParser attributeTypeParser = 
        new AttributeTypeDescriptionSchemaParser();
    private final ObjectClassDescriptionSchemaParser objectClassParser = 
        new ObjectClassDescriptionSchemaParser();
    private final MatchingRuleUseDescriptionSchemaParser matchingRuleUseParser = 
        new MatchingRuleUseDescriptionSchemaParser();
    private final DITStructureRuleDescriptionSchemaParser ditStructureRuleParser =
        new DITStructureRuleDescriptionSchemaParser();
    private final DITContentRuleDescriptionSchemaParser ditContentRuleParser =
        new DITContentRuleDescriptionSchemaParser();
    private final NameFormDescriptionSchemaParser nameFormParser =
        new NameFormDescriptionSchemaParser();
    
    private final SchemaPartitionDao dao;
    
    
    /**
     * Creates a description parser.
     * 
     * @param globalRegistries the registries to use while creating new schema entities
     */
    public DescriptionParsers( Registries globalRegistries, SchemaPartitionDao dao )
    {
        this.globalRegistries = globalRegistries;
        this.dao = dao;
    }

    
    public SyntaxCheckerDescription[] parseSyntaxCheckers( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_SYNTAX_CHECKERS;
        }
        
        SyntaxCheckerDescription[] syntaxCheckerDescriptions = new SyntaxCheckerDescription[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            try
            {
                syntaxCheckerDescriptions[ii] = 
                    syntaxCheckerParser.parseSyntaxCheckerDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the syntaxCheckerDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
        }
        
        return syntaxCheckerDescriptions;
    }
    
    
    public NormalizerDescription[] parseNormalizers( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_NORMALIZERS;
        }
        
        NormalizerDescription[] normalizerDescriptions = new NormalizerDescription[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            try
            {
                normalizerDescriptions[ii] = normalizerParser.parseNormalizerDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the normalizerDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
        }
        
        return normalizerDescriptions;
    }
    

    public ComparatorDescription[] parseComparators( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_COMPARATORS;
        }
        
        ComparatorDescription[] comparatorDescriptions = new ComparatorDescription[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            try
            {
                comparatorDescriptions[ii] = comparatorParser.parseComparatorDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the comparatorDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
        }
        
        return comparatorDescriptions;
    }
    

    /**
     * Parses a set of attributeTypeDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing attributeTypeDescriptions
     * @return the set of attributeType objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public AttributeType[] parseAttributeTypes( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_ATTRIBUTE_TYPES;
        }
        
        AttributeType[] attributeTypes = new AttributeType[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            AttributeTypeDescription desc = null;
            
            try
            {
                desc = attributeTypeParser.parseAttributeTypeDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the attributeTypeDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }

            if ( desc.getSyntax() != null && ! dao.hasSyntax( desc.getSyntax() ) )
            {
                throw new LdapOperationNotSupportedException(
                    "Cannot permit the addition of an attributeType with an invalid syntax: " + desc.getSyntax(), 
                    ResultCodeEnum.UNWILLING_TO_PERFORM );
            }

            AttributeTypeImpl at = new AttributeTypeImpl( desc.getNumericOid(), globalRegistries );
            at.setCanUserModify( desc.isUserModifiable() );
            at.setCollective( desc.isCollective() );
            at.setEqualityOid( desc.getEqualityMatchingRule() );
            at.setOrderingOid( desc.getOrderingMatchingRule() );
            at.setSingleValue( desc.isSingleValued() );
            at.setSubstrOid( desc.getSubstringsMatchingRule() );
            at.setSuperiorOid( desc.getSuperType() );
            at.setSyntaxOid( desc.getSyntax() );
            at.setUsage( desc.getUsage() );
            
            setSchemaObjectProperties( desc, at );

            attributeTypes[ii] = at;
        }
        
        return attributeTypes;
    }
    
    
    /**
     * Parses a set of objectClassDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing objectClassDescriptions
     * @return the set of objectClass objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public ObjectClass[] parseObjectClasses( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_OBJECT_CLASSES;
        }
        
        ObjectClass[] objectClasses = new ObjectClass[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            ObjectClassDescription desc = null;
            
            try
            {
                desc = objectClassParser.parseObjectClassDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the objectClassDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
         
            ObjectClassImpl oc = new ObjectClassImpl( desc.getNumericOid(), globalRegistries );
            oc.setMayListOids( desc.getMayAttributeTypes().toArray( EMPTY) );
            oc.setMustListOids( desc.getMustAttributeTypes().toArray( EMPTY ) );
            oc.setSuperClassOids( desc.getSuperiorObjectClasses().toArray( EMPTY ) );
            oc.setType( desc.getKind() );
            setSchemaObjectProperties( desc, oc );
            
            objectClasses[ii] = oc;
        }
        
        return objectClasses;
    }


    /**
     * Parses a set of matchingRuleUseDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing matchingRuleUseDescriptions
     * @return the set of matchingRuleUse objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public MatchingRuleUse[] parseMatchingRuleUses( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_MATCHING_RULE_USES;
        }
        
        MatchingRuleUse[] matchingRuleUses = new MatchingRuleUse[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            MatchingRuleUseDescription desc = null;
            
            try
            {
                desc = matchingRuleUseParser.parseMatchingRuleUseDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the matchingRuleUseDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            MatchingRuleUseImpl mru = new MatchingRuleUseImpl( desc.getNumericOid(), globalRegistries );
            mru.setApplicableAttributesOids( desc.getApplicableAttributes().toArray( EMPTY ) );
            setSchemaObjectProperties( desc, mru );
            
            matchingRuleUses[ii] = mru;
        }

        return matchingRuleUses;
    }


    /**
     * Parses a set of ldapSyntaxDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing ldapSyntaxDescriptions
     * @return the set of Syntax objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public Syntax[] parseSyntaxes( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_SYNTAXES;
        }
        
        Syntax[] syntaxes = new Syntax[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            LdapSyntaxDescription desc = null;
            
            try
            {
                desc = syntaxParser.parseLdapSyntaxDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the ldapSyntaxDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            if ( ! dao.hasSyntaxChecker( desc.getNumericOid() ) )
            {
                throw new LdapOperationNotSupportedException(
                    "Cannot permit the addition of a syntax without the prior creation of a " +
                    "\nsyntaxChecker with the same object identifier of the syntax!",
                    ResultCodeEnum.UNWILLING_TO_PERFORM );
            }

            SyntaxImpl syntax = new SyntaxImpl( desc.getNumericOid(), globalRegistries.getSyntaxCheckerRegistry() );
            setSchemaObjectProperties( desc, syntax );
            syntax.setHumanReadible( isHumanReadable( desc ) );
            syntaxes[ii] = syntax;
        }
        
        return syntaxes;
    }


    /**
     * Parses a set of matchingRuleDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing matchingRuleDescriptions
     * @return the set of matchingRule objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public MatchingRule[] parseMatchingRules( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_MATCHING_RULES;
        }
        
        MatchingRule[] matchingRules = new MatchingRule[attr.size()];

        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            MatchingRuleDescription desc = null;

            try
            {
                desc = matchingRuleParser.parseMatchingRuleDescription( ( String ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the matchingRuleDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            if ( ! dao.hasSyntax( desc.getSyntax() )  )
            {
                throw new LdapOperationNotSupportedException(
                    "Cannot create a matchingRule that depends on non-existant syntax: " + desc.getSyntax(),
                    ResultCodeEnum.UNWILLING_TO_PERFORM );
            }
            
            MatchingRuleImpl mr = new MatchingRuleImpl( desc.getNumericOid(), desc.getSyntax(), globalRegistries );
            setSchemaObjectProperties( desc, mr );
            
            matchingRules[ii] = mr;
        }
        
        return matchingRules;
    }
    

    /**
     * Parses a set of dITStructureRuleDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing dITStructureRuleDescriptions
     * @return the set of DITStructureRule objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public DITStructureRule[] parseDitStructureRules( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_DIT_STRUCTURE_RULES;
        }
        
        DITStructureRule[] ditStructureRules = new DITStructureRule[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            DITStructureRuleDescription desc = null;
     
            try
            {
                desc = ditStructureRuleParser.parseDITStructureRuleDescription( ( String ) attr.get( ii  ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the ditStructureRuleDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            DitStructureRuleImpl dsr = new DitStructureRuleImpl( desc.getNumericOid(), 
                desc.getRuleId(), globalRegistries );
            dsr.setSuperClassRuleIds( desc.getSuperRules().toArray( EMPTY_INT_ARRAY ) );
            
            setSchemaObjectProperties( desc, dsr );

            ditStructureRules[ii] = dsr;
        }
        
        return ditStructureRules;
    }

    
    /**
     * Parses a set of dITContentRuleDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing dITContentRuleDescriptions
     * @return the set of DITContentRule objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public DITContentRule[] parseDitContentRules( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_DIT_CONTENT_RULES;
        }
        
        DITContentRule[] ditContentRules = new DITContentRule[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            DITContentRuleDescription desc = null;
     
            try
            {
                desc = ditContentRuleParser.parseDITContentRuleDescription( ( String ) attr.get( ii  ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the ditContentRuleDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            DitContentRuleImpl dcr = new DitContentRuleImpl( desc.getNumericOid(), globalRegistries );
            dcr.setAuxObjectClassOids( desc.getAuxiliaryObjectClasses().toArray( EMPTY ) );
            dcr.setMayNameOids( desc.getMayAttributeTypes().toArray( EMPTY ) );
            dcr.setMustNameOids( desc.getMustAttributeTypes().toArray( EMPTY ) );
            dcr.setNotNameOids( desc.getNotAttributeTypes().toArray( EMPTY ) );
            
            setSchemaObjectProperties( desc, dcr );

            ditContentRules[ii] = dcr;
        }
        
        return ditContentRules;
    }

    
    /**
     * Parses a set of nameFormDescriptions held within an attribute into 
     * schema entities.
     * 
     * @param attr the attribute containing nameFormDescriptions
     * @return the set of NameFormRule objects for the descriptions 
     * @throws NamingException if there are problems parsing the descriptions
     */
    public NameForm[] parseNameForms( Attribute attr ) throws NamingException
    {
        if ( attr == null || attr.size() == 0 )
        {
            return EMPTY_NAME_FORMS;
        }
        
        NameForm[] nameForms = new NameForm[attr.size()];
        
        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            NameFormDescription desc = null;
            
            try
            {
                desc = nameFormParser.parseNameFormDescription( ( String  ) attr.get( ii ) );
            }
            catch ( ParseException e )
            {
                LdapInvalidAttributeValueException iave = new LdapInvalidAttributeValueException( 
                    "The following does not conform to the nameFormDescription syntax: " + attr.get( ii ), 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                iave.setRootCause( e );
                throw iave;
            }
            
            NameFormImpl nf = new NameFormImpl( desc.getNumericOid(), globalRegistries );
            nf.setMayUseOids( desc.getMayAttributeTypes().toArray( EMPTY ) );
            nf.setMustUseOids( desc.getMustAttributeTypes().toArray( EMPTY ) );
            nf.setObjectClassOid( desc.getStructuralObjectClass() );
            
            setSchemaObjectProperties( desc, nf );
            
            nameForms[ii] = nf;
        }
        
        return nameForms;
    }
    
    
    /**
     * Called to populate the common schema object properties using an abstract 
     * description object.
     *   
     * @param desc the source description object to copy properties from
     * @param obj the mutable schema object to copy properites to
     */
    private void setSchemaObjectProperties( AbstractSchemaDescription desc, MutableSchemaObject obj )
    {
        obj.setDescription( desc.getDescription() );
        obj.setSchema( getSchema( desc ) );

        if ( ! ( desc instanceof LdapSyntaxDescription ) )
        {
            obj.setNames( desc.getNames().toArray( EMPTY ) );
            obj.setObsolete( desc.isObsolete() );
        }
    }
    
    
    /**
     * Checks to see if the syntax description is human readable by checking 
     * for the presence of the X-IS-HUMAN_READABLE schema extension.
     * 
     * @param desc the ldapSyntaxDescription 
     * @return true if the syntax is human readable, false otherwise
     */
    private boolean isHumanReadable( LdapSyntaxDescription desc )
    {
        List<String> values = desc.getExtensions().get( MetaSchemaConstants.X_IS_HUMAN_READABLE );
        
        if ( values == null || values.size() == 0 )
        {
            return false;
        }
        else
        {
            String value = values.get( 0 );
            if ( value.equals( "TRUE" ) )
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    
    
    /**
     * Gets the schema name for the schema description by looking up the value 
     * of the X-SCHEMA schema extension of the description. 
     * 
     * @param desc the schema description 
     * @return the schema name for the schema entity
     */
    private String getSchema( AbstractSchemaDescription desc ) 
    {
        List<String> values = desc.getExtensions().get( MetaSchemaConstants.X_SCHEMA );
        
        if ( values == null )
        {
            return OTHER_SCHEMA;
        }
        else 
        {
            return values.get( 0 );
        }
    }
}
