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
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * This class manage the Schema's operations. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultSchemaService implements SchemaService
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** cached version of the schema subentry with all attributes in it */
    private ServerEntry schemaSubentry;
    private final Object lock = new Object();

    /** a handle on the schema partition */
    private SchemaPartition schemaPartition;

    /** the normalized name for the schema modification attributes */
    private LdapDN schemaModificationAttributesDN;
    
    /** A lock to avid concurrent generation of the SubschemaSubentry */
    private Object schemaSubentrLock = new Object();

    
    public DefaultSchemaService() throws Exception
    {
        schemaPartition = new SchemaPartition();
    }
    
    

    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#isSchemaSubentry(java.lang.String)
     */
    public boolean isSchemaSubentry( String dnString ) throws NamingException
    {
        if ( ServerDNConstants.CN_SCHEMA_DN.equalsIgnoreCase( dnString ) ||
            ServerDNConstants.CN_SCHEMA_DN_NORMALIZED.equalsIgnoreCase( dnString ) )
        {
            return true;
        }

        LdapDN dn = new LdapDN( dnString ).normalize( schemaPartition.getSchemaManager().getNormalizerMapping() );
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
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#setSchemaPartition(org.apache.directory.server.core.schema.SchemaPartition)
     */
    public void setSchemaPartition( SchemaPartition schemaPartition )
    {
        this.schemaPartition = schemaPartition;
    }


    /**
     * Generate the comparators attribute from the registry
     */
    private ServerAttribute generateComparators() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.COMPARATORS_AT ) );

        Iterator<LdapComparator<?>> list = getSchemaManager().getComparatorRegistry().iterator();
        
        while ( list.hasNext() )
        {
            attr.add( SchemaUtils.render( list.next() ) );
        }

        return attr;
    }


    private ServerAttribute generateNormalizers() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.NORMALIZERS_AT ) );

        Iterator<Normalizer> list = getSchemaManager().getNormalizerRegistry().iterator();

        while ( list.hasNext() )
        {
            attr.add( SchemaUtils.render( list.next() ) );
        }
        
        return attr;
    }


    private ServerAttribute generateSyntaxCheckers() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.SYNTAX_CHECKERS_AT ) );

        Iterator<SyntaxChecker> list = getSchemaManager().getSyntaxCheckerRegistry().iterator();

        while ( list.hasNext() )
        {
            attr.add( SchemaUtils.render( list.next() ) );
        }
        
        return attr;
    }


    private ServerAttribute generateObjectClasses() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASSES_AT ) );

        Iterator<ObjectClass> list = getSchemaManager().getObjectClassRegistry().iterator();

        while ( list.hasNext() )
        {
            ObjectClass oc = list.next();
            attr.add( SchemaUtils.render( oc ).toString() );
        }
        
        return attr;
    }


    private ServerAttribute generateAttributeTypes() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.ATTRIBUTE_TYPES_AT ) );

        Iterator<AttributeType> list = getSchemaManager().getAttributeTypeRegistry().iterator();

        while ( list.hasNext() )
        {
            AttributeType at = list.next();
            attr.add( SchemaUtils.render( at ).toString() );
        }

        return attr;
    }


    private ServerAttribute generateMatchingRules() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.MATCHING_RULES_AT ) );

        Iterator<MatchingRule> list = getSchemaManager().getMatchingRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            MatchingRule mr = list.next();
            attr.add( SchemaUtils.render( mr ).toString() );
        }

        return attr;
    }


    private ServerAttribute generateMatchingRuleUses() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.MATCHING_RULE_USE_AT ) );

        Iterator<MatchingRuleUse> list = getSchemaManager().getMatchingRuleUseRegistry().iterator();

        while ( list.hasNext() )
        {
            MatchingRuleUse mru = list.next();
            attr.add( SchemaUtils.render( mru ).toString() );
        }

        return attr;
    }


    private ServerAttribute generateSyntaxes() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.LDAP_SYNTAXES_AT ) );

        Iterator<LdapSyntax> list = getSchemaManager().getLdapSyntaxRegistry().iterator();

        while ( list.hasNext() )
        {
            LdapSyntax syntax = list.next();
            attr.add( SchemaUtils.render( syntax ).toString() );
        }

        return attr;
    }


    private ServerAttribute generateDitContextRules() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.DIT_CONTENT_RULES_AT ) );

        Iterator<DITContentRule> list = getSchemaManager().getDITContentRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            DITContentRule dcr = list.next();
            attr.add( SchemaUtils.render( dcr ).toString() );
        }
        
        return attr;
    }


    private ServerAttribute generateDitStructureRules() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.DIT_STRUCTURE_RULES_AT ) );

        Iterator<DITStructureRule> list = getSchemaManager().getDITStructureRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            DITStructureRule dsr = list.next();
            attr.add( SchemaUtils.render( dsr ).toString() );
        }
        
        return attr;
    }


    private ServerAttribute generateNameForms() throws NamingException
    {
        ServerAttribute attr = new DefaultServerAttribute( 
            getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.NAME_FORMS_AT ) );

        Iterator<NameForm> list = getSchemaManager().getNameFormRegistry().iterator();

        while ( list.hasNext() )
        {
            NameForm nf = list.next();
            attr.add( SchemaUtils.render( nf ).toString() );
        }
        
        return attr;
    }


    /**
     * Creates the SSSE by extracting all the SchemaObjects from the registries.
     */
    private void generateSchemaSubentry( ServerEntry mods ) throws NamingException
    {
        ServerEntry attrs = new DefaultServerEntry( getSchemaManager(), mods.getDn() );

        // add the objectClass attribute : 'top', 'subschema', 'subentry' and 'apacheSubschema' 
        attrs.put( SchemaConstants.OBJECT_CLASS_AT, 
            SchemaConstants.TOP_OC,
            SchemaConstants.SUBSCHEMA_OC,
            SchemaConstants.SUBENTRY_OC,
            ApacheSchemaConstants.APACHE_SUBSCHEMA_OC
            );

        // add the cn attribute as required for the RDN
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


    private void addAttribute( ServerEntry attrs, String id ) throws NamingException
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
    public ServerEntry getSubschemaEntryImmutable() throws Exception
    {
        synchronized ( schemaSubentrLock )
        {
            if ( schemaSubentry == null )
            {
                generateSchemaSubentry( schemaPartition.lookup(
                        new LookupOperationContext( null, schemaModificationAttributesDN ) ) );
            }
    
            return ( ServerEntry ) schemaSubentry.clone();
        }
    }
    
    
    /**
     * Initializes the SchemaService
     *
     * @throws Exception If the initializaion fails
     */
    public void initialize() throws Exception
    {
        try
        {
            schemaModificationAttributesDN = new LdapDN( ServerDNConstants.SCHEMA_MODIFICATIONS_DN );
            schemaModificationAttributesDN.normalize( 
                getSchemaManager().getNormalizerMapping() );
        }
        catch ( NamingException e )
        {
            throw new RuntimeException( e );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.schema.SchemaService#getSubschemaEntryCloned()
     */
    public ServerEntry getSubschemaEntryCloned() throws Exception
    {
        if ( schemaSubentry == null )
        {
            generateSchemaSubentry( schemaPartition.lookup(
                    new LookupOperationContext( null, schemaModificationAttributesDN ) ) );
        }

        return ( ServerEntry ) schemaSubentry.clone();
    }


    /**
     * {@inheritDoc}
     */
    public ServerEntry getSubschemaEntry( String[] ids ) throws Exception
    {
        if ( ids == null )
        {
            ids = EMPTY_STRING_ARRAY;
        }

        Set<String> setOids = new HashSet<String>();
        ServerEntry attrs = new DefaultServerEntry( getSchemaManager(), LdapDN.EMPTY_LDAPDN );
        boolean returnAllOperationalAttributes = false;

        synchronized( lock )
        {
            // ---------------------------------------------------------------
            // Check if we need an update by looking at timestamps on disk
            // ---------------------------------------------------------------

            ServerEntry mods = 
                schemaPartition.lookup( 
                    new LookupOperationContext( null, schemaModificationAttributesDN ) );
            
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

            // add the cn attribute as required for the RDN
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
