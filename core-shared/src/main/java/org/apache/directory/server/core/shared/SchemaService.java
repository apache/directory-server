/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.shared;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.DITContentRule;
import org.apache.directory.shared.ldap.model.schema.DITStructureRule;
import org.apache.directory.shared.ldap.model.schema.LdapComparator;
import org.apache.directory.shared.ldap.model.schema.LdapSyntax;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.model.schema.NameForm;
import org.apache.directory.shared.ldap.model.schema.Normalizer;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
import org.apache.directory.shared.ldap.model.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.model.schema.registries.NormalizerRegistry;
import org.apache.directory.shared.util.StringConstants;


/**
 * This class manage the Schema's operations. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaService
{
    /** cached version of the schema subentry with all attributes in it */
    private static Entry schemaSubentry;

    /** A lock to avid concurrent generation of the SubschemaSubentry */
    private static Object schemaSubentrLock = new Object();


    /**
     * Generate the comparators attribute from the registry
     */
    private static Attribute generateComparators( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.COMPARATORS_AT ) );

        for ( LdapComparator<?> comparator : schemaManager.getComparatorRegistry() )
        {
            attr.add( SchemaUtils.render( comparator ) );
        }

        return attr;
    }


    private static Attribute generateNormalizers( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.NORMALIZERS_AT ) );

        NormalizerRegistry nr = schemaManager.getNormalizerRegistry();

        for ( Normalizer normalizer : nr )
        {
            attr.add( SchemaUtils.render( normalizer ) );
        }

        return attr;
    }


    private static Attribute generateSyntaxCheckers( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.SYNTAX_CHECKERS_AT ) );

        for ( SyntaxChecker syntaxChecker : schemaManager.getSyntaxCheckerRegistry() )
        {
            attr.add( SchemaUtils.render( syntaxChecker ) );
        }

        return attr;
    }


    private static Attribute generateObjectClasses( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASSES_AT ) );

        for ( ObjectClass objectClass : schemaManager.getObjectClassRegistry() )
        {
            attr.add( SchemaUtils.render( objectClass ).toString() );
        }

        return attr;
    }


    private static Attribute generateAttributeTypes( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT ) );

        for ( AttributeType attributeType : schemaManager.getAttributeTypeRegistry() )
        {
            attr.add( SchemaUtils.render( attributeType ).toString() );
        }

        return attr;
    }


    private static Attribute generateMatchingRules( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.MATCHING_RULES_AT ) );

        for ( MatchingRule matchingRule : schemaManager.getMatchingRuleRegistry() )
        {
            attr.add( SchemaUtils.render( matchingRule ).toString() );
        }

        return attr;
    }


    private static Attribute generateMatchingRuleUses( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.MATCHING_RULE_USE_AT ) );

        for ( MatchingRuleUse matchingRuleUse : schemaManager.getMatchingRuleUseRegistry() )
        {
            attr.add( SchemaUtils.render( matchingRuleUse ).toString() );
        }

        return attr;
    }


    private static Attribute generateSyntaxes( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.LDAP_SYNTAXES_AT ) );

        for ( LdapSyntax syntax : schemaManager.getLdapSyntaxRegistry() )
        {
            attr.add( SchemaUtils.render( syntax ).toString() );
        }

        return attr;
    }


    private static Attribute generateDitContextRules( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.DIT_CONTENT_RULES_AT ) );

        for ( DITContentRule ditContentRule : schemaManager.getDITContentRuleRegistry() )
        {
            attr.add( SchemaUtils.render( ditContentRule ).toString() );
        }

        return attr;
    }


    private static Attribute generateDitStructureRules( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.DIT_STRUCTURE_RULES_AT ) );

        for ( DITStructureRule ditStructureRule : schemaManager.getDITStructureRuleRegistry() )
        {
            attr.add( SchemaUtils.render( ditStructureRule ).toString() );
        }

        return attr;
    }


    private static Attribute generateNameForms( SchemaManager schemaManager ) throws LdapException
    {
        Attribute attr = new DefaultAttribute(
            schemaManager.getAttributeType( SchemaConstants.NAME_FORMS_AT ) );

        for ( NameForm nameForm : schemaManager.getNameFormRegistry() )
        {
            attr.add( SchemaUtils.render( nameForm ).toString() );
        }

        return attr;
    }


    /**
     * Creates the SSSE by extracting all the SchemaObjects from the registries.
     */
    private static void generateSchemaSubentry( SchemaManager schemaManager, Entry mods ) throws LdapException
    {
        Entry attrs = new DefaultEntry( schemaManager, mods.getDn() );

        // add the objectClass attribute : 'top', 'subschema', 'subentry' and 'apacheSubschema' 
        attrs.put( SchemaConstants.OBJECT_CLASS_AT,
            SchemaConstants.TOP_OC,
            SchemaConstants.SUBSCHEMA_OC,
            SchemaConstants.SUBENTRY_OC,
            ApacheSchemaConstants.APACHE_SUBSCHEMA_OC
            );

        // add the cn attribute as required for the Rdn
        attrs.put( SchemaConstants.CN_AT, "schema" );

        // generate all the other operational attributes
        attrs.put( generateComparators( schemaManager ) );
        attrs.put( generateNormalizers( schemaManager ) );
        attrs.put( generateSyntaxCheckers( schemaManager ) );
        attrs.put( generateObjectClasses( schemaManager ) );
        attrs.put( generateAttributeTypes( schemaManager ) );
        attrs.put( generateMatchingRules( schemaManager ) );
        attrs.put( generateMatchingRuleUses( schemaManager ) );
        attrs.put( generateSyntaxes( schemaManager ) );
        attrs.put( generateDitContextRules( schemaManager ) );
        attrs.put( generateDitStructureRules( schemaManager ) );
        attrs.put( generateNameForms( schemaManager ) );
        attrs.put( SchemaConstants.SUBTREE_SPECIFICATION_AT, "{}" );

        // -------------------------------------------------------------------
        // set standard operational attributes for the subentry
        // -------------------------------------------------------------------

        // Add the createTimestamp
        Attribute createTimestamp = mods.get( SchemaConstants.CREATE_TIMESTAMP_AT );
        attrs.put( SchemaConstants.CREATE_TIMESTAMP_AT, createTimestamp.get() );

        // Add the creatorsName
        attrs.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );

        // Add the modifyTimestamp
        Attribute schemaModifyTimestamp = mods.get( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT );
        attrs.put( SchemaConstants.MODIFY_TIMESTAMP_AT, schemaModifyTimestamp.get() );

        // Add the modifiersName
        Attribute schemaModifiersName = mods.get( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );
        attrs.put( SchemaConstants.MODIFIERS_NAME_AT, schemaModifiersName.get() );

        // don't swap out if a request for the subentry is in progress or we
        // can give back an inconsistent schema back to the client so we block
        synchronized ( schemaSubentrLock )
        {
            schemaSubentry = attrs;
        }
    }


    private static void addAttribute( Entry attrs, String id ) throws LdapException
    {
        Attribute attr = schemaSubentry.get( id );

        if ( attr != null )
        {
            attrs.put( attr );
        }
    }


    /**
     * {@inheritDoc}
     */
    public static Entry getSubschemaEntryImmutable( DirectoryService directoryService ) throws LdapException
    {
        synchronized ( schemaSubentrLock )
        {
            if ( schemaSubentry == null )
            {
                Dn schemaModificationAttributesDn = new Dn( directoryService.getSchemaManager(),
                    SchemaConstants.SCHEMA_MODIFICATIONS_DN );

                generateSchemaSubentry(
                    directoryService.getSchemaManager(),
                    directoryService.getSchemaPartition().lookup(
                        new LookupOperationContext( null, schemaModificationAttributesDn ) ) );
            }

            return ( Entry ) schemaSubentry.clone();
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#getSubschemaEntryCloned()
     */
    public static Entry getSubschemaEntryCloned( DirectoryService directoryService ) throws LdapException
    {
        if ( schemaSubentry == null )
        {
            Dn schemaModificationAttributesDn = new Dn( directoryService.getSchemaManager(),
                SchemaConstants.SCHEMA_MODIFICATIONS_DN );

            generateSchemaSubentry(
                directoryService.getSchemaManager(),
                directoryService.getSchemaPartition().lookup(
                    new LookupOperationContext( null, schemaModificationAttributesDn ) ) );
        }

        return ( Entry ) schemaSubentry.clone();
    }


    /**
     * {@inheritDoc}
     */
    public static Entry getSubschemaEntry( DirectoryService directoryService, String[] ids ) throws LdapException
    {
        if ( ids == null )
        {
            ids = StringConstants.EMPTY_STRINGS;
        }

        SchemaManager schemaManager = directoryService.getSchemaManager();

        Set<String> setOids = new HashSet<String>();
        Entry attrs = new DefaultEntry( schemaManager, Dn.ROOT_DSE );
        boolean returnAllOperationalAttributes = false;

        synchronized ( schemaSubentrLock )
        {
            // ---------------------------------------------------------------
            // Check if we need an update by looking at timestamps on disk
            // ---------------------------------------------------------------
            Dn schemaModificationAttributesDn = new Dn( directoryService.getSchemaManager(),
                SchemaConstants.SCHEMA_MODIFICATIONS_DN );

            Entry mods =
                directoryService.getSchemaPartition().lookup(
                    new LookupOperationContext( null, schemaModificationAttributesDn,
                        SchemaConstants.ALL_ATTRIBUTES_ARRAY ) );

            // @todo enable this optimization at some point but for now it
            // is causing some problems so I will just turn it off
            //          Attribute modifyTimeDisk = mods.get( SchemaConstants.MODIFY_TIMESTAMP_AT );
            //
            //          Attribute modifyTimeMemory = null;
            //
            //            if ( schemaSubentry != null )
            //            {
            //                modifyTimeMemory = schemaSubentry.get( SchemaConstants.MODIFY_TIMESTAMP_AT );
            //                if ( modifyTimeDisk == null && modifyTimeMemory == null )
            //                {
            //                    // do nothing!
            //                }
            //                else if ( modifyTimeDisk != null && modifyTimeMemory != null )
            //                {
            //                    Date disk = DateUtils.getDate( ( String ) modifyTimeDisk.get() );
            //                    Date mem = DateUtils.getDate( ( String ) modifyTimeMemory.get() );
            //                    if ( disk.after( mem ) )
            //                    {
            //                        generateSchemaSubentry( mods );
            //                    }
            //                }
            //                else
            //                {
            //                    generateSchemaSubentry( mods );
            //                }
            //            }
            //            else
            //            {
            generateSchemaSubentry( schemaManager, mods );
            //            }

            // ---------------------------------------------------------------
            // Prep Work: Transform the attributes to their OID counterpart
            // ---------------------------------------------------------------

            for ( String id : ids )
            {
                // Check whether the set contains a plus, and use it below to include all
                // operational attributes.  Due to RFC 3673, and issue DIREVE-228 in JIRA
                if ( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES.equals( id ) )
                {
                    returnAllOperationalAttributes = true;
                }
                else if ( SchemaConstants.ALL_USER_ATTRIBUTES.equals( id ) )
                {
                    setOids.add( id );
                }
                else
                {
                    setOids.add( schemaManager.getAttributeTypeRegistry().getOidByName( id ) );
                }
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.COMPARATORS_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.COMPARATORS_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.NORMALIZERS_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.NORMALIZERS_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.SYNTAX_CHECKERS_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.SYNTAX_CHECKERS_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.OBJECT_CLASSES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.OBJECT_CLASSES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.ATTRIBUTE_TYPES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.ATTRIBUTE_TYPES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.MATCHING_RULES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.MATCHING_RULES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.MATCHING_RULE_USE_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.MATCHING_RULE_USE_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.LDAP_SYNTAXES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.LDAP_SYNTAXES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.DIT_CONTENT_RULES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.DIT_CONTENT_RULES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.DIT_STRUCTURE_RULES_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.DIT_STRUCTURE_RULES_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.NAME_FORMS_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.NAME_FORMS_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.SUBTREE_SPECIFICATION_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.SUBTREE_SPECIFICATION_AT );
            }

            int minSetSize = 0;

            if ( setOids.contains( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                minSetSize++;
            }

            if ( setOids.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) )
            {
                minSetSize++;
            }

            if ( setOids.contains( SchemaConstants.REF_AT_OID ) )
            {
                minSetSize++;
            }

            // add the objectClass attribute
            if ( setOids.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) ||
                setOids.contains( SchemaConstants.OBJECT_CLASS_AT_OID ) ||
                setOids.size() == minSetSize )
            {
                addAttribute( attrs, SchemaConstants.OBJECT_CLASS_AT );
            }

            // add the cn attribute as required for the Rdn
            if ( setOids.contains( SchemaConstants.ALL_USER_ATTRIBUTES ) ||
                setOids.contains( SchemaConstants.CN_AT_OID ) ||
                setOids.size() == minSetSize )
            {
                addAttribute( attrs, SchemaConstants.CN_AT );
            }

            // -------------------------------------------------------------------
            // set standard operational attributes for the subentry
            // -------------------------------------------------------------------

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.CREATE_TIMESTAMP_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.CREATE_TIMESTAMP_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.CREATORS_NAME_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.CREATORS_NAME_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.MODIFY_TIMESTAMP_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.MODIFY_TIMESTAMP_AT );
            }

            if ( returnAllOperationalAttributes || setOids.contains( SchemaConstants.MODIFIERS_NAME_AT_OID ) )
            {
                addAttribute( attrs, SchemaConstants.MODIFIERS_NAME_AT );
            }
        }

        return attrs;
    }
}
