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
package org.apache.directory.server.core.schema;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
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
import org.apache.directory.shared.ldap.schema.registries.NormalizerRegistry;


/**
 * This class manage the Schema's operations. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultSchemaService implements SchemaService
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** cached version of the schema subentry with all attributes in it */
    private Entry schemaSubentry;
    private final Object lock = new Object();

    /** a handle on the schema partition */
    private SchemaPartition schemaPartition;

    /** the normalized name for the schema modification attributes */
    private Dn schemaModificationAttributesDn;
    
    /** A lock to avid concurrent generation of the SubschemaSubentry */
    private Object schemaSubentrLock = new Object();

    
    public DefaultSchemaService() throws Exception
    {
        schemaPartition = new SchemaPartition();
    }
    
    

    /**
     * {@inheritDoc}
     */
    public boolean isSchemaSubentry( Dn dn ) throws LdapException
    {
        return dn.getNormName().equals( ServerDNConstants.CN_SCHEMA_DN_NORMALIZED );
    }


    public final SchemaManager getSchemaManager()
    {
        return schemaPartition.getSchemaManager();
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#getSchemaPartition()
     */
    public SchemaPartition getSchemaPartition()
    {
        return schemaPartition;
    }
    
    
    /**
     * Generate the comparators attribute from the registry
     */
    private EntryAttribute generateComparators() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.COMPARATORS_AT ) );

        for ( LdapComparator<?> comparator : getSchemaManager().getComparatorRegistry() )
        {
            attr.add( SchemaUtils.render( comparator ) );
        }

        return attr;
    }


    private EntryAttribute generateNormalizers() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.NORMALIZERS_AT ) );

        NormalizerRegistry nr = getSchemaManager().getNormalizerRegistry();
        
        for ( Normalizer normalizer : nr )
        {
            attr.add( SchemaUtils.render( normalizer ) );
        }

        return attr;
    }


    private EntryAttribute generateSyntaxCheckers() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.SYNTAX_CHECKERS_AT ) );

        for ( SyntaxChecker syntaxChecker : getSchemaManager().getSyntaxCheckerRegistry() )
        {
            attr.add( SchemaUtils.render( syntaxChecker ) );
        }
        
        return attr;
    }


    private EntryAttribute generateObjectClasses() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute(
            getSchemaManager().getAttributeType( SchemaConstants.OBJECT_CLASSES_AT ) );

        for ( ObjectClass objectClass : getSchemaManager().getObjectClassRegistry() )
        {
            attr.add( SchemaUtils.render( objectClass ).toString() );
        }

        return attr;
    }


    private EntryAttribute generateAttributeTypes() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.ATTRIBUTE_TYPES_AT ) );

        for ( AttributeType attributeType : getSchemaManager().getAttributeTypeRegistry() )
        {
            attr.add( SchemaUtils.render( attributeType ).toString() );
        }

        return attr;
    }


    private EntryAttribute generateMatchingRules() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.MATCHING_RULES_AT ) );

        for ( MatchingRule matchingRule : getSchemaManager().getMatchingRuleRegistry() )
        {
            attr.add( SchemaUtils.render( matchingRule ).toString() );
        }

        return attr;
    }


    private EntryAttribute generateMatchingRuleUses() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute(
            getSchemaManager().getAttributeType( SchemaConstants.MATCHING_RULE_USE_AT ) );

        for ( MatchingRuleUse matchingRuleUse : getSchemaManager().getMatchingRuleUseRegistry() )
        {
            attr.add( SchemaUtils.render( matchingRuleUse ).toString() );
        }

        return attr;
    }


    private EntryAttribute generateSyntaxes() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.LDAP_SYNTAXES_AT ) );

        for ( LdapSyntax syntax : getSchemaManager().getLdapSyntaxRegistry() )
        {
            attr.add( SchemaUtils.render( syntax ).toString() );
        }

        return attr;
    }


    private EntryAttribute generateDitContextRules() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.DIT_CONTENT_RULES_AT ) );

        for ( DITContentRule ditContentRule : getSchemaManager().getDITContentRuleRegistry() )
        {
            attr.add( SchemaUtils.render( ditContentRule ).toString() );
        }
        
        return attr;
    }


    private EntryAttribute generateDitStructureRules() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.DIT_STRUCTURE_RULES_AT ) );

        for ( DITStructureRule ditStructureRule : getSchemaManager().getDITStructureRuleRegistry() )
        {
            attr.add( SchemaUtils.render( ditStructureRule ).toString() );
        }
        
        return attr;
    }


    private EntryAttribute generateNameForms() throws LdapException
    {
        EntryAttribute attr = new DefaultEntryAttribute( 
            getSchemaManager().getAttributeType( SchemaConstants.NAME_FORMS_AT ) );

        for ( NameForm nameForm : getSchemaManager().getNameFormRegistry() )
        {
            attr.add( SchemaUtils.render( nameForm ).toString() );
        }
        
        return attr;
    }


    /**
     * Creates the SSSE by extracting all the SchemaObjects from the registries.
     */
    private void generateSchemaSubentry( Entry mods ) throws LdapException
    {
        Entry attrs = new DefaultEntry( getSchemaManager(), mods.getDn() );

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
        attrs.put( generateComparators() );
        attrs.put( generateNormalizers() );
        attrs.put( generateSyntaxCheckers() );
        attrs.put( generateObjectClasses() );
        attrs.put( generateAttributeTypes() );
        attrs.put( generateMatchingRules() );
        attrs.put( generateMatchingRuleUses() );
        attrs.put( generateSyntaxes() );
        attrs.put( generateDitContextRules() );
        attrs.put( generateDitStructureRules() );
        attrs.put( generateNameForms() );
        attrs.put( SchemaConstants.SUBTREE_SPECIFICATION_AT, "{}" );

        // -------------------------------------------------------------------
        // set standard operational attributes for the subentry
        // -------------------------------------------------------------------

        // Add the createTimestamp
        EntryAttribute createTimestamp = mods.get( SchemaConstants.CREATE_TIMESTAMP_AT );
        attrs.put( SchemaConstants.CREATE_TIMESTAMP_AT, createTimestamp.get() );

        // Add the creatorsName
        attrs.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );

        // Add the modifyTimestamp
        EntryAttribute schemaModifyTimestamp = mods.get( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT );
        attrs.put( SchemaConstants.MODIFY_TIMESTAMP_AT, schemaModifyTimestamp.get() );

        // Add the modifiersName
        EntryAttribute schemaModifiersName = mods.get( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );
        attrs.put( SchemaConstants.MODIFIERS_NAME_AT, schemaModifiersName.get() );

        // don't swap out if a request for the subentry is in progress or we
        // can give back an inconsistent schema back to the client so we block
        synchronized ( lock )
        {
            schemaSubentry = attrs;
        }
    }


    private void addAttribute( Entry attrs, String id ) throws LdapException
    {
        EntryAttribute attr = schemaSubentry.get( id );

        if ( attr != null )
        {
            attrs.put( attr );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry getSubschemaEntryImmutable() throws LdapException
    {
        synchronized ( schemaSubentrLock )
        {
            if ( schemaSubentry == null )
            {
                generateSchemaSubentry( schemaPartition.lookup(
                        new LookupOperationContext( null, schemaModificationAttributesDn) ) );
            }
    
            return ( Entry ) schemaSubentry.clone();
        }
    }
    
    
    /**
     * Initializes the SchemaService
     *
     * @throws Exception If the initializaion fails
     */
    public void initialize() throws LdapException
    {
        try
        {
            schemaModificationAttributesDn = new Dn( ServerDNConstants.SCHEMA_MODIFICATIONS_DN, getSchemaManager() );
        }
        catch ( LdapException e )
        {
            throw new RuntimeException( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#getSubschemaEntryCloned()
     */
    public Entry getSubschemaEntryCloned() throws LdapException
    {
        if ( schemaSubentry == null )
        {
            generateSchemaSubentry( schemaPartition.lookup(
                    new LookupOperationContext( null, schemaModificationAttributesDn) ) );
        }

        return ( Entry ) schemaSubentry.clone();
    }


    /**
     * {@inheritDoc}
     */
    public Entry getSubschemaEntry( String[] ids ) throws LdapException
    {
        if ( ids == null )
        {
            ids = EMPTY_STRING_ARRAY;
        }

        Set<String> setOids = new HashSet<String>();
        Entry attrs = new DefaultEntry( getSchemaManager(), Dn.EMPTY_DN );
        boolean returnAllOperationalAttributes = false;

        synchronized( lock )
        {
            // ---------------------------------------------------------------
            // Check if we need an update by looking at timestamps on disk
            // ---------------------------------------------------------------

            Entry mods = 
                schemaPartition.lookup( 
                    new LookupOperationContext( null, schemaModificationAttributesDn) );
            
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
                generateSchemaSubentry( mods );
//            }

            // ---------------------------------------------------------------
            // Prep Work: Transform the attributes to their OID counterpart
            // ---------------------------------------------------------------

            for ( String id:ids )
            {
                // Check whether the set contains a plus, and use it below to include all
                // operational attributes.  Due to RFC 3673, and issue DIREVE-228 in JIRA
                if ( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES.equals( id ) )
                {
                    returnAllOperationalAttributes = true;
                }
                else if ( SchemaConstants.ALL_USER_ATTRIBUTES.equals(  id ) )
                {
                    setOids.add( id );
                }
                else
                {
                    setOids.add( getSchemaManager().getAttributeTypeRegistry().getOidByName( id ) );
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
