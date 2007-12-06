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


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.*;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.ImmutableAttributesWrapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaService
{
    public static final String SCHEMA_SUBENTRY_DN = "cn=schema";
    public static final String SCHEMA_SUBENTRY_DN_NORMALIZED = "2.5.4.3=schema";
    public static final String SCHEMA_AREA_DN = "ou=schema";
    public static final String SCHEMA_AREA_DN_NORMALIZED = "2.5.4.11=schema";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String SCHEMA_TIMESTAMP_ENTRY_DN = "cn=schemaModifications,ou=schema";


    /** cached version of the schema subentry with all attributes in it */
    private Attributes schemaSubentry;
    private final Object lock = new Object();

    /** a handle on the registries */
    private Registries registries;

    /** a handle on the schema partition */
    private JdbmPartition schemaPartition;

    /** schema operation control */
    private SchemaOperationControl schemaControl;

    /**
     * the normalized name for the schema modification attributes
     */
    private LdapDN schemaModificationAttributesDN;



    public SchemaService( Registries registries, JdbmPartition schemaPartition, SchemaOperationControl schemaControl ) throws NamingException
    {
        this.registries = registries;
        this.schemaPartition = schemaPartition;
        this.schemaControl = schemaControl;

        schemaModificationAttributesDN = new LdapDN( SCHEMA_TIMESTAMP_ENTRY_DN );
        schemaModificationAttributesDN.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
    }


    public boolean isSchemaSubentry( String dnString ) throws NamingException
    {
        if ( dnString.equalsIgnoreCase( SCHEMA_SUBENTRY_DN ) ||
             dnString.equalsIgnoreCase( SCHEMA_SUBENTRY_DN_NORMALIZED ) )
        {
            return true;
        }

        LdapDN dn = new LdapDN( dnString ).normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        return dn.getNormName().equals( SCHEMA_SUBENTRY_DN_NORMALIZED );
    }


    public Registries getRegistries()
    {
        return registries;
    }


    private Attribute generateComparators()
    {
        Attribute attr = new AttributeImpl( SchemaConstants.COMPARATORS_AT );
        Iterator<ComparatorDescription> list = registries.getComparatorRegistry().comparatorDescriptionIterator();
        while ( list.hasNext() )
        {
            ComparatorDescription description = list.next();
            attr.add( SchemaUtils.render( description ).toString() );
        }

        return attr;
    }


    private Attribute generateNormalizers()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.NORMALIZERS_AT );
        Iterator<NormalizerDescription> list = registries.getNormalizerRegistry().normalizerDescriptionIterator();
        while ( list.hasNext() )
        {
            NormalizerDescription normalizer = list.next();
            attr.add( SchemaUtils.render( normalizer ).toString() );
        }
        return attr;
    }


    private Attribute generateSyntaxCheckers()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.SYNTAX_CHECKERS_AT );
        Iterator<SyntaxCheckerDescription> list =
            registries.getSyntaxCheckerRegistry().syntaxCheckerDescriptionIterator();

        while ( list.hasNext() )
        {
            SyntaxCheckerDescription syntaxCheckerDescription = list.next();
            attr.add( SchemaUtils.render( syntaxCheckerDescription ).toString() );
        }
        return attr;
    }


    private Attribute generateObjectClasses() throws NamingException
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.OBJECT_CLASSES_AT );
        Iterator<ObjectClass> list = registries.getObjectClassRegistry().iterator();

        while ( list.hasNext() )
        {
            ObjectClass oc = list.next();
            attr.add( SchemaUtils.render( oc ).toString() );
        }
        return attr;
    }


    private Attribute generateAttributeTypes() throws NamingException
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.ATTRIBUTE_TYPES_AT );
        Iterator<AttributeType> list = registries.getAttributeTypeRegistry().iterator();

        while ( list.hasNext() )
        {
            AttributeType at = list.next();
            attr.add( SchemaUtils.render( at ).toString() );
        }
        return attr;
    }


    private Attribute generateMatchingRules() throws NamingException
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.MATCHING_RULES_AT );
        Iterator<MatchingRule> list = registries.getMatchingRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            MatchingRule mr = list.next();
            attr.add( SchemaUtils.render( mr ).toString() );
        }
        return attr;
    }


    private Attribute generateMatchingRuleUses()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.MATCHING_RULE_USE_AT );
        Iterator<MatchingRuleUse> list = registries.getMatchingRuleUseRegistry().iterator();

        while ( list.hasNext() )
        {
            MatchingRuleUse mru = list.next();
            attr.add( SchemaUtils.render( mru ).toString() );
        }
        return attr;
    }


    private Attribute generateSyntaxes()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.LDAP_SYNTAXES_AT );
        Iterator<Syntax> list = registries.getSyntaxRegistry().iterator();

        while ( list.hasNext() )
        {
            Syntax syntax = list.next();
            attr.add( SchemaUtils.render( syntax ).toString() );
        }
        return attr;
    }


    private Attribute generateDitContextRules()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.DIT_CONTENT_RULES_AT );
        Iterator<DITContentRule> list = registries.getDitContentRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            DITContentRule dcr = list.next();
            attr.add( SchemaUtils.render( dcr ).toString() );
        }
        return attr;
    }


    private Attribute generateDitStructureRules()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.DIT_STRUCTURE_RULES_AT );
        Iterator<DITStructureRule> list = registries.getDitStructureRuleRegistry().iterator();

        while ( list.hasNext() )
        {
            DITStructureRule dsr =list.next();
            attr.add( SchemaUtils.render( dsr ).toString() );
        }
        return attr;
    }


    private Attribute generateNameForms()
    {
        Attribute attr;
        attr = new AttributeImpl( SchemaConstants.NAME_FORMS_AT );
        Iterator<NameForm> list = registries.getNameFormRegistry().iterator();

        while ( list.hasNext() )
        {
            NameForm nf = list.next();
            attr.add( SchemaUtils.render( nf ).toString() );
        }
        return attr;
    }


    private void generateSchemaSubentry( Attributes mods ) throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );

        // add the objectClass attribute
        Attribute oc = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        oc.add( SchemaConstants.TOP_OC );
        oc.add( "subschema" );
        oc.add( SchemaConstants.SUBENTRY_OC );
        oc.add( ApacheSchemaConstants.APACHE_SUBSCHEMA_OC );
        attrs.put( oc );

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
        attrs.put( new AttributeImpl( SchemaConstants.SUBTREE_SPECIFICATION_AT, "{}" ) );


        // -------------------------------------------------------------------
        // set standard operational attributes for the subentry
        // -------------------------------------------------------------------

        // Add the createTimestamp
        Attribute attr = new AttributeImpl( SchemaConstants.CREATE_TIMESTAMP_AT );
        AttributeType createTimestampAT = registries.
            getAttributeTypeRegistry().lookup( SchemaConstants.CREATE_TIMESTAMP_AT );
        Attribute createTimestamp = AttributeUtils.getAttribute( mods, createTimestampAT );
        attr.add( createTimestamp.get() );
        attrs.put( attr );

        // Add the creatorsName
        attr = new AttributeImpl( SchemaConstants.CREATORS_NAME_AT );
        attr.add( PartitionNexus.ADMIN_PRINCIPAL );
        attrs.put( attr );

        // Add the modifyTimestamp
        attr = new AttributeImpl( SchemaConstants.MODIFY_TIMESTAMP_AT );
        AttributeType schemaModifyTimestampAT = registries.
            getAttributeTypeRegistry().lookup( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT );
        Attribute schemaModifyTimestamp =
            AttributeUtils.getAttribute( mods, schemaModifyTimestampAT );
        attr.add( schemaModifyTimestamp.get() );
        attrs.put( attr );

        // Add the modifiersName
        attr = new AttributeImpl( SchemaConstants.MODIFIERS_NAME_AT );
        AttributeType schemaModifiersNameAT = registries.
            getAttributeTypeRegistry().lookup( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT );
        Attribute schemaModifiersName =
            AttributeUtils.getAttribute( mods, schemaModifiersNameAT );
        attr.add( schemaModifiersName.get() );
        attrs.put( attr );

        // don't swap out if a request for the subentry is in progress or we
        // can give back an inconsistent schema back to the client so we block
        synchronized ( lock )
        {
            schemaSubentry = attrs;
        }
    }


    private void addAttribute( Attributes attrs, String id ) throws NamingException
    {
        Attribute attr = schemaSubentry.get( id );

        if ( attr != null )
        {
            attrs.put( attr );
        }
    }


    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws NamingException if there is a failure to access schema timestamps
     */
    public Attributes getSubschemaEntryImmutable() throws NamingException
    {
        if ( schemaSubentry == null )
        {
            generateSchemaSubentry( schemaPartition.lookup(
                    new LookupOperationContext( schemaModificationAttributesDN ) ) );
        }

        return new ImmutableAttributesWrapper( schemaSubentry );
    }


    /**
     * A seriously unsafe (unsynchronized) means to access the schemaSubentry.
     *
     * @return the schemaSubentry
     * @throws NamingException if there is a failure to access schema timestamps
     */
    public Attributes getSubschemaEntryCloned() throws NamingException
    {
        if ( schemaSubentry == null )
        {
            generateSchemaSubentry( schemaPartition.lookup(
                    new LookupOperationContext( schemaModificationAttributesDN ) ) );
        }

        return ( Attributes ) schemaSubentry.clone();
    }


    /**
     * Gets the schemaSubentry based on specific search id parameters which
     * include the special '*' and '+' operators.
     *
     * @param ids the ids of the attributes that should be returned from a search
     * @return the subschema entry with the ids provided
     * @throws NamingException if there are failures during schema info access
     */
    public Attributes getSubschemaEntry( String[] ids ) throws NamingException
    {
        if ( ids == null )
        {
            ids = EMPTY_STRING_ARRAY;
        }

        Set<String> setOids = new HashSet<String>();
        AttributesImpl attrs = new AttributesImpl();
        boolean returnAllOperationalAttributes = false;

        synchronized( lock )
        {
            // ---------------------------------------------------------------
            // Check if we need an update by looking at timestamps on disk
            // ---------------------------------------------------------------

            Attributes mods = schemaPartition.lookup( new LookupOperationContext( schemaModificationAttributesDN ) );
            Attribute modifyTimeDisk = mods.get( SchemaConstants.MODIFY_TIMESTAMP_AT );
            Attribute modifyTimeMemory = schemaSubentry.get( SchemaConstants.MODIFY_TIMESTAMP_AT );

            if ( modifyTimeDisk == null && modifyTimeMemory == null )
            {
                // do nothing!
            }
            else if ( modifyTimeDisk != null && modifyTimeMemory != null )
            {
                Date disk = DateUtils.getDate( ( String ) modifyTimeDisk.get() );
                Date mem = DateUtils.getDate( ( String ) modifyTimeMemory.get() );
                if ( disk.after( mem ) )
                {
                    generateSchemaSubentry( mods );
                }
            }
            else
            {
                generateSchemaSubentry( mods );
            }


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
                    setOids.add( registries.getOidRegistry().getOid( id ) );
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


    SchemaOperationControl getSchemaControl()
    {
        return schemaControl;
    }
}
