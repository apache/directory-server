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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.Charsets;
import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAttributeInUseException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ExtensibleNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.filter.UndefinedNode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.Cascade;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.ObjectClassTypeEnum;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.schema.UsageEnum;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.ldap.model.schema.syntaxCheckers.OctetStringSyntaxChecker;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.shared.SchemaService;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link org.apache.directory.server.core.api.interceptor.Interceptor} that manages and enforces schemas.
 *
 * TODO Better interceptor description required.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaInterceptor extends BaseInterceptor
{
    /** The LoggerFactory used by this Interceptor */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaInterceptor.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * the root nexus to all database partitions
     */
    private PartitionNexus nexus;

    private TopFilter topFilter;

    private List<EntryFilter> filters = new ArrayList<>();

    /** The SubschemaSubentry Dn */
    private Dn subschemaSubentryDn;

    /** The schema manager */
    private SchemaSubentryManager schemaSubEntryManager;

    /** the base Dn (normalized) of the schema partition */
    private Dn schemaBaseDn;

    /** A map used to store all the objectClasses superiors */
    private Map<String, List<ObjectClass>> superiors;

    /** A map used to store all the objectClasses may attributes */
    private Map<String, List<AttributeType>> allMay;

    /** A map used to store all the objectClasses must */
    private Map<String, List<AttributeType>> allMust;

    /** A map used to store all the objectClasses allowed attributes (may + must) */
    private Map<String, List<AttributeType>> allowed;


    /**
     * Creates a new instance of a SchemaInterceptor.
     */
    public SchemaInterceptor()
    {
        super( InterceptorEnum.SCHEMA_INTERCEPTOR );
    }


    /**
     * Initialize the Schema Service
     *
     * @param directoryService the directory service core
     * @throws LdapException if there are problems during initialization
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Initializing SchemaInterceptor..." );
        }

        super.init( directoryService );

        nexus = directoryService.getPartitionNexus();
        topFilter = new TopFilter();
        filters.add( topFilter );

        schemaBaseDn = dnFactory.create( SchemaConstants.OU_SCHEMA );

        // stuff for dealing with subentries (garbage for now)
        Value subschemaSubentry = nexus.getRootDseValue( directoryService.getAtProvider().getSubschemaSubentry() );
        subschemaSubentryDn = dnFactory.create( subschemaSubentry.getString() );

        computeSuperiors();

        // Initialize the schema manager
        schemaSubEntryManager = new SchemaSubentryManager( schemaManager, dnFactory );

        if ( IS_DEBUG )
        {
            LOG.debug( "SchemaInterceptor Initialized !" );
        }
    }


    /**
     * Compute the MUST attributes for an objectClass. This method gather all the
     * MUST from all the objectClass and its superors.
     *
     * @param atSeen ???
     * @param objectClass the object class to gather MUST attributes for
     */
    private void computeMustAttributes( ObjectClass objectClass, Set<String> atSeen )
    {
        List<ObjectClass> parents = superiors.get( objectClass.getOid() );

        List<AttributeType> mustList = new ArrayList<>();
        List<AttributeType> allowedList = new ArrayList<>();
        Set<String> mustSeen = new HashSet<>();

        allMust.put( objectClass.getOid(), mustList );
        allowed.put( objectClass.getOid(), allowedList );

        for ( ObjectClass parent : parents )
        {
            List<AttributeType> mustParent = parent.getMustAttributeTypes();

            if ( ( mustParent != null ) && !mustParent.isEmpty() )
            {
                for ( AttributeType attributeType : mustParent )
                {
                    String oid = attributeType.getOid();

                    if ( !mustSeen.contains( oid ) )
                    {
                        mustSeen.add( oid );
                        mustList.add( attributeType );
                        allowedList.add( attributeType );
                        atSeen.add( attributeType.getOid() );
                    }
                }
            }
        }
    }


    /**
     * Compute the MAY attributes for an objectClass. This method gather all the
     * MAY from all the objectClass and its superors.
     *
     * The allowed attributes is also computed, it's the union of MUST and MAY
     *
     * @param atSeen ???
     * @param objectClass the object class to get all the MAY attributes for
     */
    private void computeMayAttributes( ObjectClass objectClass, Set<String> atSeen )
    {
        List<ObjectClass> parents = superiors.get( objectClass.getOid() );

        List<AttributeType> mayList = new ArrayList<>();
        Set<String> maySeen = new HashSet<>();
        List<AttributeType> allowedList = allowed.get( objectClass.getOid() );

        allMay.put( objectClass.getOid(), mayList );

        for ( ObjectClass parent : parents )
        {
            List<AttributeType> mustParent = parent.getMustAttributeTypes();

            if ( ( mustParent != null ) && !mustParent.isEmpty() )
            {
                for ( AttributeType attributeType : mustParent )
                {
                    String oid = attributeType.getOid();

                    if ( !maySeen.contains( oid ) )
                    {
                        maySeen.add( oid );
                        mayList.add( attributeType );

                        if ( !atSeen.contains( oid ) )
                        {
                            allowedList.add( attributeType );
                        }
                    }
                }
            }
        }
    }


    /**
     * Recursively compute all the superiors of an object class. For instance, considering
     * 'inetOrgPerson', it's direct superior is 'organizationalPerson', which direct superior
     * is 'Person', which direct superior is 'top'.
     *
     * As a result, we will gather all of these three ObjectClasses in 'inetOrgPerson' ObjectClasse
     * superiors.
     */
    private void computeOCSuperiors( ObjectClass objectClass, List<ObjectClass> superiors, Set<String> ocSeen )
        throws LdapException
    {
        List<ObjectClass> parents = objectClass.getSuperiors();

        // Loop on all the objectClass superiors
        if ( ( parents != null ) && !parents.isEmpty() )
        {
            for ( ObjectClass parent : parents )
            {
                // Top is not added
                if ( SchemaConstants.TOP_OC.equals( parent.getName() ) )
                {
                    continue;
                }

                // For each one, recurse
                computeOCSuperiors( parent, superiors, ocSeen );

                String oid = parent.getOid();

                if ( !ocSeen.contains( oid ) )
                {
                    superiors.add( parent );
                    ocSeen.add( oid );
                }
            }
        }
    }


    /**
     * Compute the superiors and MUST/MAY attributes for a specific
     * ObjectClass
     */
    private void computeSuperior( ObjectClass objectClass ) throws LdapException
    {
        List<ObjectClass> ocSuperiors = new ArrayList<>();

        superiors.put( objectClass.getOid(), ocSuperiors );

        computeOCSuperiors( objectClass, ocSuperiors, new HashSet<String>() );

        Set<String> atSeen = new HashSet<>();
        computeMustAttributes( objectClass, atSeen );
        computeMayAttributes( objectClass, atSeen );

        superiors.put( objectClass.getName(), ocSuperiors );
    }


    /**
     * Compute all ObjectClasses superiors, MAY and MUST attributes.
     * @throws Exception
     */
    private void computeSuperiors() throws LdapException
    {
        Iterator<ObjectClass> objectClasses = schemaManager.getObjectClassRegistry().iterator();
        superiors = new ConcurrentHashMap<>();
        allMust = new ConcurrentHashMap<>();
        allMay = new ConcurrentHashMap<>();
        allowed = new ConcurrentHashMap<>();

        while ( objectClasses.hasNext() )
        {
            ObjectClass objectClass = objectClasses.next();
            computeSuperior( objectClass );
        }
    }


    private Value convert( AttributeType attributeType, Value value ) throws LdapException
    {
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            if ( !value.isHumanReadable() )
            {
                return new Value( attributeType, new String( value.getBytes(), Charsets.UTF_8 ) );
            }
        }
        else
        {
            return new Value( attributeType, value.getBytes() );
        }

        return null;
    }


    /**
     * Check that the filter values are compatible with the AttributeType. Typically,
     * a HumanReadible filter should have a String value. The substring filter should
     * not be used with binary attributes.
     */
    private void checkFilter( ExprNode filter ) throws LdapException
    {
        if ( filter == null )
        {
            String message = I18n.err( I18n.ERR_49 );
            LOG.error( message );
            throw new LdapException( message );
        }

        if ( ( filter instanceof ObjectClassNode ) || ( filter instanceof UndefinedNode ) )
        {
            // Bypass (ObjectClass=*) and undifined nodes
            return;
        }

        if ( filter.isLeaf() )
        {
            if ( filter instanceof EqualityNode )
            {
                EqualityNode node = ( EqualityNode ) filter;
                Value value = node.getValue();

                Value newValue = convert( node.getAttributeType(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            else if ( filter instanceof GreaterEqNode )
            {
                GreaterEqNode node = ( GreaterEqNode ) filter;
                Value value = node.getValue();

                Value newValue = convert( node.getAttributeType(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }

            }
            else if ( filter instanceof LessEqNode )
            {
                LessEqNode node = ( LessEqNode ) filter;
                Value value = node.getValue();

                Value newValue = convert( node.getAttributeType(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            else if ( filter instanceof ExtensibleNode )
            {
                ExtensibleNode node = ( ExtensibleNode ) filter;

                // Todo : add the needed checks here
            }
            else if ( filter instanceof ApproximateNode )
            {
                ApproximateNode node = ( ApproximateNode ) filter;
                Value value = node.getValue();

                Value newValue = convert( node.getAttributeType(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            // nothing to do for SubstringNode, PresenceNode, AssertionNode, ScopeNode
        }
        else
        {
            // Recursively iterate through all the children.
            for ( ExprNode child : ( ( BranchNode ) filter ).getChildren() )
            {
                checkFilter( child );
            }
        }
    }


    private void getSuperiors( ObjectClass oc, Set<String> ocSeen, List<ObjectClass> result ) throws LdapException
    {
        for ( ObjectClass parent : oc.getSuperiors() )
        {
            // Skip 'top'
            if ( SchemaConstants.TOP_OC.equals( parent.getName() ) )
            {
                continue;
            }

            if ( !ocSeen.contains( parent.getOid() ) )
            {
                ocSeen.add( parent.getOid() );
                result.add( parent );
            }

            // Recurse on the parent
            getSuperiors( parent, ocSeen, result );
        }
    }


    private boolean getObjectClasses( Attribute objectClasses, List<ObjectClass> result ) throws LdapException
    {
        Set<String> ocSeen = new HashSet<>();

        // We must select all the ObjectClasses, except 'top',
        // but including all the inherited ObjectClasses
        boolean hasExtensibleObject = false;

        for ( Value objectClass : objectClasses )
        {
            String objectClassName = objectClass.getString();

            if ( SchemaConstants.TOP_OC.equals( objectClassName ) )
            {
                continue;
            }

            if ( SchemaConstants.EXTENSIBLE_OBJECT_OC.equalsIgnoreCase( objectClassName ) )
            {
                hasExtensibleObject = true;
            }

            ObjectClass oc = schemaManager.lookupObjectClassRegistry( objectClassName );

            // Add all unseen objectClasses to the list, except 'top'
            if ( !ocSeen.contains( oc.getOid() ) )
            {
                ocSeen.add( oc.getOid() );
                result.add( oc );
            }

            // Find all current OC parents
            getSuperiors( oc, ocSeen, result );
        }

        return hasExtensibleObject;
    }


    private Set<String> getAllMust( Attribute objectClasses ) throws LdapException
    {
        Set<String> must = new HashSet<>();

        // Loop on all objectclasses
        for ( Value value : objectClasses )
        {
            String ocName = value.getString();
            ObjectClass oc = schemaManager.lookupObjectClassRegistry( ocName );

            List<AttributeType> types = oc.getMustAttributeTypes();

            // For each objectClass, loop on all MUST attributeTypes, if any
            if ( ( types != null ) && !types.isEmpty() )
            {
                for ( AttributeType type : types )
                {
                    must.add( type.getOid() );
                }
            }
        }

        return must;
    }


    private Set<String> getAllAllowed( Attribute objectClasses, Set<String> must ) throws LdapException
    {
        Set<String> allAllowed = new HashSet<>( must );

        // Add the 'ObjectClass' attribute ID
        allAllowed.add( SchemaConstants.OBJECT_CLASS_AT_OID );

        // Loop on all objectclasses
        for ( Value objectClass : objectClasses )
        {
            String ocName = objectClass.getString();
            ObjectClass oc = schemaManager.lookupObjectClassRegistry( ocName );

            List<AttributeType> types = oc.getMayAttributeTypes();

            // For each objectClass, loop on all MAY attributeTypes, if any
            if ( ( types != null ) && !types.isEmpty() )
            {
                for ( AttributeType type : types )
                {
                    String oid = type.getOid();

                    allAllowed.add( oid );
                }
            }
        }

        return allAllowed;
    }


    /**
     * Given the objectClasses for an entry, this method adds missing ancestors
     * in the hierarchy except for top which it removes.  This is used for this
     * solution to DIREVE-276.  More information about this solution can be found
     * <a href="http://docs.safehaus.org:8080/x/kBE">here</a>.
     *
     * @param objectClassAttr the objectClass attribute to modify
     * @throws Exception if there are problems
     */
    private void alterObjectClasses( Attribute objectClassAttr ) throws LdapException
    {
        Set<String> objectClasses = new HashSet<>();
        Set<String> objectClassesUP = new HashSet<>();

        // Init the objectClass list with 'top'
        objectClasses.add( SchemaConstants.TOP_OC );
        objectClassesUP.add( SchemaConstants.TOP_OC );

        // Construct the new list of ObjectClasses
        for ( Value ocValue : objectClassAttr )
        {
            String ocName = ocValue.getString();

            if ( !ocName.equalsIgnoreCase( SchemaConstants.TOP_OC ) )
            {
                String ocLowerName = Strings.toLowerCaseAscii( ocName );

                ObjectClass objectClass = schemaManager.lookupObjectClassRegistry( ocLowerName );

                if ( !objectClasses.contains( ocLowerName ) )
                {
                    objectClasses.add( ocLowerName );
                    objectClassesUP.add( ocName );
                }

                List<ObjectClass> ocSuperiors = superiors.get( objectClass.getOid() );

                if ( ocSuperiors != null )
                {
                    for ( ObjectClass oc : ocSuperiors )
                    {
                        if ( !objectClasses.contains( Strings.toLowerCaseAscii( oc.getName() ) ) )
                        {
                            objectClasses.add( oc.getName() );
                            objectClassesUP.add( oc.getName() );
                        }
                    }
                }
            }
        }

        // Now, reset the ObjectClass attribute and put the new list into it
        objectClassAttr.clear();

        for ( String attribute : objectClassesUP )
        {
            objectClassAttr.add( attribute );
        }
    }


    /**
     * Create a new attribute using the given values
     */
    private Attribute createNewAttribute( Attribute attribute ) throws LdapException
    {
        AttributeType attributeType = attribute.getAttributeType();

        // Create the new Attribute
        Attribute newAttribute = new DefaultAttribute( attribute.getUpId(), attributeType );

        for ( Value value : attribute )
        {
            newAttribute.add( value );
        }

        return newAttribute;
    }


    /**
     * Modify an entry, applying the given modifications, and check if it's OK
     */
    private void checkModifyEntry( ModifyOperationContext modifyContext ) throws LdapException
    {
        Dn dn = modifyContext.getDn();
        Entry currentEntry = modifyContext.getEntry();
        List<Modification> mods = modifyContext.getModItems();

        // The first step is to check that the modifications are valid :
        // - the ATs are present in the schema
        // - The value is syntaxically correct
        //
        // While doing that, we will apply the modification to a copy of the current entry
        Entry tempEntry = currentEntry.clone();

        // Now, apply each mod one by one
        for ( Modification mod : mods )
        {
            Attribute attribute = mod.getAttribute();
            AttributeType attributeType = attribute.getAttributeType();

            assertAttributeIsModifyable( modifyContext, attributeType );

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    // Check the syntax here
                    Attribute currentAttribute = tempEntry.get( attributeType );

                    // First check if the added Attribute is already present in the entry
                    // If not, we have to create the entry
                    if ( currentAttribute != null )
                    {
                        for ( Value value : attribute )
                        {
                            // At this point, we know that the attribute's syntax is correct
                            // We just have to check that the current attribute does not
                            // contains the value already
                            if ( currentAttribute.contains( value ) )
                            {
                                // This is an error.
                                String msg = I18n.err( I18n.ERR_54, value );
                                LOG.error( msg );
                                throw new LdapAttributeInUseException( msg );
                            }

                            currentAttribute.add( value );
                        }
                    }
                    else
                    {
                        // We don't check if the attribute is not in the MUST or MAY at this
                        // point, as one of the following modification can change the
                        // ObjectClasses.
                        Attribute newAttribute = attribute.clone();

                        // Check that the attribute allows null values if we don'y have any value
                        if ( ( newAttribute.size() == 0 ) && !newAttribute.isValid( attributeType ) )
                        {
                            // This is an error.
                            String msg = I18n.err( I18n.ERR_54, ( Object[] ) null );
                            LOG.error( msg );
                            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
                        }

                        tempEntry.put( newAttribute );
                    }

                    break;

                case REMOVE_ATTRIBUTE:
                    // First check that the removed attribute exists
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        String msg = I18n.err( I18n.ERR_55, attributeType );
                        LOG.error( msg );
                        throw new LdapNoSuchAttributeException( msg );
                    }

                    // We may have to remove the attribute or only some values
                    if ( attribute.size() == 0 )
                    {
                        // No value : we have to remove the entire attribute
                        tempEntry.removeAttributes( attributeType );
                    }
                    else
                    {
                        currentAttribute = tempEntry.get( attributeType );

                        // Now remove all the values
                        for ( Value value : attribute )
                        {
                            // We can only remove existing values.
                            if ( currentAttribute.contains( value ) )
                            {
                                currentAttribute.remove( value );
                            }
                            else
                            {
                                String msg = I18n.err( I18n.ERR_56, attributeType );
                                LOG.error( msg );
                                throw new LdapNoSuchAttributeException( msg );
                            }
                        }

                        // If the current attribute is empty, we have to remove
                        // it from the entry
                        if ( currentAttribute.size() == 0 )
                        {
                            tempEntry.removeAttributes( attributeType );
                        }
                    }

                    break;

                case REPLACE_ATTRIBUTE:
                    // The replaced attribute might not exist, it will then be a Add
                    // If there is no value, then the attribute will be removed
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        if ( attribute.size() == 0 )
                        {
                            // Ignore the modification, as the attributeType does not
                            // exists in the entry
                            break;
                        }
                        else
                        {
                            // Create the new Attribute
                            Attribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }
                    else
                    {
                        if ( attribute.size() == 0 )
                        {
                            // Remove the attribute from the entry
                            tempEntry.removeAttributes( attributeType );
                        }
                        else
                        {
                            // Replace the existing values with the new values
                            // This is done by removing the Attribute
                            tempEntry.removeAttributes( attributeType );

                            // Create the new Attribute
                            Attribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }

                    break;
                    
                case INCREMENT_ATTRIBUTE:
                    // The incremented attribute might not exist
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        throw new IllegalArgumentException( "Increment operation on a non existing attribute"
                            + attributeType );
                    }
                    else if ( !SchemaConstants.INTEGER_SYNTAX.equals( attributeType.getSyntax().getOid() ) )
                    {
                        throw new IllegalArgumentException( "Increment operation on a non integer attribute"
                            + attributeType );
                    }
                    else
                    {
                        Attribute modified = tempEntry.get( attributeType );
                        Value[] newValues = new Value[ modified.size() ];
                        int increment = 1;
                        int i = 0;
                        
                        if ( mod.getAttribute().size() != 0 )
                        {
                            increment = Integer.parseInt( mod.getAttribute().getString() );
                        }
                        
                        for ( Value value : modified )
                        {
                            int intValue = Integer.parseInt( value.getNormalized() );
                            
                            if ( intValue >= Integer.MAX_VALUE - increment )
                            {
                                throw new IllegalArgumentException( "Increment operation overflow for attribute" 
                                    + attributeType );
                            }
                            
                            newValues[i++] = new Value( Integer.toString( intValue + increment ) );
                            modified.remove( value );
                        }
                        
                        modified.add( newValues );
                    }
                    
                    break;

                default:
                    throw new IllegalArgumentException( "Unexpected modify operation " + mod.getOperation() );
            }
        }

        // Ok, we have created the modified entry. We now have to check that it's a valid
        // entry wrt the schema.
        // We have to check that :
        // - the rdn values are present in the entry
        // - the objectClasses inheritence is correct
        // - all the MUST are present
        // - all the attribute are in MUST and MAY, except fo the extensibleObeject OC
        // is present
        // - We haven't removed a part of the Rdn
        check( dn, tempEntry );
    }


    private void assertAttributeIsModifyable( ModifyOperationContext modifyContext, AttributeType attributeType )
        throws LdapNoPermissionException
    {
        if ( attributeType.isUserModifiable() )
        {
            // We don't allow modification of operational attributes
            return;
        }

        if ( modifyContext.isReplEvent() && modifyContext.getSession().isAdministrator() )
        {
            // this is a replication related modification, allow the operation
            return;
        }

        if ( !attributeType.equals( directoryService.getAtProvider().getModifiersName() )
            && !attributeType.equals( directoryService.getAtProvider().getModifyTimestamp() )
            && !attributeType.equals( directoryService.getAtProvider().getEntryCSN() )
            && !PWD_POLICY_STATE_ATTRIBUTE_TYPES.contains( attributeType ) )
        {
            String msg = I18n.err( I18n.ERR_52, attributeType );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
    }


    /**
     * Filters objectClass attribute to inject top when not present.
     */
    private final class TopFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext operationContext, Entry entry ) throws LdapException
        {
            ServerEntryUtils.filterContents( schemaManager, operationContext, entry );

            return true;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "TopFilter";
        }
    }


    /**
     * Check that all the attributes exist in the schema for this entry.
     *
     * We also check the syntaxes
     */
    private void check( Dn dn, Entry entry ) throws LdapException
    {
        // ---------------------------------------------------------------
        // First, make sure all attributes are valid schema defined attributes
        // ---------------------------------------------------------------
        for ( Attribute attribute : entry.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();

            if ( !schemaManager.getAttributeTypeRegistry().contains( attributeType.getName() ) )
            {
                throw new LdapInvalidAttributeTypeException( I18n.err( I18n.ERR_275, attributeType.getName() ) );
            }
        }

        // We will check some elements :
        // 1) the entry must have all the MUST attributes of all its ObjectClass
        // 2) The SingleValued attributes must be SingleValued
        // 3) No attributes should be used if they are not part of MUST and MAY
        // 3-1) Except if the extensibleObject ObjectClass is used
        // 3-2) or if the AttributeType is COLLECTIVE
        // 4) We also check that for H-R attributes, we have a valid String in the values
        Attribute objectClassAttr = entry.get( directoryService.getAtProvider().getObjectClass() );

        // Protect the server against a null objectClassAttr
        // It can be the case if the user forgot to add it to the entry ...
        // In this case, we create an new one, empty
        if ( objectClassAttr == null )
        {
            objectClassAttr = new DefaultAttribute( directoryService.getAtProvider().getObjectClass() );
        }

        List<ObjectClass> ocs = new ArrayList<>();

        alterObjectClasses( objectClassAttr );

        // Now we can process the MUST and MAY attributes
        Set<String> must = getAllMust( objectClassAttr );
        Set<String> allAllowed = getAllAllowed( objectClassAttr, must );

        boolean hasExtensibleObject = getObjectClasses( objectClassAttr, ocs );

        // As we now have all the ObjectClasses updated, we have
        // to check that we don't have conflicting ObjectClasses
        assertObjectClasses( dn, ocs );

        assertRequiredAttributesPresent( dn, entry, must );
        assertNumberOfAttributeValuesValid( entry );

        if ( !hasExtensibleObject )
        {
            assertAllAttributesAllowed( dn, entry, allAllowed );
        }

        // Check the attributes values and transform them to String if necessary
        entry = assertHumanReadable( entry );

        // Now check the syntaxes
        assertSyntaxes( entry );

        assertRdn( dn, entry );
    }


    private void checkOcSuperior( Entry entry ) throws LdapException
    {
        // handle the m-supObjectClass meta attribute
        Attribute supOC = entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT );

        if ( supOC != null )
        {
            ObjectClassTypeEnum ocType = ObjectClassTypeEnum.STRUCTURAL;

            if ( entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ) != null )
            {
                String type = entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ).getString();
                ocType = ObjectClassTypeEnum.getClassType( type );
            }

            // First check that the inheritence scheme is correct.
            // 1) If the ocType is ABSTRACT, it should not have any other SUP not ABSTRACT
            for ( Value sup : supOC )
            {
                try
                {
                    String supName = sup.getString();

                    ObjectClass superior = schemaManager.lookupObjectClassRegistry( supName );

                    switch ( ocType )
                    {
                        case ABSTRACT:
                            if ( !superior.isAbstract() )
                            {
                                String message = I18n.err( I18n.ERR_57 );
                                LOG.error( message );
                                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, message );
                            }

                            break;

                        case AUXILIARY:
                            if ( !superior.isAbstract() && !superior.isAuxiliary() )
                            {
                                String message = I18n.err( I18n.ERR_58 );
                                LOG.error( message );
                                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, message );
                            }

                            break;

                        case STRUCTURAL:
                            break;

                        default:
                            throw new IllegalArgumentException( "Unexpected object class type " + ocType );
                    }
                }
                catch ( LdapException ne )
                {
                    // The superior OC does not exist : this is an error
                    String message = I18n.err( I18n.ERR_59 );
                    LOG.error( message );
                    throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, message );
                }
            }
        }
    }


    /**
     * Check that all the attributes exist in the schema for this entry.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Dn name = addContext.getDn();
        Entry entry = addContext.getEntry();

        check( name, entry );

        // Special checks for the MetaSchema branch
        if ( name.isDescendantOf( schemaBaseDn ) )
        {
            // get the schema name
            String schemaName = getSchemaName( name );

            if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.META_SCHEMA_OC ) )
            {
                next( addContext );

                if ( schemaManager.isSchemaLoaded( schemaName ) )
                {
                    // Update the OC superiors for each added ObjectClass
                    computeSuperiors();
                }
            }
            else if ( entry.contains( directoryService.getAtProvider().getObjectClass(),
                SchemaConstants.META_OBJECT_CLASS_OC ) )
            {
                // This is an ObjectClass addition
                checkOcSuperior( addContext.getEntry() );

                next( addContext );

                // Update the structures now that the schema element has been added
                Schema schema = schemaManager.getLoadedSchema( schemaName );

                if ( ( schema != null ) && schema.isEnabled() )
                {
                    Attribute oidAT = entry.get( MetaSchemaConstants.M_OID_AT );
                    String ocOid = oidAT.getString();

                    ObjectClass addedOC = schemaManager.lookupObjectClassRegistry( ocOid );
                    computeSuperior( addedOC );
                }
            }
            else if ( entry.contains( directoryService.getAtProvider().getObjectClass(),
                SchemaConstants.META_ATTRIBUTE_TYPE_OC ) )
            {
                // This is an AttributeType addition
                next( addContext );
            }
            else
            {
                next( addContext );
            }

        }
        else
        {
            next( addContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", compareContext );
        }

        // Check that the requested AT exists
        // complain if we do not recognize the attribute being compared
        if ( !schemaManager.getAttributeTypeRegistry().contains( compareContext.getOid() ) )
        {
            throw new LdapInvalidAttributeTypeException( I18n.err( I18n.ERR_266, compareContext.getOid() ) );
        }

        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Entry entry = next( lookupContext );

        ServerEntryUtils.filterContents(
            lookupContext.getSession().getDirectoryService().getSchemaManager(),
            lookupContext, entry );

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // A modification on a simple entry will be done in three steps :
        // - get the original entry (it should already been in the context)
        // - apply the modification on it
        // - check that the entry is still correct
        // - add the operational attributes (modifiersName/modifyTimeStamp)
        // - store the modified entry on the backend.
        //
        // A modification done on the schema is a bit different, as there is two more
        // steps
        // - We have to update the registries
        // - We have to modify the ou=schemaModifications entry
        //

        // First, check that the entry is either a subschemaSubentry or a schema element.
        // This is the case if it's a child of cn=schema or ou=schema
        Dn dn = modifyContext.getDn();

        // Gets the stored entry on which the modification must be applied
        if ( dn.equals( subschemaSubentryDn ) )
        {
            LOG.debug( "Modification attempt on schema subentry {}: \n{}", dn, modifyContext );

            // We can get rid of the modifiersName and modifyTimestamp, they are useless.
            List<Modification> mods = modifyContext.getModItems();
            List<Modification> cleanMods = new ArrayList<>();

            for ( Modification mod : mods )
            {
                AttributeType at = ( ( DefaultModification ) mod ).getAttribute().getAttributeType();

                if ( !directoryService.getAtProvider().getModifiersName().equals( at )
                    && !directoryService.getAtProvider().getModifyTimestamp().equals( at )
                    && !directoryService.getAtProvider().getEntryCSN().equals( at ) )
                {
                    cleanMods.add( mod );
                }
            }

            modifyContext.setModItems( cleanMods );

            // Now that the entry has been modified, update the SSSE
            schemaSubEntryManager.modifySchemaSubentry( modifyContext, modifyContext
                .hasRequestControl( Cascade.OID ) );

            return;
        }

        checkModifyEntry( modifyContext );

        next( modifyContext );
    }

    
    private Map<String, List<ModDnAva>> processRdn( Rdn oldRdn, Rdn newRdn, boolean deleteOldRdn )
    {
        Map<String, List<ModDnAva>> listAvas = new HashMap<>();
        
        // Check that the new RDN will not break the entry when added
        for ( Ava ava : newRdn )
        {
            // Three possibilities :
            // - This is a new AT (not present in the entry) : ModDnType.Add
            // - The AT is already present in the previous RDN, and in the entry : ModDnType.Modify
            // - The AT is already present in the entry, but not in the previous RDN : ModDnType.Add
            boolean found = false;

            for ( Ava oldAva : oldRdn )
            {
                if ( oldAva.getAttributeType().equals( ava.getAttributeType() ) )
                {
                    // Same At, check the value
                    if ( !oldAva.getValue().equals( ava.getValue() ) )
                    {
                        List<ModDnAva> modDnAvas = listAvas.get( ava.getAttributeType().getOid() );
                        
                        if ( modDnAvas == null )
                        {
                            modDnAvas = new ArrayList<>();
                            listAvas.put( ava.getAttributeType().getOid(), modDnAvas );
                        }

                        modDnAvas.add( new ModDnAva( ModDnAva.ModDnType.UPDATE_ADD, ava ) );
                        found = true;
                        break;
                    }
                }
            }
            
            if ( !found )
            {
                List<ModDnAva> modDnAvas = listAvas.get( ava.getAttributeType().getOid() );
                
                if ( modDnAvas == null )
                {
                    modDnAvas = new ArrayList<>();
                    listAvas.put( ava.getAttributeType().getOid(), modDnAvas );
                }
                
                modDnAvas.add( new ModDnAva( ModDnAva.ModDnType.ADD, ava ) );
            }
        }
        
        // Now process the oldRdn avas,if the deleteOldRdn flag is set to True
        if ( deleteOldRdn )
        {
            for ( Ava oldAva : oldRdn )
            {
                boolean found = false;

                for ( Ava newAva : newRdn )
                {
                    if ( newAva.getAttributeType().equals( oldAva.getAttributeType() ) )
                    {
                        // Same At, check the value
                        if ( !newAva.getValue().equals( oldAva.getValue() ) )
                        {
                            List<ModDnAva> modDnAvas = listAvas.get( oldAva.getAttributeType().getOid() );
                            
                            if ( modDnAvas == null )
                            {
                                modDnAvas = new ArrayList<>();
                                listAvas.put( oldAva.getAttributeType().getOid(), modDnAvas );
                            }

                            modDnAvas.add( new ModDnAva( ModDnAva.ModDnType.UPDATE_DELETE, oldAva ) );
                            found = true;
                            break;
                        }
                    }
                }
                
                if ( !found )
                {
                    List<ModDnAva> modDnAvas = listAvas.get( oldAva.getAttributeType().getOid() );
                    
                    if ( modDnAvas == null )
                    {
                        modDnAvas = new ArrayList<>();
                        listAvas.put( oldAva.getAttributeType().getOid(), modDnAvas );
                    }
                    
                    modDnAvas.add( new ModDnAva( ModDnAva.ModDnType.DELETE, oldAva ) );
                }
            }
        }
        
        return listAvas;
    }
    
    
    private void applyRdn( MoveAndRenameOperationContext moveAndRenameContext, Map<String, List<ModDnAva>> modifiedAvas ) throws LdapException
    {
        Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();
        List<ModDnAva> removedSVs = null;
        
        for ( List<ModDnAva> modDnAvas : modifiedAvas.values() )
        {
            List<ModDnAva> addedModDnAvs = new ArrayList<>();
            
            for ( ModDnAva modDnAva : modDnAvas )
            {
                Ava ava = modDnAva.getAva();
                
                switch ( modDnAva.getType() )
                {
                    case ADD :
                    case UPDATE_ADD :
                        // Check that the AT is not SV, otherwise we have to delete the old value
                        if ( ava.getAttributeType().isSingleValued() )
                        {
                            Attribute svAttribute = modifiedEntry.get( ava.getAttributeType() );
                            modifiedEntry.removeAttributes( ava.getAttributeType() );
                            
                            if ( removedSVs == null )
                            {
                                removedSVs = new ArrayList<>();
                            }
                            
                            addedModDnAvs.add( new ModDnAva( ModDnAva.ModDnType.UPDATE_DELETE, ava ) );
                            removedSVs.add( new ModDnAva( ModDnAva.ModDnType.UPDATE_DELETE, new Ava( schemaManager, svAttribute.getId(), svAttribute.getString() ) ) );
                        }
                        
                        modifiedEntry.add( ava.getAttributeType(), ava.getValue() );
                        break;
                        
                    case DELETE :
                    case UPDATE_DELETE :
                        modifiedEntry.remove( ava.getAttributeType(), ava.getValue() );
                        break;
                        
                    default :
                        break;
                }
            }
            
            modDnAvas.addAll( addedModDnAvs );
        }
        
        // Add the SV attributes that has to be removed to the list of ModDnAva
        if ( removedSVs != null )
        {
            for ( ModDnAva modDnAva : removedSVs )
            {
                String oid = modDnAva.getAva().getAttributeType().getOid();
                List<ModDnAva> modDnAvas = modifiedAvas.get( oid );
                
                modDnAvas.add( modDnAva );
            }
        }

        moveAndRenameContext.setModifiedAvas( modifiedAvas );
        moveAndRenameContext.setModifiedEntry( modifiedEntry );
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        // We will compute the modified entry, and check that its still valid :
        // - the new RDn's AVAs must be compatible with the existing ObjectClasses (except if the Extensible ObjectClass is present)
        // - The removal of the old RDN (if requested) must not left the entry invalid
        // - if the new RDN has SV AT, then we should remove the old RDN's AVA if it's using the same AT
        Entry entry = moveAndRenameContext.getOriginalEntry();
        Dn entryDn = entry.getDn();
        Rdn oldRdn = entryDn.getRdn();
        Rdn newRdn = moveAndRenameContext.getNewRdn();
        
        // First get the list of impacted AVAs
        Map<String, List<ModDnAva>> modifiedAvas = processRdn( oldRdn, newRdn, moveAndRenameContext.getDeleteOldRdn() );
        
        // Check if they will left the entry in a correct state
        applyRdn( moveAndRenameContext, modifiedAvas );
        
        // Check the modified entry now
        check( moveAndRenameContext.getNewDn(), moveAndRenameContext.getModifiedEntry() );

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Dn oldDn = renameContext.getDn();
        Rdn newRdn = renameContext.getNewRdn();
        boolean deleteOldRn = renameContext.getDeleteOldRdn();
        Entry entry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getClonedEntry();

        /*
         *  Note: This is only a consistency checks, to the ensure that all
         *  mandatory attributes are available after deleting the old Rdn.
         *  The real modification is done in the XdbmStore class.
         *  - TODO: this check is missing in the moveAndRename() method
         */
        if ( deleteOldRn )
        {
            Rdn oldRdn = oldDn.getRdn();

            // Delete the old Rdn means we remove some attributes and values.
            // We must make sure that after this operation all must attributes
            // are still present in the entry.
            for ( Ava atav : oldRdn )
            {
                AttributeType type = schemaManager.lookupAttributeTypeRegistry( atav.getType() );
                entry.remove( type, atav.getValue() );
            }

            // Check that no operational attributes are removed
            for ( Ava atav : oldRdn )
            {
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( atav.getType() );

                if ( !attributeType.isUserModifiable() )
                {
                    throw new LdapNoPermissionException( "Cannot modify the attribute '" + atav.getType() + "'" );
                }
            }
        }

        for ( Ava atav : newRdn )
        {
            AttributeType type = schemaManager.lookupAttributeTypeRegistry( atav.getType() );

            entry.add( new DefaultAttribute( type, atav.getValue() ) );
        }

        // Substitute the Rdn and check if the new entry is correct
        entry.setDn( renameContext.getNewDn() );

        check( renameContext.getNewDn(), entry );

        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn base = searchContext.getDn();
        ExprNode filter = searchContext.getFilter();

        // We also have to check the H/R flag for the filter attributes
        checkFilter( filter );

        // Deal with the normal case : searching for a normal value (not subSchemaSubEntry)
        if ( !subschemaSubentryDn.equals( base ) )
        {
            EntryFilteringCursor cursor = next( searchContext );

            if ( searchContext.getReturningAttributesString() != null )
            {
                cursor.addEntryFilter( topFilter );
                return cursor;
            }

            for ( EntryFilter ef : filters )
            {
                cursor.addEntryFilter( ef );
            }

            return cursor;
        }

        // The user was searching into the subSchemaSubEntry
        // This kind of search _must_ be limited to OBJECT scope (the subSchemaSubEntry
        // does not have any sub level)
        if ( searchContext.getScope() == SearchScope.OBJECT )
        {
            // The filter can be an equality or (ObjectClass=*) but nothing else
            if ( filter instanceof SimpleNode )
            {
                // We should get the value for the filter.
                // only 'top' and 'subSchema' are valid values
                SimpleNode node = ( SimpleNode ) filter;
                String objectClass;

                objectClass = node.getValue().getString();

                String objectClassOid;

                if ( schemaManager.getObjectClassRegistry().contains( objectClass ) )
                {
                    objectClassOid = schemaManager.lookupObjectClassRegistry( objectClass ).getOid();
                }
                else
                {
                    return new EntryFilteringCursorImpl( new EmptyCursor<Entry>(), searchContext, schemaManager );
                }

                AttributeType nodeAt = node.getAttributeType();

                // see if node attribute is objectClass
                if ( nodeAt.equals( directoryService.getAtProvider().getObjectClass() )
                    && ( objectClassOid.equals( SchemaConstants.TOP_OC_OID ) || objectClassOid
                        .equals( SchemaConstants.SUBSCHEMA_OC_OID ) ) && ( node instanceof EqualityNode ) )
                {
                    Entry serverEntry = SchemaService.getSubschemaEntry( directoryService,
                        searchContext );
                    serverEntry.setDn( base );
                    return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( serverEntry ), searchContext,
                        schemaManager );
                }
                else
                {
                    return new EntryFilteringCursorImpl( new EmptyCursor<Entry>(), searchContext, schemaManager );
                }
            }
            else if ( filter instanceof ObjectClassNode )
            {
                // This is (ObjectClass=*)
                Entry serverEntry = SchemaService.getSubschemaEntry( directoryService,
                    searchContext );
                serverEntry.setDn( base );
                return new EntryFilteringCursorImpl(
                    new SingletonCursor<Entry>( serverEntry ), searchContext, schemaManager );
            }
        }

        // In any case not handled previously, just return an empty result
        return new EntryFilteringCursorImpl( new EmptyCursor<Entry>(), searchContext, schemaManager );
    }


    private String getSchemaName( Dn dn ) throws LdapException
    {
        int size = dn.size();

        if ( size < 2 )
        {
            throw new LdapException( I18n.err( I18n.ERR_276 ) );
        }

        Rdn rdn = dn.getRdn( size - 2 );

        return rdn.getValue();
    }


    /**
     * Checks to see if an attribute is required by as determined from an entry's
     * set of objectClass attribute values.
     *
     * @return true if the objectClass values require the attribute, false otherwise
     * @throws Exception if the attribute is not recognized
     */
    private void assertAllAttributesAllowed( Dn dn, Entry entry, Set<String> allowed ) throws LdapException
    {
        // Loop on all the attributes
        for ( Attribute attribute : entry )
        {
            String attrOid = attribute.getAttributeType().getOid();

            AttributeType attributeType = attribute.getAttributeType();

            if ( !attributeType.isCollective() && ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS )
                && !allowed.contains( attrOid ) )
            {
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_277,
                    attribute.getUpId(), dn.getName() ) );
            }
        }
    }


    /**
     * Checks to see number of values of an attribute conforms to the schema
     */
    private void assertNumberOfAttributeValuesValid( Entry entry ) throws LdapInvalidAttributeValueException
    {
        for ( Attribute attribute : entry )
        {
            assertNumberOfAttributeValuesValid( attribute );
        }
    }


    /**
     * Checks to see numbers of values of attributes conforms to the schema
     */
    private void assertNumberOfAttributeValuesValid( Attribute attribute ) throws LdapInvalidAttributeValueException
    {
        if ( attribute.size() > 1 && attribute.getAttributeType().isSingleValued() )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.CONSTRAINT_VIOLATION, I18n.err( I18n.ERR_278,
                attribute.getUpId() ) );
        }
    }


    /**
     * Checks to see the presence of all required attributes within an entry.
     */
    private void assertRequiredAttributesPresent( Dn dn, Entry entry, Set<String> must ) throws LdapException
    {
        for ( Attribute attribute : entry )
        {
            must.remove( attribute.getAttributeType().getOid() );
        }

        if ( !must.isEmpty() )
        {
            // include AT names for better error reporting
            StringBuilder sb = new StringBuilder();
            sb.append( '[' );

            for ( String oid : must )
            {
                String name = schemaManager.getAttributeType( oid ).getName();
                sb.append( name )
                    .append( '(' )
                    .append( oid )
                    .append( "), " );
            }

            int end = sb.length();
            sb.replace( end - 2, end, "" ); // remove the trailing ', '
            sb.append( ']' );

            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_279,
                sb, dn.getName() ) );
        }
    }


    /**
     * Checck that OC does not conflict :
     * - we can't have more than one STRUCTURAL OC unless they are in the same
     * inheritance tree
     * - we must have at least one STRUCTURAL OC
     */
    private void assertObjectClasses( Dn dn, List<ObjectClass> ocs ) throws LdapException
    {
        Set<ObjectClass> structuralObjectClasses = new HashSet<>();

        /*
         * Since the number of ocs present in an entry is small it's not
         * so expensive to take two passes while determining correctness
         * since it will result in clear simple code instead of a deep nasty
         * for loop with nested loops.  Plus after the first pass we can
         * quickly know if there are no structural object classes at all.
         */

        // --------------------------------------------------------------------
        // Extract all structural objectClasses within the entry
        // --------------------------------------------------------------------
        for ( ObjectClass oc : ocs )
        {
            if ( oc.isStructural() )
            {
                structuralObjectClasses.add( oc );
            }
        }

        // --------------------------------------------------------------------
        // Throw an error if no STRUCTURAL objectClass are found.
        // --------------------------------------------------------------------

        if ( structuralObjectClasses.isEmpty() )
        {
            String message = I18n.err( I18n.ERR_60, dn );
            LOG.error( message );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, message );
        }

        // --------------------------------------------------------------------
        // Put all structural object classes into new remaining container and
        // start removing any which are superiors of others in the set.  What
        // is left in the remaining set will be unrelated structural
        /// objectClasses.  If there is more than one then we have a problem.
        // --------------------------------------------------------------------

        Set<ObjectClass> remaining = new HashSet<>( structuralObjectClasses.size() );
        remaining.addAll( structuralObjectClasses );

        for ( ObjectClass oc : structuralObjectClasses )
        {
            if ( oc.getSuperiors() != null )
            {
                for ( ObjectClass superClass : oc.getSuperiors() )
                {
                    if ( superClass.isStructural() )
                    {
                        remaining.remove( superClass );
                    }
                }
            }
        }

        // Like the highlander there can only be one :).
        if ( remaining.size() > 1 )
        {
            String message = I18n.err( I18n.ERR_61, dn, remaining );
            LOG.error( message );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, message );
        }
    }


    /**
     * Check the entry attributes syntax, using the syntaxCheckers
     */
    private void assertSyntaxes( Entry entry ) throws LdapException
    {
        // First, loop on all attributes
        for ( Attribute attribute : entry )
        {
            AttributeType attributeType = attribute.getAttributeType();
            SyntaxChecker syntaxChecker = attributeType.getSyntax().getSyntaxChecker();

            if ( syntaxChecker instanceof OctetStringSyntaxChecker )
            {
                // This is a speedup : no need to check the syntax of any value
                // if all the syntaxes are accepted...
                continue;
            }

            // Then loop on all values
            for ( Value value : attribute )
            {
                if ( value.isSchemaAware() )
                {
                    // No need to validate something which is already ok
                    continue;
                }

                if ( !syntaxChecker.isValidSyntax( value.getString() ) )
                {
                    String message = I18n.err( I18n.ERR_280, value.getString(), attribute.getUpId() );
                    LOG.info( message );
                    throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                }
            }
        }
    }


    private void assertRdn( Dn dn, Entry entry ) throws LdapException
    {
        for ( Ava atav : dn.getRdn() )
        {
            Attribute attribute = entry.get( atav.getNormType() );

            if ( ( attribute == null ) || ( !attribute.contains( atav.getValue() ) ) )
            {
                String message = I18n.err( I18n.ERR_62, dn, atav.getType() );
                LOG.error( message );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, message );
            }
        }
    }


    /**
     * Check a String attribute to see if there is some byte[] value in it.
     *
     * If this is the case, try to change it to a String value.
     */
    private boolean checkHumanReadable( Attribute attribute ) throws LdapException
    {
        boolean isModified = false;

        // Loop on each values
        for ( Value value : attribute )
        {
            if ( !value.isHumanReadable() )
            {
                // we have a byte[] value. It should be a String UTF-8 encoded
                // Let's transform it
                String valStr = new String( value.getBytes(), Charsets.UTF_8 );
                attribute.remove( value );
                attribute.add( valStr );
                isModified = true;
            }
        }

        return isModified;
    }


    /**
     * Check a binary attribute to see if there is some String value in it.
     *
     * If this is the case, try to change it to a binary value.
     */
    private boolean checkNotHumanReadable( Attribute attribute ) throws LdapException
    {
        boolean isModified = false;

        // Loop on each values
        for ( Value value : attribute )
        {
            if ( value.isHumanReadable() )
            {
                // We have a String value. It should be a byte[]
                // Let's transform it
                byte[] valBytes = value.getBytes();

                attribute.remove( value );
                attribute.add( valBytes );
                isModified = true;
            }
        }

        return isModified;
    }


    /**
     * Check that all the attribute's values which are Human Readable can be transformed
     * to valid String if they are stored as byte[], and that non Human Readable attributes
     * stored as String can be transformed to byte[]
     */
    private Entry assertHumanReadable( Entry entry ) throws LdapException
    {
        Entry clonedEntry = null;

        // Loops on all attributes
        for ( Attribute attribute : entry )
        {
            boolean isModified;
            
            AttributeType attributeType = attribute.getAttributeType();

            // If the attributeType is H-R, check all of its values
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                isModified = checkHumanReadable( attribute );
            }
            else
            {
                isModified = checkNotHumanReadable( attribute );
            }

            // If we have a returned attribute, then we need to store it
            // into a new entry
            if ( isModified )
            {
                if ( clonedEntry == null )
                {
                    clonedEntry = entry.clone();
                }

                // Switch the attributes
                clonedEntry.put( attribute );
            }
        }

        if ( clonedEntry != null )
        {
            return clonedEntry;
        }
        else
        {
            return entry;
        }
    }
}
